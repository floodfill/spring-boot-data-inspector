/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package io.github.kanxiao.spring.inspector.config;

import io.github.kanxiao.spring.inspector.controller.DataInspectorController;
import io.github.kanxiao.spring.inspector.controller.DataInspectorViewController;
import io.github.kanxiao.spring.inspector.provider.BeanDataSourceProvider;
import io.github.kanxiao.spring.inspector.provider.CacheDataSourceProvider;
import io.github.kanxiao.spring.inspector.provider.MongoDBDataSourceProvider;
import io.github.kanxiao.spring.inspector.registry.CustomDataSourceRegistry;
import io.github.kanxiao.spring.inspector.service.DataInspectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for Data Inspector
 *
 * To enable: Add this module to your dependencies - it auto-configures!
 * To disable: Set data-inspector.enabled=false in application.properties
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = "io.github.kanxiao.spring.inspector")
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
}
