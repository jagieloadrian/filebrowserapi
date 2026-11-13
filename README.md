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
 * Kotest 6 (`FunSpec` specs), MockK, Testcontainers
 * PostgreSQL 17 with Exposed 1.6.1 and Flyway for metadata (tags, descriptions, trash, versions, share links, content hashes); the
   file system stays the source of truth for files

## Prerequisites
Java 21 or above

## Getting Started
Run the REST API locally without OIDC:

    ./gradlew :FilebrowserBE:runLocal

It loads `.devops/application-local.yaml` (profiles `web,local`): every request is allowed as user `local`, files go to
`FilebrowserBE/build/local-data/filebrowser/local`. It needs Docker: Spring starts PostgreSQL from
`.devops/postgres-compose/compose.yaml` (project `filebrowser-postgres`, random host port) and stops it on exit. Never activate `local` in a deployment: it turns authentication off.

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
* Every write that adds bytes (upload, copy) checks two limits first and answers 507 / `RESOURCE_EXHAUSTED`:
  `application.quota` per user (default `5GB`, env `APPLICATION_QUOTA`), counting files, trash and versions, and
  `application.min-free-space` (default `2GB`, env `APPLICATION_MIN_FREE_SPACE`) that must stay free on the disk
  holding `application.path`. The `local-path` PVC does not enforce its size, so the second limit protects the node's
  disk. `GET /api/v1/quota` (gRPC `getQuota`) returns `used` and `limit` in bytes.
* An upload never replaces an existing file unless it sets `overwrite` (REST `?overwrite=true`, gRPC field `overwrite`);
  otherwise 409 / `ALREADY_EXISTS`.
* Names must not be blank or contain `/` (400 / `INVALID_ARGUMENT`); no path may leave the base directory.
* REST and gRPC expose the same operations (`PATCH /api/v1/files?sourcePath=&newFileName=` renames a file).
* REST uploads send the file as multipart part `uploadFile`; without it the answer is 400.
* `GET /api/v1/entries?path=&name=&extension=` (gRPC `listEntries`) lists everything below `path` with `directory`,
  `size`, `modified` and `contentType`. `name` matches part of the name ignoring case; `extension` keeps only files.
  The path-only trees `GET /api/v1/files` and `GET /api/v1/directories` stay unchanged.
* `POST /api/v1/entries/copy` and `/move` with `source` and `target` (gRPC `copyEntry`, `moveEntry`) copy or move a
  file or a whole directory. `target` is the full new path: an existing target is 409, a missing source or target
  directory 404, a directory into itself 400.
* `DELETE /api/v1/directories/recursively?path=` (gRPC `deleteDirectoryRecursively`) deletes a directory with its
  content. Plain `DELETE /api/v1/directories` still refuses a non-empty directory (409).
* `PUT /api/v1/entries/meta?path=` with `{"description", "tags"}` (gRPC `updateEntryMeta`) sets a description (max
  1000 characters) and up to 20 tags (letters, digits, space, `.`, `_`, `-`, max 50 characters; stored lowercase).
  Empty values remove the metadata. `GET /api/v1/entries?tag=` filters by tag; entries carry `description` and `tags`.
  Metadata follows renames, moves and copies.
* `GET /api/v1/entries/duplicates?path=` (gRPC `findDuplicates`) groups files with equal SHA-256 content. Only files
  that share a size are hashed; hashes are cached while size and modification time stay the same.
* Deleting a file or directory moves it to the trash (`<application.path>/filebrowser-trash/<sub>`) for 30 days; a
  job at 03:00 purges older items. `GET /api/v1/trash`, `POST /api/v1/trash/{id}/restore` (409 when the original
  path is taken), `DELETE /api/v1/trash/{id}`, `DELETE /api/v1/trash` (gRPC `listTrash`, `restoreTrash`,
  `purgeTrash`, `emptyTrash`). Disk space is freed only on purge.
* An upload with `overwrite` keeps the replaced content as a version; the last 10 per file are kept.
  `GET /api/v1/versions?path=` and `POST /api/v1/versions/{id}/restore` (gRPC `listVersions`, `restoreVersion`);
  restoring keeps the current content as a new version.
* `POST /api/v1/shares?path=&days=` (gRPC `createShare`) creates a public link to a file, 7 days by default, 1 to 30
  allowed. The answer holds the token once; only its SHA-256 is stored. Anyone with the token downloads the file
  from `GET /share/{token}` without logging in (REST only); unknown, expired, revoked or trashed links are 404.
  `GET /api/v1/shares`, `DELETE /api/v1/shares/{id}` (gRPC `listShares`, `revokeShare`). Links follow renames and
  moves; request logs show `/share/***`.
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

Both read PostgreSQL at `postgres.apps.svc.cluster.local:5432`, database and user `filebrowser`. The password comes
from Vault (`secrets: true`): key `SPRING_DATASOURCE_PASSWORD` in `secret/apps/filebrowser` and
`secret/apps/filebrowser-grpc`. Flyway migrates the schema on startup; `/health/ready` includes the database.

Before the first rollout:
* Create the `filebrowser` database and user, and store the password in Vault under both paths.
* Set `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` in both releases. It is the placeholder
  `https://issuer.invalid` until Authelia runs; health is green, but every API call fails.
* Register `.devops/helm` of this repo in the cluster's Flux (GitRepository and Kustomization).
* Add the deploy GitHub App (on the `main` ruleset bypass list): variable `DEPLOY_APP_CLIENT_ID`, secret
  `DEPLOY_APP_PRIVATE_KEY`. CI then writes each new `main` image tag into `release.yaml`; without them no tag is bumped.

`devops.yml` validates `.devops/helm` with kubeconform (Kubernetes and Flux schemas) and renders both releases with
homelab-infra `charts/app` (`helm template`, without the Flux `postRenderers` patches) through kubeconform. homelab-infra
is private: the chart is fetched with a token of the deploy GitHub App, limited to homelab-infra and Contents: read, so
the app must be installed on homelab-infra too (GitHub → Settings → GitHub Apps → the app → Configure → Repository
access → add `homelab-infra`); without it the step fails with `Not Found`.

## Quality gate
    ./gradlew check build

Runs ktlint, tests and JaCoCo (line coverage over 80 %, branch coverage over 75 %).

## CI
GitHub Actions runs the gate on every PR. Pushes to `main` publish `ghcr.io/jagieloadrian/filebrowserapi:main-<yyyyMMddHHmmss>-<sha7>`,
`v*` tags publish the semver tag. Dependabot keeps Gradle and Actions versions current.
