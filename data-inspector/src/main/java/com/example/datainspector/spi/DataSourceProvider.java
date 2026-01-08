package com.example.datainspector.spi;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;

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
}
