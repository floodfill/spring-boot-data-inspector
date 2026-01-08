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

            // Register session cache
            sessionCache.put("session-1", "user-alice");
            sessionCache.put("session-2", "user-bob");
            sessionCache.put("session-3", "user-charlie");

            registry.register(
                    "session-cache",
                    "Active Sessions",
                    "Currently active user sessions",
                    () -> sessionCache.entrySet().stream()
                            .map(e -> Map.of("sessionId", e.getKey(), "userId", e.getValue()))
                            .toList()
            );

            // Register request queue
            requestQueue.offer("/api/users - GET");
            requestQueue.offer("/api/products - GET");
            requestQueue.offer("/api/users/1 - GET");

            registry.register(
                    "request-queue",
                    "Request Queue",
                    "Pending requests in the processing queue",
                    () -> requestQueue.stream()
                            .map(req -> Map.of("request", req, "timestamp", System.currentTimeMillis()))
                            .toList()
            );

            log.info("Registered {} custom data sources", 4);
        };
    }
}
