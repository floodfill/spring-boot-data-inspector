/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.demo

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Demo application showing Scala Data Inspector usage
 *
 * This demonstrates:
 * - Automatic discovery of JVM metrics
 * - Environment and properties inspection
 * - JPA entity inspection (if JPA is configured)
 * - Export capabilities (CSV, JSON, Excel, HTML, Markdown)
 * - Usage analytics
 *
 * Run this application and visit:
 * - http://localhost:8080/data-inspector/api/datasources - List all data sources
 * - http://localhost:8080/data-inspector/api/datasources/jvm:memory/query - Query JVM memory
 * - http://localhost:8080/data-inspector/api/analytics - View analytics
 */
@SpringBootApplication
class ScalaDemoApplication

object ScalaDemoApplication {
  def main(args: Array[String]): Unit = {
    SpringApplication.run(classOf[ScalaDemoApplication], args: _*)
  }
}
