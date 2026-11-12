# Roadmap: FileBrowserAPI

Updated 2026-11-12. Kotlin 2.0 + Spring Boot 3.4, module `FilebrowserBE` with two run modes (profile `web` = REST on
8080, profile `server` = gRPC on 9090) and an empty `FilebrowserUI` module.

## Current state

Done:
- Shared service layer (`FileManager`, `DirectoryManager`) used by REST and gRPC.
- Gate `./gradlew check build`: ktlint (`ktlint_official`), warnings as errors, tests, JaCoCo (line > 80 %,
  branch > 75 %).
- One REST error body `{error, message, details}`, probes `/health/live` and `/health/ready`, Prometheus `/metrics`,
  JSON logs with `traceId` from `traceparent`.
- Jib image (`eclipse-temurin:21-jre`, amd64 + arm64, UID 10001), GitHub Actions CI, GHCR, Dependabot.

Open problems:
- **Security: path traversal.** `path` is appended to the base directory without normalisation
  (`File("${defaultPath}/$path")`), so `../../etc` leaves `/filebrowser`. No authentication.
- File download answers `multipart/form-data` instead of the file's MIME type and loads the whole file into memory.
- Upload size limits are implicit (Spring 1 MB, gRPC 4 MB) and differ.
- REST has no `renameFile`, which gRPC already exposes.
- Dependency versions drift: `grpc-netty` 1.56, `protoc-gen-grpc-java` 1.54, `protoc` 3.22 vs runtime
  `protobuf-kotlin` 4.29, `protoc-gen-grpc-kotlin` 1.3 vs stub 1.4.
- Version typo `1.0-SNPASHOT`.
- `FilebrowserUI` is a "Hello World" stub.

## Phase 1: Security and correctness (P0)
1. Normalise paths in one place (`PathModifierUtils`): `Path.normalize()` plus a `startsWith(baseDir)` check;
   reject `..` and absolute paths. Regression test for traversal.
2. Explicit upload limits: `spring.servlet.multipart.max-file-size` and gRPC `maxInboundMessageSize`, same value.
3. Correct `Content-Type` on download (`Files.probeContentType`) and `Content-Disposition`.
4. Stream large files (`Resource` in REST, streaming RPC in gRPC) instead of `readBytes`.
5. Fix the version to `1.0-SNAPSHOT`.

## Phase 2: API parity and quality
1. REST `PATCH /api/v1/files` (rename), matching gRPC.
2. Align gRPC and protobuf versions (one `grpc` version, protoc 4.x); Spring Boot 3.5.x, Gradle 8.14.
3. gRPC health service and tracing interceptor, so the `server` profile has probes and `traceId` too.

## Phase 3: Authentication
1. Spring Security + OAuth2 Resource Server (JWT), Keycloak as identity provider.
2. The same JWT check in gRPC through a `ServerInterceptor`.
3. Every protected route gets a test that expects 401 without credentials.
4. Optional: per-user directories (`/data/filebrowser/<user>`).

## Phase 4: Deployment
1. `.devops/helm/release.yaml` HelmRelease of the shared `charts/app` chart, CI bumps the image tag, `devops.yml`
   validates with `helm template` and kubeconform.
2. Local start with one command (`runLocal`).

## Phase 5: Features
1. Copy and move files and directories, recursive delete with confirmation.
2. File metadata in responses (size, modification date, type) instead of bare paths.
3. Search by name or extension.
4. A database only when data appears that the file system cannot hold (sharing, tags, permissions).

## Phase 6: FilebrowserUI
1. Decide: build a client (Compose Multiplatform or web SPA) or delete the stub module.
