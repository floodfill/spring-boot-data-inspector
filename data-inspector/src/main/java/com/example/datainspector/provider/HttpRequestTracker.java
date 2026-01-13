package com.example.datainspector.provider;

import com.example.datainspector.model.DataSourceInfo;
import com.example.datainspector.model.QueryResult;
import com.example.datainspector.spi.DataSourceProvider;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Tracks HTTP requests for debugging purposes
 * Keeps a rolling buffer of recent requests with timing and status information
 */
@Slf4j
@Component
@ConditionalOnClass(jakarta.servlet.Filter.class)
public class HttpRequestTracker implements DataSourceProvider, Filter {

    private static final int MAX_TRACKED_REQUESTS = 1000;
    private final Deque<RequestInfo> recentRequests = new ConcurrentLinkedDeque<>();
    private final Map<String, RequestInfo> activeRequests = new HashMap<>();

    public HttpRequestTracker() {
        log.info("HTTP Request Tracker enabled");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpRequest) ||
            !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // Skip tracking for data-inspector endpoints to avoid recursion
        String uri = httpRequest.getRequestURI();
        if (uri.contains("/data-inspector")) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = UUID.randomUUID().toString();
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setId(requestId);
        requestInfo.setMethod(httpRequest.getMethod());
        requestInfo.setUri(uri);
        requestInfo.setQueryString(httpRequest.getQueryString());
        requestInfo.setRemoteAddr(httpRequest.getRemoteAddr());
        requestInfo.setUserAgent(httpRequest.getHeader("User-Agent"));
        requestInfo.setStartTime(Instant.now());
        requestInfo.setStatus("IN_PROGRESS");

        synchronized (activeRequests) {
            activeRequests.put(requestId, requestInfo);
        }

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
            requestInfo.setStatusCode(httpResponse.getStatus());
            requestInfo.setStatus("COMPLETED");
        } catch (Exception e) {
            requestInfo.setStatus("ERROR");
            requestInfo.setError(e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            requestInfo.setDurationMs(duration);
            requestInfo.setEndTime(Instant.now());

            synchronized (activeRequests) {
                activeRequests.remove(requestId);
            }

            recentRequests.addFirst(requestInfo);
            if (recentRequests.size() > MAX_TRACKED_REQUESTS) {
                recentRequests.removeLast();
            }
        }
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        List<DataSourceInfo> dataSources = new ArrayList<>();

        dataSources.add(DataSourceInfo.builder()
                .id("http:recent")
                .name("Recent HTTP Requests")
                .type("http-requests")
                .description("Recent HTTP requests with timing and status")
                .size(recentRequests.size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("http:active")
                .name("Active HTTP Requests")
                .type("http-requests")
                .description("Currently in-flight HTTP requests")
                .size(activeRequests.size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("http:stats")
                .name("HTTP Statistics")
                .type("http-stats")
                .description("Aggregated HTTP request statistics")
                .size(1)
                .queryable(true)
                .build());

        return dataSources;
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        return switch (dataSourceId) {
            case "http:recent" -> queryRecentRequests(filters, limit, offset);
            case "http:active" -> queryActiveRequests(filters, limit, offset);
            case "http:stats" -> queryStats();
            default -> QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        };
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("http:");
    }

    private QueryResult queryRecentRequests(Map<String, Object> filters, int limit, int offset) {
        List<Map<String, Object>> data = recentRequests.stream()
                .map(this::requestInfoToMap)
                .collect(Collectors.toList());

        // Apply filters
        data = applyFilters(data, filters);

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("http:recent")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    private QueryResult queryActiveRequests(Map<String, Object> filters, int limit, int offset) {
        List<Map<String, Object>> data;
        synchronized (activeRequests) {
            data = activeRequests.values().stream()
                    .map(this::requestInfoToMap)
                    .collect(Collectors.toList());
        }

        // Apply filters
        data = applyFilters(data, filters);

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId("http:active")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    private QueryResult queryStats() {
        Map<String, Long> methodCounts = recentRequests.stream()
                .collect(Collectors.groupingBy(RequestInfo::getMethod, Collectors.counting()));

        Map<String, Long> statusCounts = recentRequests.stream()
                .filter(r -> r.getStatusCode() != null)
                .collect(Collectors.groupingBy(
                        r -> String.valueOf(r.getStatusCode() / 100) + "xx",
                        Collectors.counting()
                ));

        OptionalDouble avgDuration = recentRequests.stream()
                .mapToLong(RequestInfo::getDurationMs)
                .average();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTracked", recentRequests.size());
        stats.put("activeRequests", activeRequests.size());
        stats.put("averageDurationMs", avgDuration.isPresent() ? String.format("%.2f", avgDuration.getAsDouble()) : "N/A");
        stats.put("methodCounts", methodCounts);
        stats.put("statusCounts", statusCounts);

        // Top slowest requests
        List<Map<String, Object>> slowest = recentRequests.stream()
                .sorted(Comparator.comparingLong(RequestInfo::getDurationMs).reversed())
                .limit(10)
                .map(r -> {
                    Map<String, Object> req = new LinkedHashMap<>();
                    req.put("method", r.getMethod());
                    req.put("uri", r.getUri());
                    req.put("durationMs", r.getDurationMs());
                    return req;
                })
                .collect(Collectors.toList());
        stats.put("slowestRequests", slowest);

        return QueryResult.builder()
                .dataSourceId("http:stats")
                .data(List.of(stats))
                .totalCount(1)
                .limit(1)
                .offset(0)
                .build();
    }

    private Map<String, Object> requestInfoToMap(RequestInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", info.getId());
        map.put("method", info.getMethod());
        map.put("uri", info.getUri());
        map.put("queryString", info.getQueryString());
        map.put("status", info.getStatus());
        map.put("statusCode", info.getStatusCode());
        map.put("durationMs", info.getDurationMs());
        map.put("remoteAddr", info.getRemoteAddr());
        map.put("userAgent", info.getUserAgent());
        map.put("startTime", info.getStartTime() != null ? info.getStartTime().toString() : null);
        map.put("endTime", info.getEndTime() != null ? info.getEndTime().toString() : null);
        map.put("error", info.getError());
        return map;
    }

    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> data, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return data;
        }

        return data.stream()
                .filter(entry -> {
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        Object entryValue = entry.get(filter.getKey());
                        if (entryValue == null || !entryValue.toString().contains(filter.getValue().toString())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Data
    private static class RequestInfo {
        private String id;
        private String method;
        private String uri;
        private String queryString;
        private String remoteAddr;
        private String userAgent;
        private String status;
        private Integer statusCode;
        private long durationMs;
        private Instant startTime;
        private Instant endTime;
        private String error;
    }
}
