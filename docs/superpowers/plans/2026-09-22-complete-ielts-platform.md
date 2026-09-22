# Complete IELTS AI Tutor Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans when implementing this plan task-by-task in the current worktree.

**Goal:** Complete the remaining locally implementable authentication, persistence, four-skill practice, Tutor context, search, security, and documentation work without changing the approved Phase 2B boundaries.

**Architecture:** Add a small Spring JDBC application layer for users, opaque sessions, practice fixtures/attempts, and writing/speaking submissions. Keep official keys deterministic, keep AI behind `AiProvider`, and expose normalized DTOs to React. Replace placeholder routes with route-aware pages and explicit state handling while retaining the existing homepage and RAG admin adapter.

**Tech Stack:** Spring Boot 4.1.1, Java 21, Spring JDBC, PostgreSQL/Flyway/pgvector, React, Vite, React Router, Vitest, Testing Library, existing Lucide/Recharts/Framer Motion stack.

**Spec:** `docs/superpowers/specs/2026-09-22-complete-ielts-platform-design.md`

## Global Constraints

- Keep `vector(768)`, `GEMINI_MODEL=gemini-3.8-flash`, `GEMINI_EMBEDDING_MODEL=gemini-embedding-2`, and configured embedding dimension request behavior.
- Never expose API keys, passwords, bearer tokens, raw provider payloads, raw embeddings, chain-of-thought, or full source files to React or logs.
- Use project-owned synthetic fixtures only; do not add Cambridge IELTS or British Council materials.
- Preserve provider-independent frontend AI fields and the existing Phase 2B retrieval eligibility predicates.
- Use TDD for every new behavior and commit each coherent verified task.
- Do not push, merge, deploy, or modify billing.

## Review Focus

- Duplicate register email must be a safe 409 without revealing stored data; owned by Task 1 tests.
- A member must not read another member's attempt; owned by Task 1/2 ownership tests.
- Stored answer keys must determine Reading/Listening scores even when AI is unavailable; owned by Task 3 tests.
- Provider 429/malformed/timeout must never fabricate Writing/Speaking feedback; owned by Task 4/5 tests.
- Guest, empty account, and unauthorized UI states must not render demo personal data; owned by Task 6 tests.

### Task 1: Authentication and role foundation

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__create_auth_schema.sql`
- Create: `backend/src/main/java/com/ieltsaitutor/auth/**`
- Create: `backend/src/test/java/com/ieltsaitutor/auth/**`
- Modify: `backend/src/main/java/com/ieltsaitutor/IeltsAiTutorApplication.java`, `backend/src/main/java/com/ieltsaitutor/ai/exception/**`, `backend/src/main/java/com/ieltsaitutor/rag/admin/**`
- Create: `frontend/src/services/authApi.js`, `frontend/src/features/auth/**`, `frontend/src/__tests__/auth.test.jsx`

**Interfaces:** `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`; `Authorization: Bearer <opaque-token>`; normalized `{user:{id,email,firstName,role}}`.

- [ ] Write tests for registration, BCrypt/SCrypt-style one-way hashing, duplicate email, login, logout invalidation, `/me`, CUSTOMER/ADMIN role authorization, and missing/invalid bearer tokens.
- [ ] Run focused auth tests and observe RED because tables/controllers/services do not exist.
- [ ] Add Flyway V2 tables `app_users` and `auth_sessions` with unique email, role check, hashed token, expiry, revoked timestamp, and indexes.
- [ ] Implement repositories, password encoder, random token service, auth service, controller, bearer interceptor, and safe exception payloads.
- [ ] Add frontend auth store/service and protected route boundary; keep admin token compatibility isolated.
- [ ] Run focused tests, full backend tests, and frontend auth tests.
- [ ] Commit `feat: add persistent authentication and roles`.

### Task 2: Attempts, progress, and real member dashboard data

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__create_learning_schema.sql`
- Create: `backend/src/main/java/com/ieltsaitutor/learning/**`
- Create: `backend/src/test/java/com/ieltsaitutor/learning/**`
- Modify: `frontend/src/services/**`, `frontend/src/pages/HomePage.jsx`, `frontend/src/components/home/ProgressOverviewSection.jsx`, `frontend/src/__tests__/progress.test.jsx`

**Interfaces:** `GET /api/me/progress`, `GET /api/me/activity`, `GET /api/me/attempts`; authenticated user-scoped responses with four skills and intentional empty states.

- [ ] Write RED repository/service/controller tests for empty new account, recorded result aggregation, recent activity, and IDOR rejection.
- [ ] Add attempts/results/activity tables with user foreign keys and skill/check constraints.
- [ ] Implement user-scoped JDBC repositories and progress service; keep `?demo=member` separate from authenticated mode.
- [ ] Wire HomePage/member progress to auth state and API with loading/error/empty handling.
- [ ] Run focused/full tests and commit `feat: persist member progress and activity`.

### Task 3: Reading and Listening deterministic practice

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/practice/**`
- Create: `backend/src/main/resources/fixtures/practice/**`
- Create: `backend/src/test/java/com/ieltsaitutor/practice/**`
- Create: `frontend/src/features/reading/**`, `frontend/src/features/listening/**`, `frontend/src/pages/PracticePage.jsx`, `frontend/src/__tests__/practice.test.jsx`
- Modify: `frontend/src/App.jsx`, `frontend/src/pages/PlaceholderPage.jsx` only as replaced by real pages

**Interfaces:** `GET /api/practice/{skill}/sets`, `GET /api/practice/{skill}/sets/{id}`, `POST /api/practice/{skill}/attempts`; deterministic `{score,total,answers,review}`.

- [ ] Write RED tests for fixture loading, question validation, exact answer-key scoring, persistence, review, no cross-user access, and empty/list states.
- [ ] Add project-owned synthetic Reading and Listening sets; add a small audio asset only if repository policy permits, otherwise expose a deterministic audio-unavailable state with an accessible text control.
- [ ] Implement immutable fixture catalog, answer submission validation, scoring, attempt persistence, and progress event creation.
- [ ] Build responsive keyboard-accessible practice/review pages; AI Tutor receives skill/question context but never scores.
- [ ] Run focused/full tests and commit `feat: add deterministic reading and listening practice`.

### Task 4: Writing assessment contract and persistence

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__create_writing_schema.sql`
- Create: `backend/src/main/java/com/ieltsaitutor/writing/**`
- Create: `backend/src/test/java/com/ieltsaitutor/writing/**`
- Create: `frontend/src/features/writing/**`, `frontend/src/__tests__/writing.test.jsx`

**Interfaces:** `GET /api/practice/writing/tasks`, `POST /api/practice/writing/submissions`, `GET /api/practice/writing/submissions`; normalized assessment fields `overallBandEstimate`, `criteria`, `strengths`, `issues`, `suggestions`, `citations`, `grounding`, `disclaimer`.

- [ ] Write RED tests for Task 1/2 criterion labels, word count, persistence, malformed AI response, 429/503/timeout, insufficient evidence, and estimate disclaimer.
- [ ] Add writing task/submission/result tables and provider-independent assessment port.
- [ ] Implement validation, persistence, Gemini adapter boundary using existing `AiProvider`, and safe unavailable results with no fabricated feedback.
- [ ] Build Task 1/2 editor, submit/history/review states and “Band ước lượng” labels.
- [ ] Run focused/full tests and commit `feat: add writing practice assessment flow`.

### Task 5: Speaking workflow and provider boundary

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__create_speaking_schema.sql`
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/**`
- Create: `backend/src/test/java/com/ieltsaitutor/speaking/**`
- Create: `frontend/src/features/speaking/**`, `frontend/src/__tests__/speaking.test.jsx`

**Interfaces:** `GET /api/practice/speaking/prompts`, `POST /api/practice/speaking/attempts`, `GET /api/practice/speaking/attempts`; result status `RECORDED`, `INPUT_SAVED`, or `STT_NOT_CONFIGURED`.

- [ ] Write RED tests for Part 1/2/3 prompt loading, input persistence, unsupported transcription boundary, permission/unavailable state, and estimate disclaimer.
- [ ] Add speaking prompt/attempt tables and a `SpeechToTextProvider` interface with deterministic unavailable adapter.
- [ ] Implement prompt flow, text/recording metadata persistence, and safe feedback boundary without fake transcripts.
- [ ] Build accessible mobile-safe speaking room and history/review UI.
- [ ] Run focused/full tests and commit `feat: add speaking practice boundary`.

### Task 6: Tutor context, search, and route integration

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/search/**`, `backend/src/test/java/com/ieltsaitutor/search/**`
- Create: `frontend/src/pages/SearchPage.jsx`, `frontend/src/pages/AssessmentPage.jsx`, `frontend/src/pages/LoginPage.jsx`, `frontend/src/components/auth/**`
- Modify: `backend/src/main/java/com/ieltsaitutor/ai/**`, `frontend/src/App.jsx`, `frontend/src/components/tutor/**`, `frontend/src/services/aiTutorApi.js`, `frontend/src/__tests__/**`

**Interfaces:** `GET /api/practice/search?q=...`; Tutor context includes skill/lesson/exercise/question identifiers; all client responses remain normalized.

- [ ] Write RED tests for search validation/results/empty state, context propagation, complete response rendering, 429/503/timeout UI, grounded/insufficient citations, and no fetch on guest demo shell.
- [ ] Implement fixture-backed search service and wire HeroSearch to it.
- [ ] Add route guards and auth pages; pass practice context to FloatingTutor and keep source chips provider-independent.
- [ ] Run frontend/backend focused tests and commit `feat: integrate practice routes and tutor context`.

### Task 7: Admin auth bridge, security, and async states

**Files:**
- Modify only confirmed files under `backend/src/main/java/com/ieltsaitutor/security/**`, `backend/src/main/java/com/ieltsaitutor/rag/admin/**`, `frontend/src/pages/AdminRagPage.jsx`, `frontend/src/services/ragAdminApi.js`
- Create: `backend/src/test/java/com/ieltsaitutor/security/**`, `frontend/src/__tests__/security-states.test.jsx`

- [ ] Write RED tests for admin role enforcement, ownership, safe upload names/MIME/size, CORS, malformed input, and all loading/error/empty branches.
- [ ] Implement authenticated ADMIN authorization while retaining an explicit local token compatibility adapter only where needed by current Phase 2B docs.
- [ ] Ensure no infinite skeletons and no sensitive error/token logging.
- [ ] Run security tests and commit `fix: harden platform authorization and async states`.

### Task 8: Documentation, migration review, and whole-branch verification

**Files:**
- Create/modify: `README.md`, `backend/.env.example`, `frontend/README.md`, relevant docs under `docs/`
- Modify only confirmed defects from review

- [ ] Write a fresh migration and startup verification checklist from zero and existing Phase 2B database.
- [ ] Execute backend/frontend suites, package/build/lint, route smoke checks, accessibility/reduced-motion checks, and safe local browser verification.
- [ ] Reproduce Testcontainers skip; do not change dependencies unless a deterministic compatibility fix makes the test execute.
- [ ] Run `git diff --check`, security scans, and an independent whole-branch review.
- [ ] Commit `docs: complete platform runbook and verification` if documentation changes remain.

## Commit sequence

1. `feat: add persistent authentication and roles`
2. `feat: persist member progress and activity`
3. `feat: add deterministic reading and listening practice`
4. `feat: add writing practice assessment flow`
5. `feat: add speaking practice boundary`
6. `feat: integrate practice routes and tutor context`
7. `fix: harden platform authorization and async states`
8. `docs: complete platform runbook and verification`
