package com.example.demo.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Example scheduled tasks to demonstrate the Scheduled Tasks provider
 */
@Slf4j
@Component
public class DemoScheduledTasks {

    private int cacheCleanupCount = 0;
    private int healthCheckCount = 0;
    private int reportGenerationCount = 0;

    /**
     * Cache cleanup every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupCache() {
        cacheCleanupCount++;
        log.debug("Cache cleanup executed {} times", cacheCleanupCount);
    }

    /**
     * Health check every 30 seconds
     */
    @Scheduled(fixedDelay = 30000) // 30 seconds
    public void healthCheck() {
        healthCheckCount++;
        log.debug("Health check executed {} times", healthCheckCount);
    }

    /**
     * Daily report at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateDailyReport() {
        reportGenerationCount++;
        log.info("Daily report generated");
    }

    /**
     * Metrics collection every minute
     */
    @Scheduled(fixedRate = 60000, initialDelay = 10000)
    public void collectMetrics() {
        log.debug("Collecting metrics");
    }
}
