/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.service

import com.example.datainspector.model.{DataSourceInfo, QueryResult}
import com.example.datainspector.spi.DataSourceProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.stereotype.Service

import scala.jdk.CollectionConverters._

/**
 * Core service that aggregates all data sources and handles queries
 */
@Service
class DataInspectorService(providers: java.util.List[DataSourceProvider]) {

  private val logger: Logger = LoggerFactory.getLogger(classOf[DataInspectorService])
  private val providerList: List[DataSourceProvider] = providers.asScala.toList

  logger.info(s"Data Inspector initialized with ${providerList.size} provider(s)")

  /**
   * Get all available data sources from all providers
   */
  def getAllDataSources(): java.util.List[DataSourceInfo] = {
    providerList.flatMap(_.discoverDataSources()).asJava
  }

  /**
   * Query a specific data source
   */
  def query(dataSourceId: String, filters: java.util.Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    val scalaFilters = Option(filters).map(_.asScala.toMap).getOrElse(Map.empty[String, AnyRef])

    val provider = providerList
      .find(_.supports(dataSourceId))
      .getOrElse(throw new IllegalArgumentException(s"No provider found for data source: $dataSourceId"))

    provider.query(dataSourceId, scalaFilters, limit, offset)
  }

  /**
   * Get a specific data source by ID
   */
  def getDataSource(dataSourceId: String): DataSourceInfo = {
    providerList
      .flatMap(_.discoverDataSources())
      .find(_.id == dataSourceId)
      .getOrElse(throw new IllegalArgumentException(s"Data source not found: $dataSourceId"))
  }
}
