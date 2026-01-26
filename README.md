# Spring Inspector - Professional Debugging Dashboard for Spring Boot

A comprehensive, production-ready debugging dashboard that exposes your Spring Boot application's internal state through an elegant web interface. Perfect for development, staging, and production debugging.

## Features

### Auto-Discovery & Data Sources

Spring Inspector automatically discovers and exposes:

- **Spring Caches** - All `@Cacheable` and `CacheManager` caches with live data
- **Spring Beans** - Bean registry with state inspection
- **JPA Entities** - All database tables with query and pagination support
- **MongoDB Collections** - Collections with filtering, stats, and document inspection
- **Custom Data Sources** - Register your own collections, maps, and data structures

### Production Debugging Tools

- **JVM Metrics** - Memory usage, garbage collection, thread states, deadlock detection
- **HTTP Request Tracking** - Recent requests with timing, status codes, and error tracking
- **Scheduled Tasks** - View all `@Scheduled` tasks and their next execution times
- **Environment Variables** - Application properties, system properties, active profiles
- **System Information** - CPU, memory, disk, and operating system metrics

### Developer Experience

- **Beautiful Web UI** - Modern, intuitive dashboard with real-time data
- **REST API** - Query all data sources programmatically
- **Zero Configuration** - Just add the dependency - works out of the box
- **Production-Ready** - Disable with a single property, secure with Spring Security
- **Sensitive Data Masking** - Automatically masks passwords, secrets, and tokens
- **Export Capabilities** - Export to CSV, JSON, Excel, HTML, Markdown
- **Usage Analytics** - Track usage patterns and optimize performance

## Quick Start

### 1. Add to your project

**Gradle:**
```gradle
dependencies {
    implementation 'io.github.kanxiao:data-inspector:1.0.0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>io.github.kanxiao</groupId>
    <artifactId>data-inspector</artifactId>
    <version>1.0.0</version>
</dependency>
```

*(Note: You'll need to publish this to Maven Central or a local repo first, or include it as a composite build)*

### 2. Run your application

```bash
./gradlew bootRun
```

### 3. Open the dashboard

Navigate to: **http://localhost:8080/data-inspector**

That's it! Spring Inspector will automatically discover your caches, beans, databases, and more.

## Usage Guide

### Viewing Database Tables (JPA/SQL)

Any JPA entity is automatically discovered and queryable:

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private String email;
    private String name;
}
```

Access via Spring Inspector to:
- View all records with pagination
- Filter by any field
- See table statistics
- Export to JSON

### Viewing MongoDB Collections

If you have MongoDB configured, all collections are automatically exposed:

```java
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String name;
    private double price;
}
```

### Registering Custom Data Sources

Expose your application's internal data structures:

```java
@Configuration
public class DataSourceConfig {
    @Autowired
    private CustomDataSourceRegistry registry;

    @Bean
    public CommandLineRunner registerCustomData(MyService myService) {
        return args -> {
            // Register a Collection
            registry.register(
                "active-sessions",
                "Active User Sessions",
                "Currently active user sessions",
                () -> myService.getActiveSessions()
            );
        };
    }
}
```

### Configuration

```properties
# Core settings
data-inspector.enabled=true
data-inspector.basePath=/data-inspector

# Feature toggles
data-inspector.discoverCaches=true
data-inspector.discoverBeans=true
data-inspector.mongodbEnabled=true
data-inspector.jpaEnabled=true
data-inspector.jvmMetricsEnabled=true
data-inspector.trackHttpRequests=true
data-inspector.scheduledTasksEnabled=true
data-inspector.environmentEnabled=true
```

## Architecture

```
spring-inspector/              - Root project
├── data-inspector/            - Core module (reusable library)
│   ├── config/                - Auto-configuration
│   ├── model/                 - Data models
│   ├── provider/              - Auto-discovery providers
│   ├── registry/              - Custom data source registry
│   ├── service/               - Core service
│   └── controller/            - REST API
└── demo-app/                  - Demo application
    └── Shows all features in action
```

## Demo Application

Run the demo to see all features:

```bash
cd demo-app
../gradlew bootRun
```

Then open: http://localhost:8080/data-inspector

## License

MIT License - Use freely in your projects!
