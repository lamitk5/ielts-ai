# IELTS AI Tutor — Final Acceptance Report

## Final status

**INCOMPLETE — runtime/database gate blocked in the current environment.**

The integrated source and automated frontend regression are ready for the next
QA gate, but the application cannot be truthfully marked production/demo-ready
until PostgreSQL/Flyway startup and browser smoke are rerun with Docker
available.

## Branch and integration

- Branch: `feature/final-learning-platform`
- Starting baseline: `master` at `291fa11f0009e50df125d256f8e2f2573d1156de`
- Integrated release source: `integration/final-release-candidate` at `9a6a41c6033e7035b34b78da15e17bd777db2921`
- Integration commit: `23be491`

## Features present in source

- Auth, sessions, learner/admin roles, profile and onboarding.
- Reading and Listening practice catalog, attempts, autosave, deterministic scoring, results and history.
- Writing submission versions, retryable AI evaluation, structured criteria and truthful estimated-band labeling.
- Speaking prompt/recording boundary, transcript submission and manual review boundary without fabricated pronunciation scoring.
- Tutor conversations, contextual orchestration, deterministic tools, RAG citations/insufficient-evidence behavior and attachment processing for supported files.
- Saved practice, search, progress, today's plan, error notebook, diagnostic and mock-test flows.
- Admin portal, RAG management, practice generator, validation, review and explicit approval before learner publication.
- Responsive Academic Luxury frontend with settings, reduced-motion behavior and accessibility tests.

## Database and API

- Flyway migrations are present through V70 in the current source tree.
- PostgreSQL + pgvector is configured through `backend/docker-compose.yml` and environment-driven datasource settings.
- Backend APIs are under `/api`; frontend uses same-origin requests and Vite proxies them to `VITE_API_PROXY_TARGET`, defaulting to `http://127.0.0.1:8081`.
- No migration or database mutation was performed during this audit.

## Verification executed

| Check | Result |
|---|---|
| Baseline frontend on old `master` | 7 files / 45 tests passed |
| Integrated frontend tests | 98 files / 548 tests passed |
| Frontend lint | PASS with existing warnings |
| Frontend build | PASS; Vite emitted chunk-size warning |
| Integrated backend tests | 718 run / 0 failures / 5 errors / 8 skipped; all 5 errors are Spring context/Flyway connection refusals on `localhost:5432` |
| Local backend/frontend port contract test | 1/1 passed after fixing the default from 8080 to 8081 |
| Backend package with tests | FAIL: Maven reached the same 5 database-gated context errors; no assertion failures |
| Backend package with tests skipped | PASS; compilation and JAR packaging completed |
| Flyway first/second startup | BLOCKED: Docker/PostgreSQL unavailable |
| Docker/Compose runtime | BLOCKED: Docker client is installed, but Docker Engine named pipe is unavailable; compose could not start `postgres` |
| Local PostgreSQL runtime | NOT AVAILABLE: no `psql`, `postgres`, `pg_ctl`, or PostgreSQL Windows service found |
| Browser E2E/runtime | NOT EXECUTED: backend/database unavailable; no Playwright configuration exists in the repository |
| Real AI provider smoke | NOT EXECUTED during this audit |
| `git diff --check` | PASS for the current source diff |

## Remaining limitations

- Docker Desktop/PostgreSQL must be restored before claiming backend full pass or runtime acceptance.
- Required machine-owner action: start Docker Desktop until `docker info` succeeds, then run `docker compose -f backend/docker-compose.yml up -d postgres` and repeat the database-gated checks.
- Cloudflare credentials are not configured; fallback code is covered by mocks/tests but live Cloudflare behavior is not claimed.
- Real provider calls were intentionally not made during this audit.
- The existing local Search/logo files and user-provided report assets remain uncommitted and preserved in the working tree.
- Important commits: `23be491` integration, `5a73692` audit/report, `98f60fd` local port contract fix.

## Exact local run commands

```powershell
docker compose -f backend/docker-compose.yml up -d
cd backend
.\mvnw.cmd test
.\mvnw.cmd package
cd ..\frontend
npm test -- --run
npm run lint
npm run build
npm run dev
```

Use backend environment variables from `backend/.env.example`; never commit
real keys. Start the backend on the configured `PORT` and use the frontend
proxy default or an explicit `VITE_API_PROXY_TARGET` for the selected local
backend port.
