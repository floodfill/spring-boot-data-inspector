/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Result of querying a data source
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private String dataSourceId;
    private List<Map<String, Object>> data;
    private long totalCount;
    private int limit;
    private int offset;
    private Map<String, Object> stats;
}
