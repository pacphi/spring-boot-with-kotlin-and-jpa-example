# Appendices

## A. Tool Prerequisites and Installation

### Core Development Tools

#### Java 21 (Required)

**OpenJDK Installation:**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk

# macOS with Homebrew
brew install openjdk@21

# Windows with Chocolatey
choco install openjdk21

# Verify installation
java -version
javac -version
```

**Alternative JDK Distributions:**

- **Eclipse Temurin**: <https://adoptium.net/>
- **Amazon Corretto**: <https://aws.amazon.com/corretto/>
- **BellSoft Liberica**: <https://bell-sw.com/pages/downloads/>
- **GraalVM**: <https://www.graalvm.org/>
- **Oracle JDK**: <https://www.oracle.com/java/technologies/downloads/>

#### Docker (Required)

```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# macOS
brew install --cask docker

# Windows
# Download Docker Desktop from https://www.docker.com/products/docker-desktop

# Verify installation
docker --version
docker-compose --version
```

#### Git (Required)

```bash
# Ubuntu/Debian
sudo apt install git

# macOS
brew install git

# Windows
choco install git

# Configure Git
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### Database Tools

#### PostgreSQL Client

```bash
# Ubuntu/Debian
sudo apt install postgresql-client

# macOS
brew install postgresql

# Windows
choco install postgresql

# Verify installation
psql --version
```

#### Database GUI Tools (Optional)

- **pgAdmin**: <https://www.pgadmin.org/>
- **DBeaver**: <https://dbeaver.io/>
- **DataGrip**: <https://www.jetbrains.com/datagrip/>

### HTTP Testing Tools

#### HTTPie (Recommended)

```bash
# Using pip
pip install httpie

# Ubuntu/Debian
sudo apt install httpie

# macOS
brew install httpie

# Windows
choco install httpie

# Verify installation
http --version

# Basic usage
http GET localhost:8080/cities
http POST localhost:8080/cities name="New City" latitude:=40.7 longitude:=-74.0
```

#### cURL (Alternative)

```bash
# Usually pre-installed on most systems
curl --version

# Basic usage
curl -X GET http://localhost:8080/cities
curl -X POST http://localhost:8080/cities \
  -H "Content-Type: application/json" \
  -d '{"name": "New City", "latitude": 40.7, "longitude": -74.0}'
```

### JSON Processing

#### jq (Recommended)

```bash
# Ubuntu/Debian
sudo apt install jq

# macOS
brew install jq

# Windows
choco install jq

# Usage examples
curl http://localhost:8080/cities | jq .
curl http://localhost:8080/cities | jq '.[0].name'
curl http://localhost:8080/cities | jq 'map(select(.name | contains("San")))'
```

### Container Orchestration Tools

#### kubectl

```bash
# Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# macOS
brew install kubectl

# Windows
choco install kubernetes-cli

# Verify installation
kubectl version --client
```

#### minikube

```bash
# Linux
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# macOS
brew install minikube

# Windows
choco install minikube

# Verify installation
minikube version
```

#### KOPS (Kubernetes Operations)

```bash
# Linux
curl -Lo kops https://github.com/kubernetes/kops/releases/latest/download/kops-linux-amd64
chmod +x kops
sudo mv kops /usr/local/bin/kops

# macOS
brew install kops

# Windows
# Download from GitHub releases: https://github.com/kubernetes/kops/releases/latest

# Verify installation
kops version
```

#### Juju (Model-Driven Operations)

```bash
# Ubuntu/Debian
sudo snap install juju --classic

# macOS
brew install juju

# Windows
choco install juju

# Verify installation
juju version
```

#### Helm

```bash
# Using script (Linux/macOS)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# macOS
brew install helm

# Windows
choco install kubernetes-helm

# Verify installation
helm version
```

### Cloud CLIs

#### AWS CLI

```bash
# Linux/macOS
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# macOS
brew install awscli

# Windows
choco install awscli

# Configure
aws configure
```

#### Azure CLI

```bash
# Linux
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# macOS
brew install azure-cli

# Windows
choco install azure-cli

# Login
az login
```

#### Google Cloud SDK

```bash
# Linux/macOS
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# macOS
brew install --cask google-cloud-sdk

# Windows
choco install gcloudsdk

# Initialize
gcloud init
```

#### Cloud Foundry CLI

```bash
# Linux
curl -L "https://packages.cloudfoundry.org/stable?release=linux64-binary&source=github" | tar -zx
sudo mv cf8 /usr/local/bin/cf

# macOS
brew install cloudfoundry/tap/cf-cli@8

# Windows
choco install cloudfoundry-cli

# Verify installation
cf version
```

## B. Command Reference

### Maven Commands

#### Build Commands

```bash
# Clean build
./mvnw clean compile

# Package (creates JAR)
./mvnw package

# Install to local repository
./mvnw install

# Skip tests
./mvnw package -Dmaven.test.skip=true

# Specific goals
./mvnw clean test
./mvnw dependency:tree
./mvnw spring-boot:run
./mvnw spring-boot:build-image
```

#### Test Commands

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CityControllerTest

# Run tests with specific profile
./mvnw test -Dspring.profiles.active=hsql

# Generate coverage report
./mvnw jacoco:report
```

#### Spring Boot Commands

```bash
# Run application
./mvnw spring-boot:run

# Run with profiles
./mvnw spring-boot:run -Dspring.profiles.active=postgres,seeded

# Run with debug
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

### Gradle Commands

#### Gradle Build Commands

```bash
# Clean build
./gradlew clean build

# Assemble (creates JAR)
./gradlew assemble

# Build without tests
./gradlew build -x test

# Specific tasks
./gradlew clean test
./gradlew dependencies
./gradlew bootRun
./gradlew bootBuildImage
```

#### Gradle Test Commands

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests CityControllerTest

# Continuous testing
./gradlew test --continuous

# Generate coverage report
./gradlew jacocoTestReport
```

#### Gradle Spring Boot Commands

```bash
# Run application
./gradlew bootRun

# Run with profiles
./gradlew bootRun -Dspring.profiles.active=postgres,seeded

# Run with debug
./gradlew bootRun --debug-jvm
```

### Docker Commands

#### Image Management

```bash
# Build image
docker build -t cities-web .

# List images
docker images

# Remove image
docker rmi cities-web

# Pull image
docker pull postgres:15

# Push image
docker push registry/cities-web:latest
```

#### Container Management

```bash
# Run container
docker run -d -p 8080:8080 --name cities-web cities-web

# List running containers
docker ps

# List all containers
docker ps -a

# Stop container
docker stop cities-web

# Remove container
docker rm cities-web

# View logs
docker logs cities-web

# Execute command in container
docker exec -it cities-web bash
```

#### Docker Compose Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Restart service
docker-compose restart app

# Scale service
docker-compose up -d --scale app=3
```

### Kubernetes Commands

#### Cluster Management

```bash
# Get cluster info
kubectl cluster-info

# Get nodes
kubectl get nodes

# Get all resources
kubectl get all

# Describe resource
kubectl describe pod <pod-name>
```

#### Application Management

```bash
# Apply manifests
kubectl apply -f deployment.yaml

# Get deployments
kubectl get deployments

# Get pods
kubectl get pods

# Get services
kubectl get services

# Scale deployment
kubectl scale deployment cities-web --replicas=5

# Update image
kubectl set image deployment/cities-web cities-web=new-image:tag

# Rollout status
kubectl rollout status deployment/cities-web

# Rollback deployment
kubectl rollout undo deployment/cities-web
```

#### Debugging

```bash
# View logs
kubectl logs deployment/cities-web

# Follow logs
kubectl logs -f deployment/cities-web

# Execute in pod
kubectl exec -it <pod-name> -- bash

# Port forward
kubectl port-forward service/cities-web-service 8080:80

# Debug pod
kubectl run debug --image=busybox --rm -it --restart=Never -- sh
```

### Minikube Commands

#### Minikube Cluster Management

```bash
# Start cluster
minikube start

# Start with specific configuration
minikube start --driver=docker --memory=4096 --cpus=2

# Start with AI support (2025 feature)
minikube start --driver=krunkit

# Stop cluster
minikube stop

# Delete cluster
minikube delete

# Check status
minikube status
```

#### Addons Management

```bash
# List available addons
minikube addons list

# Enable addon
minikube addons enable ingress
minikube addons enable metrics-server
minikube addons enable dashboard

# Disable addon
minikube addons disable dashboard

# Configure addon with file (2025 feature)
minikube addons configure <addon-name> -f config.yaml
```

#### Development Workflow

```bash
# Configure Docker environment
eval $(minikube docker-env)

# Load local images
minikube image load cities-web:latest

# List images
minikube image ls

# Access dashboard
minikube dashboard

# Get cluster IP
minikube ip

# Enable tunnel for LoadBalancer
minikube tunnel
```

### KOPS Commands

#### Cluster Lifecycle

```bash
# Set environment variables
export KOPS_CLUSTER_NAME=cities.k8s.local
export KOPS_STATE_STORE=s3://cities-kops-state-store

# Create cluster configuration
kops create cluster \
    --name=${KOPS_CLUSTER_NAME} \
    --state=${KOPS_STATE_STORE} \
    --zones=us-west-2a,us-west-2b,us-west-2c \
    --node-count=3 \
    --node-size=t3.medium \
    --master-size=t3.medium \
    --master-count=3

# Deploy cluster
kops update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes

# Validate cluster
kops validate cluster --wait 10m --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Delete cluster
kops delete cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes
```

#### KOPS Cluster Management

```bash
# Edit cluster configuration
kops edit cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Edit instance group
kops edit ig nodes --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Rolling update
kops rolling-update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes

# Get cluster info
kops get cluster --state=${KOPS_STATE_STORE}

# Export kubecfg
kops export kubecfg --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
```

### Juju Commands

#### Model Management

```bash
# Add cloud
juju add-cloud aws
juju add-cloud my-k8s-cloud --cluster-name=my-cluster

# Bootstrap controller
juju bootstrap aws aws-controller
juju bootstrap my-k8s-cloud k8s-controller

# Create model
juju add-model cities-app

# Switch model
juju switch cities-app

# List models
juju models

# Destroy model
juju destroy-model cities-app --destroy-storage
```

#### Application Deployment

```bash
# Deploy application
juju deploy postgresql postgres --channel=14/stable
juju deploy ./cities-web.charm cities-web

# Scale application
juju scale-application cities-web 3
juju add-unit cities-web -n 2
juju remove-unit cities-web/2

# Relate applications
juju relate cities-web:database postgres:db

# Expose application
juju expose cities-web
```

#### Operations

```bash
# Check status
juju status
juju status --relations

# View logs
juju debug-log --include cities-web
juju debug-log --include cities-web/0 --tail

# Run actions
juju run-action cities-web/0 backup location=s3://backup-bucket
juju show-action-output <action-id>

# Configuration
juju config cities-web java_opts="-XX:+UseG1GC -Xmx2G"
juju config postgres max_connections=200

# SSH to unit
juju ssh cities-web/0
```

### Database Commands

#### PostgreSQL

```bash
# Connect to database
psql -h localhost -p 5432 -U postgres -d geo_data

# Common SQL commands
\dt                    # List tables
\d city               # Describe table
SELECT * FROM city;   # Query data
\q                    # Quit

# Backup database
pg_dump -h localhost -U postgres geo_data > backup.sql

# Restore database
psql -h localhost -U postgres geo_data < backup.sql
```

#### Docker PostgreSQL

```bash
# Run PostgreSQL container
docker run -d \
  --name postgres-cities \
  -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=geo_data \
  postgres:15

# Connect to PostgreSQL in container
docker exec -it postgres-cities psql -U postgres -d geo_data
```

## C. Configuration Templates

### application.yml Template

```yaml
spring:
  application:
    name: cities-web

  profiles:
    active: local

  datasource:
    url: jdbc:postgresql://localhost:5432/geo_data
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false

  flyway:
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 1.0

  jackson:
    serialization:
      write-dates-as-timestamps: false
    serialization-inclusion: non_empty

server:
  port: 8080
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    distribution:
      percentiles-histogram:
        http:
          server:
            requests: true

logging:
  level:
    org.springframework.web: INFO
    org.hibernate: INFO
    io.pivotal: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

---
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:hsqldb:mem:testdb
    driver-class-name: org.hsqldb.jdbc.JDBCDriver
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop

---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: jdbc:postgresql://postgres:5432/geo_data

---
spring:
  config:
    activate:
      on-profile: k8s
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:postgres-service}:5432/${POSTGRES_DB:geo_data}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD}
```

### Docker Compose Template

```yaml
version: '3.8'

services:
  app:
    image: cities-web:latest
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - POSTGRES_HOST=postgres
      - POSTGRES_DB=geo_data
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=geo_data
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
    driver: local

networks:
  default:
    driver: bridge
```

### Kubernetes Deployment Template

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: cities
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cities-web
  namespace: cities
  labels:
    app: cities-web
    version: v1
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: cities-web
  template:
    metadata:
      labels:
        app: cities-web
        version: v1
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
      - name: cities-web
        image: cities-web:latest
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: POSTGRES_HOST
          value: "postgres-service"
        - name: POSTGRES_DB
          valueFrom:
            configMapKeyRef:
              name: postgres-config
              key: database-name
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        securityContext:
          allowPrivilegeEscalation: false
          capabilities:
            drop:
            - ALL
          readOnlyRootFilesystem: true
          seccompProfile:
            type: RuntimeDefault
        volumeMounts:
        - name: tmp-volume
          mountPath: /tmp
      volumes:
      - name: tmp-volume
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: cities-web-service
  namespace: cities
  labels:
    app: cities-web
spec:
  type: ClusterIP
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: cities-web
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  namespace: cities
data:
  database-name: "geo_data"
  database-port: "5432"
---
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: cities
type: Opaque
data:
  username: cG9zdGdyZXM=  # base64 encoded 'postgres'
  password: cG9zdGdyZXM=  # base64 encoded 'postgres'
```

### Cloud Foundry Manifest Template

```yaml
---
applications:
- name: cities-web
  path: cities-web/target/cities-web-1.0.0-SNAPSHOT.jar
  memory: 1G
  instances: 3
  disk_quota: 512M
  stack: cflinuxfs4
  buildpacks:
    - java_buildpack
  env:
    SPRING_PROFILES_ACTIVE: cloud
    JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
    JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{ enabled: false }'
    JAVA_OPTS: '-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC'
  routes:
  - route: cities-web.apps.your-domain.com
  services:
  - cities-postgres
  health-check-type: http
  health-check-http-endpoint: /actuator/health
  timeout: 180

# Development manifest
---
applications:
- name: cities-web-dev
  path: cities-web/target/cities-web-1.0.0-SNAPSHOT.jar
  memory: 512M
  instances: 1
  env:
    SPRING_PROFILES_ACTIVE: cloud,seeded
    LOGGING_LEVEL_IO_PIVOTAL: DEBUG
  routes:
  - route: cities-web-dev.apps.your-domain.com
  services:
  - cities-postgres-dev
```

## D. Troubleshooting Guide

### Common Build Issues

#### Maven Issues

```bash
# Problem: Maven wrapper not executable
chmod +x mvnw

# Problem: Java version mismatch
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./mvnw -version

# Problem: Dependencies not downloading
./mvnw dependency:resolve -U

# Problem: Tests failing
./mvnw clean test -Dspring.profiles.active=test
```

#### Gradle Issues

```bash
# Problem: Gradle wrapper not executable
chmod +x gradlew

# Problem: Gradle daemon issues
./gradlew --stop
./gradlew build --no-daemon

# Problem: Build cache issues
./gradlew clean build --refresh-dependencies

# Problem: Out of memory
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
```

### Application Issues

#### Database Connection

```bash
# Check if PostgreSQL is running
docker ps | grep postgres
netstat -an | grep 5432

# Test connection manually
psql -h localhost -p 5432 -U postgres -d geo_data

# Check application logs
tail -f logs/application.log
kubectl logs deployment/cities-web
```

#### Port Conflicts

```bash
# Find process using port 8080
lsof -i :8080
netstat -tulpn | grep 8080

# Kill process
kill -9 <PID>

# Use different port
java -jar cities-web.jar --server.port=8081
```

#### Memory Issues

```bash
# Check JVM memory usage
jstat -gc <pid>
jmap -histo <pid>

# Increase heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Monitor memory in container
docker stats cities-web
kubectl top pods
```

### Container Issues

#### Docker Build Problems

```bash
# Problem: Build context too large
echo "target/" >> .dockerignore
echo "build/" >> .dockerignore

# Problem: Image too large
docker images | grep cities-web
docker history cities-web

# Problem: Container won't start
docker logs cities-web
docker run -it cities-web bash
```

#### Kubernetes Issues

```bash
# Pod not starting
kubectl describe pod <pod-name>
kubectl logs <pod-name>

# Image pull errors
kubectl describe pod <pod-name> | grep -A 10 Events

# Service connectivity
kubectl get endpoints
kubectl run debug --image=busybox --rm -it --restart=Never -- nc -zv service-name 80
```

### Network Issues

#### Service Discovery

```bash
# Check service endpoints
kubectl get endpoints cities-web-service

# Test internal connectivity
kubectl exec -it <pod-name> -- curl http://postgres-service:5432

# Check DNS resolution
kubectl exec -it <pod-name> -- nslookup postgres-service
```

#### Load Balancer Issues

```bash
# Check service status
kubectl get service cities-web-service
kubectl describe service cities-web-service

# Check ingress
kubectl get ingress
kubectl describe ingress cities-web-ingress

# Check external IP
curl -I http://external-ip/actuator/health
```

## E. Performance Tuning

### JVM Tuning

#### Memory Settings

```bash
# Production settings
-Xmx2g -Xms1g
-XX:MaxRAMPercentage=75.0
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# Development settings
-Xmx1g -Xms512m
-XX:+UseZGC  # For Java 21
```

#### Garbage Collection

```bash
# G1GC (recommended for most cases)
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+G1UseAdaptiveIHOP

# ZGC (for low latency)
-XX:+UseZGC
-XX:+UnlockExperimentalVMOptions

# Parallel GC (for throughput)
-XX:+UseParallelGC
-XX:ParallelGCThreads=4
```

#### Monitoring Options

```bash
# GC logging
-Xlog:gc*:gc.log:time

# JFR profiling
-XX:+FlightRecorder
-XX:StartFlightRecording=duration=60s,filename=profile.jfr

# JMX monitoring
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9999
-Dcom.sun.management.jmxremote.authenticate=false
```

### Database Optimization

#### Connection Pool Settings

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

#### JPA/Hibernate Settings

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
    hibernate:
      ddl-auto: validate
    show-sql: false
```

### Container Optimization

#### Resource Limits

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "500m"
```

#### Startup Optimization

```yaml
# Spring Boot properties
spring:
  jmx:
    enabled: false
  jpa:
    defer-datasource-initialization: true
  main:
    lazy-initialization: true  # Use carefully
```

## F. Security Checklist

### Application Security

- [ ] No hardcoded credentials in source code
- [ ] Environment variables for sensitive data
- [ ] HTTPS enabled in production
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (using JPA/prepared statements)
- [ ] CORS configuration appropriate for environment
- [ ] Actuator endpoints secured
- [ ] Security headers configured
- [ ] Authentication and authorization implemented

### Container Security

- [ ] Non-root user in containers
- [ ] Read-only root filesystem
- [ ] Security context configured
- [ ] No unnecessary capabilities
- [ ] Image vulnerability scanning
- [ ] Distroless or minimal base images
- [ ] Secrets managed externally
- [ ] Network policies implemented

### Kubernetes Security

- [ ] Pod Security Standards enforced
- [ ] RBAC configured properly
- [ ] Network policies in place
- [ ] Service accounts with minimal permissions
- [ ] Secrets mounted as volumes, not environment variables
- [ ] Resource quotas and limits set
- [ ] Admission controllers enabled
- [ ] Regular security updates

### Cloud Security

- [ ] IAM roles with minimal permissions
- [ ] VPC/network security groups configured
- [ ] Database encryption at rest
- [ ] SSL/TLS for all communications
- [ ] Backup encryption
- [ ] Audit logging enabled
- [ ] Compliance requirements met
- [ ] Regular security assessments

## G. Version Compatibility Matrix

| Component | Version | Compatibility |
|-----------|---------|---------------|
| Java | 21 (LTS) | Required |
| Spring Boot | 3.5.6 | Latest stable |
| Kotlin | 2.2.20 | Latest stable |
| PostgreSQL | 15.x | Recommended |
| Docker | 20.10+ | Required |
| Kubernetes | 1.29+ | Supported |
| Maven | 3.9+ | Required |
| Gradle | 8.14+ | Required |

### Cloud Platform Versions

| Platform | Version | Status |
|----------|---------|--------|
| AWS EKS | 1.33.x | Latest |
| Azure AKS | 1.29+ | Supported |
| Google GKE | 1.33+ | Latest |
| Tanzu Platform | v10.0 | Latest |

This appendix provides comprehensive reference information for working with the Cities application across all supported platforms and deployment scenarios.
