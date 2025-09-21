# Spring Boot with Kotlin and JPA Example

[![CI](https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example/actions/workflows/ci.yml/badge.svg)](https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example/actions/workflows/ci.yml) [![Known Vulnerabilities](https://snyk.io/test/github/pacphi/spring-boot-with-kotlin-and-jpa-example/badge.svg)](https://snyk.io/test/github/pacphi/spring-boot-with-kotlin-and-jpa-example)

A modern Spring Boot microservice demonstrating **cloud-native application development** with Kotlin, JPA, and comprehensive multi-platform deployment strategies.

## What's Inside

* **Spring Boot 3.5.6** with **Java 21** - Latest LTS foundation
* **Kotlin 2.2.20** - Modern, expressive language
* **Multi-module architecture** - Domain and web separation
* **Cloud Native Buildpacks** - Container images without Dockerfiles
* **Dual build support** - Both Maven and Gradle
* **Multi-platform deployments** - AWS, Azure, Google Cloud, Tanzu Platform
* **Production-ready** - Actuator, metrics, health checks

## Quick Start

### Prerequisites

* **Java 21** (LTS) - Required for Spring Boot 3.5.6
* **Docker** - For database and container builds
* **Git** - For version control

Optional but recommended:
* **HTTPie** - API testing
* **jq** - JSON processing

### Get Started in 3 Steps

```bash
# 1. Clone and build
git clone https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example.git
cd spring-boot-with-kotlin-and-jpa-example
./mvnw package  # or ./gradlew build

# 2. Start database
docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=geo_data postgres:15

# 3. Run application
./mvnw -pl cities-web spring-boot:run -Dspring.profiles.active=postgres,seeded
```

**Verify it works:**
```bash
curl http://localhost:8080/cities | jq .
```

## Architecture

This application demonstrates **domain-driven design** with clear separation:

* **cities-domain** - Business logic, entities, and repositories
* **cities-web** - REST API, controllers, and web configuration

**Key Features:**
* RESTful API with HATEOAS support
* PostgreSQL with Flyway migrations
* Comprehensive monitoring with Actuator
* Multi-environment configuration profiles
* Container-first deployment approach

[**→ See full architecture details**](docs/ARCHITECTURE.md)

## Build and Run

**Choose your build tool:**

* [**Maven**](docs/BUILD.md#maven-build) - Traditional XML-based configuration
* [**Gradle**](docs/BUILD.md#gradle-build) - Modern, flexible build system

**Quick build commands:**
```bash
# Maven
./mvnw package

# Gradle
./gradlew build
```

[**→ See complete build guide**](docs/BUILD.md)

## Deployment Options

This application showcases deployment strategies across major cloud platforms:

### 🚀 **Quick Deploy**

* **[Local Development](docs/RUN.md)** - Docker Compose, embedded database options
* **[Docker](docs/BUILD.md#container-image-creation)** - Cloud Native Buildpacks (no Dockerfile needed!)

### 🏗️ **Kubernetes & Orchestration**

* **[Minikube](docs/MINIKUBE.md)** - Local Kubernetes development with 2025 features (AI workloads, enhanced security)
* **[KOPS](docs/KOPS.md)** - Production Kubernetes on AWS with full infrastructure management
* **[Juju](docs/JUJU.md)** - Model-driven operations and multi-cloud application management

### ☁️ **Cloud Platforms (2025 Ready)**

* **[AWS EKS](docs/AWS.md)** - Latest Kubernetes 1.33+, EKS Automatic, advanced security
* **[Azure AKS](docs/AZURE.md)** - AKS Automatic, Azure Linux migration, AI/ML integration
* **[Google Cloud GKE](docs/GOOGLE.md)** - AI-powered features, TPU support, latest performance improvements
* **[Tanzu Platform](docs/CF.md)** - Cloud Foundry v10.0, simplified developer experience

### 📋 **Complete Guides**

Each deployment guide includes:
* **2025 platform updates** and new features
* **Step-by-step instructions** with code examples
* **Security best practices** and compliance
* **CI/CD pipeline examples** (GitHub Actions, Jenkins, etc.)
* **Cost optimization** strategies
* **Monitoring and troubleshooting** guides

## API Documentation

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/cities` | List all cities |
| `GET` | `/cities/{id}` | Get city by ID |
| `POST` | `/cities` | Create new city |
| `PUT` | `/cities/{id}` | Update existing city |
| `DELETE` | `/cities/{id}` | Delete city |

### Example Usage

```bash
# List cities
curl http://localhost:8080/cities | jq .

# Create city
curl -X POST http://localhost:8080/cities \
  -H "Content-Type: application/json" \
  -d '{
    "id": "NYC",
    "name": "New York City",
    "description": "The Big Apple",
    "location": {"latitude": 40.7128, "longitude": -74.0060}
  }'

# Update city
curl -X PUT http://localhost:8080/cities/NYC \
  -H "Content-Type: application/json" \
  -d '{"description": "Updated description"}'
```

### Monitoring Endpoints

Spring Boot Actuator provides production-ready monitoring:

* `/actuator/health` - Application health status
* `/actuator/metrics` - Application metrics
* `/actuator/prometheus` - Prometheus-format metrics
* `/actuator/info` - Application information

[**→ See complete API documentation**](docs/RUN.md#api-testing)

## Development Guide

### Project Structure

```
├── cities-domain/          # Business logic and entities
│   ├── src/main/kotlin/   # Domain services and repositories
│   └── src/test/kotlin/   # Domain tests
├── cities-web/            # REST API and web layer
│   ├── src/main/kotlin/   # Controllers and configuration
│   └── src/test/kotlin/   # Integration tests
├── docs/                  # Documentation
└── specs/                 # Deployment configurations
    ├── docker/           # Docker Compose files
    └── k8s/              # Kubernetes manifests
```

### Technologies Used

* **Backend**: Spring Boot 3.5.6, Kotlin 2.2.20, Java 21
* **Database**: PostgreSQL 15+ with Flyway migrations
* **Containerization**: Cloud Native Buildpacks, Docker
* **Monitoring**: Spring Actuator, Micrometer, Prometheus
* **Testing**: JUnit 5, MockK, TestContainers

[**→ See detailed architecture**](docs/ARCHITECTURE.md)

## Documentation

### 📚 **Comprehensive Guides**

#### Core Documentation

* **[Architecture](docs/ARCHITECTURE.md)** - Application design, patterns, and structure
* **[Build](docs/BUILD.md)** - Maven/Gradle builds, CI/CD, container creation
* **[Run](docs/RUN.md)** - Local development, configuration, API testing

#### Cloud Deployments

* **[AWS Deployment](docs/AWS.md)** - EKS, ECR, RDS, latest 2025 features
* **[Azure Deployment](docs/AZURE.md)** - AKS, ACR, PostgreSQL, AKS Automatic
* **[Google Cloud Deployment](docs/GOOGLE.md)** - GKE, Artifact Registry, Cloud SQL, AI features
* **[Tanzu Platform Deployment](docs/CF.md)** - Cloud Foundry v10.0, service bindings

#### Kubernetes & Orchestration

* **[Minikube](docs/MINIKUBE.md)** - Local Kubernetes development, AI workload support, security hardening
* **[KOPS](docs/KOPS.md)** - Production-grade Kubernetes on AWS, auto-scaling, monitoring
* **[Juju](docs/JUJU.md)** - Model-driven operations, charms, multi-cloud management

#### Reference

* **[Appendices](docs/APPENDICES.md)** - Tools, commands, troubleshooting, reference

### 🛠 **Quick References**

* **[Tool Installation](docs/APPENDICES.md#a-tool-prerequisites-and-installation)** - All required tools and CLIs
* **[Command Reference](docs/APPENDICES.md#b-command-reference)** - Maven, Gradle, Docker, kubectl commands
* **[Configuration Templates](docs/APPENDICES.md#c-configuration-templates)** - Ready-to-use config files
* **[Troubleshooting](docs/APPENDICES.md#d-troubleshooting-guide)** - Common issues and solutions

## Contributing

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

## What's New in 2025

This project demonstrates the latest cloud-native development practices:

* ✅ **Java 21 LTS** - Latest long-term support release
* ✅ **Spring Boot 3.5.6** - Modern Spring ecosystem
* ✅ **Cloud Native Buildpacks** - Dockerfile-free containerization
* ✅ **Multi-platform deployment** - AWS, Azure, Google Cloud, Tanzu Platform
* ✅ **Advanced Kubernetes** - Latest features across all major cloud providers
* ✅ **AI/ML Ready** - Supports latest cloud AI/ML integrations
* ✅ **Security Best Practices** - Pod Security Standards, RBAC, Zero Trust

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

* Built upon ideas from the [codecentric blog article](https://blog.codecentric.de/en/2017/06/kotlin-spring-working-jpa-data-classes/)
* Demonstrates cloud-native patterns and multi-platform deployment strategies
* Updated for 2025 with latest platform features and best practices
