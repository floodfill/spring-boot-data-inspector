/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.provider

import com.example.datainspector.model.{DataSourceInfo, QueryResult}
import com.example.datainspector.spi.DataSourceProvider
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.stereotype.Component

import java.lang.management._
import java.util.Date
import scala.jdk.CollectionConverters._

/**
 * Provides JVM runtime metrics - memory, threads, CPU, garbage collection, etc.
 * Essential for production debugging
 */
@Component
class JvmMetricsProvider extends DataSourceProvider {

  private val logger: Logger = LoggerFactory.getLogger(classOf[JvmMetricsProvider])
  private val memoryMXBean: MemoryMXBean = ManagementFactory.getMemoryMXBean
  private val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean
  private val runtimeMXBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean
  private val gcMXBeans: List[GarbageCollectorMXBean] = ManagementFactory.getGarbageCollectorMXBeans.asScala.toList
  private val osMXBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean
  private val classLoadingMXBean: ClassLoadingMXBean = ManagementFactory.getClassLoadingMXBean

  logger.info("JVM Metrics provider enabled")

  override def discoverDataSources(): List[DataSourceInfo] = {
    List(
      DataSourceInfo(
        id = "jvm:memory",
        name = "Memory Usage",
        `type` = "jvm-memory",
        description = "Heap and non-heap memory usage, garbage collection stats",
        size = 1,
        queryable = true
      ),
      DataSourceInfo(
        id = "jvm:threads",
        name = "Thread Information",
        `type` = "jvm-threads",
        description = "Active threads, thread states, deadlock detection",
        size = threadMXBean.getThreadCount,
        queryable = true
      ),
      DataSourceInfo(
        id = "jvm:runtime",
        name = "Runtime Information",
        `type` = "jvm-runtime",
        description = "JVM uptime, version, system properties",
        size = 1,
        queryable = true
      ),
      DataSourceInfo(
        id = "jvm:gc",
        name = "Garbage Collection",
        `type` = "jvm-gc",
        description = "GC statistics and performance metrics",
        size = gcMXBeans.size,
        queryable = true
      ),
      DataSourceInfo(
        id = "jvm:system",
        name = "System Information",
        `type` = "jvm-system",
        description = "Operating system and CPU information",
        size = 1,
        queryable = true
      )
    )
  }

  override def query(dataSourceId: String, filters: Map[String, AnyRef], limit: Int, offset: Int): QueryResult = {
    dataSourceId match {
      case "jvm:memory" => queryMemory()
      case "jvm:threads" => queryThreads(limit, offset)
      case "jvm:runtime" => queryRuntime()
      case "jvm:gc" => queryGarbageCollection()
      case "jvm:system" => querySystem()
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
    dataSourceId != null && dataSourceId.startsWith("jvm:")
  }

  private def queryMemory(): QueryResult = {
    val heapMemory = memoryMXBean.getHeapMemoryUsage
    val nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage

    val data = Map[String, AnyRef](
      "heapUsed" -> formatBytes(heapMemory.getUsed),
      "heapUsedBytes" -> Long.box(heapMemory.getUsed),
      "heapCommitted" -> formatBytes(heapMemory.getCommitted),
      "heapMax" -> formatBytes(heapMemory.getMax),
      "heapUsagePercent" -> f"${heapMemory.getUsed * 100.0 / heapMemory.getMax}%.2f%%",
      "nonHeapUsed" -> formatBytes(nonHeapMemory.getUsed),
      "nonHeapUsedBytes" -> Long.box(nonHeapMemory.getUsed),
      "nonHeapCommitted" -> formatBytes(nonHeapMemory.getCommitted),
      "nonHeapMax" -> (if (nonHeapMemory.getMax > 0) formatBytes(nonHeapMemory.getMax) else "undefined"),
      "objectsPendingFinalization" -> Int.box(memoryMXBean.getObjectPendingFinalizationCount)
    )

    // Memory pools
    val pools = ManagementFactory.getMemoryPoolMXBeans.asScala.map { pool =>
      val usage = pool.getUsage
      Map[String, AnyRef](
        "name" -> pool.getName,
        "type" -> pool.getType.toString,
        "used" -> formatBytes(usage.getUsed),
        "max" -> (if (usage.getMax > 0) formatBytes(usage.getMax) else "undefined")
      )
    }.toList

    val dataWithPools = data + ("memoryPools" -> pools.asJava)

    QueryResult(
      dataSourceId = "jvm:memory",
      data = List(dataWithPools),
      totalCount = 1,
      limit = 1,
      offset = 0
    )
  }

  private def queryThreads(limit: Int, offset: Int): QueryResult = {
    val threadInfos = threadMXBean.dumpAllThreads(false, false)

    val threads = threadInfos.filter(_ != null).map { threadInfo =>
      val cpuTime = if (threadMXBean.isThreadCpuTimeSupported) {
        (threadMXBean.getThreadCpuTime(threadInfo.getThreadId) / 1_000_000) + "ms"
      } else {
        "N/A"
      }

      Map[String, AnyRef](
        "id" -> Long.box(threadInfo.getThreadId),
        "name" -> threadInfo.getThreadName,
        "state" -> threadInfo.getThreadState.toString,
        "cpuTime" -> cpuTime,
        "blockedTime" -> Long.box(threadInfo.getBlockedTime),
        "blockedCount" -> Long.box(threadInfo.getBlockedCount),
        "waitedTime" -> Long.box(threadInfo.getWaitedTime),
        "waitedCount" -> Long.box(threadInfo.getWaitedCount),
        "suspended" -> Boolean.box(threadInfo.isSuspended),
        "inNative" -> Boolean.box(threadInfo.isInNative)
      )
    }.toList

    // Sort by CPU time if available
    val sortedThreads = threads.sortBy { thread =>
      val cpuTime = thread("cpuTime").asInstanceOf[String]
      if (cpuTime == "N/A") 0L
      else cpuTime.stripSuffix("ms").toLong
    }.reverse

    val total = sortedThreads.size
    val paginated = sortedThreads.slice(offset, offset + limit)

    val stats = Map[String, AnyRef](
      "totalThreads" -> Int.box(threadMXBean.getThreadCount),
      "peakThreads" -> Int.box(threadMXBean.getPeakThreadCount),
      "daemonThreads" -> Int.box(threadMXBean.getDaemonThreadCount),
      "totalStarted" -> Long.box(threadMXBean.getTotalStartedThreadCount),
      "deadlockedThreads" -> {
        val deadlockedThreads = threadMXBean.findDeadlockedThreads()
        Int.box(if (deadlockedThreads != null) deadlockedThreads.length else 0)
      }
    )

    QueryResult(
      dataSourceId = "jvm:threads",
      data = paginated,
      totalCount = total,
      limit = limit,
      offset = offset,
      stats = stats
    )
  }

  private def queryRuntime(): QueryResult = {
    val data = Map[String, AnyRef](
      "uptime" -> formatDuration(runtimeMXBean.getUptime),
      "uptimeMs" -> Long.box(runtimeMXBean.getUptime),
      "startTime" -> new Date(runtimeMXBean.getStartTime).toString,
      "vmName" -> runtimeMXBean.getVmName,
      "vmVendor" -> runtimeMXBean.getVmVendor,
      "vmVersion" -> runtimeMXBean.getVmVersion,
      "specName" -> runtimeMXBean.getSpecName,
      "specVendor" -> runtimeMXBean.getSpecVendor,
      "specVersion" -> runtimeMXBean.getSpecVersion,
      "managementSpecVersion" -> runtimeMXBean.getManagementSpecVersion,
      "classPath" -> runtimeMXBean.getClassPath,
      "libraryPath" -> runtimeMXBean.getLibraryPath,
      "inputArguments" -> runtimeMXBean.getInputArguments
    )

    QueryResult(
      dataSourceId = "jvm:runtime",
      data = List(data),
      totalCount = 1,
      limit = 1,
      offset = 0
    )
  }

  private def queryGarbageCollection(): QueryResult = {
    val gcData = gcMXBeans.map { gcBean =>
      Map[String, AnyRef](
        "name" -> gcBean.getName,
        "collectionCount" -> Long.box(gcBean.getCollectionCount),
        "collectionTime" -> (gcBean.getCollectionTime + "ms"),
        "memoryPoolNames" -> gcBean.getMemoryPoolNames.mkString(", ")
      )
    }

    QueryResult(
      dataSourceId = "jvm:gc",
      data = gcData,
      totalCount = gcData.size,
      limit = gcData.size,
      offset = 0
    )
  }

  private def querySystem(): QueryResult = {
    val baseData = Map[String, AnyRef](
      "osName" -> osMXBean.getName,
      "osVersion" -> osMXBean.getVersion,
      "osArch" -> osMXBean.getArch,
      "availableProcessors" -> Int.box(osMXBean.getAvailableProcessors),
      "systemLoadAverage" -> Double.box(osMXBean.getSystemLoadAverage),
      "totalClasses" -> Long.box(classLoadingMXBean.getTotalLoadedClassCount),
      "loadedClasses" -> Int.box(classLoadingMXBean.getLoadedClassCount),
      "unloadedClasses" -> Long.box(classLoadingMXBean.getUnloadedClassCount)
    )

    // Try to get more detailed system info if available
    val data = osMXBean match {
      case sunOsMXBean: com.sun.management.OperatingSystemMXBean =>
        baseData ++ Map[String, AnyRef](
          "totalPhysicalMemory" -> formatBytes(sunOsMXBean.getTotalPhysicalMemorySize),
          "freePhysicalMemory" -> formatBytes(sunOsMXBean.getFreePhysicalMemorySize),
          "committedVirtualMemory" -> formatBytes(sunOsMXBean.getCommittedVirtualMemorySize),
          "processCpuLoad" -> f"${sunOsMXBean.getProcessCpuLoad * 100}%.2f%%",
          "systemCpuLoad" -> f"${sunOsMXBean.getSystemCpuLoad * 100}%.2f%%",
          "processCpuTime" -> ((sunOsMXBean.getProcessCpuTime / 1_000_000) + "ms")
        )
      case _ => baseData
    }

    QueryResult(
      dataSourceId = "jvm:system",
      data = List(data),
      totalCount = 1,
      limit = 1,
      offset = 0
    )
  }

  private def formatBytes(bytes: Long): String = {
    if (bytes < 1024) s"$bytes B"
    else if (bytes < 1024 * 1024) f"${bytes / 1024.0}%.2f KB"
    else if (bytes < 1024 * 1024 * 1024) f"${bytes / (1024.0 * 1024)}%.2f MB"
    else f"${bytes / (1024.0 * 1024 * 1024)}%.2f GB"
  }

  private def formatDuration(millis: Long): String = {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    if (days > 0) f"${days}%dd ${hours % 24}%dh ${minutes % 60}%dm"
    else if (hours > 0) f"${hours}%dh ${minutes % 60}%dm ${seconds % 60}%ds"
    else if (minutes > 0) f"${minutes}%dm ${seconds % 60}%ds"
    else f"${seconds}%ds"
  }
}
