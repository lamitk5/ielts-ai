# Academic Luxury 2.0 — Homepage, Dashboard & Tutor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the homepage, member dashboard presentation, and Tutor normal/fullscreen experience into a premium but truthful learner shell.

**Architecture:** Consume AL2-A tokens, primitives, navigation, and Settings contracts. Keep homepage data in `homepageMockData.js` only where it is explicitly non-personal, and keep member progress from existing server/member mock adapters. Extend `TutorShell` and its existing provider-neutral state rather than creating another chat model.

**Tech Stack:** React, Vite, CSS, Framer Motion, Lucide React, Vitest, Testing Library.

**Spec:** `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

## Global Constraints

- Dependency: AL2-A must be complete and reviewed first.
- No fake Phase 2 weaknesses, roadmap, Today's Focus, mistakes, or recommendations.
- Preserve Hero, four equal skills, current routes, auth state, and honest guest/member boundaries.
- Tutor has exactly one fullscreen/restore control and one close control.
- Keep `TutorMessageSkeleton`, source chips, grounding, retry/cancel, AuthGate, and attachment lifecycle contracts.
- Upload type alignment belongs to AL2-D; this plan may refine presentation but must not claim backend image support early.
- No AI Phase 2/3, fake vision, STT, official IELTS claims, or live provider calls.

## Review Focus

1. Guest homepage/dashboard must never render personal band, exam, mistakes, or roadmap data.
2. Member sections must use centralized existing data and preserve four-skill equality.
3. Tutor composer and cancel/send controls must remain reachable with an attachment at mobile height.
4. Fullscreen and restore must be the same toggle, never two competing resize controls.
5. Contextual quick actions must not appear when their context is unavailable.

### Task 1: Establish homepage visual hierarchy and premium section rhythm

**Files:**
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/components/home/HeroSection.jsx`
- Modify: `frontend/src/components/home/HeroVisual.jsx`
- Modify: `frontend/src/components/home/HeroSearch.jsx`
- Modify: `frontend/src/components/home/CTASection.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/hero.test.jsx`
- Test: `frontend/src/__tests__/app.test.jsx`

**Interfaces:**
- Consumes: existing hero search/route behavior, `HeroVisual`, shared `Button`, semantic tokens, and homepage mock data.
- Produces: a concise editorial hero with correct CTA order, no guest/debug label, balanced section spacing, and preserved `/assessment`, `/#skills`, and `/practice/search` behavior.

- [ ] **Step 1: Write failing tests** for exact hero copy/CTA order, guest/member differences, no technical labels, hero search route, and semantic heading hierarchy.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/hero.test.jsx src/__tests__/app.test.jsx`; expected failure for new hierarchy and token-backed layout assertions.
- [ ] **Step 3: Implement minimal hierarchy polish**: keep `HeroVisual` concept, tighten oversized rhythm, preserve natural headline wrapping, and use only semantic tokens.
- [ ] **Step 4: Run GREEN** with targeted tests and inspect 375/768/1024/1440 for CTA visibility and no overflow.
- [ ] **Step 5: Commit** with `git add frontend/src/pages/HomePage.jsx frontend/src/components/home frontend/src/styles/globals.css frontend/src/__tests__/hero.test.jsx frontend/src/__tests__/app.test.jsx && git commit -m "feat: refine Academic Luxury homepage hierarchy"`.

### Task 2: Refine equal four-skill cards and non-personal empty states

**Files:**
- Modify: `frontend/src/components/home/SkillsSection.jsx`
- Modify: `frontend/src/components/home/SkillCard.jsx`
- Modify: `frontend/src/components/home/ProgressOverviewSection.jsx`
- Modify: `frontend/src/components/home/CommonMistakesWidget.jsx`
- Modify: `frontend/src/components/home/ExamCountdownCard.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/skills.test.jsx`
- Test: `frontend/src/__tests__/progress.test.jsx`
- Test: `frontend/src/__tests__/learning-dashboard-cards.test.jsx`

**Interfaces:**
- Consumes: centralized `skillCards`, existing progress/member/guest adapters, `SkeletonBlock`, and shared card/button primitives.
- Produces: equal Reading/Listening/Writing/Speaking card hierarchy, restrained card density, and a truthful guest progress preview without fake numbers/bands/dates.

- [ ] **Step 1: Write failing tests** for equal card structure/routes, guest empty-state copy, no fake personal metrics, member four-skill presence, and no Writing-default visual marker.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/skills.test.jsx src/__tests__/progress.test.jsx src/__tests__/learning-dashboard-cards.test.jsx`; expected failure for visual/state assertions.
- [ ] **Step 3: Implement the card and progress treatment** with fewer pills, calmer surfaces, 2×2 desktop grid, responsive one-column mobile, and adapter-backed empty states.
- [ ] **Step 4: Run GREEN** and verify `/` and `/?demo=member` in both themes at required widths.
- [ ] **Step 5: Commit** with `git add frontend/src/components/home frontend/src/styles/globals.css frontend/src/__tests__/skills.test.jsx frontend/src/__tests__/progress.test.jsx frontend/src/__tests__/learning-dashboard-cards.test.jsx && git commit -m "feat: polish skill and learning overview surfaces"`.

### Task 3: Refine member dashboard hierarchy and activity presentation

**Files:**
- Modify: `frontend/src/components/home/ProgressOverviewSection.jsx`
- Modify: `frontend/src/components/home/CommonMistakesWidget.jsx`
- Modify: `frontend/src/components/home/ExamCountdownCard.jsx`
- Modify: `frontend/src/components/home/TutorPreviewSection.jsx`
- Modify: `frontend/src/data/homepageMockData.js`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/progress.test.jsx`
- Test: `frontend/src/__tests__/learning-intelligence-state.test.jsx`
- Test: `frontend/src/__tests__/streak-rules.test.jsx`
- Test: `frontend/src/__tests__/skill-energy.test.jsx`

**Interfaces:**
- Consumes: `getMemberProgress`, learning-intelligence adapter states, centralized mistakes/streak rules, and existing `/?demo=member` query behavior.
- Produces: Continue Learning, Today's Focus, four-skill progress, Roadmap, Common Mistakes, Streak, and Recent Activity zones that render only real data or explicit empty states.

- [ ] **Step 1: Write failing tests** for section order, adapter-unavailable empty states, centralized mistakes, non-negative countdown, meaningful streak logic, and no fabricated personalized values.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/progress.test.jsx src/__tests__/learning-intelligence-state.test.jsx src/__tests__/streak-rules.test.jsx src/__tests__/skill-energy.test.jsx`.
- [ ] **Step 3: Implement presentation-only hierarchy**; keep existing data logic, labels such as `Band ước lượng`, and equal four-skill treatment.
- [ ] **Step 4: Run GREEN** with the targeted suite and verify member/guest route transitions do not leak state.
- [ ] **Step 5: Commit** with `git add frontend/src/components/home frontend/src/data/homepageMockData.js frontend/src/styles/globals.css frontend/src/__tests__ && git commit -m "feat: refine learner dashboard hierarchy"`.

### Task 4: Refine Tutor normal panel and compact attachment presentation

**Files:**
- Modify: `frontend/src/components/tutor/TutorShell.jsx`
- Modify: `frontend/src/components/tutor/TutorPanel.jsx`
- Modify: `frontend/src/components/tutor/TutorComposer.jsx`
- Modify: `frontend/src/components/tutor/AttachmentComposer.jsx`
- Modify: `frontend/src/components/tutor/AttachmentStatus.jsx`
- Modify: `frontend/src/components/tutor/TutorMessage.jsx`
- Modify: `frontend/src/components/tutor/TutorMessageList.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/tutor-shell.test.jsx`
- Test: `frontend/src/__tests__/tutor-attachments.test.jsx`
- Test: `frontend/src/__tests__/tutor.test.jsx`

**Interfaces:**
- Consumes: existing shell states, context badge, normalized messages, `SourceChip`, `TutorMessageSkeleton`, `AttachmentStatus`, and attachment callbacks.
- Produces: one fullscreen/restore toggle, X close, compact attachment row, readable bubbles, contextual badges/actions, and a viewport-safe composer without changing AI payloads.

- [ ] **Step 1: Write failing tests** for exactly one resize control, close, context/source hierarchy, attachment chip/thumbnail presentation, composer reachability, SkeletonBlock loading, cancel/retry, and no provider-specific UI.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/tutor-shell.test.jsx src/__tests__/tutor-attachments.test.jsx src/__tests__/tutor.test.jsx`.
- [ ] **Step 3: Implement minimal visual composition** using existing components and semantic tokens; keep the message body independently scrollable and input/send controls inside `100dvh` bounds.
- [ ] **Step 4: Run GREEN** and manually inspect normal Tutor in Light/Dark at 375/768/1024/1440.
- [ ] **Step 5: Commit** with `git add frontend/src/components/tutor frontend/src/styles/globals.css frontend/src/__tests__/tutor-shell.test.jsx frontend/src/__tests__/tutor-attachments.test.jsx frontend/src/__tests__/tutor.test.jsx && git commit -m "feat: refine Tutor companion panel"`.

### Task 5: Add Tutor fullscreen workspace and contextual actions

**Files:**
- Modify: `frontend/src/components/tutor/TutorShell.jsx`
- Modify: `frontend/src/components/tutor/TutorQuickActions.jsx`
- Modify: `frontend/src/components/tutor/ContextBadge.jsx`
- Modify: `frontend/src/components/tutor/FloatingTutor.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/tutor-shell.test.jsx`
- Test: `frontend/src/__tests__/tutor-rag.test.jsx`
- Test: `frontend/src/__tests__/rag-accessibility.test.jsx`

**Interfaces:**
- Consumes: `SHELL_STATES`, trusted context/reference data, normalized request states, and existing FloatingTutor state ownership.
- Produces: `STANDARD`, `EXPANDED`, and mobile fullscreen layouts with an optional trusted-context pane; contextual quick actions only when valid; unchanged close/focus/retry semantics.

- [ ] **Step 1: Write failing tests** for normal↔fullscreen round trip, one toggle label, mobile fullscreen, optional context pane, stale context badge, valid/invalid quick actions, Escape, focus return, and no horizontal overflow.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/tutor-shell.test.jsx src/__tests__/tutor-rag.test.jsx src/__tests__/rag-accessibility.test.jsx`.
- [ ] **Step 3: Implement the fullscreen layout** without a second conversation store, fake stages, or arbitrary context rendering; keep the composer visible.
- [ ] **Step 4: Run GREEN** and manually verify 375/768/1024/1440 in both themes.
- [ ] **Step 5: Commit** with `git add frontend/src/components/tutor frontend/src/styles/globals.css frontend/src/__tests__/tutor-shell.test.jsx frontend/src/__tests__/tutor-rag.test.jsx frontend/src/__tests__/rag-accessibility.test.jsx && git commit -m "feat: add Tutor study workspace presentation"`.

### Task 6: Integrate homepage/dashboard/Tutor regression gate

**Files:**
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/components/home/TutorPreviewSection.jsx`
- Modify: `frontend/src/components/tutor/FloatingTutor.jsx`
- Test: `frontend/src/__tests__/hero.test.jsx`
- Test: `frontend/src/__tests__/progress.test.jsx`
- Test: `frontend/src/__tests__/tutor.test.jsx`
- Test: `frontend/src/__tests__/app.test.jsx`

**Interfaces:**
- Consumes: AL2-A shell and AL2-B Tasks 1–5 contracts.
- Produces: verified guest/member homepage, truthful dashboard, and Tutor entry with no future-phase data leakage.

- [ ] **Step 1: Write failing route-level assertions** for guest/member transitions, CTA/hash navigation, Tutor open/fullscreen/close, no fake data, and empty history.
- [ ] **Step 2: Run RED** with the focused homepage/dashboard/Tutor suites.
- [ ] **Step 3: Make only integration fixes**; do not alter server data or AI/RAG behavior.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, `npm run build`, and responsive checks at 375/768/1024/1440.
- [ ] **Step 5: Commit** with `git add frontend/src && git commit -m "feat: integrate Academic Luxury learner shell"`.

## Plan Verification

Run before AL2-C/AL2-D:

- `npm test -- --run`
- `npm run lint`
- `npm run build`
- `git diff --check`
