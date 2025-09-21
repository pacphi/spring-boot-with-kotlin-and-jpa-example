# Google Cloud Deployment Guide

## Overview

Deploy the Spring Boot Cities application to **Google Cloud Platform (GCP)** using **Google Kubernetes Engine (GKE)** with the latest 2025 features and AI/ML capabilities.

## GKE 2025 Features

### Latest Innovations

- **Kubernetes 1.33+** - Latest upstream versions with rapid channel support
- **Cluster Director** - General availability for simplified cluster management
- **GKE Inference Gateway** - Public preview for AI/ML workload optimization
- **GKE Inference Quickstart** - Streamlined AI model deployment
- **A3 Ultra VMs** - AI-optimized compute with GPUDirect RDMA support
- **Cloud TPUs** - Available in GKE Autopilot for machine learning workloads
- **Performance HPA Profile** - Enhanced autoscaling reaction time and speed

### AI/ML Integration

GKE 2025 significantly enhances AI and machine learning capabilities:
- **Native TPU Support** in Autopilot clusters
- **GPU Performance Monitoring** with DCGM metrics
- **Advanced Container Platform** for optimized AI workloads
- **Reservation Sub-blocks** for specialized hardware targeting

### Container-Optimized Platform

- **New Autopilot Platform** - Rolling out with enhanced performance
- **Preloading Support** - Faster workload deployment and autoscaling
- **Local SSD Integration** - C4 machine series with high-performance storage

## Prerequisites

### Required Tools

```bash
# Google Cloud SDK
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# kubectl
gcloud components install kubectl

# Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Docker (for local builds)
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh
```

### Google Cloud Setup

```bash
# Initialize gcloud
gcloud init

# Set project
export PROJECT_ID=your-project-id
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable container.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable sqladmin.googleapis.com

# Set default region and zone
export REGION=us-central1
export ZONE=us-central1-a
gcloud config set compute/region $REGION
gcloud config set compute/zone $ZONE

# Authenticate for container registry
gcloud auth configure-docker
```

## Container Registry Setup

### Artifact Registry (Recommended 2025)

```bash
# Create Artifact Registry repository
gcloud artifacts repositories create cities-repo \
  --repository-format=docker \
  --location=$REGION \
  --description="Cities application container registry"

# Configure Docker authentication
gcloud auth configure-docker $REGION-docker.pkg.dev

# Set registry URL
REGISTRY_URL=$REGION-docker.pkg.dev/$PROJECT_ID/cities-repo
echo "Registry URL: $REGISTRY_URL"
```

### Build and Push Container Images

**Using Spring Boot Buildpacks:**

```bash
# Build image with Maven
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=$REGISTRY_URL/cities-web:latest

# Build image with Gradle
./gradlew bootBuildImage \
  --imageName=$REGISTRY_URL/cities-web:latest

# Push to Artifact Registry
docker push $REGISTRY_URL/cities-web:latest
docker push $REGISTRY_URL/cities-web:1.0.0-SNAPSHOT
```

**Using Cloud Build:**

```bash
# Build in Google Cloud (no local Docker required)
gcloud builds submit \
  --tag $REGISTRY_URL/cities-web:latest \
  .

# Automated builds with GitHub integration
gcloud alpha builds triggers create github \
  --repo-name=spring-boot-with-kotlin-and-jpa-example \
  --repo-owner=yourusername \
  --branch-pattern="^main$" \
  --build-config=cloudbuild.yaml
```

**cloudbuild.yaml:**
```yaml
steps:
# Build with buildpacks
- name: 'gcr.io/buildpacks/builder:v1'
  args: ['--tag', '$REGISTRY_URL/cities-web:$COMMIT_SHA']

# Build with Maven (alternative)
- name: 'maven:3.9-openjdk-21'
  entrypoint: 'mvn'
  args: ['spring-boot:build-image', '-Dspring-boot.build-image.imageName=$REGISTRY_URL/cities-web:$COMMIT_SHA']

# Push image
- name: 'gcr.io/cloud-builders/docker'
  args: ['push', '$REGISTRY_URL/cities-web:$COMMIT_SHA']

substitutions:
  _REGISTRY_URL: ${REGION}-docker.pkg.dev/${PROJECT_ID}/cities-repo

images:
- '$REGISTRY_URL/cities-web:$COMMIT_SHA'
```

## GKE Cluster Creation

### Option 1: GKE Autopilot (Recommended 2025)

```bash
# Create Autopilot cluster with latest features
gcloud container clusters create-auto cities-autopilot \
  --region=$REGION \
  --release-channel=rapid \
  --enable-network-policy \
  --enable-private-nodes \
  --enable-ip-alias \
  --disk-type=pd-ssd

# Get credentials
gcloud container clusters get-credentials cities-autopilot --region=$REGION

# Verify cluster
kubectl get nodes
```

### Option 2: GKE Standard with Advanced Features

```bash
# Create standard cluster with 2025 optimizations
gcloud container clusters create cities-standard \
  --zone=$ZONE \
  --cluster-version=1.33.1-gke.1584000 \
  --release-channel=rapid \
  --enable-autoscaling \
  --min-nodes=1 \
  --max-nodes=10 \
  --num-nodes=3 \
  --machine-type=e2-standard-4 \
  --disk-type=pd-ssd \
  --disk-size=50GB \
  --enable-autorepair \
  --enable-autoupgrade \
  --enable-network-policy \
  --enable-ip-alias \
  --enable-stackdriver-kubernetes \
  --enable-autoscaling \
  --enable-cluster-autoscaling

# Enable Performance HPA Profile (2025 feature)
gcloud container clusters update cities-standard \
  --zone=$ZONE \
  --enable-horizontal-pod-autoscaling-performance-profile

# Get credentials
gcloud container clusters get-credentials cities-standard --zone=$ZONE
```

### Infrastructure as Code (Terraform)

**main.tf:**
```hcl
provider "google" {
  project = var.project_id
  region  = var.region
}

resource "google_container_cluster" "cities_cluster" {
  name     = "cities-gke"
  location = var.zone

  # GKE Autopilot
  enable_autopilot = true

  # Release channel for latest features
  release_channel {
    channel = "RAPID"
  }

  # Network configuration
  network    = google_compute_network.vpc.name
  subnetwork = google_compute_subnetwork.subnet.name

  # Private cluster configuration
  private_cluster_config {
    enable_private_nodes    = true
    enable_private_endpoint = false
    master_ipv4_cidr_block  = "10.0.0.0/28"
  }

  # IP allocation policy
  ip_allocation_policy {
    cluster_ipv4_cidr_block  = "10.1.0.0/16"
    services_ipv4_cidr_block = "10.2.0.0/16"
  }

  # Monitoring and logging
  monitoring_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS"]
  }

  logging_config {
    enable_components = ["SYSTEM_COMPONENTS", "WORKLOADS"]
  }

  # Security configuration
  network_policy {
    enabled = true
  }

  # Workload Identity
  workload_identity_config {
    workload_pool = "${var.project_id}.svc.id.goog"
  }
}

# VPC Network
resource "google_compute_network" "vpc" {
  name                    = "cities-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "subnet" {
  name          = "cities-subnet"
  ip_cidr_range = "10.0.1.0/24"
  region        = var.region
  network       = google_compute_network.vpc.name

  secondary_ip_range {
    range_name    = "pods"
    ip_cidr_range = "10.1.0.0/16"
  }

  secondary_ip_range {
    range_name    = "services"
    ip_cidr_range = "10.2.0.0/16"
  }
}
```

**Deploy with Terraform:**
```bash
terraform init
terraform plan -var="project_id=$PROJECT_ID" -var="region=$REGION" -var="zone=$ZONE"
terraform apply
```

## Database Setup

### Cloud SQL for PostgreSQL

```bash
# Create Cloud SQL PostgreSQL instance
gcloud sql instances create cities-postgres \
  --database-version=POSTGRES_15 \
  --tier=db-g1-small \
  --region=$REGION \
  --storage-type=SSD \
  --storage-size=20GB \
  --storage-auto-increase \
  --backup-start-time=03:00 \
  --backup-location=$REGION \
  --maintenance-window-day=SUN \
  --maintenance-window-hour=04 \
  --deletion-protection

# Create database
gcloud sql databases create geo_data --instance=cities-postgres

# Create user
gcloud sql users create postgres \
  --instance=cities-postgres \
  --password=SecurePassword123!

# Get connection name
CONNECTION_NAME=$(gcloud sql instances describe cities-postgres \
  --format="value(connectionName)")
echo "Connection Name: $CONNECTION_NAME"

# Enable private IP (recommended for production)
gcloud sql instances patch cities-postgres \
  --network=projects/$PROJECT_ID/global/networks/cities-vpc \
  --no-assign-ip
```

### Cloud SQL Proxy for Secure Connection

```yaml
# cloud-sql-proxy-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cloud-sql-proxy
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cloud-sql-proxy
  template:
    metadata:
      labels:
        app: cloud-sql-proxy
    spec:
      serviceAccountName: cloud-sql-proxy-sa
      containers:
      - name: cloud-sql-proxy
        image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2.8.0
        args:
        - "--structured-logs"
        - "--port=5432"
        - "$(CONNECTION_NAME)"
        env:
        - name: CONNECTION_NAME
          value: "PROJECT_ID:REGION:cities-postgres"
        ports:
        - containerPort: 5432
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "200m"
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
spec:
  selector:
    app: cloud-sql-proxy
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
```

### Workload Identity Setup

```bash
# Create Google Service Account
gcloud iam service-accounts create cloud-sql-proxy-sa \
  --display-name="Cloud SQL Proxy Service Account"

# Grant Cloud SQL Client role
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:cloud-sql-proxy-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

# Create Kubernetes Service Account
kubectl create serviceaccount cloud-sql-proxy-sa

# Bind Google and Kubernetes Service Accounts
gcloud iam service-accounts add-iam-policy-binding \
  cloud-sql-proxy-sa@$PROJECT_ID.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:$PROJECT_ID.svc.id.goog[default/cloud-sql-proxy-sa]"

# Annotate Kubernetes Service Account
kubectl annotate serviceaccount cloud-sql-proxy-sa \
  iam.gke.io/gcp-service-account=cloud-sql-proxy-sa@$PROJECT_ID.iam.gserviceaccount.com
```

## Application Deployment

### Kubernetes Manifests

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cities-web
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cities-web
  template:
    metadata:
      labels:
        app: cities-web
    spec:
      serviceAccountName: cities-web-sa
      containers:
      - name: cities-web
        image: us-central1-docker.pkg.dev/PROJECT_ID/cities-repo/cities-web:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: POSTGRES_HOST
          value: "postgres-service"
        - name: POSTGRES_DB
          value: "geo_data"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
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
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5
        # Security context for restricted PSS
        securityContext:
          allowPrivilegeEscalation: false
          runAsNonRoot: true
          runAsUser: 1000
          capabilities:
            drop:
            - ALL
          seccompProfile:
            type: RuntimeDefault
---
apiVersion: v1
kind: Service
metadata:
  name: cities-web-service
  annotations:
    cloud.google.com/neg: '{"ingress": true}'
spec:
  selector:
    app: cities-web
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
---
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
type: Opaque
data:
  username: cG9zdGdyZXM=  # base64 encoded 'postgres'
  password: U2VjdXJlUGFzc3dvcmQxMjMh  # base64 encoded password
```

### Deploy Application

```bash
# Substitute environment variables
sed "s/PROJECT_ID/$PROJECT_ID/g" deployment.yaml | kubectl apply -f -

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get services

# Check logs
kubectl logs -l app=cities-web
```

## Google Cloud Load Balancer Integration

### Global HTTP(S) Load Balancer

**ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cities-web-ingress
  annotations:
    kubernetes.io/ingress.global-static-ip-name: cities-web-ip
    networking.gke.io/managed-certificates: cities-web-ssl
    kubernetes.io/ingress.class: "gce"
    kubernetes.io/ingress.allow-http: "false"
spec:
  rules:
  - host: cities.example.com
    http:
      paths:
      - path: /*
        pathType: ImplementationSpecific
        backend:
          service:
            name: cities-web-service
            port:
              number: 80
---
apiVersion: networking.gke.io/v1
kind: ManagedCertificate
metadata:
  name: cities-web-ssl
spec:
  domains:
  - cities.example.com
```

**Setup Global IP and SSL:**
```bash
# Reserve global static IP
gcloud compute addresses create cities-web-ip --global

# Get IP address
gcloud compute addresses describe cities-web-ip --global --format="value(address)"

# Apply ingress
kubectl apply -f ingress.yaml

# Check certificate status
kubectl describe managedcertificate cities-web-ssl
```

## Auto Scaling and Performance

### Horizontal Pod Autoscaler with Custom Metrics

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cities-web-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cities-web
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  # Custom metric from Google Cloud Monitoring
  - type: External
    external:
      metric:
        name: pubsub.googleapis.com|subscription|num_undelivered_messages
        selector:
          matchLabels:
            resource.labels.subscription_id: cities-queue
      target:
        type: AverageValue
        averageValue: "30"
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
```

### Vertical Pod Autoscaler

```yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: cities-web-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: cities-web
  updatePolicy:
    updateMode: "Auto"
  resourcePolicy:
    containerPolicies:
    - containerName: cities-web
      minAllowed:
        cpu: 100m
        memory: 128Mi
      maxAllowed:
        cpu: 2
        memory: 4Gi
      controlledResources: ["cpu", "memory"]
```

### Node Auto Provisioning

```bash
# Enable node auto provisioning
gcloud container clusters update cities-standard \
  --zone=$ZONE \
  --enable-autoprovisioning \
  --max-cpu=32 \
  --max-memory=128 \
  --max-accelerator=type=nvidia-tesla-k80,count=4

# Create specialized node pool for high-memory workloads
gcloud container node-pools create high-memory-pool \
  --cluster=cities-standard \
  --zone=$ZONE \
  --machine-type=n2-highmem-4 \
  --num-nodes=0 \
  --enable-autoscaling \
  --min-nodes=0 \
  --max-nodes=5 \
  --node-taints=workload-type=memory-intensive:NoSchedule
```

## AI/ML Integration (2025 Features)

### TPU Support in Autopilot

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-workload
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ai-workload
  template:
    metadata:
      labels:
        app: ai-workload
    spec:
      containers:
      - name: ai-container
        image: gcr.io/tpu-pytorch/xla:r2.0_3.8_tpu
        resources:
          requests:
            google.com/tpu: 8  # Request TPU v4-8
          limits:
            google.com/tpu: 8
        command: ["python3", "train.py"]
      nodeSelector:
        cloud.google.com/gke-accelerator: tpu-v4-podslice
```

### GPU Workloads with A3 Ultra VMs

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gpu-workload
spec:
  replicas: 1
  selector:
    matchLabels:
      app: gpu-workload
  template:
    metadata:
      labels:
        app: gpu-workload
    spec:
      containers:
      - name: gpu-container
        image: nvidia/cuda:12.0-runtime-ubuntu20.04
        resources:
          requests:
            nvidia.com/gpu: 8  # A3 Ultra with 8 GPUs
          limits:
            nvidia.com/gpu: 8
        command: ["nvidia-smi"]
      nodeSelector:
        cloud.google.com/gke-accelerator: nvidia-h100-80gb
        cloud.google.com/machine-family: a3-ultragpu-8g
      tolerations:
      - key: nvidia.com/gpu
        operator: Exists
        effect: NoSchedule
```

## Monitoring and Observability

### Google Cloud Operations Suite

**Enable monitoring:**
```bash
# Already enabled with cluster creation
# Verify monitoring
kubectl get pods -n gke-system | grep metrics-server
kubectl get pods -n kube-system | grep fluentbit
```

**Custom monitoring configuration:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: cities-web-monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
    - job_name: 'cities-web'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_label_app]
        action: keep
        regex: cities-web
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        regex: (.+)
        target_label: __address__
        replacement: ${1}
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
```

### Cloud Trace Integration

**Add to application:**
```yaml
env:
- name: GOOGLE_CLOUD_PROJECT
  value: "PROJECT_ID"
- name: SPRING_SLEUTH_ZIPKIN_ENABLED
  value: "false"
- name: SPRING_SLEUTH_GCP_TRACE_ENABLED
  value: "true"
```

### Error Reporting

```yaml
env:
- name: GOOGLE_CLOUD_PROJECT
  value: "PROJECT_ID"
- name: SPRING_CLOUD_GCP_ERROR_REPORTING_ENABLED
  value: "true"
```

## Security Best Practices

### Binary Authorization

```bash
# Create Binary Authorization policy
cat <<EOF > binauth-policy.yaml
admissionWhitelistPatterns:
- namePattern: $REGION-docker.pkg.dev/$PROJECT_ID/cities-repo/*
defaultAdmissionRule:
  requireAttestationsBy:
  - projects/$PROJECT_ID/attestors/prod-attestor
  evaluationMode: REQUIRE_ATTESTATION
  enforcementMode: ENFORCED_BLOCK_AND_AUDIT_LOG
EOF

# Apply policy
gcloud container binauthz policy import binauth-policy.yaml
```

### Workload Identity for Service Accounts

```bash
# Create Google Service Account for application
gcloud iam service-accounts create cities-web-sa \
  --display-name="Cities Web Application Service Account"

# Grant necessary permissions
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:cities-web-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudsql.client"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:cities-web-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/cloudtrace.agent"

# Create Kubernetes Service Account
kubectl create serviceaccount cities-web-sa

# Bind accounts
gcloud iam service-accounts add-iam-policy-binding \
  cities-web-sa@$PROJECT_ID.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="serviceAccount:$PROJECT_ID.svc.id.goog[default/cities-web-sa]"

kubectl annotate serviceaccount cities-web-sa \
  iam.gke.io/gcp-service-account=cities-web-sa@$PROJECT_ID.iam.gserviceaccount.com
```

### Pod Security Standards

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: cities
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

## CI/CD Pipeline

### Cloud Build with GitHub Integration

**cloudbuild.yaml:**
```yaml
steps:
# Run tests
- name: 'maven:3.9-openjdk-21'
  entrypoint: 'mvn'
  args: ['test']

# Build image with buildpacks
- name: 'gcr.io/buildpacks/builder:v1'
  args:
  - '--tag'
  - '$REGISTRY_URL/cities-web:$COMMIT_SHA'
  - '--tag'
  - '$REGISTRY_URL/cities-web:latest'

# Security scanning
- name: 'gcr.io/cloud-builders/gcloud'
  args:
  - 'beta'
  - 'container'
  - 'images'
  - 'scan'
  - '$REGISTRY_URL/cities-web:$COMMIT_SHA'

# Deploy to GKE
- name: 'gcr.io/cloud-builders/gke-deploy'
  args:
  - 'run'
  - '--filename=k8s/'
  - '--cluster=cities-autopilot'
  - '--location=$REGION'
  - '--image=$REGISTRY_URL/cities-web:$COMMIT_SHA'

substitutions:
  _REGISTRY_URL: ${REGION}-docker.pkg.dev/${PROJECT_ID}/cities-repo

options:
  logging: CLOUD_LOGGING_ONLY
```

### GitHub Actions

```yaml
name: Deploy to GKE
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v5

    - id: 'auth'
      uses: 'google-github-actions/auth@v2'
      with:
        credentials_json: '${{ secrets.GCP_SA_KEY }}'

    - name: 'Set up Cloud SDK'
      uses: 'google-github-actions/setup-gcloud@v2'

    - name: 'Configure Docker'
      run: gcloud auth configure-docker $REGION-docker.pkg.dev

    - name: 'Build and Push'
      run: |
        gcloud builds submit \
          --tag $REGISTRY_URL/cities-web:$GITHUB_SHA \
          --tag $REGISTRY_URL/cities-web:latest

    - name: 'Deploy to GKE'
      run: |
        gcloud container clusters get-credentials cities-autopilot --region $REGION
        kubectl set image deployment/cities-web cities-web=$REGISTRY_URL/cities-web:$GITHUB_SHA
        kubectl rollout status deployment/cities-web
      env:
        REGISTRY_URL: us-central1-docker.pkg.dev/${{ secrets.PROJECT_ID }}/cities-repo
```

## Cost Optimization

### Preemptible Instances

```bash
# Create preemptible node pool
gcloud container node-pools create preemptible-pool \
  --cluster=cities-standard \
  --zone=$ZONE \
  --preemptible \
  --machine-type=e2-standard-2 \
  --num-nodes=3 \
  --enable-autoscaling \
  --min-nodes=0 \
  --max-nodes=10
```

### Spot VMs (Successor to Preemptible)

```bash
# Create Spot VM node pool
gcloud container node-pools create spot-pool \
  --cluster=cities-standard \
  --zone=$ZONE \
  --spot \
  --machine-type=e2-standard-2 \
  --num-nodes=3 \
  --enable-autoscaling \
  --min-nodes=0 \
  --max-nodes=10
```

### Resource Quotas

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: cities-quota
spec:
  hard:
    requests.cpu: "4"
    requests.memory: 8Gi
    limits.cpu: "8"
    limits.memory: 16Gi
    persistentvolumeclaims: "4"
    pods: "10"
```

## Backup and Disaster Recovery

### Database Backups

```bash
# Automated backups are enabled by default
# Create on-demand backup
gcloud sql backups create \
  --instance=cities-postgres \
  --description="Pre-deployment backup"

# List backups
gcloud sql backups list --instance=cities-postgres

# Restore from backup
gcloud sql backups restore BACKUP_ID \
  --restore-instance=cities-postgres-restored \
  --backup-instance=cities-postgres
```

### Application Backups with Velero

```bash
# Install Velero with Google Cloud Storage
velero install \
  --provider gcp \
  --plugins velero/velero-plugin-for-gcp:v1.7.0 \
  --bucket cities-backup-bucket \
  --secret-file ./credentials-velero

# Create backup
velero backup create cities-backup --include-namespaces default

# Schedule regular backups
velero create schedule cities-daily --schedule="0 2 * * *" --ttl 720h0m0s
```

## Troubleshooting

### Common Issues

**Image Pull Errors:**
```bash
# Check Artifact Registry permissions
gcloud artifacts repositories get-iam-policy cities-repo --location=$REGION

# Verify image exists
gcloud artifacts docker images list $REGION-docker.pkg.dev/$PROJECT_ID/cities-repo
```

**Networking Issues:**
```bash
# Check VPC connectivity
gcloud compute networks describe cities-vpc
gcloud compute firewall-rules list --filter="network:cities-vpc"

# Test Cloud SQL connectivity
kubectl run debug --image=busybox --rm -it --restart=Never -- nc -zv postgres-service 5432
```

**Performance Issues:**
```bash
# Check GKE cluster status
gcloud container clusters describe cities-autopilot --region=$REGION

# View metrics
gcloud logging read "resource.type=k8s_container AND resource.labels.cluster_name=cities-autopilot" --limit=50
```

### Google Cloud Specific Debugging

```bash
# Check quotas
gcloud compute project-info describe --project=$PROJECT_ID

# View audit logs
gcloud logging read "protoPayload.serviceName=container.googleapis.com" --limit=10

# Check service health
gcloud container operations list
```

This comprehensive guide covers deploying the Cities application to Google Cloud GKE using the latest 2025 features, including AI/ML capabilities, advanced autoscaling, and security best practices.