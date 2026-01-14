/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.config

import org.slf4j.{Logger, LoggerFactory}
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

/**
 * Auto-configuration for Data Inspector
 *
 * To enable: Add this module to your dependencies - it auto-configures!
 * To disable: Set data-inspector.enabled=false in application.properties
 */
@AutoConfiguration
@ComponentScan(Array("com.example.datainspector"))
@EnableConfigurationProperties(Array(classOf[DataInspectorProperties]))
@ConditionalOnProperty(name = Array("data-inspector.enabled"), havingValue = "true", matchIfMissing = true)
class DataInspectorAutoConfiguration {

  private val logger: Logger = LoggerFactory.getLogger(classOf[DataInspectorAutoConfiguration])

  logger.info("==================================================")
  logger.info("  Data Inspector Enabled (Scala)")
  logger.info("  Dashboard: http://localhost:8080/data-inspector")
  logger.info("  API: http://localhost:8080/data-inspector/api")
  logger.info("==================================================")
}
