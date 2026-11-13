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

## Prerequisites
Java 21 or above

## Getting Started
Run the REST API locally without OIDC:

    ./gradlew :FilebrowserBE:runLocal

It loads `.devops/application-local.yaml` (profiles `web,local`): every request is allowed as user `local`, files go to
`FilebrowserBE/build/local-data/filebrowser/local`. Never activate `local` in a deployment: it turns authentication off.

 Path to swagger: [http://localhost:8080/swagger-ui/index.html#/](http://localhost:8080/swagger-ui/index.html#/)

   The app creates the `filebrowser` directory inside that path on startup.

### Authentication
Every REST route under `/api/v1` and every gRPC method needs `Authorization: Bearer <JWT access token>` from the OIDC
provider. The app verifies signature (issuer JWKS), `iss`, `aud`, `exp` and `nbf`, and refuses to start without:

    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.example.com
    SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=filebrowser

Roles come from the `groups` claim: `filebrowser-reader` may read (GET, `showFilesTree`, `getFile`,
`showDirectoriesTree`), `filebrowser-writer` may read and write. Anything else is denied: 401 without or with an
invalid token, 403 without the role (gRPC: `UNAUTHENTICATED`, `PERMISSION_DENIED`). Public: `/health/**`, `/metrics`,
Swagger, gRPC `grpc.health.v1.Health`.

The OIDC client (Authelia) must issue JWT access tokens (`access_token_signed_response_alg: RS256`; Authelia issues
opaque ones by default) whose `aud` contains the client ID and which carry the `groups` claim.

Each user works in `filebrowser/<sub>` (the token's `sub` claim) and cannot reach other users' files. After upgrading
from a version without authentication, move existing files from `filebrowser/` into `filebrowser/<sub>/`.

In Swagger, click "Authorize" and paste an access token to try the API.

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

    docker run -p 8080:8080 -v filebrowser-data:/data \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.example.com \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=filebrowser ghcr.io/jagieloadrian/filebrowserapi:local

Run the gRPC server (port 9090, profile `server`):

    docker run -p 9090:9090 -e SPRING_PROFILES_ACTIVE=server -v filebrowser-data:/data \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.example.com \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=filebrowser ghcr.io/jagieloadrian/filebrowserapi:local

The container logs JSON with `timestamp`, `level`, `message`, `logger` and `traceId` (from `traceparent`).
The REST profile serves `/health/live`, `/health/ready` and Prometheus metrics at `/metrics`.

## Deployment
`.devops/helm/release.yaml` holds two HelmReleases of the cluster's shared chart `charts/app` (Flux source
`flux-system`), on node `homelab/node: rpi`:
* `filebrowser`: REST (`web`), `filebrowser.home.arpa`, HTTP probes on `/health/live` and `/health/ready`.
* `filebrowser-grpc`: gRPC (`server`), `filebrowser-grpc.home.arpa`, native Kubernetes `grpc` probes on 9090.

Both mount the PVC `filebrowser-data` (`.devops/helm/pvc.yaml`, `local-path`, kept when the release is removed) at
`/data`. The chart has no volumes or gRPC probes, so Flux `postRenderers` patch them in. `/tmp` is 2Gi because uploads
spool there before they are moved.

Before the first rollout:
* Set `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` in both releases. It is the placeholder
  `https://issuer.invalid` until Authelia runs; health is green, but every API call fails.
* Register `.devops/helm` of this repo in the cluster's Flux (GitRepository and Kustomization).
* Add the deploy GitHub App (on the `main` ruleset bypass list): variable `DEPLOY_APP_CLIENT_ID`, secret
  `DEPLOY_APP_PRIVATE_KEY`. CI then writes each new `main` image tag into `release.yaml`; without them no tag is bumped.

`devops.yml` validates `.devops/helm` with kubeconform (Kubernetes and Flux schemas). The chart lives outside this
repo, so CI does not render it with the patches.

## Quality gate
    ./gradlew check build

Runs ktlint, tests and JaCoCo (line coverage over 80 %, branch coverage over 75 %).

## CI
GitHub Actions runs the gate on every PR. Pushes to `main` publish `ghcr.io/jagieloadrian/filebrowserapi:main-<yyyyMMddHHmmss>-<sha7>`,
`v*` tags publish the semver tag. Dependabot keeps Gradle and Actions versions current.
