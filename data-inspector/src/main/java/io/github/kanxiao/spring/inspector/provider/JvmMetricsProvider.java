/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.*;

/**
 * Provides JVM runtime metrics - memory, threads, CPU, garbage collection, etc.
 * Essential for production debugging
 */
@Slf4j
public class JvmMetricsProvider implements DataSourceProvider {

    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    private final OperatingSystemMXBean osMXBean;
    private final ClassLoadingMXBean classLoadingMXBean;

    public JvmMetricsProvider() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
        this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        log.info("JVM Metrics provider enabled");
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        List<DataSourceInfo> dataSources = new ArrayList<>();

        dataSources.add(DataSourceInfo.builder()
                .id("jvm:memory")
                .name("Memory Usage")
                .type("jvm-memory")
                .description("Heap and non-heap memory usage, garbage collection stats")
                .size(1)
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("jvm:threads")
                .name("Thread Information")
                .type("jvm-threads")
                .description("Active threads, thread states, deadlock detection")
                .size(threadMXBean.getThreadCount())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("jvm:runtime")
                .name("Runtime Information")
                .type("jvm-runtime")
                .description("JVM uptime, version, system properties")
                .size(1)
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("jvm:gc")
                .name("Garbage Collection")
                .type("jvm-gc")
                .description("GC statistics and performance metrics")
                .size(gcMXBeans.size())
                .queryable(true)
                .build());

        dataSources.add(DataSourceInfo.builder()
                .id("jvm:system")
                .name("System Information")
                .type("jvm-system")
                .description("Operating system and CPU information")
                .size(1)
                .queryable(true)
                .build());

        return dataSources;
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        return switch (dataSourceId) {
            case "jvm:memory" -> queryMemory();
            case "jvm:threads" -> queryThreads(limit, offset);
            case "jvm:runtime" -> queryRuntime();
            case "jvm:gc" -> queryGarbageCollection();
            case "jvm:system" -> querySystem();
            default -> QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        };
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("jvm:");
    }

    private QueryResult queryMemory() {
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("heapUsed", formatBytes(heapMemory.getUsed()));
        data.put("heapUsedBytes", heapMemory.getUsed());
        data.put("heapCommitted", formatBytes(heapMemory.getCommitted()));
        data.put("heapMax", formatBytes(heapMemory.getMax()));
        data.put("heapUsagePercent", String.format("%.2f%%", (heapMemory.getUsed() * 100.0 / heapMemory.getMax())));

        data.put("nonHeapUsed", formatBytes(nonHeapMemory.getUsed()));
        data.put("nonHeapUsedBytes", nonHeapMemory.getUsed());
        data.put("nonHeapCommitted", formatBytes(nonHeapMemory.getCommitted()));
        data.put("nonHeapMax", nonHeapMemory.getMax() > 0 ? formatBytes(nonHeapMemory.getMax()) : "undefined");

        data.put("objectsPendingFinalization", memoryMXBean.getObjectPendingFinalizationCount());

        // Memory pools
        List<Map<String, Object>> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            Map<String, Object> poolData = new LinkedHashMap<>();
            poolData.put("name", pool.getName());
            poolData.put("type", pool.getType().toString());
            poolData.put("used", formatBytes(usage.getUsed()));
            poolData.put("max", usage.getMax() > 0 ? formatBytes(usage.getMax()) : "undefined");
            pools.add(poolData);
        }
        data.put("memoryPools", pools);

        return QueryResult.builder()
                .dataSourceId("jvm:memory")
                .data(List.of(data))
                .totalCount(1)
                .limit(1)
                .offset(0)
                .build();
    }

    private QueryResult queryThreads(int limit, int offset) {
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(false, false);

        List<Map<String, Object>> threads = new ArrayList<>();
        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo == null) continue;

            Map<String, Object> threadData = new LinkedHashMap<>();
            threadData.put("id", threadInfo.getThreadId());
            threadData.put("name", threadInfo.getThreadName());
            threadData.put("state", threadInfo.getThreadState().toString());
            threadData.put("cpuTime", threadMXBean.isThreadCpuTimeSupported() ?
                    threadMXBean.getThreadCpuTime(threadInfo.getThreadId()) / 1_000_000 + "ms" : "N/A");
            threadData.put("blockedTime", threadInfo.getBlockedTime());
            threadData.put("blockedCount", threadInfo.getBlockedCount());
            threadData.put("waitedTime", threadInfo.getWaitedTime());
            threadData.put("waitedCount", threadInfo.getWaitedCount());
            threadData.put("suspended", threadInfo.isSuspended());
            threadData.put("inNative", threadInfo.isInNative());

            threads.add(threadData);
        }

        // Sort by CPU time if available
        threads.sort((a, b) -> {
            String timeA = (String) a.get("cpuTime");
            String timeB = (String) b.get("cpuTime");
            if (timeA.equals("N/A") || timeB.equals("N/A")) return 0;
            return timeB.compareTo(timeA);
        });

        int total = threads.size();
        List<Map<String, Object>> paginated = threads.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalThreads", threadMXBean.getThreadCount());
        stats.put("peakThreads", threadMXBean.getPeakThreadCount());
        stats.put("daemonThreads", threadMXBean.getDaemonThreadCount());
        stats.put("totalStarted", threadMXBean.getTotalStartedThreadCount());

        // Check for deadlocks
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        stats.put("deadlockedThreads", deadlockedThreads != null ? deadlockedThreads.length : 0);

        return QueryResult.builder()
                .dataSourceId("jvm:threads")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .stats(stats)
                .build();
    }

    private QueryResult queryRuntime() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uptime", formatDuration(runtimeMXBean.getUptime()));
        data.put("uptimeMs", runtimeMXBean.getUptime());
        data.put("startTime", new Date(runtimeMXBean.getStartTime()).toString());
        data.put("vmName", runtimeMXBean.getVmName());
        data.put("vmVendor", runtimeMXBean.getVmVendor());
        data.put("vmVersion", runtimeMXBean.getVmVersion());
        data.put("specName", runtimeMXBean.getSpecName());
        data.put("specVendor", runtimeMXBean.getSpecVendor());
        data.put("specVersion", runtimeMXBean.getSpecVersion());
        data.put("managementSpecVersion", runtimeMXBean.getManagementSpecVersion());
        data.put("classPath", runtimeMXBean.getClassPath());
        data.put("libraryPath", runtimeMXBean.getLibraryPath());
        data.put("inputArguments", runtimeMXBean.getInputArguments());

        return QueryResult.builder()
                .dataSourceId("jvm:runtime")
                .data(List.of(data))
                .totalCount(1)
                .limit(1)
                .offset(0)
                .build();
    }

    private QueryResult queryGarbageCollection() {
        List<Map<String, Object>> gcData = new ArrayList<>();

        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            Map<String, Object> gc = new LinkedHashMap<>();
            gc.put("name", gcBean.getName());
            gc.put("collectionCount", gcBean.getCollectionCount());
            gc.put("collectionTime", gcBean.getCollectionTime() + "ms");
            gc.put("memoryPoolNames", Arrays.toString(gcBean.getMemoryPoolNames()));
            gcData.add(gc);
        }

        return QueryResult.builder()
                .dataSourceId("jvm:gc")
                .data(gcData)
                .totalCount(gcData.size())
                .limit(gcData.size())
                .offset(0)
                .build();
    }

    private QueryResult querySystem() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("osName", osMXBean.getName());
        data.put("osVersion", osMXBean.getVersion());
        data.put("osArch", osMXBean.getArch());
        data.put("availableProcessors", osMXBean.getAvailableProcessors());
        data.put("systemLoadAverage", osMXBean.getSystemLoadAverage());

        data.put("totalClasses", classLoadingMXBean.getTotalLoadedClassCount());
        data.put("loadedClasses", classLoadingMXBean.getLoadedClassCount());
        data.put("unloadedClasses", classLoadingMXBean.getUnloadedClassCount());

        // Try to get more detailed system info if available
        if (osMXBean instanceof com.sun.management.OperatingSystemMXBean sunOsMXBean) {
            data.put("totalPhysicalMemory", formatBytes(sunOsMXBean.getTotalPhysicalMemorySize()));
            data.put("freePhysicalMemory", formatBytes(sunOsMXBean.getFreePhysicalMemorySize()));
            data.put("committedVirtualMemory", formatBytes(sunOsMXBean.getCommittedVirtualMemorySize()));
            data.put("processCpuLoad", String.format("%.2f%%", sunOsMXBean.getProcessCpuLoad() * 100));
            data.put("systemCpuLoad", String.format("%.2f%%", sunOsMXBean.getSystemCpuLoad() * 100));
            data.put("processCpuTime", sunOsMXBean.getProcessCpuTime() / 1_000_000 + "ms");
        }

        return QueryResult.builder()
                .dataSourceId("jvm:system")
                .data(List.of(data))
                .totalCount(1)
                .limit(1)
                .offset(0)
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        return String.format("%ds", seconds);
    }
}
