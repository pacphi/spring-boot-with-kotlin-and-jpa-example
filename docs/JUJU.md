# Deploying Cities Application with Juju Model-Driven Operations

This guide demonstrates how to deploy the Cities Spring Boot application using Juju, Canonical's model-driven application management platform, which simplifies complex application deployment and lifecycle management across clouds and Kubernetes.

## What is Juju?

Juju is an open-source orchestration engine for software operators that enables the deployment, integration and lifecycle management of applications at any scale, on any infrastructure. Juju embraces a model-driven approach to operations, using "charms" (operators) to automate every aspect of an application's lifecycle including deployment, configuration, scaling, integration, and maintenance.

## Key Concepts

### Model-Driven Operations

Juju moves beyond traditional configuration management to focus on application management and resource allocation. Operators focus on the software model rather than machines and instance details, dramatically reducing operational complexity.

### Charms (Operators)

A charm is an operator - business logic encapsulated in reusable software packages that automate every aspect of an application's life. Charms are collections of YAML configuration files and hooks that can install software, manage relationships, upgrade applications, and handle scaling.

### Models

A model in Juju represents a workspace where applications and their relationships are defined. Models are portable and reusable across different infrastructure environments.

### Relations

Juju treats integrations between different applications as first-class primitives, enabling your observability stack, database, SSO, and other components to evolve with your system.

## Prerequisites

### Required Tools

- **Juju CLI** (latest version)
- **Java 21** (for building the application)
- **Maven 3.9+** or **Gradle 8.14+**
- **Cloud credentials** (AWS, Azure, GCP, or local LXD)

### Infrastructure Options

Juju supports multiple substrates:

- **Public Clouds**: AWS, Azure, Google Cloud Platform
- **Kubernetes**: Any Kubernetes cluster
- **Private Clouds**: OpenStack, MAAS
- **Local Development**: LXD containers
- **Bare Metal**: Direct server deployment

## Installation

### Install Juju

#### Ubuntu/Debian

```bash
# Install via snap (recommended)
sudo snap install juju --classic

# Or via apt
sudo apt update
sudo apt install juju
```

#### macOS

```bash
# Using Homebrew
brew install juju

# Or download from GitHub
curl -LO https://github.com/juju/juju/releases/latest/download/juju-darwin-amd64.tar.xz
tar xf juju-darwin-amd64.tar.xz
sudo mv juju /usr/local/bin/
```

#### Windows

```powershell
# Using Chocolatey
choco install juju

# Or using Scoop
scoop install juju

# Or download from GitHub releases
```

### Verify Installation

```bash
juju version
```

## Cloud Setup

### Option 1: Local Development with LXD

#### Install and Configure LXD

```bash
# Install LXD
sudo snap install lxd

# Initialize LXD
sudo lxd init --auto

# Add cloud to Juju
juju add-cloud localhost lxd
juju bootstrap localhost localhost-controller
```

### Option 2: AWS Cloud

#### Configure AWS Credentials

```bash
# Configure AWS CLI first
aws configure

# Add AWS cloud to Juju
juju add-cloud aws

# Add credentials
juju add-credential aws
# Follow prompts to enter access-key and secret-key

# Bootstrap controller
juju bootstrap aws aws-controller --config vpc-id=vpc-xxxxxxxx --config vpc-id-force=true
```

### Option 3: Kubernetes

#### Add Kubernetes Cloud

```bash
# For existing kubectl context
juju add-k8s my-k8s-cloud --cluster-name=my-cluster

# Bootstrap on Kubernetes
juju bootstrap my-k8s-cloud k8s-controller
```

### Option 4: Azure Cloud

```bash
# Add Azure cloud
juju add-cloud azure

# Add credentials (requires service principal)
juju add-credential azure

# Bootstrap controller
juju bootstrap azure azure-controller
```

## Application Deployment

### Step 1: Create Application Model

```bash
# Create a new model for the cities application
juju add-model cities-app

# Switch to the model
juju switch cities-app

# Check model status
juju status
```

### Step 2: Deploy PostgreSQL Database

```bash
# Deploy PostgreSQL charm
juju deploy postgresql postgres --channel=14/stable --config profile=production

# Or for development
juju deploy postgresql postgres --channel=14/stable --config profile=testing

# Set database configuration
juju config postgres db_name=geo_data
juju config postgres db_user=cities_user

# Scale database for high availability (optional)
juju add-unit postgres -n 2
```

### Step 3: Create Custom Charm for Cities Application

Since there's no existing charm for our specific application, we'll create a minimal charm structure:

#### Create Charm Directory

```bash
mkdir -p charms/cities-web
cd charms/cities-web

# Create charm metadata
cat > metadata.yaml <<EOF
name: cities-web
display-name: Cities Web Application
summary: Spring Boot Cities Web Application
description: |
  A Spring Boot application for managing city data with PostgreSQL backend.
  Demonstrates CRUD operations with REST API and web interface.

series:
  - jammy
  - focal

peers:
  cluster:
    interface: cities-cluster

requires:
  database:
    interface: pgsql

provides:
  website:
    interface: http

resources:
  cities-web-image:
    type: oci-image
    description: Docker image for Cities Web Application

containers:
  cities-web:
    resource: cities-web-image
    mounts:
      - storage: data
        location: /data

storage:
  data:
    type: filesystem
    description: Data storage for Cities application
    minimum-size: 1G
EOF

# Create charm config
cat > config.yaml <<EOF
options:
  java_opts:
    type: string
    default: "-XX:+UseG1GC -Xmx1G"
    description: JVM options for the application
  spring_profiles:
    type: string
    default: "k8s,seeded"
    description: Spring Boot active profiles
  port:
    type: int
    default: 8080
    description: Port for the web application
  replicas:
    type: int
    default: 3
    description: Number of application replicas
EOF

# Create actions
mkdir actions
cat > actions.yaml <<EOF
backup:
  description: Create a backup of application data
  params:
    location:
      type: string
      description: Backup location
  required: [location]

restore:
  description: Restore application data from backup
  params:
    location:
      type: string
      description: Backup location to restore from
  required: [location]

scale:
  description: Scale the application
  params:
    replicas:
      type: int
      description: Number of replicas
  required: [replicas]
EOF
```

#### Create Charm Code (Python)

```bash
# Create src directory
mkdir src

# Create charm.py
cat > src/charm.py <<'EOF'
#!/usr/bin/env python3

import logging
import ops

logger = logging.getLogger(__name__)

class CitiesWebCharm(ops.CharmBase):
    """Charm for Cities Web Application."""

    def __init__(self, *args):
        super().__init__(*args)

        # Observe charm events
        self.framework.observe(self.on.start, self._on_start)
        self.framework.observe(self.on.config_changed, self._on_config_changed)
        self.framework.observe(self.on.database_relation_changed, self._on_database_changed)
        self.framework.observe(self.on.backup_action, self._on_backup_action)
        self.framework.observe(self.on.restore_action, self._on_restore_action)
        self.framework.observe(self.on.scale_action, self._on_scale_action)

    def _on_start(self, event):
        """Handle start event."""
        logger.info("Starting Cities Web Application")
        self._update_app_config()
        self.unit.status = ops.ActiveStatus("Cities Web application started")

    def _on_config_changed(self, event):
        """Handle config changed event."""
        logger.info("Configuration changed")
        self._update_app_config()

    def _on_database_changed(self, event):
        """Handle database relation changed."""
        logger.info("Database relation changed")
        if self._get_database_config():
            self._update_app_config()
            self.unit.status = ops.ActiveStatus("Connected to database")
        else:
            self.unit.status = ops.WaitingStatus("Waiting for database")

    def _update_app_config(self):
        """Update application configuration."""
        # Get configuration
        java_opts = self.config["java_opts"]
        spring_profiles = self.config["spring_profiles"]
        port = self.config["port"]

        # Get database config
        db_config = self._get_database_config()

        # Configure the application container
        container = self.unit.get_container("cities-web")
        if container.can_connect():
            # Set environment variables
            env_vars = {
                "JAVA_OPTS": java_opts,
                "SPRING_PROFILES_ACTIVE": spring_profiles,
                "SERVER_PORT": str(port),
            }

            if db_config:
                env_vars.update({
                    "POSTGRES_HOST": db_config["host"],
                    "POSTGRES_PORT": str(db_config["port"]),
                    "POSTGRES_DB": db_config["database"],
                    "POSTGRES_USER": db_config["user"],
                    "POSTGRES_PASSWORD": db_config["password"],
                })

            # Update container configuration
            layer = ops.pebble.Layer({
                "summary": "Cities web layer",
                "description": "Pebble config layer for Cities web app",
                "services": {
                    "cities-web": {
                        "override": "replace",
                        "summary": "Cities web service",
                        "command": "java $JAVA_OPTS -jar /app.jar",
                        "startup": "enabled",
                        "environment": env_vars,
                    }
                },
            })

            container.add_layer("cities-web", layer, combine=True)
            container.replan()

    def _get_database_config(self):
        """Get database configuration from relation."""
        for relation in self.model.relations["database"]:
            if relation.app:
                # Get database connection details from relation data
                relation_data = relation.data[relation.app]
                if "host" in relation_data:
                    return {
                        "host": relation_data["host"],
                        "port": relation_data.get("port", "5432"),
                        "database": relation_data.get("database", "geo_data"),
                        "user": relation_data.get("user", "cities_user"),
                        "password": relation_data.get("password", ""),
                    }
        return None

    def _on_backup_action(self, event):
        """Handle backup action."""
        location = event.params["location"]
        logger.info(f"Creating backup at {location}")
        # Implement backup logic
        event.set_results({"backup-location": location})

    def _on_restore_action(self, event):
        """Handle restore action."""
        location = event.params["location"]
        logger.info(f"Restoring from backup at {location}")
        # Implement restore logic
        event.set_results({"restored-from": location})

    def _on_scale_action(self, event):
        """Handle scale action."""
        replicas = event.params["replicas"]
        logger.info(f"Scaling to {replicas} replicas")
        # Implement scaling logic
        self.app.planned_units = replicas
        event.set_results({"replicas": replicas})

if __name__ == "__main__":
    ops.main(CitiesWebCharm)
EOF

# Make charm executable
chmod +x src/charm.py

# Create requirements
cat > requirements.txt <<EOF
ops >= 2.0
EOF
```

### Step 4: Deploy Custom Charm

#### Build and Deploy Charm

```bash
# Go back to main directory
cd ../..

# Pack the charm
juju pack charms/cities-web

# Deploy the charm with our container image
juju deploy ./cities-web.charm cities-web \
  --resource cities-web-image=pivotalio/cities-web:latest \
  --config java_opts="-XX:+UseG1GC -Xmx2G" \
  --config spring_profiles="k8s,seeded"

# Scale the application
juju scale-application cities-web 3
```

### Step 5: Establish Relations

```bash
# Connect cities-web to postgres
juju relate cities-web:database postgres:db

# Check relation status
juju status --relations
```

### Step 6: Expose Application

```bash
# Expose the application to external traffic
juju expose cities-web --endpoints website

# Or expose to specific networks
juju expose cities-web --to-cidrs 0.0.0.0/0
```

## Advanced Deployment Patterns

### High Availability Setup

```bash
# Deploy with multiple units and constraints
juju deploy postgresql postgres --channel=14/stable -n 3 \
  --constraints "cores=2 mem=4G root-disk=20G"

# Deploy application with placement directives
juju deploy ./cities-web.charm cities-web -n 3 \
  --constraints "cores=2 mem=4G" \
  --placement zone=us-west-2a,zone=us-west-2b,zone=us-west-2c
```

### Load Balancer Integration

```bash
# Deploy HAProxy for load balancing
juju deploy haproxy

# Relate web application to load balancer
juju relate cities-web:website haproxy:reverseproxy

# Expose load balancer
juju expose haproxy
```

### Monitoring Stack

```bash
# Deploy Prometheus monitoring
juju deploy prometheus2 prometheus
juju deploy grafana

# Deploy monitoring relations
juju relate prometheus:grafana-source grafana:grafana-source
juju relate cities-web:metrics prometheus:target

# Expose monitoring
juju expose grafana
```

### Logging Stack

```bash
# Deploy Elasticsearch and Kibana
juju deploy elasticsearch
juju deploy kibana

# Deploy Filebeat for log collection
juju deploy filebeat

# Establish relations
juju relate elasticsearch kibana
juju relate filebeat elasticsearch
juju relate cities-web:logs filebeat:logstash-client
```

## Configuration Management

### Application Configuration

```bash
# Update application configuration
juju config cities-web java_opts="-XX:+UseG1GC -Xmx4G -XX:MaxRAMPercentage=75"
juju config cities-web spring_profiles="k8s,seeded,monitoring"
juju config cities-web replicas=5

# View current configuration
juju config cities-web
```

### Database Configuration

```bash
# Configure PostgreSQL
juju config postgres max_connections=200
juju config postgres shared_buffers="256MB"
juju config postgres effective_cache_size="1GB"

# Set up database backup
juju config postgres backup_schedule="0 2 * * *"
juju config postgres backup_retention_days=30
```

### Resource Management

```bash
# Set resource constraints
juju set-constraints cities-web cores=2 mem=4G root-disk=20G

# Update model constraints
juju set-model-constraints cores=1 mem=2G
```

## Operations and Maintenance

### Monitoring and Status

```bash
# Check overall status
juju status

# Check detailed status with relations
juju status --relations

# Check specific application
juju status cities-web

# Watch status changes
juju status --watch 5s
```

### Scaling Operations

```bash
# Scale application units
juju add-unit cities-web -n 2
juju remove-unit cities-web/2

# Scale using actions
juju run-action cities-web/0 scale replicas=10
juju show-action-output <action-id>
```

### Updates and Upgrades

```bash
# Refresh charm to latest version
juju refresh cities-web

# Upgrade to specific charm revision
juju refresh cities-web --revision=5

# Update container image
juju refresh cities-web --resource cities-web-image=pivotalio/cities-web:v2.0.0
```

### Backup Operations

```bash
# Create application backup
juju run-action cities-web/0 backup location=s3://my-backup-bucket/cities-app
juju show-action-output <action-id>

# Create database backup
juju run-action postgres/0 backup

# Schedule regular backups
juju config postgres backup_schedule="0 2 * * *"
```

### Log Management

```bash
# View application logs
juju debug-log --include cities-web

# View specific unit logs
juju debug-log --include cities-web/0

# Follow logs in real-time
juju debug-log --include cities-web --tail

# Filter by log level
juju debug-log --include cities-web --level ERROR
```

## Multi-Cloud Deployment

### Cross-Cloud Model

```bash
# Create model on different clouds
juju add-model cities-aws aws
juju add-model cities-azure azure
juju add-model cities-gcp google

# Deploy to specific clouds
juju deploy -m cities-aws postgresql postgres-aws
juju deploy -m cities-azure postgresql postgres-azure

# Cross-model relations (if supported)
juju offer cities-aws:postgres database
juju find-offers
juju consume aws-admin/cities-aws.database
```

### Kubernetes Integration

```bash
# Deploy on Kubernetes
juju add-model cities-k8s my-k8s-cloud

# Switch to Kubernetes model
juju switch cities-k8s

# Deploy with Kubernetes-specific configurations
juju deploy ./cities-web.charm cities-web \
  --config storage_class=ssd-retain \
  --config ingress_class=nginx
```

## Troubleshooting

### Common Issues

#### Unit in Error State

```bash
# Check unit status and errors
juju status cities-web/0
juju show-status-log cities-web/0

# Debug the unit
juju debug-hooks cities-web/0

# SSH to unit for investigation
juju ssh cities-web/0
```

#### Relation Issues

```bash
# Check relation data
juju show-unit cities-web/0

# Debug relation hooks
juju debug-hooks cities-web/0 database-relation-changed

# Remove and re-add relations
juju remove-relation cities-web postgres
juju relate cities-web:database postgres:db
```

#### Container Issues (for K8s)

```bash
# Check container logs
juju debug-log --include cities-web --level DEBUG

# Execute commands in container
juju exec --unit cities-web/0 -- ls -la /app

# Check container status
juju show-unit cities-web/0 --format yaml
```

### Performance Tuning

#### Application Performance

```bash
# Update JVM settings
juju config cities-web java_opts="-XX:+UseG1GC -Xmx4G -XX:+UseStringDeduplication"

# Enable performance monitoring
juju config cities-web spring_profiles="k8s,seeded,monitoring,performance"
```

#### Database Performance

```bash
# Optimize PostgreSQL configuration
juju config postgres shared_buffers="512MB"
juju config postgres effective_cache_size="2GB"
juju config postgres random_page_cost=1.1
juju config postgres checkpoint_completion_target=0.9
```

## Security Hardening

### Application Security

```bash
# Enable security profiles
juju config cities-web spring_profiles="k8s,seeded,security"

# Configure HTTPS
juju config cities-web use_https=true
juju config cities-web ssl_cert_path=/etc/ssl/certs/app.crt
juju config cities-web ssl_key_path=/etc/ssl/private/app.key
```

### Database Security

```bash
# Enable SSL for database connections
juju config postgres ssl_mode=require
juju config postgres ssl_cert_file=/etc/ssl/certs/server.crt
juju config postgres ssl_key_file=/etc/ssl/private/server.key

# Configure authentication
juju config postgres password_encryption=scram-sha-256
```

### Network Security

```bash
# Configure firewalls (cloud-dependent)
juju config cities-web firewall_rules="22/tcp,8080/tcp"

# Set up VPN or private networking
juju config postgres private_network=true
```

## Cost Optimization

### Resource Optimization

```bash
# Use spot instances (cloud-dependent)
juju set-constraints cities-web instance-type=t3.medium spot=true

# Schedule scaling based on demand
juju config cities-web auto_scale_min=2
juju config cities-web auto_scale_max=10
juju config cities-web auto_scale_metric=cpu
juju config cities-web auto_scale_threshold=70
```

### Storage Optimization

```bash
# Use appropriate storage classes
juju config postgres storage_class=gp3
juju config postgres storage_size=50Gi

# Enable compression
juju config postgres enable_compression=true
```

## Model-Driven Observability

### Prometheus Integration

```bash
# Deploy monitoring bundle
juju deploy cos-lite --trust

# Relate applications to monitoring
juju relate cities-web cos-lite
juju relate postgres cos-lite

# Access Grafana dashboard
juju status cos-lite
# Get the Grafana URL and credentials
```

### Custom Metrics

```bash
# Configure application metrics
juju config cities-web metrics_enabled=true
juju config cities-web metrics_path="/actuator/prometheus"

# Configure database metrics
juju config postgres enable_metrics=true
juju config postgres metrics_port=9187
```

## Cleanup

### Remove Applications

```bash
# Remove specific applications
juju remove-application cities-web
juju remove-application postgres

# Remove relations first if needed
juju remove-relation cities-web postgres
```

### Destroy Model

```bash
# Destroy the entire model
juju destroy-model cities-app --destroy-storage

# Confirm destruction
juju destroy-model cities-app --destroy-storage --force
```

### Remove Controller

```bash
# Destroy controller (removes all models)
juju destroy-controller localhost-controller --destroy-all-models
```

## Best Practices

### Charm Development

1. **Idempotency**: Ensure charm operations can be run multiple times safely
2. **Error Handling**: Implement proper error handling and status reporting
3. **Testing**: Write unit tests and integration tests for charms
4. **Documentation**: Document charm configuration options and actions

### Operations

1. **Monitoring**: Set up comprehensive monitoring from the beginning
2. **Backup**: Implement regular backup strategies
3. **Security**: Follow security best practices for all components
4. **Scaling**: Plan for scaling requirements early
5. **Updates**: Keep charms and applications updated regularly

### Model Design

1. **Separation**: Use separate models for different environments
2. **Relations**: Design clear relation interfaces between applications
3. **Configuration**: Use configuration management for environment-specific settings
4. **Lifecycle**: Plan for complete application lifecycle management

## Juju vs Traditional Deployment

| Aspect | Traditional | Juju |
|--------|-------------|------|
| **Complexity** | High manual configuration | Model-driven automation |
| **Repeatability** | Scripts and documentation | Portable, reusable charms |
| **Integration** | Manual service discovery | First-class relations |
| **Scaling** | Manual or basic automation | Built-in lifecycle management |
| **Operations** | Tool-specific procedures | Unified operational model |
| **Observability** | Manual setup and configuration | Model-driven observability |

## Next Steps

- Explore [Kubernetes native deployment](./MINIKUBE.md) for comparison
- Learn about [cloud-specific deployments](./AWS.md)
- Set up [CI/CD pipelines](./BUILD.md) with Juju integration
- Develop [custom charms](https://juju.is/docs/sdk) for your applications

## References

- [Official Juju Documentation](https://juju.is/docs)
- [Juju Charm Hub](https://charmhub.io/)
- [Juju GitHub Repository](https://github.com/juju/juju)
- [Charm Development SDK](https://juju.is/docs/sdk)
- [Model-Driven Operations Manifesto](https://juju.is/model-driven-operations-manifesto)
- [Canonical Juju Blog](https://canonical.com/blog/tag/juju)
