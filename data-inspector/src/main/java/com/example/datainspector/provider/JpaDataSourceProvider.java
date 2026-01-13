package com.example.datainspector.provider;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import com.example.datainspector.spi.DataSourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.EntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides JPA entity data sources for SQL databases
 * Discovers all JPA entities and allows querying them
 */
@Slf4j
@Component
public class JpaDataSourceProvider implements DataSourceProvider {

    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;

    public JpaDataSourceProvider(Optional<EntityManagerFactory> entityManagerFactory, ObjectMapper objectMapper) {
        this.entityManagerFactory = entityManagerFactory.orElse(null);
        this.objectMapper = objectMapper;
        if (entityManagerFactory.isPresent()) {
            log.info("JPA data source provider enabled");
        }
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        if (entityManagerFactory == null) {
            return Collections.emptyList();
        }

        try {
            Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();
            EntityManager em = entityManagerFactory.createEntityManager();

            List<DataSourceInfo> dataSources = new ArrayList<>();

            // Add database overview
            dataSources.add(DataSourceInfo.builder()
                    .id("jpa:overview")
                    .name("Database Overview")
                    .type("jpa-overview")
                    .description("Overview of all JPA entities and database statistics")
                    .size(entities.size())
                    .queryable(true)
                    .build());

            // Add each entity as a data source
            for (EntityType<?> entity : entities) {
                String entityName = entity.getName();
                Class<?> javaType = entity.getJavaType();

                try {
                    long count = getEntityCount(em, entityName);

                    dataSources.add(DataSourceInfo.builder()
                            .id("jpa:entity:" + entityName)
                            .name("Entity: " + entityName)
                            .type("jpa-entity")
                            .description("JPA Entity: " + javaType.getName())
                            .size(count)
                            .queryable(true)
                            .metadata(Map.of(
                                    "entityName", entityName,
                                    "javaType", javaType.getName(),
                                    "tableName", getTableName(entity)
                            ))
                            .build());
                } catch (Exception e) {
                    log.debug("Could not get count for entity {}: {}", entityName, e.getMessage());
                }
            }

            em.close();
            return dataSources;
        } catch (Exception e) {
            log.warn("JPA is not available or no entities found: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        if (dataSourceId.equals("jpa:overview")) {
            return queryOverview();
        } else if (dataSourceId.startsWith("jpa:entity:")) {
            String entityName = dataSourceId.substring("jpa:entity:".length());
            return queryEntity(entityName, filters, limit, offset);
        }

        return QueryResult.builder()
                .dataSourceId(dataSourceId)
                .data(Collections.emptyList())
                .totalCount(0)
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("jpa:");
    }

    private long getEntityCount(EntityManager em, String entityName) {
        Query query = em.createQuery("SELECT COUNT(e) FROM " + entityName + " e");
        return ((Number) query.getSingleResult()).longValue();
    }

    private String getTableName(EntityType<?> entity) {
        try {
            jakarta.persistence.Table table = entity.getJavaType().getAnnotation(jakarta.persistence.Table.class);
            if (table != null && !table.name().isEmpty()) {
                return table.name();
            }
            return entity.getName();
        } catch (Exception e) {
            return entity.getName();
        }
    }

    private QueryResult queryOverview() {
        EntityManager em = entityManagerFactory.createEntityManager();
        Set<EntityType<?>> entities = entityManagerFactory.getMetamodel().getEntities();

        List<Map<String, Object>> data = new ArrayList<>();

        for (EntityType<?> entity : entities) {
            String entityName = entity.getName();
            try {
                long count = getEntityCount(em, entityName);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("entityName", entityName);
                row.put("javaType", entity.getJavaType().getName());
                row.put("tableName", getTableName(entity));
                row.put("recordCount", count);
                row.put("attributes", entity.getAttributes().size());
                data.add(row);
            } catch (Exception e) {
                log.debug("Error querying entity {}: {}", entityName, e.getMessage());
            }
        }

        em.close();

        return QueryResult.builder()
                .dataSourceId("jpa:overview")
                .data(data)
                .totalCount(data.size())
                .limit(data.size())
                .offset(0)
                .build();
    }

    private QueryResult queryEntity(String entityName, Map<String, Object> filters, int limit, int offset) {
        EntityManager em = entityManagerFactory.createEntityManager();

        try {
            // Get total count
            long totalCount = getEntityCount(em, entityName);

            // Build query with filters
            StringBuilder jpql = new StringBuilder("SELECT e FROM " + entityName + " e");

            if (filters != null && !filters.isEmpty()) {
                jpql.append(" WHERE ");
                List<String> conditions = new ArrayList<>();
                for (String key : filters.keySet()) {
                    conditions.add("e." + key + " = :" + key);
                }
                jpql.append(String.join(" AND ", conditions));
            }

            TypedQuery<?> query = em.createQuery(jpql.toString(), Object.class);

            // Set filter parameters
            if (filters != null) {
                for (Map.Entry<String, Object> entry : filters.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }

            // Apply pagination
            query.setFirstResult(offset);
            query.setMaxResults(limit);

            List<?> results = query.getResultList();

            // Convert entities to maps
            List<Map<String, Object>> data = results.stream()
                    .map(this::convertEntityToMap)
                    .collect(Collectors.toList());

            return QueryResult.builder()
                    .dataSourceId("jpa:entity:" + entityName)
                    .data(data)
                    .totalCount(totalCount)
                    .limit(limit)
                    .offset(offset)
                    .stats(Map.of("entityName", entityName))
                    .build();
        } catch (Exception e) {
            log.error("Error querying entity {}: {}", entityName, e.getMessage(), e);
            return QueryResult.builder()
                    .dataSourceId("jpa:entity:" + entityName)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .limit(limit)
                    .offset(offset)
                    .build();
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertEntityToMap(Object entity) {
        try {
            // Use ObjectMapper to convert entity to map
            return objectMapper.convertValue(entity, Map.class);
        } catch (Exception e) {
            log.debug("Could not convert entity to map: {}", e.getMessage());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", entity.getClass().getSimpleName());
            map.put("value", entity.toString());
            return map;
        }
    }
}
