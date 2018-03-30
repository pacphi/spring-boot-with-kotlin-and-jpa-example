# Example: Spring Boot with Kotlin and JPA

This repository contains a sample Spring Boot microservice implemented with Kotlin.

Builds further upon the ideas and implementation in this [article](https://blog.codecentric.de/en/2017/06/kotlin-spring-working-jpa-data-classes/) on the codecentric blog.

Also serves as a proving ground to explore and compare the relative levels of effort to get a service and database configured and deployed to multiple container orchestration platforms:

* Docker
* Pivotal Application Service
* Kubernetes ( minikube | kops | GKE | PKS )

## Prerequisites

* Java [JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 1.8u162 or better
* [CF CLI](https://github.com/cloudfoundry/cli#downloads) 6.35.2 or better if you want to push the application to a Cloud Foundry (CF) instance
* [Postman](https://www.getpostman.com) 6.0.10 or better to simplify interaction with API endpoints
* An instance of [Postgres](https://www.postgresql.org) 10.3 or better
* Docker ([Community](https://store.docker.com/search?type=edition&offering=community) or [Enterprise](https://store.docker.com/search?type=edition&offering=enterprise) Editions for Windows | Mac | Linux) 18.03.0 or better
* Google Cloud [SDK](https://cloud.google.com/sdk/) 195.0.0 or better
* Kubernetes if you want to deploy the application to minikube, kops, GKE, or PKS
	* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/) 1.10.0 or better
	* [minikube](https://github.com/kubernetes/minikube/releases) 0.25.2 or better 
	* [kops](https://github.com/kubernetes/kops) 1.9.0-alpha.3 or better
	* [GKE](https://cloud.google.com/kubernetes-engine/docs/quickstart)
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


## How to configure a private registry

### with [Google Container Registry](https://cloud.google.com/container-registry/?hl=en_US)

Be sure to initialize your application default credentials

```
gcloud auth application-default login
```

Have a look at the following guides to get acquainted with Google Cloud Registry
 
* [Quickstart for Container Registry](https://cloud.google.com/container-registry/docs/quickstart)
* [Pushing and Pulling Images](https://cloud.google.com/container-registry/docs/pushing-and-pulling)

### on Minikube

Be sure to [authorize](https://github.com/kubernetes/minikube/issues/321#issuecomment-265222572) minikube to work with your private registry.  See [Google Container Registry Advanced Authentication](https://cloud.google.com/container-registry/docs/advanced-authentication).

The easiest way to do this is to:

```	
gcloud auth application-default print-access-token
minikube ssh
sudo docker login -u oauth2accesstoken -p "<replace_me_with_token>" https://us.gcr.io
exit
```


## How to create a Docker image

This project supports building, tagging, and deploying a Docker container image to a private registry via Maven plugin configuration

> Replace occurrences of `{HOSTNAME}` with `us.gcr.io`, `{PROJECT_ID}` with your Google Project Id, and `{VERSION}` with the tagged version of the artifact you want to push/pull from the registry.


### with Maven

```
docker login -u oauth2accesstoken -p "$(gcloud auth application-default print-access-token)" https://us.gcr.io
./mvnw install -Ddocker.image.prefix={HOSTNAME}/{PROJECT_ID}
```

### with Gradle

```
gcloud auth application-default print-access-token > build/oauth2accesstoken
cat build/oauth2accesstoken
docker login -u oauth2accesstoken -p "<replace_me_with_oauth2accesstoken>" https://us.gcr.io
./gradlew build pushDockerToGcr -PdockerImagePrefix={HOSTNAME}/{PROJECT_ID}
```

> Note: if you do not specify `docker.image.prefix` as above it will default to `pivotalio`.   

### with Docker and Google Cloud SDK

```
docker build -t cities-web .
docker tag cities-web {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
gcloud docker -- push {HOSTNAME}/{PROJECT_ID}/cities-web:{VERSION}
```



## How to start Postgres

### with Docker

Start

```
cd specs/docker
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

### with Maven

```
java -Dspring.profiles.active=postgres,seeded -jar cities-web/target/cities-web-x.x.x.jar
```

or 

```
./mvnw spring-boot:run -Dspring.profiles.active=postgres,seeded
```

### with Gradle

```
java -Dspring.profiles.active=postgres,seeded -jar cities-web/build/libs/cities-web-x.x.x-exec.jar
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

Edit `specs/k8s/cities-web.yml` and replace image prefix with your project id, then deploy the app with

```
kubectl create -f specs/k8s/cities-web.yml
```

Create an external load balancer for your app

```
kubectl expose deployment cities-web --type=LoadBalancer --port=8080
```

#### on minikube

Get the external IP address of service

```
minikube service cities-web --url
```

#### on GKE

Get the external IP address of service, then the app will be accessible at `http://<External IP Address>:8080`

```
kubectl get svc cities-web
```

> Note: It may take a few minutes for the load balancer to be created


#### Scaling

Scale your application

```
kubectl scale deployment cities-web --replicas=3
```

#### Updating

To update the image that the containers in your deployment are using

```
kubectl set image deployment/cities-web cities-web=us.gcr.io/{project_id}/cities-web:{new_version}
```

> Above assumes your image is hosted in Google Container Registry


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



