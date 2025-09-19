# Example: Spring Boot with Kotlin and JPA

This repository contains a sample Spring Boot microservice implemented with Kotlin.

Builds further upon the ideas and implementation in this [article](https://blog.codecentric.de/en/2017/06/kotlin-spring-working-jpa-data-classes/) on the codecentric blog.

Also serves as a proving ground to explore and compare the relative levels of effort to get a service and database configured and deployed to multiple container orchestration platforms:

* Docker
* Pivotal Application Service
* Kubernetes ( kind | GKE | Azure )

## Prerequisites

* [jq](https://stedolan.github.io/jq/) 1.7 or better
* Java [JDK](https://www.oracle.com/java/technologies/downloads/#java21) 21.0.8 or better
* [CF CLI](https://github.com/cloudfoundry/cli#downloads) 8.12.0 or better if you want to push the application to a Cloud Foundry (CF) instance
* [httpie](https://httpie.io/cli) 3.2.4 or better to simplify interaction with API endpoints
* An instance of [Postgres](https://www.postgresql.org) 10.3 or better
* Docker ([Community](https://store.docker.com/search?type=edition&offering=community) or [Enterprise](https://store.docker.com/search?type=edition&offering=enterprise) Editions for Windows | Mac | Linux) 18.03.0 or better
* Google Cloud [SDK](https://cloud.google.com/sdk/) 195.0.0 or better
* Azure [CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) 2.0 or better
* Kubernetes if you want to deploy the application to minikube, GKE, or Azure
  * [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) 1.10.0 or better
  * [minikube](https://github.com/kubernetes/minikube/releases) 1.35.0 or better
  * [kops](https://github.com/kubernetes/kops) 1.32.0 or better
  * [juju](https://documentation.ubuntu.com/juju/3.6/tutorial/) 3.6 or better
  * [GKE](https://cloud.google.com/kubernetes-engine/docs/quickstart)
  * [Azure](https://docs.microsoft.com/en-us/azure/virtual-machines/linux/overview#getting-started-with-linux-on-azure)

## Clone

```bash
git clone https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example.git
```

## How to Build

### with Maven

```
cd spring-boot-with-kotlin-and-jpa-example
./mvnw package
```

### with Gradle

```bash
cd spring-boot-with-kotlin-and-jpa-example
rm -Rf cities-web/build
mkdir -p cities-web/build
touch cities-web/build/oauth2accesstoken
./gradlew build
```

An artifact is produced with a `version` which is managed in both the `pom.xml` and `gradle.properties` files in the root of this project.  Where you see `x.x.x` below, replace with the a real version (e.g., `1.0.0-SNAPSHOT`).

## How to set up a Kubernetes cluster

### on Minikube

Setup

```bash
minikube start
minikube dashboard
```

Teardown

```bash
minikube stop
minikube delete
```

### on GKE using kops

> Distilled from [Getting Started with kops on GCE](https://github.com/kubernetes/kops/blob/master/docs/tutorial/gce.md)

Create environment variables

```bash
export BUCKET_SUFFIX=<replace_with_bucket_suffix>
export PROJECT=`gcloud config get-value project`
export KOPS_FEATURE_FLAGS=AlphaAllowGCE
export ZONE=<replace_with_zone>
export KOPS_STATE_STORE=gs://kubernetes-clusters-${BUCKET_SUFFIX}/
```

Create storage bucket

```bash
gsutil mb gs://kubernetes-clusters-${BUCKET_SUFFIX}
```

Create cluster configuration

```bash
kops create cluster simple.k8s.local --zones ${ZONE} --project=${PROJECT} --ssh-public-key=<replace_with_ssh_public_key>
```

Check configuration

```bash
kops get cluster
kops get cluster simple.k8s.local -oyaml
kops get instancegroup --name simple.k8s.local
```

Create cluster

```bash
kops update cluster simple.k8s.local --yes
kops validate cluster
```

Use

```bash
kubectl get nodes --show-labels
```

Teardown

```bash
kops delete cluster simple.k8s.local --yes
```

### on Azure using juju

> Distilled from [Using the Microsoft Azure public cloud](https://jujucharms.com/docs/devel/help-azure) and [Setting up Kubernetes with Juju](https://kubernetes.io/docs/getting-started-guides/ubuntu/installation/)

Initialize juju

```bash
juju update-clouds
```

Authenticate and list subscription(s)

```bash
az login
az account list
```

Create service principal (if one does not already exist)

```bash
export SUB_ID=`az account list | jq '.[0].id' -r`
export APP_PASSWORD=b00tMe
az ad sp create-for-rbac --name "my.k8s.io" --password $APP_PASSWORD --role Owner
```

> Notes: (1) We're setting a subscription id variable to the first id returned from the acccount list. If you have more than one account; be sure choose one with elevated privileges. (2) The `APP_PASSWORD` value should be replaced. (3) The `--name` option from the create service principal command above is arbitrary. (4) Capture `appId` and `tenantId` from the output. Export two additional environment variables based on these values.

```bash
export APP_ID=...
export TENANT_ID=...
```

Login with service principal

```bash
az login --service-principal -u $APP_ID -p $APP_PASSWORD --tenant $TENANT_ID
```

List environment variables with ID

```bash
env | grep ID
```

Add service principal credentials to juju

```bash
juju add-credential azure
```

> When prompted enter an arbitrary credential name, select `service-principal-secret` as the `Auth type`, and employ `$APP_ID`, `$SUB_ID` and `$APP_PASSWORD` environment variables values respectively for `application-id`, `subscription-id` and `application-password`.

Create controller and deploy cluster

```bash
juju bootstrap azure/{REGION} {CLUSTER_NAME}
juju deploy canonical-kubernetes
```

> Replace {REGION} and {CLUSTER_NAME} above with a valid [region](https://azure.microsoft.com/en-us/global-infrastructure/regions/) within Azure and an arbitrary name for your cluster. Creating the controller will take 5-10 minutes. Deploying the cluster will take another 10-15 minutes.

Monitor deployment

```bash
juju status
```

> When all states are green and Idle, the cluster is ready to be used

Create the kubectl config directory

```bash
mkdir -p ~/.kube
```

Copy the kubeconfig file to the default location

```bash
juju scp kubernetes-master/0:/home/ubuntu/config ~/.kube/config
```

Query the cluster

```bash
kubectl cluster-info
```

Teardown

```bash
export CLUSTER_CONTROLLER=`juju switch`
juju destroy-controller $CLUSTER_CONTROLLER --destroy-all-models
```

* [Delete the service principal](https://docs.microsoft.com/en-us/cli/azure/ad/sp?view=azure-cli-latest#az-ad-sp-delete) you created.

## How to configure a private registry

### with Google Container Registry

Have a look at the following guides to get acquainted with Google Cloud Registry

* [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart)
* [Pushing and Pulling Images](https://cloud.google.com/container-registry/docs/pushing-and-pulling)

Be sure to initialize your application default credentials

```bash
gcloud auth application-default login
```

### with Azure Container Registry

> Distilled from the [Quickstart](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli)

```bash
export AZ_GROUP=containers-dc64336a2
export AZ_REGISTRY=cr0dc64336a2
az group create --name $AZ_GROUP --location westus2
az acr create --resource-group $AZ_GROUP --name $AZ_REGISTRY --sku Basic
```

> Feel free to modify the `AZ_GROUP` and `AZ_REGISTRY` environment variable values above.

## Preparing Minikube to work with a private registry

You must [authorize](https://github.com/kubernetes/minikube/issues/321#issuecomment-265222572) minikube to work with your private registry.

### with Google Container Registry

See [Google Container Registry Advanced Authentication](https://cloud.google.com/container-registry/docs/advanced-authentication).

The easiest way to do this is to:

```bash
gcloud auth application-default print-access-token
minikube ssh
sudo docker login -u oauth2accesstoken -p "<replace_me_with_token>" https://us.gcr.io
exit
```

## How to create a Docker image

This project uses Spring Boot's built-in buildpacks support to create Docker container images. The buildpacks approach uses Cloud Native Buildpacks (CNB) to automatically detect and configure the runtime environment without requiring a Dockerfile.

### Benefits of using Spring Boot Buildpacks

- **No Dockerfile required**: Buildpacks automatically detect and configure the appropriate runtime environment
- **Optimized layering**: Images are built with optimal layer structure for efficient caching and updates
- **Security**: Regularly updated base images with security patches
- **Performance**: Supports advanced JVM optimizations like Class Data Sharing (CDS) and Ahead-of-Time (AOT) compilation
- **Consistency**: Standardized images across development, testing, and production environments

### Requirements

- Docker daemon must be running and accessible
- Java 21 runtime (configured via `BP_JVM_VERSION` environment variable)
- Maven 3.9+ or Gradle 8.14+ with Spring Boot plugin

Consult the following table and replace occurences of bracketed variables appearing below

| Hostname | Project Id | Version | Cloud |
|:------------|:--------------|:-----------|:-------|
| {HOSTNAME} | {PROJECT_ID} | {VERSION} | |
| us.gcr.io | fe-cphillipson | latest | Google  |
| cr0dc64336a2.azurecr.io | fe-cphillipson | latest | Azure |

Start here

```bash
cd cities-web
```

### Building Docker images locally

#### with Maven

```bash
./mvnw spring-boot:build-image -Ddocker.image.prefix={HOSTNAME}/{PROJECT_ID}
```

> Note: if you do not specify `docker.image.prefix` as above it will default to `pivotalio`.

The Maven plugin will create an image with both the version tag and `latest` tag automatically.

#### with Gradle

```bash
./gradlew bootBuildImage -PdockerImagePrefix={HOSTNAME}/{PROJECT_ID}
```

> Note: if you do not specify `dockerImagePrefix` as above it will default to `pivotalio`.

The Gradle task will create an image with both the version tag and `latest` tag automatically.

### Pushing to Google Container Registry

#### with Maven

```bash
docker login -u oauth2accesstoken -p "$(gcloud auth application-default print-access-token)" https://us.gcr.io
./mvnw spring-boot:build-image -Ddocker.image.prefix={HOSTNAME}/{PROJECT_ID}
docker push {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
docker push {HOSTNAME}/{PROJECT_ID}/cities-web:latest
```

#### with Gradle

```bash
gcloud auth application-default print-access-token > build/oauth2accesstoken
./gradlew bootBuildImage -PdockerImagePrefix={HOSTNAME}/{PROJECT_ID} -PpublishImage=true
```

> The Gradle configuration supports automatic publishing when the `publishImage` property is set to `true`.


#### with Docker

```bash
docker build -t cities-web .
docker tag cities-web {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
gcloud docker -- push {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
```

### Pushing to Azure Container Registry

#### with Maven

```bash
az acr login --name $AZ_REGISTRY
export AZ_REGISTRY_HOSTNAME=`az acr list --resource-group $AZ_GROUP --query "[].{acrLoginServer:loginServer}" --output json | jq '.[0].acrLoginServer'`
./mvnw spring-boot:build-image -Ddocker.image.prefix=$AZ_REGISTRY_HOSTNAME/fe-cphillipson
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:{VERSION}
```

#### with Gradle

```bash
az acr login --name $AZ_REGISTRY
export AZ_REGISTRY_HOSTNAME=`az acr list --resource-group $AZ_GROUP --query "[].{acrLoginServer:loginServer}" --output json | jq '.[0].acrLoginServer'`
./gradlew bootBuildImage -PdockerImagePrefix=$AZ_REGISTRY_HOSTNAME/fe-cphillipson
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:{VERSION}
```

#### with Docker

```bash
az acr login --name $AZ_REGISTRY
export AZ_REGISTRY_HOSTNAME=`az acr list --resource-group $AZ_GROUP --query "[].{acrLoginServer:loginServer}" --output json | jq '.[0].acrLoginServer'`
docker tag pivotalio/cities-web $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
```


## How to start Postgres

### with Docker

Start

```bash
docker run -it --rm -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=geo_data -d postgres:10.3
```

Stop and cleanup

```bash
docker ps -a
docker stop {container_id}
docker rm {container_id}
```

> `{container_id} is the identifer for the running/stopped postgres instance


### with Kubernetes

```bash
cd cities-web
```

Deploy postgres with a persistent volume claim

```bash
kubectl create -f specs/k8s/postgres.yml
```

Create a config map with the hostname of Postgres

```bash
kubectl create configmap hostname-config --from-literal=postgres_host=$(kubectl get svc postgres -o jsonpath="{.spec.clusterIP}")
```

Delete the hostname config map

```bash
kubectl delete cm hostname-config
```

Delete Postgres

```bash
kubectl delete -f specs/k8s/postgres.yml
```


### with Pivotal Cloud Foundry

Assuming a Postgres option is available from the Marketplace, like [ElephantSQL](https://www.elephantsql.com) on [Pivotal Web Services](https://run.pivotal.io)

```bash
cf create-service elephantsql panda my-pgdb
```

> Note: this plan will cost you $19/month, so if you're merely evaluating, please remember to shutdown the service with `cf delete-service my-pgdb`.


## How to Run

### with Maven

```bash
cd cities-web
java -Dspring.profiles.active=postgres,seeded -jar target/cities-web-x.x.x.jar
```

or

```bash
./mvnw -p cities-web spring-boot:run -Dspring.profiles.active=postgres,seeded
```

### with Gradle

```bash
cd cities-web
java -Dspring.profiles.active=postgres,seeded -jar build/libs/cities-web-x.x.x-exec.jar
```

or

```bash
./gradlew cities-web:bootRun -Dspring.profiles.active=postgres,seeded
```

> Press `Ctrl+C` to exit.


### with Docker Compose

This setup uses ephemeral storage.  When the `db` container is stopped all data will be lost!

> Optional: edit the `docker-compose.yml` file to swap the `webapp` image.

to startup (from root directory)

```bash
cd specs/docker
docker-compose up -d
```

to shutdown

```bash
docker-compose down
```

### with Kubernetes

Edit `specs/k8s/cities-web.yml` and replace `image` with appropriate private registry image, then deploy the app with

```bash
kubectl create -f specs/k8s/cities-web.yml
```

> Note: pulling an image from Azure Container service requires an [image pull secret](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-auth-aks#access-with-kubernetes-secret).  So the yml file mentioned above will need to modified to accommodate this constraint.

#### on Azure or GKE

Create an external load balancer for your app

```bash
kubectl expose deployment cities-web --type=LoadBalancer --port=8080
```

> Note: It may take a few minutes for the load balancer to be created

```bash
kubectl get svc cities-web
```

> Get the external IP address of service, then the app will be accessible at `http://<External IP Address>:8080`

#### Logs

View logs for troubleshooting purposes

```bash
kubectl logs deployment/cities-web
```

#### Scaling

Scale your application

```bash
kubectl scale deployment cities-web --replicas=3
```

#### Updating

To update the image that the containers in your deployment are using

```bash
kubectl set image deployment/cities-web cities-web={HOSTNAME}/{PROJECT_ID}/cities-web:{VERSON}
```

#### Cleaning up

Delete the Spring Boot app deployment

```bash
kubectl delete -f specs/k8s/cities-web.yml
```

Delete the service for the app

```bash
kubectl delete svc cities-web
```

### with Pivotal Cloud Foundry

#### How to target a foundation

```bash
cf login -a {CF_INSTANCE_URL}
```

E.g., to deploy to Pivotal Web Services

```bash
cf login -a https://api.run.pivotal.io
```

> when prompted, supply your account credentials.

then to target a new organization and space, execute

```bash
cf target -o {ORG} -s {SPACE}
```

> where `{ORG}` is an organization name and `{SPACE}` is an environment; e.g., `cf target -o catepillar -s test`

#### How to deploy application instance(s)

##### (cf push)

with manifest.yml

```bash
cf push -p {PATH/TO/ARTIFACT}
```

> Note: `{PATH/TO/ARTIFACT}` is the path to an executable JAR. If Maven was used to build project, specify `target/cities-web-x.x.x.jar`. If Gradle was used, specify `build/libs/cities-web-x.x.x-exec.jar`.

##### (Gradle CF plugin)

```bash
./gradlew cf-push -Pcf.ccHost={CF_INSTANCE_URL} -Pcf.ccUser={CF_USER} -Pcf.ccPassword={CF_PASSWORD} -Pcf.org={ORG} -Pcf.space={SPACE}
```

or if you want to orchestrate a blue-green deployment, try

```bash
./gradlew cf-push-blue-green -Pcf.ccHost={CF_INSTANCE_URL} -Pcf.domain={CF_DOMAIN} -Pcf.ccUser={CF_USER} -Pcf.ccPassword={CF_PASSWORD} -Pcf.org={ORG} -Pcf.space={SPACE}
```

> Consult [pivotalservices/ya-cf-app-gradle-plugin](https://github.com/pivotalservices/ya-cf-app-gradle-plugin#using-the-plugin) for detailed configuration options.

#### How to delete application instance(s)

##### (cf delete)

```bash
cf delete cities-web
```

## Working with sample data

Edit the .sql file in `cities-web/src/main/resources/db/sql/test` and add `INSERT` statements, like:

```bash
INSERT INTO city (id, name, description, latitude, longitude, updated_at, created_at) VALUES ('SFO', 'San Francisco', '', 37.781555, -122.393990, '2018-03-25 15:00:00', '2018-03-25 15:00:00');
```

## Working with API

Application endpoints

```bash
GET /cities
GET /cities/{id}
PUT /cities/{id}
POST /cities/{id}
DELETE /cities/{id}
```

and of course there are the [actuator endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html) provided by Spring Boot
