# Quick Start Guide

## Prerequisites

- Java 17 or higher
- Gradle 8.0+ (or use included wrapper)
- Optional: MongoDB running on localhost:27017

## Building the Project

### Option 1: Using Gradle

```bash
# Build both modules
./gradlew build

# Run the demo application
./gradlew :demo-app:bootRun
```

### Option 2: Using your IDE

1. Open the project in IntelliJ IDEA or Eclipse
2. Import as a Gradle project
3. Run `DemoApplication.java` in the demo-app module

### Option 3: Without MongoDB

If you don't have MongoDB running, simply comment out the MongoDB dependency:

**demo-app/build.gradle**:
```gradle
dependencies {
    // implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
}
```

**demo-app/src/main/resources/application.properties**:
```properties
# spring.data.mongodb.uri=mongodb://localhost:27017/demo
```

## Testing the Data Inspector

### 1. Start the application

The application will start on port 8080. You should see:

```
==================================================
  Data Inspector Enabled
  Dashboard: http://localhost:8080/data-inspector
  API: http://localhost:8080/data-inspector/api
==================================================
```

### 2. Populate some caches

Open your browser and visit these URLs to populate the caches:

- http://localhost:8080/api/users
- http://localhost:8080/api/users/1
- http://localhost:8080/api/products
- http://localhost:8080/api/products/1

### 3. Open the Data Inspector

Navigate to: **http://localhost:8080/data-inspector**

You should see:
- **Caches**: users, products, orders (with cached data)
- **Application Beans**: UserService, ProductService, DemoController, etc.
- **Custom Data Sources**: In-Memory Users, Active Sessions, Request Queue
- **MongoDB** (if running): Database stats and collections

### 4. Explore your data

Click on any data source in the sidebar to:
- View all data in a beautiful table
- Filter and search
- Paginate through large datasets
- Export to JSON
- See real-time statistics

## API Examples

### List all data sources

```bash
curl http://localhost:8080/data-inspector/api/datasources | jq
```

### Query a cache

```bash
curl "http://localhost:8080/data-inspector/api/datasources/cache:users/query?limit=10" | jq
```

### Query Spring beans

```bash
curl "http://localhost:8080/data-inspector/api/datasources/beans:application/query" | jq
```

### Query custom data source

```bash
curl "http://localhost:8080/data-inspector/api/datasources/custom:session-cache/query" | jq
```

## Customization

### Register your own data

In any `@Configuration` class:

```java
@Autowired
private CustomDataSourceRegistry registry;

@Bean
public CommandLineRunner myData() {
    return args -> {
        registry.register(
            "my-data",
            "My Custom Data",
            "Description here",
            () -> myList
        );
    };
}
```

### Configure the inspector

**application.properties**:
```properties
# Disable if needed
data-inspector.enabled=true

# Customize path
data-inspector.basePath=/debug
```

## Troubleshooting

### Port 8080 already in use

Change the port in `application.properties`:
```properties
server.port=9090
```

### MongoDB connection error

Either:
1. Start MongoDB: `mongod --dbpath /path/to/data`
2. Or disable MongoDB in `build.gradle` (see Option 3 above)

### Build errors

Ensure you have Java 17+:
```bash
java -version
```

## What's Next?

- Add this to your existing Spring Boot project
- Register your own data sources
- Secure the endpoints with Spring Security
- Deploy to production with `data-inspector.enabled=false`

Enjoy debugging with style! 🚀
