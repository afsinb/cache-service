# Cache Service - Sample In-Memory Cache for Self-Healing Monitoring

An in-memory cache service with intentional memory leak for testing self-healing system.

## Features

- Simple key-value cache API
- Prometheus metrics
- Structured logging
- Intentional issues:
  - No cache eviction policy (memory leak)
  - Continuous cache growth
  - Hit/miss ratio tracking

## Running Locally

```bash
mvn clean package
java -jar target/cache-service-1.0.0.jar
```

## API Endpoints

- `GET /api/cache/{key}` - Get value from cache
- `POST /api/cache/{key}` - Put value in cache
- `DELETE /api/cache` - Clear cache
- `GET /api/cache/health` - Health check
- `GET /api/cache/stats` - Statistics
- `GET /actuator/prometheus` - Prometheus metrics

## Self-Healing Configuration

```yaml
system:
  name: cache-service

metrics:
  endpoint: http://localhost:8083/actuator/prometheus

logs:
  location: logs/cache-service.log

thresholds:
  memory_percent: 80
  cache_entries: 5000
```

## Example Usage

```bash
# Put value
curl -X POST http://localhost:8083/api/cache/user:123 \
  -H "Content-Type: application/json" \
  -d '"John Doe"'

# Get value
curl http://localhost:8083/api/cache/user:123

# Health check
curl http://localhost:8083/api/cache/health

# Clear cache
curl -X DELETE http://localhost:8083/api/cache
```

## Expected Behavior

Memory usage will continuously grow as entries are added every 2 seconds.
Self-healing system should detect this memory leak and trigger a restart.
