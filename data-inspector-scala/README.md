# Data Inspector - Scala Implementation

A Scala implementation of the Data Inspector library for Spring Boot applications. This provides the same functionality as the Java version but leverages Scala's powerful type system and functional programming features.

## Features

All features from the Java version, implemented in idiomatic Scala:

- **Automatic Data Source Discovery**: JPA entities, MongoDB collections, JVM metrics, environment variables
- **Powerful Query API**: Filter, paginate, and explore data sources via REST API
- **Multiple Export Formats**: CSV, JSON, Excel, HTML, Markdown
- **Usage Analytics**: Track queries, exports, and user activity
- **Production-Ready**: Thread-safe, privacy-first, configurable

## Quick Start

### 1. Add Dependency

Add to your Scala Spring Boot project's `build.gradle`:

```gradle
dependencies {
    implementation project(':data-inspector-scala')
}
```

Or in Maven `pom.xml`:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>data-inspector-scala</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Auto-Configuration

Data Inspector automatically configures itself! Just add the dependency and it works.

### 3. Access the API

Visit these endpoints:
- `http://localhost:8080/data-inspector/api/datasources` - List all data sources
- `http://localhost:8080/data-inspector/api/datasources/{id}/query` - Query a data source
- `http://localhost:8080/data-inspector/api/datasources/{id}/export?format=csv` - Export data
- `http://localhost:8080/data-inspector/api/analytics` - View usage analytics

## Configuration

In `application.properties`:

```properties
# Enable/disable Data Inspector
data-inspector.enabled=true

# Base path
data-inspector.base-path=/data-inspector

# Feature toggles
data-inspector.telemetry-enabled=true
data-inspector.export-enabled=true
data-inspector.jpa-enabled=true
data-inspector.jvm-metrics-enabled=true
data-inspector.environment-enabled=true

# Export configuration
data-inspector.max-export-records=10000
```

## Scala-Specific Features

### Case Classes and Pattern Matching

The Scala version uses case classes for immutable data models:

```scala
case class DataSourceInfo(
  id: String,
  name: String,
  `type`: String,
  description: String,
  size: Long,
  metadata: Map[String, AnyRef] = Map.empty,
  queryable: Boolean = true
)

case class QueryResult(
  dataSourceId: String,
  data: List[Map[String, AnyRef]],
  totalCount: Long,
  limit: Int = 100,
  offset: Int = 0,
  stats: Map[String, AnyRef] = Map.empty
)
```

### Functional Programming

Data source providers use functional patterns:

```scala
override def query(dataSourceId: String, filters: Map[String, AnyRef],
                   limit: Int, offset: Int): QueryResult = {
  dataSourceId match {
    case "jvm:memory" => queryMemory()
    case "jvm:threads" => queryThreads(limit, offset)
    case _ => QueryResult(dataSourceId, List.empty, 0, limit, offset)
  }
}
```

### Java Interoperability

Full Java interop for Spring integration:

```scala
@Component
class JvmMetricsProvider extends DataSourceProvider {
  // Scala collections internally
  override def discoverDataSources(): List[DataSourceInfo] = { ... }

  // Java interop method
  override def discoverDataSources: java.util.List[DataSourceInfo] =
    discoverDataSources().asJava
}
```

## Available Data Sources

### JVM Metrics
- `jvm:memory` - Heap/non-heap memory usage
- `jvm:threads` - Thread information and deadlock detection
- `jvm:runtime` - JVM uptime and version info
- `jvm:gc` - Garbage collection statistics
- `jvm:system` - OS and CPU information

### Environment
- `env:properties` - Application properties (sensitive values masked)
- `env:system` - JVM system properties
- `env:variables` - Environment variables
- `env:profiles` - Active Spring profiles

### JPA (if configured)
- `jpa:overview` - All entities overview
- `jpa:entity:{name}` - Individual entity data

## Creating Custom Providers

Implement the `DataSourceProvider` trait:

```scala
@Component
class CustomScalaProvider extends DataSourceProvider {

  override def discoverDataSources(): List[DataSourceInfo] = {
    List(
      DataSourceInfo(
        id = "custom:data",
        name = "My Custom Data",
        `type` = "custom",
        description = "Custom data source in Scala",
        size = 100
      )
    )
  }

  override def query(dataSourceId: String, filters: Map[String, AnyRef],
                     limit: Int, offset: Int): QueryResult = {
    val data = List(
      Map[String, AnyRef]("key" -> "value1", "count" -> Int.box(42)),
      Map[String, AnyRef]("key" -> "value2", "count" -> Int.box(84))
    )

    QueryResult(dataSourceId, data, data.size, limit, offset)
  }

  override def supports(dataSourceId: String): Boolean = {
    dataSourceId != null && dataSourceId.startsWith("custom:")
  }
}
```

## Export API Examples

### Export to CSV
```bash
curl "http://localhost:8080/data-inspector/api/datasources/jvm:memory/export?format=csv" \
  -o memory.csv
```

### Export to JSON
```bash
curl "http://localhost:8080/data-inspector/api/datasources/jvm:threads/export?format=json" \
  -o threads.json
```

### Export with Filters
```bash
curl -X POST "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=excel" \
  -H "Content-Type: application/json" \
  -d '{"status": "active"}' \
  -o active_users.csv
```

## Analytics API

### Get Overall Analytics
```bash
curl "http://localhost:8080/data-inspector/api/analytics"
```

Response:
```json
{
  "totalQueries": 150,
  "totalExports": 25,
  "uptimeSeconds": 3600,
  "dailyActiveUsers": 5,
  "topDataSources": {
    "jvm:memory": 50,
    "jpa:entity:User": 30
  },
  "exportFormats": {
    "csv": 15,
    "json": 10
  }
}
```

### Get Period Analytics
```bash
curl "http://localhost:8080/data-inspector/api/analytics/period?hours=24"
```

## Architecture

### Scala Components

```
data-inspector-scala/
├── model/
│   ├── DataSourceInfo.scala      # Case class for data source metadata
│   └── QueryResult.scala          # Case class for query results
├── spi/
│   └── DataSourceProvider.scala   # Trait for data source providers
├── provider/
│   ├── JvmMetricsProvider.scala   # JVM monitoring
│   ├── JpaDataSourceProvider.scala # JPA entity inspection
│   └── EnvironmentProvider.scala  # Properties & env vars
├── service/
│   ├── DataInspectorService.scala # Core service
│   ├── ExportService.scala        # Multi-format export
│   └── TelemetryService.scala     # Usage tracking
├── controller/
│   └── DataInspectorController.scala # REST API
└── config/
    ├── DataInspectorProperties.scala # Configuration
    └── DataInspectorAutoConfiguration.scala # Spring auto-config
```

## Scala Version Benefits

1. **Immutable by Default**: Case classes and immutable collections
2. **Pattern Matching**: Clean, type-safe conditional logic
3. **Functional Style**: Higher-order functions, map/filter/fold
4. **Type Safety**: Scala's powerful type system catches errors at compile time
5. **Conciseness**: Less boilerplate than Java
6. **Java Interop**: Works seamlessly with Spring Boot and Java libraries

## Performance

The Scala implementation has comparable performance to the Java version:
- Concurrent data structures for thread safety
- Lazy evaluation where appropriate
- Efficient collection operations
- Same underlying JVM optimizations

## Testing

Run tests:
```bash
./gradlew :data-inspector-scala:test
```

Run demo:
```bash
./gradlew :data-inspector-scala:bootRun
```

## License

MIT License - see LICENSE file in the project root.

## Contributing

We welcome contributions! Please see CONTRIBUTING.md for guidelines.

## Comparison with Java Version

| Feature | Java Version | Scala Version |
|---------|-------------|---------------|
| Spring Boot Integration | ✅ | ✅ |
| Auto-configuration | ✅ | ✅ |
| REST API | ✅ | ✅ |
| Export Formats | ✅ | ✅ |
| Telemetry | ✅ | ✅ |
| JVM Metrics | ✅ | ✅ |
| JPA Support | ✅ | ✅ |
| Pattern Matching | ❌ | ✅ |
| Case Classes | ❌ | ✅ |
| Functional Style | Partial | ✅ |
| Immutability | Manual | Default |

## Support

- 📖 Documentation: See main README.md
- 🐛 Issues: https://github.com/anthropics/claude-code/issues
- 💬 Discussions: GitHub Discussions

## Roadmap

Future enhancements:
- [ ] Scala 3 support
- [ ] ZIO integration
- [ ] Cats Effect support
- [ ] More functional programming patterns
- [ ] GraphQL API option
