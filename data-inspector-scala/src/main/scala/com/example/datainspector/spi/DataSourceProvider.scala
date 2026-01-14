/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.spi

import com.example.datainspector.model.{DataSourceInfo, QueryResult}

import scala.jdk.CollectionConverters._

/**
 * SPI for providing data sources to the inspector
 */
trait DataSourceProvider {

  /**
   * Discover and return all data sources this provider knows about
   */
  def discoverDataSources(): List[DataSourceInfo]

  /**
   * Query a specific data source with filters
   * @param dataSourceId The ID of the data source
   * @param filters Query filters (field -> value)
   * @param limit Max results to return
   * @param offset Offset for pagination
   */
  def query(dataSourceId: String,
            filters: Map[String, AnyRef],
            limit: Int,
            offset: Int): QueryResult

  /**
   * Check if this provider can handle the given data source
   */
  def supports(dataSourceId: String): Boolean
}
