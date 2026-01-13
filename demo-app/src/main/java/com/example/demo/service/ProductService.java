/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.demo.service;

import com.example.demo.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProductService {

    private final List<Product> products = new ArrayList<>();

    public ProductService() {
        // Initialize with sample data
        products.add(new Product(1L, "Laptop", 999.99, "Electronics", 50));
        products.add(new Product(2L, "Mouse", 29.99, "Electronics", 200));
        products.add(new Product(3L, "Keyboard", 79.99, "Electronics", 150));
        products.add(new Product(4L, "Monitor", 299.99, "Electronics", 75));
        products.add(new Product(5L, "Desk Chair", 199.99, "Furniture", 30));
        log.info("Initialized ProductService with {} products", products.size());
    }

    @Cacheable("products")
    public Optional<Product> getProductById(Long id) {
        log.info("Fetching product from database (cache miss): {}", id);
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst();
    }

    @Cacheable("products")
    public List<Product> getAllProducts() {
        log.info("Fetching all products from database (cache miss)");
        return new ArrayList<>(products);
    }

    public List<Product> getInMemoryProducts() {
        return products;
    }
}
