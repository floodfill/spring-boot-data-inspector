/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.model

import scala.beans.BeanProperty

/**
 * Represents a discoverable data source in the application
 */
case class DataSourceInfo(
  @BeanProperty id: String,
  @BeanProperty name: String,
  @BeanProperty `type`: String,
  @BeanProperty description: String,
  @BeanProperty size: Long,
  @BeanProperty metadata: Map[String, AnyRef] = Map.empty,
  @BeanProperty queryable: Boolean = true
)

object DataSourceInfo {
  def apply(id: String, name: String, dataType: String, description: String, size: Long): DataSourceInfo =
    DataSourceInfo(id, name, dataType, description, size, Map.empty, queryable = true)
}
