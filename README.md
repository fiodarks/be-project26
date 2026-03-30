# Digital Social Archive — Backend Service (Project26)

Spring Boot backend for the **Digital Social Archive** platform. It exposes a REST API (API-first via OpenAPI) for:

- browsing public materials (map viewport + previews),
- creator workflows (upload/edit/delete own photos + metadata),
- administration (moderation actions, user roles, blocking),
- hierarchy navigation (tree + viewport).

The service runs under the base path: `http://localhost:8080/api/v1`.

## Project goal (backend scope)

The goal is to manage a **digital community archive** that helps local residents and history enthusiasts collect and share historical photos of a given area.

Backend responsibilities implemented in this service:

- expose a public REST API to browse and search materials by **geographic location** and **time period**
- expose authenticated endpoints for creators to add/manage their materials
- provide administration endpoints for moderation and security/audit support

## Architecture (hexagonal)

The code is organized in a ports-and-adapters style:

- `*.domain.*` — domain model (pure business logic)
- `*.application.*` — use cases + ports (`port.in` / `port.out`)
- `*.adapter.in.*` — inbound adapters (HTTP controllers)
- `*.adapter.out.*` — outbound adapters (persistence, storage, external APIs)

**API-first:** the contract lives in `src/main/resources/openapi/openapi.yaml` and is used during the build to generate HTTP API interfaces + DTOs.

## Roles & authorization

Authentication is **JWT Bearer** (`Authorization: Bearer <token>`).

Roles are defined in `src/main/java/com/github/fiodarks/project26/security/Role.java`:

- `VIEWER` — read-only access (mostly public endpoints; can exist for authenticated users with no editing rights)
- `CREATOR` — can upload new materials and manage *own* materials
- `ADMIN` — moderation: manage users (roles, blocking) and can override creator ownership checks (e.g. edit/delete any faulty content)

**Bootstrap admins (local/dev):** set `archive.security.bootstrap.admin-emails` (comma-separated) to grant `ADMIN` on login/registration.

## Implemented requirements (backend)

Roles in the system: `ADMIN`, `VIEWER`, `CREATOR`.

- Administrator:
  - delete defective content: `DELETE /materials/{id}`
  - edit photo metadata/description: `PUT /materials/{id}`
  - block/unblock users from adding content: `POST|DELETE /users/{userId}/block`
  - monitor activity + security audit: `GET /users`, `GET /users/{userId}`, `GET /audit/events`, `PUT /users/{userId}/roles`
- Viewer:
  - browse materials: `GET /materials`, `POST /materials/previews`, `GET /materials/{id}`, `GET /files/materials/{id}`
  - browse hierarchy (requires authentication): `GET /hierarchy`, `GET /hierarchy/tree`
  - search by bbox + time + phrase + metadata/tags + hierarchy level: `GET /materials?bbox=...&dateFrom=...&dateTo=...&search=...&filter=...&tags=...&hierarchyLevelId=...`
- Creator (includes Viewer):
  - register/login (implemented): `POST /auth/register`, `POST /auth/login`, Google OAuth2 (`/auth/google/*`)
  - upload material + required metadata: `POST /materials` (multipart/form-data)
  - update/delete own materials (admins can override): `PUT /materials/{id}`, `DELETE /materials/{id}`

Non-functional UI requirements (WCAG 2.1 AA, responsive UI, theme switching) are handled by the frontend; this backend focuses on the REST API and security rules.

## Build

Prerequisites:

- JDK **25** (the Gradle toolchain is configured to use Java 25)

Build + tests:

```powershell
.\gradlew clean build
```

## Run

### Option A: Docker Compose (recommended)

Starts Postgres + the app:

```bash
docker compose up --build
```

This uses the `local` Spring profile and exposes port `8080`.

Database credentials come from your local environment (or `.env`) via:

- `POSTGRES_DB` (default: `project26`)
- `POSTGRES_USER` (default: `postgres`)
- `POSTGRES_PASSWORD` (default: `postgres`)

Stop:

```bash
docker compose down
```

### Option B: Run locally with Gradle

#### `dev` profile (in-memory H2)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

#### `local` profile (Postgres on localhost)

1) Start Postgres (local dev only; for example via Docker):

```bash
docker run --name project26-postgres -e POSTGRES_DB=project26 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16
```

If you changed `POSTGRES_*` for Compose, use the same values here.

2) Run the app:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Config is loaded from `.env` if present (see `.env.example`).

Important: `.env` in this repository is intentionally sanitized (no real secrets). Put real OAuth/JWT secrets only in your local `.env` and don’t include it when sharing/submitting the project.

## Photo storage (uploaded files)

Materials metadata is stored in the database, while the **photo binary** is stored on the server filesystem.

- Storage directory: `archive.storage.base-dir` (default: `${user.home}/.project26/uploads`; Docker uses `/data/uploads`)
- On upload, the file is saved under: `<base-dir>/<materialId>/original.<ext>` (extension is sanitized; the file is replaced on re-upload)
- The API serves the file publicly via: `GET /api/v1/files/materials/{materialId}` (no auth)
- `archive.storage.public-base-url` controls whether `fileUrl` / `thumbnailUrl` are returned as absolute URLs (e.g. `http://localhost:8080/api/v1`) or as relative paths (`/files/materials/{id}`)

Note: at the moment `thumbnailUrl` points to the same file as `fileUrl` (no separate thumbnail generation).

## Swagger / OpenAPI

When the app is running on `localhost:8080`:

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/api/v1/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/api/v1/v3/api-docs.yaml`
