# football-stats-app

Spring Boot backend environment for the Football Stats project.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+

## Local environment (.env style)

1. Create `.env` from `.env.example`.
2. Adjust values if needed.

Spring Boot reads `.env` via:

- `spring.config.import=optional:file:.env[.properties]`

## Run

```bash
mvn spring-boot:run
```

## Build

```bash
mvn clean package
```

## Current local DB defaults

- Host: `localhost`
- Port: `5432`
- DB: `football_db`
- User: `football_app`
- Password: `football_app_dev`

## Database schema

- Application data schema: `work`
- Flyway history schema: `public`
- Active application schema comes from `.env` variable `DB_SCHEMA` (default: `work`)

Objects created in early migrations are moved to `work` by `V3__move_objects_to_work_schema.sql`.

Table naming with `w_` prefix and Russian comments are applied by `V5__w_prefix_and_ru_comments.sql`:

- `work.w_user_login`
- `work.w_bootstrap_log`

Convention: all application tables in schema `work` must start with `w_`.

## Authentication API

- `POST /api/auth/register`
	- body: `{ "email": "user@mail.com", "name": "User", "password": "secret123" }`
	- creates user with default role `USER` and returns JWT token

- `POST /api/auth/login`
	- body: `{ "email": "user@mail.com", "password": "secret123" }`
	- returns JWT token and active roles

- `GET /api/auth/me`
	- header: `Authorization: Bearer <token>`
	- returns current user payload with active roles

## Role model and permissions

Roles in DB table `work.w_role`:

- `SUPER_ADMIN`
- `USER`
- `TEAM_REP`

Server-side method restrictions for `/api/**`:

- `GET`: `USER`, `TEAM_REP`, `SUPER_ADMIN`
- `POST`: `TEAM_REP`, `SUPER_ADMIN`
- `PUT`: `TEAM_REP`, `SUPER_ADMIN`
- `PATCH`: `TEAM_REP`, `SUPER_ADMIN`
- `DELETE`: `TEAM_REP`, `SUPER_ADMIN`

Anonymous access is allowed only for:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/health`

## Access management API (only SUPER_ADMIN)

Base path: `/api/admin/access`

- `POST /users/{userId}/roles/{roleCode}`
- `DELETE /users/{userId}/roles/{roleCode}`
- `POST /users/{userId}/team-scopes`
	- body: `{ "teamId": 1, "canEditRoster": true, "canEditApplication": true }`
- `DELETE /users/{userId}/team-scopes/{teamId}`
- `GET /users/{userId}`

Current user access overview:

- `GET /api/admin/access/me`

JWT settings are controlled through `.env`:

- `JWT_SECRET`
- `JWT_EXPIRES_MINUTES`
- `APP_SUPER_ADMIN_EMAIL` (existing user email to auto-grant `SUPER_ADMIN` on app startup)

Database schema setting in `.env`:

- `DB_SCHEMA`

## New DB objects from V6 migration

- `work.w_role`
- `work.w_user_role`
- `work.w_team`
- `work.w_user_team_scope`
- `work.w_auth_audit_log`

## API Explorer synchronization rule

When backend API is added or changed, update frontend API Explorer in the web project:

- `/Users/korytov/projects/football-stats-web/src/data/apiExplorerCatalog.js`
- `/Users/korytov/projects/football-stats-web/src/pages/ApiExplorer.vue`
- `/Users/korytov/projects/football-stats-web/src/pages/ApiExplorerTest.vue`

Session handoff and restart checklist is documented in:

- `/Users/korytov/projects/football-stats-web/API_EXPLORER_SESSION_CONTEXT_RU.md`
