# Project Udaan

A collaborative problem-solving platform: citizens raise local problems; individuals, NGOs, companies and authorities propose and deliver solutions. The first pilot is in Lucknow.

| Part | Stack | Folder |
| --- | --- | --- |
| API | Java 21, Spring Boot 4.1, PostgreSQL 16 + PostGIS, Flyway | `backend/` |
| Web app | React 19, TypeScript, Vite, Tailwind CSS, TanStack Query, React Router | `web/` |
| Pipelines | GitHub Actions: CI on pull requests; deploy to test, then production after approval | `.github/workflows/` |

The full architecture (modules, schema, API, security, roadmap) is in the Udaan architecture document.

## Run locally

Requirements: Docker, Java 21, Node 22.

```bash
docker compose up -d                 # PostGIS on 5432, MinIO on 9000/9001

cd backend && ./gradlew bootRun      # API on http://localhost:8080
cd web && npm install && npm run dev # Web on http://localhost:5173 (proxies /api to 8080)
```

Useful endpoints: `GET /api/v1/meta`, `GET /api/v1/categories`, `GET /actuator/health`.

## Tests

```bash
cd backend && ./gradlew test   # needs the docker compose database running
cd web && npm run lint && npm test && npm run build
```

## Environments and deployment

| Environment | Deploys when | Approval |
| --- | --- | --- |
| test | every push to `main`, after CI passes | none |
| production | right after test passes | a required reviewer clicks **Approve** in GitHub |

Production always receives the same Docker image and commit that passed on test. One-time hosting setup is in [docs/SETUP.md](docs/SETUP.md).
