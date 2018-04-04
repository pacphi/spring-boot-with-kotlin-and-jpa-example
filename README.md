# Example: Spring Boot with Kotlin and JPA

This repository contains a sample Spring Boot microservice implemented with Kotlin.

Builds further upon the ideas and implementation in this [article](https://blog.codecentric.de/en/2017/06/kotlin-spring-working-jpa-data-classes/) on the codecentric blog.

Also serves as a proving ground to explore and compare the relative levels of effort to get a service and database configured and deployed to multiple container orchestration platforms:

* Docker
* Pivotal Application Service
* Kubernetes ( minikube | GKE | Azure | Pivotal Container Service (PKS) )

## Prerequisites

* [jq](https://stedolan.github.io/jq/) 1.5 or better
* Java [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 1.8u162 or better
* [CF CLI](https://github.com/cloudfoundry/cli#downloads) 6.35.2 or better if you want to push the application to a Cloud Foundry (CF) instance
* [Postman](https://www.getpostman.com) 6.0.10 or better to simplify interaction with API endpoints
* An instance of [Postgres](https://www.postgresql.org) 10.3 or better
* Docker ([Community](https://store.docker.com/search?type=edition&offering=community) or [Enterprise](https://store.docker.com/search?type=edition&offering=enterprise) Editions for Windows | Mac | Linux) 18.03.0 or better
* Google Cloud [SDK](https://cloud.google.com/sdk/) 195.0.0 or better
* Azure [CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli?view=azure-cli-latest) 2.0 or better
* Kubernetes if you want to deploy the application to minikube, GKE, Azure or PKS
	* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) 1.10.0 or better
	* [minikube](https://github.com/kubernetes/minikube/releases) 0.25.2 or better 
	* [kops](https://github.com/kubernetes/kops) 1.8.1 or better
	* [juju](https://kubernetes.io/docs/getting-started-guides/ubuntu/installation/) 2.3 or better
	* [GKE](https://cloud.google.com/kubernetes-engine/docs/quickstart)
	* [Azure](https://docs.microsoft.com/en-us/azure/virtual-machines/linux/overview#getting-started-with-linux-on-azure)
	* [PKS](https://docs.pivotal.io/runtimes/pks/1-0/using-prerequisites.html) 1.0.0


## Clone

```
git clone https://github.com/pacphi/spring-boot-with-kotlin-and-jpa-example.git
```


## How to Build

### with Maven

```
./mvnw package
```

### with Gradle

```
./gradlew build
```

An artifact is produced with a `version` which is managed in both the `pom.xml` and `gradle.properties` files in the root of this project.  Where you see `x.x.x` below, replace with the a real version (e.g., `1.0.0-SNAPSHOT`).


## How to set up a Kubernetes cluster

### on Minikube

Setup

```
minikube start
minikube dashboard
```

Teardown

```
minikube stop
minikube delete
```

### on GKE using kops

> Distilled from [Getting Started with kops on GCE](https://github.com/kubernetes/kops/blob/master/docs/tutorial/gce.md)

Create environment variables

```
export BUCKET_SUFFIX=<replace_with_bucket_suffix>
export PROJECT=`gcloud config get-value project`
export KOPS_FEATURE_FLAGS=AlphaAllowGCE
export ZONE=<replace_with_zone>
export KOPS_STATE_STORE=gs://kubernetes-clusters-${BUCKET_SUFFIX}/
```

Create storage bucket

```
gsutil mb gs://kubernetes-clusters-${BUCKET_SUFFIX}
```

Create cluster configuration

```
kops create cluster simple.k8s.local --zones ${ZONE} --project=${PROJECT} --ssh-public-key=<replace_with_ssh_public_key>
```

Check configuration

```
kops get cluster
kops get cluster simple.k8s.local -oyaml
kops get instancegroup --name simple.k8s.local
```

Create cluster

```
kops update cluster simple.k8s.local --yes
kops validate cluster
```

Use 

```
kubectl get nodes --show-labels
```

Teardown

```
kops delete cluster simple.k8s.local --yes
```

### on Azure using juju

> Distilled from [Using the Micorsoft Azure public cloud](https://jujucharms.com/docs/devel/help-azure) and [Setting up Kubernetes with Juju](https://kubernetes.io/docs/getting-started-guides/ubuntu/installation/)

Initialize juju

```
juju update-clouds
```

Authenticate and list subscription(s)

```
az login
az account list
```

Create service principal (if one does not already exist)

```
export SUB_ID=`az account list | jq '.[0].id' -r`
export APP_PASSWORD=b00tMe
az ad sp create-for-rbac --name "my.k8s.io" --password $APP_PASSWORD --role Owner
```

> Notes: (1) We're setting a subscription id variable to the first id returned from the acccount list. If you have more than one account, be sure choose one with elevated privileges. (2) The `APP_PASSWORD` value should be replaced. (3) The `--name` option from the create service principal command above is arbitrary. (4) Capture `appId` and `tenantId` from the output. Export two additional environment variables based on these values.

```
export APP_ID=...
export TENANT_ID=...
```

Login with service principal

```
az login --service-principal -u $APP_ID -p $APP_PASSWORD --tenant $TENANT_ID
```

List environment variables with ID

```
env | grep ID
```

Add service principal credentials to juju

```
juju add-credential azure
```

> When prompted enter an arbitrary credential name, select `service-principal-secret` as the `Auth type`, and employ `$APP_ID`, `$SUB_ID` and `$APP_PASSWORD` environment variables values respectively for `application-id`, `subscription-id` and `application-password`.

Create controller and deploy cluster

```
juju bootstrap azure/{REGION} {CLUSTER_NAME}
juju deploy canonical-kubernetes
```

> Replace {REGION} and {CLUSTER_NAME} above with a valid [region](https://azure.microsoft.com/en-us/global-infrastructure/regions/) within Azure and an arbitrary name for your cluster. Creating the controller will take 5-10 minutes. Deploying the cluster will take another 10-15 minutes.

Monitor deployment

```
juju status
```

> When all states are green and Idle, the cluster is ready to be used

Create the kubectl config directory

```
mkdir -p ~/.kube
```

Copy the kubeconfig file to the default location

```
juju scp kubernetes-master/0:/home/ubuntu/config ~/.kube/config
```

Query the cluster

```
kubectl cluster-info
```

Teardown

```
export CLUSTER_CONTROLLER=`juju switch`
juju destroy-controller $CLUSTER_CONTROLLER --destroy-all-models
```

* [Delete the service principal](https://docs.microsoft.com/en-us/cli/azure/ad/sp?view=azure-cli-latest#az-ad-sp-delete) you created.


### on PKS

> Assuming you've installed PKS on [vSphere](https://docs.pivotal.io/runtimes/pks/1-0/vsphere.html) or [GCP](https://docs.pivotal.io/runtimes/pks/1-0/gcp.html), consult [Using PKS](https://docs.pivotal.io/runtimes/pks/1-0/using.html).  Also see [PKS CLI](https://docs.pivotal.io/runtimes/pks/1-0/cli/index.html).


## How to configure a private registry

### with Google Container Registry

Have a look at the following guides to get acquainted with Google Cloud Registry
 
* [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart)
* [Pushing and Pulling Images](https://cloud.google.com/container-registry/docs/pushing-and-pulling)

Be sure to initialize your application default credentials

```
gcloud auth application-default login
```

### with Azure Container Registry

> Distilled from the [Quickstart](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-azure-cli)

```
export AZ_GROUP=containers-dc64336a2
export AZ_REGISTRY=cr0dc64336a2
az group create --name $AZ_GROUP --location westus2
az acr create --resource-group $AZ_GROUP --name $AZ_REGISTRY --sku Basic
```

> Feel free to modify the `AZ_GROUP` and `AZ_REGISTRY` environment variable values above.

## Preparing Minikube to work with privte registry

You must [authorize](https://github.com/kubernetes/minikube/issues/321#issuecomment-265222572) minikube to work with your private registry.  

### with Google Container Registry

See [Google Container Registry Advanced Authentication](https://cloud.google.com/container-registry/docs/advanced-authentication).

The easiest way to do this is to:

```	
gcloud auth application-default print-access-token
minikube ssh
sudo docker login -u oauth2accesstoken -p "<replace_me_with_token>" https://us.gcr.io
exit
```


## How to create a Docker image

This project supports building, tagging, and deploying a Docker container image to a private registry via Maven plugin configuration or standard Docker commandline.

Consult the following table and replace occurences of bracketed variables appearing below

| Hostname | Project Id | Version | Cloud |
|:------------|:--------------|:-----------|:-------|
| {HOSTNAME} | {PROJECT_ID} | {VERSION} | |
| us.gcr.io | fe-cphillipson | latest | Google  |
| cr0dc64336a2.azurecr.io | fe-cphillipson | latest | Azure |

Start here

```
cd cities-web
```

### push to Google Container Registry

#### with Maven

```
docker login -u oauth2accesstoken -p "$(gcloud auth application-default print-access-token)" https://us.gcr.io
./mvnw install -Ddocker.image.prefix={HOSTNAME}/{PROJECT_ID}
```

> Note: if you do not specify `docker.image.prefix` as above it will default to `pivotalio`.

#### with Gradle

```
gcloud auth application-default print-access-token > build/oauth2accesstoken
cat build/oauth2accesstoken
docker login -u oauth2accesstoken -p "<replace_me_with_token>" https://us.gcr.io
./gradlew build pushDockerToGcr -PdockerImagePrefix={HOSTNAME}/{PROJECT_ID}
```

> Note: if you do not specify `dockerImagePrefix` as above it will default to `pivotalio`.
   

#### with Docker

```
docker build -t cities-web .
docker tag cities-web {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
gcloud docker -- push {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
```

### push to Azure Container Registry

#### with Docker

```
az acr login --name $AZ_REGISTRY
export AZ_REGISTRY_HOSTNAME=`az acr list --resource-group myResourceGroup --query "[].{acrLoginServer:loginServer}" --output json | jq '.[0].acrLoginServer'`
docker tag pivotalio/cities-web $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
docker push $AZ_REGISTRY_HOSTNAME/fe-cphillipson/cities-web:latest
```


## How to start Postgres

### with Docker

Start

```
docker run -it --rm -p 5432:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=geo_data -d postgres:10.3
```
Stop and cleanup

```
docker ps -a
docker stop {container_id}
docker rm {container_id}
```

> `{container_id} is the identifer for the running/stopped postgres instance


### with Kubernetes

```
cd cities-web
```

Deploy postgres with a persistent volume claim

```
kubectl create -f specs/k8s/postgres.yml
```

Create a config map with the hostname of Postgres

```
kubectl create configmap hostname-config --from-literal=postgres_host=$(kubectl get svc postgres -o jsonpath="{.spec.clusterIP}")
```

Delete the hostname config map

```
kubectl delete cm hostname-config
```

Delete Postgres

```
kubectl delete -f specs/k8s/postgres.yml
```


### with Pivotal Cloud Foundry

Assuming a Postgres option is available from the Marketplace, like [ElephantSQL](https://www.elephantsql.com) on [Pivotal Web Services](https://run.pivotal.io)

```
cf create-service elephantsql panda my-pgdb
```

> Note: this plan will cost you $19/month, so if you're merely evaluating, please remember to shutdown the service with `cf delete-service my-pgdb`.


## How to Run 

```
cd cities-web
```

### with Maven

```
java -Dspring.profiles.active=postgres,seeded -jar target/cities-web-x.x.x.jar
```

or 

```
./mvnw spring-boot:run -Dspring.profiles.active=postgres,seeded
```

### with Gradle

```
java -Dspring.profiles.active=postgres,seeded -jar build/libs/cities-web-x.x.x-exec.jar
```

or

```
./gradlew bootRun -Dspring.profiles.active=postgres,seeded
```

> Press `Ctrl+C` to exit.


### with Docker Compose

This setup uses ephemeral storage.  When the `db` contaner stopped all data will be lost!

> Optional: edit the `docker-compose.yml` file to swap the `webapp` image.

to startup

```
cd specs/docker
docker-compose up -d
```

to shutdown

```
docker-compose down
```


### with Kubernetes

Edit `specs/k8s/cities-web.yml` and replace `image` with appropriate private registry image, then deploy the app with

```
kubectl create -f specs/k8s/cities-web.yml
```

> Note: pulling an image from Azure Container service requires an [image pull secret](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-auth-aks#access-with-kubernetes-secret).  So the yml file mentioned above will need to modified to accommodate this constraint.

#### on Minikube

Get the external IP address of service

```
minikube service cities-web --url
```

#### on Azure, GKE or PKS

Create an external load balancer for your app

```
kubectl expose deployment cities-web --type=LoadBalancer --port=8080
```

> Note: It may take a few minutes for the load balancer to be created

```
kubectl get svc cities-web
```

> Get the external IP address of service, then the app will be accessible at `http://<External IP Address>:8080`


#### Logs

View logs for troubleshooting purposes

```
kubectl logs deployment/cities-web
```

#### Scaling

Scale your application

```
kubectl scale deployment cities-web --replicas=3
```

#### Updating

To update the image that the containers in your deployment are using

```
kubectl set image deployment/cities-web cities-web={HOSTNAME}/{PROJECT_ID}/cities-web:{VERSON}
```

#### Cleaning up

Delete the Spring Boot app deployment

```
kubectl delete -f specs/k8s/cities-web.yml
```

Delete the service for the app

```
kubectl delete svc cities-web
```

### with Pivotal Cloud Foundry

#### How to target a foundation

```
cf login -a {CF_INSTANCE_URL}
```

E.g., to deploy to Pivotal Web Services

```
cf login -a https://api.run.pivotal.io
```

> when prompted, supply your account credentials.

then to target a new organization and space, execute

```
cf target -o {ORG} -s {SPACE}
```

> where `{ORG}` is an organization name and `{SPACE}` is an environment; e.g., `cf target -o catepillar -s test`


#### How to deploy application instance(s)

##### (cf push)

with manifest.yml

```
cf push -p {PATH/TO/ARTIFACT}
```

> Note: `{PATH/TO/ARTIFACT}` is the path to an executable JAR. If Maven was used to build project, specify `target/cities-web-x.x.x.jar`. If Gradle was used, specify `build/libs/cities-web-x.x.x-exec.jar`.

##### (Gradle CF plugin)

```
./gradlew cf-push -Pcf.ccHost={CF_INSTANCE_URL} -Pcf.ccUser={CF_USER} -Pcf.ccPassword={CF_PASSWORD} -Pcf.org={ORG} -Pcf.space={SPACE}
```

or if you want to orchestrate a blue-green deployment, try

```
./gradlew cf-push-blue-green -Pcf.ccHost={CF_INSTANCE_URL} -Pcf.domain={CF_DOMAIN} -Pcf.ccUser={CF_USER} -Pcf.ccPassword={CF_PASSWORD} -Pcf.org={ORG} -Pcf.space={SPACE}
```

> Consult [pivotalservices/ya-cf-app-gradle-plugin](https://github.com/pivotalservices/ya-cf-app-gradle-plugin#using-the-plugin) for detailed configuration options.

#### How to delete application instance(s)

##### (cf delete)

```
cf delete cities-web
```


## Working with sample data

Edit the .sql file in `cities-web/src/main/resources/db/sql/test` and add `INSERT` statements, like:

```
INSERT INTO city (id, name, description, latitude, longitude, updated_at, created_at) VALUES ('SFO', 'San Francisco', '', 37.781555, -122.393990, '2018-03-25 15:00:00', '2018-03-25 15:00:00');
```


## Working with API

Application endpoints

```
GET /cities
GET /cities/{id}
PUT /cities/{id}
POST /cities/{id}
DELETE /cities/{id}
```

and of course there are the [actuator endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html) provided by Spring Boot



