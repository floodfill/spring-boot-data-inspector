# Export and Analytics Features

Data Inspector now includes powerful export capabilities and comprehensive usage analytics.

## Export Capabilities

Export your data in multiple formats with a simple API call.

### Supported Formats

- **CSV** - Comma-separated values (Excel compatible)
- **JSON** - Pretty-printed JSON format
- **Excel** - CSV with UTF-8 BOM for Excel
- **HTML** - Formatted HTML table with styling
- **Markdown** - Markdown table format

### Export API

#### GET Export (Simple)
```bash
# Export as CSV (default)
GET /data-inspector/api/datasources/{id}/export

# Export as JSON
GET /data-inspector/api/datasources/{id}/export?format=json

# Export as Excel
GET /data-inspector/api/datasources/{id}/export?format=excel

# Export as HTML
GET /data-inspector/api/datasources/{id}/export?format=html

# Export as Markdown
GET /data-inspector/api/datasources/{id}/export?format=markdown

# With pagination
GET /data-inspector/api/datasources/{id}/export?format=csv&limit=5000&offset=0
```

#### POST Export (With Filters)
```bash
POST /data-inspector/api/datasources/{id}/export?format=json
Content-Type: application/json

{
  "filters": {
    "status": "active",
    "role": "admin"
  },
  "limit": 1000,
  "offset": 0
}
```

### Export Examples

#### Export JPA Entity to CSV
```bash
curl -o users.csv \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=csv"
```

#### Export JVM Memory Stats to JSON
```bash
curl -o memory.json \
  "http://localhost:8080/data-inspector/api/datasources/jvm:memory/export?format=json"
```

#### Export MongoDB Collection to Excel
```bash
curl -o products.csv \
  "http://localhost:8080/data-inspector/api/datasources/mongodb:collection:products/export?format=excel"
```

#### Export with Filters
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"status":"active"}' \
  -o active_users.csv \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=csv"
```

### Export from UI

You can also export directly from the Data Inspector UI:

1. Navigate to a data source
2. Click the "Export" button
3. Select your preferred format
4. Download begins automatically

### Configuration

```properties
# Enable/disable export functionality
data-inspector.exportEnabled=true

# Maximum records to export at once (default: 10000)
data-inspector.maxExportRecords=10000
```

## Analytics & Telemetry

Track usage patterns to optimize your debugging workflow and understand system behavior.

### What is Tracked

- **Queries**: Which data sources are queried most frequently
- **Exports**: Which export formats are most popular
- **Views**: UI page views
- **Errors**: Errors encountered during operations
- **Daily Active Users**: Unique users per day

### Privacy

- **No Personal Data**: Only anonymous usage statistics
- **Local Only**: All data stored in-memory, never sent externally
- **Optional**: Can be disabled anytime
- **User IDs**: Optional, uses "anonymous" if not provided

### Analytics API

#### Get Overall Analytics
```bash
GET /data-inspector/api/analytics

Response:
{
  "totalQueries": 1523,
  "totalExports": 234,
  "uptimeSeconds": 86400,
  "dailyActiveUsers": 15,
  "topDataSources": {
    "jpa:entity:User": 542,
    "jvm:memory": 321,
    "cache:products": 156
  },
  "exportFormats": {
    "csv": 150,
    "json": 84
  },
  "recentEvents": [...],
  "eventCountsByType": {
    "QUERY": 1523,
    "EXPORT": 234,
    "VIEW": 789,
    "ERROR": 12
  }
}
```

#### Get Analytics for Time Period
```bash
# Last 24 hours (default)
GET /data-inspector/api/analytics/period

# Last 7 days
GET /data-inspector/api/analytics/period?hours=168

# Last hour
GET /data-inspector/api/analytics/period?hours=1
```

#### Reset Analytics (Admin)
```bash
POST /data-inspector/api/analytics/reset
```

### Tracking Your Usage

To track specific users, add the `X-User-Id` header to your requests:

```bash
curl -H "X-User-Id: alice@example.com" \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/query"
```

### Configuration

```properties
# Enable/disable telemetry (default: true)
data-inspector.telemetryEnabled=true

# If disabled, no tracking occurs and analytics endpoints return empty data
```

### Use Cases for Analytics

1. **Optimize Performance**: Identify heavily-used data sources and optimize queries
2. **Resource Planning**: Understand peak usage times
3. **Feature Usage**: See which export formats are most popular
4. **Error Tracking**: Identify common errors and failure points
5. **User Behavior**: Understand how developers use the tool

### Analytics Dashboard Example

Create a simple monitoring dashboard:

```javascript
// Fetch analytics every 60 seconds
setInterval(async () => {
  const analytics = await fetch('/data-inspector/api/analytics').then(r => r.json());

  console.log('Total Queries:', analytics.totalQueries);
  console.log('Top Data Source:', Object.keys(analytics.topDataSources)[0]);
  console.log('Active Users Today:', analytics.dailyActiveUsers);
}, 60000);
```

### Programmatic Export Example

```javascript
// Export all active users to CSV
async function exportActiveUsers() {
  const response = await fetch('/data-inspector/api/datasources/jpa:entity:User/export?format=csv', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': 'admin@example.com'
    },
    body: JSON.stringify({
      filters: { status: 'active' },
      limit: 5000
    })
  });

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'active_users.csv';
  a.click();
}
```

## Advanced Export Options

### Batch Export

Export multiple data sources:

```bash
#!/bin/bash

# Export all JPA entities
for entity in User Product Order; do
  curl -o "${entity}.csv" \
    "http://localhost:8080/data-inspector/api/datasources/jpa:entity:${entity}/export?format=csv"
done
```

### Scheduled Export

Use cron to export data daily:

```cron
# Export users daily at 2 AM
0 2 * * * curl -o /backups/users-$(date +\%Y\%m\%d).csv \
  "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=csv"
```

### Export Pipeline

Combine export with data processing:

```bash
# Export users, filter active, count
curl -s "http://localhost:8080/data-inspector/api/datasources/jpa:entity:User/export?format=json" \
  | jq '.data[] | select(.status=="active")' \
  | wc -l
```

## Security Considerations

### Export Access Control

Secure export endpoints with Spring Security:

```java
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/data-inspector/api/*/export").hasRole("ADMIN")
            .requestMatchers("/data-inspector/api/analytics/**").hasRole("ADMIN")
            .requestMatchers("/data-inspector/**").hasRole("DEVELOPER")
        );
        return http.build();
    }
}
```

### Rate Limiting

Implement rate limiting for export endpoints to prevent abuse:

```java
@Component
public class ExportRateLimiter implements Filter {
    private final Map<String, AtomicInteger> requests = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String uri = ((HttpServletRequest) request).getRequestURI();
        if (uri.contains("/export")) {
            String ip = request.getRemoteAddr();
            int count = requests.computeIfAbsent(ip, k -> new AtomicInteger(0)).incrementAndGet();

            if (count > 100) { // Max 100 exports per IP
                throw new TooManyRequestsException();
            }
        }
        chain.doFilter(request, response);
    }
}
```

## Troubleshooting

### Export Too Large

If exports are timing out:

1. Reduce limit: `?limit=1000`
2. Use pagination: Export in batches
3. Add filters to reduce dataset
4. Increase `max-export-records` setting

### Analytics Not Updating

Check that telemetry is enabled:
```properties
data-inspector.telemetryEnabled=true
```

### Missing User IDs

Add `X-User-Id` header to track specific users:
```bash
curl -H "X-User-Id: alice" "http://localhost:8080/data-inspector/api/..."
```

## Performance Tips

1. **Use appropriate formats**: CSV is fastest for large datasets
2. **Paginate large exports**: Export in chunks of 1000-5000 records
3. **Filter data**: Use filters to reduce dataset size
4. **Schedule off-peak**: Run large exports during low-traffic periods
5. **Cache analytics**: Analytics data is computed on-demand, cache if needed

## Examples Repository

See the demo application for complete working examples of all export and analytics features.

---

**Need help?** Check the main README or open an issue on GitHub.
