/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.demo.config;

import com.example.datainspector.registry.CustomDataSourceRegistry;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example of registering custom data sources with the Data Inspector
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomDataSourceConfig {

    private final CustomDataSourceRegistry registry;
    private final UserService userService;
    private final ProductService productService;

    // Custom in-memory data structures to expose
    private final Map<String, String> sessionCache = new ConcurrentHashMap<>();
    private final Map<Long, String> userIdToName = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> connectionPool = new ConcurrentHashMap<>();
    private final Queue<String> requestQueue = new LinkedList<>();

    @Bean
    public CommandLineRunner registerCustomDataSources() {
        return args -> {
            // Register in-memory user list
            registry.register(
                    "in-memory-users",
                    "In-Memory Users",
                    "Direct access to UserService's in-memory user list",
                    () -> userService.getInMemoryUsers()
            );

            // Register in-memory product list
            registry.register(
                    "in-memory-products",
                    "In-Memory Products",
                    "Direct access to ProductService's in-memory product list",
                    () -> productService.getInMemoryProducts()
            );

            // Example 1: Register a simple Map directly
            sessionCache.put("session-1", "user-alice");
            sessionCache.put("session-2", "user-bob");
            sessionCache.put("session-3", "user-charlie");
            sessionCache.put("session-4", "user-diana");

            registry.registerMap(
                    "session-map",
                    "Session Map (Direct)",
                    "Simple ConcurrentHashMap tracking active sessions",
                    sessionCache
            );

            // Example 2: Register a Map with custom transformers
            userIdToName.put(1L, "Alice Smith");
            userIdToName.put(2L, "Bob Jones");
            userIdToName.put(3L, "Charlie Brown");

            registry.registerMap(
                    "user-id-map",
                    "User ID to Name Map",
                    "Map of user IDs to names with custom formatting",
                    userIdToName,
                    id -> "User#" + id,
                    name -> Map.of("fullName", name, "length", name.length())
            );

            // Example 3: Register a complex Map structure
            connectionPool.put("conn-1", Map.of(
                    "host", "db-primary.example.com",
                    "port", 5432,
                    "status", "ACTIVE",
                    "lastUsed", System.currentTimeMillis()
            ));
            connectionPool.put("conn-2", Map.of(
                    "host", "db-replica.example.com",
                    "port", 5432,
                    "status", "IDLE",
                    "lastUsed", System.currentTimeMillis() - 60000
            ));

            registry.registerMapSupplier(
                    "connection-pool",
                    "Database Connection Pool",
                    "Active database connections with status",
                    () -> connectionPool
            );

            // Example 4: Old style with Collection for backward compatibility
            registry.register(
                    "session-cache-old",
                    "Active Sessions (Legacy)",
                    "Currently active user sessions (old registration style)",
                    () -> sessionCache.entrySet().stream()
                            .map(e -> Map.of("sessionId", e.getKey(), "userId", e.getValue()))
                            .toList()
            );

            // Register request queue
            requestQueue.offer("/api/users - GET");
            requestQueue.offer("/api/products - GET");
            requestQueue.offer("/api/users/1 - GET");
            requestQueue.offer("/api/cache/clear - POST");

            registry.register(
                    "request-queue",
                    "Request Queue",
                    "Pending requests in the processing queue",
                    () -> requestQueue.stream()
                            .map(req -> Map.of("request", req, "timestamp", System.currentTimeMillis()))
                            .toList()
            );

            log.info("Registered {} custom data sources with various Map examples", 7);
        };
    }
}
