# Roadmap — FileBrowserAPI

Stan na 2026-11-04. Projekt: Kotlin 2.0 + Spring Boot 3.4, moduł `FilebrowserBE` z dwoma trybami
uruchomienia (profil `web` = REST :8080, profil `server` = gRPC :9090), pusty moduł `FilebrowserUI`.

## Analiza — stan obecny

**Mocne strony**
- Prosta, czytelna warstwa serwisów (`FileManager`, `DirectoryManager`) współdzielona przez REST i gRPC.
- Testy jednostkowe serwisów i kontrolerów (33 testy, zielone).
- Swagger UI, Actuator, logowanie żądań (interceptory REST i gRPC).
- Obraz Docker (multi-stage, non-root, wolumen `/data`) + CI w GitHub Actions.

**Problemy znalezione w analizie**
- **Bezpieczeństwo: path traversal.** Parametr `path` jest doklejany do katalogu bazowego bez normalizacji
  (`File("${defaultPath}/$path")`), więc `../../etc` wychodzi poza `/filebrowser`. Brak uwierzytelniania.
- Pobieranie pliku zwraca `multipart/form-data` zamiast właściwego typu MIME; cały plik ładowany do pamięci (`readBytes`).
- Brak limitów rozmiaru uploadu (domyślne 1 MB Springa, 4 MB gRPC — niespójne).
- REST nie ma `renameFile`, które gRPC już ma — brak parzystości API.
- Rozjechane wersje zależności: `grpc-netty` 1.56, `grpc-testing` 1.57, `protoc-gen-grpc-java` 1.54,
  `protoc` 3.22 vs runtime `protobuf-kotlin` 4.29, `protoc-gen-grpc-kotlin` 1.3 vs stub 1.4.
- Literówka w wersji: `1.0-SNPASHOT`.
- `FilebrowserUI` to pusty stub z `jvmToolchain(17)` (BE używa 21).
- Test gRPC startuje prawdziwy serwer na stałym porcie 9090 — konflikt, gdy port zajęty.
- README: instrukcja „utwórz katalog filebrowser” jest nieaktualna (aplikacja tworzy go sama).

## Etap 1 — Bezpieczeństwo i poprawność (P0)
1. Normalizacja ścieżek w jednym miejscu (`PathModifierUtils`): `Path.normalize()` + sprawdzenie
   `startsWith(baseDir)`; odrzucenie `..` i ścieżek absolutnych. Test regresyjny na traversal.
2. Limity uploadu: `spring.servlet.multipart.max-file-size` oraz `maxInboundMessageSize` w gRPC, spójne wartości.
3. Poprawny `Content-Type` przy pobieraniu (`Files.probeContentType`) + `Content-Disposition`.
4. Streaming dużych plików (`Resource`/`InputStreamResource` w REST, streaming RPC w gRPC) zamiast `readBytes`.
5. Poprawić wersję `1.0-SNAPSHOT`.

## Etap 2 — Spójność API i jakość
1. Endpoint REST `PATCH /api/v1/files` (rename) — parzystość z gRPC.
2. Jednolity format błędów (RFC 7807 `ProblemDetail`) w REST; mapowanie wyjątków na `Status` w gRPC już jest — uzupełnić testy.
3. Test gRPC na `InProcessServerBuilder` lub porcie losowym (0) zamiast stałego 9090.
4. Testy integracyjne REST na prawdziwym systemie plików (`@SpringBootTest` + `@TempDir`).
5. Wyrównanie wersji gRPC/protobuf (jedna wersja `grpc`, protoc 4.x), aktualizacja Spring Boot do 3.5.x, Gradle do 8.14.
6. Dependabot (Gradle, GitHub Actions, Docker) — automatyczne PR z aktualizacjami.

## Etap 3 — Uwierzytelnianie i autoryzacja
1. Spring Security + OAuth2 Resource Server (JWT) — Keycloak jako IdP (zgodnie z README).
2. Autoryzacja w gRPC przez `ServerInterceptor` weryfikujący ten sam JWT.
3. Opcjonalnie: izolacja katalogów per użytkownik (`/data/filebrowser/<user>`).

## Etap 4 — Wdrożenie i operacje
1. `HEALTHCHECK` w Dockerfile (actuator `/actuator/health`), probe `liveness`/`readiness`.
2. Obraz multi-arch (`linux/amd64`, `linux/arm64`) w CI.
3. Manifesty Kubernetes (Deployment, Service, PVC) — ewentualnie Helm, gdy pojawi się więcej niż jedno środowisko.
4. Metryki Prometheus (`micrometer-registry-prometheus`) i ustrukturyzowane logi JSON.
5. Release przez tag `v*` — CI już publikuje obraz do GHCR; dodać changelog / GitHub Release.

## Etap 5 — Funkcje
1. Kopiowanie i przenoszenie plików/katalogów, usuwanie rekurencyjne (z potwierdzeniem).
2. Metadane plików w odpowiedziach (rozmiar, data modyfikacji, typ) zamiast samych ścieżek.
3. Wyszukiwanie po nazwie/rozszerzeniu, paginacja dużych drzew.
4. Baza danych (README) — tylko jeśli pojawią się dane, których system plików nie trzyma
   (udostępnianie, tagi, uprawnienia). Do tego czasu YAGNI.

## Etap 6 — FilebrowserUI
1. Decyzja: klient (Compose Desktop / web SPA na gRPC-Web lub REST) albo usunięcie pustego modułu.
2. Jeśli klient: przeglądanie drzewa, upload drag&drop, pobieranie, rename/delete.

## Zrobione
- [x] Obraz Docker działający dla obu profili (`web`, `server`).
- [x] CI w GitHub Actions: build + testy, obraz do `ghcr.io` na `main` i tagach `v*`.
- [x] Naprawiony zawieszający się test gRPC / blokujący start kontekstu.
