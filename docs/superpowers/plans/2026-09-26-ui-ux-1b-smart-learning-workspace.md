# UI/UX-1B Smart Learning Workspace Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Turn Reading and Writing into contextual, responsive workspaces with safe split panes, version-bound Tutor references, truthful AI request states, and recoverable session continuity.

**Architecture:** Consume the preference, AuthGate, TutorShell, and attachment contracts produced by UI/UX-1A. Keep skill scoring and answer ownership in the current server-side practice services, add a shared split layout primitive, and resolve Tutor references against explicit workspace registries rather than arbitrary DOM selectors. Mutable Writing references are valid only for an exact trusted draft version.

**Tech Stack:** React, React Router, Vitest, Testing Library, existing practice APIs and CSS tokens, Spring Boot MVC, JDBC repositories, Flyway.

**Spec:** docs/superpowers/specs/2026-09-26-ui-ux-phase-1-learning-experience-design.md

## Global Constraints

- Dependency order is UI/UX-1A → UI/UX-1B; consume 1A interfaces rather than redefining them.
- Preserve deterministic server-side practice validation; the browser never owns answer keys, scores, band decisions, or ownership.
- Reading stable IDs may resolve without a version when passage/question content is immutable.
- Writing references containing offsets or mutable draft targets require contentVersion or draftVersion exact matching.
- Editing a draft makes old references stale; never approximate old offsets onto changed text.
- Tutor pending HTTP requests show one generic state such as “Đang chuẩn bị phản hồi…”.
- Named PREPARING, PROCESSING, or FINALIZING stages require future backend-emitted events; do not implement SSE or fake timers here.
- Drafts, practice state, routes, and references are user-scoped and revalidated before restoration.
- Mobile uses tabs or stacked panes; do not compress desktop split panes onto a phone.
- All controls are keyboard accessible, focus-visible, reduced-motion safe, and non-color-only.
- Do not add a 3D framework, large state manager, or permanent microphone loop.
- External AI calls are mocked; tests consume zero Groq, Cloudflare, or Gemini quota.

## Review Focus

1. Stale Writing reference highlights the wrong sentence: exact draft-version mismatch must render a readable stale state and never move an old offset.
2. Divider is inaccessible by keyboard: the separator must expose ARIA value semantics and keyboard resize/preset behavior without relying on pointer drag.
3. Mobile tab loses draft or scroll position: switching Reading/Writing tabs must preserve the active text, pane scroll, and selected tab.
4. Old exercise restores without authorization: session restoration must revalidate skill/set/question/draft ownership and safely discard unauthorized state.
5. AI request never settles: pending, timeout, 429, cancel, network failure, unavailable, retry, and final response must all reach a terminal UI state.

---

### Task 1: Define shared SplitLearningWorkspace interfaces and divider contract

Files:
- Create: frontend/src/components/workspace/SplitLearningWorkspace.jsx
- Create: frontend/src/components/workspace/WorkspaceDivider.jsx
- Create: frontend/src/components/workspace/WorkspacePresetControls.jsx
- Create: frontend/src/components/workspace/MobileWorkspaceTabs.jsx
- Create: frontend/src/features/workspace/workspacePreferences.js
- Modify: frontend/src/features/preferences/preferenceSchema.js
- Test: frontend/src/__tests__/split-workspace.test.jsx

Interfaces:
- Consumes: usePreferences() from UI/UX-1A; pane labels and React children supplied by the skill page.
- Produces: SplitLearningWorkspace({ left, right, leftLabel, rightLabel, ratio, onRatioChange, mobileMode }), WorkspaceDivider({ value, min, max, onChange }), and WorkspacePresetControls({ value, onChange }); ratios are 40 | 50 | 60 where value is the left-pane percentage.

- [ ] Step 1: Write failing tests for 40/60, 50/50, and 60/40 presets; independent pane regions; mobile tabs; divider role=separator; aria-valuemin=40; aria-valuemax=60; aria-valuenow; Arrow/Home/End keyboard behavior; and no pointer-only operation.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/split-workspace.test.jsx; expected failure because workspace components do not exist.
- [ ] Step 3: Implement the shared layout with CSS grid/flex, minimum readable pane widths, independent scroll containers, pointer drag with batched updates, and semantic ratio callbacks. Keep drag state local so the full application does not rerender on every pointermove.
- [ ] Step 4: Implement MobileWorkspaceTabs as real buttons with selected state and caller-owned pane content; preserve each pane's scroll container through tab changes.
- [ ] Step 5: Run and verify GREEN with the targeted test; expected all ratio, ARIA, keyboard, and mobile-tab assertions pass.
- [ ] Step 6: Commit with git add frontend/src/components/workspace frontend/src/features/workspace frontend/src/features/preferences/preferenceSchema.js frontend/src/__tests__/split-workspace.test.jsx && git commit -m "feat: add split learning workspace primitive".

### Task 2: Integrate Reading practice into the split workspace

Files:
- Create: frontend/src/components/workspace/ReadingPassagePane.jsx
- Create: frontend/src/components/workspace/ReadingQuestionPane.jsx
- Create: frontend/src/components/workspace/QuestionStateRail.jsx
- Modify: frontend/src/pages/PracticePage.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/reading-workspace.test.jsx
- Modify: frontend/src/__tests__/practice.test.jsx

Interfaces:
- Consumes: practiceFixtures, fetchPracticeSet, submitPracticeAttempt, FloatingTutor context, and SplitLearningWorkspace.
- Produces: question state model UNANSWERED | ANSWERED | FLAGGED | CURRENT | REVIEWED; registered passage/question target IDs; deterministic submit payload identical to the current practice API.

- [ ] Step 1: Write failing tests for passage-left/questions-right layout, current question focus, question navigation, answer selection, flagging, unanswered/answered/reviewed states, Reading 40/60 persistence, and no client answer key.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/reading-workspace.test.jsx src/__tests__/practice.test.jsx; expected failure because PracticePage is currently a single-column list.
- [ ] Step 3: Extract passage and question responsibilities into the new panes while retaining the existing fixture/API data and current question/attempt context sent to FloatingTutor.
- [ ] Step 4: Add the question rail and state transitions. Use labels/icons/structure in addition to color, and keep submit scoring on submitPracticeAttempt only.
- [ ] Step 5: Run and verify GREEN with targeted Reading/practice tests and existing accessibility tests.
- [ ] Step 6: Commit with git add frontend/src/components/workspace frontend/src/pages/PracticePage.jsx frontend/src/styles/globals.css frontend/src/__tests__/reading-workspace.test.jsx frontend/src/__tests__/practice.test.jsx && git commit -m "feat: add Reading learning workspace".

### Task 3: Add Writing split workspace and truthful editor state

Files:
- Create: frontend/src/components/workspace/WritingPromptPane.jsx
- Create: frontend/src/components/workspace/WritingEditorPane.jsx
- Create: frontend/src/components/workspace/DraftSaveStatus.jsx
- Modify: frontend/src/pages/WritingPage.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/writing-workspace.test.jsx
- Modify: frontend/src/__tests__/writing.test.jsx

Interfaces:
- Consumes: existing tasks, word-count logic, submitWriting, getWritingSubmissions, FloatingTutor, and SplitLearningWorkspace.
- Produces: Writing pane labels Đề bài and Bài viết; plain textarea editing; word count; save status IDLE | SAVING | SAVED | DIRTY | SAVE_FAILED; optional timer presentation; existing submission/estimate boundary.

- [ ] Step 1: Write failing tests for prompt-left/editor-right layout, Task 1/Task 2 selection, word count, editor focus, Writing 40/60 preference, no official-score claim, and text-only Tutor context.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/writing-workspace.test.jsx src/__tests__/writing.test.jsx; expected failure because WritingPage is not split and has no draft status.
- [ ] Step 3: Implement the split page using the existing accessible textarea, preserving the current submission API and Band ước lượng disclaimer.
- [ ] Step 4: Add optional timer presentation as a practice aid only; do not make it an official exam timer and do not let timer display alter submission/scoring behavior.
- [ ] Step 5: Run and verify GREEN with targeted Writing tests and existing Tutor tests.
- [ ] Step 6: Commit with git add frontend/src/components/workspace frontend/src/pages/WritingPage.jsx frontend/src/styles/globals.css frontend/src/__tests__/writing-workspace.test.jsx frontend/src/__tests__/writing.test.jsx && git commit -m "feat: add Writing learning workspace".

### Task 4: Add authenticated Writing draft persistence and stale-write protection

Files:
- Create: frontend/src/services/learningDraftsApi.js
- Create: backend/src/main/java/com/ieltsaitutor/learning/draft/LearningDraft.java
- Create: backend/src/main/java/com/ieltsaitutor/learning/draft/LearningDraftController.java
- Create: backend/src/main/java/com/ieltsaitutor/learning/draft/LearningDraftService.java
- Create: backend/src/main/java/com/ieltsaitutor/learning/draft/LearningDraftRepository.java
- Create: backend/src/main/java/com/ieltsaitutor/learning/draft/JdbcLearningDraftRepository.java
- Create: backend/src/test/java/com/ieltsaitutor/learning/draft/LearningDraftControllerTest.java
- Create: backend/src/test/java/com/ieltsaitutor/learning/draft/LearningDraftServiceTest.java
- Modify: frontend/src/components/workspace/DraftSaveStatus.jsx
- Modify: frontend/src/pages/WritingPage.jsx
- Test: frontend/src/__tests__/writing-drafts.test.jsx
- Plan-only migration target: backend/src/main/resources/db/migration/V9__create_learning_drafts.sql

Interfaces:
- Consumes: AuthPrincipal.userId(), taskId/referenceId, bounded plain text, and UI/UX-1A AuthProvider/preferences.
- Produces: GET /api/learning/drafts/current?skill=WRITING&referenceId=..., PUT /api/learning/drafts/{id}, DELETE /api/learning/drafts/{id}; draft version and status ACTIVE | SUBMITTED | EXPIRED | DELETED.

- [ ] Step 1: Write failing frontend/backend tests for authenticated ownership, bounded text, save debounce, version conflict, stale response rejection, logout clearing, expiry, delete, and non-blocking typing.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/writing-drafts.test.jsx and .\mvnw.cmd -Dtest=LearningDraftControllerTest,LearningDraftServiceTest test; expected missing API/service failures.
- [ ] Step 3: Define the plan-only V9 migration target with user_id ownership, skill/reference ID, bounded content_snapshot, monotonic version, status, timestamps, expiry, and indexes for current user/reference.
- [ ] Step 4: Implement the backend draft service/controller with principal-derived ownership, optimistic version checks, expiry handling, and normalized conflict/errors.
- [ ] Step 5: Implement frontend debounced autosave. Keep the latest text in the editor while requests settle; reject stale responses, display Đang lưu/Đã lưu/Chưa lưu/Không thể lưu, and clear account drafts on logout.
- [ ] Step 6: Run and verify GREEN with frontend and backend targeted tests; expected no typing block and no stale overwrite.
- [ ] Step 7: Commit with git add frontend/src/services/learningDraftsApi.js frontend/src/components/workspace/DraftSaveStatus.jsx frontend/src/pages/WritingPage.jsx frontend/src/__tests__/writing-drafts.test.jsx backend/src/main/java/com/ieltsaitutor/learning/draft backend/src/test/java/com/ieltsaitutor/learning/draft && git commit -m "feat: add safe Writing drafts".

### Task 5: Define normalized version-bound Tutor references

Files:
- Create: backend/src/main/java/com/ieltsaitutor/ai/dto/AiTutorReference.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatResponse.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/model/AiChatResult.java
- Create: frontend/src/features/tutor/tutorReferenceSchema.js
- Modify: frontend/src/services/aiTutorApi.js
- Test: backend/src/test/java/com/ieltsaitutor/ai/dto/AiTutorReferenceTest.java
- Test: frontend/src/__tests__/tutor-references.test.jsx

Interfaces:
- Consumes: current AiChatResponse status/answer/sources/grounding/meta contract and Writing draft version from Task 4.
- Produces: AiTutorReference fields referenceType, targetId, questionId, paragraphId, sentenceId, startOffset, endOffset, evidenceId, severity, label, contentVersion, draftVersion; normalized frontend reference records with the same allowlisted fields.

- [ ] Step 1: Write failing tests for immutable Reading stable-ID references, mutable Writing reference requiring contentVersion or draftVersion, bounded offsets, safe omission of irrelevant fields, and rejection of CSS selectors, XPath, HTML, scripts, arbitrary navigation, and cross-owner IDs.
- [ ] Step 2: Run and verify RED with backend and frontend targeted reference tests; expected missing DTO/schema failures.
- [ ] Step 3: Implement the additive normalized response field references without changing existing status/answer/grounding/source behavior. Keep provider adapters responsible for producing only the normalized model.
- [ ] Step 4: Implement frontend schema normalization and safe rejection. A Writing target with offsets resolves only when the current draft version equals the reference version.
- [ ] Step 5: Run and verify GREEN with targeted tests and existing Tutor/RAG regression tests.
- [ ] Step 6: Commit with git add backend/src/main/java/com/ieltsaitutor/ai/dto backend/src/main/java/com/ieltsaitutor/ai/model frontend/src/features/tutor frontend/src/services/aiTutorApi.js backend/src/test/java/com/ieltsaitutor/ai/dto frontend/src/__tests__/tutor-references.test.jsx && git commit -m "feat: add version-bound Tutor references".

### Task 6: Implement reference registry, resolver, and cross-highlighting

Files:
- Create: frontend/src/features/tutor/TutorReferenceRegistry.js
- Create: frontend/src/features/tutor/TutorReferenceResolver.js
- Create: frontend/src/components/tutor/CrossHighlightLayer.jsx
- Modify: frontend/src/components/workspace/ReadingPassagePane.jsx
- Modify: frontend/src/components/workspace/ReadingQuestionPane.jsx
- Modify: frontend/src/components/workspace/WritingEditorPane.jsx
- Modify: frontend/src/components/tutor/TutorMessage.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/cross-highlighting.test.jsx

Interfaces:
- Consumes: normalized AiTutorReference, current workspace target registry, current Writing draft version, and Tutor message rendering.
- Produces: TutorReferenceRegistry.register(target), unregister(targetId), resolve(reference), clear(); TutorReferenceResolver.resolve(reference, registry, workspaceState); CrossHighlightLayer preview/activate/clear behavior.

- [ ] Step 1: Write failing tests for Reading hover/focus preview, activation scroll/focus, Escape clear, non-color state, Writing exact-version resolution, stale message readability, and unknown/stale no-op behavior.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/cross-highlighting.test.jsx; expected missing registry/resolver/layer failures.
- [ ] Step 3: Implement the registry with stable application IDs and no DOM selector input. Register targets from the workspace panes, not from AI text.
- [ ] Step 4: Implement the resolver and layer. A valid target activates a persistent highlight, scrolls safely, and exposes icon/label/outline semantics; a stale target returns REFERENCE_STALE and never approximates an offset.
- [ ] Step 5: Add a learner-owned “Phân tích lại bản nháp” action only when the app can issue a new current-version analysis; do not create arbitrary generated links.
- [ ] Step 6: Run and verify GREEN with targeted cross-highlighting tests and accessibility tests.
- [ ] Step 7: Commit with git add frontend/src/features/tutor frontend/src/components/tutor frontend/src/components/workspace frontend/src/styles/globals.css frontend/src/__tests__/cross-highlighting.test.jsx && git commit -m "feat: resolve Tutor references safely".

### Task 7: Make Tutor request states truthful and bounded

Files:
- Create: frontend/src/features/tutor/tutorRequestState.js
- Modify: frontend/src/components/tutor/TutorShell.jsx
- Modify: frontend/src/components/tutor/TutorComposer.jsx
- Modify: frontend/src/components/tutor/TimeoutRetry.jsx
- Modify: frontend/src/services/aiTutorApi.js
- Test: frontend/src/__tests__/tutor-request-state.test.jsx

Interfaces:
- Consumes: POST /api/ai/chat request/response, AbortController, normalized errors, and SkeletonBlock.
- Produces: request states IDLE | PENDING_GENERIC | ANSWERED | INSUFFICIENT_CONTEXT | TIMEOUT | RATE_LIMITED | CANCELLED | UNAVAILABLE | ERROR; one generic pending copy for current HTTP transport.

- [ ] Step 1: Write failing tests for generic pending copy, no grammar/vocabulary/rubric fake progression, timeout, 429, cancellation, network failure, retry, unavailable, and final answer settlement.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/tutor-request-state.test.jsx; expected missing state reducer/behavior.
- [ ] Step 3: Implement a finite request-state reducer with a bounded timeout and AbortController cleanup. Use one SkeletonBlock/generic live message while HTTP is pending.
- [ ] Step 4: Keep PREPARING/PROCESSING/FINALIZING as future event-driven extension values only; do not implement SSE, fake timers, or client-generated stage transitions.
- [ ] Step 5: Run and verify GREEN with targeted request-state tests and all existing Tutor tests.
- [ ] Step 6: Commit with git add frontend/src/features/tutor frontend/src/components/tutor frontend/src/services/aiTutorApi.js frontend/src/__tests__/tutor-request-state.test.jsx && git commit -m "fix: make Tutor loading states truthful".

### Task 8: Add session continuity and mobile workspace integration

Files:
- Create: frontend/src/features/session/sessionContinuity.js
- Create: frontend/src/features/session/sessionStorage.js
- Modify: frontend/src/pages/PracticePage.jsx
- Modify: frontend/src/pages/WritingPage.jsx
- Modify: frontend/src/components/workspace/MobileWorkspaceTabs.jsx
- Modify: frontend/src/features/auth/AuthProvider.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/session-continuity.test.jsx
- Test: frontend/src/__tests__/mobile-workspace.test.jsx

Interfaces:
- Consumes: authenticated user identity, practice route/set/question IDs, draft IDs/versions, split ratios, active tabs, and Task 7 Tutor state.
- Produces: saveSessionSnapshot(snapshot), loadSessionSnapshot(userId), clearSessionSnapshot(userId), validateSessionSnapshot(snapshot, serverState); no stale Tutor context restoration.

- [ ] Step 1: Write failing tests for valid Reading/Writing restore, split ratio restore, active tab/scroll preservation, draft version restore, malformed snapshot, expired attachment, stale Tutor context exclusion, logout clearing, and unauthorized exercise discard.
- [ ] Step 2: Run and verify RED with the targeted continuity/mobile tests; expected missing session helpers and integration behavior.
- [ ] Step 3: Implement versioned, account-namespaced session storage containing only safe route/skill/set/question/draft/split/tab data. Do not store answer keys, tokens, raw private Tutor history, or expired attachments.
- [ ] Step 4: Revalidate the route and draft against server-owned state before restoration; discard invalid/unauthorized records and show a recoverable start-over state.
- [ ] Step 5: Preserve pane scroll positions and active mobile tabs without forcing a desktop split on phones. Clear account snapshots on logout.
- [ ] Step 6: Run and verify GREEN with targeted tests and all practice/writing/auth regressions.
- [ ] Step 7: Commit with git add frontend/src/features/session frontend/src/pages/PracticePage.jsx frontend/src/pages/WritingPage.jsx frontend/src/components/workspace frontend/src/features/auth/AuthProvider.jsx frontend/src/styles/globals.css frontend/src/__tests__/session-continuity.test.jsx frontend/src/__tests__/mobile-workspace.test.jsx && git commit -m "feat: restore safe learning sessions".

### Task 9: Whole-workspace verification

Files:
- Modify: frontend/src/pages/PracticePage.jsx
- Modify: frontend/src/pages/WritingPage.jsx
- Modify: frontend/src/components/tutor/TutorShell.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/practice.test.jsx
- Test: frontend/src/__tests__/writing.test.jsx
- Test: frontend/src/__tests__/tutor-rag.test.jsx
- Test: frontend/src/__tests__/rag-accessibility.test.jsx
- Test: backend/src/test/java/com/ieltsaitutor/ai/controller/AiChatControllerTest.java
- Test: backend/src/test/java/com/ieltsaitutor/auth/AuthControllerTest.java

Interfaces:
- Consumes: Tasks 1–8 contracts.
- Produces: verified Reading/Writing workspace, version-safe references, bounded Tutor requests, and mobile/accessibility behavior.

- [ ] Step 1: Write failing regression assertions for all five Review Focus items at the route level.
- [ ] Step 2: Run and verify RED before final integration fixes.
- [ ] Step 3: Make only integration fixes; do not redesign unrelated homepage or backend RAG behavior.
- [ ] Step 4: Run and verify GREEN with npm test -- --run, npm run lint, npm run build, .\mvnw.cmd test, .\mvnw.cmd package, and git diff --check.
- [ ] Step 5: Commit with git add frontend/src backend/src && git commit -m "feat: integrate smart learning workspace".

## Plan-level Verification

Run after all tasks:

- npm test -- --run
- npm run lint
- npm run build
- .\mvnw.cmd test
- .\mvnw.cmd package
- git diff --check

Do not run live provider calls. Do not implement SSE, AI Phase 2, or AI Phase 3.
