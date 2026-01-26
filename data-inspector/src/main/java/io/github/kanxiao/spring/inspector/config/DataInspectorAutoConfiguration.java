/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kanxiao.spring.inspector.controller.DataInspectorController;
import io.github.kanxiao.spring.inspector.controller.DataInspectorViewController;
import io.github.kanxiao.spring.inspector.provider.*;
import io.github.kanxiao.spring.inspector.registry.CustomDataSourceRegistry;
import io.github.kanxiao.spring.inspector.service.DataInspectorService;
import io.github.kanxiao.spring.inspector.service.ExportService;
import io.github.kanxiao.spring.inspector.service.TelemetryService;
import io.github.kanxiao.spring.inspector.spi.DataSourceProvider;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.util.List;
import java.util.Optional;

/**
 * Auto-configuration for Data Inspector
 *
 * To enable: Add this module to your dependencies - it auto-configures!
 * To disable: Set data-inspector.enabled=false in application.properties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(DataInspectorProperties.class)
@ConditionalOnProperty(name = "data-inspector.enabled", havingValue = "true", matchIfMissing = true)
public class DataInspectorAutoConfiguration {

    public DataInspectorAutoConfiguration() {
        log.info("==================================================");
        log.info("  Data Inspector Enabled");
        log.info("  Dashboard: http://localhost:8080/data-inspector");
        log.info("  API: http://localhost:8080/data-inspector/api");
        log.info("==================================================");
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomDataSourceRegistry customDataSourceRegistry() {
        return new CustomDataSourceRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public TelemetryService telemetryService(DataInspectorProperties properties) {
        return new TelemetryService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExportService exportService(ObjectMapper objectMapper) {
        return new ExportService(objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataInspectorService dataInspectorService(List<DataSourceProvider> providers) {
        return new DataInspectorService(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataInspectorController dataInspectorController(
            DataInspectorService service,
            ExportService exportService,
            TelemetryService telemetryService) {
        return new DataInspectorController(service, exportService, telemetryService);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataInspectorViewController dataInspectorViewController() {
        return new DataInspectorViewController();
    }

    // --- Providers ---

    @Bean
    @ConditionalOnProperty(name = "data-inspector.discover-beans", havingValue = "true", matchIfMissing = true)
    public BeanDataSourceProvider beanDataSourceProvider(ApplicationContext context, ObjectMapper objectMapper) {
        return new BeanDataSourceProvider(context, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.environment-enabled", havingValue = "true", matchIfMissing = true)
    public EnvironmentProvider environmentProvider(Environment environment) {
        return new EnvironmentProvider(environment);
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.jvm-metrics-enabled", havingValue = "true", matchIfMissing = true)
    public JvmMetricsProvider jvmMetricsProvider() {
        return new JvmMetricsProvider();
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.track-http-requests", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(jakarta.servlet.Filter.class)
    public HttpRequestTracker httpRequestTracker() {
        return new HttpRequestTracker();
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.discover-caches", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(CacheManager.class)
    public CacheDataSourceProvider cacheDataSourceProvider(Optional<CacheManager> cacheManager, ObjectMapper objectMapper) {
        return new CacheDataSourceProvider(cacheManager, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.scheduled-tasks-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(ScheduledAnnotationBeanPostProcessor.class)
    public ScheduledTasksProvider scheduledTasksProvider(Optional<ScheduledAnnotationBeanPostProcessor> postProcessor) {
        return new ScheduledTasksProvider(postProcessor);
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.jpa-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(EntityManagerFactory.class)
    @ConditionalOnBean(EntityManagerFactory.class)
    public JpaDataSourceProvider jpaDataSourceProvider(Optional<EntityManagerFactory> entityManagerFactory, ObjectMapper objectMapper) {
        return new JpaDataSourceProvider(entityManagerFactory, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "data-inspector.mongodb-enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(MongoTemplate.class)
    @ConditionalOnBean(MongoTemplate.class)
    public MongoDBDataSourceProvider mongoDBDataSourceProvider(Optional<MongoTemplate> mongoTemplate) {
        return new MongoDBDataSourceProvider(mongoTemplate);
    }
}
