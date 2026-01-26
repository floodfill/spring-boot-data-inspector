/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-discovers Spring caches
 */
@Slf4j
@Component
public class CacheDataSourceProvider implements DataSourceProvider {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public CacheDataSourceProvider(Optional<CacheManager> cacheManager, ObjectMapper objectMapper) {
        this.cacheManager = cacheManager.orElse(null);
        this.objectMapper = objectMapper;
        if (cacheManager.isPresent()) {
            log.info("Cache data source provider enabled");
        }
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        if (cacheManager == null) {
            return Collections.emptyList();
        }

        return cacheManager.getCacheNames().stream()
                .map(cacheName -> {
                    Cache cache = cacheManager.getCache(cacheName);
                    return DataSourceInfo.builder()
                            .id("cache:" + cacheName)
                            .name(cacheName)
                            .type("cache")
                            .description("Spring Cache: " + cacheName)
                            .size(getCacheSize(cache))
                            .queryable(true)
                            .metadata(Map.of(
                                    "cacheType", cache.getClass().getSimpleName(),
                                    "nativeCache", cache.getNativeCache().getClass().getName()
                            ))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        String cacheName = dataSourceId.substring("cache:".length());
        Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            return QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }

        List<Map<String, Object>> entries = new ArrayList<>();
        Object nativeCache = cache.getNativeCache();

        // Handle Caffeine cache
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache =
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) nativeCache;

            caffeineCache.asMap().forEach((key, value) -> {
                entries.add(convertToMap(key, value));
            });
        }
        // Handle ConcurrentMapCache
        else if (nativeCache instanceof java.util.concurrent.ConcurrentMap) {
            java.util.concurrent.ConcurrentMap<Object, Object> map =
                (java.util.concurrent.ConcurrentMap<Object, Object>) nativeCache;

            map.forEach((key, value) -> {
                entries.add(convertToMap(key, value));
            });
        }

        // Apply filters
        List<Map<String, Object>> filtered = applyFilters(entries, filters);

        // Apply pagination
        int total = filtered.size();
        List<Map<String, Object>> paginated = filtered.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        return QueryResult.builder()
                .dataSourceId(dataSourceId)
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .stats(Map.of("cacheSize", entries.size()))
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("cache:");
    }

    private long getCacheSize(Cache cache) {
        try {
            Object nativeCache = cache.getNativeCache();
            if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                return ((com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache).estimatedSize();
            } else if (nativeCache instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) nativeCache).size();
            }
        } catch (Exception e) {
            log.debug("Could not determine cache size", e);
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object key, Object value) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("keyType", key != null ? key.getClass().getSimpleName() : "null");

        if (value != null) {
            entry.put("valueType", value.getClass().getSimpleName());

            // Try to convert value to map for better inspection
            Map<String, Object> valueMap = objectMapper.convertValue(value, Map.class);
            entry.put("value", valueMap);
        } else {
            entry.put("value", null);
            entry.put("valueType", "null");
        }

        return entry;
    }

    private List<Map<String, Object>> applyFilters(List<Map<String, Object>> entries, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return entries;
        }

        return entries.stream()
                .filter(entry -> {
                    for (Map.Entry<String, Object> filter : filters.entrySet()) {
                        Object entryValue = entry.get(filter.getKey());
                        if (!Objects.equals(entryValue, filter.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
}
