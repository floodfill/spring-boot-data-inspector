/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long id;
    private String name;
    private Double price;
    private String category;
    private Integer stock;
}
