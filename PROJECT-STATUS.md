# Project Status

Last update: 2026-11-16, branch `feature-14/kmp-ui`.

## Completed

### 2026-11-16, `feature-14/kmp-ui`: simplifications
- gRPC responses use the generated Kotlin DSL builders instead of Java builder chains.
- Paths: `formatResponsePath`, `mergePath`, `formatPath`, `extractMidPath`, `generatePathToRename` and
  `OperationType` removed; `relativePath`, `resolve` and `resolveName` cover them. The not-found message is now
  "Path <path> does not exist"; renaming with an empty source path answers 400 (`INVALID_ARGUMENT`), not 500.
- File and directory controllers return plain bodies with `@ResponseStatus`, like the other controllers.
- REST logger without no-op `super` calls; `TrashViewModel.refresh` is the loader itself.
- `Palette`, `ThemeMode` and the navigation `Destination` derive ids and labels from enum names; stored values
  unchanged.
- Build: `runLocalGrpc` and the `filebrowserapi.jar` boot jar name removed; gRPC runs locally with
  `./gradlew :FilebrowserBE:runLocal --args='--spring.profiles.active=server,local'`.

### 2026-11-16, `feature-14/kmp-ui`: preview limits, row menu, code highlighting
- Images: Skia's `Codec` reads the size from the header first. Up to 4096 × 4096 pixels (16.7 Mpx) decode as before;
  bigger JPEG and WebP decode at 1/2, 1/4 or 1/8 into an N32 bitmap that fits, turned upright by the EXIF origin;
  other formats, or images too big even at 1/8, say "Too large to preview" with the size in pixels and are never
  decoded. A header Skia cannot read says "Cannot preview this file".
- Text: split into lines (CRLF, LF, CR; no extra line for a trailing newline) and lines over 1000 characters into
  chunks, shown in a `LazyColumn` so only visible lines are laid out. Wrap off scrolls the whole list horizontally,
  as wide as the longest line measured in the monospace style.
- Escape: the dialog root takes focus and closes on Escape through `onPreviewKeyEvent`; closing twice is a no-op.
- Row menu: previewable files list "Preview" first. Not in Trash, which has no way to read a trashed file's content.
- Code highlighting with a toggle (on by default) for C-like, Python and shell, JSON, YAML/TOML/ini/conf/properties,
  XML/HTML, CSS and SQL; plain for txt, md, log, csv, tsv and unknown extensions. Colors are theme roles.
- Tests: pixel limit decisions, small PNG/JPEG, a generated 4100 px JPEG at half size (also with EXIF rotation), an
  oversized PNG that never reaches the full decoder, a cut header; line and chunk splitting; at most 100 composed
  line nodes for 1 MiB of text, wrapped or not; Escape closes and cancels the download; the row menu item; tokenizer
  cases per family; spans on and off in a .kt preview; token colors at least 4.5:1 on `surface` in every theme.

### 2026-11-16, `feature-14/kmp-ui`: list dividers, file preview
- Lists: a divider between rows (none above the first or after the last), inset 16 dp, `outlineVariant`. Wide rows
  (columns from 840 dp) draw vertical dividers between the name and each column over the full row height; stacked
  rows (compact or large text) keep only the row dividers.
- File preview: clicking a text or image file opens a dialog (full screen below 600 dp, large and centered above) with
  close, Download, Rename and Delete; text is plain monospace, selectable, with a line wrapping toggle; images fit the
  view. Formats and limits are in README "UI". Other files still open the actions menu ("Preview file" for readers).
- `FilebrowserApi.download` takes an optional byte limit: REST sends `Range: bytes=0-<n-1>` (Spring serves 206 for
  file resources), gRPC stops collecting the stream. `Entry` now carries the listing's `contentType`.
- Tests: divider counts and row-height dividers, extension and content type to preview kind, size limits, UTF-8
  replacement, loading, errors and cancel on close, Range header, gRPC early stop, offscreen .txt, .png and too-large
  previews.

### 2026-11-16, `feature-14/kmp-ui`: narrow windows, color themes, full width, UI review
- Narrow windows: the bottom bar below 600 dp already worked; the desktop window had no minimum size (now 320 × 480
  dp), and a long account name pushed the top bar title out at 320 dp. The account menu is an icon button now.
- Color themes Blue (default), Green, Purple, Orange, Rose, each light and dark, plus System, Light or Dark mode, in
  the top bar menu (settings icon when sign-in is off). Stored per user, applied before the first frame.
- Lists use the full width; from 840 dp (scaled with text size) rows show type, size and date columns. Search field,
  quota bar and long text keep a 720 dp cap.
- UI review fixes: the last file row clears the upload button, rows stack their details at large text, row taps
  have screen reader labels, empty and error titles are headings.
- Tests: offscreen Compose UI tests (`ui-test`, `runComposeUiTest` v2) in `AppTest` and `AppMenuTest`, settings
  round trip on a throwaway Preferences node, fallback cases, 4.5:1 text contrast for every theme and mode.

### 2026-11-15, `feature-14/kmp-ui`: content ETags, one HelmRelease per deployable unit
- UI files served by the backend carry a strong ETag (MD5 of the content, hashed once per file and process);
  `If-None-Match` gets 304 without a body, `no-cache` stays, `Last-Modified` stays off. `RestServerConfig` registers
  `/**` before Boot's handler, so the cache policy moved from `application-web.yaml` into it. Tests in
  `WebClientTest` (ETag on `/`, `/index.html`, `.wasm`; 304; same file same ETag, other content other ETag).
- `.devops/helm`: `filebrowser-app` (REST + UI), `filebrowser-api` (REST only), `filebrowser-ui` (nginx, proxies to
  `filebrowser-api`), `filebrowser-grpc`, each in a file of its own name; `release.yaml` and `ui.yaml` are gone.
  Default kustomization: `pvc`, `filebrowser-app`, `filebrowser-grpc`. kubeconform green; `kubectl kustomize` builds
  the default, split, API-only, with and without gRPC; CI tag `sed` checked on a copy (all four image lines).

### 2026-11-15, `feature-14/kmp-ui`: REST-only browser, web client served by the backend
- The browser client talks REST only: the browser gRPC client, its proxy and their settings are gone. BuildKonfig
  `transport` exists for the desktop target only; the Wasm build always uses `HttpFilebrowserApi`.
- Wasm `backendUrl` defaults to the page's origin; the dev server on 8081 proxies `/api` and `/share` to 8080, and
  `.devops/application-local.yaml` no longer allows CORS from 8081.
- Backend serves the web client when `spring.web.resources.static-locations` is set: GET outside `/api/**` is public,
  `/api/**` keeps its rules, `no-cache` without `Last-Modified`. Embedded-server tests (`WebClientTest`).
- Jib copies the Wasm bundle to `/app/ui` and depends on `:FilebrowserUI:wasmJsBrowserDistribution`.
- `.devops/helm/kustomization.yaml` (pvc + release) is the A/B switch; `filebrowser-ui` moved to `ui.yaml`. CI builds
  the bundle once (one Gradle run with the OIDC variables) for both images and bumps tags in every helm file.

### 2026-11-15, `feature-14/kmp-ui` (from `main`): roadmap phase 6 done
- `FilebrowserProto` module owns `filebrowser.proto`; backend and UI depend on it.
- `FilebrowserUI`: Compose Multiplatform 1.12.2, desktop (`jvm("desktop")`) and `wasmJs`, Ktor, Koin, BuildKonfig.
  `-Ptransport=http|grpc` (desktop), `-PbackendUrl=`; HTTP and native gRPC (desktop) clients.
- Backend: CORS origins from configuration, `runLocalGrpc` (profiles `server,local`, user `local`).
- nginx web image `ghcr.io/jagieloadrian/filebrowserapi-ui`, HelmRelease `filebrowser-ui`, CI builds the Wasm
  bundle and pushes the image with the backend's tag.
- OIDC sign-in in the UI (authorization code + PKCE S256, public client), token on REST and gRPC, refresh
  and one retry on 401; 403 names the needed groups. Tested against fakes only.
- Code comments removed (standard general/01); gate green.

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

### 2026-11-13, `feature-11/file-operations` (from `main`): roadmap phase 5 done
- `GET /api/v1/entries` (gRPC `listEntries`): metadata (`directory`, `size`, `modified`, `contentType`), filters
  `name` and `extension`.
- Copy and move of files and directories (`/api/v1/entries/copy|move`, gRPC `copyEntry`/`moveEntry`).
- Recursive delete on `DELETE /api/v1/directories/recursively` (gRPC `deleteDirectoryRecursively`).
- Fix: a REST upload without the `uploadFile` part answered 500, now 400.
- 302 tests, gate green; all new endpoints smoke-tested with `runLocal`.

### 2026-11-13, `feature-12/file-metadata` (from `feature-11`): metadata features
- PostgreSQL 17 + Flyway (V1 to V5), `JdbcClient`; Testcontainers in tests, Docker Compose in `runLocal`.
- Tags and descriptions (`PUT /api/v1/entries/meta`, `?tag=` filter), duplicates by SHA-256
  (`/api/v1/entries/duplicates`), trash with 30-day purge, last 10 versions on overwrite, expiring public share links.
- REST and gRPC parity for every feature; `listTrash`, `listVersions`, `listShares`, `findDuplicates` are reads.
- Fix found by a test: request logs printed the share token in the URL; now masked as `/share/***`.
- Standards follow-up (owner's review): queries use Exposed 1.6.1 DSL instead of `JdbcClient`; all tests are Kotest
  `FunSpec` specs.
- Per-user quota (5GB incl. trash and versions) and a 2GB free-space reserve on the data disk: 507 /
  `RESOURCE_EXHAUSTED`, `GET /api/v1/quota`.
- 464 tests, all Kotest, gate green; a deliberate token leak makes the log-capture test fail.

## Decisions
- No `.gitlab-ci.yml` ever existed; CI was written for GitHub Actions from scratch.
- Package-by-feature layout not applied: the standard targets new projects, and moving packages has no gain here.
- REST error `error` field is the exception's simple name (e.g. `NotFoundException`).
- `/data` in the image is mode 777: Jib cannot set ownership, and UID 10001 must write to the volume.
- `FilebrowserUI` stub kept with toolchain 21; phase 6 turned it into the Compose Multiplatform client.
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
- 2026-11-13: CI references no other repository (owner's rule until the repos share an organization): `devops.yml`
  validates only `.devops/helm`; the chart output with patches is checked by hand (helm + kubectl kustomize).

- 2026-11-13: phase 5 only adds endpoints and RPCs; nothing in `/api/v1` or the proto changed shape, so no `v2`.
  The explicit `/recursively` endpoint is the delete confirmation.
- 2026-11-13: PostgreSQL for metadata only; files stay on disk. Rows are keyed by owner and path ("/docs/a.txt") and
  follow rename, move and copy by prefix; a stale row is harmless. Trashed items use the key `trash:<id>`, which no
  user path matches, so their tags, versions and links come back on restore and vanish on purge.
- DELETE moves to the trash (breaking behaviour, `feat!`); defaults chosen without a design round (owner's request):
  30 days trash, 10 versions, links 7 days (max 30), files only.
- Share tokens: 32 random bytes, base64url, stored as SHA-256 hex; the public download is REST only.
- Exposed without its Spring starter: one `Database.connect(dataSource)` bean, `transaction(db)` per repository call.
  Services are blocking (Spring MVC), so no `Dispatchers.IO` wrapper (`Db.tx`) as in the coroutine standard.
- Repository specs run on Testcontainers + Flyway without a Spring context and clean the tables before each test.
- Spring specs: class-body `FunSpec()` with `@ApplyExtension(SpringExtension::class)`; springmockk `@MockkBean` only
  works on fields, so no constructor injection there.
- Data tests use `withTests`, not `withData`: in Kotest 6.2.7 `withData` cases are containers, so `beforeEach` and
  Spring's per-test hooks skip them. Blank values get named cases (Kotest rejects blank test names).
- Log assertions use `TestObjects.captureOutput` (swaps `System.out`/`err`); Kotest has no `OutputCaptureExtension`.
- Quota usage is computed by walking the user's files, trash and versions on each write (fine for a homelab; keep a
  running total in the DB if it gets slow). Concurrent writes can overshoot the quota by one upload.

- 2026-11-15: UI targets desktop and Wasm only; transport and backend URL are build-time settings (BuildKonfig).
- 2026-11-15: the browser uses REST only (owner's decision); gRPC stays a desktop option.
- The web client is same-origin in both deployments (backend or nginx proxy), so the cluster needs no CORS.
- Deployments are picked only by listing release files in `kustomization.yaml`; no env edits. Default
  `filebrowser-app` (one pod, no nginx hop); split `filebrowser-api` + `filebrowser-ui`; `filebrowser-api` alone has
  no web client; `filebrowser-grpc` is optional. `filebrowser-app` and `filebrowser-ui` both own
  `filebrowser.home.arpa` (alternatives), so they are never listed together and share one OIDC redirect URI.
- The releases were renamed before the first rollout, so Vault paths, Service names and Istio targets follow the
  new names; nothing deployed had to move.
- The backend serves the UI from `spring.web.resources.static-locations`. Every GET outside `/api/**` is public, so
  unknown paths get 404 instead of 401. Jib pins file times to the epoch, so `Last-Modified` is off; content ETags
  with `no-cache` make each load a revalidation, and the bundle (about 14 MB) downloads only after a deploy. MD5
  (Spring `DigestUtils`, streamed) is enough for change detection; the hash cache lives as long as the process.
- OIDC: empty `oidcIssuer` means no sign-in (local runs). Tokens stay in memory; the PKCE verifier and state sit
  in `sessionStorage` only for the browser redirect. SHA-256 from Ktor, randomness from `Uuid.random()`, no new
  dependency. Any failed refresh signs the user out.
- 2026-11-16: color schemes come from MaterialKolor's `material-color-utilities` (tonal spot, standard contrast),
  mapped to `ColorScheme` in one function. `material-kolor` itself would raise material3 to an alpha; hand-written
  schemes would be about 480 hex values. Settings sit behind `SettingsStorage` (`expect fun`, like `authBrowser()`):
  Java Preferences on the desktop, `localStorage` in the browser, where read or write errors are printed and fall
  back to the defaults.
- 2026-11-16, not changed in the UI review: the red "Empty trash" and dialog buttons (a warning on an action that
  cannot be undone, not decoration), adjacent 48 dp top bar targets (Material 3 default), focus shown as the
  Material state layer instead of an outline, the 14 sp trash note, and the 48 dp state icon outside `Spacing`.

- 2026-11-16, preview: bytes come through the existing download (token, REST and desktop gRPC), no new endpoint.
  Text over 1 MiB is read partially instead of hitting a 5 MiB cap, since both transports can stop early. Images
  decode with Compose's `ByteArray.decodeToImageBitmap()` (in `ui-graphics`, no new dependency) on
  `Dispatchers.Default`, scaled with `ContentScale.Inside` so small images are not blown up.
- Left out of the preview on purpose: SVG, PDF, audio, video, Markdown or HTML rendering.
- 2026-11-16, preview image limit: `org.jetbrains.skia.Codec` is reachable from `commonMain` (both targets use
  Skiko), so no `expect`/`actual` and no header parser of our own. 4096 × 4096 pixels is about 64 MB as N32. Only
  JPEG and WebP downscale natively in Skia; PNG, GIF and BMP would need a full decode first, so they are refused.
  The scaled path applies the EXIF origin itself, because the full decode does and phone photos rely on it.
- 2026-11-16, text preview: lines are cut at 1000 characters (never inside a surrogate pair) so minified files do
  not make one huge item; a cut line continues as the next row. `SelectionContainer` wraps the list, but Compose
  selects only composed items: a selection that is scrolled out of view is lost, and copying a range longer than
  the screen copies only the visible part. With wrap off the list is as wide as the longest line by characters,
  measured in the monospace style; a shorter line with wider characters (CJK, emoji, tabs) can be cut off.
- 2026-11-16, Escape: handled on the focused dialog root instead of relying on the platform back mapping, which
  offscreen tests cannot press; the dialog's own back handling stays on as a fallback when focus is elsewhere.
- 2026-11-16, highlighting: an own line tokenizer instead of `dev.snipme:highlights` (JVM and Wasm, small), which
  has no JSON, YAML, TOML, XML, CSS or SQL, works on whole texts and whose 1.2.0 was under two weeks old. Lines are
  tokenized when composed and cached per item; block comments, multi-line strings and open XML tags carry over to
  the next line through a state scan that only runs up to the furthest line shown. Single-line strings and line
  comments carry only across the chunks of one cut line. Heuristics: keys are words before `:`/`=` (CSS selectors
  such as `a:hover` count as keys), an apostrophe in an unquoted YAML value starts a string to the end of the line,
  Rust lifetimes read as strings, TOML/ini section headers stay plain. Keywords and keys use `primary`, numbers and
  attributes `secondary`, strings `tertiary`, comments italic `onSurfaceVariant`.

## Next steps and risks
- Preview, not checked in a real window or browser (offscreen desktop tests only, no Wasm test target): Escape and
  focus in the dialog, scrolling speed of 1 MiB of text in the browser, selection across lines, horizontal scroll
  with wrap off, the dialog sizes, scaled JPEG and WebP decoding on Wasm, `Range` through nginx. Jumping far into a
  big code file runs a one-time state scan of the lines above on the UI thread.
- Not checked in a real browser or desktop window: the theme menu, `localStorage` persistence and its fallback, the
  320 dp window minimum (offscreen Compose tests only; no Wasm test target).
- Database for the rollout: create `filebrowser` in the cluster's PostgreSQL, password (`SPRING_DATASOURCE_PASSWORD`)
  in Vault under `secret/apps/<release>` for each listed backend release: `filebrowser-app`, `filebrowser-api`,
  `filebrowser-grpc`.
- Trash and versions count against the quota until purged; users free space by emptying the trash.
- Authelia client: register it as in README "UI sign-in" (public, PKCE, `RS256`, `aud` = client ID, `groups` in the
  access token), then set the backend issuer and the CI variables `OIDC_ISSUER` and `OIDC_CLIENT_ID`.
- UI sign-in is untested against Authelia: check the loopback redirect with a random port, CORS on the discovery
  and token endpoints, and that the name shows in the account menu. Sign out does not end the Authelia session.
- UI rollout: register `https://filebrowser.home.arpa/` (and the desktop loopback URI) in Authelia, then open it.
  Istio AuthorizationPolicies: the gateway to `filebrowser-app`, `filebrowser-api`, `filebrowser-ui` and
  `filebrowser-grpc`; `filebrowser-ui` to `filebrowser-api:8080` for the split. nginx restarts until the upstream
  Service exists. The UI image tag is a placeholder until the first `main` build.
- Flux applies `.devops/helm` through `kustomization.yaml` now: a new manifest there must be listed to be deployed.
- Not run here: the webpack dev server proxy (only its generated config was checked) and the backend image with a
  real `/app/ui` against PostgreSQL.
- Existing deployments: move files from `filebrowser/` to `filebrowser/<sub>/` before upgrading.
- Phase 4 rollout: set the issuer once Authelia runs, register `.devops/helm` in the cluster's Flux, deploy GitHub
  App variables. The first `main` build then replaces the `main-placeholder` image tag.
