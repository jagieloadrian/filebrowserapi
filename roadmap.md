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
- Phases 1 to 3, see below.

Open problems: none known; next work is deployment (phase 4).
- `FilebrowserUI` is a "Hello World" stub.

## Phase 1: Security and correctness (P0), done 2026-11-12
1. Done: one resolver in `service/PathModifierUtils.kt` normalises every user-supplied segment and rejects anything
   outside the base directory (400 / `INVALID_ARGUMENT`); regression test per entry point, Kotest `checkAll` property
   test.
2. Done: uploads answer 409 / `ALREADY_EXISTS` for an existing file unless `overwrite` is set; a directory is never
   replaced.
3. Done: the gRPC logging interceptor logs the method name only.
4. Done: `validation/ValidName.kt` rejects blank names and names containing `/`. REST runs it as Bean Validation and
   lists each field in `details`; gRPC has no validation step, so the path resolver applies the same rule.
5. Done: typed domain errors, mapped once per transport: not found 404 / `NOT_FOUND`, already exists 409 /
   `ALREADY_EXISTS`, not empty 409 / `FAILED_PRECONDITION`, bad input 400 / `INVALID_ARGUMENT`. Shipped as a bug fix in
   `/api/v1` (owner decision).
6. Done: `application.max-upload-size` (1GB) sets the multipart limits (413) and caps the bytes of one gRPC upload
   stream (`RESOURCE_EXHAUSTED`); a single gRPC message keeps the 4 MiB default, chunks are 64 KiB.
7. Done: downloads carry the file's `Content-Type` and a UTF-8 attachment `Content-Disposition`. The type comes from
   Spring's `MediaTypeFactory` instead of `Files.probeContentType`, which depends on OS MIME files missing from the
   JRE image.
8. Done: REST downloads stream a `FileSystemResource`; gRPC `getFile` is server-streaming and `uploadFile`
   client-streaming (breaking proto change, owner decision). gRPC uploads spool to `java.io.tmpdir` first, like
   Tomcat multipart, so a broken stream never leaves a partial file.
9. Done: version `1.0-SNAPSHOT`. Renames use `Files.move` (former phase 2 item).

## Phase 2: API parity and quality, done 2026-11-12
1. Done: REST `PATCH /api/v1/files?sourcePath=&newFileName=` renames a file, matching gRPC `renameFile`.
2. Done: one version per family in `gradle/libs.versions.toml` (protoc = protobuf runtime, gRPC generator = gRPC
   runtime, `grpc-bom` aligns every `io.grpc` module). Dependencies moved to the latest stable releases on Maven
   Central: Gradle 9.9.1, Kotlin 2.4.21, Spring Boot 4.1.2, gRPC 1.85.1, protobuf 4.36.2, Kotest 6.2.7.
3. Done: the `server` profile runs on `spring-boot-starter-grpc-server`: `grpc.health.v1.Health`, reflection,
   observation interceptor (`traceId` from `traceparent`, gRPC metrics), graceful shutdown (15 s). Exceptions map
   through a `GrpcExceptionHandler` bean instead of a custom interceptor.

## Phase 3: Authentication, done 2026-11-12
1. Done: the OIDC provider owns users and roles; the app has no user store. Its OIDC client must issue JWT access
   tokens (Authelia issues opaque ones by default).
2. Done: Spring Security resource server with `issuer-uri` and `audiences` (startup fails without an audience); gRPC
   uses Spring gRPC's `GrpcSecurity` with the same decoder and converter.
3. Done: `groups` mapped to `ROLE_*` in one place (`security/SecurityConfig.kt`): `filebrowser-reader` reads,
   `filebrowser-writer` reads and writes, everything else is denied; health, metrics and API docs stay public.
4. Done: tests sign real JWTs against a local OIDC discovery and JWKS endpoint; every REST route and every RPC (taken
   from the service descriptor) answers 401/`UNAUTHENTICATED` without or with a bad token (wrong `aud`, `iss`,
   expired, foreign key) and 403/`PERMISSION_DENIED` without the role; tokens never reach the logs.
5. Done: each user sees only `filebrowser/<sub>`; a `sub` that cannot name a directory gets 400.

## Phase 4: Deployment
1. Done: `.devops/helm/release.yaml` with HelmReleases for REST and gRPC, a shared PVC, CI bumps the image tag,
   `devops.yml` validates with kubeconform. Open: OIDC issuer (placeholder until Authelia runs) and the registration
   in the cluster's Flux.
2. Done: `runLocal` starts the REST API without OIDC.
3. Done: `/tmp` is a 2Gi `emptyDir`, enough for one 1GB upload.
4. Done: HTTP probes for `web`, native `grpc` probes on 9090 for `server`.

Done when: a push to `main` rolls the new image out to the cluster.

## Phase 5: Features
1. Copy and move files and directories, recursive delete with confirmation.
2. File metadata in responses (size, modification date, type) instead of bare paths.
3. Search by name or extension.
4. A database only when data appears that the file system cannot hold (sharing, tags, permissions).

## Phase 6: FilebrowserUI
1. Decide: build a client (Compose Multiplatform or web SPA) or delete the stub module.
2. A web client needs CORS hosts from configuration.
