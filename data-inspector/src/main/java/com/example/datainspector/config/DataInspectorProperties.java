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

    /**
     * Enable JPA/SQL integration
     */
    private boolean jpaEnabled = true;

    /**
     * Enable JVM metrics monitoring
     */
    private boolean jvmMetricsEnabled = true;

    /**
     * Enable HTTP request tracking
     */
    private boolean trackHttpRequests = true;

    /**
     * Enable scheduled tasks monitoring
     */
    private boolean scheduledTasksEnabled = true;

    /**
     * Enable environment and properties monitoring
     */
    private boolean environmentEnabled = true;

    /**
     * Maximum number of HTTP requests to track
     */
    private int maxTrackedHttpRequests = 1000;

    /**
     * Mask sensitive property values (password, secret, token, etc.)
     */
    private boolean maskSensitiveData = true;

    /**
     * Require authentication to access Data Inspector
     * When true, endpoints are secured using Spring Security if available
     */
    private boolean requireAuth = false;

    /**
     * Allowed roles to access Data Inspector (when requireAuth is true)
     */
    private String[] allowedRoles = {"ADMIN", "DEVELOPER"};
}
