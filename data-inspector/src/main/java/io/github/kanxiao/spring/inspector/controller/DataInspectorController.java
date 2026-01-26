/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.controller;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.service.DataInspectorService;
import io.github.kanxiao.spring.inspector.service.ExportService;
import io.github.kanxiao.spring.inspector.service.TelemetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private final ExportService exportService;
    private final TelemetryService telemetryService;

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
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        telemetryService.trackQuery(id, userId);
        return dataInspectorService.query(id, filters, limit, offset);
    }

    /**
     * Quick query without filters (GET endpoint)
     */
    @GetMapping("/datasources/{id}/query")
    public QueryResult queryGet(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        telemetryService.trackQuery(id, userId);
        return dataInspectorService.query(id, null, limit, offset);
    }

    /**
     * Export data source to various formats (csv, json, excel, html, markdown)
     */
    @GetMapping("/datasources/{id}/export")
    public ResponseEntity<byte[]> export(
            @PathVariable String id,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = "X-User-Id", required = false) String userId) throws IOException {

        telemetryService.trackExport(id, format, userId);
        QueryResult queryResult = dataInspectorService.query(id, null, limit, offset);

        byte[] exportData = switch (format.toLowerCase()) {
            case "json" -> exportService.exportAsJson(queryResult);
            case "csv" -> exportService.exportAsCsv(queryResult);
            case "excel" -> exportService.exportAsExcel(queryResult);
            case "html" -> exportService.exportAsHtml(queryResult);
            case "markdown", "md" -> exportService.exportAsMarkdown(queryResult);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };

        String filename = id.replace(":", "_") + "." + exportService.getFileExtension(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportService.getContentType(format)))
                .body(exportData);
    }

    /**
     * Export data source with filters
     */
    @PostMapping("/datasources/{id}/export")
    public ResponseEntity<byte[]> exportWithFilters(
            @PathVariable String id,
            @RequestParam(defaultValue = "csv") String format,
            @RequestBody(required = false) Map<String, Object> filters,
            @RequestParam(defaultValue = "1000") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestHeader(value = "X-User-Id", required = false) String userId) throws IOException {

        telemetryService.trackExport(id, format, userId);
        QueryResult queryResult = dataInspectorService.query(id, filters, limit, offset);

        byte[] exportData = switch (format.toLowerCase()) {
            case "json" -> exportService.exportAsJson(queryResult);
            case "csv" -> exportService.exportAsCsv(queryResult);
            case "excel" -> exportService.exportAsExcel(queryResult);
            case "html" -> exportService.exportAsHtml(queryResult);
            case "markdown", "md" -> exportService.exportAsMarkdown(queryResult);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };

        String filename = id.replace(":", "_") + "." + exportService.getFileExtension(format);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(exportService.getContentType(format)))
                .body(exportData);
    }

    /**
     * Get usage analytics
     */
    @GetMapping("/analytics")
    public TelemetryService.AnalyticsSummary getAnalytics() {
        return telemetryService.getAnalytics();
    }

    /**
     * Get usage analytics for a specific time period
     */
    @GetMapping("/analytics/period")
    public TelemetryService.AnalyticsSummary getAnalyticsByPeriod(
            @RequestParam(defaultValue = "24") int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        return telemetryService.getAnalytics(start, end);
    }

    /**
     * Reset telemetry data (admin only)
     */
    @PostMapping("/analytics/reset")
    public ResponseEntity<String> resetAnalytics() {
        telemetryService.reset();
        return ResponseEntity.ok("Analytics data reset successfully");
    }
}
