# AWS Deployment Guide

## Overview

Deploy the Spring Boot Cities application to **Amazon Web Services (AWS)** using modern container orchestration with **Amazon Elastic Kubernetes Service (EKS)**.

## AWS EKS 2025 Features

### Latest Capabilities

- **Kubernetes 1.33+** - Latest upstream Kubernetes versions
- **Managed Node Groups** - Automated node lifecycle management
- **EKS Automatic** - Simplified cluster creation and management
- **Enhanced Security** - IAM RBAC integration and Pod Security Standards
- **Cost Optimization** - Spot instances and auto-scaling
- **Observability** - CloudWatch Container Insights integration

### EKS Version Support

AWS EKS supports Kubernetes versions for approximately 14 months:

- **Standard Support**: First 14 months after release
- **Extended Support**: Additional 12 months (with fees)
- **Release Cadence**: 3 minor versions annually

Current supported versions (as of 2025):

- Kubernetes 1.33.x (latest)
- Kubernetes 1.32.x (stable)
- Kubernetes 1.31.x (supported)

## Prerequisites

### Required Tools

```bash
# AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# eksctl
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin

# Helm (for package management)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

### AWS Account Setup

```bash
# Configure AWS credentials
aws configure

# Verify account access
aws sts get-caller-identity

# Set default region
export AWS_DEFAULT_REGION=us-west-2
```

## Container Registry Setup

### Amazon Elastic Container Registry (ECR)

```bash
# Create ECR repository
aws ecr create-repository \
  --repository-name cities-web \
  --region us-west-2

# Get registry URI
REGISTRY_URI=$(aws ecr describe-repositories \
  --repository-names cities-web \
  --query 'repositories[0].repositoryUri' \
  --output text)

echo "Registry URI: $REGISTRY_URI"
```

### Build and Push Container Images

**Using Spring Boot Buildpacks:**

```bash
# Login to ECR
aws ecr get-login-password --region us-west-2 | \
  docker login --username AWS --password-stdin $REGISTRY_URI

# Build image with Maven
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=$REGISTRY_URI:latest

# Build image with Gradle
./gradlew bootBuildImage \
  --imageName=$REGISTRY_URI:latest

# Push to ECR
docker push $REGISTRY_URI:latest
docker push $REGISTRY_URI:1.0.0-SNAPSHOT
```

**Using Traditional Docker:**

```bash
# Build using Dockerfile
docker build -t cities-web .

# Tag for ECR
docker tag cities-web:latest $REGISTRY_URI:latest

# Push to ECR
docker push $REGISTRY_URI:latest
```

## EKS Cluster Creation

### Option 1: EKS Automatic (Recommended 2025)

```bash
# Create cluster with EKS Automatic
eksctl create cluster \
  --name cities-cluster \
  --region us-west-2 \
  --tier automatic \
  --version 1.33

# Verify cluster
kubectl get nodes
```

### Option 2: Manual Configuration

```bash
# Create cluster with managed node groups
eksctl create cluster \
  --name cities-cluster \
  --version 1.33 \
  --region us-west-2 \
  --nodegroup-name standard-workers \
  --node-type t3.medium \
  --nodes 3 \
  --nodes-min 1 \
  --nodes-max 4 \
  --managed

# Enable logging
eksctl utils update-cluster-logging \
  --enable-types all \
  --cluster cities-cluster \
  --region us-west-2
```

### Infrastructure as Code (CloudFormation)

**cluster.yaml:**

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: 'EKS Cluster for Cities Application'

Parameters:
  ClusterName:
    Type: String
    Default: cities-cluster

Resources:
  EKSCluster:
    Type: AWS::EKS::Cluster
    Properties:
      Name: !Ref ClusterName
      Version: '1.33'
      RoleArn: !GetAtt EKSClusterRole.Arn
      ResourcesVpcConfig:
        SubnetIds:
          - !Ref PublicSubnet1
          - !Ref PublicSubnet2
          - !Ref PrivateSubnet1
          - !Ref PrivateSubnet2
      Logging:
        ClusterLogging:
          EnabledTypes:
            - Type: api
            - Type: audit
            - Type: authenticator
            - Type: controllerManager
            - Type: scheduler

  NodeGroup:
    Type: AWS::EKS::Nodegroup
    Properties:
      ClusterName: !Ref EKSCluster
      NodegroupName: standard-workers
      NodeRole: !GetAtt NodeInstanceRole.Arn
      InstanceTypes:
        - t3.medium
      ScalingConfig:
        MinSize: 1
        MaxSize: 4
        DesiredSize: 3
      Subnets:
        - !Ref PrivateSubnet1
        - !Ref PrivateSubnet2
```

**Deploy with:**

```bash
aws cloudformation deploy \
  --template-file cluster.yaml \
  --stack-name cities-eks-cluster \
  --capabilities CAPABILITY_IAM
```

## Database Setup

### Amazon RDS PostgreSQL

```bash
# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier cities-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.7 \
  --master-username postgres \
  --master-user-password SecurePassword123! \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-xxxxxxxx \
  --db-subnet-group-name cities-db-subnet-group \
  --backup-retention-period 7 \
  --storage-encrypted

# Get RDS endpoint
RDS_ENDPOINT=$(aws rds describe-db-instances \
  --db-instance-identifier cities-postgres \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text)

echo "RDS Endpoint: $RDS_ENDPOINT"
```

### Alternative: PostgreSQL on EKS

```yaml
# postgres-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_DB
          value: geo_data
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: postgres-secret
              key: password
        ports:
        - containerPort: 5432
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
  type: ClusterIP
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
      containers:
      - name: cities-web
        image: ${REGISTRY_URI}:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: POSTGRES_HOST
          value: "cities-postgres.xxxxxx.us-west-2.rds.amazonaws.com"
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
---
apiVersion: v1
kind: Service
metadata:
  name: cities-web-service
spec:
  selector:
    app: cities-web
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
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
envsubst < deployment.yaml | kubectl apply -f -

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get services

# Get external IP
kubectl get service cities-web-service -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

### Helm Chart Deployment

**Chart.yaml:**

```yaml
apiVersion: v2
name: cities-web
description: A Helm chart for Cities Web Application
type: application
version: 0.1.0
appVersion: "1.0.0"
```

**values.yaml:**

```yaml
replicaCount: 3

image:
  repository: ${REGISTRY_URI}
  tag: latest
  pullPolicy: Always

service:
  type: LoadBalancer
  port: 80
  targetPort: 8080

ingress:
  enabled: false

resources:
  limits:
    cpu: 500m
    memory: 1Gi
  requests:
    cpu: 250m
    memory: 512Mi

database:
  host: cities-postgres.xxxxxx.us-west-2.rds.amazonaws.com
  name: geo_data
  port: 5432

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
```

**Install with Helm:**

```bash
# Install chart
helm install cities-web ./helm/cities-web \
  --set image.repository=$REGISTRY_URI \
  --set database.host=$RDS_ENDPOINT

# Upgrade
helm upgrade cities-web ./helm/cities-web \
  --set image.tag=1.0.1

# Uninstall
helm uninstall cities-web
```

## Security Best Practices

### IAM Roles and Service Accounts

**Create OIDC provider:**

```bash
eksctl utils associate-iam-oidc-provider \
  --cluster cities-cluster \
  --approve
```

**Create service account with IAM role:**

```bash
eksctl create iamserviceaccount \
  --name cities-web-sa \
  --namespace default \
  --cluster cities-cluster \
  --attach-policy-arn arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy \
  --approve
```

### Pod Security Standards

**pod-security-policy.yaml:**

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

### Network Policies

**network-policy.yaml:**

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: cities-web-netpol
spec:
  podSelector:
    matchLabels:
      app: cities-web
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: nginx-ingress
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to: []
    ports:
    - protocol: TCP
      port: 5432  # PostgreSQL
    - protocol: TCP
      port: 80   # HTTP
    - protocol: TCP
      port: 443  # HTTPS
```

## Auto Scaling

### Horizontal Pod Autoscaler (HPA)

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
  maxReplicas: 10
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
```

### Cluster Autoscaler

```bash
# Enable cluster autoscaler on node group
eksctl scale nodegroup \
  --cluster cities-cluster \
  --name standard-workers \
  --nodes 3 \
  --nodes-min 1 \
  --nodes-max 10
```

### Vertical Pod Autoscaler (VPA)

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
        cpu: 1
        memory: 2Gi
```

## Monitoring and Observability

### CloudWatch Container Insights

```bash
# Enable Container Insights
aws eks update-cluster-config \
  --region us-west-2 \
  --name cities-cluster \
  --logging '{"enable":["api","audit","authenticator","controllerManager","scheduler"]}'

# Deploy CloudWatch agent
kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cloudwatch-namespace.yaml

kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cwagentconfig.yaml

kubectl apply -f https://raw.githubusercontent.com/aws-samples/amazon-cloudwatch-container-insights/latest/k8s-deployment-manifest-templates/deployment-mode/daemonset/container-insights-monitoring/cloudwatch-agent-daemonset.yaml
```

### Prometheus and Grafana

```bash
# Add Helm repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace

# Install Grafana
helm install grafana grafana/grafana \
  --namespace monitoring \
  --set adminPassword=admin123
```

### Application Performance Monitoring

**ServiceMonitor for Prometheus:**

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: cities-web-metrics
spec:
  selector:
    matchLabels:
      app: cities-web
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
```

## Load Balancing and Ingress

### Application Load Balancer (ALB)

```bash
# Install AWS Load Balancer Controller
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.4.4/docs/install/iam_policy.json

aws iam create-policy \
  --policy-name AWSLoadBalancerControllerIAMPolicy \
  --policy-document file://iam_policy.json

eksctl create iamserviceaccount \
  --cluster=cities-cluster \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::ACCOUNT-ID:policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --approve

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=cities-cluster \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

**Ingress Resource:**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cities-web-ingress
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}, {"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: '443'
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:us-west-2:ACCOUNT-ID:certificate/CERT-ID
spec:
  rules:
  - host: cities.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: cities-web-service
            port:
              number: 80
```

## CI/CD Pipeline

### GitHub Actions with AWS

```yaml
name: Deploy to EKS
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v5

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2

    - name: Login to Amazon ECR
      id: login-ecr
      uses: aws-actions/amazon-ecr-login@v1

    - name: Build and push image
      env:
        ECR_REGISTRY: ${{ steps.login-ecr.outputs.registry }}
        ECR_REPOSITORY: cities-web
        IMAGE_TAG: ${{ github.sha }}
      run: |
        ./gradlew bootBuildImage --imageName=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

    - name: Deploy to EKS
      run: |
        aws eks update-kubeconfig --region us-west-2 --name cities-cluster
        kubectl set image deployment/cities-web cities-web=$ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG
        kubectl rollout status deployment/cities-web
```

## Cost Optimization

### Spot Instances

```bash
# Create spot instance node group
eksctl create nodegroup \
  --cluster cities-cluster \
  --name spot-workers \
  --node-type m5.large \
  --nodes 2 \
  --nodes-min 0 \
  --nodes-max 5 \
  --spot
```

### Reserved Instances

- Purchase Reserved Instances for predictable workloads
- Use Savings Plans for flexible compute usage
- Monitor with AWS Cost Explorer

### Resource Right-Sizing

```bash
# Use VPA recommendations
kubectl get vpa cities-web-vpa -o yaml

# Monitor actual usage
kubectl top pods
kubectl top nodes
```

## Backup and Disaster Recovery

### Database Backups

```bash
# RDS automated backups (configured during creation)
aws rds modify-db-instance \
  --db-instance-identifier cities-postgres \
  --backup-retention-period 7 \
  --apply-immediately

# Manual snapshot
aws rds create-db-snapshot \
  --db-instance-identifier cities-postgres \
  --db-snapshot-identifier cities-postgres-snapshot-$(date +%Y%m%d)
```

### Application Backups with Velero

```bash
# Install Velero
wget https://github.com/vmware-tanzu/velero/releases/latest/download/velero-linux-amd64.tar.gz
tar -xzf velero-linux-amd64.tar.gz
sudo mv velero-*/velero /usr/local/bin/

# Configure with S3 backend
velero install \
  --provider aws \
  --plugins velero/velero-plugin-for-aws:v1.7.0 \
  --bucket cities-backup-bucket \
  --backup-location-config region=us-west-2 \
  --snapshot-location-config region=us-west-2

# Create backup
velero backup create cities-backup --include-namespaces default
```

## Troubleshooting

### Common Issues

**Pod Startup Issues:**

```bash
# Check pod logs
kubectl logs deployment/cities-web

# Check pod events
kubectl describe pod <pod-name>

# Check resource constraints
kubectl top pods
```

**Database Connection Issues:**

```bash
# Test connectivity from pod
kubectl exec -it <pod-name> -- nc -zv $POSTGRES_HOST 5432

# Check security groups
aws ec2 describe-security-groups --group-ids sg-xxxxxxxx
```

**Load Balancer Issues:**

```bash
# Check service endpoints
kubectl get endpoints cities-web-service

# Check ALB controller logs
kubectl logs -n kube-system deployment/aws-load-balancer-controller
```

### Performance Debugging

```bash
# Application metrics
curl http://<load-balancer-url>/actuator/metrics

# JVM analysis
kubectl exec -it <pod-name> -- jstack 1

# Memory usage
kubectl exec -it <pod-name> -- jmap -histo 1
```

This comprehensive guide covers deploying the Cities application to AWS EKS using 2025 best practices, including security, monitoring, and cost optimization.
