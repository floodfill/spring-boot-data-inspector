/*
 * Copyright (c) 2026 Data Inspector Contributors
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package com.example.datainspector.config;

import com.example.datainspector.provider.HttpRequestTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration for Data Inspector filters
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "data-inspector.enabled", havingValue = "true", matchIfMissing = true)
public class DataInspectorFilterConfig {

    /**
     * Register the HTTP request tracker as a servlet filter
     */
    @Bean
    @ConditionalOnBean(HttpRequestTracker.class)
    @ConditionalOnProperty(name = "data-inspector.track-http-requests", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<HttpRequestTracker> httpRequestTrackerFilter(HttpRequestTracker tracker) {
        FilterRegistrationBean<HttpRequestTracker> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(tracker);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registrationBean.setName("httpRequestTracker");
        log.info("HTTP Request Tracker filter registered");
        return registrationBean;
    }
}
