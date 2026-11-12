# Project Status

Last update: 2026-11-12, branch `feature-6/standards-compliance`.

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

## Decisions
- No `.gitlab-ci.yml` ever existed; CI was written for GitHub Actions from scratch.
- Package-by-feature layout not applied: the standard targets new projects, and moving packages has no gain here.
- REST error `error` field keeps the exception name (`CannotRealizeException`) so v1 clients do not break.
- `/data` in the image is mode 777: Jib cannot set ownership, and UID 10001 must write to the volume.
- `FilebrowserUI` stub kept with toolchain 21; deleting it is the owner's call (roadmap phase 6).
- Not applied, needs input or later phases: Helm/Flux deploy (`.devops/helm`), `runLocal` task, OIDC, Testcontainers
  (no database), gRPC health/tracing.
- Earlier commits on `main` carry `Co-Authored-By` trailers; history was not rewritten.
- 2026-11-12: roles will come from the OIDC provider (Keycloak JWT), not from an app user store; the owner
  updates the standards to match.

## Next steps and risks
- P0: path traversal in `PathModifierUtils` (roadmap phase 1). Do not expose the service publicly until fixed.
- Push `feature-6/standards-compliance`, check the first CI run and the first GHCR push with Jib.
- Roadmap phases 2 to 6.
