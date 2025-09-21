# Build Guide

## Overview

This project supports **dual build systems** - both Maven and Gradle - allowing teams to choose their preferred build tool while maintaining feature parity.

## Prerequisites

- **Java 21** (LTS) - Required for Spring Boot 3.5.6
- **Docker** - For container image creation
- **Git** - Version control operations

## Build Tools Comparison (2025)

| Feature | Maven | Gradle |
|---------|-------|--------|
| **Performance** | Consistent, predictable | Faster incremental builds |
| **Learning Curve** | Easier for beginners | More flexible, complex |
| **CI/CD Integration** | Excellent | Excellent |
| **Kotlin DSL Support** | XML-based | Native Kotlin DSL |
| **Build Cache** | Limited | Advanced local/remote caching |
| **Parallel Execution** | Basic | Advanced parallel task execution |

### 2025 Recommendation

- **Choose Gradle** for: Performance-critical builds, complex multi-module projects, teams comfortable with Groovy/Kotlin DSL
- **Choose Maven** for: Predictable CI/CD pipelines, teams preferring XML configuration, enterprise environments requiring stability

## Maven Build

### Project Structure

```text
pom.xml                    # Parent POM
├── cities-domain/pom.xml  # Domain module
└── cities-web/pom.xml     # Web module
```

### Key Dependencies (Maven)

```xml
<properties>
    <kotlin.version>2.2.20</kotlin.version>
    <spring-boot.version>3.5.6</spring-boot.version>
    <java.version>21</java.version>
</properties>
```

### Build Commands

```bash
# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package (creates executable JAR)
./mvnw package

# Skip tests (faster build)
./mvnw package -Dmaven.test.skip=true

# Install to local repository
./mvnw install

# Generate test coverage report
./mvnw jacoco:report
```

### Multi-Module Build

Maven automatically handles module dependencies:

1. **cities-domain** builds first (no dependencies)
2. **cities-web** builds second (depends on cities-domain)

### Maven Profiles

```bash
# Development with PostgreSQL
./mvnw spring-boot:run -Dspring.profiles.active=postgres,seeded

# Testing with HSQLDB
./mvnw test -Dspring.profiles.active=hsql
```

## Gradle Build

### Project Structure

```text
build.gradle               # Root build script
settings.gradle           # Multi-project settings
├── cities-domain/build.gradle
└── cities-web/build.gradle
```

### Key Configuration (Gradle)

```groovy
ext {
    kotlinVersion = '2.2.20'
    springBootVersion = '3.5.6'
}

sourceCompatibility = JavaVersion.VERSION_21
targetCompatibility = JavaVersion.VERSION_21
```

### Build Commands

```bash
# Clean and build all modules
./gradlew clean build

# Run tests
./gradlew test

# Run tests for specific module
./gradlew cities-web:test

# Build without tests (faster)
./gradlew build -x test

# Generate test coverage report
./gradlew jacocoTestReport

# Continuous build (watches for changes)
./gradlew build --continuous
```

### Gradle Performance Features

```bash
# Enable build cache
./gradlew build --build-cache

# Enable parallel execution
./gradlew build --parallel

# Enable daemon (faster subsequent builds)
./gradlew build --daemon
```

### Gradle Profiles

```bash
# Development with PostgreSQL
./gradlew bootRun -Dspring.profiles.active=postgres,seeded

# Build for specific environment
./gradlew build -Penv=production
```

## Container Image Creation

### Cloud Native Buildpacks (Recommended 2025 Approach)

Both Maven and Gradle support **Cloud Native Buildpacks** - eliminating the need for Dockerfiles.

#### Benefits of Buildpacks

- **No Dockerfile required** - Automatic runtime detection
- **Optimized layering** - Efficient caching and updates
- **Security patches** - Regularly updated base images
- **Performance optimizations** - CDS and AOT compilation support
- **Consistent images** - Standardized across environments

#### Maven Buildpack Commands

```bash
# Build container image locally
./mvnw spring-boot:build-image

# Build with custom image name
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=myregistry/cities-web:1.0.0

# Build with custom registry prefix
./mvnw spring-boot:build-image -Ddocker.image.prefix=myregistry.com/myproject
```

#### Gradle Buildpack Commands

```bash
# Build container image locally
./gradlew bootBuildImage

# Build with custom image name
./gradlew bootBuildImage --imageName=myregistry/cities-web:1.0.0

# Build with registry prefix
./gradlew bootBuildImage -PdockerImagePrefix=myregistry.com/myproject
```

#### Buildpack Configuration

**Maven (pom.xml):**

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <image>
            <name>${docker.image.prefix}/${project.artifactId}:${project.version}</name>
            <tags>
                <tag>${docker.image.prefix}/${project.artifactId}:latest</tag>
            </tags>
            <env>
                <BP_JVM_VERSION>21</BP_JVM_VERSION>
            </env>
        </image>
    </configuration>
</plugin>
```

**Gradle (build.gradle):**

```groovy
bootBuildImage {
    imageName = "${dockerImagePrefix}/${project.name}:${version}"
    tags = ["${dockerImagePrefix}/${project.name}:latest"]
    environment = [
        "BP_JVM_VERSION": "21"
    ]
}
```

### Native Image Support (GraalVM)

#### Maven Native Build

```bash
# Add GraalVM plugin
./mvnw spring-boot:build-image -Pnative

# Test native executable
./mvnw -PnativeTest test
```

#### Gradle Native Build

```bash
# Build native image
./gradlew bootBuildImage --imageName=cities-web:native

# Test native executable
./gradlew nativeTest
```

## CI/CD Integration

### GitHub Actions (Recommended 2025)

**Dual Build Matrix:**

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        build-tool: [gradle, maven]

    steps:
    - uses: actions/checkout@v5

    - name: Set up JDK 21
      uses: actions/setup-java@v5
      with:
        java-version: '21'
        distribution: 'liberica'
        cache: ${{ matrix.build-tool }}

    - name: Build with Gradle
      if: matrix.build-tool == 'gradle'
      run: ./gradlew build

    - name: Build with Maven
      if: matrix.build-tool == 'maven'
      run: ./mvnw verify
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    tools {
        jdk 'jdk-21.0.8'
        gradle 'gradle-9.1.0'
    }
    stages {
        stage('Build') {
            parallel {
                stage('Maven Build') {
                    steps {
                        sh './mvnw clean verify'
                    }
                }
                stage('Gradle Build') {
                    steps {
                        sh './gradlew clean build'
                    }
                }
            }
        }
        stage('Build Images') {
            steps {
                sh './gradlew bootBuildImage'
            }
        }
    }
}
```

### Travis CI

```yaml
language: java
jdk: openjdk21

script:
  - "./gradlew clean build"

before_cache:
  - rm -f  $HOME/.gradle/caches/modules-2/modules-2.lock
  - rm -fr $HOME/.gradle/caches/*/plugin-resolution/

cache:
  directories:
    - "$HOME/.gradle/caches/"
    - "$HOME/.gradle/wrapper/"
```

## Testing Strategy

### Unit Testing

**Maven:**

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CityControllerTest

# Skip integration tests
./mvnw test -DskipITs
```

**Gradle:**

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests CityControllerTest

# Run tests continuously
./gradlew test --continuous
```

### Integration Testing

**Test Profiles:**

```bash
# Maven with PostgreSQL
./mvnw test -Dspring.profiles.active=postgres

# Gradle with HSQLDB
./gradlew test -Dspring.profiles.active=hsql
```

### Coverage Reports

**Maven (Jacoco):**

```bash
./mvnw jacoco:report
# Report: target/site/jacoco/index.html
```

**Gradle (Jacoco):**

```bash
./gradlew jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

## Build Optimization

### Maven Optimizations

```bash
# Parallel builds
./mvnw -T 4 clean install

# Offline mode (skip dependency updates)
./mvnw -o package

# Skip non-essential plugins
./mvnw package -Dcheckstyle.skip -Dspotbugs.skip
```

### Gradle Optimizations

```bash
# Enable all performance features
./gradlew build --parallel --build-cache --daemon

# Profile build performance
./gradlew build --profile

# Analyze build dependencies
./gradlew dependencies
```

### Build Cache Configuration

**Gradle (gradle.properties):**

```properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.daemon=true
```

## Dependency Management

### Version Catalog (Gradle 2025 Best Practice)

**gradle/libs.versions.toml:**

```toml
[versions]
spring-boot = "3.5.6"
kotlin = "2.2.20"

[libraries]
spring-boot-starter-web = { group = "org.springframework.boot", name = "spring-boot-starter-web" }
kotlin-reflect = { group = "org.jetbrains.kotlin", name = "kotlin-reflect" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
```

### Dependency Updates

**Maven:**

```bash
# Check for updates
./mvnw versions:display-dependency-updates

# Update versions
./mvnw versions:use-latest-versions
```

**Gradle:**

```bash
# With gradle-versions-plugin
./gradlew dependencyUpdates

# Manual check
./gradlew dependencies --configuration runtimeClasspath
```

## Build Troubleshooting

### Common Issues

1. **Java Version Mismatch**

```bash
# Check Java version
java -version
./mvnw --version
./gradlew --version
```

2. **Memory Issues**

```bash
# Maven
export MAVEN_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"

# Gradle
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
```

3. **Clean Build**

```bash
# Maven
./mvnw clean install

# Gradle
./gradlew clean build --refresh-dependencies
```

### Build Performance Monitoring

**Gradle Build Scan:**

```bash
./gradlew build --scan
```

**Maven Build Profiling:**

```bash
./mvnw clean install -Dorg.slf4j.simpleLogger.showDateTime=true
```

## Migration Between Build Tools

### Maven to Gradle

```bash
# Initialize Gradle wrapper
gradle wrapper

# Generate Gradle build from Maven
gradle init --type java-application
```

### Gradle to Maven

Use the Maven archetype plugin to create equivalent structure, then migrate dependencies manually.

### Verification Strategy

When maintaining dual builds:

1. **Compare artifacts** - Ensure both builds produce identical JARs
2. **Test compatibility** - Run same test suites on both builds
3. **Performance benchmark** - Compare build times and resource usage
4. **CI validation** - Both builds must pass in pipeline

This ensures confidence when switching between build tools or maintaining both for different environments.
