package com.example.datainspector.provider;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import com.example.datainspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides access to application properties and environment variables
 * Useful for debugging configuration issues in production
 */
@Slf4j
@Component
public class EnvironmentProvider implements DataSourceProvider {

    private final Environment environment;
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "secret", "key", "token", "credential", "auth",
            "api_key", "apikey", "private", "cert", "pwd", "pass"
    );

    public EnvironmentProvider(Environment environment) {
        this.environment = environment;
        log.info("Environment provider enabled");
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        List<DataSourceInfo> dataSources = new ArrayList<>();

        dataSources.add(DataSourceInfo.builder()
                .id("env:properties")
                .name("Application Properties")
                .type("environment")
                .description("All application properties from all property sources")
                .size(getAllProperties().size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("env:system")
                .name("System Properties")
                .type("environment")
                .description("JVM system properties")
                .size(System.getProperties().size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("env:variables")
                .name("Environment Variables")
                .type("environment")
                .description("Operating system environment variables")
                .size(System.getenv().size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("env:profiles")
                .name("Active Profiles")
                .type("environment")
                .description("Spring active profiles and default profiles")
                .size(environment.getActiveProfiles().length + environment.getDefaultProfiles().length)
                .queryable(true)
                .build());

        return dataSources;
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        return switch (dataSourceId) {
            case "env:properties" -> queryProperties(filters, limit, offset);
            case "env:system" -> querySystemProperties(filters, limit, offset);
            case "env:variables" -> queryEnvironmentVariables(filters, limit, offset);
            case "env:profiles" -> queryProfiles();
            default -> QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        };
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("env:");
    }

    private QueryResult queryProperties(Map<String, Object> filters, int limit, int offset) {
        Map<String, Object> allProperties = getAllProperties();

        List<Map<String, Object>> data = allProperties.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", entry.getKey());
                    row.put("value", maskSensitiveValue(entry.getKey(), entry.getValue()));
                    row.put("source", getPropertySource(entry.getKey()));
                    return row;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("key")))
                .collect(Collectors.toList());

        // Apply filters
        data = applyFilters(data, filters);

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("env:properties")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    private QueryResult querySystemProperties(Map<String, Object> filters, int limit, int offset) {
        List<Map<String, Object>> data = System.getProperties().entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", entry.getKey().toString());
                    row.put("value", maskSensitiveValue(entry.getKey().toString(), entry.getValue()));
                    return row;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("key")))
                .collect(Collectors.toList());

        // Apply filters
        data = applyFilters(data, filters);

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("env:system")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    private QueryResult queryEnvironmentVariables(Map<String, Object> filters, int limit, int offset) {
        List<Map<String, Object>> data = System.getenv().entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", entry.getKey());
                    row.put("value", maskSensitiveValue(entry.getKey(), entry.getValue()));
                    return row;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("key")))
                .collect(Collectors.toList());

        // Apply filters
        data = applyFilters(data, filters);

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("env:variables")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    private QueryResult queryProfiles() {
        List<Map<String, Object>> data = new ArrayList<>();

        Map<String, Object> active = new LinkedHashMap<>();
        active.put("type", "Active Profiles");
        active.put("profiles", Arrays.toString(environment.getActiveProfiles()));
        active.put("count", environment.getActiveProfiles().length);
        data.add(active);

        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("type", "Default Profiles");
        defaults.put("profiles", Arrays.toString(environment.getDefaultProfiles()));
        defaults.put("count", environment.getDefaultProfiles().length);
        data.add(defaults);

        return QueryResult.builder()
                .dataSourceId("env:profiles")
                .data(data)
                .totalCount(data.size())
                .limit(data.size())
                .offset(0)
                .build();
    }

    private Map<String, Object> getAllProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();

        if (environment instanceof ConfigurableEnvironment configurableEnv) {
            for (PropertySource<?> propertySource : configurableEnv.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource<?> enumerableSource) {
                    for (String key : enumerableSource.getPropertyNames()) {
                        if (!properties.containsKey(key)) {
                            properties.put(key, environment.getProperty(key));
                        }
                    }
                }
            }
        }

        return properties;
    }

    private String getPropertySource(String key) {
        if (environment instanceof ConfigurableEnvironment configurableEnv) {
            for (PropertySource<?> propertySource : configurableEnv.getPropertySources()) {
                if (propertySource instanceof EnumerablePropertySource<?> enumerableSource) {
                    for (String propertyName : enumerableSource.getPropertyNames()) {
                        if (propertyName.equals(key)) {
                            return propertySource.getName();
                        }
                    }
                }
            }
        }
        return "unknown";
    }

    private Object maskSensitiveValue(String key, Object value) {
        if (value == null) {
            return null;
        }

        String lowerKey = key.toLowerCase();
        for (String sensitiveKey : SENSITIVE_KEYS) {
            if (lowerKey.contains(sensitiveKey)) {
                return "***MASKED***";
            }
        }

        return value;
    }

    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> data, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return data;
        }

        return data.stream()
                .filter(entry -> {
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        Object entryValue = entry.get(filter.getKey());
                        if (entryValue == null || !entryValue.toString().contains(filter.getValue().toString())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
