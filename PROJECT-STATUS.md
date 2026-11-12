# Project Status

Last update: 2026-11-12, branch `feature-7/path-safety`.

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

## Next steps and risks
- Push `feature-7/path-safety` (on top of `main`, which has feature-6 as PR #2) and open a PR; check CI.
- gRPC clients must move to the streaming `uploadFile`/`getFile`.
- Roadmap phase 2 (REST file rename, generator versions, gRPC health/tracing), then phase 3 (OIDC).
