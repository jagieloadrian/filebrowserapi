# FileBrowserAPI

## Information

 Simple application to managed files and folders by http requests. You can here create, rename, delete folders and files.


 Most free solutions and apps don't meet with my expectations.<br>
 I wanted to create an app to manage folders and files, supporting most extensions.<br>
 The application is lightweight, doesn't have UI, it's served only via http request.<br>

 I use in this project: 
 * Kotlin 2.0.21
 * SpringBoot 3.4.0
 * Gradle 8.5
 * gRPC + Protobuf
 * Kotest

### Future Features
* add database to easier managing of paths, serializing files
* add keycloak to managing security
* add configuration for kubernetes 

## Prerequisites
Java 21 or above

## Getting Started
How to run app locally
* In `application.yaml`, set your workingPath, using Intellij/other IDE and build by gradle plugin and start by default compiler</br>

   *OR* 
* Use gradle, build project, in working directory go to `build/libs` and in here set in command line
   `java -Dapplication.path="yourWorkingPath" -jar filebrowserapi-0.0.1-SNAPSHOT.jar`, where `yourWorkingPath` is a path direct to your folder,
   where app will be working.

 Path to swagger: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

   Remember in your path you need create directory with name "filebrowser", but do not provide this name in configuration


## Docker
Build image from repository root:

    docker build -f devops/Dockerfile -t filebrowserapi .

Run REST API (port 8080, profile `web`, files stored in volume `/data`):

    docker run -p 8080:8080 -v filebrowser-data:/data filebrowserapi

Run gRPC server (port 9090, profile `server`):

    docker run -p 9090:9090 -e SPRING_PROFILES_ACTIVE=server -v filebrowser-data:/data filebrowserapi

CI (GitHub Actions) builds and tests every PR and pushes image to `ghcr.io/jagieloadrian/filebrowserapi` on `main` and `v*` tags.
