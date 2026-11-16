# FileBrowserAPI

## Information

 Simple application to managed files and folders by http requests. You can here create, rename, delete folders and files.


 Most free solutions and apps don't meet with my expectations.<br>
 I wanted to create an app to manage folders and files, supporting most extensions.<br>
 The application is lightweight and serves REST and gRPC; `FilebrowserUI` is a client for the desktop and the browser.<br>

 I use in this project: 
 * Kotlin 2.4.21
 * Spring Boot 4.1.2 (Spring gRPC server starter for the `server` profile)
 * Gradle 9.9.1
 * gRPC 1.85.1 + Protobuf 4.36.2
 * Kotest 6 (`FunSpec` specs), MockK, Testcontainers
 * Compose Multiplatform 1.12.2, Ktor 3.6.1 and Koin 4.2.2 for the UI
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


## UI
`FilebrowserUI` is a Compose Multiplatform client for the desktop (JVM) and the browser (Kotlin/Wasm). The browser
always talks REST. Gradle properties set its configuration at build time:
* `-Ptransport=http|grpc` (default `http`): REST or gRPC for the desktop app only. The Wasm build has no gRPC code.
* `-PbackendUrl=`: the server. The desktop app defaults to `http://localhost:8080` (REST) or `http://localhost:9090`
  (gRPC). The browser defaults to an empty value, the page's own origin.
* `-PoidcIssuer=`, `-PoidcClientId=` (default `filebrowser`), `-PoidcScopes=`: see [UI sign-in](#ui-sign-in). An empty
  issuer (the default) turns sign-in off.

The top bar menu (settings, or the account menu when signed in) picks a color theme (Blue, Green, Purple, Orange, Rose)
and System, Light or Dark mode. The choice is kept per user under the keys `filebrowser.theme` and `filebrowser.mode`:
on the desktop in Java Preferences (user node `com/anjo/filebrowser`), in the browser in `localStorage`. Unknown or
unreadable values fall back to Blue and System. The desktop window is at least 320 × 480 dp.

Clicking a file opens a preview when the UI can show it; other files open the actions menu, where previewable files
also list "Preview" first. The extension decides, and the content type from the listing helps when the extension is
unknown:
* Text, shown as monospace text (never rendered as HTML or Markdown), decoded as UTF-8 with replacement characters,
  line wrapping on by default: `txt md log csv tsv json yaml yml xml toml ini conf properties html css sql sh kt kts
  java js ts py go rs c h cpp`, or a `text/*`, JSON, XML or JavaScript content type. Files over 1 MiB show their
  first 1 MiB with a note: REST asks for `Range: bytes=0-1048575`, gRPC stops reading the stream after 1 MiB. Only
  the visible lines are laid out; lines over 1000 characters continue on the next row. Code is highlighted (keywords,
  strings, comments, numbers, keys, tags and attributes, in theme colors) with a toggle, on by default, for every
  extension above except `txt md log csv tsv`.
* Images, scaled down to fit and never up, aspect ratio kept: `png jpg jpeg gif webp bmp` (GIF shows its first frame),
  up to 20 MiB. Bigger files say "Too large to preview" and are not downloaded. Images up to 4096 × 4096 pixels
  decode in full; bigger JPEG and WebP decode at 1/2, 1/4 or 1/8 size to fit that limit (EXIF orientation kept), and
  other formats, or images still too big at 1/8, say "Too large to preview" without being decoded.
* No preview for SVG, PDF, audio and video. The preview offers Download, Rename and Delete; a file that does not decode
  says "Cannot preview this file" and offers Download. Escape or the close button closes the preview and cancels its
  download.

Local runs, without Authelia (the backend runs with the `local` profile):

    ./gradlew :FilebrowserBE:runLocal                         # REST on 8080
    ./gradlew :FilebrowserUI:wasmJsBrowserDevelopmentRun      # browser on http://localhost:8081

The dev server on 8081 proxies `/api` and `/share` to `http://localhost:8080`, so the page stays on one origin and needs
no CORS. To run it like `filebrowser-app` (the backend serves the built UI on 8080):

    ./gradlew :FilebrowserUI:wasmJsBrowserDistribution
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS="file:$PWD/FilebrowserUI/build/dist/wasmJs/productionExecutable/" \
        ./gradlew :FilebrowserBE:runLocal

For the desktop app over gRPC on 9090, run the backend with the `server` profile:

    ./gradlew :FilebrowserBE:runLocal --args='--spring.profiles.active=server,local'

The desktop app is packaged with `./gradlew [-Ptransport=grpc] :FilebrowserUI:createDistributable`
(`FilebrowserUI/build/compose/binaries/main/app`). A browser client on another origin needs that origin in
`application.cors.allowed-origins`.

### Web image
The Wasm bundle (`FilebrowserUI/build/dist/wasmJs/productionExecutable`) ships in two images, both on one origin with
the API:
* The backend image (default deployment): Jib copies the bundle to `/app/ui`; every `jib*` task builds it first. The
  REST profile serves it only when `SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/ui/` is set: `index.html` at `/`,
  `.wasm` as `application/wasm`, every file with `Cache-Control: no-cache`, a strong ETag (MD5 of the file, hashed
  once per process) and without `Last-Modified` (Jib sets file times to the epoch). The browser revalidates on each
  load and gets 304 until a deploy changes a file. GET requests outside `/api/**` need no token; unknown paths get 404.
* `ghcr.io/jagieloadrian/filebrowserapi-ui`: `nginxinc/nginx-unprivileged` on port 8080 serves the bundle and proxies
  `/api/` and `/share/` to `BACKEND_URL`. Build it locally:

      ./gradlew :FilebrowserUI:wasmJsBrowserDistribution
      docker build -t filebrowserapi-ui:local FilebrowserUI

### UI sign-in
With `-PoidcIssuer` set, the UI signs in with the authorization code flow and PKCE (S256) as a public client and
sends the access token to REST and gRPC. Endpoints come from the issuer's discovery document. The browser redirects in
the same window back to the page's origin (`/`). `filebrowser-app` and `filebrowser-ui` both serve the UI at
`https://filebrowser.home.arpa/`, so that is the one web redirect URI. The desktop app opens the system browser and
receives the code on `http://127.0.0.1:<random port>/callback` (RFC 8252). Tokens live only in memory. The app
refreshes them once after a 401 and shows the sign-in screen again when the refresh fails. 403 means the account lacks
the `filebrowser-reader` or `filebrowser-writer` group. CI passes the repository variables `OIDC_ISSUER` and
`OIDC_CLIENT_ID` to the Wasm build that both images carry.

Authelia client (untested until Authelia runs):

```yaml
identity_providers:
  oidc:
    claims_policies:
      filebrowser:
        access_token: ['groups']
        id_token: ['preferred_username', 'name']
    cors:
      endpoints: ['authorization', 'token', 'revocation', 'introspection', 'userinfo']
      allowed_origins_from_client_redirect_uris: true
    clients:
      - client_id: 'filebrowser'
        client_name: 'Filebrowser'
        public: true
        token_endpoint_auth_method: 'none'
        require_pkce: true
        pkce_challenge_method: 'S256'
        redirect_uris:
          - 'https://filebrowser.home.arpa/'
          - 'http://127.0.0.1/callback'
        scopes: ['openid', 'profile', 'groups', 'offline_access']
        grant_types: ['authorization_code', 'refresh_token']
        response_types: ['code']
        access_token_signed_response_alg: 'RS256'
        audience: ['filebrowser']
        requested_audience_mode: 'implicit'
        claims_policy: 'filebrowser'
```

The backend's `audiences` must contain the same client ID. Authelia has no `end_session_endpoint`, so "Sign out" only
drops the tokens in the app.

## Container image
The image is built with Jib (`eclipse-temurin:21-jre`, `linux/amd64` and `linux/arm64`, UID 10001).
Build it into the local Docker daemon:

    ./gradlew :FilebrowserBE:jibDockerBuild

Run the REST API (port 8080, profile `web`, files in volume `/data`):

    docker run -p 8080:8080 -v filebrowser-data:/data \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.example.com \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=filebrowser ghcr.io/jagieloadrian/filebrowserapi:local

Add `-e SPRING_WEB_RESOURCES_STATIC_LOCATIONS=file:/app/ui/` to serve the web client at `/`.

Run the gRPC server (port 9090, profile `server`):

    docker run -p 9090:9090 -e SPRING_PROFILES_ACTIVE=server -v filebrowser-data:/data \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=https://auth.example.com \
        -e SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_AUDIENCES=filebrowser ghcr.io/jagieloadrian/filebrowserapi:local

The container logs JSON with `timestamp`, `level`, `message`, `logger` and `traceId` (from `traceparent`).
The REST profile serves `/health/live`, `/health/ready` and Prometheus metrics at `/metrics`.

## Deployment
`.devops/helm` holds one HelmRelease per file, named like the file, on the cluster's shared chart `charts/app` (Flux
source `flux-system`) and node `homelab/node: rpi`:

| Release | Serves | Image | Host | Vault path |
|---------|--------|-------|------|------------|
| `filebrowser-app` | REST (`web`) and the web client from `/app/ui` | backend | `filebrowser.home.arpa` | `secret/apps/filebrowser-app` |
| `filebrowser-api` | REST (`web`) only | backend | `filebrowser-api.home.arpa` | `secret/apps/filebrowser-api` |
| `filebrowser-ui` | web client (nginx), proxies `/api/` and `/share/` to `filebrowser-api:8080` | `filebrowserapi-ui` | `filebrowser.home.arpa` | none |
| `filebrowser-grpc` | gRPC (`server`), native Kubernetes `grpc` probes on 9090 | backend | `filebrowser-grpc.home.arpa` | `secret/apps/filebrowser-grpc` |

`resources` in `.devops/helm/kustomization.yaml` picks the deployment; no env value changes:

| Combination | `resources` besides `pvc.yaml` |
|-------------|-------------------------------|
| Together (default) | `filebrowser-app.yaml`, `filebrowser-grpc.yaml` |
| Split | `filebrowser-api.yaml`, `filebrowser-ui.yaml`, optional `filebrowser-grpc.yaml` |
| No web client | `filebrowser-api.yaml`, optional `filebrowser-grpc.yaml` |

Never list `filebrowser-app.yaml` and `filebrowser-ui.yaml` together: both claim `filebrowser.home.arpa`.
`filebrowser-ui` needs `filebrowser-api`. REST releases have HTTP probes on `/health/live` and `/health/ready`.

The backend releases mount the PVC `filebrowser-data` (`.devops/helm/pvc.yaml`, `local-path`, kept when a release is
removed) at `/data`. The chart has no volumes or gRPC probes, so Flux `postRenderers` patch them in. `/tmp` is 2Gi
because uploads spool there before they are moved.

They read PostgreSQL at `postgres.apps.svc.cluster.local:5432`, database and user `filebrowser`. The password comes
from Vault (`secrets: true`): key `SPRING_DATASOURCE_PASSWORD` in the release's path from the table. Flyway migrates
the schema on startup; `/health/ready` includes the database.

Before the first rollout:
* Create the `filebrowser` database and user, and store the password in Vault under the path of each backend release
  you list.
* Set `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` in every backend release. It is the placeholder
  `https://issuer.invalid` until Authelia runs; health is green, but every API call fails.
* Register `.devops/helm` of this repo in the cluster's Flux (GitRepository and Kustomization).
* Add the deploy GitHub App (on the `main` ruleset bypass list): variable `DEPLOY_APP_CLIENT_ID`, secret
  `DEPLOY_APP_PRIVATE_KEY`. CI then writes each new `main` image tag into every file under `.devops/helm`; without them
  no tag is bumped.
* For the web client, set the repository variables `OIDC_ISSUER` (and `OIDC_CLIENT_ID` if not `filebrowser`).

`devops.yml` validates every manifest in `.devops/helm`, listed or not, with kubeconform (Kubernetes and Flux
schemas); it skips `kustomization.yaml`. The chart lives outside this repo, so CI does not render it with the patches.

## Quality gate
    ./gradlew check build

Runs ktlint, tests and JaCoCo (line coverage over 80 %, branch coverage over 75 %).

## CI
GitHub Actions runs the gate on every PR. Pushes to `main` publish `ghcr.io/jagieloadrian/filebrowserapi:main-<yyyyMMddHHmmss>-<sha7>`,
`v*` tags publish the semver tag. One Gradle run builds the Wasm bundle with `OIDC_ISSUER` and `OIDC_CLIENT_ID` and the
Jib image that carries it; `ghcr.io/jagieloadrian/filebrowserapi-ui` copies the same bundle and gets the same tags.
Dependabot keeps Gradle and Actions versions current.
