/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.spi;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;

import java.util.List;
import java.util.Map;

/**
 * SPI for providing data sources to the inspector
 */
public interface DataSourceProvider {

    /**
     * Discover and return all data sources this provider knows about
     */
    List<DataSourceInfo> discoverDataSources();

    /**
     * Query a specific data source with filters
     * @param dataSourceId The ID of the data source
     * @param filters Query filters (field -> value)
     * @param limit Max results to return
     * @param offset Offset for pagination
     */
    QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset);

    /**
     * Check if this provider can handle the given data source
     */
    boolean supports(String dataSourceId);

    /**
     * Execute an action on a data source
     * @param dataSourceId The ID of the data source
     * @param action The action name
     * @param params Action parameters
     * @return Result object
     */
    default Object executeAction(String dataSourceId, String action, Map<String, Object> params) {
        throw new UnsupportedOperationException("Action not supported: " + action);
    }
}
