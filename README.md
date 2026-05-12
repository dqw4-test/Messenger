# Social Network

Full-stack social network workspace with:
- Spring Boot 3 + Spring Security + JWT
- PostgreSQL + JPA
- React + Vite SPA
- Docker / Docker Compose
- GitHub Actions CI/CD templates

## Implemented

Backend:
- JWT auth flow: `register`, `login`, `refresh`, `logout`, `me`
- Protected API for all business endpoints
- Account profile editing
- Flexible privacy rules:
  - who can message me
  - who can see optional info
  - who can invite me
- Attachments API with local photo storage
- Direct chats and message history with `limit`, `offset`, `page`
- Group chats:
  - create
  - invite
  - apply
  - get invites
  - get members
  - set admins
  - group privacy
- Global search for users and groups
- Local search for current user chats and groups
- Healthcheck via `/actuator/health`

Frontend:
- login / register
- current account profile + privacy editor
- attachment upload and attachment selection for messages
- direct chats and groups as vertical lists
- paged history loading upward
- global search for users/groups
- local search for chats/groups
- group create / apply / invite / privacy controls

## API summary

Main routes:
- `/api/auth/*`
- `/api/account/*`
- `/api/users`
- `/api/attachments`
- `/api/messages/*`
- `/api/chats/*`
- `/api/groups/*`
- `/api/search/*`

Swagger:
- `/swagger-ui.html`

Health:
- `/actuator/health`

## Local run without Docker

Backend:

```bash
cp .env.example .env
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Default URLs:
- backend: `http://localhost:8080`
- frontend: `http://localhost:5173`

## Local run with Docker Compose

```bash
docker compose up --build
```

Services:
- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

## Environment variables

Backend:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO`
- `JWT_ISSUER`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`
- `JWT_ACCESS_TTL_MINUTES`
- `JWT_REFRESH_TTL_DAYS`
- `APP_UPLOAD_DIR`
- `CORS_ALLOWED_ORIGINS`

Frontend:
- `VITE_API_BASE_URL`

## Validation

Backend tests:

```bash
./mvnw -Dmaven.repo.local=/tmp/socialnetwork-m2 test
```

Frontend build:

```bash
cd frontend
npm run build
```

## CI/CD

Prepared files:
- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`
- `render.yaml`

`ci.yml` runs:
- backend tests
- frontend install + build

`deploy.yml` is prepared for Render deploy hooks and post-deploy healthcheck.

Required GitHub secrets for deploy:
- `RENDER_DEPLOY_HOOK_BACKEND`
- `RENDER_DEPLOY_HOOK_FRONTEND`
- `RENDER_HEALTHCHECK_URL`

## PaaS deployment

Prepared for Render:
- backend web service
- frontend static site
- PostgreSQL

The repository contains `render.yaml`, but actual deployment still requires:
- a Render account
- project creation/import
- real environment secrets
- deploy hook secrets in GitHub

That external account step cannot be executed from this workspace alone.
