/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.service;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core service that aggregates all data sources and handles queries
 */
@Slf4j
public class DataInspectorService {

    private final List<DataSourceProvider> providers;

    public DataInspectorService(List<DataSourceProvider> providers) {
        this.providers = providers;
        log.info("Data Inspector initialized with {} provider(s)", providers.size());
    }

    /**
     * Get all available data sources from all providers
     */
    public List<DataSourceInfo> getAllDataSources() {
        return providers.stream()
                .flatMap(provider -> provider.discoverDataSources().stream())
                .collect(Collectors.toList());
    }

    /**
     * Query a specific data source
     */
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        DataSourceProvider provider = providers.stream()
                .filter(p -> p.supports(dataSourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for data source: " + dataSourceId));

        return provider.query(dataSourceId, filters, limit, offset);
    }

    public Object executeAction(String dataSourceId, String action, Map<String, Object> params) {
        DataSourceProvider provider = providers.stream()
                .filter(p -> p.supports(dataSourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No provider found for data source: " + dataSourceId));

        return provider.executeAction(dataSourceId, action, params);
    }

    /**
     * Get a specific data source by ID
     */
    public DataSourceInfo getDataSource(String dataSourceId) {
        return getAllDataSources().stream()
                .filter(ds -> ds.getId().equals(dataSourceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Data source not found: " + dataSourceId));
    }
}
