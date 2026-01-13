/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.service;

import com.example.datainspector.model.QueryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for exporting data in various formats (CSV, JSON, Excel)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ObjectMapper objectMapper;

    /**
     * Export data as JSON
     */
    public byte[] exportAsJson(QueryResult queryResult) throws IOException {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("dataSourceId", queryResult.getDataSourceId());
        export.put("totalCount", queryResult.getTotalCount());
        export.put("exportedAt", new Date().toString());
        export.put("data", queryResult.getData());
        if (queryResult.getStats() != null) {
            export.put("stats", queryResult.getStats());
        }

        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(export);
    }

    /**
     * Export data as CSV
     */
    public byte[] exportAsCsv(QueryResult queryResult) throws IOException {
        List<Map<String, Object>> data = queryResult.getData();
        if (data == null || data.isEmpty()) {
            return "No data to export".getBytes(StandardCharsets.UTF_8);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);

        // Get all unique column names
        Set<String> columns = data.stream()
                .flatMap(row -> row.keySet().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Write header
        writer.write(columns.stream()
                .map(this::escapeCsv)
                .collect(Collectors.joining(",")));
        writer.write("\n");

        // Write data rows
        for (Map<String, Object> row : data) {
            writer.write(columns.stream()
                    .map(col -> {
                        Object value = row.get(col);
                        return escapeCsv(value != null ? value.toString() : "");
                    })
                    .collect(Collectors.joining(",")));
            writer.write("\n");
        }

        writer.flush();
        return baos.toByteArray();
    }

    /**
     * Export data as Excel-compatible CSV (with BOM for Excel)
     */
    public byte[] exportAsExcel(QueryResult queryResult) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Add UTF-8 BOM for Excel compatibility
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        // Write CSV data
        byte[] csvData = exportAsCsv(queryResult);
        baos.write(csvData);

        return baos.toByteArray();
    }

    /**
     * Export data as HTML table
     */
    public byte[] exportAsHtml(QueryResult queryResult) throws IOException {
        List<Map<String, Object>> data = queryResult.getData();
        if (data == null || data.isEmpty()) {
            return "<html><body><p>No data to export</p></body></html>"
                    .getBytes(StandardCharsets.UTF_8);
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Data Inspector Export</title>\n");
        html.append("<style>\n");
        html.append("  body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("  table { border-collapse: collapse; width: 100%; }\n");
        html.append("  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("  th { background-color: #4CAF50; color: white; }\n");
        html.append("  tr:nth-child(even) { background-color: #f2f2f2; }\n");
        html.append("  .metadata { margin-bottom: 20px; color: #666; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // Metadata
        html.append("<div class='metadata'>\n");
        html.append("<h2>Data Inspector Export</h2>\n");
        html.append("<p><strong>Data Source:</strong> ").append(escapeHtml(queryResult.getDataSourceId())).append("</p>\n");
        html.append("<p><strong>Total Records:</strong> ").append(queryResult.getTotalCount()).append("</p>\n");
        html.append("<p><strong>Exported At:</strong> ").append(new Date()).append("</p>\n");
        html.append("</div>\n");

        // Table
        Set<String> columns = data.stream()
                .flatMap(row -> row.keySet().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        html.append("<table>\n");
        html.append("<thead>\n<tr>\n");
        for (String column : columns) {
            html.append("  <th>").append(escapeHtml(column)).append("</th>\n");
        }
        html.append("</tr>\n</thead>\n");

        html.append("<tbody>\n");
        for (Map<String, Object> row : data) {
            html.append("<tr>\n");
            for (String column : columns) {
                Object value = row.get(column);
                String valueStr = value != null ? value.toString() : "";
                html.append("  <td>").append(escapeHtml(valueStr)).append("</td>\n");
            }
            html.append("</tr>\n");
        }
        html.append("</tbody>\n");
        html.append("</table>\n");

        html.append("</body>\n</html>");

        return html.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Export data as Markdown table
     */
    public byte[] exportAsMarkdown(QueryResult queryResult) throws IOException {
        List<Map<String, Object>> data = queryResult.getData();
        if (data == null || data.isEmpty()) {
            return "No data to export".getBytes(StandardCharsets.UTF_8);
        }

        StringBuilder md = new StringBuilder();

        // Metadata
        md.append("# Data Inspector Export\n\n");
        md.append("**Data Source:** ").append(queryResult.getDataSourceId()).append("\n\n");
        md.append("**Total Records:** ").append(queryResult.getTotalCount()).append("\n\n");
        md.append("**Exported At:** ").append(new Date()).append("\n\n");
        md.append("---\n\n");

        // Table
        Set<String> columns = data.stream()
                .flatMap(row -> row.keySet().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Header
        md.append("| ").append(String.join(" | ", columns)).append(" |\n");
        md.append("| ").append(columns.stream().map(c -> "---").collect(Collectors.joining(" | "))).append(" |\n");

        // Rows
        for (Map<String, Object> row : data) {
            md.append("| ");
            md.append(columns.stream()
                    .map(col -> {
                        Object value = row.get(col);
                        String str = value != null ? value.toString() : "";
                        return str.replace("|", "\\|").replace("\n", " ");
                    })
                    .collect(Collectors.joining(" | ")));
            md.append(" |\n");
        }

        return md.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get appropriate content type for format
     */
    public String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "excel" -> "text/csv";
            case "html" -> "text/html";
            case "markdown", "md" -> "text/markdown";
            default -> "application/octet-stream";
        };
    }

    /**
     * Get appropriate file extension for format
     */
    public String getFileExtension(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> "json";
            case "csv", "excel" -> "csv";
            case "html" -> "html";
            case "markdown", "md" -> "md";
            default -> "txt";
        };
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
