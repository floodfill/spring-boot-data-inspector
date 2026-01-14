/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.model

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

/**
 * Result of querying a data source
 */
case class QueryResult(
  @BeanProperty dataSourceId: String,
  @BeanProperty data: java.util.List[java.util.Map[String, AnyRef]],
  @BeanProperty totalCount: Long,
  @BeanProperty limit: Int = 100,
  @BeanProperty offset: Int = 0,
  @BeanProperty stats: java.util.Map[String, AnyRef] = java.util.Collections.emptyMap()
)

object QueryResult {
  def apply(dataSourceId: String,
            data: List[Map[String, AnyRef]],
            totalCount: Long,
            limit: Int,
            offset: Int): QueryResult = {
    val javaData = data.map(_.asJava).asJava
    QueryResult(dataSourceId, javaData, totalCount, limit, offset,
      java.util.Collections.emptyMap[String, AnyRef]())
  }

  def apply(dataSourceId: String,
            data: List[Map[String, AnyRef]],
            totalCount: Long,
            limit: Int,
            offset: Int,
            stats: Map[String, AnyRef]): QueryResult = {
    val javaData = data.map(_.asJava).asJava
    QueryResult(dataSourceId, javaData, totalCount, limit, offset, stats.asJava)
  }
}
