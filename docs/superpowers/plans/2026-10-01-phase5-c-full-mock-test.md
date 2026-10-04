# Phase 5C â€” Full Mock Test Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Add a resumable IELTS-style, explicitly non-official Mock Test that composes Phase 4 submissions across all four skills.

**Architecture:** A mock session pins a definition/version and owns ordered section records; each section delegates answer/content submission and scoring to Phase 4. The server owns timing, state, autosave, idempotency, partial grading, and final result composition.

**Tech Stack:** Spring Boot, Java 21, JDBC/PostgreSQL, Flyway, React/Vite/React Router, existing workspace/result components, JUnit 5, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-5-learner-journey-design.md`

**Global Constraints:** Depends on 4Aâ€“4E. Label all UI `IELTS-style` and non-official. Reading/Listening stay deterministic; Writing uses Band Æ°á»›c lÆ°á»£ng only when available; Speaking keeps manual/text/audio boundary. No overall official score.

**Review Focus:**

1. Browser refresh does not lose saved answers.
2. An expired/completed test cannot mutate illegally.
3. Partially graded state is represented truthfully.
4. An overall estimate cannot pretend to be an official IELTS score.
5. Resume cannot expose another user's mock session.

### Task 1: Define Mock Test session/section domain and persistence

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/mock/MockTestSession.java`, `MockTestSection.java`, states, repositories
- Create: additive Flyway migration allocated after Phase 4 integrated base
- Test: `backend/src/test/java/com/ieltsaitutor/mock/MockTestDomainTest.java`, `MockTestRepositoryTest.java`

**Interfaces:**
- Consumes: owner, mock definition/version, ordered approved section references
- Produces: session/section identity, owner, state, time policy, Phase 4 submission references, and result state

- [ ] Step 1: Write failing tests for identity, section order, version pinning, legal states, ownership, and terminal immutability.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Add minimal domain/repository/schema; do not duplicate submission answer storage.
- [ ] Step 4: Run repository/migration tests and verify GREEN.
- [ ] Step 5: Commit with `feat(mock): add mock session foundation`.

### Task 2: Implement server-owned mock state and timing policy

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/mock/MockTestStateMachine.java`, `MockTestTimingService.java`, conflict exceptions
- Test: `backend/src/test/java/com/ieltsaitutor/mock/MockTestStateMachineTest.java`, `MockTestTimingServiceTest.java`

**Interfaces:**
- Consumes: session state, server timestamps, pause/resume/section commands
- Produces: legal transitions, elapsed seconds, expiry/time-warning state, and immutable terminal state

- [ ] Step 1: Write failing tests for create/start/pause/resume/expire/submit, clock drift, section boundaries, and duplicate commands.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement pure state/timing policy with injected clock.
- [ ] Step 4: Run unit tests and verify GREEN.
- [ ] Step 5: Commit with `feat(mock): enforce timed session lifecycle`.

### Task 3: Compose approved sections and pin Phase 4 submissions

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/mock/MockTestService.java`, `MockSectionResolver.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionPracticeResolver.java` only for mock source metadata
- Test: `backend/src/test/java/com/ieltsaitutor/mock/MockTestServiceTest.java`

**Interfaces:**
- Consumes: approved catalog, mock definition, owner, section order
- Produces: fixed Reading/Listening/Writing/Speaking section set with exact practice/version pins and owned Phase 4 submission IDs

- [ ] Step 1: Write failing tests for approved-only sections, version change after start, missing capability, and user isolation.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement composition through catalog/submission services.
- [ ] Step 4: Run service/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(mock): compose versioned practice sections`.

### Task 4: Add mock autosave, resume, and interruption recovery API

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/mock/MockTestController.java`, DTOs
- Modify: `backend/src/main/java/com/ieltsaitutor/submission/SubmissionDraftService.java` only for section delegation
- Test: `backend/src/test/java/com/ieltsaitutor/mock/MockTestControllerSecurityTest.java`

**Interfaces:**
- Consumes: owner session, section draft, expected revisions, resume request
- Produces: authoritative session/section snapshots, conflict state, and safe resume after refresh/network loss

- [ ] Step 1: Write failing MVC tests for autosave, stale revision, refresh, expiry, duplicate resume, and User A/B isolation.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement thin controller over mock/submission services.
- [ ] Step 4: Run controller/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(mock): add resumable mock api`.

### Task 5: Implement submit, partial grading, and full result composition

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/mock/MockTestGradingService.java`, `MockTestResult.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/results/LearnerResultViewMapper.java`
- Test: `backend/src/test/java/com/ieltsaitutor/mock/MockTestGradingServiceTest.java`

**Interfaces:**
- Consumes: finalized section submissions and Phase 4 result statuses
- Produces: `PARTIALLY_GRADED`, `FULLY_GRADED`, or `FAILED` result with section breakdown and explicit unavailable components

- [ ] Step 1: Write failing tests for deterministic objective sections, pending Writing/Speaking, retry, and no official overall estimate.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Compose results without recalculating skill scores or inventing an overall band.
- [ ] Step 4: Run service/result tests and verify GREEN.
- [ ] Step 5: Commit with `feat(mock): compose truthful partial results`.

### Task 6: Build the Mock Test learner workspace

**Files:**
- Create: `frontend/src/pages/MockTestPage.jsx`, `frontend/src/components/mock/MockTestShell.jsx`, `MockTestTimer.jsx`, `MockSectionNavigator.jsx`
- Create: `frontend/src/services/mockTestApi.js`
- Test: `frontend/src/__tests__/mock-test.test.jsx`

**Interfaces:**
- Consumes: session/section/timer/autosave/status APIs
- Produces: four-section navigation, timer warnings, resume/conflict UI, submit controls, accessible keyboard flow, and non-official copy

- [ ] Step 1: Write failing UI/API tests for start, navigation, refresh rehydrate, autosave failure, expiry, reduced motion, and mobile layout semantics.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement with existing Reading/Listening/Writing/Speaking workspaces and shared components.
- [ ] Step 4: Run targeted workspace tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add mock test workspace`.

### Task 7: Build partial/full Mock Test results

**Files:**
- Create: `frontend/src/components/mock/MockTestResult.jsx`
- Modify: `frontend/src/App.jsx`, `frontend/src/components/results/LearnerResultShell.jsx`, `frontend/src/services/mockTestApi.js`
- Test: `frontend/src/__tests__/mock-test-results.test.jsx`

**Interfaces:**
- Consumes: section result statuses, score/estimate/disclaimer fields, retry actions
- Produces: section-by-section result and next actions without a fabricated official overall score

- [ ] Step 1: Write failing tests for partial, full, failed, pending manual review, and no-estimate states.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement minimal result presentation and routing.
- [ ] Step 4: Run targeted/full result regressions and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add mock test results`.

### Task 8: Verify Mock Test recovery and security end to end

**Files:**
- Modify: only 5C files required by acceptance failures
- Test: `backend/src/test/java/com/ieltsaitutor/acceptance/MockTestAcceptanceTest.java`, `frontend/src/__tests__/mock-test-flow.test.jsx`

**Interfaces:**
- Consumes: clean database, approved four-skill definitions, mocked AI/STT, two users
- Produces: create â†’ autosave â†’ refresh â†’ resume â†’ expire/submit â†’ partial/full result with no cross-user access

- [ ] Step 1: Write failing cross-module tests for refresh, duplicate submit, expiry, partial grading, and owner isolation.
- [ ] Step 2: Run acceptance tests and verify RED.
- [ ] Step 3: Make minimal integration fixes.
- [ ] Step 4: Run focused backend/frontend tests and verify GREEN.
- [ ] Step 5: Commit with `test(mock): verify full mock acceptance`.
