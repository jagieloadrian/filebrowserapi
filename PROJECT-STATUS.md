# Project Status

Last update: 2026-11-04 (branch `feature/5-implement-docker-image`)

## Completed
- Fixed gRPC profile hanging startup/tests (`awaitTermination` moved off main thread).
- `main()` forwards CLI args to Spring.
- Docker: multi-stage `devops/Dockerfile` (JDK 21 build, JRE 21 alpine runtime, non-root, `/data` volume).
  Verified locally: REST, Swagger, upload, gRPC port, graceful shutdown.
- CI: `.github/workflows/ci.yml` — build+test on PR/main, image pushed to `ghcr.io/jagieloadrian/filebrowserapi` on `main` and `v*` tags.
- `roadmap.md` with analysis and phased plan.
- Tree listings (`showFilesTree`, `showDirectoriesTree`) sorted: `File.walk()` order is filesystem-dependent, broke tests on GitHub runner.

## Decisions
- No `.gitlab-ci.yml` existed in repo or history; GitHub Actions workflow written from scratch.
- Config via env (`SPRING_PROFILES_ACTIVE`, `APPLICATION_PATH`) instead of separate `devops/application.yml`; removed `copy_jar.sh`.
- Build stage uses glibc image: Maven `protoc` binary does not run on alpine/musl.
- Boot jar named `filebrowserapi.jar`.

## Next steps
- P0: fix path traversal in `PathModifierUtils` (see `roadmap.md`, Etap 1).
- Push branch, confirm first GitHub Actions run green, open PR to `main`.
