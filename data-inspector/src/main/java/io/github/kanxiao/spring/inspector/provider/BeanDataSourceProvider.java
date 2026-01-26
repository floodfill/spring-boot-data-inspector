/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exposes Spring beans and their state for inspection
 */
@Slf4j
@Component
public class BeanDataSourceProvider implements DataSourceProvider {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    public BeanDataSourceProvider(ApplicationContext applicationContext, ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.objectMapper = objectMapper;
        log.info("Bean data source provider enabled");
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        // Group beans by type to make it more manageable
        Map<String, List<String>> beansByType = new HashMap<>();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                String packageName = bean.getClass().getPackage() != null ?
                        bean.getClass().getPackage().getName() : "default";

                // Only include application beans, not framework beans
                if (packageName.startsWith("com.example")) {
                    String simpleType = bean.getClass().getSimpleName();
                    beansByType.computeIfAbsent(simpleType, k -> new ArrayList<>()).add(beanName);
                }
            } catch (Exception e) {
                // Skip beans we can't instantiate
            }
        }

        List<DataSourceInfo> dataSources = new ArrayList<>();

        // Create a data source for application beans
        dataSources.add(DataSourceInfo.builder()
                .id("beans:application")
                .name("Application Beans")
                .type("bean")
                .description("All application Spring beans and their state")
                .size(beansByType.values().stream().mapToLong(List::size).sum())
                .queryable(true)
                .metadata(Map.of(
                        "beanTypes", beansByType.keySet(),
                        "totalBeans", beansByType.values().stream().mapToLong(List::size).sum()
                ))
                .build());

        return dataSources;
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        List<Map<String, Object>> beanData = new ArrayList<>();

        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                String packageName = bean.getClass().getPackage() != null ?
                        bean.getClass().getPackage().getName() : "default";

                // Only include application beans
                if (!packageName.startsWith("com.example")) {
                    continue;
                }

                Map<String, Object> beanInfo = new LinkedHashMap<>();
                beanInfo.put("beanName", beanName);
                beanInfo.put("beanType", bean.getClass().getSimpleName());
                beanInfo.put("fullClassName", bean.getClass().getName());
                beanInfo.put("scope", getBeanScope(beanName));

                // Extract bean fields and their values
                Map<String, Object> fields = extractFields(bean);
                beanInfo.put("fields", fields);
                beanInfo.put("fieldCount", fields.size());

                beanData.add(beanInfo);

            } catch (Exception e) {
                log.debug("Could not inspect bean: {}", beanName, e);
            }
        }

        // Apply filters
        List<Map<String, Object>> filtered = applyFilters(beanData, filters);

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
                .stats(Map.of("totalBeans", total))
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("beans:");
    }

    private String getBeanScope(String beanName) {
        try {
            if (applicationContext instanceof org.springframework.context.ConfigurableApplicationContext) {
                org.springframework.context.ConfigurableApplicationContext configurableContext =
                    (org.springframework.context.ConfigurableApplicationContext) applicationContext;
                BeanDefinition beanDefinition = configurableContext.getBeanFactory().getBeanDefinition(beanName);
                return beanDefinition.getScope();
            }
            return "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private Map<String, Object> extractFields(Object bean) {
        Map<String, Object> fields = new LinkedHashMap<>();

        Class<?> clazz = bean.getClass();
        while (clazz != null && !clazz.equals(Object.class)) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(bean);

                    // Simplify complex objects
                    if (value != null && !isPrimitiveOrWrapper(value.getClass())) {
                        fields.put(field.getName(), Map.of(
                                "type", value.getClass().getSimpleName(),
                                "toString", value.toString()
                        ));
                    } else {
                        fields.put(field.getName(), value);
                    }
                } catch (Exception e) {
                    fields.put(field.getName(), "<inaccessible>");
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fields;
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
                type.equals(String.class) ||
                type.equals(Integer.class) ||
                type.equals(Long.class) ||
                type.equals(Double.class) ||
                type.equals(Float.class) ||
                type.equals(Boolean.class) ||
                type.equals(Character.class) ||
                type.equals(Byte.class) ||
                type.equals(Short.class);
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
}
