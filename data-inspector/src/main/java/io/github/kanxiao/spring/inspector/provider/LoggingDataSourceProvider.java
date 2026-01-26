/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides logging configuration inspection and modification
 */
@Slf4j
public class LoggingDataSourceProvider implements DataSourceProvider {

    private final LoggingSystem loggingSystem;

    public LoggingDataSourceProvider() {
        this.loggingSystem = LoggingSystem.get(ClassLoader.getSystemClassLoader());
        log.info("Logging data source provider enabled");
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        return List.of(
                DataSourceInfo.builder()
                        .id("logging:loggers")
                        .name("Loggers")
                        .type("logging")
                        .description("Application loggers and their levels")
                        .size(loggingSystem.getLoggerConfigurations().size())
                        .queryable(true)
                        .build()
        );
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        if (!"logging:loggers".equals(dataSourceId)) {
            return QueryResult.builder().dataSourceId(dataSourceId).data(Collections.emptyList()).build();
        }

        List<LoggerConfiguration> loggers = loggingSystem.getLoggerConfigurations();
        
        List<Map<String, Object>> data = loggers.stream()
                .sorted(Comparator.comparing(LoggerConfiguration::getName))
                .map(logger -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", logger.getName());
                    map.put("configuredLevel", logger.getConfiguredLevel() != null ? logger.getConfiguredLevel().name() : null);
                    map.put("effectiveLevel", logger.getEffectiveLevel() != null ? logger.getEffectiveLevel().name() : null);
                    return map;
                })
                .collect(Collectors.toList());

        // Apply filters
        if (filters != null && !filters.isEmpty()) {
            data = data.stream().filter(row -> {
                for (Map.Entry<String, Object> filter : filters.entrySet()) {
                    String key = filter.getKey();
                    String value = String.valueOf(filter.getValue()).toLowerCase();
                    Object rowValue = row.get(key);
                    if (rowValue != null && !rowValue.toString().toLowerCase().contains(value)) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
        }

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
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
        return dataSourceId != null && dataSourceId.startsWith("logging:");
    }

    @Override
    public Object executeAction(String dataSourceId, String action, Map<String, Object> params) {
        if ("setLogLevel".equals(action)) {
            String loggerName = (String) params.get("loggerName");
            String level = (String) params.get("level");
            
            if (loggerName == null || level == null) {
                throw new IllegalArgumentException("loggerName and level are required");
            }

            LogLevel logLevel = "null".equalsIgnoreCase(level) ? null : LogLevel.valueOf(level.toUpperCase());
            loggingSystem.setLogLevel(loggerName, logLevel);
            log.info("Changed log level for {} to {}", loggerName, logLevel);
            
            return Map.of("success", true, "message", "Log level updated");
        }
        throw new UnsupportedOperationException("Unknown action: " + action);
    }
}
