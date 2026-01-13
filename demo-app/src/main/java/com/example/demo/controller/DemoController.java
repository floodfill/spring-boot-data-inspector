/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Demo REST controller that uses caching
 * Access these endpoints to populate the caches, then view them in Data Inspector!
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoController {

    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable String id) {
        return userService.getUserById(id).orElse(null);
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/products/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.getProductById(id).orElse(null);
    }

    @GetMapping("/")
    public String home() {
        return "Demo API is running! Try: /api/users, /api/products, or /data-inspector";
    }
}
