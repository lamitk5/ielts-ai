# Overnight Final QA — 2026-09-27

## Scope

- Branch: `feature/ai-phase2-phase3-integration`
- Starting HEAD: `372121fb101a5b68fb794e2b2a24b2b81339b34f`
- QA database: `ielts_ai_overnight_qa_20260927`
- Shared development database was inspected read-only and not modified.
- No source changes were required; no live AI provider was called.

## Flyway and runtime

- The shared database contained V1–V8 and V28, but not V9/V10. This is the expected branch-order anomaly: V28 had previously been applied from the isolated Phase 3 line before the Phase 2 migrations were present in the shared database.
- A brand-new QA database was created without dropping or reusing an existing database.
- First normal Spring Boot startup applied, in order: V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V28.
- QA database verification: 31 public tables, 11 successful Flyway rows, 0 failed rows. Representative V9, V10, and V28 objects were present.
- Second normal startup validated all 11 migrations, reported schema current/up-to-date, and left the Flyway row count unchanged.
- Backend HTTP smoke: health and public practice catalog returned 200; protected and admin endpoints rejected unauthenticated requests with 401; no migration-related 500s.
- Backend URL during QA: `http://127.0.0.1:8081`.
- Frontend URL during QA: `http://127.0.0.1:4173`.

## Automated verification

- Frontend: 329/329 tests passed across 51 files.
- Backend: 372 tests passed, 0 failures, 0 errors, 1 skipped (Testcontainers environment check).
- Frontend lint: passed with existing warnings only.
- Frontend build: passed; Vite emitted the existing large-chunk warning.
- Backend package: passed.
- Attachment-focused frontend regression: 20/20 tests passed, including attachment and admin generator/review flows.
- Backend attachment coverage: 13/13 tests passed within the full backend run.
- `git diff --check`: passed.

## Runtime and security coverage

- Phase 2 adaptive learning, Tutor memory, deterministic tools, provider routing, RAG governance, ownership, auth, and security regression suites passed.
- Phase 3 generation, validation, admin review, approval gate, audit identity, bank hydration, student boundary, and cross-phase catalog acceptance suites passed.
- Static boundary review found no Phase 2 → generator or Phase 3 → learner-profile dependency.
- Provider tests used mocks; Groq, Cloudflare, and Gemini live endpoints were not called.

## Browser QA

- Existing in-app browser tooling was used; no new browser framework was installed.
- Home, login, register, reading, writing, speaking, unknown-practice fallback, and admin generator routes served without a crash.
- Admin review is correctly routed under `/admin/practice-generator/sets/:setId`; `/admin/practice-review` is not an application route.
- Dark and light theme switching was visually checked. Home hero, skills, guest progress preview, Tutor, settings, and navigation were readable at the available 876×694 viewport.
- Exact 1440/1024/768/375 browser viewport overrides were unavailable in the existing tool, so those widths were not independently rendered. Responsive behavior remains covered by source layout rules and automated component tests.

## Testcontainers limitation

- Docker CLI was healthy on context `desktop-linux` with Docker Desktop server 29.6.2 and the project PostgreSQL container healthy.
- Testcontainers 1.20.6 still skipped its PostgreSQL integration test because the Npipe client received HTTP 400 with an empty Docker server response and the docker-machine executable was unavailable. No Docker configuration or dependency was changed.

## Result

- No Critical or Important product defects were found.
- Temporary backend/frontend processes were stopped cleanly.
- The QA database was intentionally left intact for inspection.
- Push, PR, merge, and deploy were not performed.
