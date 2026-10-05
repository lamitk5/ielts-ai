# IELTS AI Tutor — Final Manual / Browser E2E Report

## Run metadata

- Date: 2026-10-04
- Branch: `fix/final-browser-e2e`
- Starting HEAD: `eafa24ce2fc13231c969559d592e80c5903773b2`
- Frontend: `http://127.0.0.1:5173/`
- Backend: `http://127.0.0.1:8081/`
- Database: local PostgreSQL `ielts_ai_tutor`, schema version 72

## Result

**WEB TEST STATUS: PASS — no confirmed core product defect remains.**

Attachment automation, the final Mock Test resume rerun, and exact viewport/screenshot capture remain environment-blocked and are listed separately below.

The core learner, admin, authentication, practice, result, Tutor, and Mock Test entry flows were exercised. One real Mock Test startup defect was found and fixed on this branch; the targeted regression and full backend suite pass.

## Flows tested

| Area | Result | Evidence / limitation |
| --- | --- | --- |
| Home / learner entry | PASS | Academic Luxury home, hero search, assessment CTA, four skill entry points, progress/empty state, and Én launcher rendered. |
| `/dashboard` | NOT APPLICABLE | This build intentionally has no separate `/dashboard` route; `/` is the authenticated learner entry surface. |
| Login / logout | PASS | Student and admin demo accounts logged in through the real UI; session survived reload. |
| Role isolation | PASS | Student was redirected away from `/admin` and had no admin navigation; admin saw the admin entry. |
| Practice bank | PASS | Reading, Listening, Writing, and Speaking catalog cards and saved-practice entry rendered. |
| Reading | PASS | Real attempt start, answer persistence after reload, deterministic submit, and saved result were verified. |
| Listening | PASS | Real attempt, answers, submit, and result were verified; UI truthfully states audio is not configured. |
| Writing | PASS | Draft autosave and submit produced a truthful `Band ước lượng` result with the non-official-score disclaimer. |
| Speaking | PASS | Text answer save worked; UI truthfully states STT/transcription and band scoring are not configured. |
| Mock Test | FIXED / PASS | Start previously returned 502; after the fix the real browser opened the published four-section mock with timer and navigation. Resume was not rerun in the final attempt because Docker/PostgreSQL became unavailable; no resume defect was demonstrated. |
| Vocabulary | PASS | Real empty/search/status UI rendered; external Tutor-dependent loading displayed the existing unavailable state. |
| Study plan | BLOCKED | Page rendered, but plan generation remained in the existing provider-unavailable state. |
| Analytics | PASS | Empty/insufficient-data states were truthful and did not fabricate scores. |
| Results / history | PARTIAL | Objective result pages rendered. The separate submission-history view remained empty because objective attempts are stored in the learning-attempt path. |
| Tutor | PASS | Én opened, history loaded, a real contextual question returned a complete response, and generic chat returned a complete response. |
| Attachments | BLOCKED BY TEST TOOL | The documented browser file chooser accepted `setFiles`, but the input remained empty and no preview/state appeared. No product defect was asserted from this harness limitation. |
| Admin portal | PASS | Admin dashboard, mock-test catalog, generator, and review surfaces rendered with role protection. Temporary admin content was not created because destructive cleanup was not performed. |
| Console | PASS | No browser error/warn entries were present in the final inspected session. |

## Defect found and fixed

`MockTestService.startOrResume` caught a missing canonical publication while the surrounding transaction was already marked rollback-only. The suppressed exception caused `UnexpectedRollbackException`, which was exposed as a misleading 502 `AI_PROVIDER_ERROR`.

The minimal fix checks for an active approved publication before creating a canonical submission for a mock section. A regression test proves unpublished sections skip canonical submission creation and still start the mock session.

Changed product files:

- `backend/src/main/java/com/ieltsaitutor/mock/MockTestService.java`
- `backend/src/test/java/com/ieltsaitutor/mock/MockTestServiceTest.java`

## Responsive and visual evidence

- Small viewport was inspected in the available browser surface.
- Explicit 1440px, 768px, and 390px viewport control was not exposed by the available CUA surface, so those widths are **NOT VERIFIED** here.
- Screenshots were **NOT CAPTURED** to disk: the available CUA screenshot API returned display bytes but provided no local-file export operation.
- Intended screenshot directory: `docs/e2e-screenshots/` (not created).

## External / environment blockers

- Browser attachment upload could not be completed through the available file-chooser automation surface.
- The final browser rerun was stopped by Docker Desktop failing to bring its engine back online; PostgreSQL port 5432 was refused by Flyway. The earlier real-browser Mock Test start evidence remains valid for the unchanged fix.
- Study-plan AI loading remained provider-dependent and unavailable during the run.
- Listening audio and Speaking STT are intentionally not configured; the UI does not claim those capabilities are active.
- Testcontainers integration was skipped by the existing test guard because its Docker named-pipe probe returned HTTP 400, although Docker CLI and the project PostgreSQL container were healthy.

## Verification

- Backend tests: **735 run, 0 failures, 0 errors, 8 skipped**
- Backend package: **PASS**
- Frontend tests: **99 files, 550 tests passed**
- Frontend lint: **PASS** (pre-existing warnings only)
- Frontend build: **PASS**
- `git diff --check`: **PASS**
- Runtime backend: **PASS**, `/api/health` returned `{"status":"UP"}` on port 8081
- Runtime frontend: **PASS**, port 5173 served the application

The final rerun could not restart the backend after Docker became unavailable; this is an environment blocker, not a new product failure.

## Working state

No merge, push, or deployment was performed. The branch contains only the focused Mock Test fix, its regression test, and this report pending the final commit.
