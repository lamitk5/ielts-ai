# IELTS AI Tutor — Final Acceptance Report

## Final status

**NOT READY — core and authenticated deterministic gates PASS; browser file-upload acceptance remains blocked by the local Chrome extension file-URL permission.**

The local database, Flyway chain, backend, frontend, automated suites,
authenticated learner/admin API smoke, deterministic Reading/Listening flows
and one real Én chat are healthy. The local-only `local-demo` profile provides
synthetic learner/admin accounts without changing production defaults. Browser
attachment upload could not be exercised because the connected Chrome
extension rejected local file selection until its file-URL permission is
enabled.

## Branch and integration

- Branch: `feature/final-learning-platform`
- Acceptance base HEAD: `903319623331bf4f0bf6023f4581311b4080864f`
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
| Backend full tests | PASS — 727 run / 0 failures / 0 errors / 8 skipped |
| Testcontainers PostgreSQL integration | SKIPPED — Java Testcontainers received HTTP 400 from the `docker_cli` named pipe; Docker CLI/Compose PostgreSQL runtime passed separately |
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
| Local demo authentication | PASS — `local-demo` profile, real PBKDF2 password hashing, idempotent seed; no production-default activation |
| Authenticated learner API smoke | PASS — `/api/auth/me`, profile, submissions history, saved practices; admin API denied with HTTP 403 |
| Authenticated learner browser smoke | PASS — login, Dashboard, Reading autosave/submit/result, Listening answer/submit/result, Writing workspace, Én panel |
| Authenticated admin API smoke | PASS — `/api/auth/me`, overview, practice-generator jobs, review sets |
| Real Én chat/provider integration | PASS — authenticated `hello` returned a complete response through the running provider chain |
| RAG/grounded chat runtime | NOT CLAIMED — no new grounded source was created during this acceptance run |
| Upload/image attachment runtime | BLOCKED — browser file chooser rejected local file URLs; backend attachment tests PASS |
| Admin generator/approval runtime | PARTIAL — authenticated generator/review reads PASS; no live generation/approval mutation claimed |
| Ownership/role runtime smoke | PASS — learner admin endpoint HTTP 403; owner-bound learner endpoints HTTP 200 |
| Responsive browser screenshots | NOT CAPTURED — no repository Playwright configuration; layout covered by automated tests/source review |
| `git diff --check` | PASS |

## Known limitations

- Local demo login is enabled only with `SPRING_PROFILES_ACTIVE=local-demo`
  and environment-provided credentials; passwords are intentionally not
  stored in this report or source control.
- Browser attachment acceptance requires the connected Chrome extension to
  allow access to local file URLs; the backend remains covered by automated
  attachment, ownership and validation tests.
- Grounded RAG acceptance was not rerun in this pass because no new source was
  indexed; existing mocked and integration coverage remains green.
- Cloudflare credentials are not configured; mocked fallback coverage remains
  in the backend suite.
- Frontend source/config/report assets that were dirty before this run were
  preserved and not overwritten.
- Flyway runs with out-of-order mode enabled; the current database is valid and
  reports V70 as current.
- The 8 skipped tests include the Testcontainers PostgreSQL integration group;
  the local Docker CLI uses a healthy Desktop engine, while this Java runtime's
  Testcontainers socket discovery still resolves to the `docker_cli` pipe.

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

The acceptance changes include the local-only demo-account seeder and two
runtime persistence fixes: PostgreSQL-compatible timestamp binding for durable
attempts, preservation of required objective-attempt score columns during
generic submission, and typed nullable filters for learner submission history.
The backend, frontend and PostgreSQL processes remain available for authorized
follow-up manual acceptance.
