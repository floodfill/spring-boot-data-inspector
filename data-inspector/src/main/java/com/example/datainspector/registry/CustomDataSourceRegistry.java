package com.example.datainspector.registry;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import com.example.datainspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Registry for manually registering custom data sources
 * Users can register their own data structures here
 */
@Slf4j
@Component
public class CustomDataSourceRegistry implements DataSourceProvider {

    private final Map<String, RegisteredDataSource> dataSources = new ConcurrentHashMap<>();

    /**
     * Register a custom data source
     *
     * @param id Unique identifier
     * @param name Display name
     * @param description Description
     * @param dataSupplier Supplier that returns the data
     */
    public void register(String id, String name, String description, Supplier<Collection<?>> dataSupplier) {
        dataSources.put(id, new RegisteredDataSource(id, name, description, dataSupplier));
        log.info("Registered custom data source: {}", id);
    }

    /**
     * Unregister a data source
     */
    public void unregister(String id) {
        dataSources.remove(id);
        log.info("Unregistered custom data source: {}", id);
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        return dataSources.values().stream()
                .map(rds -> DataSourceInfo.builder()
                        .id("custom:" + rds.id)
                        .name(rds.name)
                        .type("custom")
                        .description(rds.description)
                        .size(getSize(rds))
                        .queryable(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        String id = dataSourceId.substring("custom:".length());
        RegisteredDataSource rds = dataSources.get(id);

        if (rds == null) {
            return QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }

        Collection<?> data = rds.dataSupplier.get();
        List<Map<String, Object>> converted = data.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        // Apply filters
        List<Map<String, Object>> filtered = applyFilters(converted, filters);

        // Apply pagination
        int total = filtered.size();
        List<Map<String, Object>> paginated = filtered.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId(dataSourceId)
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("custom:");
    }

    private long getSize(RegisteredDataSource rds) {
        try {
            Collection<?> data = rds.dataSupplier.get();
            return data != null ? data.size() : 0;
        } catch (Exception e) {
            log.debug("Could not get size for data source: {}", rds.id, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }

        // Simple conversion - users can customize this
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", obj.getClass().getSimpleName());
        map.put("value", obj.toString());
        return map;
    }

    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> entries, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return entries;
        }

        return entries.stream()
                .filter(entry -> {
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        Object entryValue = entry.get(filter.getKey());
                        if (!Objects.equals(entryValue, filter.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private static class RegisteredDataSource {
        final String id;
        final String name;
        final String description;
        final Supplier<Collection<?>> dataSupplier;

        RegisteredDataSource(String id, String name, String description, Supplier<Collection<?>> dataSupplier) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.dataSupplier = dataSupplier;
        }
    }
}
