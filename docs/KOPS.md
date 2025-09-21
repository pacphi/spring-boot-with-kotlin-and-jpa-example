# Deploying Cities Application with KOPS on AWS

This guide demonstrates how to deploy the Cities Spring Boot application to a production-grade Kubernetes cluster on AWS using KOPS (Kubernetes Operations).

## What is KOPS?

KOPS (Kubernetes Operations) is a powerful CLI tool that automates the deployment and management of production-grade, highly available Kubernetes clusters on AWS. KOPS not only helps you create, destroy, upgrade and maintain Kubernetes clusters but also provisions the necessary cloud infrastructure including VPC, subnets, EC2 instances, auto-scaling groups, security groups, Route 53 DNS, and S3 storage.

## Key Features

✅ **Complete Infrastructure Management** - VPC, Subnets, EC2 Instances, Auto Scaling Groups, Security Groups
✅ **DNS Management** - Route 53 Hosted Zone integration
✅ **State Storage** - S3 bucket for cluster configuration
✅ **High Availability** - Multi-AZ master and worker node deployment
✅ **Auto Scaling** - Dynamic node scaling based on demand
✅ **Rolling Updates** - Zero-downtime Kubernetes version upgrades
✅ **Security** - IAM roles, security groups, and encryption

## Prerequisites

### Required Tools

- **AWS CLI** configured with appropriate credentials
- **kubectl** (Kubernetes CLI)
- **KOPS** (latest version)
- **Java 21** (for building the application)
- **Maven 3.9+** or **Gradle 8.14+**

### AWS Requirements

- **AWS Account** with sufficient permissions
- **Domain** or subdomain for cluster DNS (e.g., `k8s.example.com`)
- **S3 Bucket** for KOPS state storage
- **EC2 Key Pair** for SSH access

### AWS Permissions

Your AWS user needs these permissions:

- AmazonEC2FullAccess
- AmazonRoute53FullAccess
- AmazonS3FullAccess
- IAMFullAccess
- AmazonVPCFullAccess

## Installation

### Install KOPS

#### macOS

```bash
# Using Homebrew
brew install kops

# Using curl
curl -Lo kops https://github.com/kubernetes/kops/releases/latest/download/kops-darwin-amd64
chmod +x kops
sudo mv kops /usr/local/bin/kops
```

#### Linux

```bash
curl -Lo kops https://github.com/kubernetes/kops/releases/latest/download/kops-linux-amd64
chmod +x kops
sudo mv kops /usr/local/bin/kops
```

#### Windows

```powershell
# Download from GitHub releases
# https://github.com/kubernetes/kops/releases/latest
```

### Verify Installation

```bash
kops version
aws --version
kubectl version --client
```

## AWS Setup

### Configure AWS CLI

```bash
# Configure AWS credentials
aws configure
# Enter: Access Key ID, Secret Access Key, Default Region (us-west-2), Output format (json)

# Verify configuration
aws sts get-caller-identity
```

### Create S3 Bucket for State Storage

```bash
# Set variables
export KOPS_CLUSTER_NAME=cities.k8s.local
export KOPS_STATE_STORE=s3://cities-kops-state-store
export AWS_REGION=us-west-2

# Create S3 bucket
aws s3 mb ${KOPS_STATE_STORE} --region ${AWS_REGION}

# Enable versioning
aws s3api put-bucket-versioning --bucket cities-kops-state-store --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption --bucket cities-kops-state-store --server-side-encryption-configuration '{
  "Rules": [
    {
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      }
    }
  ]
}'
```

### Set Environment Variables

```bash
# Add to ~/.bashrc or ~/.zshrc
export KOPS_CLUSTER_NAME=cities.k8s.local
export KOPS_STATE_STORE=s3://cities-kops-state-store
export AWS_REGION=us-west-2

# Source the file
source ~/.bashrc  # or ~/.zshrc
```

### Create EC2 Key Pair

```bash
# Create key pair
aws ec2 create-key-pair --key-name cities-kops-key --query 'KeyMaterial' --output text > ~/.ssh/cities-kops-key.pem
chmod 400 ~/.ssh/cities-kops-key.pem

# Or use existing key
# aws ec2 describe-key-pairs --key-names your-existing-key
```

## Cluster Creation

### Create Cluster Configuration

#### Basic Cluster

```bash
kops create cluster \
    --name=${KOPS_CLUSTER_NAME} \
    --state=${KOPS_STATE_STORE} \
    --zones=us-west-2a,us-west-2b,us-west-2c \
    --node-count=3 \
    --node-size=t3.medium \
    --master-size=t3.medium \
    --master-count=3 \
    --ssh-public-key=~/.ssh/cities-kops-key.pem.pub \
    --dry-run \
    --output yaml > cities-cluster.yaml
```

#### Production-Grade Cluster

```bash
kops create cluster \
    --name=${KOPS_CLUSTER_NAME} \
    --state=${KOPS_STATE_STORE} \
    --zones=us-west-2a,us-west-2b,us-west-2c \
    --node-count=6 \
    --node-size=t3.large \
    --master-size=t3.medium \
    --master-count=3 \
    --master-zones=us-west-2a,us-west-2b,us-west-2c \
    --ssh-public-key=~/.ssh/cities-kops-key.pem.pub \
    --kubernetes-version=1.33.0 \
    --networking=calico \
    --topology=private \
    --bastion \
    --encrypt-etcd-storage \
    --dry-run \
    --output yaml > cities-cluster-prod.yaml
```

### Customize Cluster Configuration

```bash
# Edit cluster configuration
kops edit cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Edit node instance group
kops edit ig nodes --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Edit master instance group
kops edit ig master-us-west-2a --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
```

### Deploy Cluster

```bash
# Create the cluster (this takes 10-15 minutes)
kops update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes

# Wait for cluster to be ready
kops validate cluster --wait 10m --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
```

### Verify Cluster

```bash
# Check cluster status
kubectl cluster-info
kubectl get nodes
kubectl get pods --all-namespaces

# Check KOPS cluster status
kops get cluster --state=${KOPS_STATE_STORE}
```

## Application Deployment

### Step 1: Build and Push Application Image

#### Option A: Push to ECR

```bash
# Create ECR repository
aws ecr create-repository --repository-name cities-web --region ${AWS_REGION}

# Get login token
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin $(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com

# Build and tag image
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=cities-web:latest
docker tag cities-web:latest $(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com/cities-web:latest

# Push to ECR
docker push $(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com/cities-web:latest
```

#### Option B: Use Docker Hub

```bash
# Build and tag for Docker Hub
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=your-dockerhub-username/cities-web:latest

# Push to Docker Hub
docker push your-dockerhub-username/cities-web:latest
```

### Step 2: Create Namespace and Storage

```bash
# Create namespace
kubectl create namespace cities

# Create storage class for SSD volumes
kubectl apply -f - <<EOF
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ssd-retain
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  fsType: ext4
  encrypted: "true"
reclaimPolicy: Retain
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer
EOF
```

### Step 3: Deploy PostgreSQL with High Availability

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: cities
type: Opaque
data:
  username: cG9zdGdyZXM=  # base64: postgres
  password: $(echo -n 'your-secure-password' | base64 -w 0)
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
kind: PersistentVolumeClaim
metadata:
  name: postgres-pv-claim
  namespace: cities
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: ssd-retain
  resources:
    requests:
      storage: 20Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: cities
  labels:
    app: postgres
spec:
  selector:
    matchLabels:
      app: postgres
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: postgres
    spec:
      securityContext:
        fsGroup: 999
      volumes:
        - name: postgres-storage
          persistentVolumeClaim:
            claimName: postgres-pv-claim
      containers:
        - image: postgres:15
          name: postgres
          env:
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
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: postgres-config
                  key: database-name
            - name: PGDATA
              value: /var/lib/postgresql/data/pgdata
          ports:
            - containerPort: 5432
              name: postgres
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
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 15
            timeoutSeconds: 2
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 45
            timeoutSeconds: 2
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: cities
  labels:
    app: postgres
spec:
  type: ClusterIP
  ports:
    - port: 5432
  selector:
    app: postgres
EOF
```

### Step 4: Deploy Cities Application

```bash
# Set your image repository
export IMAGE_REPO=$(aws sts get-caller-identity --query Account --output text).dkr.ecr.${AWS_REGION}.amazonaws.com/cities-web:latest

kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: cities-web-config
  namespace: cities
data:
  spring_profiles_active: k8s,seeded
  java_opts: -XX:+UseG1GC -Xmx2G -XX:MaxRAMPercentage=75
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
        image: ${IMAGE_REPO}
        imagePullPolicy: Always
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          valueFrom:
            configMapKeyRef:
              name: cities-web-config
              key: spring_profiles_active
        - name: JAVA_OPTS
          valueFrom:
            configMapKeyRef:
              name: cities-web-config
              key: java_opts
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
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 90
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 60
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
        - name: cache-volume
          mountPath: /home/app/.cache
      volumes:
      - name: tmp-volume
        emptyDir: {}
      - name: cache-volume
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
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: cities-web-hpa
  namespace: cities
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
EOF
```

### Step 5: Configure Load Balancer and Ingress

#### Option A: Application Load Balancer (ALB)

```bash
# Install AWS Load Balancer Controller
curl -o iam_policy.json https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.9.0/docs/install/iam_policy.json

aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

# Create service account
eksctl create iamserviceaccount \
  --cluster=${KOPS_CLUSTER_NAME} \
  --namespace=kube-system \
  --name=aws-load-balancer-controller \
  --attach-policy-arn=arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):policy/AWSLoadBalancerControllerIAMPolicy \
  --override-existing-serviceaccounts \
  --approve

# Install controller
kubectl apply \
    --validate=false \
    -f https://github.com/jetstack/cert-manager/releases/download/v1.16.0/cert-manager.yaml

helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=${KOPS_CLUSTER_NAME} \
  --set serviceAccount.create=false \
  --set serviceAccount.name=aws-load-balancer-controller
```

#### Option B: Classic Load Balancer

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: cities-web-lb
  namespace: cities
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "classic"
    service.beta.kubernetes.io/aws-load-balancer-cross-zone-load-balancing-enabled: "true"
    service.beta.kubernetes.io/aws-load-balancer-connection-draining-enabled: "true"
    service.beta.kubernetes.io/aws-load-balancer-connection-draining-timeout: "60"
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8080
    protocol: TCP
    name: http
  selector:
    app: cities-web
EOF
```

## Monitoring and Operations

### Install Monitoring Stack

#### Prometheus and Grafana

```bash
# Add Prometheus Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set grafana.adminPassword=admin123 \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.storageClassName=ssd-retain
```

#### Cluster Autoscaler

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    app: cluster-autoscaler
spec:
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
    spec:
      serviceAccountName: cluster-autoscaler
      containers:
      - image: k8s.gcr.io/autoscaling/cluster-autoscaler:v1.33.0
        name: cluster-autoscaler
        resources:
          limits:
            cpu: 100m
            memory: 300Mi
          requests:
            cpu: 100m
            memory: 300Mi
        command:
        - ./cluster-autoscaler
        - --v=4
        - --stderrthreshold=info
        - --cloud-provider=aws
        - --skip-nodes-with-local-storage=false
        - --expander=least-waste
        - --node-group-auto-discovery=asg:tag=k8s.io/cluster-autoscaler/enabled,k8s.io/cluster-autoscaler/${KOPS_CLUSTER_NAME}
        - --balance-similar-node-groups
        - --skip-nodes-with-system-pods=false
        env:
        - name: AWS_REGION
          value: ${AWS_REGION}
EOF
```

### Logging with ELK Stack

```bash
# Install Elasticsearch
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch \
  --namespace logging \
  --create-namespace \
  --set replicas=3 \
  --set volumeClaimTemplate.resources.requests.storage=30Gi \
  --set volumeClaimTemplate.storageClassName=ssd-retain

# Install Kibana
helm install kibana elastic/kibana \
  --namespace logging \
  --set service.type=LoadBalancer

# Install Filebeat
helm install filebeat elastic/filebeat \
  --namespace logging
```

## Scaling and Maintenance

### Manual Scaling

```bash
# Scale worker nodes
kops edit ig nodes --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
# Change minSize and maxSize
kops update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes
kops rolling-update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes

# Scale application
kubectl scale deployment cities-web --replicas=5 -n cities
```

### Kubernetes Version Upgrade

```bash
# Check available versions
kops get cluster --state=${KOPS_STATE_STORE} -o yaml

# Edit cluster to new Kubernetes version
kops edit cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
# Update kubernetesVersion

# Apply upgrade
kops update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes
kops rolling-update cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes
```

### Backup and Disaster Recovery

```bash
# Backup cluster state
kops get cluster --state=${KOPS_STATE_STORE} -o yaml > cluster-backup.yaml

# Backup application data
kubectl exec -n cities deployment/postgres -- pg_dump -U postgres geo_data > cities-db-backup.sql

# Create snapshots of EBS volumes
aws ec2 describe-instances --filters "Name=tag:k8s.io/cluster/${KOPS_CLUSTER_NAME},Values=owned" \
  --query 'Reservations[].Instances[].BlockDeviceMappings[].Ebs.VolumeId' --output text | \
  xargs -I {} aws ec2 create-snapshot --volume-id {} --description "Backup for ${KOPS_CLUSTER_NAME}"
```

## Troubleshooting

### Common Issues

#### Cluster Creation Failed

```bash
# Check cluster events
kops validate cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}

# Check CloudFormation stacks
aws cloudformation describe-stacks --region ${AWS_REGION}

# Delete and recreate if needed
kops delete cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes
```

#### Node Not Ready

```bash
# Check node status
kubectl describe node <node-name>

# Check system pods
kubectl get pods -n kube-system

# SSH to node for debugging
ssh -i ~/.ssh/cities-kops-key.pem admin@<node-ip>
```

#### Application Issues

```bash
# Check pod logs
kubectl logs -n cities deployment/cities-web -f

# Check service endpoints
kubectl get endpoints -n cities

# Test connectivity
kubectl run debug --image=busybox --rm -it --restart=Never -- sh
```

### Performance Tuning

#### Node Instance Types

```bash
# Use compute-optimized instances for CPU-intensive workloads
kops edit ig nodes --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
# Change machineType to c5.xlarge, c5.2xlarge, etc.

# Use memory-optimized instances for memory-intensive workloads
# Change machineType to r5.xlarge, r5.2xlarge, etc.
```

#### Network Optimization

```bash
# Enable enhanced networking
kops edit cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
# Add to spec:
#   cloudConfig:
#     awsEBSCSIDriver:
#       enabled: true
#   networking:
#     calico:
#       majorVersion: v3
```

## Security Hardening

### Pod Security Standards

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: cities
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
EOF
```

### Network Policies

```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: cities-web-netpol
  namespace: cities
spec:
  podSelector:
    matchLabels:
      app: cities-web
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: postgres
    ports:
    - protocol: TCP
      port: 5432
  - to: []
    ports:
    - protocol: TCP
      port: 443
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53
EOF
```

### RBAC Configuration

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cities-web-sa
  namespace: cities
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: cities
  name: cities-web-role
rules:
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: cities-web-rolebinding
  namespace: cities
subjects:
- kind: ServiceAccount
  name: cities-web-sa
  namespace: cities
roleRef:
  kind: Role
  name: cities-web-role
  apiGroup: rbac.authorization.k8s.io
EOF
```

## Cost Optimization

### Reserved Instances

```bash
# Use Spot instances for non-critical workloads
kops edit ig nodes --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE}
# Add to spec:
#   maxPrice: "0.10"  # Spot price
#   mixedInstancesPolicy:
#     instances: ["t3.medium", "t3.large"]
#     onDemandBase: 1
#     onDemandPercentageAboveBase: 25
#     spotAllocationStrategy: "price-capacity-optimized"
```

### Resource Optimization

```bash
# Use Vertical Pod Autoscaler
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.1/vertical-pod-autoscaler/deploy/vpa-v1-crd-gen.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.1/vertical-pod-autoscaler/deploy/vpa-rbac.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/vpa-release-1.1/vertical-pod-autoscaler/deploy/vpa-deployment.yaml
```

## Cleanup

### Delete Application

```bash
# Delete application resources
kubectl delete namespace cities

# Delete monitoring
helm uninstall prometheus -n monitoring
kubectl delete namespace monitoring
```

### Delete Cluster

```bash
# Delete cluster (this will remove ALL AWS resources)
kops delete cluster --name=${KOPS_CLUSTER_NAME} --state=${KOPS_STATE_STORE} --yes

# Clean up S3 bucket
aws s3 rm ${KOPS_STATE_STORE} --recursive
aws s3 rb ${KOPS_STATE_STORE}

# Clean up IAM policies (if created)
aws iam delete-policy --policy-arn arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):policy/AWSLoadBalancerControllerIAMPolicy
```

## KOPS vs EKS Comparison

| Feature | KOPS | EKS |
|---------|------|-----|
| **Control Plane** | Self-managed EC2 instances | AWS-managed |
| **Setup Complexity** | Simpler initial setup | More complex (IAM, VPC config) |
| **Maintenance** | Manual updates required | Automated updates available |
| **Cost** | Lower (no EKS service fee) | Higher (EKS service fee + nodes) |
| **Flexibility** | Full control over everything | Limited customization |
| **AWS Integration** | Manual configuration | Native AWS integration |
| **Best For** | Cost-conscious, customization | Enterprise, managed services |

## Best Practices

1. **High Availability**: Always use multiple availability zones
2. **Security**: Enable encryption, use IAM roles, implement network policies
3. **Monitoring**: Set up comprehensive monitoring and logging from day one
4. **Backup**: Regular backups of cluster state and application data
5. **Cost Management**: Use spot instances, right-size resources, monitor costs
6. **Updates**: Regular security updates and Kubernetes version upgrades
7. **Disaster Recovery**: Test backup and restore procedures regularly

## Next Steps

- Explore [EKS managed Kubernetes](./AWS.md) as an alternative
- Set up [CI/CD pipelines](./BUILD.md) for automated deployments
- Configure [advanced monitoring](../APPENDICES.md#monitoring-options)
- Implement [GitOps workflows](https://argoproj.github.io/argo-cd/)

## References

- [Official KOPS Documentation](https://kops.sigs.k8s.io/)
- [KOPS GitHub Repository](https://github.com/kubernetes/kops)
- [AWS Best Practices for Kubernetes](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Production Readiness](https://kubernetes.io/docs/setup/best-practices/)
