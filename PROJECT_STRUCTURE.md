# Project Structure

## Overview

This is a multi-module Gradle project with a reusable Data Inspector library and a demo application.

```
data-inspector-demo/
├── build.gradle                    # Root build configuration
├── settings.gradle                 # Module definitions
├── README.md                       # Main documentation
├── QUICKSTART.md                   # Quick start guide
│
├── data-inspector/                 # Core reusable module
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
│       │   │   └── MongoDBDataSourceProvider.java    # MongoDB integration
│       │   │
│       │   ├── registry/
│       │   │   └── CustomDataSourceRegistry.java     # Manual registration
│       │   │
│       │   ├── service/
│       │   │   └── DataInspectorService.java         # Core service
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

### Data Inspector Module

**Auto-Discovery Providers**:
- `CacheDataSourceProvider` - Finds all Spring caches (Caffeine, ConcurrentMap)
- `BeanDataSourceProvider` - Exposes Spring beans and their internal state
- `MongoDBDataSourceProvider` - MongoDB stats and collections

**Core Features**:
- Zero-config auto-configuration via `spring.factories`
- REST API for programmatic access
- Beautiful single-page web dashboard
- Pagination, filtering, and JSON export
- Custom data source registration API

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

## Future Enhancements

Possible improvements:
- Real-time updates via WebSocket
- Search/filter UI
- Data editing capabilities
- Export to CSV/Excel
- Query builder UI
- Performance metrics
- Request tracing integration

---

Built as a beautiful, production-ready debugging tool for Spring Boot.
