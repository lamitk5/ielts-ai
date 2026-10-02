# Phase 4C — Writing Assessment and Versioning Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Preserve every Writing submission/version and add safe, provider-neutral AI evaluation with honest Band ước lượng states.

**Architecture:** Writing uses the 4A canonical submission and immutable version snapshots. A normalized evaluator validates AI output before persistence, keeps AI and human results separate, and makes provider failure retryable without changing the learner’s original essay.

**Tech Stack:** Spring Boot, Java 21, existing `AiProvider`/`AiProviderRouter`, JDBC/PostgreSQL, Flyway, JUnit 5, React/Vite.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-4-learning-assessment-design.md`

**Global Constraints:** Task 1 and Task 2 criteria are mutually exclusive. Use `Band ước lượng bởi AI` and `Không phải điểm thi IELTS chính thức.`. Never fabricate a grade, evidence, transcript, or evaluator certainty; all automated tests mock providers.

**Review Focus:**

1. An AI timeout never destroys the submitted essay.
2. A malformed AI response never becomes a grade.
3. The original Writing version is immutable.
4. An AI estimate is never stored as a human grade.
5. Task 1 and Task 2 criteria cannot be mixed accidentally.

### Task 1: Map Writing attempts to canonical submissions and immutable versions

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/writing/WritingSubmissionVersion.java`, `WritingVersionRepository.java`, `JdbcWritingVersionRepository.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/writing/WritingAttempt.java`, `WritingRepository.java`, `WritingAssessmentService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingSubmissionVersionTest.java`, `backend/src/test/java/com/ieltsaitutor/writing/WritingVersionRepositoryTest.java`

**Interfaces:**
- Consumes: canonical submission ID, task/version metadata, response text, parent version ID
- Produces: immutable sequential Writing version with word count, timestamp, and canonical submission link

- [ ] Step 1: Write failing tests for first version, sequential versions, parent relationship, and mutation rejection.
- [ ] Step 2: Run focused tests and verify RED because current `writing_submissions` stores one assessment payload without version history.
- [ ] Step 3: Add the minimal version domain/repository and additive schema allocation from the 4A-integrated base.
- [ ] Step 4: Run repository/migration tests and verify GREEN.
- [ ] Step 5: Commit with `feat(writing): add immutable submission versions`.

### Task 2: Implement task-type-specific rubric policies

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/writing/WritingTaskRubric.java`, `WritingRubricPolicy.java`, `WritingEvaluationCommand.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/writing/WritingTask.java`, `WritingAssessmentService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingRubricPolicyTest.java`

**Interfaces:**
- Consumes: task ID/type and submitted version metadata
- Produces: exactly four criteria: Task Achievement for Task 1 or Task Response for Task 2, plus Coherence & Cohesion, Lexical Resource, and Grammatical Range & Accuracy

- [ ] Step 1: Write failing tests for Task 1/Task 2 mappings, unknown task, and cross-rubric contamination.
- [ ] Step 2: Run tests and verify RED for absent explicit policy.
- [ ] Step 3: Implement a pure rubric policy with version identifier and provider-neutral command.
- [ ] Step 4: Run unit and existing Writing tests and verify GREEN.
- [ ] Step 5: Commit with `feat(writing): enforce task-specific rubric`.

### Task 3: Define and validate the provider-neutral evaluator contract

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/writing/WritingEvaluator.java`, `WritingEvaluationResult.java`, `WritingEvaluationValidator.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/writing/WritingAssessmentService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingEvaluationValidatorTest.java`, `backend/src/test/java/com/ieltsaitutor/writing/WritingAssessmentServiceTest.java`

**Interfaces:**
- Consumes: task rubric, immutable response version, provider-neutral `AiChatResult`
- Produces: validated estimate/criteria/strengths/issues/suggestions/evidence/priority improvements/grounding/disclaimer or a retryable unavailable result

- [ ] Step 1: Write failing tests for valid JSON, missing required fields, band range, wrong criteria, malformed arrays, and raw provider errors.
- [ ] Step 2: Run focused tests and verify RED.
- [ ] Step 3: Implement schema validation and normalized result mapping; reject malformed output without persisting a grade.
- [ ] Step 4: Run Writing provider-mocked tests and verify GREEN.
- [ ] Step 5: Commit with `feat(writing): validate provider-neutral evaluation`.

### Task 4: Add retry-safe AI evaluation lifecycle and persistence

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/writing/WritingEvaluationRepository.java`, `JdbcWritingEvaluationRepository.java`, `WritingEvaluationService.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/writing/WritingController.java`, `WritingAssessmentService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingEvaluationLifecycleTest.java`

**Interfaces:**
- Consumes: submitted Writing version, evaluation command, mocked `AiProvider`
- Produces: `SUBMITTED` → `AI_EVALUATING` → `GRADED` or retryable `FAILED`, with evaluation version and separate immutable source text

- [ ] Step 1: Write failing tests for timeout/429/provider-unavailable, retry, duplicate evaluation request, and safe status responses.
- [ ] Step 2: Run tests and verify RED because current assessment persists a single payload and collapses unavailable states.
- [ ] Step 3: Implement durable lifecycle, idempotent evaluation key, and failure-safe persistence.
- [ ] Step 4: Run backend Writing tests and verify GREEN without live provider calls.
- [ ] Step 5: Commit with `feat(writing): add retryable evaluation lifecycle`.

### Task 5: Add version comparison and defensible issue tracking

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/writing/WritingVersionComparisonService.java`, `WritingVersionComparisonView.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/writing/WritingController.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingVersionComparisonTest.java`

**Interfaces:**
- Consumes: two owned immutable versions and their available evaluations
- Produces: changed paragraphs, resolved/repeated issues only when evidence supports them, criteria deltas, and explicit unavailable fields

- [ ] Step 1: Write failing tests for paragraph changes, missing evaluation, repeated issue, and false-resolution prevention.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement bounded comparison and an owner-scoped endpoint.
- [ ] Step 4: Run focused tests and verify GREEN.
- [ ] Step 5: Commit with `feat(writing): compare response versions safely`.

### Task 6: Publish only trusted Writing evidence to Phase 2

**Files:**
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningEvidencePipeline.java`, `backend/src/main/java/com/ieltsaitutor/writing/WritingEvaluationService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingAdaptiveEvidenceTest.java`

**Interfaces:**
- Consumes: durable graded evaluation/version and confidence/status
- Produces: one idempotent Writing learning event; no profile/band update on unavailable or failed evaluation

- [ ] Step 1: Write failing tests for graded evaluation, unavailable evaluation, duplicate retry, and no fabricated score.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Add the smallest event adapter using existing Phase 2 services and source references.
- [ ] Step 4: Run adaptive/Writing regression tests and verify GREEN.
- [ ] Step 5: Commit with `feat(writing): publish trusted evaluation evidence`.

### Task 7: Build the Writing result, version, and failure UI

**Files:**
- Create: `frontend/src/components/results/WritingResultShell.jsx`, `frontend/src/components/results/WritingVersionHistory.jsx`
- Modify: `frontend/src/pages/WritingPage.jsx`, `frontend/src/services/writingApi.js`, `frontend/src/components/workspace/WritingEditorPane.jsx`
- Test: `frontend/src/__tests__/writing-results-versioning.test.jsx`

**Interfaces:**
- Consumes: lifecycle/evaluation/version DTOs and retry-safe errors
- Produces: comfortable editor state, criteria breakdown, “Band ước lượng bởi AI”, disclaimer, history/comparison, and truthful loading/failure UI

- [ ] Step 1: Write failing component/API tests for draft recovery, AI evaluating, graded, failed/retry, version history, and keyboard actions.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement provider-neutral UI using existing `GlassCard`, `SkeletonBlock`, and design tokens without changing global styling.
- [ ] Step 4: Run targeted Writing/workspace tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add writing evaluation history`.

### Task 8: Verify Writing end to end

**Files:**
- Modify: only 4C files required by acceptance failures
- Test: `backend/src/test/java/com/ieltsaitutor/writing/WritingAttemptEndToEndTest.java`, `backend/src/test/java/com/ieltsaitutor/writing/WritingProviderUnavailableTest.java`, `frontend/src/__tests__/writing-attempt-flow.test.jsx`

**Interfaces:**
- Consumes: mocked providers with valid/invalid/timeout responses and a clean database
- Produces: immutable versions, correct rubric, safe retry, separate AI/human-ready records, and no source text loss

- [ ] Step 1: Write failing acceptance coverage for Task 1, Task 2, timeout, malformed response, and repeated evaluation.
- [ ] Step 2: Run the acceptance tests and verify RED before final wiring.
- [ ] Step 3: Make only minimal integration fixes.
- [ ] Step 4: Run focused backend/frontend regressions and verify GREEN.
- [ ] Step 5: Commit with `test(writing): verify phase 4c acceptance`.
