# Academic Luxury 2.0 — Learning Workspaces Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish Reading, Writing, and Speaking into calm, premium, exam-focused workspaces while preserving their trusted data and AI boundaries.

**Architecture:** Consume AL2-A semantic tokens and the existing split workspace, draft/version, reference resolver, session continuity, and Speaking local visualizer contracts. This plan changes presentation and interaction polish only; it does not change scoring, draft ownership, AI payloads, STT, or reference trust rules.

**Tech Stack:** React, Vite, CSS, Framer Motion, Lucide React, Vitest, Testing Library.

**Spec:** `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

## Global Constraints

- Dependency: AL2-A must be complete; use AL2-B Tutor surfaces where present.
- Preserve Reading split/divider/ratios/scroll/state rail and server-owned scoring.
- Preserve Writing drafts, autosave, word count, timer presentation, exact draft-version references, and `Band ước lượng`.
- Preserve Speaking local amplitude and text fallback; never add STT, transcript, pronunciation score, or audio band.
- Reading/Writing use tabs or stacked panes on narrow screens; Speaking becomes single-column.
- No fake AI Phase 2/3, fake vision, official IELTS claim, new dependency, or large framework.

## Review Focus

1. Writing editor must remain usable at 375px, including autosave and submit controls.
2. Reading/Writing split panes must remain readable at 1024px and preserve scroll/tab state.
3. A stale Writing reference must never highlight edited text after a draft version changes.
4. Denied microphone permission must leave the Speaking text path usable and truthful.
5. Reduced motion must stop animated Speaking effects without hiding timer/status meaning.

### Task 1: Refine Reading exam workspace presentation

**Files:**
- Modify: `frontend/src/pages/PracticePage.jsx`
- Modify: `frontend/src/components/workspace/ReadingPassagePane.jsx`
- Modify: `frontend/src/components/workspace/ReadingQuestionPane.jsx`
- Modify: `frontend/src/components/workspace/QuestionStateRail.jsx`
- Modify: `frontend/src/components/workspace/SplitLearningWorkspace.jsx`
- Modify: `frontend/src/components/workspace/workspace.css`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/reading-workspace.test.jsx`
- Test: `frontend/src/__tests__/practice.test.jsx`

**Interfaces:**
- Consumes: stable passage/paragraph IDs, server-owned questions, 40/50/60 divider, state rail, and current Tutor context.
- Produces: low-decoration Reading desk with readable passage typography, clear question hierarchy, review/submit navigation, and unchanged answer/flag/current semantics.

- [ ] **Step 1: Write failing tests** for theme-backed passage/editor surfaces, stable paragraph labels, question state text/icon signals, current focus, ratio control, independent scroll, and submit payload preservation.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/reading-workspace.test.jsx src/__tests__/practice.test.jsx`; expected failure for new visual/state assertions.
- [ ] **Step 3: Implement presentation-only changes** with semantic tokens and calm exam spacing; do not move answer keys or scoring into the browser.
- [ ] **Step 4: Run GREEN** and inspect Light/Dark long-passage behavior at 768/1024/1440.
- [ ] **Step 5: Commit** with `git add frontend/src/pages/PracticePage.jsx frontend/src/components/workspace frontend/src/styles/globals.css frontend/src/__tests__/reading-workspace.test.jsx frontend/src/__tests__/practice.test.jsx && git commit -m "feat: polish Reading exam workspace"`.

### Task 2: Refine Writing editor workspace and action bar

**Files:**
- Modify: `frontend/src/pages/WritingPage.jsx`
- Modify: `frontend/src/components/workspace/WritingPromptPane.jsx`
- Modify: `frontend/src/components/workspace/WritingEditorPane.jsx`
- Modify: `frontend/src/components/workspace/DraftSaveStatus.jsx`
- Modify: `frontend/src/components/workspace/SplitLearningWorkspace.jsx`
- Modify: `frontend/src/components/workspace/workspace.css`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/writing-workspace.test.jsx`
- Test: `frontend/src/__tests__/writing-drafts.test.jsx`
- Test: `frontend/src/__tests__/writing.test.jsx`

**Interfaces:**
- Consumes: `getCurrentDraft`, `saveDraft`, `deleteDraft`, `DraftSaveStatus`, task selection, `wordCount`, and `draftVersion`.
- Produces: editor-first Writing workspace with prompt/chart treatment, action bar for timer/word count/autosave/Tutor/submit, comfortable editor surface, and unchanged estimate boundary.

- [ ] **Step 1: Write failing tests** for 375px editor usability, Task 1/Task 2 selection, word count, status transitions, text-only Tutor context, timer-as-aid copy, and no official score claim.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/writing-workspace.test.jsx src/__tests__/writing-drafts.test.jsx src/__tests__/writing.test.jsx`.
- [ ] **Step 3: Implement the editorial Writing treatment** without changing draft API/version behavior or blocking typing while autosave settles.
- [ ] **Step 4: Run GREEN** and manually inspect Light Mode at 375/768/1024/1440.
- [ ] **Step 5: Commit** with `git add frontend/src/pages/WritingPage.jsx frontend/src/components/workspace frontend/src/styles/globals.css frontend/src/__tests__/writing-workspace.test.jsx frontend/src/__tests__/writing-drafts.test.jsx frontend/src/__tests__/writing.test.jsx && git commit -m "feat: polish Writing editor workspace"`.

### Task 3: Refine Speaking exam room without expanding capability

**Files:**
- Modify: `frontend/src/pages/SpeakingPage.jsx`
- Modify: `frontend/src/components/speaking/SpeakingRoom.jsx`
- Modify: `frontend/src/components/speaking/SpeakingOrb.jsx`
- Modify: `frontend/src/components/speaking/SpeakingTimer.jsx`
- Modify: `frontend/src/components/speaking/LocalAudioVisualizer.jsx`
- Modify: `frontend/src/components/speaking/MicrophonePermissionState.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/speaking-room.test.jsx`
- Test: `frontend/src/__tests__/speaking-timer.test.jsx`
- Test: `frontend/src/__tests__/local-audio-visualizer.test.jsx`
- Test: `frontend/src/__tests__/speaking.test.jsx`

**Interfaces:**
- Consumes: local mic permission/amplitude state, prompt selection, preparation/speaking timers, text fallback, and save attempt callback.
- Produces: immersive but truthful exam room with Part selector, cue, orb, timer, local visualizer, status, and text response path.

- [ ] **Step 1: Write failing tests** for room hierarchy, timer labels, permission denied fallback, no transcript/pronunciation/band copy, local visualizer semantics, reduced-motion behavior, and mobile single-column layout classes.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/speaking-room.test.jsx src/__tests__/speaking-timer.test.jsx src/__tests__/local-audio-visualizer.test.jsx src/__tests__/speaking.test.jsx`.
- [ ] **Step 3: Implement the visual room** with dark-first exam surface plus intentional Light support; keep amplitude local and never derive text/score from audio.
- [ ] **Step 4: Run GREEN** and manually verify denied permission and reduced motion at 375/768/1024/1440.
- [ ] **Step 5: Commit** with `git add frontend/src/pages/SpeakingPage.jsx frontend/src/components/speaking frontend/src/styles/globals.css frontend/src/__tests__/speaking-room.test.jsx frontend/src/__tests__/speaking-timer.test.jsx frontend/src/__tests__/local-audio-visualizer.test.jsx frontend/src/__tests__/speaking.test.jsx && git commit -m "feat: polish Speaking exam room"`.

### Task 4: Restyle safe Tutor references and cross-highlighting

**Files:**
- Modify: `frontend/src/features/tutor/TutorReferenceResolver.js`
- Modify: `frontend/src/features/tutor/TutorReferenceRegistry.js`
- Modify: `frontend/src/components/tutor/CrossHighlightLayer.jsx`
- Modify: `frontend/src/components/tutor/TutorMessage.jsx`
- Modify: `frontend/src/components/workspace/ReadingPassagePane.jsx`
- Modify: `frontend/src/components/workspace/WritingEditorPane.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/cross-highlighting.test.jsx`
- Test: `frontend/src/__tests__/tutor-references.test.jsx`

**Interfaces:**
- Consumes: normalized allowlisted references, registered application targets, current draft/content version, and stale resolver results.
- Produces: accessible preview/active/stale visual states with icon/label/outline semantics; no new selector, XPath, HTML, or arbitrary navigation input.

- [ ] **Step 1: Write failing tests** for Light/Dark reference chip contrast, keyboard activation, stale draft readability, Escape clear, unknown no-op, and unchanged exact-version resolution.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/cross-highlighting.test.jsx src/__tests__/tutor-references.test.jsx`.
- [ ] **Step 3: Implement presentation-only reference styling**; keep resolver trust boundary and exact version checks untouched.
- [ ] **Step 4: Run GREEN** with Tutor/RAG accessibility regressions and `npm run lint`.
- [ ] **Step 5: Commit** with `git add frontend/src/features/tutor frontend/src/components/tutor frontend/src/components/workspace frontend/src/styles/globals.css frontend/src/__tests__/cross-highlighting.test.jsx frontend/src/__tests__/tutor-references.test.jsx && git commit -m "feat: polish safe Tutor references"`.

### Task 5: Integrate mobile workspace and reduced-motion gate

**Files:**
- Modify: `frontend/src/components/workspace/MobileWorkspaceTabs.jsx`
- Modify: `frontend/src/components/workspace/workspace.css`
- Modify: `frontend/src/components/speaking/SpeakingRoom.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/mobile-workspace.test.jsx`
- Test: `frontend/src/__tests__/session-continuity.test.jsx`
- Test: `frontend/src/__tests__/speaking-room.test.jsx`

**Interfaces:**
- Consumes: active tab/scroll preservation, session snapshots, explicit reduced-motion token, and workspace breakpoint behavior.
- Produces: no-overflow responsive workspace behavior at 375/768/1024/1440 with state continuity.

- [ ] **Step 1: Write failing integration tests** for tab/state/scroll preservation, editor reachability, single-column Speaking, reduced-motion suppression, and no horizontal overflow assumptions.
- [ ] **Step 2: Run RED** with the focused mobile/session/Speaking suites.
- [ ] **Step 3: Make only integration CSS/state fixes**; do not alter draft/reference ownership.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, `npm run build`, and responsive checks in both themes.
- [ ] **Step 5: Commit** with `git add frontend/src/components/workspace frontend/src/components/speaking frontend/src/styles/globals.css frontend/src/__tests__ && git commit -m "feat: integrate responsive learning workspaces"`.

### Task 6: Learning-workspace regression gate

**Files:**
- Modify: `frontend/src/pages/PracticePage.jsx`
- Modify: `frontend/src/pages/WritingPage.jsx`
- Modify: `frontend/src/pages/SpeakingPage.jsx`
- Test: `frontend/src/__tests__/practice.test.jsx`
- Test: `frontend/src/__tests__/writing.test.jsx`
- Test: `frontend/src/__tests__/speaking.test.jsx`
- Test: `frontend/src/__tests__/cross-highlighting.test.jsx`

**Interfaces:**
- Consumes: Tasks 1–5 workspace contracts.
- Produces: route-level proof that Reading, Writing, Speaking, drafts, references, sessions, and Tutor context remain behaviorally unchanged.

- [ ] **Step 1: Write failing route regression assertions** for Reading submission, Writing draft/version, Speaking text fallback, stale reference, and mobile state boundaries.
- [ ] **Step 2: Run RED** before integration fixes.
- [ ] **Step 3: Make only confirmed presentation/integration fixes**.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, `npm run build`, and `git diff --check`.
- [ ] **Step 5: Commit** with `git add frontend/src && git commit -m "test: harden Academic Luxury workspace compatibility"`.

## Plan Verification

Run before AL2-E:

- `npm test -- --run`
- `npm run lint`
- `npm run build`
- `git diff --check`
