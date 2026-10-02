# Phase 5A — Onboarding and Diagnostic Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Add optional learner onboarding and a versioned diagnostic that produces an honest Estimated starting profile without turning self-report into measured ability.

**Architecture:** Onboarding goals are stored separately from Phase 2 evidence. Diagnostic sections reuse Phase 4 submissions/scoring and retain immutable diagnostic/session/result versions with confidence states.

**Tech Stack:** Spring Boot, Java 21, JDBC/PostgreSQL, Flyway, React/Vite/React Router, existing practice catalog and result APIs, JUnit 5, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-5-learner-journey-design.md`

**Global Constraints:** Depends on Phase 4 canonical submission/scoring contracts. Self-reported current level/weak skill is never written as evidence. Diagnostic copy must say `Estimated starting profile`, not official placement; incomplete or unavailable sections remain insufficient evidence.

**Review Focus:**

1. Self-reported weakness never becomes measured weakness.
2. An incomplete diagnostic yields an insufficient-evidence state.
3. A diagnostic session cannot be read by another user.
4. A retake preserves prior diagnostic history.
5. Diagnostic output never claims an official IELTS level.

### Task 1: Persist owner-scoped onboarding goals

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/onboarding/LearnerOnboardingProfile.java`, repository, service, DTOs
- Create: additive Flyway migration allocated after the Phase 4 integrated base
- Test: `backend/src/test/java/com/ieltsaitutor/onboarding/OnboardingProfileServiceTest.java`

**Interfaces:**
- Consumes: authenticated owner goals, bounded band/date/minutes/days/skill fields, skip/edit commands
- Produces: versioned owner-scoped onboarding profile with completion/skipped state and no adaptive evidence mutation

- [ ] Step 1: Write failing tests for validation, skip, edit/version, ownership, and separation from `StudentLearningProfile`.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement repository/service/API model and additive schema only.
- [ ] Step 4: Run service/repository/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(onboarding): persist learner goals separately`.

### Task 2: Add onboarding API and learner setup UI

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/onboarding/OnboardingController.java`
- Create: `frontend/src/components/onboarding/OnboardingPanel.jsx`, `frontend/src/services/onboardingApi.js`
- Modify: `frontend/src/App.jsx`, `frontend/src/pages/HomePage.jsx`, `frontend/src/features/auth/AuthProvider.jsx`
- Test: `backend/src/test/java/com/ieltsaitutor/onboarding/OnboardingControllerSecurityTest.java`, `frontend/src/__tests__/onboarding.test.jsx`

**Interfaces:**
- Consumes: authenticated session and onboarding profile API
- Produces: optional Skip/Edit setup, accessible validation, reload recovery, and neutral home/dashboard entry without fake personalization

- [ ] Step 1: Write failing API/UI tests for first learner, partial state, skip, edit later, unauthenticated denial, and keyboard flow.
- [ ] Step 2: Run focused backend/frontend tests and verify RED.
- [ ] Step 3: Implement the smallest route/gate and form using existing design system components.
- [ ] Step 4: Run targeted tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add learner onboarding flow`.

### Task 3: Define versioned diagnostic session and content resolver

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/diagnostic/DiagnosticSession.java`, `DiagnosticState.java`, `DiagnosticContentResolver.java`, repository/service
- Create: additive migration allocated after the Phase 4 base
- Test: `backend/src/test/java/com/ieltsaitutor/diagnostic/DiagnosticSessionServiceTest.java`

**Interfaces:**
- Consumes: owner, diagnostic definition/version, approved Reading/Listening/Writing/Speaking content
- Produces: owner-scoped resumable diagnostic session with immutable content version and state transitions

- [ ] Step 1: Write failing tests for create/resume/submit/retake, content pinning, ownership, and no duplicate active session.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement session state/domain and approved-content resolver over Phase 4 APIs.
- [ ] Step 4: Run service/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(diagnostic): add versioned diagnostic sessions`.

### Task 4: Implement objective diagnostic sections

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/diagnostic/DiagnosticSubmissionService.java`, `DiagnosticResult.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/results/LearnerResult.java` only for diagnostic source metadata
- Test: `backend/src/test/java/com/ieltsaitutor/diagnostic/DiagnosticObjectiveSectionTest.java`

**Interfaces:**
- Consumes: diagnostic session and Phase 4 deterministic Reading/Listening submission contracts
- Produces: per-skill score/evidence/confidence source records without writing official placement or bypassing server scoring

- [ ] Step 1: Write failing tests for complete, incomplete, unavailable-media, and retaken objective sections.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Delegate scoring to Phase 4 and persist diagnostic source/result references.
- [ ] Step 4: Run tests and verify GREEN.
- [ ] Step 5: Commit with `feat(diagnostic): reuse deterministic objective scoring`.

### Task 5: Add Writing and safe Speaking diagnostic paths

**Files:**
- Modify: `backend/src/main/java/com/ieltsaitutor/diagnostic/DiagnosticSubmissionService.java`, `backend/src/main/java/com/ieltsaitutor/writing/WritingAssessmentService.java`, `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/diagnostic/DiagnosticWritingSpeakingTest.java`

**Interfaces:**
- Consumes: Phase 4 Writing evaluator and Speaking text/manual boundary
- Produces: Band ước lượng only when real evaluation exists, transcript source state, and insufficient evidence when capability is unavailable

- [ ] Step 1: Write failing tests for AI unavailable, malformed Writing evaluation, text-only Speaking, and no-STT audio.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Add diagnostic adapters without new scoring/AI/STT engines.
- [ ] Step 4: Run Writing/Speaking/diagnostic tests and verify GREEN.
- [ ] Step 5: Commit with `feat(diagnostic): add safe writing speaking paths`.

### Task 6: Aggregate confidence and Estimated starting profile

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/diagnostic/DiagnosticAggregationService.java`, controller, DTOs
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningProfileService.java` only for explicit diagnostic evidence source integration
- Test: `backend/src/test/java/com/ieltsaitutor/diagnostic/DiagnosticAggregationServiceTest.java`, `DiagnosticControllerSecurityTest.java`

**Interfaces:**
- Consumes: immutable section evidence, self-report only as separate display context
- Produces: per-skill confidence, `INSUFFICIENT_DATA` states, initial recommendations, and Estimated starting profile response

- [ ] Step 1: Write failing tests for complete/partial/empty diagnostic, confidence thresholds, retake history, and official-claim absence.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement aggregation and owner-scoped result endpoint; never overwrite stronger Phase 2 evidence with self-report.
- [ ] Step 4: Run service/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(diagnostic): aggregate honest starting profile`.

### Task 7: Build diagnostic UI and acceptance flow

**Files:**
- Create: `frontend/src/pages/DiagnosticPage.jsx`, `frontend/src/components/diagnostic/DiagnosticStepper.jsx`, `DiagnosticResult.jsx`, `frontend/src/services/diagnosticApi.js`
- Modify: `frontend/src/App.jsx`, `frontend/src/pages/HomePage.jsx`
- Test: `frontend/src/__tests__/diagnostic.test.jsx`, `backend/src/test/java/com/ieltsaitutor/acceptance/DiagnosticAcceptanceTest.java`

**Interfaces:**
- Consumes: session/section/result APIs and confidence states
- Produces: resumable diagnostic, honest unavailable states, Estimated starting profile, and route into neutral/targeted practice

- [ ] Step 1: Write failing UI/acceptance tests for resume, skip, incomplete, retake, and result wording.
- [ ] Step 2: Run targeted tests and verify RED.
- [ ] Step 3: Implement minimal accessible flow using existing result/Academic Luxury components.
- [ ] Step 4: Run backend/frontend targeted tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add diagnostic journey`.
