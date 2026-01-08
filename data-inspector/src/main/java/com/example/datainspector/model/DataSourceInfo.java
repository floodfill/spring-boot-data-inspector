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
