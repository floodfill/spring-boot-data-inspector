package com.example.datainspector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Data Inspector
 */
@Data
@ConfigurationProperties(prefix = "data-inspector")
public class DataInspectorProperties {

    /**
     * Enable or disable the Data Inspector
     */
    private boolean enabled = true;

    /**
     * Base path for the Data Inspector UI
     */
    private String basePath = "/data-inspector";

    /**
     * Enable auto-discovery of caches
     */
    private boolean discoverCaches = true;

    /**
     * Enable auto-discovery of Spring beans
     */
    private boolean discoverBeans = true;

    /**
     * Enable MongoDB integration
     */
    private boolean mongodbEnabled = true;
}
