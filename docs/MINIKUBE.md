# Deploying Cities Application with Minikube

This guide demonstrates how to deploy the Cities Spring Boot application to a local Kubernetes cluster using Minikube for development and testing.

## What is Minikube?

Minikube is a lightweight Kubernetes implementation that creates a VM or container on your local machine and deploys a simple cluster containing only one node. Minikube implements a local Kubernetes cluster on macOS, Linux, and Windows with the primary goal of being the best tool for local Kubernetes application development.

## Prerequisites

### Required Tools

- **Docker Desktop** or compatible container runtime
- **kubectl** (Kubernetes CLI)
- **Minikube** (latest version)
- **Java 21** (for building the application)
- **Maven 3.9+** or **Gradle 8.14+**

### System Requirements

- **CPU**: 2 CPUs or more
- **Memory**: 2GB of free RAM
- **Disk**: 20GB of free disk space
- **Internet**: Internet connection for pulling images
- **Container Runtime**: Docker, Hyperkit, VirtualBox, or VMware

## Installation

### Install Minikube

#### macOS

```bash
# Using Homebrew
brew install minikube

# Using curl
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-darwin-amd64
sudo install minikube-darwin-amd64 /usr/local/bin/minikube
```

#### Linux

```bash
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
```

#### Windows

```powershell
# Using Chocolatey
choco install minikube

# Using Winget
winget install Kubernetes.minikube
```

### Verify Installation

```bash
minikube version
kubectl version --client
```

## Cluster Setup

### Start Minikube Cluster

#### Basic Start

```bash
# Start with default settings (Docker driver)
minikube start

# Start with specific configuration
minikube start --driver=docker --memory=4096 --cpus=2
```

#### Advanced Configuration

```bash
# Start with specific Kubernetes version
minikube start --kubernetes-version=v1.33.0

# Start with multiple nodes (for testing distributed scenarios)
minikube start --nodes=3

# Start with specific VM driver
minikube start --driver=virtualbox  # or hyperkit, vmware
```

### Enable Required Addons

```bash
# Enable ingress for external access
minikube addons enable ingress

# Enable metrics server for resource monitoring
minikube addons enable metrics-server

# Enable dashboard for web UI
minikube addons enable dashboard

# Enable registry for local image builds
minikube addons enable registry
```

### Verify Cluster

```bash
# Check cluster status
minikube status

# View cluster info
kubectl cluster-info

# List available addons
minikube addons list
```

## Application Deployment

### Step 1: Build Application Image

#### Option A: Build and Push to Minikube Registry

```bash
# Configure shell to use Minikube's Docker daemon
eval $(minikube docker-env)

# Build application using Spring Boot
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=cities-web:latest

# Or using Gradle
./gradlew bootBuildImage --imageName=cities-web:latest
```

#### Option B: Load External Image

```bash
# If you have a pre-built image
minikube image load cities-web:latest

# Or pull from registry
minikube image pull pivotalio/cities-web:latest
minikube image ls
```

### Step 2: Deploy PostgreSQL Database

Create PostgreSQL configuration:

```bash
# Create namespace
kubectl create namespace cities

# Deploy PostgreSQL
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-config
  namespace: cities
data:
  postgres_user: postgres
  postgres_password: postgres
  postgres_db: geo_data
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pv-claim
  namespace: cities
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 2Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: cities
spec:
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
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
                configMapKeyRef:
                  name: postgres-config
                  key: postgres_user
            - name: POSTGRES_PASSWORD
              valueFrom:
                configMapKeyRef:
                  name: postgres-config
                  key: postgres_password
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: postgres-config
                  key: postgres_db
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
spec:
  type: ClusterIP
  ports:
    - port: 5432
  selector:
    app: postgres
EOF
```

### Step 3: Deploy Cities Application

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: cities-web-config
  namespace: cities
data:
  spring_profiles_active: k8s,seeded
  java_opts: -XX:+UseG1GC -Xmx1G
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
  replicas: 2
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
        imagePullPolicy: IfNotPresent
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
              key: postgres_db
        - name: POSTGRES_USER
          valueFrom:
            configMapKeyRef:
              name: postgres-config
              key: postgres_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            configMapKeyRef:
              name: postgres-config
              key: postgres_password
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
EOF
```

### Step 4: Create Ingress for External Access

```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: cities-web-ingress
  namespace: cities
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: cities.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: cities-web-service
            port:
              number: 80
EOF
```

## Access Application

### Get Minikube IP and Configure Access

```bash
# Get Minikube IP
minikube ip

# Add to /etc/hosts (replace with actual IP)
echo "$(minikube ip) cities.local" | sudo tee -a /etc/hosts

# Access via browser
open http://cities.local

# Or use port forwarding
kubectl port-forward -n cities service/cities-web-service 8080:80
```

### Test API Endpoints

```bash
# Using kubectl proxy
kubectl proxy &
curl http://localhost:8001/api/v1/namespaces/cities/services/cities-web-service:80/proxy/cities

# Using port forwarding
kubectl port-forward -n cities service/cities-web-service 8080:80 &
curl http://localhost:8080/cities
curl http://localhost:8080/actuator/health
```

## Monitoring and Debugging

### View Logs

```bash
# Application logs
kubectl logs -n cities deployment/cities-web -f

# Database logs
kubectl logs -n cities deployment/postgres -f

# All pods in namespace
kubectl logs -n cities --all-containers=true -f
```

### Check Resources

```bash
# Pod status
kubectl get pods -n cities

# Service endpoints
kubectl get endpoints -n cities

# Ingress status
kubectl get ingress -n cities

# Resource usage
kubectl top pods -n cities
kubectl top nodes
```

### Debug Issues

```bash
# Describe problematic resources
kubectl describe pod -n cities <pod-name>
kubectl describe service -n cities cities-web-service

# Execute commands in pod
kubectl exec -it -n cities deployment/cities-web -- /bin/sh

# Check events
kubectl get events -n cities --sort-by=.metadata.creationTimestamp
```

## Development Workflow

### Local Development Cycle

```bash
# 1. Make code changes

# 2. Rebuild image with Minikube Docker
eval $(minikube docker-env)
./mvnw spring-boot:build-image -Dspring-boot.build-image.imageName=cities-web:latest

# 3. Force pod restart to pick up new image
kubectl rollout restart -n cities deployment/cities-web

# 4. Watch rollout status
kubectl rollout status -n cities deployment/cities-web

# 5. Test changes
curl http://cities.local/cities
```

### Hot Reload Development

```bash
# Enable tunnel for LoadBalancer services
minikube tunnel

# Use Skaffold for continuous development (optional)
skaffold dev
```

## Advanced Features

### Enable Dashboard

```bash
# Start dashboard
minikube dashboard

# Access dashboard in browser (opens automatically)
```

### Resource Monitoring

```bash
# Enable metrics server
minikube addons enable metrics-server

# View resource usage
kubectl top nodes
kubectl top pods -n cities
```

### Registry Integration

```bash
# Enable local registry
minikube addons enable registry

# Push to local registry
docker tag cities-web:latest localhost:5000/cities-web:latest
docker push localhost:5000/cities-web:latest
```

## Troubleshooting

### Common Issues

#### Image Pull Errors

```bash
# Check if image exists in Minikube
minikube image ls | grep cities-web

# Load image if missing
minikube image load cities-web:latest
```

#### Service Connection Issues

```bash
# Check service endpoints
kubectl get endpoints -n cities

# Test internal connectivity
kubectl run debug --image=busybox --rm -it --restart=Never -- sh
# Inside pod: nc -zv postgres-service 5432
```

#### Storage Issues

```bash
# Check PV/PVC status
kubectl get pv,pvc -n cities

# Clean up storage
kubectl delete pvc -n cities postgres-pv-claim
```

#### Memory/CPU Limits

```bash
# Increase Minikube resources
minikube stop
minikube start --memory=8192 --cpus=4

# Check node resources
kubectl describe node minikube
```

### Performance Optimization

```bash
# Use faster storage driver
minikube start --driver=docker --memory=4096 --cpus=2

# Enable feature gates for performance
minikube start --feature-gates="ServerSideApply=true"

# Use local storage class for faster I/O
kubectl patch storageclass standard -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"false"}}}'
```

## Cleanup

### Remove Application

```bash
# Delete namespace (removes all resources)
kubectl delete namespace cities

# Or delete individual resources
kubectl delete -f cities-web-deployment.yaml
kubectl delete -f postgres-deployment.yaml
```

### Stop Minikube

```bash
# Stop cluster
minikube stop

# Delete cluster
minikube delete

# Clean up Docker environment
docker system prune -f
```

## 2025 Features and Improvements

### AI Workload Support

Minikube now supports running AI workloads on MacBook's GPU using the new krunkit driver:

```bash
# Start with AI support
minikube start --driver=krunkit

# Follow AI Playground tutorial
minikube addons enable ai-playground
```

### Enhanced Configuration Management

```bash
# Use config file for addon configuration
minikube addons configure <addon-name> -f config.yaml
```

### Updated Components (2025)

- **CNI plugins**: v1.7.1
- **cri-dockerd**: v0.4.0
- **Docker**: 28.0.4
- **runc**: v1.3.0

## Best Practices

1. **Resource Management**: Always set resource limits and requests
2. **Health Checks**: Implement liveness and readiness probes
3. **Security**: Use security contexts and non-root users
4. **Monitoring**: Enable metrics and logging from the start
5. **Development**: Use image pull policies appropriately for development vs production
6. **Cleanup**: Regularly clean up unused resources to save disk space

## Next Steps

- Explore [Kubernetes local development alternatives](../APPENDICES.md#container-orchestration-tools)
- Learn about [production deployment on cloud platforms](./GOOGLE.md)
- Set up [CI/CD pipelines](./BUILD.md) for automated deployments
- Configure [monitoring and observability](../APPENDICES.md#monitoring-options)

## References

- [Official Minikube Documentation](https://minikube.sigs.k8s.io/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/topicals/spring-boot-kubernetes/)
- [Minikube GitHub Repository](https://github.com/kubernetes/minikube)
