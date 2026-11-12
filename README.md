# FileBrowserAPI

## Information

 Simple application to managed files and folders by http requests. You can here create, rename, delete folders and files.


 Most free solutions and apps don't meet with my expectations.<br>
 I wanted to create an app to manage folders and files, supporting most extensions.<br>
 The application is lightweight, doesn't have UI, it's served only via http request.<br>

 I use in this project: 
 * Kotlin 2.4.21
 * Spring Boot 4.1.2 (Spring gRPC server starter for the `server` profile)
 * Gradle 9.9.1
 * gRPC 1.85.1 + Protobuf 4.36.2
 * Kotest 6

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
   `java -Dapplication.path="yourWorkingPath" -jar filebrowserapi.jar`, where `yourWorkingPath` is a path direct to your folder,
   where app will be working.

 Path to swagger: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

   The app creates the `filebrowser` directory inside that path on startup.

### API rules
* `application.max-upload-size` (default `1GB`, env `APPLICATION_MAX_UPLOAD_SIZE`) limits one upload: REST answers 413,
  gRPC `RESOURCE_EXHAUSTED`.
* An upload never replaces an existing file unless it sets `overwrite` (REST `?overwrite=true`, gRPC field `overwrite`);
  otherwise 409 / `ALREADY_EXISTS`.
* Names must not be blank or contain `/` (400 / `INVALID_ARGUMENT`); no path may leave the base directory.
* REST and gRPC expose the same operations (`PATCH /api/v1/files?sourcePath=&newFileName=` renames a file).
* The `server` profile serves gRPC on `spring.grpc.server.port` (default 9090, env `SPRING_GRPC_SERVER_PORT`) with
  the standard `grpc.health.v1.Health` service, server reflection and `traceId` from `traceparent` in logs.
* gRPC `uploadFile` is client-streaming (name, path and `overwrite` in the first message); `getFile` streams 64 KiB
  chunks. REST downloads carry the file's `Content-Type` and an attachment `Content-Disposition`.


## Container image
The image is built with Jib (`eclipse-temurin:21-jre`, `linux/amd64` and `linux/arm64`, UID 10001).
Build it into the local Docker daemon:

    ./gradlew :FilebrowserBE:jibDockerBuild

Run the REST API (port 8080, profile `web`, files in volume `/data`):

    docker run -p 8080:8080 -v filebrowser-data:/data ghcr.io/jagieloadrian/filebrowserapi:local

Run the gRPC server (port 9090, profile `server`):

    docker run -p 9090:9090 -e SPRING_PROFILES_ACTIVE=server -v filebrowser-data:/data ghcr.io/jagieloadrian/filebrowserapi:local

The container logs JSON with `timestamp`, `level`, `message`, `logger` and `traceId` (from `traceparent`).
The REST profile serves `/health/live`, `/health/ready` and Prometheus metrics at `/metrics`.

## Quality gate
    ./gradlew check build

Runs ktlint, tests and JaCoCo (line coverage over 80 %, branch coverage over 75 %).

## CI
GitHub Actions runs the gate on every PR. Pushes to `main` publish `ghcr.io/jagieloadrian/filebrowserapi:main-<yyyyMMddHHmmss>-<sha7>`,
`v*` tags publish the semver tag. Dependabot keeps Gradle and Actions versions current.
