/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a discoverable data source in the application
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceInfo {
    private String id;
    private String name;
    private String type; // "cache", "bean", "custom", "mongodb", etc.
    private String description;
    private long size;
    private Map<String, Object> metadata;
    private boolean queryable;
}
