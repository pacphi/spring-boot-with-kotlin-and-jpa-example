# Cloud Foundry / Tanzu Platform Deployment Guide

## Overview

Deploy the Spring Boot Cities application to **Tanzu Platform for Cloud Foundry v10.0** - the latest 2025 release that represents the evolution of the Cloud Foundry platform under VMware by Broadcom.

## Tanzu Platform for Cloud Foundry v10.0 (2025)

### Platform Evolution

**Tanzu Platform for Cloud Foundry v10.0** is the next major version following Tanzu Application Service v6.0, bringing significant improvements:

- **Enhanced Security**: Updated CVE management and compliance standards
- **Improved Networking**: Advanced routing and load balancing capabilities
- **Container Runtime Updates**: Latest Kubernetes integration and buildpack support
- **Developer Experience**: Streamlined application deployment workflows
- **Enterprise Features**: Enhanced monitoring, logging, and operational capabilities

### Key 2025 Features

- **Cloud Native Buildpacks 2.0**: Advanced container image creation
- **Service Binding 2.0**: Improved service discovery and configuration
- **Multi-Foundation Support**: Seamless cross-platform deployments
- **Enhanced Observability**: Native integration with modern monitoring tools
- **Security Compliance**: SOC 2, FedRAMP, and industry standard certifications

### Platform Components

- **Diego**: Container orchestration and runtime
- **Garden**: Container management
- **Loggregator**: Logging and metrics aggregation
- **Cloud Controller**: API and application lifecycle management
- **Router**: Traffic routing and load balancing
- **BOSH**: Infrastructure management and deployment

## Prerequisites

### Required Tools

```bash
# CF CLI v8.16.0+
curl -L "https://packages.cloudfoundry.org/stable?release=linux64-binary&version=8.16.0&source=github" | tar -zx
sudo mv cf8 /usr/local/bin/cf

# Verify installation
cf version

# Install useful plugins
cf install-plugin -r CF-Community "top"
cf install-plugin -r CF-Community "app-autoscaler-plugin"
```

### Platform Access

```bash
# Target your Tanzu Platform foundation
cf api https://api.system.your-domain.com

# Login with your credentials
cf login

# Target organization and space
cf target -o your-org -s development
```

## Application Preparation

### Manifest Configuration

**manifest.yml:**

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
    SPRING_PROFILES_ACTIVE: cloud,seeded
    JBP_CONFIG_OPEN_JDK_JRE: '{ jre: { version: 21.+ } }'
    JBP_CONFIG_SPRING_AUTO_RECONFIGURATION: '{ enabled: false }'
  routes:
  - route: cities-web.apps.your-domain.com
  services:
  - cities-postgres
  health-check-type: http
  health-check-http-endpoint: /actuator/health
  timeout: 180
```

### Multi-Environment Manifests

**manifest-dev.yml:**

```yaml
---
applications:
- name: cities-web-dev
  memory: 512M
  instances: 1
  env:
    SPRING_PROFILES_ACTIVE: cloud,seeded
    LOGGING_LEVEL_IO_PIVOTAL: DEBUG
  routes:
  - route: cities-web-dev.apps.your-domain.com
```

**manifest-prod.yml:**

```yaml
---
applications:
- name: cities-web
  memory: 2G
  instances: 5
  env:
    SPRING_PROFILES_ACTIVE: cloud
    LOGGING_LEVEL_ROOT: WARN
    JVM_OPTS: "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
  routes:
  - route: cities.your-domain.com
  - route: www.cities.your-domain.com
```

## Service Configuration

### PostgreSQL Service

```bash
# List available PostgreSQL services in marketplace
cf marketplace | grep postgres

# Create PostgreSQL service instance
cf create-service elephantsql panda cities-postgres

# Alternative with Azure/AWS/GCP marketplace
cf create-service azure-postgresql-db basic cities-postgres
cf create-service aws-rds basic cities-postgres
cf create-service gcp-postgresql db-n1-standard-1 cities-postgres

# Check service creation status
cf service cities-postgres

# View service details
cf service cities-postgres --guid
```

### Service Configuration Parameters

**Create service with custom parameters:**

```bash
# Create with specific configuration
cf create-service postgres-service standard cities-postgres -c '{
  "version": "15",
  "storage": "20GB",
  "backup_retention": "7",
  "high_availability": true
}'
```

### Service Keys for External Access

```bash
# Create service key for external access
cf create-service-key cities-postgres cities-web-key

# Retrieve service credentials
cf service-key cities-postgres cities-web-key
```

## Application Deployment

### Standard Deployment

```bash
# Build application first
./mvnw clean package

# Deploy application
cf push -f manifest.yml

# Verify deployment
cf apps
cf app cities-web

# View application logs
cf logs cities-web --recent
```

### Blue-Green Deployment

```bash
# Deploy to staging route
cf push cities-web-green -f manifest.yml \
  --no-route \
  --var app-name=cities-web-green

# Add staging route for testing
cf map-route cities-web-green apps.your-domain.com \
  --hostname cities-web-staging

# Test staging deployment
curl https://cities-web-staging.apps.your-domain.com/actuator/health

# Switch traffic to new version
cf unmap-route cities-web apps.your-domain.com \
  --hostname cities-web
cf map-route cities-web-green apps.your-domain.com \
  --hostname cities-web

# Clean up old version
cf delete cities-web -f
cf rename cities-web-green cities-web
```

### Zero-Downtime Deployment with Rolling Strategy

```bash
# Use rolling deployment strategy
cf push cities-web -f manifest.yml --strategy rolling

# Monitor deployment progress
cf app cities-web
```

## Environment Configuration

### Environment Variables

```bash
# Set environment variables
cf set-env cities-web SPRING_PROFILES_ACTIVE "cloud,seeded"
cf set-env cities-web JVM_OPTS "-Xmx1g -XX:+UseG1GC"
cf set-env cities-web MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE "*"

# User-provided service for external configuration
cf create-user-provided-service cities-config -p '{
  "spring.datasource.hikari.maximum-pool-size": "20",
  "spring.jpa.hibernate.ddl-auto": "validate",
  "logging.level.org.springframework.web": "DEBUG"
}'

# Bind configuration service
cf bind-service cities-web cities-config

# Restart to apply changes
cf restart cities-web
```

### Spring Cloud Services Integration

```bash
# Create Config Server service
cf create-service p-config-server standard cities-config-server

# Create Service Registry
cf create-service p-service-registry standard cities-service-registry

# Create Circuit Breaker Dashboard
cf create-service p-circuit-breaker-dashboard standard cities-circuit-breaker

# Bind services to application
cf bind-service cities-web cities-config-server
cf bind-service cities-web cities-service-registry
cf bind-service cities-web cities-circuit-breaker

# Update manifest to include services
```

**Updated manifest.yml with Spring Cloud Services:**

```yaml
applications:
- name: cities-web
  services:
  - cities-postgres
  - cities-config-server
  - cities-service-registry
  - cities-circuit-breaker
  env:
    SPRING_PROFILES_ACTIVE: cloud
    SPRING_CLOUD_CONFIG_ENABLED: true
```

## Scaling and Performance

### Manual Scaling

```bash
# Scale instances
cf scale cities-web -i 5

# Scale memory
cf scale cities-web -m 2G

# Scale disk
cf scale cities-web -k 1G

# Scale both instances and memory
cf scale cities-web -i 3 -m 1G
```

### Auto Scaling

```bash
# Install App Autoscaler plugin
cf install-plugin -r CF-Community "app-autoscaler-plugin"

# Create autoscaling policy
cat > autoscaler-policy.json <<EOF
{
  "instance_min_count": 2,
  "instance_max_count": 10,
  "scaling_rules": [
    {
      "metric_type": "cpu",
      "breach_duration_secs": 60,
      "threshold": 80,
      "operator": ">",
      "cool_down_secs": 300,
      "adjustment": "+1"
    },
    {
      "metric_type": "cpu",
      "breach_duration_secs": 180,
      "threshold": 20,
      "operator": "<",
      "cool_down_secs": 300,
      "adjustment": "-1"
    },
    {
      "metric_type": "memory_util",
      "breach_duration_secs": 60,
      "threshold": 90,
      "operator": ">",
      "cool_down_secs": 300,
      "adjustment": "+1"
    }
  ],
  "schedules": [
    {
      "timezone": "America/New_York",
      "start_date": "2025-01-01",
      "end_date": "2025-12-31",
      "days_of_week": [1,2,3,4,5],
      "start_time": "08:00",
      "end_time": "18:00",
      "instance_min_count": 3,
      "instance_max_count": 15
    }
  ]
}
EOF

# Apply autoscaling policy
cf attach-autoscaling-policy cities-web autoscaler-policy.json

# Enable autoscaling
cf enable-autoscaling cities-web

# Check autoscaling status
cf autoscaling-apps
cf autoscaling-history cities-web
```

## Monitoring and Observability

### Application Metrics

```bash
# View application events
cf events cities-web

# Real-time application logs
cf logs cities-web

# Application metrics
cf app cities-web

# Get detailed app information
cf curl /v3/apps/$(cf app cities-web --guid)/stats
```

### Metrics Integration

**Configure metrics in application:**

```yaml
# application-cloud.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      wavefront:
        enabled: true
        api-token: ${WAVEFRONT_API_TOKEN}
        uri: ${WAVEFRONT_URI}
    distribution:
      percentiles-histogram:
        http:
          server:
            requests: true
```

### Application Performance Monitoring

```bash
# Bind APM service (e.g., New Relic, AppDynamics)
cf create-service newrelic standard cities-newrelic
cf bind-service cities-web cities-newrelic

# Configure APM agent
cf set-env cities-web NEW_RELIC_LICENSE_KEY "your-license-key"
cf set-env cities-web NEW_RELIC_APP_NAME "cities-web"
```

## Security Configuration

### OAuth 2.0 / OpenID Connect Integration

```bash
# Create SSO service
cf create-service p-identity sso-service cities-sso

# Bind SSO service
cf bind-service cities-web cities-sso

# Configure SSO plan
cf update-service cities-sso -c '{
  "allowed_grant_types": ["authorization_code", "refresh_token"],
  "redirect_uris": ["https://cities-web.apps.your-domain.com/login/oauth2/code/sso"]
}'
```

**Spring Security Configuration:**

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          sso:
            client-id: ${vcap.services.cities-sso.credentials.client_id}
            client-secret: ${vcap.services.cities-sso.credentials.client_secret}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
        provider:
          sso:
            authorization-uri: ${vcap.services.cities-sso.credentials.auth_domain}/oauth/authorize
            token-uri: ${vcap.services.cities-sso.credentials.auth_domain}/oauth/token
            user-info-uri: ${vcap.services.cities-sso.credentials.auth_domain}/userinfo
```

### Network Security

```bash
# Create security group for application
cf create-security-group cities-web-sg security-group.json

# Apply security group
cf bind-security-group cities-web-sg your-org your-space
```

**security-group.json:**

```json
[
  {
    "protocol": "tcp",
    "destination": "10.0.0.0/8",
    "ports": "5432",
    "description": "PostgreSQL access"
  },
  {
    "protocol": "tcp",
    "destination": "0.0.0.0-255.255.255.255",
    "ports": "80,443",
    "description": "HTTP/HTTPS outbound"
  }
]
```

## CI/CD Integration

### GitHub Actions with CF

```yaml
name: Deploy to Cloud Foundry
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v5

    - name: Set up JDK 21
      uses: actions/setup-java@v5
      with:
        java-version: '21'
        distribution: 'liberica'
        cache: maven

    - name: Build application
      run: ./mvnw clean package

    - name: Install CF CLI
      run: |
        curl -L "https://packages.cloudfoundry.org/stable?release=linux64-binary&source=github" | tar -zx
        sudo mv cf8 /usr/local/bin/cf

    - name: Deploy to Cloud Foundry
      env:
        CF_USERNAME: ${{ secrets.CF_USERNAME }}
        CF_PASSWORD: ${{ secrets.CF_PASSWORD }}
        CF_ORG: ${{ secrets.CF_ORG }}
        CF_SPACE: ${{ secrets.CF_SPACE }}
      run: |
        cf api https://api.system.your-domain.com
        cf auth "$CF_USERNAME" "$CF_PASSWORD"
        cf target -o "$CF_ORG" -s "$CF_SPACE"
        cf push -f manifest.yml
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    environment {
        CF_HOME = '/tmp'
    }

    stages {
        stage('Build') {
            steps {
                sh './mvnw clean package'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Deploy to Staging') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'cf-credentials',
                    usernameVariable: 'CF_USERNAME',
                    passwordVariable: 'CF_PASSWORD'
                )]) {
                    sh '''
                        cf api https://api.system.your-domain.com
                        cf auth "$CF_USERNAME" "$CF_PASSWORD"
                        cf target -o your-org -s staging
                        cf push -f manifest-dev.yml
                    '''
                }
            }
        }

        stage('Integration Tests') {
            steps {
                sh '''
                    # Wait for app to be ready
                    sleep 30
                    # Run integration tests against staging
                    curl -f https://cities-web-dev.apps.your-domain.com/actuator/health
                '''
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'cf-credentials',
                    usernameVariable: 'CF_USERNAME',
                    passwordVariable: 'CF_PASSWORD'
                )]) {
                    sh '''
                        cf target -o your-org -s production
                        cf push -f manifest-prod.yml --strategy rolling
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'cf logout'
        }
    }
}
```

### GitLab CI/CD

```yaml
# .gitlab-ci.yml
stages:
  - build
  - test
  - deploy-staging
  - deploy-production

variables:
  CF_API: "https://api.system.your-domain.com"
  CF_ORG: "your-org"

build:
  stage: build
  image: maven:3.9-openjdk-21
  script:
    - ./mvnw clean package
  artifacts:
    paths:
      - cities-web/target/*.jar
    expire_in: 1 hour

test:
  stage: test
  image: maven:3.9-openjdk-21
  script:
    - ./mvnw test
  artifacts:
    reports:
      junit: target/surefire-reports/TEST-*.xml

deploy-staging:
  stage: deploy-staging
  image: governmentpaas/cf-cli:latest
  script:
    - cf api $CF_API
    - cf auth $CF_USERNAME $CF_PASSWORD
    - cf target -o $CF_ORG -s staging
    - cf push -f manifest-dev.yml
  environment:
    name: staging
    url: https://cities-web-dev.apps.your-domain.com
  only:
    - develop

deploy-production:
  stage: deploy-production
  image: governmentpaas/cf-cli:latest
  script:
    - cf api $CF_API
    - cf auth $CF_USERNAME $CF_PASSWORD
    - cf target -o $CF_ORG -s production
    - cf push -f manifest-prod.yml --strategy rolling
  environment:
    name: production
    url: https://cities-web.apps.your-domain.com
  only:
    - main
  when: manual
```

## Advanced Configuration

### Custom Buildpack Usage

```bash
# Use specific Java buildpack version
cf push cities-web -b https://github.com/cloudfoundry/java-buildpack.git#v4.50

# Use offline buildpack
cf push cities-web -b java_buildpack_offline

# Use multiple buildpacks
cf push cities-web -b binary_buildpack -b java_buildpack
```

### Container-to-Container Networking

```bash
# Enable container networking
cf enable-feature-flag diego_docker

# Create network policy
cf create-network-policy cities-web cities-postgres \
  --port 5432 --protocol tcp
```

### Route Services

```bash
# Create route service for SSL termination/caching
cf create-user-provided-service cities-route-service \
  -r https://ssl-proxy.example.com

# Bind route service
cf bind-route-service apps.your-domain.com cities-route-service \
  --hostname cities-web
```

## Troubleshooting

### Common Deployment Issues

**Application Start Failures:**

```bash
# Check application logs
cf logs cities-web --recent

# Check application events
cf events cities-web

# Check health
cf app cities-web
```

**Memory Issues:**

```bash
# Check memory usage
cf app cities-web
cf curl /v3/apps/$(cf app cities-web --guid)/stats

# Increase memory allocation
cf scale cities-web -m 2G
```

**Database Connection Issues:**

```bash
# Check service binding
cf env cities-web | grep VCAP_SERVICES

# Recreate service binding
cf unbind-service cities-web cities-postgres
cf bind-service cities-web cities-postgres
cf restart cities-web
```

**Routing Issues:**

```bash
# Check routes
cf routes

# Recreate route
cf unmap-route cities-web apps.your-domain.com --hostname cities-web
cf map-route cities-web apps.your-domain.com --hostname cities-web
```

### Performance Troubleshooting

```bash
# Application performance
cf app cities-web

# Real-time metrics
cf install-plugin -r CF-Community "top"
cf top

# Thread dumps
cf ssh cities-web -c "kill -3 \$(pgrep java)"
cf logs cities-web --recent | grep "Full thread dump"
```

### Platform-Specific Debugging

```bash
# Check platform status
cf curl /v2/info

# Check service marketplace
cf marketplace

# Check quota usage
cf quotas
cf space-quota your-space

# Check buildpack information
cf buildpacks
```

## Migration from Legacy CF

### TAS to Tanzu Platform Migration

```bash
# Export application configuration
cf curl /v3/apps > apps-backup.json
cf curl /v3/service_instances > services-backup.json

# Update buildpack references
sed -i 's/java_buildpack/tanzu_java_buildpack/g' manifest.yml

# Update stack references
sed -i 's/cflinuxfs3/cflinuxfs4/g' manifest.yml

# Test deployment
cf push cities-web -f manifest.yml --no-start
cf start cities-web
```

### Blue-Green Migration Strategy

```bash
# Deploy new version to staging
cf push cities-web-v10 -f manifest-v10.yml \
  --no-route

# Bind same services
cf bind-service cities-web-v10 cities-postgres

# Test new version
cf map-route cities-web-v10 apps.your-domain.com \
  --hostname cities-web-staging

# Switch traffic after validation
cf unmap-route cities-web apps.your-domain.com \
  --hostname cities-web
cf map-route cities-web-v10 apps.your-domain.com \
  --hostname cities-web

# Clean up old version
cf delete cities-web -f
cf rename cities-web-v10 cities-web
```

This comprehensive guide covers deploying the Cities application to Tanzu Platform for Cloud Foundry v10.0, incorporating the latest 2025 features, security practices, and operational best practices.
