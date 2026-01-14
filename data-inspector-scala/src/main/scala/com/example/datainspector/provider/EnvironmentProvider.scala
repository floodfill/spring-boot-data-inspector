/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.provider

import com.example.datainspector.model.{DataSourceInfo, QueryResult}
import com.example.datainspector.spi.DataSourceProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.core.env.{ConfigurableEnvironment, EnumerablePropertySource, Environment, PropertySource}
import org.springframework.stereotype.Component

import scala.jdk.CollectionConverters._

/**
 * Provides access to application properties and environment variables
 * Useful for debugging configuration issues in production
 */
@Component
class EnvironmentProvider(environment: Environment) extends DataSourceProvider {

  private val logger: Logger = LoggerFactory.getLogger(classOf[EnvironmentProvider])

  private val SENSITIVE_KEYS = Set(
    "password", "secret", "key", "token", "credential", "auth",
    "api_key", "apikey", "private", "cert", "pwd", "pass"
  )

  logger.info("Environment provider enabled")

  override def discoverDataSources(): List[DataSourceInfo] = {
    List(
      DataSourceInfo(
        id = "env:properties",
        name = "Application Properties",
        `type` = "environment",
        description = "All application properties from all property sources",
        size = getAllProperties.size,
        queryable = true
      ),
      DataSourceInfo(
        id = "env:system",
        name = "System Properties",
        `type` = "environment",
        description = "JVM system properties",
        size = System.getProperties.size(),
        queryable = true
      ),
      DataSourceInfo(
        id = "env:variables",
        name = "Environment Variables",
        `type` = "environment",
        description = "Operating system environment variables",
        size = System.getenv().size(),
        queryable = true
      ),
      DataSourceInfo(
        id = "env:profiles",
        name = "Active Profiles",
        `type` = "environment",
        description = "Spring active profiles and default profiles",
        size = environment.getActiveProfiles.length + environment.getDefaultProfiles.length,
        queryable = true
      )
    )
  }

  override def query(dataSourceId: String, filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    dataSourceId match {
      case "env:properties" => queryProperties(filters, limit, offset)
      case "env:system" => querySystemProperties(filters, limit, offset)
      case "env:variables" => queryEnvironmentVariables(filters, limit, offset)
      case "env:profiles" => queryProfiles()
      case _ => QueryResult(
        dataSourceId = dataSourceId,
        data = List.empty,
        totalCount = 0,
        limit = limit,
        offset = offset
      )
    }
  }

  override def supports(dataSourceId: String): Boolean = {
    dataSourceId != null && dataSourceId.startsWith("env:")
  }

  private def queryProperties(filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    val allProperties = getAllProperties

    var data = allProperties.toSeq
      .sortBy(_._1)
      .map { case (key, value) =>
        Map[String, AnyRef](
          "key" -> key,
          "value" -> maskSensitiveValue(key, value),
          "source" -> getPropertySource(key)
        )
      }
      .toList

    // Apply filters
    data = applyFilters(data, filters)

    val total = data.size
    val paginated = data.slice(offset, offset + limit)

    QueryResult(
      dataSourceId = "env:properties",
      data = paginated,
      totalCount = total,
      limit = limit,
      offset = offset
    )
  }

  private def querySystemProperties(filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    var data = System.getProperties.asScala.toSeq
      .sortBy(_._1.toString)
      .map { case (key, value) =>
        Map[String, AnyRef](
          "key" -> key.toString,
          "value" -> maskSensitiveValue(key.toString, value)
        )
      }
      .toList

    // Apply filters
    data = applyFilters(data, filters)

    val total = data.size
    val paginated = data.slice(offset, offset + limit)

    QueryResult(
      dataSourceId = "env:system",
      data = paginated,
      totalCount = total,
      limit = limit,
      offset = offset
    )
  }

  private def queryEnvironmentVariables(filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    var data = System.getenv().asScala.toSeq
      .sortBy(_._1)
      .map { case (key, value) =>
        Map[String, AnyRef](
          "key" -> key,
          "value" -> maskSensitiveValue(key, value)
        )
      }
      .toList

    // Apply filters
    data = applyFilters(data, filters)

    val total = data.size
    val paginated = data.slice(offset, offset + limit)

    QueryResult(
      dataSourceId = "env:variables",
      data = paginated,
      totalCount = total,
      limit = limit,
      offset = offset
    )
  }

  private def queryProfiles(): QueryResult = {
    val data = List(
      Map[String, AnyRef](
        "type" -> "Active Profiles",
        "profiles" -> environment.getActiveProfiles.mkString("[", ", ", "]"),
        "count" -> Int.box(environment.getActiveProfiles.length)
      ),
      Map[String, AnyRef](
        "type" -> "Default Profiles",
        "profiles" -> environment.getDefaultProfiles.mkString("[", ", ", "]"),
        "count" -> Int.box(environment.getDefaultProfiles.length)
      )
    )

    QueryResult(
      dataSourceId = "env:profiles",
      data = data,
      totalCount = data.size,
      limit = data.size,
      offset = 0
    )
  }

  private def getAllProperties: Map[String, AnyRef] = {
    environment match {
      case configurableEnv: ConfigurableEnvironment =>
        val properties = scala.collection.mutable.LinkedHashMap[String, AnyRef]()

        configurableEnv.getPropertySources.iterator().asScala.foreach {
          case enumerableSource: EnumerablePropertySource[_] =>
            enumerableSource.getPropertyNames.foreach { key =>
              if (!properties.contains(key)) {
                properties.put(key, environment.getProperty(key))
              }
            }
          case _ => // Skip non-enumerable sources
        }

        properties.toMap
      case _ => Map.empty
    }
  }

  private def getPropertySource(key: String): String = {
    environment match {
      case configurableEnv: ConfigurableEnvironment =>
        configurableEnv.getPropertySources.iterator().asScala.foreach {
          case enumerableSource: EnumerablePropertySource[_] =>
            if (enumerableSource.getPropertyNames.contains(key)) {
              return enumerableSource.getName
            }
          case _ => // Skip non-enumerable sources
        }
        "unknown"
      case _ => "unknown"
    }
  }

  private def maskSensitiveValue(key: String, value: AnyRef): AnyRef = {
    if (value == null) {
      return null
    }

    val lowerKey = key.toLowerCase
    if (SENSITIVE_KEYS.exists(lowerKey.contains)) {
      "***MASKED***"
    } else {
      value
    }
  }

  private def applyFilters(data: List[Map[String, AnyRef]], filters: Map[String, AnyRef]): List[Map[String, AnyRef]] = {
    if (filters.isEmpty) {
      return data
    }

    data.filter { entry =>
      filters.forall { case (filterKey, filterValue) =>
        entry.get(filterKey) match {
          case Some(entryValue) => entryValue.toString.contains(filterValue.toString)
          case None => false
        }
      }
    }
  }
}
