# Phase 5B — Today's Plan and Error Notebook Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Turn trusted Phase 2/Phase 4 evidence into a bounded daily plan and an owner-scoped Error Notebook without inventing personalization.

**Architecture:** Today's Plan is a read/service projection over existing Phase 2 profiles, issues, roadmaps, goals, and approved practice. Error Notebook initially projects existing `learning_mistakes` plus Phase 4 question evidence; it does not create a second weakness engine.

**Tech Stack:** Spring Boot, Java 21, JDBC/PostgreSQL, existing Phase 2 services, React/Vite, existing Dashboard/Progress components, JUnit 5, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-5-learner-journey-design.md`

**Global Constraints:** Depends on trusted 4E result events and optional 5A goals. Maximum five active plan items. Every targeted recommendation has a reason/source reference; insufficient evidence falls back honestly to diagnostic/general practice. One retry never erases history.

**Review Focus:**

1. One mistake cannot become a repeated-error claim.
2. Today's Plan cannot exceed five active items.
3. No recommendation appears without a real reason or explicit fallback.
4. Error Notebook remains owner-scoped.
5. One successful retry cannot silently erase historical evidence.

### Task 1: Define Today's Plan policy over existing Phase 2 evidence

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/learning/plan/TodaysPlanItem.java`, `TodaysPlanService.java`, `TodaysPlanReason.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningRoadmapPlanner.java` only for reusable bounded policy hooks
- Test: `backend/src/test/java/com/ieltsaitutor/learning/plan/TodaysPlanServiceTest.java`

**Interfaces:**
- Consumes: profile, issues, roadmap, goals, available minutes, approved catalog
- Produces: 0–5 prioritized items with skill, duration, approved target, reason code, evidence reference, and confidence/fallback state

- [ ] Step 1: Write failing tests for cap, time budget, issue recurrence threshold, no evidence, approved availability, and stable ordering.
- [ ] Step 2: Run tests and verify RED for absent bounded plan projection.
- [ ] Step 3: Implement pure policy/service using existing Phase 2 analyzers and no new weakness scoring.
- [ ] Step 4: Run service tests and verify GREEN.
- [ ] Step 5: Commit with `feat(plan): define evidence-backed todays plan`.

### Task 2: Add owner-scoped Today's Plan API

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/learning/plan/TodaysPlanController.java`, DTOs
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningIntelligenceController.java` only for shared authentication/error conventions
- Test: `backend/src/test/java/com/ieltsaitutor/learning/plan/TodaysPlanControllerSecurityTest.java`

**Interfaces:**
- Consumes: authenticated learner, optional date/available-minutes input with server bounds
- Produces: `/api/me/today` summary, complete/skip action delegated to roadmap policy, and honest empty/fallback response

- [ ] Step 1: Write failing MVC/security tests for User A/B isolation, five-item cap, invalid inputs, and insufficient-data copy.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement thin controller over the plan service and existing roadmap completion service.
- [ ] Step 4: Run controller/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(plan): expose todays plan api`.

### Task 3: Build Today's Plan dashboard presentation

**Files:**
- Create: `frontend/src/components/learning/TodaysPlanSection.jsx`, `frontend/src/services/todaysPlanApi.js`
- Modify: `frontend/src/pages/HomePage.jsx`, `frontend/src/components/learning/TodaysFocusCard.jsx`, `frontend/src/components/home/ProgressOverviewSection.jsx`
- Test: `frontend/src/__tests__/todays-plan.test.jsx`

**Interfaces:**
- Consumes: plan items, reason/evidence references, loading/empty/error states
- Produces: `Hôm nay học gì?`, Continue Learning priority, at most five cards, neutral fallback when evidence is insufficient

- [ ] Step 1: Write failing UI/API tests for evidence reasons, item cap, loading/empty/error, completion, and no fake recommendation.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement minimal dashboard integration with existing Academic Luxury components and no styling redesign.
- [ ] Step 4: Run targeted/home/progress tests and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): show evidence-backed todays plan`.

### Task 4: Define the Error Notebook projection and bounded queries

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/learning/notebook/ErrorNotebookEntry.java`, `ErrorNotebookService.java`, `ErrorNotebookQuery.java`, controller DTOs
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningIntelligenceRepository.java` only for bounded evidence joins
- Test: `backend/src/test/java/com/ieltsaitutor/learning/notebook/ErrorNotebookServiceTest.java`, `ErrorNotebookControllerSecurityTest.java`

**Interfaces:**
- Consumes: owned `learning_mistakes`, question results, explanations, retry counts, status filters
- Produces: paginated notebook fields, recurrence/confidence, first/last seen, resolution state, and allowed actions

- [ ] Step 1: Write failing tests for single observation vs repeated issue, grouping, pagination, evidence, owner isolation, and missing answer key before submit.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement a projection over existing records; add no duplicate mistake classifier.
- [ ] Step 4: Run service/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(notebook): add owned mistake projection`.

### Task 5: Add notebook acknowledgement/resolution actions safely

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/learning/notebook/ErrorNotebookActionService.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/learning/intelligence/MistakeRecordService.java`, `MistakeStatus.java` only for explicit acknowledgement semantics
- Test: `backend/src/test/java/com/ieltsaitutor/learning/notebook/ErrorNotebookActionServiceTest.java`

**Interfaces:**
- Consumes: owned notebook entry, `Đánh dấu đã hiểu`, retry result, Tutor/similar action
- Produces: acknowledgement/audit state while preserving source mistake history and recurrence evidence

- [ ] Step 1: Write failing tests proving one retry does not delete/resolve a recurring issue, acknowledgement is owner-scoped, and stale entries conflict safely.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement explicit action policy without editing historical result rows.
- [ ] Step 4: Run notebook/adaptive regression tests and verify GREEN.
- [ ] Step 5: Commit with `feat(notebook): preserve mistake history on acknowledgement`.

### Task 6: Build Error Notebook UI and actions

**Files:**
- Create: `frontend/src/pages/ErrorNotebookPage.jsx`, `frontend/src/components/learning/ErrorNotebookList.jsx`, `frontend/src/services/errorNotebookApi.js`
- Modify: `frontend/src/App.jsx`, `frontend/src/components/learning/CommonMistakesPanel.jsx`
- Test: `frontend/src/__tests__/error-notebook.test.jsx`

**Interfaces:**
- Consumes: paginated projection, filters, acknowledgement, retry/Tutor/similar routes
- Produces: accessible `Sổ lỗi sai` with real evidence, honest empty state, and action feedback

- [ ] Step 1: Write failing UI/API tests for filters, empty/error/loading, actions, repeated counts, and keyboard navigation.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement the smallest page/component reuse; do not hardcode mistakes or scores.
- [ ] Step 4: Run targeted/progress/Tutor regressions and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add learner error notebook`.

### Task 7: Verify plan/notebook adaptive acceptance

**Files:**
- Modify: only 5B files needed by acceptance failures
- Test: `backend/src/test/java/com/ieltsaitutor/acceptance/LearningPlanNotebookAcceptanceTest.java`, `frontend/src/__tests__/learning-intelligence-adapters.test.jsx`

**Interfaces:**
- Consumes: trusted Phase 4 result events, insufficient-data learner, approved catalog
- Produces: mistake → plan reason → retry → preserved history flow with no duplicate adaptive engine

- [ ] Step 1: Write failing acceptance tests for one mistake, repeated mistake, five-item cap, no evidence, and cross-user isolation.
- [ ] Step 2: Run the acceptance tests and verify RED.
- [ ] Step 3: Make only minimal integration fixes.
- [ ] Step 4: Run focused backend/frontend tests and verify GREEN.
- [ ] Step 5: Commit with `test(plan): verify todays plan and notebook`.
