# Academic Luxury 2.0 — Verification & Compatibility Hardening Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the Academic Luxury 2.0 redesign does not regress existing auth, learning, Tutor, attachment, accessibility, or truthful-capability behavior.

**Architecture:** This plan owns verification and narrowly scoped integration fixes only. It does not add product features, new routes, migrations, providers, or fake data. Tests exercise existing contracts at component, route, backend, responsive, accessibility, and security boundaries.

**Tech Stack:** Vitest, Testing Library, Vite, Oxlint, Maven/JUnit/MockMvc, existing browser-capable QA tooling.

**Spec:** `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

## Global Constraints

- Dependencies AL2-A, AL2-B, AL2-C, and AL2-D must be complete and reviewed.
- No production feature work except a confirmed regression fix proven by a failing test.
- Do not consume live Groq, Cloudflare, or Gemini quota.
- Do not add AI Phase 2/3, STT, fake vision, official IELTS scoring, migrations, or packages.
- Final checks cover Dark, Light, System and widths 375, 768, 1024, and 1440+.

## Review Focus

1. Theme switching must not leave stale styles, focus rings, overlays, or reduced-motion state.
2. Responsive Tutor and attachment composer must never clip input/send/retry/remove controls.
3. Reading/Writing session, draft, reference, and scroll/tab continuity must survive route transitions.
4. Guest/member/account ownership boundaries must not leak personal data or attachments.
5. Speaking remains local amplitude/text-only and no AI capability claim appears.

### Task 1: Build the frontend regression and route matrix

**Files:**
- Create: `frontend/src/__tests__/academic-luxury-2-regression.test.jsx`
- Modify: `frontend/src/__tests__/app.test.jsx`
- Modify: `frontend/src/__tests__/hero.test.jsx`
- Modify: `frontend/src/__tests__/progress.test.jsx`
- Modify: `frontend/src/__tests__/tutor.test.jsx`
- Modify: `frontend/src/__tests__/reading-workspace.test.jsx`
- Modify: `frontend/src/__tests__/writing-workspace.test.jsx`
- Modify: `frontend/src/__tests__/speaking-room.test.jsx`

**Interfaces:**
- Consumes: final AL2-A/B/C contracts and existing route components.
- Produces: one-shot route assertions for Home, Settings, Tutor normal/fullscreen, Dashboard/member, Reading, Writing, Speaking, Login/AuthGate, and unknown-route behavior.

- [ ] **Step 1: Write failing regression assertions** for the critical routes, exact CTA/navigation behavior, theme state, no fake data, and no future-phase claims.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/academic-luxury-2-regression.test.jsx`; expected failure until final integration behavior is pinned.
- [ ] **Step 3: Make only confirmed integration fixes** in the owning component; keep route/API contracts unchanged.
- [ ] **Step 4: Run GREEN** with `npm test -- --run` and `npm run lint`.
- [ ] **Step 5: Commit** with `git add frontend/src && git commit -m "test: add Academic Luxury route regression matrix"`.

### Task 2: Run theme, responsive, and accessibility matrix

**Files:**
- Create: `frontend/src/__tests__/academic-luxury-2-accessibility.test.jsx`
- Modify: `frontend/src/__tests__/shared-ui.test.jsx`
- Modify: `frontend/src/__tests__/rag-accessibility.test.jsx`
- Modify: `frontend/src/__tests__/mobile-workspace.test.jsx`
- Modify: `frontend/src/styles/globals.css` only for confirmed failures

**Interfaces:**
- Consumes: semantic tokens, Settings dialog, Tutor shell, split workspaces, Speaking room, and reduced-motion state.
- Produces: evidence for Dark/Light/System at 375/768/1024/1440+, 200% text, keyboard/focus/contrast semantics, no overflow, and reduced motion.

- [ ] **Step 1: Write failing matrix assertions** for theme parity, focus return, dialog inertness, divider/tabs, Tutor composer reachability, Speaking mobile layout, and no horizontal overflow.
- [ ] **Step 2: Run RED** with the new matrix and existing accessibility/mobile suites.
- [ ] **Step 3: Apply only minimal confirmed CSS/component fixes**; each fix must have a failing assertion or captured visual defect.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, `npm run build`; manually capture 375/768/1024/1440 in Dark and Light.
- [ ] **Step 5: Commit** with `git add frontend/src && git commit -m "test: verify Academic Luxury responsive accessibility"`.

### Task 3: Run backend/security/contract compatibility matrix

**Files:**
- Create: `backend/src/test/java/com/ieltsaitutor/compatibility/AcademicLuxuryCompatibilityTest.java`
- Modify: `backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java`
- Modify: `backend/src/test/java/com/ieltsaitutor/ai/controller/AiChatControllerTest.java`
- Modify: `backend/src/test/java/com/ieltsaitutor/auth/AuthControllerTest.java`
- Modify: `backend/src/test/java/com/ieltsaitutor/practice/PracticeControllerTest.java`

**Interfaces:**
- Consumes: existing auth, practice, Tutor, attachment, draft, and reference response contracts.
- Produces: proof that ownership, normalized errors, no provider leakage, server-owned scores, attachment allowlist, and optional-credential startup remain intact.

- [ ] **Step 1: Write failing compatibility assertions** for guest AuthGate/backend 401, account isolation, JPG/WEBP upload, stale draft/reference behavior, practice answer-key absence, and no live provider dependency.
- [ ] **Step 2: Run RED**: `.\mvnw.cmd -Dtest=AcademicLuxuryCompatibilityTest,TutorAttachmentControllerTest,AiChatControllerTest,AuthControllerTest,PracticeControllerTest test`.
- [ ] **Step 3: Apply only confirmed backend compatibility fixes**; do not alter unrelated AI/RAG internals.
- [ ] **Step 4: Run GREEN** with the targeted Maven suite and full backend suite.
- [ ] **Step 5: Commit** with `git add backend/src/test && git commit -m "test: harden Academic Luxury backend compatibility"`.

### Task 4: Final whole-branch quality gate and review

**Files:**
- Modify: `frontend/src/__tests__/academic-luxury-2-regression.test.jsx` only for confirmed final assertions
- Modify: `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md` only if implementation evidence reveals a contradiction

**Interfaces:**
- Consumes: reviewed AL2-A through AL2-D commits and all verification artifacts.
- Produces: final evidence package; no new product behavior.

- [ ] **Step 1: Write any missing failing final-gate assertion** discovered from the matrix; do not add speculative scope.
- [ ] **Step 2: Run RED** for that assertion, or record that no additional assertion is needed if the matrix is complete.
- [ ] **Step 3: Make only a confirmed regression fix**, with a focused diff and task review.
- [ ] **Step 4: Run GREEN and the complete gate**: `npm test -- --run`, `npm run lint`, `npm run build`, `.\mvnw.cmd test`, `.\mvnw.cmd package`, and `git diff --check`.
- [ ] **Step 5: Commit** with `git add frontend/src backend/src && git commit -m "test: complete Academic Luxury compatibility hardening"` only if code/test changes were required; otherwise leave no extra product commit.

## Final Verification Matrix

Required screens: Home, Settings, Tutor normal, Tutor fullscreen, Dashboard/member, Reading, Writing, Speaking, Login/AuthGate.

Required themes: Dark, Light, System.

Required widths: 375, 768, 1024, 1440+.

Required commands:

- `npm test -- --run`
- `npm run lint`
- `npm run build`
- `.\mvnw.cmd test`
- `.\mvnw.cmd package`
- `git diff --check`
- `git status --short`
