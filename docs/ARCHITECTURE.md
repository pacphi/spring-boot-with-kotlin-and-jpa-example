# Architecture Overview

## Project Structure

This Spring Boot application follows a **multi-module architecture** with clear separation of concerns:

```text
spring-boot-with-kotlin-and-jpa-example/
├── cities-domain/          # Domain layer (business logic)
├── cities-web/             # Web layer (REST API)
├── docs/                   # Documentation
└── specs/                  # Deployment specifications
    ├── docker/             # Docker Compose
    └── k8s/                # Kubernetes manifests
```

## Module Dependencies

```text
cities-web ──► cities-domain
```

The web module depends on the domain module, following **dependency inversion principles**.

## Technology Stack

### Core Framework

- **Spring Boot 3.5.6** - Main application framework
- **Kotlin 2.2.20** - Primary programming language
- **Java 21** - Target JVM version

### Data Layer

- **Spring Data JPA** - ORM and data access
- **Hibernate** - JPA implementation
- **Flyway 11.13.1** - Database migration
- **PostgreSQL 42.7.8** - Primary database (runtime)
- **HSQLDB 2.7.4** - In-memory database (testing)

### Web Layer

- **Spring Web MVC** - REST API framework
- **Spring HATEOAS** - Hypermedia API support
- **Jackson Kotlin Module** - JSON serialization
- **Thymeleaf** - Template engine

### Monitoring & Observability

- **Spring Boot Actuator** - Production monitoring
- **Micrometer 1.15.4** - Metrics collection
- **Prometheus Registry** - Metrics export

## Domain Model

### City Entity

The core domain entity represents a geographical city:

```kotlin
@Entity
@Table(name = "city")
data class CityEntity(
    @Id val id: String?,
    val name: String,
    val description: String?,
    @Embedded val location: Coordinate,
    val updatedAt: LocalDateTime,
    val createdAt: LocalDateTime
)
```

### Location Model

Geographical coordinates are embedded as a value object:

```kotlin
@Embeddable
data class Coordinate(
    val latitude: Double,
    val longitude: Double
)
```

## API Design

### RESTful Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/cities` | Retrieve all cities |
| GET | `/cities/{id}` | Retrieve specific city |
| POST | `/cities` | Create new city |
| PUT | `/cities/{id}` | Update existing city |
| DELETE | `/cities/{id}` | Delete city |

### Content Types

- **JSON** (primary): `application/json`
- **XML**: `application/xml`, `text/xml`

### HATEOAS Support

Responses include hypermedia links for navigation:

```json
{
  "id": "SFO",
  "name": "San Francisco",
  "location": {
    "latitude": 37.781555,
    "longitude": -122.393990
  },
  "_links": {
    "self": {
      "href": "/cities/SFO"
    }
  }
}
```

## Data Transfer Objects (DTOs)

### API Layer DTOs

- `CityDto` - Complete city representation
- `CreateCityDto` - City creation payload
- `UpdateCityDto` - City update payload (partial)
- `CoordinateDto` - Location data

### Resource Layer

- `CityResource` - HATEOAS-enabled city representation

## Configuration Profiles

### Database Profiles

- **`postgres`** - PostgreSQL database configuration
- **`hsql`** - HSQLDB in-memory database
- **`cloud`** - Cloud Foundry PostgreSQL service
- **`k8s`** - Kubernetes environment variables

### Data Profiles

- **`seeded`** - Includes test data migration scripts

## Security Architecture

### Authentication & Authorization

- No authentication configured (demo application)
- Production deployments should add Spring Security

### Database Security

- Connection pooling via HikariCP
- Environment-based credentials
- No embedded credentials in production profiles

## Monitoring & Health Checks

### Actuator Endpoints

All actuator endpoints exposed for monitoring:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

### Key Endpoints

- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment information
- `/actuator/flyway` - Database migration status

### Metrics Collection

- **HTTP request metrics** with percentile histograms
- **SLA tracking** for response times (1ms, 5ms)
- **Prometheus integration** for metrics export

## Data Migration Strategy

### Flyway Configuration

```yaml
spring:
  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1.0
```

### Migration Locations

- **`db/migration`** - Schema migrations
- **`db/sql/test`** - Test data (seeded profile only)

## Build Architecture

### Multi-Module Build

Both Maven and Gradle support multi-module builds:

- **Parent module** - Defines common dependencies and plugins
- **Child modules** - Inherit configuration and add specific dependencies

### Artifact Generation

- **cities-domain** - JAR library
- **cities-web** - Executable JAR with embedded Tomcat

## Container Architecture

### Cloud Native Buildpacks

Uses Spring Boot's built-in buildpack support:

```bash
# Maven
./mvnw spring-boot:build-image

# Gradle
./gradlew bootBuildImage
```

### Benefits

- **No Dockerfile required** - Automatic runtime detection
- **Optimized layering** - Efficient caching and updates
- **Security patches** - Regularly updated base images
- **Performance optimizations** - CDS and AOT compilation support

## Deployment Architectures

### Local Development

```text
Application ──► PostgreSQL
    │              (Docker)
    └── Actuator ──► Prometheus
```

### Kubernetes Deployment

```text
LoadBalancer ──► Pod(cities-web) ──► PostgreSQL Service
                      │                      │
                 ConfigMap              PersistentVolume
```

### Cloud Foundry Deployment

```text
Route ──► App Instance ──► Service Binding
              │                  │
         Environment          PostgreSQL
         Variables            Service
```

## Performance Considerations

### JPA Optimization

- **Hibernate query logging** enabled in development
- **Connection pooling** via HikariCP
- **JPA metadata caching** disabled for PostgreSQL

### JSON Serialization

- **Non-empty inclusion** reduces payload size
- **Kotlin module** provides native Kotlin support

### Caching Strategy

- **Local caching** via Spring Cache abstraction
- **HTTP caching** headers for static resources
- **Database connection pooling** for performance

## Development Patterns

### Domain-Driven Design

- **Entities** represent core business objects
- **Services** encapsulate business logic
- **Repositories** handle data persistence
- **DTOs** manage data transfer across boundaries

### Dependency Injection

- **Constructor injection** (Kotlin primary constructors)
- **Interface-based services** for testability
- **Configuration classes** for environment setup

### Error Handling

- **Global exception handling** via @ControllerAdvice
- **HTTP status codes** for API responses
- **Validation** using Bean Validation (JSR-303)

## Testing Strategy

### Unit Testing

- **JUnit 5** for test framework
- **Mockito Kotlin** for mocking
- **AssertJ** for fluent assertions

### Integration Testing

- **Spring Boot Test** for application context
- **TestContainers** for database integration
- **Jacoco** for test coverage reporting

### Test Profiles

- **HSQLDB** for fast in-memory testing
- **PostgreSQL** for integration testing
- **Seeded data** for comprehensive testing
