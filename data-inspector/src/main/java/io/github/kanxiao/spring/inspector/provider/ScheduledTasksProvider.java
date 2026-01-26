/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.provider;

import io.github.kanxiao.spring.inspector.model.DataSourceInfo;
import io.github.kanxiao.spring.inspector.model.QueryResult;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Provides information about scheduled tasks in the application
 * Shows all @Scheduled tasks and their schedules
 */
@Slf4j
public class ScheduledTasksProvider implements DataSourceProvider {

    private final ScheduledAnnotationBeanPostProcessor postProcessor;

    public ScheduledTasksProvider(Optional<ScheduledAnnotationBeanPostProcessor> postProcessor) {
        this.postProcessor = postProcessor.orElse(null);
        if (postProcessor.isPresent()) {
            log.info("Scheduled Tasks provider enabled");
        }
    }

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        if (postProcessor == null) {
            return Collections.emptyList();
        }

        try {
            Set<ScheduledTask> scheduledTasks = postProcessor.getScheduledTasks();

            return List.of(
                    DataSourceInfo.builder()
                            .id("scheduled:tasks")
                            .name("Scheduled Tasks")
                            .type("scheduled")
                            .description("All @Scheduled tasks and their schedules")
                            .size(scheduledTasks.size())
                            .queryable(true)
                            .build()
            );
        } catch (Exception e) {
            log.debug("Could not discover scheduled tasks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters, int limit, int offset) {
        if (!dataSourceId.equals("scheduled:tasks")) {
            return QueryResult.builder()
                    .dataSourceId(dataSourceId)
                    .data(Collections.emptyList())
                    .totalCount(0)
                    .build();
        }

        Set<ScheduledTask> scheduledTasks = postProcessor.getScheduledTasks();
        List<Map<String, Object>> data = new ArrayList<>();

        for (ScheduledTask scheduledTask : scheduledTasks) {
            try {
                Map<String, Object> taskData = new LinkedHashMap<>();
                Task task = scheduledTask.getTask();

                // Get runnable info
                Runnable runnable = task.getRunnable();
                taskData.put("taskClass", runnable.getClass().getName());
                taskData.put("taskString", runnable.toString());

                // Get schedule info
                String scheduleInfo = extractScheduleInfo(task);
                taskData.put("schedule", scheduleInfo);

                // Get next execution time if possible
                try {
                    ScheduledFuture<?> future = (ScheduledFuture<?>) scheduledTask.getClass()
                            .getDeclaredMethod("getScheduledFuture")
                            .invoke(scheduledTask);

                    if (future != null) {
                        long delay = future.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
                        taskData.put("nextExecutionIn", formatDuration(delay));
                        taskData.put("nextExecutionInMs", delay);
                        taskData.put("cancelled", future.isCancelled());
                        taskData.put("done", future.isDone());
                    }
                } catch (Exception e) {
                    log.debug("Could not get scheduled future: {}", e.getMessage());
                }

                data.add(taskData);
            } catch (Exception e) {
                log.debug("Error processing scheduled task: {}", e.getMessage());
            }
        }

        // Sort by next execution time
        data.sort((a, b) -> {
            Long timeA = (Long) a.get("nextExecutionInMs");
            Long timeB = (Long) b.get("nextExecutionInMs");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeA.compareTo(timeB);
        });

        int total = data.size();
        List<Map<String, Object>> paginated = data.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        return QueryResult.builder()
                .dataSourceId("scheduled:tasks")
                .data(paginated)
                .totalCount(total)
                .limit(limit)
                .offset(offset)
                .build();
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("scheduled:");
    }

    private String extractScheduleInfo(Task task) {
        try {
            // Try to extract schedule information using reflection
            String taskString = task.toString();

            // Look for common patterns
            if (taskString.contains("cron")) {
                int cronStart = taskString.indexOf("cron='") + 6;
                int cronEnd = taskString.indexOf("'", cronStart);
                if (cronStart > 6 && cronEnd > cronStart) {
                    return "Cron: " + taskString.substring(cronStart, cronEnd);
                }
            }

            if (taskString.contains("fixedDelay")) {
                int delayStart = taskString.indexOf("fixedDelay=") + 11;
                int delayEnd = taskString.indexOf(",", delayStart);
                if (delayEnd == -1) delayEnd = taskString.indexOf("]", delayStart);
                if (delayStart > 11 && delayEnd > delayStart) {
                    long delay = Long.parseLong(taskString.substring(delayStart, delayEnd).trim());
                    return "Fixed Delay: " + formatDuration(delay);
                }
            }

            if (taskString.contains("fixedRate")) {
                int rateStart = taskString.indexOf("fixedRate=") + 10;
                int rateEnd = taskString.indexOf(",", rateStart);
                if (rateEnd == -1) rateEnd = taskString.indexOf("]", rateStart);
                if (rateStart > 10 && rateEnd > rateStart) {
                    long rate = Long.parseLong(taskString.substring(rateStart, rateEnd).trim());
                    return "Fixed Rate: " + formatDuration(rate);
                }
            }

            return taskString;
        } catch (Exception e) {
            log.debug("Could not extract schedule info: {}", e.getMessage());
            return "Unknown schedule";
        }
    }

    private String formatDuration(long millis) {
        if (millis < 0) return "Overdue";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return String.format("%dd %dh", days, hours % 24);
        if (hours > 0) return String.format("%dh %dm", hours, minutes % 60);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds % 60);
        if (seconds > 0) return String.format("%ds", seconds);
        return String.format("%dms", millis);
    }
}
