/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.service;

import com.example.datainspector.config.DataInspectorProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Telemetry and analytics service for tracking usage patterns
 * Helps understand how Data Inspector is being used and optimize features
 */
@Slf4j
@Service
public class TelemetryService {

    private final DataInspectorProperties properties;
    private final boolean enabled;

    // Usage counters
    private final Map<String, AtomicLong> dataSourceAccessCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> exportFormatCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> dailyActiveUsers = new ConcurrentHashMap<>();
    private final List<UsageEvent> recentEvents = Collections.synchronizedList(new ArrayList<>());

    // System metrics
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong totalExports = new AtomicLong(0);
    private final Instant startTime = Instant.now();

    private static final int MAX_RECENT_EVENTS = 1000;

    public TelemetryService(DataInspectorProperties properties) {
        this.properties = properties;
        this.enabled = properties.isTelemetryEnabled();
        if (enabled) {
            log.info("Telemetry service enabled");
        } else {
            log.info("Telemetry service disabled");
        }
    }

    /**
     * Track a data source query
     */
    public void trackQuery(String dataSourceId, String userId) {
        if (!enabled) return;

        try {
            totalQueries.incrementAndGet();
            dataSourceAccessCount.computeIfAbsent(dataSourceId, k -> new AtomicLong(0)).incrementAndGet();
            trackDailyActiveUser(userId);

            UsageEvent event = new UsageEvent();
            event.setType("QUERY");
            event.setDataSourceId(dataSourceId);
            event.setUserId(userId);
            event.setTimestamp(Instant.now());
            addEvent(event);
        } catch (Exception e) {
            log.debug("Error tracking query: {}", e.getMessage());
        }
    }

    /**
     * Track an export operation
     */
    public void trackExport(String dataSourceId, String format, String userId) {
        if (!enabled) return;

        try {
            totalExports.incrementAndGet();
            exportFormatCount.computeIfAbsent(format, k -> new AtomicLong(0)).incrementAndGet();
            trackDailyActiveUser(userId);

            UsageEvent event = new UsageEvent();
            event.setType("EXPORT");
            event.setDataSourceId(dataSourceId);
            event.setFormat(format);
            event.setUserId(userId);
            event.setTimestamp(Instant.now());
            addEvent(event);
        } catch (Exception e) {
            log.debug("Error tracking export: {}", e.getMessage());
        }
    }

    /**
     * Track a UI view
     */
    public void trackView(String page, String userId) {
        if (!enabled) return;

        try {
            trackDailyActiveUser(userId);

            UsageEvent event = new UsageEvent();
            event.setType("VIEW");
            event.setPage(page);
            event.setUserId(userId);
            event.setTimestamp(Instant.now());
            addEvent(event);
        } catch (Exception e) {
            log.debug("Error tracking view: {}", e.getMessage());
        }
    }

    /**
     * Track an error
     */
    public void trackError(String errorType, String message, String userId) {
        if (!enabled) return;

        try {
            UsageEvent event = new UsageEvent();
            event.setType("ERROR");
            event.setErrorType(errorType);
            event.setMessage(message);
            event.setUserId(userId);
            event.setTimestamp(Instant.now());
            addEvent(event);
        } catch (Exception e) {
            log.debug("Error tracking error: {}", e.getMessage());
        }
    }

    /**
     * Get analytics summary
     */
    public AnalyticsSummary getAnalytics() {
        AnalyticsSummary summary = new AnalyticsSummary();

        // Overall stats
        summary.setTotalQueries(totalQueries.get());
        summary.setTotalExports(totalExports.get());
        summary.setUptimeSeconds(Instant.now().getEpochSecond() - startTime.getEpochSecond());

        // Top data sources
        summary.setTopDataSources(dataSourceAccessCount.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (a, b) -> a,
                        LinkedHashMap::new
                )));

        // Export formats
        summary.setExportFormats(exportFormatCount.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                )));

        // Daily active users
        summary.setDailyActiveUsers(dailyActiveUsers.size());

        // Recent events
        synchronized (recentEvents) {
            summary.setRecentEvents(new ArrayList<>(recentEvents).stream()
                    .limit(100)
                    .collect(Collectors.toList()));
        }

        // Event counts by type
        Map<String, Long> eventCounts = new HashMap<>();
        synchronized (recentEvents) {
            eventCounts = recentEvents.stream()
                    .collect(Collectors.groupingBy(UsageEvent::getType, Collectors.counting()));
        }
        summary.setEventCountsByType(eventCounts);

        return summary;
    }

    /**
     * Get analytics for a specific time period
     */
    public AnalyticsSummary getAnalytics(Instant start, Instant end) {
        AnalyticsSummary summary = new AnalyticsSummary();

        List<UsageEvent> filteredEvents;
        synchronized (recentEvents) {
            filteredEvents = recentEvents.stream()
                    .filter(e -> e.getTimestamp().isAfter(start) && e.getTimestamp().isBefore(end))
                    .collect(Collectors.toList());
        }

        // Queries in period
        long queries = filteredEvents.stream()
                .filter(e -> "QUERY".equals(e.getType()))
                .count();
        summary.setTotalQueries(queries);

        // Exports in period
        long exports = filteredEvents.stream()
                .filter(e -> "EXPORT".equals(e.getType()))
                .count();
        summary.setTotalExports(exports);

        // Top data sources in period
        Map<String, Long> dataSourceCounts = filteredEvents.stream()
                .filter(e -> e.getDataSourceId() != null)
                .collect(Collectors.groupingBy(UsageEvent::getDataSourceId, Collectors.counting()));

        summary.setTopDataSources(dataSourceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                )));

        summary.setRecentEvents(filteredEvents);

        return summary;
    }

    /**
     * Reset all telemetry data
     */
    public void reset() {
        if (!enabled) return;

        dataSourceAccessCount.clear();
        exportFormatCount.clear();
        dailyActiveUsers.clear();
        synchronized (recentEvents) {
            recentEvents.clear();
        }
        totalQueries.set(0);
        totalExports.set(0);

        log.info("Telemetry data reset");
    }

    private void trackDailyActiveUser(String userId) {
        if (userId == null) {
            userId = "anonymous";
        }
        String today = LocalDate.now().toString();
        String key = today + ":" + userId;
        dailyActiveUsers.putIfAbsent(key, new AtomicLong(System.currentTimeMillis()));
    }

    private void addEvent(UsageEvent event) {
        synchronized (recentEvents) {
            recentEvents.add(0, event); // Add to beginning
            if (recentEvents.size() > MAX_RECENT_EVENTS) {
                recentEvents.remove(recentEvents.size() - 1); // Remove oldest
            }
        }
    }

    /**
     * Usage event model
     */
    @Data
    public static class UsageEvent {
        private String type; // QUERY, EXPORT, VIEW, ERROR
        private String dataSourceId;
        private String format;
        private String page;
        private String errorType;
        private String message;
        private String userId;
        private Instant timestamp;
    }

    /**
     * Analytics summary model
     */
    @Data
    public static class AnalyticsSummary {
        private long totalQueries;
        private long totalExports;
        private long uptimeSeconds;
        private Map<String, Long> topDataSources;
        private Map<String, Long> exportFormats;
        private int dailyActiveUsers;
        private List<UsageEvent> recentEvents;
        private Map<String, Long> eventCountsByType;
    }
}
