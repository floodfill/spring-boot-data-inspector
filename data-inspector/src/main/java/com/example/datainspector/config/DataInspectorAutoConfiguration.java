package com.example.datainspector.config;

import com.example.datainspector.controller.DataInspectorController;
import com.example.datainspector.controller.DataInspectorViewController;
import com.example.datainspector.provider.BeanDataSourceProvider;
import com.example.datainspector.provider.CacheDataSourceProvider;
import com.example.datainspector.provider.MongoDBDataSourceProvider;
import com.example.datainspector.registry.CustomDataSourceRegistry;
import com.example.datainspector.service.DataInspectorService;
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
@ComponentScan(basePackages = "com.example.datainspector")
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
