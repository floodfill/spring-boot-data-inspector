/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides MongoDB statistics and information
 */
@Slf4j
public class MongoDBDataSourceProvider implements DataSourceProvider {

    private final MongoTemplate mongoTemplate;

    public MongoDBDataSourceProvider(Optional<MongoTemplate> mongoTemplate) {
        this.mongoTemplate = mongoTemplate.orElse(null);
        if (mongoTemplate.isPresent()) {
            log.info("MongoDB data source provider enabled");
        }
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        if (mongoTemplate == null) {
            return Collections.emptyList();
        }

        try {
            List<DataSourceInfo> dataSources = new ArrayList<>();

            // Overall MongoDB stats
            dataSources.add(DataSourceInfo.builder()
                    .id("mongodb:stats")
                    .name("MongoDB Statistics")
                    .type("mongodb")
                    .description("MongoDB connection and database statistics")
                    .size(getCollectionCount())
                    .queryable(true)
                    .metadata(getDatabaseMetadata())
                    .build());

            // Individual collections
            Set<String> collectionNames = mongoTemplate.getCollectionNames();
            for (String collectionName : collectionNames) {
                long count = mongoTemplate.getCollection(collectionName).countDocuments();
                dataSources.add(DataSourceInfo.builder()
                        .id("mongodb:collection:" + collectionName)
                        .name("Collection: " + collectionName)
                        .type("mongodb-collection")
                        .description("MongoDB collection: " + collectionName)
                        .size(count)
                        .queryable(true)
                        .metadata(Map.of("collectionName", collectionName))
                        .build());
            }

            return dataSources;
        } catch (Exception e) {
            log.warn("MongoDB is not available, skipping MongoDB data sources: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        if (dataSourceId.equals("mongodb:stats")) {
            return queryDatabaseStats();
        } else if (dataSourceId.startsWith("mongodb:collection:")) {
            String collectionName = dataSourceId.substring("mongodb:collection:".length());
            return queryCollection(collectionName, filters, limit, offset);
        }

        return QueryResult.builder()
                .dataSourceId(dataSourceId)
                .data(Collections.emptyList())
                .totalCount(0)
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("mongodb:");
    }

    private long getCollectionCount() {
        try {
            return mongoTemplate.getCollectionNames().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> getDatabaseMetadata() {
        try {
            MongoDatabase database = mongoTemplate.getDb();
            Document stats = database.runCommand(new Document("dbStats", 1));

            return Map.of(
                    "databaseName", database.getName(),
                    "collections", stats.getInteger("collections", 0),
                    "dataSize", formatBytes(stats.getDouble("dataSize")),
                    "storageSize", formatBytes(stats.getDouble("storageSize")),
                    "indexes", stats.getInteger("indexes", 0)
            );
        } catch (Exception e) {
            log.debug("Could not retrieve MongoDB metadata", e);
            return Collections.emptyMap();
        }
    }

    private QueryResult queryDatabaseStats() {
        MongoDatabase database = mongoTemplate.getDb();
        Document stats = database.runCommand(new Document("dbStats", 1));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("database", database.getName());
        data.put("collections", stats.getInteger("collections", 0));
        data.put("views", stats.getInteger("views", 0));
        Long objects = stats.getLong("objects");
        data.put("objects", objects != null ? objects : 0L);
        data.put("dataSize", formatBytes(stats.getDouble("dataSize")));
        data.put("storageSize", formatBytes(stats.getDouble("storageSize")));
        data.put("indexes", stats.getInteger("indexes", 0));
        data.put("indexSize", formatBytes(stats.getDouble("indexSize")));
        data.put("totalSize", formatBytes(stats.getDouble("totalSize")));

        // Get server status
        try {
            Document serverStatus = database.runCommand(new Document("serverStatus", 1));
            Document connections = serverStatus.get("connections", Document.class);
            if (connections != null) {
                Long totalCreated = connections.getLong("totalCreated");
                data.put("connections", Map.of(
                        "current", connections.getInteger("current", 0),
                        "available", connections.getInteger("available", 0),
                        "totalCreated", totalCreated != null ? totalCreated : 0L
                ));
            }
        } catch (Exception e) {
            log.debug("Could not retrieve server status", e);
        }

        return QueryResult.builder()
                .dataSourceId("mongodb:stats")
                .data(List.of(data))
                .totalCount(1)
                .limit(1)
                .offset(0)
                .build();
    }

    private QueryResult queryCollection(String collectionName, Map<String, Object> filters, int limit, int offset) {
        // Build filter document
        Document filterDoc = new Document();
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                // Support simple equality filters
                // For more complex queries, users can use MongoDB compass or shell
                filterDoc.append(entry.getKey(), entry.getValue());
            }
        }

        long totalCount = mongoTemplate.getCollection(collectionName).countDocuments(filterDoc);

        // Get documents with filters
        List<Map<String, Object>> documents = mongoTemplate.getCollection(collectionName)
                .find(filterDoc)
                .skip(offset)
                .limit(limit)
                .into(new ArrayList<>())
                .stream()
                .map(doc -> new LinkedHashMap<String, Object>(doc))
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("mongodb:collection:" + collectionName)
                .data(documents)
                .totalCount(totalCount)
                .limit(limit)
                .offset(offset)
                .stats(Map.of(
                        "collectionName", collectionName,
                        "filtersApplied", filters != null ? filters.size() : 0
                ))
                .build();
    }

    private String formatBytes(double bytes) {
        if (bytes < 1024) return String.format("%.0f B", bytes);
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024 * 1024));
        return String.format("%.2f GB", bytes / (1024 * 1024 * 1024));
    }
}
