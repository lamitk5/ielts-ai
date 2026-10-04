# IELTS AI Tutor — Final Acceptance Report

## Final status

**INCOMPLETE — core runtime gate PASS; authenticated and live-provider acceptance remains unverified.**

The local database, Flyway chain, backend, frontend, automated suites and
anonymous browser smoke are healthy. Authenticated learner/admin E2E and live
AI/RAG/upload acceptance are not claimed because no documented local test
credentials are present and creating accounts was outside this acceptance run.

## Branch and integration

- Branch: `feature/final-learning-platform`
- Acceptance HEAD before this report update: `630a6cc3ab75ad87c218d65783a6ebe3ab7caa2c`
- Integrated release source: `integration/final-release-candidate`
- Docker database: `ielts_ai_tutor` in `ielts-ai-tutor-postgres`
- Legacy/shared databases were not touched.

## Features present in source

- Auth, sessions, learner/admin roles, profile and onboarding.
- Reading and Listening practice catalog, attempts, autosave, deterministic scoring, results and history.
- Writing submission versions, retryable AI evaluation, structured criteria and truthful estimated-band labeling.
- Speaking prompt/recording boundary, transcript submission and manual review boundary without fabricated pronunciation scoring.
- Tutor conversations, contextual orchestration, deterministic tools, RAG citations/insufficient-evidence behavior and supported attachment processing.
- Saved practice, search, progress, today's plan, error notebook, diagnostic and mock-test flows.
- Admin portal, RAG management, practice generator, validation, review and explicit approval before learner publication.
- Responsive Academic Luxury frontend with settings, reduced-motion behavior and accessibility tests.

## Runtime environment

| Check | Result |
|---|---|
| Docker Engine | PASS — Docker Desktop Linux engine, server 29.6.2 |
| Docker Compose | PASS — v5.3.1 |
| PostgreSQL container | PASS — `ielts-ai-tutor-postgres`, healthy, pgvector/pg16 |
| PostgreSQL port | PASS — `127.0.0.1:5432` reachable |
| Datasource | PASS — `jdbc:postgresql://127.0.0.1:5432/ielts_ai_tutor` |
| Backend runtime | PASS — Spring Boot on `http://127.0.0.1:8081` |
| Frontend runtime | PASS — Vite on `http://127.0.0.1:5173` |

## Database and Flyway

| Check | Result |
|---|---|
| First startup | PASS — 29 migrations applied through schema version V70 |
| Flyway validation | PASS — 29 migrations validated |
| Second startup | PASS — schema V70 up to date; no migration necessary |
| Flyway failures | NONE |
| Legacy database | UNTOUCHED |

## Verification executed

| Check | Result |
|---|---|
| Integrated frontend tests | PASS — 98 files / 548 tests |
| Frontend lint | PASS — exit 0; existing non-blocking warnings only |
| Frontend build | PASS — Vite build completed; chunk-size warning only |
| Backend full tests | PASS — 718 run / 0 failures / 0 errors / 8 skipped |
| Backend package | PASS — tests and executable JAR packaging completed |
| Backend health | PASS — `GET /api/health` returned HTTP 200 |
| Frontend proxy health | PASS — `GET /api/health` via port 5173 returned HTTP 200 |
| Public practice catalog | PASS — Reading practice sets returned HTTP 200 |
| Unauthenticated `/api/auth/me` | PASS — HTTP 401 |
| Unauthenticated admin API | PASS — HTTP 401 |
| Anonymous Home browser smoke | PASS — hero, four skills, progress, Én and footer rendered |
| Anonymous practice browser smoke | PASS — Reading, Listening, Writing, Speaking and catalog rendered |
| Assessment browser smoke | PASS — four skill entry points rendered |
| Search browser smoke | PASS — Writing result and practice link rendered for the requested query |
| Login/Register browser smoke | PASS — lamp-gated forms rendered and unlocked locally |
| Anonymous admin guard | PASS — `/admin` redirected to Home |
| Authenticated learner E2E | BLOCKED — no documented local learner credentials; no account created |
| Authenticated admin E2E | BLOCKED — no documented local admin credentials; no account created |
| Real Én chat/provider integration | NOT TESTED in this acceptance run — requires authenticated flow/live provider |
| RAG/grounded chat runtime | NOT TESTED in this acceptance run — requires authenticated flow/live provider |
| Upload/image attachment runtime | NOT TESTED in this acceptance run — requires authenticated flow |
| Admin generator/approval runtime | NOT TESTED in this acceptance run — requires authenticated admin |
| Ownership/role runtime smoke | PARTIAL — unauthenticated guard PASS; authenticated ownership not exercised |
| Responsive browser screenshots | NOT CAPTURED — no repository Playwright configuration; layout covered by automated tests/source review |
| `git diff --check` | PASS |

## Known limitations

- Authenticated learner/admin acceptance requires legitimate local test
  credentials or explicit authorization to create QA accounts in the isolated
  database.
- Live AI, RAG and attachment acceptance was not claimed in this run.
- Cloudflare credentials are not configured; mocked fallback coverage remains
  in the backend suite.
- Frontend source/config/report assets that were dirty before this run were
  preserved and not overwritten.
- Flyway runs with out-of-order mode enabled; the current database is valid and
  reports V70 as current.

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

No product source code was changed during this acceptance run. The backend,
frontend and PostgreSQL processes remain available for authorized follow-up
manual acceptance.
