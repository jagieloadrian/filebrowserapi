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

Open problems (file references point into `FilebrowserBE/src/main/kotlin/com/anjo/filebrowserapi`):
- **Security: path traversal.** Every user-supplied name is appended to the base directory without normalisation:
  `path`, `fileName` (REST `{filename}`, gRPC `name`), `directoryName`, `newName`
  (`service/PathModifierUtils.kt:55`) and the recursive directory path. `../../etc` leaves `/filebrowser`.
  No authentication.
- **Data loss: uploads overwrite existing files silently** (`StandardCopyOption.REPLACE_EXISTING`,
  `service/FileManager.kt:47`).
- **gRPC logs every request header** (`logging/GrpcLoggingInterceptor.kt:21`). Once tokens arrive they end up in logs.
- Errors are not typed. `CannotRealizeException` means "not found", "already exists" and "not empty" at once, REST
  answers 422 for all of them, gRPC maps it to `DATA_LOSS` (`exception/GrpcGlobalControllerAdvice.kt:67`). Reading a
  missing file throws `FileNotFoundException` from `readBytes` (`service/FileManager.kt:59`) and returns 500;
  deleting a missing file returns 400 (`service/FileManager.kt:94`).
- Blank or slash-containing names are not rejected at validation.
- Renames use `File.renameTo` (`service/FileManager.kt:73`, `service/DirectoryManager.kt:70`), which only returns
  `false` and hides the reason.
- File download answers `multipart/form-data` instead of the file's MIME type and loads the whole file into memory.
- Upload size limits are implicit (Spring 1 MB, gRPC 4 MB) and differ.
- REST has no `renameFile`, which gRPC already exposes.
- gRPC code generators drift from the runtime: `protoc` 3.22 vs `protobuf-kotlin` 4.29, `protoc-gen-grpc-java` 1.54
  vs `grpc` 1.56, `protoc-gen-grpc-kotlin` 1.3 vs stub 1.4.
- Version typo `1.0-SNPASHOT`.
- `FilebrowserUI` is a "Hello World" stub.

## Phase 1: Security and correctness (P0)
1. One function in `PathModifierUtils` resolves every user-supplied segment (path, file name, directory name, new
   name): `Path.normalize()`, then a `startsWith(baseDir)` check; `..` and absolute paths are rejected. Regression
   tests for each entry point, plus a Kotest `checkAll` property test for the resolver (adds `kotest-property`).
2. Upload no longer overwrites: 409 when the file exists, unless the request sets an explicit `overwrite` flag.
3. gRPC logging interceptor logs the method name and duration only, never headers.
4. Request validation in a `validation` package: blank names and names containing `/` are rejected before any file
   system call.
5. Typed domain errors with a status, mapped once per transport: not found 404 / `NOT_FOUND`, already exists 409 /
   `ALREADY_EXISTS`, not empty 409 / `FAILED_PRECONDITION`, bad input 400 / `INVALID_ARGUMENT`. Changing 422 to
   404/409 changes the API contract: decide between `/api/v2` and treating it as a bug fix before starting.
6. Explicit upload limits: `spring.servlet.multipart.max-file-size` and gRPC `maxInboundMessageSize`, same value.
7. Correct `Content-Type` on download (`Files.probeContentType`) and `Content-Disposition`.
8. Stream large files (`Resource` in REST, streaming RPC in gRPC) instead of `readBytes`.
9. Fix the version to `1.0-SNAPSHOT`.

Done when: no request can touch a path outside the base directory, no write replaces a file without `overwrite`,
and every expected failure has its own status in REST and gRPC, each covered by a test.

## Phase 2: API parity and quality
1. REST `PATCH /api/v1/files` (rename), matching gRPC.
2. `Files.move` instead of `File.renameTo`, so rename failures map to typed errors.
3. Align the gRPC and protobuf generator versions with the runtime in `gradle/libs.versions.toml`; framework and
   Gradle upgrades come through Dependabot PRs.
4. gRPC health service and a tracing interceptor, so the `server` profile has probes and `traceId` too.

Done when: REST and gRPC expose the same operations and both profiles report health.

## Phase 3: Authentication
1. The OIDC provider (Authelia in the homelab) owns users and roles; the app keeps no user store. Its OIDC client
   must issue JWT access tokens, because Authelia issues opaque ones by default.
2. Spring Security OAuth2 Resource Server with `issuer-uri` and `audiences`; the same JWT check in gRPC through a
   `ServerInterceptor`.
3. Roles from the `groups` claim with the app prefix (`filebrowser-reader`, `filebrowser-writer`), mapped to
   authorities in one place, deny by default.
4. Tests: every protected route and RPC answers 401 without a token and 403 with a token that lacks the role.
5. Optional: per-user directories keyed by `sub` (`/data/filebrowser/<sub>`), never by e-mail.

Done when: every REST route and RPC rejects unauthenticated calls and no token appears in logs.

## Phase 4: Deployment
1. Blocked until the infra repo provides the `charts/app` values: `.devops/helm/release.yaml` HelmRelease, CI bumps
   the image tag, `devops.yml` validates with `helm template` and kubeconform.
2. Local start with one command (`runLocal`).

Done when: a push to `main` rolls the new image out to the cluster.

## Phase 5: Features
1. Copy and move files and directories, recursive delete with confirmation.
2. File metadata in responses (size, modification date, type) instead of bare paths.
3. Search by name or extension.
4. A database only when data appears that the file system cannot hold (sharing, tags, permissions).

## Phase 6: FilebrowserUI
1. Decide: build a client (Compose Multiplatform or web SPA) or delete the stub module.
2. A web client needs CORS hosts from configuration.
