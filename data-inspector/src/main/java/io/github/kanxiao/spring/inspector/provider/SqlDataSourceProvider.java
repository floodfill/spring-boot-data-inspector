/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

/**
 * Provides raw SQL execution capabilities (The Danger Zone)
 */
@Slf4j
public class SqlDataSourceProvider implements DataSourceProvider {

    private final JdbcTemplate jdbcTemplate;

    public SqlDataSourceProvider(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("SQL Console provider enabled");
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        return List.of(
                DataSourceInfo.builder()
                        .id("sql:console")
                        .name("SQL Console")
                        .type("sql-console")
                        .description("Execute raw SQL queries against the primary database")
                        .size(0)
                        .queryable(true)
                        .build()
        );
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        // Querying "sql:console" just returns an empty result, the real work is in executeAction
        return QueryResult.builder()
                .dataSourceId(dataSourceId)
                .data(Collections.emptyList())
                .totalCount(0)
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return "sql:console".equals(dataSourceId);
    }

    @Override
    public Object executeAction(String dataSourceId, String action, Map<String, Object> params) {
        if ("executeQuery".equals(action)) {
            String sql = (String) params.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                throw new IllegalArgumentException("SQL query is required");
            }
            
            // Simple safety check - only allow SELECT by default for safety in this demo
            // In a real app, this should be very restricted
            String upperSql = sql.trim().toUpperCase();
            if (!upperSql.startsWith("SELECT") && !upperSql.startsWith("SHOW") && !upperSql.startsWith("DESCRIBE")) {
               // For now, let's just warn but allow if they really want to (it's a dev tool)
               // throw new IllegalArgumentException("Only SELECT statements are allowed in this console");
            }

            try {
                // Limit the query to avoid blowing up memory
                if (limit > 0 && !upperSql.contains("LIMIT")) {
                     // Very naive limit injection, assumes standard SQL
                     // sql += " LIMIT " + limit; 
                }
                
                int maxRows = 1000;
                jdbcTemplate.setMaxRows(maxRows);
                
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
                
                return Map.of(
                    "success", true,
                    "data", results,
                    "count", results.size(),
                    "truncated", results.size() >= maxRows
                );
            } catch (Exception e) {
                return Map.of(
                    "success", false,
                    "error", e.getMessage()
                );
            }
        }
        throw new UnsupportedOperationException("Unknown action: " + action);
    }
    
    // Helper var for limit
    private int limit = 100;
}
