package com.example.datainspector.controller;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import com.example.datainspector.service.DataInspectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the Data Inspector
 */
@RestController
@RequestMapping("/data-inspector/api")
@RequiredArgsConstructor
public class DataInspectorController {

    private final DataInspectorService dataInspectorService;

    /**
     * Get all available data sources
     */
    @GetMapping("/datasources")
    public List<DataSourceInfo> getDataSources() {
        return dataInspectorService.getAllDataSources();
    }

    /**
     * Get a specific data source
     */
    @GetMapping("/datasources/{id}")
    public DataSourceInfo getDataSource(@PathVariable String id) {
        // Handle URL-encoded IDs (e.g., "cache:users" becomes "cache%3Ausers")
        return dataInspectorService.getDataSource(id);
    }

    /**
     * Query a data source with optional filters and pagination
     */
    @PostMapping("/datasources/{id}/query")
    public QueryResult query(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> filters,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        return dataInspectorService.query(id, filters, limit, offset);
    }

    /**
     * Quick query without filters (GET endpoint)
     */
    @GetMapping("/datasources/{id}/query")
    public QueryResult queryGet(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        return dataInspectorService.query(id, null, limit, offset);
    }
}
