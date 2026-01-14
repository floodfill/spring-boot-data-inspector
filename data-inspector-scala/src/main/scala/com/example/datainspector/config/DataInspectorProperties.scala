/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.config

import org.springframework.boot.context.properties.ConfigurationProperties

import scala.beans.BeanProperty

/**
 * Configuration properties for Data Inspector
 */
@ConfigurationProperties(prefix = "data-inspector")
class DataInspectorProperties {

  /**
   * Enable or disable the Data Inspector
   */
  @BeanProperty var enabled: Boolean = true

  /**
   * Base path for the Data Inspector UI
   */
  @BeanProperty var basePath: String = "/data-inspector"

  /**
   * Enable auto-discovery of caches
   */
  @BeanProperty var discoverCaches: Boolean = true

  /**
   * Enable auto-discovery of Spring beans
   */
  @BeanProperty var discoverBeans: Boolean = true

  /**
   * Enable MongoDB integration
   */
  @BeanProperty var mongodbEnabled: Boolean = true

  /**
   * Enable JPA/SQL integration
   */
  @BeanProperty var jpaEnabled: Boolean = true

  /**
   * Enable JVM metrics monitoring
   */
  @BeanProperty var jvmMetricsEnabled: Boolean = true

  /**
   * Enable HTTP request tracking
   */
  @BeanProperty var trackHttpRequests: Boolean = true

  /**
   * Enable scheduled tasks monitoring
   */
  @BeanProperty var scheduledTasksEnabled: Boolean = true

  /**
   * Enable environment and properties monitoring
   */
  @BeanProperty var environmentEnabled: Boolean = true

  /**
   * Maximum number of HTTP requests to track
   */
  @BeanProperty var maxTrackedHttpRequests: Int = 1000

  /**
   * Mask sensitive property values (password, secret, token, etc.)
   */
  @BeanProperty var maskSensitiveData: Boolean = true

  /**
   * Require authentication to access Data Inspector
   * When true, endpoints are secured using Spring Security if available
   */
  @BeanProperty var requireAuth: Boolean = false

  /**
   * Allowed roles to access Data Inspector (when requireAuth is true)
   */
  @BeanProperty var allowedRoles: Array[String] = Array("ADMIN", "DEVELOPER")

  /**
   * Enable telemetry and usage analytics
   * Helps improve Data Inspector by understanding usage patterns
   */
  @BeanProperty var telemetryEnabled: Boolean = true

  /**
   * Enable export functionality
   */
  @BeanProperty var exportEnabled: Boolean = true

  /**
   * Maximum records to export at once
   */
  @BeanProperty var maxExportRecords: Int = 10000
}
