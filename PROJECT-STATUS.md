# Project Status

Last update: 2026-11-12, branch `feature-10/deployment`.

## Completed

### 2026-11-04, `feature/5-implement-docker-image` (merged as PR #1)
- gRPC profile no longer blocks startup; `main()` forwards CLI args.
- First container image and GitHub Actions CI; `roadmap.md`.
- Tree listings sorted (`File.walk()` order is filesystem-dependent).

### 2026-11-12, `feature-6/standards-compliance`
Project aligned with the coding standards from the `standards` MCP server:
- Build: `gradle/libs.versions.toml`, root plugins `apply false`, ktlint (`ktlint_official`, `.editorconfig`),
  `allWarningsAsErrors`, JVM toolchain 21 in both modules, JaCoCo gate (line > 80 %, branch > 75 %).
- Code: no comments or TODOs, single-implementation `facade` interfaces and `CommonUtils` removed, typed
  `@ConfigurationProperties` data classes (`AppConfiguration`, `GrpcServerProperties`), no raw thread
  (`spring.main.keep-alive`), gRPC drains for up to 15 s on shutdown, web shutdown phase 20 s.
- API: one error body `{error, message, details}` for every REST error, including 404 and 500.
- Observability: `/health/live`, `/health/ready`, Prometheus `/metrics`, JSON logs with `traceId` from `traceparent`
  (enabled in the image through `LOGGING_STRUCTURED_FORMAT_CONSOLE`, plain logs locally).
- Tests: `should <action> when <condition>` names, `@WebMvcTest` slices, gRPC tests without a Spring context,
  file system tests in `@TempDir`; 82 tests, line coverage 96 %, branch 78 %.
- Bugs fixed: gRPC `showDirectoriesTree` returned files; a failed gRPC call without a cause closed with a null
  status and hung the client.
- Image: Jib (`eclipse-temurin:21-jre`, amd64 + arm64, UID 10001), Dockerfile removed. CI runs
  `./gradlew check build`, pushes `main-<yyyyMMddHHmmss>-<sha7>` or semver tags to GHCR. Dependabot added.
- Docs in English (`roadmap.md`), `.claude/settings.json` turns AI attribution off.

### 2026-11-12, `feature-7/path-safety` (on `main`): roadmap phase 1 done
- One path resolver for every user-supplied segment: traversal and the base directory itself give 400.
- gRPC no longer logs headers.
- Typed errors: `BadInput` 400, `NotFound` 404, `AlreadyExists`/`NotEmpty` 409, with matching gRPC codes. Renames use
  `Files.move`.
- Uploads: 409 unless `overwrite`; one `application.max-upload-size` (1GB) for REST (413) and gRPC
  (`RESOURCE_EXHAUSTED`).
- Names: blank or `/` rejected (Bean Validation with `details` in REST, same rule in the resolver for gRPC).
- Downloads: REST streams `FileSystemResource` with `Content-Type` and UTF-8 `Content-Disposition`; gRPC
  `getFile`/`uploadFile` are streaming RPCs (breaking proto change).
- Version `1.0-SNAPSHOT`. 126 tests, gate green.

### 2026-11-12, `feature-8/phase-2-dependencies` (from `main`): roadmap phase 2 done, dependencies current
- Latest stable from Maven Central: Gradle 9.9.1, Kotlin 2.4.21, ktlint 1.8.0, JaCoCo 0.8.16, Spring Boot 4.1.2
  (Jackson 3, springdoc 3.2.0, springmockk 5.0.1), gRPC 1.85.1, protobuf 4.36.2, grpc-kotlin 1.5.0, Kotest 6.2.7,
  coroutines 1.11.0, kotlin-logging 8.0.7. Generators share the runtime versions.
- REST `PATCH /api/v1/files` renames a file.
- gRPC runs on `spring-boot-starter-grpc-server`: health service, reflection, tracing; custom server config deleted.
- 130 tests, gate green; jar smoke-tested in both profiles.

### 2026-11-12, `feature-9/oidc-auth` (from `main`): roadmap phase 3 done
- OIDC JWT bearer tokens required for REST (`/api/v1/**`) and gRPC; `filebrowser-reader` reads, `filebrowser-writer`
  writes; 401/403 with the shared error body, `UNAUTHENTICATED`/`PERMISSION_DENIED` in gRPC.
- Tests run the production decoder path against a local OIDC discovery + JWKS server; 226 tests, gate green; jar
  smoke-tested (no audience: startup fails; health public, API 401).
- OpenAPI documents the bearer JWT scheme (Swagger "Authorize").
- Per-user directories: every path resolves inside `filebrowser/<sub>`; 237 tests, gate green.

### 2026-11-12, `feature-10/deployment` (from `main`): roadmap phase 4 mostly done
- `runLocal`: REST without OIDC (profile `local`, user `local`), `.devops/application-local.yaml`.
- HelmReleases `filebrowser` (REST) and `filebrowser-grpc` on the shared chart, PVC `filebrowser-data`, 2Gi `/tmp`,
  memory 512Mi / 384Mi (measured RSS 313 / 273 MiB). `devops.yml` validates; CI bumps the tag via the deploy App.
- 238 tests, gate green; chart output with patches rendered locally (helm + kubectl kustomize).

## Decisions
- No `.gitlab-ci.yml` ever existed; CI was written for GitHub Actions from scratch.
- Package-by-feature layout not applied: the standard targets new projects, and moving packages has no gain here.
- REST error `error` field is the exception's simple name (e.g. `NotFoundException`).
- `/data` in the image is mode 777: Jib cannot set ownership, and UID 10001 must write to the volume.
- `FilebrowserUI` stub kept with toolchain 21; deleting it is the owner's call (roadmap phase 6).
- Not applied, needs input or later phases: Helm/Flux deploy (`.devops/helm`), `runLocal` task, OIDC, Testcontainers
  (no database), gRPC health/tracing.
- Earlier commits on `main` carry `Co-Authored-By` trailers; history was not rewritten.
- 2026-11-12: users and roles come from the OIDC provider (Authelia, `groups` claim), no app user store; the owner
  updates the standards to match.
- 2026-11-12: 422 to 404/409 ships as a bug fix in `/api/v1`; gRPC switched to streaming RPCs now, breaking old clients.
- gRPC has no validation step, so `PathModifierUtils.resolveName` applies the `validation` rule (deviation from
  standard 04 for gRPC only).
- Content type from Spring `MediaTypeFactory`, not `Files.probeContentType` (needs OS MIME files absent in the image).
- gRPC uploads spool to `java.io.tmpdir` before the copy, like Tomcat multipart: no partial files.

- Spring BOM through native `platform()`: the `io.spring.dependency-management` plugin forced Kotlin 2.4 onto ktlint's
  embedded compiler (ktlint needs 2.2). gRPC uses `grpc-bom` 1.85.1, newer than Spring's managed 1.84.1.
- Tracing via `spring-boot-micrometer-tracing-opentelemetry`, not the OTel starter (its OTLP registry pushes metrics
  to localhost by default).
- gRPC port property is `spring.grpc.server.port`; the old `grpc.server.port` is gone.

- Writer implies reader. Health, `/metrics`, Swagger and gRPC health are public; gRPC reflection needs a token.
- Our `GrpcExceptionHandler` returns null for Spring Security exceptions so Spring gRPC maps them to
  `UNAUTHENTICATED`/`PERMISSION_DENIED`.
- Boot 4 cannot combine `public-key-location` with `issuer-uri`, hence the local JWKS server in tests.
- Allowed `sub`: `[A-Za-z0-9][A-Za-z0-9._@-]*`, otherwise 400; directory created on first use. No migration code:
  existing files are moved by hand (README).

- 2026-11-12: storage and gRPC probes are patched in by Flux `postRenderers` in this repo; the shared chart stays
  unchanged (owner's choice). Both releases share one RWO `local-path` PVC on the `rpi` node.
- The issuer is the placeholder `https://issuer.invalid`; API calls answer 500 until it points at Authelia.
- The `local` profile turns authentication off; it is only used by `runLocal`.

## Next steps and risks
- Authelia client: JWT access tokens (`RS256`), `aud` = client ID, `groups` in the access token.
- Existing deployments: move files from `filebrowser/` to `filebrowser/<sub>/` before upgrading.
- Phase 4 rollout: set the issuer once Authelia runs, `scripts/new-app.sh own filebrowser` in homelab-infra, deploy
  GitHub App variables. The first `main` build then replaces the `main-placeholder` image tag.
- kubeconform was not run locally (not installed); `devops.yml` runs it on the PR.
