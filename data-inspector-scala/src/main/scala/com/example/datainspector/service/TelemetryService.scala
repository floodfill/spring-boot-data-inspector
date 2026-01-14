/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.service

import com.example.datainspector.config.DataInspectorProperties
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.stereotype.Service

import java.time.{Instant, LocalDate, ZoneId}
import java.util.concurrent.atomic.AtomicLong
import scala.beans.BeanProperty
import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.jdk.CollectionConverters._

/**
 * Telemetry and analytics service for tracking usage patterns
 * Helps understand how Data Inspector is being used and optimize features
 */
@Service
class TelemetryService(properties: DataInspectorProperties) {

  private val logger: Logger = LoggerFactory.getLogger(classOf[TelemetryService])
  private val enabled: Boolean = properties.getTelemetryEnabled

  // Usage counters
  private val dataSourceAccessCount = new TrieMap[String, AtomicLong]()
  private val exportFormatCount = new TrieMap[String, AtomicLong]()
  private val dailyActiveUsers = new TrieMap[String, AtomicLong]()
  private val recentEvents = mutable.ArrayBuffer[UsageEvent]()

  // System metrics
  private val totalQueries = new AtomicLong(0)
  private val totalExports = new AtomicLong(0)
  private val startTime: Instant = Instant.now()

  private val MAX_RECENT_EVENTS = 1000

  if (enabled) {
    logger.info("Telemetry service enabled")
  } else {
    logger.info("Telemetry service disabled")
  }

  /**
   * Track a data source query
   */
  def trackQuery(dataSourceId: String, userId: String): Unit = {
    if (!enabled) return

    try {
      totalQueries.incrementAndGet()
      dataSourceAccessCount.getOrElseUpdate(dataSourceId, new AtomicLong(0)).incrementAndGet()
      trackDailyActiveUser(userId)

      val event = UsageEvent(
        `type` = "QUERY",
        dataSourceId = Some(dataSourceId),
        userId = Option(userId),
        timestamp = Instant.now()
      )
      addEvent(event)
    } catch {
      case e: Exception => logger.debug(s"Error tracking query: ${e.getMessage}")
    }
  }

  /**
   * Track an export operation
   */
  def trackExport(dataSourceId: String, format: String, userId: String): Unit = {
    if (!enabled) return

    try {
      totalExports.incrementAndGet()
      exportFormatCount.getOrElseUpdate(format, new AtomicLong(0)).incrementAndGet()
      trackDailyActiveUser(userId)

      val event = UsageEvent(
        `type` = "EXPORT",
        dataSourceId = Some(dataSourceId),
        format = Some(format),
        userId = Option(userId),
        timestamp = Instant.now()
      )
      addEvent(event)
    } catch {
      case e: Exception => logger.debug(s"Error tracking export: ${e.getMessage}")
    }
  }

  /**
   * Track a UI view
   */
  def trackView(page: String, userId: String): Unit = {
    if (!enabled) return

    try {
      trackDailyActiveUser(userId)

      val event = UsageEvent(
        `type` = "VIEW",
        page = Some(page),
        userId = Option(userId),
        timestamp = Instant.now()
      )
      addEvent(event)
    } catch {
      case e: Exception => logger.debug(s"Error tracking view: ${e.getMessage}")
    }
  }

  /**
   * Track an error
   */
  def trackError(errorType: String, message: String, userId: String): Unit = {
    if (!enabled) return

    try {
      val event = UsageEvent(
        `type` = "ERROR",
        errorType = Some(errorType),
        message = Some(message),
        userId = Option(userId),
        timestamp = Instant.now()
      )
      addEvent(event)
    } catch {
      case e: Exception => logger.debug(s"Error tracking error: ${e.getMessage}")
    }
  }

  /**
   * Get analytics summary
   */
  def getAnalytics: AnalyticsSummary = {
    // Overall stats
    val uptimeSeconds = Instant.now().getEpochSecond - startTime.getEpochSecond

    // Top data sources
    val topDataSources = dataSourceAccessCount.toSeq
      .sortBy { case (_, count) => -count.get() }
      .take(10)
      .map { case (key, count) => (key, count.get()) }
      .toMap

    // Export formats
    val exportFormats = exportFormatCount.toSeq
      .map { case (key, count) => (key, count.get()) }
      .toMap

    // Recent events (synchronized access)
    val recentEventsCopy = recentEvents.synchronized {
      recentEvents.take(100).toList
    }

    // Event counts by type
    val eventCountsByType = recentEvents.synchronized {
      recentEvents.groupBy(_.`type`).view.mapValues(_.size.toLong).toMap
    }

    AnalyticsSummary(
      totalQueries = totalQueries.get(),
      totalExports = totalExports.get(),
      uptimeSeconds = uptimeSeconds,
      topDataSources = topDataSources,
      exportFormats = exportFormats,
      dailyActiveUsers = dailyActiveUsers.size,
      recentEvents = recentEventsCopy,
      eventCountsByType = eventCountsByType
    )
  }

  /**
   * Get analytics for a specific time period
   */
  def getAnalytics(start: Instant, end: Instant): AnalyticsSummary = {
    val filteredEvents = recentEvents.synchronized {
      recentEvents.filter { e =>
        e.timestamp.isAfter(start) && e.timestamp.isBefore(end)
      }.toList
    }

    // Queries in period
    val queries = filteredEvents.count(_.`type` == "QUERY")

    // Exports in period
    val exports = filteredEvents.count(_.`type` == "EXPORT")

    // Top data sources in period
    val topDataSources = filteredEvents
      .filter(_.dataSourceId.isDefined)
      .groupBy(_.dataSourceId.get)
      .view
      .mapValues(_.size.toLong)
      .toSeq
      .sortBy { case (_, count) => -count }
      .take(10)
      .toMap

    AnalyticsSummary(
      totalQueries = queries,
      totalExports = exports,
      topDataSources = topDataSources,
      recentEvents = filteredEvents
    )
  }

  /**
   * Reset all telemetry data
   */
  def reset(): Unit = {
    if (!enabled) return

    dataSourceAccessCount.clear()
    exportFormatCount.clear()
    dailyActiveUsers.clear()
    recentEvents.synchronized {
      recentEvents.clear()
    }
    totalQueries.set(0)
    totalExports.set(0)

    logger.info("Telemetry data reset")
  }

  private def trackDailyActiveUser(userId: String): Unit = {
    val user = Option(userId).getOrElse("anonymous")
    val today = LocalDate.now().toString
    val key = s"$today:$user"
    dailyActiveUsers.putIfAbsent(key, new AtomicLong(System.currentTimeMillis()))
  }

  private def addEvent(event: UsageEvent): Unit = {
    recentEvents.synchronized {
      recentEvents.prepend(event) // Add to beginning
      if (recentEvents.size > MAX_RECENT_EVENTS) {
        recentEvents.remove(recentEvents.size - 1) // Remove oldest
      }
    }
  }
}

/**
 * Usage event model
 */
case class UsageEvent(
  @BeanProperty `type`: String,
  @BeanProperty dataSourceId: Option[String] = None,
  @BeanProperty format: Option[String] = None,
  @BeanProperty page: Option[String] = None,
  @BeanProperty errorType: Option[String] = None,
  @BeanProperty message: Option[String] = None,
  @BeanProperty userId: Option[String] = None,
  @BeanProperty timestamp: Instant = Instant.now()
) {
  // Java interop getters for Option fields
  def getDataSourceIdOrNull: String = dataSourceId.orNull
  def getFormatOrNull: String = format.orNull
  def getPageOrNull: String = page.orNull
  def getErrorTypeOrNull: String = errorType.orNull
  def getMessageOrNull: String = message.orNull
  def getUserIdOrNull: String = userId.orNull
}

/**
 * Analytics summary model
 */
case class AnalyticsSummary(
  @BeanProperty totalQueries: Long = 0,
  @BeanProperty totalExports: Long = 0,
  @BeanProperty uptimeSeconds: Long = 0,
  @BeanProperty topDataSources: Map[String, Long] = Map.empty,
  @BeanProperty exportFormats: Map[String, Long] = Map.empty,
  @BeanProperty dailyActiveUsers: Int = 0,
  @BeanProperty recentEvents: List[UsageEvent] = List.empty,
  @BeanProperty eventCountsByType: Map[String, Long] = Map.empty
) {
  // Java interop methods
  def getTopDataSourcesAsJava: java.util.Map[String, java.lang.Long] =
    topDataSources.view.mapValues(Long.box).toMap.asJava

  def getExportFormatsAsJava: java.util.Map[String, java.lang.Long] =
    exportFormats.view.mapValues(Long.box).toMap.asJava

  def getRecentEventsAsJava: java.util.List[UsageEvent] =
    recentEvents.asJava

  def getEventCountsByTypeAsJava: java.util.Map[String, java.lang.Long] =
    eventCountsByType.view.mapValues(Long.box).toMap.asJava
}
