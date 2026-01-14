/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.controller

import com.example.datainspector.model.{DataSourceInfo, QueryResult}
import com.example.datainspector.service.{AnalyticsSummary, DataInspectorService, ExportService, TelemetryService}
import org.springframework.http.{HttpHeaders, MediaType, ResponseEntity}
import org.springframework.web.bind.annotation._

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._

/**
 * REST API for the Data Inspector
 */
@RestController
@RequestMapping(Array("/data-inspector/api"))
class DataInspectorController(
  dataInspectorService: DataInspectorService,
  exportService: ExportService,
  telemetryService: TelemetryService
) {

  /**
   * Get all available data sources
   */
  @GetMapping(Array("/datasources"))
  def getDataSources: java.util.List[DataSourceInfo] = {
    dataInspectorService.getAllDataSources()
  }

  /**
   * Get a specific data source
   */
  @GetMapping(Array("/datasources/{id}"))
  def getDataSource(@PathVariable id: String): DataSourceInfo = {
    // Handle URL-encoded IDs (e.g., "cache:users" becomes "cache%3Ausers")
    dataInspectorService.getDataSource(id)
  }

  /**
   * Query a data source with optional filters and pagination
   */
  @PostMapping(Array("/datasources/{id}/query"))
  def query(
    @PathVariable id: String,
    @RequestBody(required = false) filters: java.util.Map[String, AnyRef],
    @RequestParam(defaultValue = "100") limit: Int,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestHeader(value = "X-User-Id", required = false) userId: String
  ): QueryResult = {
    telemetryService.trackQuery(id, userId)
    dataInspectorService.query(id, filters, limit, offset)
  }

  /**
   * Quick query without filters (GET endpoint)
   */
  @GetMapping(Array("/datasources/{id}/query"))
  def queryGet(
    @PathVariable id: String,
    @RequestParam(defaultValue = "100") limit: Int,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestHeader(value = "X-User-Id", required = false) userId: String
  ): QueryResult = {
    telemetryService.trackQuery(id, userId)
    dataInspectorService.query(id, null.asInstanceOf[java.util.Map[String, AnyRef]], limit, offset)
  }

  /**
   * Export data source to various formats (csv, json, excel, html, markdown)
   */
  @GetMapping(Array("/datasources/{id}/export"))
  def export(
    @PathVariable id: String,
    @RequestParam(defaultValue = "csv") format: String,
    @RequestParam(defaultValue = "1000") limit: Int,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestHeader(value = "X-User-Id", required = false) userId: String
  ): ResponseEntity[Array[Byte]] = {
    telemetryService.trackExport(id, format, userId)
    val queryResult = dataInspectorService.query(id, null.asInstanceOf[java.util.Map[String, AnyRef]], limit, offset)

    val exportData = format.toLowerCase match {
      case "json" => exportService.exportAsJson(queryResult)
      case "csv" => exportService.exportAsCsv(queryResult)
      case "excel" => exportService.exportAsExcel(queryResult)
      case "html" => exportService.exportAsHtml(queryResult)
      case "markdown" | "md" => exportService.exportAsMarkdown(queryResult)
      case _ => throw new IllegalArgumentException(s"Unsupported format: $format")
    }

    val filename = id.replace(":", "_") + "." + exportService.getFileExtension(format)

    ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, s"""attachment; filename="$filename"""")
      .contentType(MediaType.parseMediaType(exportService.getContentType(format)))
      .body(exportData)
  }

  /**
   * Export data source with filters
   */
  @PostMapping(Array("/datasources/{id}/export"))
  def exportWithFilters(
    @PathVariable id: String,
    @RequestParam(defaultValue = "csv") format: String,
    @RequestBody(required = false) filters: java.util.Map[String, AnyRef],
    @RequestParam(defaultValue = "1000") limit: Int,
    @RequestParam(defaultValue = "0") offset: Int,
    @RequestHeader(value = "X-User-Id", required = false) userId: String
  ): ResponseEntity[Array[Byte]] = {
    telemetryService.trackExport(id, format, userId)
    val queryResult = dataInspectorService.query(id, filters, limit, offset)

    val exportData = format.toLowerCase match {
      case "json" => exportService.exportAsJson(queryResult)
      case "csv" => exportService.exportAsCsv(queryResult)
      case "excel" => exportService.exportAsExcel(queryResult)
      case "html" => exportService.exportAsHtml(queryResult)
      case "markdown" | "md" => exportService.exportAsMarkdown(queryResult)
      case _ => throw new IllegalArgumentException(s"Unsupported format: $format")
    }

    val filename = id.replace(":", "_") + "." + exportService.getFileExtension(format)

    ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, s"""attachment; filename="$filename"""")
      .contentType(MediaType.parseMediaType(exportService.getContentType(format)))
      .body(exportData)
  }

  /**
   * Get usage analytics
   */
  @GetMapping(Array("/analytics"))
  def getAnalytics: AnalyticsSummary = {
    telemetryService.getAnalytics
  }

  /**
   * Get usage analytics for a specific time period
   */
  @GetMapping(Array("/analytics/period"))
  def getAnalyticsByPeriod(@RequestParam(defaultValue = "24") hours: Int): AnalyticsSummary = {
    val end = Instant.now()
    val start = end.minus(hours, ChronoUnit.HOURS)
    telemetryService.getAnalytics(start, end)
  }

  /**
   * Reset telemetry data (admin only)
   */
  @PostMapping(Array("/analytics/reset"))
  def resetAnalytics(): ResponseEntity[String] = {
    telemetryService.reset()
    ResponseEntity.ok("Analytics data reset successfully")
  }
}
