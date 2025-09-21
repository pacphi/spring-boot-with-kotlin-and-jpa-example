# Azure Deployment Guide

## Overview

Deploy the Spring Boot Cities application to **Microsoft Azure** using **Azure Kubernetes Service (AKS)** with the latest 2025 features and best practices.

## AKS 2025 Features

### AKS Automatic (New in 2025)

**One-click, production-ready clusters** - The biggest advancement in Azure Kubernetes for 2025:

- **Fully Managed Experience**: Azure handles node setup, networking, and integrations using best practices
- **Zero Configuration**: No upfront decisions required for production-grade clusters
- **Automatic Security**: Built-in security configurations and compliance standards
- **Cost Optimization**: Automatic resource sizing and scaling

### Key 2025 Updates

- **Kubernetes 1.29+** required (1.28 deprecated as of January 30, 2025)
- **Azure Linux Migration**: Must migrate from Azure Linux 2.0 by November 30, 2025
- **Dynamic VM Selection**: Default VM SKU dynamically selected based on available capacity
- **AI/ML Integration**: Seamless deployment of AI models with Kubernetes AI toolchain operator (KAITO)
- **GitHub Copilot Integration**: Create Kubernetes YAML files using Copilot in Azure

### Version Support Policy

- **N-1 Support**: Latest and previous minor versions fully supported
- **Security Updates**: Regular patches and updates for supported versions
- **Migration Windows**: 6-month notice for version deprecations

## Prerequisites

### Required Tools

```bash
# Azure CLI
curl -sL https://aka.ms/InstallAzureCLIDeb | sudo bash

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Azure Kubernetes CLI extension
az extension add --name aks-preview
az extension update --name aks-preview
```

### Azure Account Setup

```bash
# Login to Azure
az login

# Set subscription
az account set --subscription "Your Subscription Name"

# Verify account
az account show

# Set default location
export LOCATION=eastus
export RESOURCE_GROUP=cities-rg
export CLUSTER_NAME=cities-aks
```

## Container Registry Setup

### Azure Container Registry (ACR)

```bash
# Create resource group
az group create --name $RESOURCE_GROUP --location $LOCATION

# Create ACR (Premium for geo-replication, Basic for development)
az acr create \
  --resource-group $RESOURCE_GROUP \
  --name citiesacr \
  --sku Premium \
  --location $LOCATION

# Get ACR login server
ACR_LOGIN_SERVER=$(az acr show --name citiesacr --query loginServer --output tsv)
echo "ACR Login Server: $ACR_LOGIN_SERVER"

# Login to ACR
az acr login --name citiesacr
```

### Build and Push Container Images

**Using Spring Boot Buildpacks:**

```bash
# Build image with Maven
./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=$ACR_LOGIN_SERVER/cities-web:latest

# Build image with Gradle
./gradlew bootBuildImage \
  --imageName=$ACR_LOGIN_SERVER/cities-web:latest

# Push to ACR
docker push $ACR_LOGIN_SERVER/cities-web:latest
docker push $ACR_LOGIN_SERVER/cities-web:1.0.0-SNAPSHOT
```

**Using ACR Tasks (Cloud Build):**

```bash
# Build in Azure (no local Docker required)
az acr build \
  --registry citiesacr \
  --image cities-web:latest \
  --image cities-web:1.0.0-SNAPSHOT \
  .

# Schedule automated builds on git commits
az acr task create \
  --registry citiesacr \
  --name cities-web-task \
  --image cities-web:{{.Run.ID}} \
  --context https://github.com/yourusername/spring-boot-with-kotlin-and-jpa-example.git \
  --file Dockerfile \
  --git-access-token <github-token>
```

## AKS Cluster Creation

### Option 1: AKS Automatic (Recommended 2025)

```bash
# Create AKS Automatic cluster
az aks create \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --location $LOCATION \
  --tier automatic \
  --generate-ssh-keys \
  --attach-acr citiesacr

# Get credentials
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME

# Verify cluster
kubectl get nodes
```

### Option 2: Standard AKS with Custom Configuration

```bash
# Create AKS cluster with specific configuration
az aks create \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --location $LOCATION \
  --kubernetes-version 1.29.2 \
  --node-count 3 \
  --node-vm-size Standard_D2s_v3 \
  --enable-addons monitoring \
  --enable-managed-identity \
  --generate-ssh-keys \
  --attach-acr citiesacr \
  --enable-cluster-autoscaler \
  --min-count 1 \
  --max-count 5 \
  --network-plugin azure \
  --load-balancer-sku standard

# Enable additional features
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --enable-pod-security-policy \
  --enable-network-policy
```

### Infrastructure as Code (ARM Template)

**azuredeploy.json:**

```json
{
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "clusterName": {
      "type": "string",
      "defaultValue": "cities-aks"
    },
    "location": {
      "type": "string",
      "defaultValue": "[resourceGroup().location]"
    },
    "kubernetesVersion": {
      "type": "string",
      "defaultValue": "1.29.2"
    }
  },
  "resources": [
    {
      "type": "Microsoft.ContainerService/managedClusters",
      "apiVersion": "2023-10-01",
      "name": "[parameters('clusterName')]",
      "location": "[parameters('location')]",
      "properties": {
        "kubernetesVersion": "[parameters('kubernetesVersion')]",
        "dnsPrefix": "[parameters('clusterName')]",
        "agentPoolProfiles": [
          {
            "name": "nodepool1",
            "count": 3,
            "vmSize": "Standard_D2s_v3",
            "type": "VirtualMachineScaleSets",
            "mode": "System",
            "enableAutoScaling": true,
            "minCount": 1,
            "maxCount": 5
          }
        ],
        "servicePrincipalProfile": {
          "clientId": "msi"
        },
        "addonProfiles": {
          "omsagent": {
            "enabled": true
          }
        },
        "networkProfile": {
          "networkPlugin": "azure",
          "loadBalancerSku": "standard"
        }
      },
      "identity": {
        "type": "SystemAssigned"
      }
    }
  ]
}
```

**Deploy with:**

```bash
az deployment group create \
  --resource-group $RESOURCE_GROUP \
  --template-file azuredeploy.json \
  --parameters clusterName=$CLUSTER_NAME
```

## Database Setup

### Azure Database for PostgreSQL

```bash
# Create PostgreSQL Flexible Server (recommended for 2025)
az postgres flexible-server create \
  --resource-group $RESOURCE_GROUP \
  --name cities-postgres \
  --location $LOCATION \
  --admin-user postgres \
  --admin-password SecurePassword123! \
  --sku-name Standard_B1ms \
  --version 15 \
  --storage-size 32 \
  --public-access 0.0.0.0 \
  --high-availability Disabled

# Create database
az postgres flexible-server db create \
  --resource-group $RESOURCE_GROUP \
  --server-name cities-postgres \
  --database-name geo_data

# Get connection string
POSTGRES_HOST=$(az postgres flexible-server show \
  --resource-group $RESOURCE_GROUP \
  --name cities-postgres \
  --query fullyQualifiedDomainName \
  --output tsv)

echo "PostgreSQL Host: $POSTGRES_HOST"
```

### Configure Firewall Rules

```bash
# Allow Azure services
az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name cities-postgres \
  --rule-name AllowAzureServices \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Allow AKS subnet (get subnet CIDR from AKS)
AKS_SUBNET_CIDR=$(az aks show \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --query agentPoolProfiles[0].vnetSubnetId \
  --output tsv)

az postgres flexible-server firewall-rule create \
  --resource-group $RESOURCE_GROUP \
  --name cities-postgres \
  --rule-name AllowAKS \
  --start-ip-address 10.0.0.0 \
  --end-ip-address 10.0.255.255
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
        image: citiesacr.azurecr.io/cities-web:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "k8s"
        - name: POSTGRES_HOST
          value: "cities-postgres.postgres.database.azure.com"
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
# Apply manifests
kubectl apply -f deployment.yaml

# Verify deployment
kubectl get deployments
kubectl get pods
kubectl get services

# Get external IP
kubectl get service cities-web-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'
```

### Azure-Specific Deployment with Annotations

```yaml
apiVersion: v1
kind: Service
metadata:
  name: cities-web-service
  annotations:
    service.beta.kubernetes.io/azure-load-balancer-internal: "false"
    service.beta.kubernetes.io/azure-dns-label-name: cities-web-app
    service.beta.kubernetes.io/azure-load-balancer-resource-group: cities-rg
spec:
  selector:
    app: cities-web
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

## Security Configuration

### Azure AD Integration

```bash
# Enable Azure AD integration
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --enable-aad \
  --aad-admin-group-object-ids <admin-group-id>

# Create Azure AD group for developers
az ad group create \
  --display-name "AKS Developers" \
  --mail-nickname aksdevs

# Get group ID
DEV_GROUP_ID=$(az ad group show --group "AKS Developers" --query objectId --output tsv)

# Create role binding
kubectl create clusterrolebinding aks-developers \
  --clusterrole=edit \
  --group=$DEV_GROUP_ID
```

### Pod Security Standards (2025)

```bash
# Enable Pod Security Standards
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --enable-pod-security-policy
```

**pod-security-config.yaml:**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: cities
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
---
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
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
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
    - protocol: UDP
      port: 53   # DNS
```

### Azure Key Vault Integration

```bash
# Create Key Vault
az keyvault create \
  --resource-group $RESOURCE_GROUP \
  --name cities-keyvault \
  --location $LOCATION

# Store database password
az keyvault secret set \
  --vault-name cities-keyvault \
  --name postgres-password \
  --value "SecurePassword123!"

# Enable Key Vault integration
az aks addon enable \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --addon azure-keyvault-secrets-provider
```

**secret-provider-class.yaml:**

```yaml
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: cities-secrets
spec:
  provider: azure
  parameters:
    usePodIdentity: "false"
    useVMManagedIdentity: "true"
    userAssignedIdentityID: <managed-identity-client-id>
    keyvaultName: cities-keyvault
    objects: |
      array:
        - |
          objectName: postgres-password
          objectType: secret
          objectVersion: ""
  secretObjects:
  - secretName: db-credentials
    type: Opaque
    data:
    - objectName: postgres-password
      key: password
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
# Enable cluster autoscaler
az aks update \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --enable-cluster-autoscaler \
  --min-count 1 \
  --max-count 10

# Update scaling parameters
az aks nodepool update \
  --resource-group $RESOURCE_GROUP \
  --cluster-name $CLUSTER_NAME \
  --name nodepool1 \
  --min-count 2 \
  --max-count 15
```

### KEDA for Event-Driven Autoscaling

```bash
# Install KEDA
helm repo add kedacore https://kedacore.github.io/charts
helm repo update
helm install keda kedacore/keda --namespace keda --create-namespace

# Scale based on HTTP requests
kubectl apply -f - <<EOF
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: cities-web-scaler
spec:
  scaleTargetRef:
    name: cities-web
  minReplicaCount: 3
  maxReplicaCount: 20
  triggers:
  - type: prometheus
    metadata:
      serverAddress: http://prometheus:9090
      metricName: http_requests_per_second
      threshold: '100'
      query: sum(rate(http_requests_total[1m]))
EOF
```

## Monitoring and Observability

### Azure Monitor for Containers

```bash
# Enable Container Insights
az aks enable-addons \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --addons monitoring

# Create Log Analytics workspace
az monitor log-analytics workspace create \
  --resource-group $RESOURCE_GROUP \
  --workspace-name cities-logs \
  --location $LOCATION
```

### Application Insights Integration

```bash
# Create Application Insights
az extension add --name application-insights
az monitor app-insights component create \
  --app cities-web-insights \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP

# Get instrumentation key
APPINSIGHTS_KEY=$(az monitor app-insights component show \
  --app cities-web-insights \
  --resource-group $RESOURCE_GROUP \
  --query instrumentationKey \
  --output tsv)
```

**Update deployment with Application Insights:**

```yaml
env:
- name: APPLICATIONINSIGHTS_CONNECTION_STRING
  value: "InstrumentationKey=$APPINSIGHTS_KEY"
- name: SPRING_PROFILES_ACTIVE
  value: "k8s,appinsights"
```

### Prometheus and Grafana

```bash
# Install monitoring stack
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Install Prometheus with Azure integration
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false

# Install Grafana with Azure AD integration
helm install grafana grafana/grafana \
  --namespace monitoring \
  --set admin.password=admin123 \
  --set grafana.ini.auth.azure_ad.enabled=true \
  --set grafana.ini.auth.azure_ad.client_id=<azure-ad-app-id> \
  --set grafana.ini.auth.azure_ad.client_secret=<azure-ad-app-secret>
```

## Load Balancing and Ingress

### Azure Application Gateway Ingress Controller

```bash
# Create Application Gateway
az network application-gateway create \
  --name cities-appgw \
  --location $LOCATION \
  --resource-group $RESOURCE_GROUP \
  --sku Standard_v2 \
  --public-ip-address cities-appgw-ip \
  --vnet-name cities-vnet \
  --subnet appgw-subnet \
  --capacity 2 \
  --http-settings-cookie-based-affinity Disabled \
  --frontend-port 80 \
  --http-settings-port 80 \
  --http-settings-protocol Http

# Enable AGIC addon
az aks enable-addons \
  --resource-group $RESOURCE_GROUP \
  --name $CLUSTER_NAME \
  --addons ingress-appgw \
  --appgw-name cities-appgw \
  --appgw-subnet-cidr "10.2.0.0/16"
```

**Ingress Resource:**

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cities-web-ingress
  annotations:
    kubernetes.io/ingress.class: azure/application-gateway
    appgw.ingress.kubernetes.io/ssl-redirect: "true"
    appgw.ingress.kubernetes.io/connection-draining: "true"
    appgw.ingress.kubernetes.io/connection-draining-timeout: "30"
spec:
  tls:
  - hosts:
    - cities.example.com
    secretName: cities-tls
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

### NGINX Ingress Controller (Alternative)

```bash
# Install NGINX Ingress Controller
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --create-namespace \
  --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-load-balancer-health-probe-request-path"=/healthz
```

## CI/CD Pipeline

### Azure DevOps Pipeline

**azure-pipelines.yml:**

```yaml
trigger:
- main

variables:
  azureSubscription: 'Azure Subscription'
  resourceGroup: 'cities-rg'
  kubernetesCluster: 'cities-aks'
  containerRegistry: 'citiesacr.azurecr.io'
  imageRepository: 'cities-web'
  tag: '$(Build.BuildId)'

stages:
- stage: Build
  jobs:
  - job: Build
    pool:
      vmImage: 'ubuntu-latest'
    steps:
    - task: Docker@2
      displayName: Build and push image
      inputs:
        containerRegistry: 'citiesacr'
        repository: $(imageRepository)
        command: buildAndPush
        Dockerfile: '**/Dockerfile'
        tags: |
          $(tag)
          latest

- stage: Deploy
  jobs:
  - deployment: Deploy
    environment: 'production'
    pool:
      vmImage: 'ubuntu-latest'
    strategy:
      runOnce:
        deploy:
          steps:
          - task: KubernetesManifest@0
            displayName: Deploy to Kubernetes cluster
            inputs:
              action: deploy
              azureSubscriptionConnection: $(azureSubscription)
              azureResourceGroup: $(resourceGroup)
              kubernetesCluster: $(kubernetesCluster)
              manifests: |
                k8s/deployment.yaml
                k8s/service.yaml
              containers: |
                $(containerRegistry)/$(imageRepository):$(tag)
```

### GitHub Actions with Azure

```yaml
name: Deploy to AKS
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v5

    - name: Azure Login
      uses: azure/login@v2
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}

    - name: Build and push to ACR
      run: |
        az acr build \
          --registry citiesacr \
          --image cities-web:${{ github.sha }} \
          --image cities-web:latest \
          .

    - name: Set up kubeconfig
      run: |
        az aks get-credentials \
          --resource-group cities-rg \
          --name cities-aks

    - name: Deploy to AKS
      run: |
        kubectl set image deployment/cities-web \
          cities-web=citiesacr.azurecr.io/cities-web:${{ github.sha }}
        kubectl rollout status deployment/cities-web
```

## Cost Optimization

### Spot Instances

```bash
# Create spot node pool
az aks nodepool add \
  --resource-group $RESOURCE_GROUP \
  --cluster-name $CLUSTER_NAME \
  --name spotnodes \
  --priority Spot \
  --eviction-policy Delete \
  --spot-max-price -1 \
  --node-count 2 \
  --min-count 0 \
  --max-count 5 \
  --node-vm-size Standard_D2s_v3
```

### Reserved Instances

- Purchase Reserved VM Instances for predictable workloads
- Use Azure Hybrid Benefit for Windows workloads
- Monitor with Azure Cost Management

### Automatic Shutdown

```bash
# Schedule cluster shutdown for development
az aks stop --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME

# Start cluster
az aks start --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
```

## Backup and Disaster Recovery

### Database Backups

```bash
# Automated backups are enabled by default for Azure Database for PostgreSQL
# Configure backup retention
az postgres flexible-server parameter set \
  --resource-group $RESOURCE_GROUP \
  --server-name cities-postgres \
  --name backup_retention_days \
  --value 14

# Create on-demand backup
az postgres flexible-server backup create \
  --resource-group $RESOURCE_GROUP \
  --server-name cities-postgres \
  --backup-name cities-backup-$(date +%Y%m%d)
```

### Application Backups with Velero

```bash
# Install Velero with Azure Blob Storage
velero install \
  --provider azure \
  --plugins velero/velero-plugin-for-microsoft-azure:v1.7.0 \
  --bucket cities-backup \
  --secret-file ./credentials-velero \
  --backup-location-config resourceGroup=cities-rg,storageAccount=citiesbackupstorage,subscriptionId=<subscription-id>

# Create backup
velero backup create cities-backup --include-namespaces default
```

## Troubleshooting

### Common Issues

**Pod Image Pull Errors:**

```bash
# Check ACR integration
az aks check-acr --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --acr citiesacr

# Verify image exists
az acr repository list --name citiesacr
az acr repository show-tags --name citiesacr --repository cities-web
```

**Network Issues:**

```bash
# Check network configuration
az aks show --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --query networkProfile

# Test connectivity
kubectl run debug --image=busybox --rm -it --restart=Never -- nslookup cities-postgres.postgres.database.azure.com
```

**Performance Issues:**

```bash
# Check node resource usage
kubectl top nodes
kubectl top pods

# View metrics in Azure Monitor
az monitor log-analytics query \
  --workspace cities-logs \
  --analytics-query "ContainerInventory | limit 10"
```

### Azure-Specific Debugging

```bash
# Check AKS cluster health
az aks show --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --query provisioningState

# View AKS events
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME
kubectl get events --sort-by=.metadata.creationTimestamp

# Check Azure AD integration
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME --admin
```

This comprehensive guide covers deploying the Cities application to Azure AKS using the latest 2025 features, including AKS Automatic, enhanced security, and cost optimization strategies.
