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
- Phase 1 (security and correctness), see below.

Open problems:
- No authentication.
- REST has no `renameFile`, which gRPC already exposes.
- gRPC code generators drift from the runtime: `protoc` 3.22 vs `protobuf-kotlin` 4.29, `protoc-gen-grpc-java` 1.54
  vs `grpc` 1.56, `protoc-gen-grpc-kotlin` 1.3 vs stub 1.4.
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

## Phase 2: API parity and quality
1. REST `PATCH /api/v1/files` (rename), matching gRPC.
2. Align the gRPC and protobuf generator versions with the runtime in `gradle/libs.versions.toml`; framework and
   Gradle upgrades come through Dependabot PRs.
3. gRPC health service and a tracing interceptor, so the `server` profile has probes and `traceId` too.

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
3. A read-only root filesystem needs a writable `/tmp` volume: multipart and gRPC uploads spool there.

Done when: a push to `main` rolls the new image out to the cluster.

## Phase 5: Features
1. Copy and move files and directories, recursive delete with confirmation.
2. File metadata in responses (size, modification date, type) instead of bare paths.
3. Search by name or extension.
4. A database only when data appears that the file system cannot hold (sharing, tags, permissions).

## Phase 6: FilebrowserUI
1. Decide: build a client (Compose Multiplatform or web SPA) or delete the stub module.
2. A web client needs CORS hosts from configuration.
