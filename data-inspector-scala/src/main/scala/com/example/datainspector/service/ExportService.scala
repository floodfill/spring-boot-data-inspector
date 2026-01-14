/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.service

import com.example.datainspector.model.QueryResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.stereotype.Service

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Date
import scala.jdk.CollectionConverters._

/**
 * Service for exporting data in various formats (CSV, JSON, Excel, HTML, Markdown)
 */
@Service
class ExportService(objectMapper: ObjectMapper) {

  objectMapper.registerModule(DefaultScalaModule)

  /**
   * Export data as JSON
   */
  def exportAsJson(queryResult: QueryResult): Array[Byte] = {
    val export = Map(
      "dataSourceId" -> queryResult.getDataSourceId,
      "totalCount" -> queryResult.getTotalCount,
      "exportedAt" -> new Date().toString,
      "data" -> queryResult.getData.asScala.toList
    ) ++ Option(queryResult.getStats)
      .filter(!_.isEmpty)
      .map(stats => Map("stats" -> stats))
      .getOrElse(Map.empty)

    objectMapper.writerWithDefaultPrettyPrinter()
      .writeValueAsBytes(export)
  }

  /**
   * Export data as CSV
   */
  def exportAsCsv(queryResult: QueryResult): Array[Byte] = {
    val data = queryResult.getData.asScala.toList

    if (data.isEmpty) {
      return "No data to export".getBytes(StandardCharsets.UTF_8)
    }

    val output = new ByteArrayOutputStream()
    val writer = new java.io.OutputStreamWriter(output, StandardCharsets.UTF_8)

    // Get all unique column names
    val columns = data.flatMap(_.asScala.keys).distinct

    // Write header
    writer.write(columns.map(escapeCsv).mkString(","))
    writer.write("\n")

    // Write data rows
    data.foreach { row =>
      val rowData = columns.map { col =>
        Option(row.get(col)).map(_.toString).getOrElse("")
      }
      writer.write(rowData.map(escapeCsv).mkString(","))
      writer.write("\n")
    }

    writer.flush()
    output.toByteArray
  }

  /**
   * Export data as Excel-compatible CSV (with BOM)
   */
  def exportAsExcel(queryResult: QueryResult): Array[Byte] = {
    val output = new ByteArrayOutputStream()

    // Add UTF-8 BOM for Excel compatibility
    output.write(0xEF)
    output.write(0xBB)
    output.write(0xBF)

    // Write CSV data
    val csvData = exportAsCsv(queryResult)
    output.write(csvData)

    output.toByteArray
  }

  /**
   * Export data as HTML table
   */
  def exportAsHtml(queryResult: QueryResult): Array[Byte] = {
    val data = queryResult.getData.asScala.toList

    if (data.isEmpty) {
      return "<html><body><p>No data to export</p></body></html>"
        .getBytes(StandardCharsets.UTF_8)
    }

    val html = new StringBuilder()
    html.append("<!DOCTYPE html>\n")
    html.append("<html>\n<head>\n")
    html.append("<meta charset='UTF-8'>\n")
    html.append("<title>Data Inspector Export</title>\n")
    html.append("<style>\n")
    html.append("  body { font-family: Arial, sans-serif; margin: 20px; }\n")
    html.append("  table { border-collapse: collapse; width: 100%; }\n")
    html.append("  th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
    html.append("  th { background-color: #4CAF50; color: white; }\n")
    html.append("  tr:nth-child(even) { background-color: #f2f2f2; }\n")
    html.append("  .metadata { margin-bottom: 20px; color: #666; }\n")
    html.append("</style>\n")
    html.append("</head>\n<body>\n")

    // Metadata
    html.append("<div class='metadata'>\n")
    html.append("<h2>Data Inspector Export</h2>\n")
    html.append(s"<p><strong>Data Source:</strong> ${escapeHtml(queryResult.getDataSourceId)}</p>\n")
    html.append(s"<p><strong>Total Records:</strong> ${queryResult.getTotalCount}</p>\n")
    html.append(s"<p><strong>Exported At:</strong> ${new Date()}</p>\n")
    html.append("</div>\n")

    // Table
    val columns = data.flatMap(_.asScala.keys).distinct

    html.append("<table>\n")
    html.append("<thead>\n<tr>\n")
    columns.foreach { column =>
      html.append(s"  <th>${escapeHtml(column)}</th>\n")
    }
    html.append("</tr>\n</thead>\n")

    html.append("<tbody>\n")
    data.foreach { row =>
      html.append("<tr>\n")
      columns.foreach { column =>
        val value = Option(row.get(column)).map(_.toString).getOrElse("")
        html.append(s"  <td>${escapeHtml(value)}</td>\n")
      }
      html.append("</tr>\n")
    }
    html.append("</tbody>\n")
    html.append("</table>\n")

    html.append("</body>\n</html>")

    html.toString().getBytes(StandardCharsets.UTF_8)
  }

  /**
   * Export data as Markdown table
   */
  def exportAsMarkdown(queryResult: QueryResult): Array[Byte] = {
    val data = queryResult.getData.asScala.toList

    if (data.isEmpty) {
      return "No data to export".getBytes(StandardCharsets.UTF_8)
    }

    val md = new StringBuilder()

    // Metadata
    md.append("# Data Inspector Export\n\n")
    md.append(s"**Data Source:** ${queryResult.getDataSourceId}\n\n")
    md.append(s"**Total Records:** ${queryResult.getTotalCount}\n\n")
    md.append(s"**Exported At:** ${new Date()}\n\n")
    md.append("---\n\n")

    // Table
    val columns = data.flatMap(_.asScala.keys).distinct

    // Header
    md.append("| ").append(columns.mkString(" | ")).append(" |\n")
    md.append("| ").append(columns.map(_ => "---").mkString(" | ")).append(" |\n")

    // Rows
    data.foreach { row =>
      md.append("| ")
      val rowValues = columns.map { col =>
        Option(row.get(col))
          .map(_.toString.replace("|", "\\|").replace("\n", " "))
          .getOrElse("")
      }
      md.append(rowValues.mkString(" | "))
      md.append(" |\n")
    }

    md.toString().getBytes(StandardCharsets.UTF_8)
  }

  /**
   * Get appropriate content type for format
   */
  def getContentType(format: String): String = format.toLowerCase match {
    case "json" => "application/json"
    case "csv" => "text/csv"
    case "excel" => "text/csv"
    case "html" => "text/html"
    case "markdown" | "md" => "text/markdown"
    case _ => "application/octet-stream"
  }

  /**
   * Get appropriate file extension for format
   */
  def getFileExtension(format: String): String = format.toLowerCase match {
    case "json" => "json"
    case "csv" | "excel" => "csv"
    case "html" => "html"
    case "markdown" | "md" => "md"
    case _ => "txt"
  }

  private def escapeCsv(value: String): String = {
    if (value == null) return ""
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      "\"" + value.replace("\"", "\"\"") + "\""
    } else {
      value
    }
  }

  private def escapeHtml(value: String): String = {
    if (value == null) return ""
    value.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")
  }
}
