# Project Structure

## Overview

This is a multi-module Gradle project with Data Inspector libraries (Java and Scala implementations) and a demo application.

```
data-inspector-demo/
├── build.gradle                    # Root build configuration
├── settings.gradle                 # Module definitions (includes data-inspector, data-inspector-scala, demo-app)
├── README.md                       # Main documentation
├── QUICKSTART.md                   # Quick start guide
├── SCALA_IMPLEMENTATION.md         # Scala implementation summary
│
├── data-inspector/                 # Core reusable module (Java)
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/example/datainspector/
│       │   ├── model/
│       │   │   ├── DataSourceInfo.java         # Data source metadata
│       │   │   └── QueryResult.java            # Query result model
│       │   │
│       │   ├── spi/
│       │   │   └── DataSourceProvider.java     # SPI for providers
│       │   │
│       │   ├── provider/                       # Auto-discovery providers
│       │   │   ├── CacheDataSourceProvider.java      # Discovers Spring caches
│       │   │   ├── BeanDataSourceProvider.java       # Discovers Spring beans
│       │   │   ├── MongoDBDataSourceProvider.java    # MongoDB integration
│       │   │   ├── JpaDataSourceProvider.java        # JPA/SQL entities
│       │   │   ├── JvmMetricsProvider.java           # JVM metrics (memory, threads, GC)
│       │   │   ├── HttpRequestTracker.java           # HTTP request tracking
│       │   │   ├── ScheduledTasksProvider.java       # Scheduled tasks monitoring
│       │   │   └── EnvironmentProvider.java          # Environment variables & properties
│       │   │
│       │   ├── registry/
│       │   │   └── CustomDataSourceRegistry.java     # Manual registration
│       │   │
│       │   ├── service/
│       │   │   ├── DataInspectorService.java         # Core service
│       │   │   ├── ExportService.java                # Export to CSV/JSON/Excel/HTML/MD
│       │   │   └── TelemetryService.java             # Usage analytics & telemetry
│       │   │
│       │   ├── controller/
│       │   │   ├── DataInspectorController.java      # REST API
│       │   │   └── DataInspectorViewController.java  # Web UI controller
│       │   │
│       │   └── config/
│       │       ├── DataInspectorAutoConfiguration.java   # Auto-config
│       │       └── DataInspectorProperties.java          # Properties
│       │
│       └── resources/
│           ├── META-INF/
│           │   └── spring.factories            # Enable auto-config
│           └── data-inspector-ui.html          # Beautiful web dashboard
│
├── data-inspector-scala/           # Core reusable module (Scala)
│   ├── build.gradle
│   ├── README.md                               # Scala-specific documentation
│   └── src/main/
│       ├── scala/com/example/datainspector/
│       │   ├── model/
│       │   │   ├── DataSourceInfo.scala         # Data source case class
│       │   │   └── QueryResult.scala            # Query result case class
│       │   │
│       │   ├── spi/
│       │   │   └── DataSourceProvider.scala     # Provider trait
│       │   │
│       │   ├── provider/                        # Auto-discovery providers
│       │   │   ├── JvmMetricsProvider.scala     # JVM metrics (memory, threads, GC)
│       │   │   ├── JpaDataSourceProvider.scala  # JPA/SQL entities
│       │   │   └── EnvironmentProvider.scala    # Environment & properties
│       │   │
│       │   ├── service/
│       │   │   ├── DataInspectorService.scala   # Core service
│       │   │   ├── ExportService.scala          # Export to CSV/JSON/Excel/HTML/MD
│       │   │   └── TelemetryService.scala       # Usage analytics & telemetry
│       │   │
│       │   ├── controller/
│       │   │   └── DataInspectorController.scala # REST API
│       │   │
│       │   └── config/
│       │       ├── DataInspectorAutoConfiguration.scala # Auto-config
│       │       └── DataInspectorProperties.scala        # Properties
│       │
│       └── resources/
│           └── META-INF/
│               └── spring.factories             # Enable auto-config
│
└── demo-app/                       # Demo application
    ├── build.gradle
    └── src/main/
        ├── java/com/example/demo/
        │   ├── DemoApplication.java            # Main application
        │   │
        │   ├── model/
        │   │   ├── User.java                   # User entity
        │   │   └── Product.java                # Product entity
        │   │
        │   ├── service/
        │   │   ├── UserService.java            # With @Cacheable
        │   │   └── ProductService.java         # With @Cacheable
        │   │
        │   ├── controller/
        │   │   └── DemoController.java         # REST API
        │   │
        │   └── config/
        │       ├── CacheConfig.java            # Caffeine cache setup
        │       └── CustomDataSourceConfig.java # Register custom data
        │
        └── resources/
            ├── application.properties          # Configuration
            └── banner.txt                      # Custom banner
```

## Key Components

### Data Inspector Modules

The project includes **two implementations** with identical functionality:

#### Java Implementation (`data-inspector/`)
- Standard Java with Lombok
- All providers and features listed below

#### Scala Implementation (`data-inspector-scala/`)
- Functional Scala with case classes and pattern matching
- Same features as Java version
- Scala-specific benefits: immutability, Option types, pattern matching
- See [data-inspector-scala/README.md](data-inspector-scala/README.md) for details

**Auto-Discovery Providers** (both implementations):
- `CacheDataSourceProvider` - Finds all Spring caches (Caffeine, ConcurrentMap)
- `BeanDataSourceProvider` - Exposes Spring beans and their internal state
- `MongoDBDataSourceProvider` - MongoDB stats and collections
- `JpaDataSourceProvider` - JPA entities and SQL database tables
- `JvmMetricsProvider` - JVM metrics (memory, threads, GC, CPU)
- `HttpRequestTracker` - HTTP request tracking with timing
- `ScheduledTasksProvider` - @Scheduled tasks monitoring
- `EnvironmentProvider` - Environment variables and application properties

**Core Features** (both implementations):
- Zero-config auto-configuration via `spring.factories`
- REST API for programmatic access
- Beautiful single-page web dashboard
- Pagination, filtering, and export (CSV, JSON, Excel, HTML, Markdown)
- Custom data source registration API
- Usage analytics and telemetry
- Sensitive data masking

### Demo Application

**Features Demonstrated**:
1. **Caching**: Caffeine caches for users and products
2. **Spring Beans**: Multiple services with internal state
3. **Custom Data Sources**: Sessions, request queues, in-memory lists
4. **MongoDB**: (Optional) Database and collection stats
5. **REST API**: Endpoints that populate caches

## How It Works

1. **Auto-Configuration**: Spring Boot automatically discovers the `DataInspectorAutoConfiguration` via `spring.factories`

2. **Provider Discovery**: All `DataSourceProvider` beans are autowired into `DataInspectorService`

3. **Auto-Discovery**: Each provider scans the application:
   - CacheProvider → finds all `CacheManager` caches
   - BeanProvider → introspects Spring beans
   - MongoDBProvider → queries MongoDB stats

4. **Manual Registration**: Use `CustomDataSourceRegistry` to register your own data:
   ```java
   registry.register("id", "name", "description", dataSupplier);
   ```

5. **Web UI**: Single-page React-like dashboard (vanilla JS) with:
   - Sidebar showing all data sources
   - Main content area with tables
   - Real-time refresh and export

6. **REST API**: JSON endpoints for programmatic access

## Design Principles

Following Steve Jobs' philosophy:

1. **It Just Works**
   - Single dependency, zero configuration
   - Automatic discovery of everything important
   - Works out of the box

2. **Beautiful**
   - Modern gradient UI
   - Smooth animations
   - Clean typography
   - Responsive design

3. **Simple**
   - One module to import
   - One URL to visit
   - Intuitive interface

4. **Powerful**
   - Auto-discovers all data sources
   - Extensible via SPI
   - Full REST API
   - Production-ready

## Usage in Your Project

### Step 1: Copy the module

```bash
cp -r data-inspector /your-project/modules/
```

### Step 2: Add to settings.gradle

```gradle
include 'modules:data-inspector'
```

### Step 3: Add dependency

```gradle
implementation project(':modules:data-inspector')
```

### Step 4: Run your app

```bash
./gradlew bootRun
```

### Step 5: Open dashboard

http://localhost:8080/data-inspector

That's it! Everything auto-configures.

## Customization

### Disable for production

```properties
data-inspector.enabled=false
```

### Change base path

```properties
data-inspector.basePath=/internal/debug
```

### Register custom data

```java
@Autowired
private CustomDataSourceRegistry registry;

registry.register("my-data", "My Data", "Description", () -> myCollection);
```

### Secure endpoints

```java
http.authorizeRequests()
    .antMatchers("/data-inspector/**").hasRole("ADMIN");
```

## Recent Enhancements (v1.0)

✅ **Completed**:
- Export to CSV, JSON, Excel, HTML, Markdown
- Usage analytics and telemetry tracking
- JPA/SQL database inspection
- JVM metrics monitoring
- HTTP request tracking
- Scheduled tasks monitoring
- Environment and properties inspection
- MIT license with proper headers

## Future Enhancements

Possible improvements:
- Real-time updates via WebSocket
- Advanced UI with React/Vue
- Data editing capabilities
- PDF export format
- Query builder UI
- More chart visualizations
- Alerts and notifications

---

Built as a beautiful, production-ready debugging tool for Spring Boot.
