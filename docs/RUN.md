# Running the Application

## Prerequisites

Ensure you have the following tools installed:

- **Java 21** (LTS) - OpenJDK or compatible JVM
- **Docker** - For database and containerized deployments
- **HTTPie** (optional) - For API testing
- **jq** (optional) - For JSON response processing

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example.git
cd spring-boot-with-kotlin-and-jpa-example

# Build with Maven
./mvnw package

# OR build with Gradle
./gradlew build
```

### 2. Start Database

```bash
# PostgreSQL with Docker
docker run -d \
  --name postgres-cities \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=geo_data \
  postgres:15
```

### 3. Run Application

```bash
# With Maven
./mvnw -pl cities-web spring-boot:run -Dspring.profiles.active=postgres,seeded

# With Gradle
./gradlew cities-web:bootRun -Dspring.profiles.active=postgres,seeded

# OR run JAR directly
java -Dspring.profiles.active=postgres,seeded -jar cities-web/target/cities-web-1.0.0-SNAPSHOT.jar
```

### 4. Verify Application

```bash
# Check health
curl http://localhost:8080/actuator/health

# List cities
curl http://localhost:8080/cities | jq .
```

## Database Configuration

### PostgreSQL (Recommended)

**Docker Setup:**

```bash
# Start PostgreSQL
docker run -d \
  --name postgres-cities \
  -p 5432:5432 \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=geo_data \
  -v postgres_data:/var/lib/postgresql/data \
  postgres:15

# Verify connection
docker exec -it postgres-cities psql -U postgres -d geo_data -c "SELECT version();"
```

**Direct Installation:**

```bash
# macOS with Homebrew
brew install postgresql@15
brew services start postgresql@15

# Ubuntu/Debian
sudo apt-get install postgresql-15 postgresql-client-15

# Create database
createdb geo_data
```

### HSQLDB (Testing)

For development and testing, you can use in-memory HSQLDB:

```bash
# No setup required - runs in memory
./mvnw spring-boot:run -Dspring.profiles.active=hsql,seeded
```

## Application Profiles

### Available Profiles

| Profile | Database | Purpose |
|---------|----------|---------|
| `postgres` | PostgreSQL | Production-like development |
| `hsql` | HSQLDB | Fast testing/development |
| `cloud` | Service-bound | Cloud Foundry deployments |
| `k8s` | Environment vars | Kubernetes deployments |
| `seeded` | Any | Includes test data |

### Profile Combinations

```bash
# Development with PostgreSQL and test data
-Dspring.profiles.active=postgres,seeded

# Testing with in-memory database
-Dspring.profiles.active=hsql,seeded

# Production with PostgreSQL (no test data)
-Dspring.profiles.active=postgres

# Kubernetes deployment
-Dspring.profiles.active=k8s
```

## Running Options

### Maven

```bash
# Standard run with profiles
./mvnw -pl cities-web spring-boot:run -Dspring.profiles.active=postgres,seeded

# With custom JVM options
./mvnw -pl cities-web spring-boot:run -Dspring.profiles.active=postgres \
  -Dspring-boot.run.jvmArguments="-Xmx1g -Dserver.port=8081"

# Debug mode
./mvnw -pl cities-web spring-boot:run -Dspring.profiles.active=postgres \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Gradle

```bash
# Standard run
./gradlew cities-web:bootRun -Dspring.profiles.active=postgres,seeded

# With arguments
./gradlew cities-web:bootRun --args='--spring.profiles.active=postgres --server.port=8081'

# Debug mode
./gradlew cities-web:bootRun --debug-jvm
```

### JAR Execution

```bash
# Build executable JAR first
./mvnw package  # Maven
# OR
./gradlew build  # Gradle

# Run JAR (Maven)
java -Dspring.profiles.active=postgres,seeded \
  -jar cities-web/target/cities-web-1.0.0-SNAPSHOT.jar

# Run JAR (Gradle)
java -Dspring.profiles.active=postgres,seeded \
  -jar cities-web/build/libs/cities-web-1.0.0-SNAPSHOT-exec.jar
```

## Docker Compose

### Complete Development Stack

Create or use the provided `docker-compose.yml`:

```yaml
version: '3.8'
services:
  app:
    image: pivotalio/cities-web:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=postgres
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/geo_data
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=geo_data
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

**Usage:**

```bash
# Start complete stack
cd cities-web/specs/docker
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop stack
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Environment Variables

### Database Configuration

```bash
# PostgreSQL connection
export POSTGRES_HOST=localhost
export POSTGRES_DB=geo_data
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres

# Application settings
export SERVER_PORT=8080
export MANAGEMENT_PORT=8080
```

### Kubernetes Environment

```bash
# Set via ConfigMap or Secret
export POSTGRES_HOST=postgres-service
export POSTGRES_DB=geo_data
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=secretpassword
```

## Data Management

### Database Initialization

The application uses **Flyway** for database migration:

```sql
-- V1__create_city_table.sql
CREATE TABLE city (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    updated_at TIMESTAMP,
    created_at TIMESTAMP
);
```

### Test Data Loading

With the `seeded` profile, sample data is loaded:

```sql
-- test-data.sql
INSERT INTO city (id, name, description, latitude, longitude, updated_at, created_at)
VALUES ('SFO', 'San Francisco', 'City by the Bay', 37.781555, -122.393990, NOW(), NOW());
```

### Manual Data Management

```bash
# Connect to PostgreSQL
docker exec -it postgres-cities psql -U postgres -d geo_data

# View tables
\dt

# Query cities
SELECT * FROM city;

# Check migration status
SELECT * FROM flyway_schema_history;
```

## API Testing

### Basic Operations

```bash
# Get all cities
curl http://localhost:8080/cities

# Get specific city
curl http://localhost:8080/cities/SFO

# Create new city
curl -X POST http://localhost:8080/cities \
  -H "Content-Type: application/json" \
  -d '{
    "id": "NYC",
    "name": "New York City",
    "description": "The Big Apple",
    "location": {
      "latitude": 40.712776,
      "longitude": -74.005974
    }
  }'

# Update city
curl -X PUT http://localhost:8080/cities/NYC \
  -H "Content-Type: application/json" \
  -d '{
    "name": "New York",
    "description": "Updated description"
  }'

# Delete city
curl -X DELETE http://localhost:8080/cities/NYC
```

### Using HTTPie

```bash
# Install HTTPie
pip install httpie

# Get all cities with pretty output
http GET localhost:8080/cities

# Create city with HTTPie
http POST localhost:8080/cities \
  id=LAX \
  name="Los Angeles" \
  description="City of Angels" \
  location:='{"latitude": 34.052235, "longitude": -118.243683}'

# Update with partial data
http PUT localhost:8080/cities/LAX description="Updated LA description"
```

## Monitoring and Health Checks

### Actuator Endpoints

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health with components
curl http://localhost:8080/actuator/health | jq .

# Application info
curl http://localhost:8080/actuator/info

# Metrics
curl http://localhost:8080/actuator/metrics

# Environment properties
curl http://localhost:8080/actuator/env

# Database flyway status
curl http://localhost:8080/actuator/flyway
```

### Prometheus Metrics

```bash
# Prometheus format metrics
curl http://localhost:8080/actuator/prometheus

# Specific metric
curl http://localhost:8080/actuator/metrics/http.server.requests
```

### Application Logs

```bash
# Follow logs (when running via Maven/Gradle)
# Logs appear in console output

# Container logs
docker logs -f cities-web-container

# Docker Compose logs
docker-compose logs -f app
```

## Development Configuration

### IDE Setup

**IntelliJ IDEA:**

1. Import as Maven/Gradle project
2. Set Project SDK to Java 21
3. Enable Kotlin plugin
4. Configure run configuration with profiles: `postgres,seeded`

**VS Code:**

1. Install Java Extension Pack
2. Install Kotlin Language extension
3. Open project folder
4. Use integrated terminal for running commands

### Hot Reload

**Spring Boot DevTools** (for development):

Add to `cities-web/pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Or to `cities-web/build.gradle`:

```groovy
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### Custom Configuration

**application-local.yml** (for local overrides):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/geo_data  # Custom port
  jpa:
    show-sql: true
logging:
  level:
    io.pivotal: TRACE
server:
  port: 8081
```

**Usage:**

```bash
./mvnw spring-boot:run -Dspring.profiles.active=local,postgres,seeded
```

## Troubleshooting

### Common Issues

**Database Connection Failed:**

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check port availability
netstat -an | grep 5432

# Test connection manually
psql -h localhost -p 5432 -U postgres -d geo_data
```

**Port Already in Use:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
./mvnw spring-boot:run -Dserver.port=8081
```

**Migration Failures:**

```bash
# Check Flyway status
curl http://localhost:8080/actuator/flyway

# Clean database (development only)
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

### Performance Tuning

**JVM Options:**

```bash
# Production-like settings
java -Xmx2g -Xms1g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Dspring.profiles.active=postgres \
  -jar cities-web/target/cities-web-1.0.0-SNAPSHOT.jar
```

**Database Connection Pool:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

### Testing Different Scenarios

**High Load Testing:**

```bash
# Install Apache Bench
apt-get install apache2-utils

# Test API endpoint
ab -n 1000 -c 10 http://localhost:8080/cities
```

**Memory Usage:**

```bash
# Monitor with JVisualVM
jvisualvm

# Or use built-in tools
curl http://localhost:8080/actuator/metrics/jvm.memory.used
```

This guide covers all aspects of running the application locally, from basic setup to advanced configuration and troubleshooting.
