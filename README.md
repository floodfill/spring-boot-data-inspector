# Data Inspector - Beautiful Debugging Dashboard for Spring Boot

A beautiful, zero-config debugging dashboard that exposes your Spring Boot application's internal data through an elegant web interface.

## Features

- **Auto-Discovery**: Automatically finds and exposes:
  - Spring Caches (Caffeine, ConcurrentMap, etc.)
  - Spring Beans and their state
  - MongoDB statistics and collections
  - Custom data sources you register

- **Beautiful Web UI**: Modern, intuitive dashboard with:
  - Real-time data exploration
  - Filtering and pagination
  - JSON export
  - Responsive design

- **REST API**: Query your data programmatically
- **Zero Configuration**: Just add the dependency - it works!
- **Production-Ready**: Disable with a single property

## Quick Start

### 1. Add to your project

```gradle
dependencies {
    implementation project(':data-inspector')
}
```

### 2. Run your application

```bash
./gradlew bootRun
```

### 3. Open the dashboard

Navigate to: **http://localhost:8080/data-inspector**

That's it! The Data Inspector will automatically discover your caches, beans, and data.

## Usage

### Viewing Caches

Any Spring `@Cacheable` methods or `CacheManager` caches are automatically discovered:

```java
@Cacheable("users")
public User getUserById(String id) {
    return database.findUser(id);
}
```

Access the cache through Data Inspector to see all cached entries.

### Registering Custom Data Sources

Expose your own data structures:

```java
@Configuration
public class DataSourceConfig {
    @Autowired
    private CustomDataSourceRegistry registry;

    @Bean
    public CommandLineRunner registerData() {
        return args -> {
            registry.register(
                "my-queue",
                "Request Queue",
                "Pending requests",
                () -> myQueue.stream().toList()
            );
        };
    }
}
```

### MongoDB Integration

If you have `spring-boot-starter-data-mongodb` on your classpath, Data Inspector automatically exposes:
- Database statistics
- Collection information
- Sample documents from each collection

### Configuration

```properties
# Disable Data Inspector (enabled by default)
data-inspector.enabled=false

# Customize base path (default: /data-inspector)
data-inspector.basePath=/debug

# Toggle specific features
data-inspector.discoverCaches=true
data-inspector.discoverBeans=true
data-inspector.mongodbEnabled=true
```

## Architecture

```
data-inspector/          - Core module (reusable library)
├── model/              - Data models
├── spi/                - Service Provider Interface
├── provider/           - Auto-discovery providers
│   ├── CacheDataSourceProvider
│   ├── BeanDataSourceProvider
│   └── MongoDBDataSourceProvider
├── registry/           - Custom data source registry
├── service/            - Core service
├── controller/         - REST API
└── config/             - Auto-configuration

demo-app/               - Demo application
└── Shows all features in action
```

## REST API

### Get all data sources
```bash
GET /data-inspector/api/datasources
```

### Query a data source
```bash
GET /data-inspector/api/datasources/{id}/query?limit=100&offset=0
POST /data-inspector/api/datasources/{id}/query
```

## Design Philosophy

Inspired by Steve Jobs' philosophy:
- **It just works** - Zero configuration needed
- **Beautiful** - Clean, modern UI you'll love to use
- **Simple** - One dependency, instant results
- **Powerful** - Auto-discovers everything important

## Demo Application

Run the demo to see it in action:

```bash
cd demo-app
../gradlew bootRun
```

Then open: http://localhost:8080/data-inspector

The demo includes:
- Cached users and products
- Spring beans with state
- Custom data sources (sessions, queues)
- MongoDB integration (if running locally)

## Production Use

For production environments, either:

1. Disable entirely:
```properties
data-inspector.enabled=false
```

2. Secure the endpoints with Spring Security:
```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http.authorizeRequests()
            .antMatchers("/data-inspector/**").hasRole("ADMIN");
        return http.build();
    }
}
```

## License

MIT License - Use freely in your projects!

---

Built with ❤️ for Spring Boot developers who want to debug with style.
