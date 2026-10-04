# Phase 4A — Submission Engine Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or superpowers:subagent-driven-development

**Goal:** Create one server-authoritative, owner-scoped submission foundation for Reading, Listening, Writing, and Speaking.

**Architecture:** Evolve the current `learning_attempts`/`practice.attempt` flow behind a canonical submission service instead of creating four attempt systems. The service pins an approved practice version, owns lifecycle/timing/autosave/idempotency, and exposes compatibility adapters until all clients migrate.

**Tech Stack:** Spring Boot, Java 21, JDBC repositories, PostgreSQL, Flyway, JUnit 5, Spring MVC security tests.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-4-learning-assessment-design.md`

**Global Constraints:** No client-supplied score, answer key, user ID, status, or practice ownership is trusted. Allocate future migration numbers only after computing the highest migration on the execution base. Preserve existing attempts through additive/backfill compatibility and keep all external AI calls mocked.

**Review Focus:**

1. Duplicate submit does not create duplicate scoring.
2. User A cannot read/write User B's submission.
3. The frontend cannot forge score, answer key, or user ID.
4. An autosave conflict does not silently lose newer work.
5. The submitted practice version remains pinned after practice changes.

### Task 1: Establish the canonical submission domain and persistence boundary

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/PracticeSubmission.java`, `SubmissionStatus.java`, `SubmissionRepository.java`, `JdbcPracticeSubmissionRepository.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/attempt/PracticeAttempt.java`, `AttemptStatus.java` only for compatibility mapping
- Create: one additive Flyway migration under `backend/src/main/resources/db/migration/` using the next version calculated at execution time
- Test: `backend/src/test/java/com/ieltsaitutor/submission/PracticeSubmissionRepositoryTest.java`

**Interfaces:**
- Consumes: authenticated user ID, skill, approved publication/version identity, start idempotency key
- Produces: canonical submission ID, owner, pinned practice version, lifecycle state, timestamps, revision, and retry metadata

- [ ] Step 1: Write failing repository/domain tests for all required identity fields, valid statuses, owner/version constraints, and additive legacy compatibility.
- [ ] Step 2: Run the focused test and verify RED because the canonical submission contract and table do not exist.
- [ ] Step 3: Add the smallest domain records, repository interface, additive schema, indexes, and compatibility mapper; do not add unrelated learner tables.
- [ ] Step 4: Run the focused repository tests against the supported test database and verify GREEN, including migration freshness.
- [ ] Step 5: Commit with `feat(submission): add canonical submission foundation`.

### Task 2: Implement the legal lifecycle state machine

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionStateMachine.java`, `SubmissionConflictException.java`, `SubmissionCommandService.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionStateMachineTest.java`

**Interfaces:**
- Consumes: current submission state and a server-side command (`start`, `begin`, `submit`, `beginScoring`, `grade`, `fail`, `retry`)
- Produces: legal next state or a structured conflict; no direct state mutation from controllers

- [ ] Step 1: Write failing transition-table tests for `DRAFT`, `IN_PROGRESS`, `SUBMITTED`, `SCORING`, `GRADED`, `FAILED`, plus Writing/Speaking extensions.
- [ ] Step 2: Run the state tests and verify RED for missing legal-transition enforcement.
- [ ] Step 3: Implement the state machine and route compatibility attempt commands through it.
- [ ] Step 4: Run transition and regression tests and verify GREEN, including illegal transition error codes.
- [ ] Step 5: Commit with `feat(submission): enforce submission lifecycle`.

### Task 3: Resolve authenticated ownership and approved version pins

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionPracticeResolver.java`, `SubmissionOwnershipService.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogService.java`, `backend/src/main/java/com/ieltsaitutor/practice/PracticeService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionOwnershipAndVersionTest.java`, `backend/src/test/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogServiceTest.java`

**Interfaces:**
- Consumes: `AuthPrincipal`, published set ID/skill, and current catalog publication
- Produces: owned submission commands pinned to `published_set_id`, generated version, and publication revision; denial for inactive/unapproved content

- [ ] Step 1: Write failing tests for User A/User B isolation, unknown IDs, inactive publications, and version changes after start.
- [ ] Step 2: Run focused tests and verify RED because the current adapters resolve synthetic/catalog data without a canonical ownership/version guard.
- [ ] Step 3: Implement server-side resolution and owner checks; never accept a client owner ID or answer key.
- [ ] Step 4: Run service and controller security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(submission): pin approved practice ownership`.

### Task 4: Add revisioned autosave with optimistic concurrency

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionDraftService.java`, `DraftRevisionConflictException.java`, `SubmissionDraftCommand.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/draft/LearningDraftService.java` only where the canonical submission link is required
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionAutosaveTest.java`, `backend/src/test/java/com/ieltsaitutor/learning/draft/LearningDraftServiceTest.java`

**Interfaces:**
- Consumes: owned submission ID, draft payload, expected server revision, client idempotency token
- Produces: authoritative snapshot, incremented revision, conflict response with latest safe snapshot, and no adaptive event

- [ ] Step 1: Write failing tests for stale revisions, duplicate autosave, non-editable states, and refresh recovery.
- [ ] Step 2: Run the focused tests and verify RED for absent revision guards and canonical draft persistence.
- [ ] Step 3: Implement conditional updates, server revisions, safe conflict payloads, and compatibility with existing `learning_drafts`.
- [ ] Step 4: Run repository/service tests and verify GREEN without changing score/result fields.
- [ ] Step 5: Commit with `feat(submission): add conflict-safe autosave`.

### Task 5: Finalize submissions idempotently and calculate server duration

**Files:**
- Modify: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionCommandService.java`, `backend/src/main/java/com/ieltsaitutor/submission/JdbcPracticeSubmissionRepository.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptService.java`, `backend/src/main/java/com/ieltsaitutor/practice/attempt/JdbcAttemptRepository.java`
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionFinalizeIdempotencyTest.java`

**Interfaces:**
- Consumes: final answer/content snapshot, owner, submission ID, idempotency key, server clock
- Produces: one immutable `SUBMITTED` record with server-calculated duration and content hash; repeated identical request returns the same record

- [ ] Step 1: Write failing tests for concurrent duplicate submits, changed payload after submit, mismatched keys, timer bounds, and empty/invalid payloads.
- [ ] Step 2: Run focused tests and verify RED because the current generic attempt endpoint accepts client score/total and lacks canonical finalization.
- [ ] Step 3: Implement atomic conditional finalization and remove score/result authority from compatibility request DTOs.
- [ ] Step 4: Run service, JDBC, and existing attempt tests and verify GREEN.
- [ ] Step 5: Commit with `feat(submission): make finalization idempotent`.

### Task 6: Add paginated learner submission history

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionHistoryQuery.java`, `SubmissionHistoryController.java`, `SubmissionHistoryView.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptController.java` as a compatibility adapter
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionHistoryControllerTest.java`

**Interfaces:**
- Consumes: authenticated owner, skill/status filters, bounded page/size, sort direction
- Produces: paginated `Bài làm của tôi` summaries with status, exact practice version reference, duration, score availability, and result link

- [ ] Step 1: Write failing controller/repository tests for pagination, filters, invalid bounds, empty state, and owner isolation.
- [ ] Step 2: Run the tests and verify RED for missing history endpoint and query bounds.
- [ ] Step 3: Implement bounded queries and a learner-safe DTO that hides admin/generator internals.
- [ ] Step 4: Run focused tests and verify GREEN.
- [ ] Step 5: Commit with `feat(submission): add paginated learner history`.

### Task 7: Expose the canonical submission API and harden compatibility routes

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionController.java`, request/response DTOs, and error handler additions
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/PracticeController.java`, `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptController.java`
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionControllerSecurityTest.java`, `backend/src/test/java/com/ieltsaitutor/practice/attempt/AttemptControllerRegressionTest.java`

**Interfaces:**
- Consumes: `POST /api/submissions`, `GET /api/submissions/{id}`, draft, submit, result, and history requests
- Produces: provider-neutral status/result envelopes and safe 401/403/404/409/422 errors

- [ ] Step 1: Write failing MVC tests proving score/answer key/user ID fields are ignored or rejected and all object routes require the authenticated owner.
- [ ] Step 2: Run the controller tests and verify RED for the current client-authoritative paths.
- [ ] Step 3: Implement thin controllers over the canonical service, preserving existing routes only as adapters.
- [ ] Step 4: Run backend security/regression tests and verify GREEN.
- [ ] Step 5: Commit with `feat(submission): expose canonical submission api`.

### Task 8: Verify Phase 4A integration and migration safety

**Files:**
- Modify: only Phase 4A files that fail verification; no unrelated source
- Test: `backend/src/test/java/com/ieltsaitutor/submission/SubmissionEngineIntegrationTest.java`, `backend/src/test/java/com/ieltsaitutor/acceptance/StudentReadyLearnerFlowE2ETest.java`

**Interfaces:**
- Consumes: real approved practice fixture, mocked authenticated users, and a clean database
- Produces: evidence that start → autosave → refresh → submit → history works without duplicate state/events

- [ ] Step 1: Write failing cross-module acceptance tests for the full durable lifecycle and legacy attempt compatibility.
- [ ] Step 2: Run the acceptance test and verify RED before wiring all adapters.
- [ ] Step 3: Make only the minimal integration fixes required by the canonical contract.
- [ ] Step 4: Run focused backend tests, Flyway fresh validation, and `git diff --check`; verify GREEN.
- [ ] Step 5: Commit with `test(submission): verify phase 4a integration`.
