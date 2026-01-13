/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.provider;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JvmMetricsProvider
 */
class JvmMetricsProviderTest {

    private JvmMetricsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JvmMetricsProvider();
    }

    @Test
    void testDiscoverDataSources() {
        List<DataSourceInfo> dataSources = provider.discoverDataSources();

        assertNotNull(dataSources);
        assertFalse(dataSources.isEmpty());
        assertTrue(dataSources.size() >= 5, "Should discover at least 5 JVM data sources");

        // Verify expected data sources exist
        assertTrue(dataSources.stream().anyMatch(ds -> ds.getId().equals("jvm:memory")));
        assertTrue(dataSources.stream().anyMatch(ds -> ds.getId().equals("jvm:threads")));
        assertTrue(dataSources.stream().anyMatch(ds -> ds.getId().equals("jvm:runtime")));
        assertTrue(dataSources.stream().anyMatch(ds -> ds.getId().equals("jvm:gc")));
        assertTrue(dataSources.stream().anyMatch(ds -> ds.getId().equals("jvm:system")));
    }

    @Test
    void testQueryMemory() {
        QueryResult result = provider.query("jvm:memory", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("jvm:memory", result.getDataSourceId());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());

        Map<String, Object> memoryData = result.getData().get(0);
        assertTrue(memoryData.containsKey("heapUsed"));
        assertTrue(memoryData.containsKey("heapMax"));
        assertTrue(memoryData.containsKey("heapUsagePercent"));
        assertTrue(memoryData.containsKey("nonHeapUsed"));
    }

    @Test
    void testQueryThreads() {
        QueryResult result = provider.query("jvm:threads", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("jvm:threads", result.getDataSourceId());
        assertNotNull(result.getData());
        assertNotNull(result.getStats());

        Map<String, Object> stats = result.getStats();
        assertTrue(stats.containsKey("totalThreads"));
        assertTrue(stats.containsKey("peakThreads"));
        assertTrue(stats.containsKey("daemonThreads"));
        assertTrue((Integer) stats.get("totalThreads") > 0);
    }

    @Test
    void testQueryRuntime() {
        QueryResult result = provider.query("jvm:runtime", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("jvm:runtime", result.getDataSourceId());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());

        Map<String, Object> runtimeData = result.getData().get(0);
        assertTrue(runtimeData.containsKey("uptime"));
        assertTrue(runtimeData.containsKey("vmName"));
        assertTrue(runtimeData.containsKey("vmVersion"));
    }

    @Test
    void testQueryGarbageCollection() {
        QueryResult result = provider.query("jvm:gc", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("jvm:gc", result.getDataSourceId());
        assertNotNull(result.getData());
        // GC beans should exist (at least one)
        assertFalse(result.getData().isEmpty());
    }

    @Test
    void testQuerySystem() {
        QueryResult result = provider.query("jvm:system", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("jvm:system", result.getDataSourceId());
        assertNotNull(result.getData());
        assertFalse(result.getData().isEmpty());

        Map<String, Object> systemData = result.getData().get(0);
        assertTrue(systemData.containsKey("osName"));
        assertTrue(systemData.containsKey("osVersion"));
        assertTrue(systemData.containsKey("availableProcessors"));
    }

    @Test
    void testSupports() {
        assertTrue(provider.supports("jvm:memory"));
        assertTrue(provider.supports("jvm:threads"));
        assertTrue(provider.supports("jvm:runtime"));
        assertTrue(provider.supports("jvm:gc"));
        assertTrue(provider.supports("jvm:system"));

        assertFalse(provider.supports("cache:something"));
        assertFalse(provider.supports("mongodb:collection:users"));
        assertFalse(provider.supports(null));
    }

    @Test
    void testQueryWithInvalidDataSourceId() {
        QueryResult result = provider.query("invalid:datasource", new HashMap<>(), 10, 0);

        assertNotNull(result);
        assertEquals("invalid:datasource", result.getDataSourceId());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
        assertEquals(0, result.getTotalCount());
    }

    @Test
    void testThreadsPagination() {
        // Query first page
        QueryResult result1 = provider.query("jvm:threads", new HashMap<>(), 5, 0);
        assertNotNull(result1);
        assertTrue(result1.getData().size() <= 5);

        // Query second page
        QueryResult result2 = provider.query("jvm:threads", new HashMap<>(), 5, 5);
        assertNotNull(result2);
        assertTrue(result2.getData().size() <= 5);

        // If we have more than 5 threads, pages should be different
        if (result1.getTotalCount() > 5) {
            assertNotEquals(result1.getData(), result2.getData());
        }
    }
}
