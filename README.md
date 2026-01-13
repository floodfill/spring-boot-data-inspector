# Data Inspector - Professional Debugging Dashboard for Spring Boot

A comprehensive, production-ready debugging dashboard that exposes your Spring Boot application's internal state through an elegant web interface. Perfect for development, staging, and production debugging.

## Features

### Auto-Discovery & Data Sources

Data Inspector automatically discovers and exposes:

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
    implementation project(':data-inspector')
}
```

**Maven:**
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>data-inspector</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Run your application

```bash
./gradlew bootRun
```

### 3. Open the dashboard

Navigate to: **http://localhost:8080/data-inspector**

That's it! Data Inspector will automatically discover your caches, beans, databases, and more.

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

Access via Data Inspector to:
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

Features:
- View documents with pagination
- Filter by fields
- See collection statistics
- Database overview

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

### Registering Maps

Special support for Map-like data structures:

```java
// Register a Map directly
Map<String, User> userCache = new ConcurrentHashMap<>();
registry.registerMap("user-cache", "User Cache", "In-memory user cache", userCache);

// Register with custom transformers
registry.registerMap(
    "request-queue",
    "Request Queue",
    "Pending requests",
    requestMap,
    key -> key.toString(),
    value -> Map.of(
        "id", value.getId(),
        "status", value.getStatus(),
        "timestamp", value.getTimestamp()
    )
);

// Register a Map supplier (for dynamic maps)
registry.registerMapSupplier(
    "active-connections",
    "Active Connections",
    "Current active connections",
    () -> connectionManager.getActiveConnections()
);
```

### Monitoring HTTP Requests

Data Inspector automatically tracks HTTP requests:

```java
// No configuration needed - it just works!
// View in Data Inspector:
// - Recent requests with timing
// - Active (in-flight) requests
// - Request statistics and slowest endpoints
```

Features:
- Tracks last 1000 requests (configurable)
- Shows method, URI, status code, duration
- Identifies slow requests
- Groups by status code (2xx, 3xx, 4xx, 5xx)

### Viewing JVM Metrics

Essential for production debugging:

- **Memory** - Heap/non-heap usage, memory pools, pending finalization
- **Threads** - All threads with state, CPU time, blocked/waiting counts
- **Garbage Collection** - GC stats, collection counts and times
- **System** - CPU load, OS info, physical memory
- **Runtime** - Uptime, JVM version, input arguments

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

# Advanced settings
data-inspector.maxTrackedHttpRequests=1000
data-inspector.maskSensitiveData=true

# Export settings
data-inspector.exportEnabled=true
data-inspector.maxExportRecords=10000

# Analytics settings
data-inspector.telemetryEnabled=true

# Security (optional)
data-inspector.requireAuth=false
data-inspector.allowedRoles=ADMIN,DEVELOPER
```

## Export Data

Export any data source to multiple formats:

```bash
# Export as CSV
GET /data-inspector/api/datasources/{id}/export?format=csv

# Export as JSON
GET /data-inspector/api/datasources/{id}/export?format=json

# Export as Excel (CSV with BOM)
GET /data-inspector/api/datasources/{id}/export?format=excel

# Export as HTML table
GET /data-inspector/api/datasources/{id}/export?format=html

# Export as Markdown
GET /data-inspector/api/datasources/{id}/export?format=markdown
```

Example:
```bash
# Export users to CSV
curl -o users.csv \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=csv"

# Export with filters
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"status":"active"}' \
  -o active_users.json \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=json"
```

See [EXPORT_AND_ANALYTICS.md](EXPORT_AND_ANALYTICS.md) for detailed documentation.

## Usage Analytics

Track how Data Inspector is being used:

```bash
# Get overall analytics
GET /data-inspector/api/analytics

# Get analytics for last 24 hours
GET /data-inspector/api/analytics/period?hours=24
```

Analytics include:
- Query counts by data source
- Export format popularity
- Daily active users
- Error tracking
- Recent activity

Configuration:
```properties
# Enable/disable telemetry (default: true)
data-inspector.telemetryEnabled=true
```

## REST API

### Get all data sources

```bash
GET /data-inspector/api/datasources

Response:
[
  {
    "id": "jpa:entity:User",
    "name": "Entity: User",
    "type": "jpa-entity",
    "description": "JPA Entity: com.example.User",
    "size": 1523,
    "queryable": true,
    "metadata": {
      "entityName": "User",
      "javaType": "com.example.User",
      "tableName": "users"
    }
  },
  ...
]
```

### Query a data source

```bash
GET /data-inspector/api/datasources/jpa:entity:User/query?limit=10&offset=0

POST /data-inspector/api/datasources/jpa:entity:User/query
Content-Type: application/json

{
  "filters": {
    "status": "active"
  },
  "limit": 10,
  "offset": 0
}

Response:
{
  "dataSourceId": "jpa:entity:User",
  "data": [
    {
      "id": 1,
      "email": "user@example.com",
      "name": "John Doe",
      "status": "active"
    },
    ...
  ],
  "totalCount": 1523,
  "limit": 10,
  "offset": 0
}
```

## Architecture

```
data-inspector/                 - Core module (reusable library)
├── config/                    - Auto-configuration
│   ├── DataInspectorAutoConfiguration
│   ├── DataInspectorProperties
│   └── DataInspectorFilterConfig
├── model/                     - Data models
│   ├── DataSourceInfo
│   └── QueryResult
├── spi/                       - Service Provider Interface
│   └── DataSourceProvider
├── provider/                  - Auto-discovery providers
│   ├── CacheDataSourceProvider      - Spring caches
│   ├── BeanDataSourceProvider       - Spring beans
│   ├── MongoDBDataSourceProvider    - MongoDB collections
│   ├── JpaDataSourceProvider        - JPA entities
│   ├── JvmMetricsProvider          - JVM metrics
│   ├── HttpRequestTracker          - HTTP request tracking
│   ├── ScheduledTasksProvider      - Scheduled tasks
│   └── EnvironmentProvider         - Environment/properties
├── registry/                  - Custom data source registry
│   └── CustomDataSourceRegistry
├── service/                   - Core service
│   └── DataInspectorService
└── controller/                - REST API
    ├── DataInspectorController
    └── DataInspectorViewController

demo-app/                      - Demo application
└── Shows all features in action
```

## Production Use

### Security Best Practices

1. **Disable in production** (recommended for public-facing apps):
```properties
data-inspector.enabled=false
```

2. **Enable authentication**:
```properties
data-inspector.requireAuth=true
data-inspector.allowedRoles=ADMIN
```

3. **Use Spring Security**:
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/data-inspector/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

4. **Use network-level restrictions**:
   - Only allow access from internal networks
   - Use VPN for remote access
   - Configure firewall rules

### Performance Considerations

- HTTP request tracking keeps max 1000 requests in memory (configurable)
- All queries support pagination to avoid loading large datasets
- Lazy evaluation of data sources - only computed when accessed
- No performance impact when disabled

### Data Privacy

- Sensitive values (passwords, secrets, tokens) are automatically masked
- Configure `data-inspector.maskSensitiveData=true` (default)
- Custom sensitive key patterns can be added
- Environment variables and system properties are masked by default

## Demo Application

Run the demo to see all features:

```bash
cd demo-app
../gradlew bootRun
```

Then open: http://localhost:8080/data-inspector

The demo includes:
- JPA entities (users, products)
- MongoDB collections
- Cached data
- Custom data sources (sessions, queues, maps)
- Scheduled tasks
- HTTP endpoints to generate traffic

## Use Cases

### Development
- Inspect cache contents while developing features
- View database state without SQL queries
- Monitor HTTP requests and responses
- Check scheduled task execution

### Staging/QA
- Verify data migrations
- Inspect production-like data
- Debug integration issues
- Monitor system health

### Production
- Emergency debugging
- Performance troubleshooting
- Memory leak investigation
- Request tracking for specific issues

## Advanced Features

### Custom Data Transformers

Transform data before display:

```java
registry.register(
    "complex-objects",
    "Complex Objects",
    "Custom business objects",
    () -> myService.getObjects().stream()
        .map(obj -> Map.of(
            "id", obj.getId(),
            "summary", obj.getSummary(),
            "status", obj.getStatus().name(),
            "metrics", obj.getMetrics()
        ))
        .collect(Collectors.toList())
);
```

### Real-time Monitoring

All data sources are queried in real-time:

```java
// Register a supplier that returns current state
registry.registerMapSupplier(
    "real-time-stats",
    "Real-time Statistics",
    "Current system statistics",
    () -> statsService.getCurrentStats()
);
```

### Integration with Monitoring Tools

Export data via REST API:

```bash
# Get JVM metrics
curl http://localhost:8080/data-inspector/api/datasources/jvm:memory/query

# Get active requests
curl http://localhost:8080/data-inspector/api/datasources/http:active/query

# Get database stats
curl http://localhost:8080/data-inspector/api/datasources/jpa:overview/query
```

## FAQ

**Q: Does this impact performance?**
A: Minimal impact when enabled, zero when disabled. Data is only computed when requested.

**Q: Is it safe for production?**
A: Yes, with proper security. Disable for public apps or secure with Spring Security.

**Q: Can I customize the UI?**
A: Currently, the UI is bundled. REST API allows building custom interfaces.

**Q: Does it work with Spring Boot 3?**
A: Yes, fully compatible with Spring Boot 3.x and Jakarta EE.

**Q: Can I filter/search data?**
A: Yes, all data sources support filtering via REST API.

**Q: How much memory does it use?**
A: Minimal - only tracks configurable number of HTTP requests. All other data is computed on-demand.

## Contributing

Contributions welcome! Areas for improvement:
- Additional data source providers
- Enhanced UI features
- More filtering options
- Export formats (CSV, Excel)
- Real-time updates via WebSockets

## License

MIT License - Use freely in your projects!

## Credits

Built with ❤️ for Spring Boot developers who need powerful debugging tools in production.

---

**Need help?** Open an issue or check out the demo application for examples.

**Want more features?** Let us know what data sources you'd like to inspect!
