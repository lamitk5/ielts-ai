# LUMEN Mystical Portal Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Wrap the existing HeroSearch → `/practice/search?q=...` → real search API flow in a restrained Academic Luxury mystical portal without changing backend contracts or learner data.

**Architecture:** Keep `HeroSearch` as the owner of query, focus, validation, typing pulse, and navigation. Add one presentational `MysticalPortal` around the existing search controls; its decorative aura/rings are CSS-driven and non-interactive. Extend the existing `AmbientGoldenParticles` engine through a small mutable portal-target store so attraction and cursor repulsion share one RAF/DOM-variable loop without React state updates per frame. Keep `SearchPage` and `searchApi.js` as the owners of real results, loading, empty, and error behavior.

**Tech Stack:** React, React Router, Vitest, Testing Library, existing CSS variables/keyframes, existing `requestAnimationFrame` particle engine, no new dependency.

**Spec:** `docs/superpowers/specs/2026-09-30-lumen-mystical-portal-search-design.md`

## Global Constraints

- Preserve `HeroSearch` navigation to `/practice/search?q=<encoded trimmed query>`.
- Preserve `GET /api/practice/search?q=...` and the current result payload contract.
- Do not add backend, database, authentication, Tutor, Én, attachment, Groq, Gemini, Cloudflare, or RAG changes.
- Reuse the existing Academic Luxury navy, ivory, champagne/gold tokens and Playfair Display/Inter typography.
- Decorative portal and particle layers must use `aria-hidden="true"` and `pointer-events: none`.
- Reduced motion and Animation OFF must disable continuous portal motion, attraction, repulsion, shimmer, and typing pulse while preserving search functionality and a static focus treatment.
- Keep particle count bounded and use the existing RAF/CSS-variable approach; do not add Three.js, WebGL, canvas, or a second global pointer loop.
- Do not use fake results, fake recommendations, recent-search persistence, or provider-specific AI payloads.
- Preserve responsive behavior at 1440, 1024, 768, and 375 px with no horizontal overflow.
- The working tree already contains unrelated DEV upload-trace edits and `backend/backend/` QA artifacts; never delete, clean, stage, or commit them.
- Every commit must stage explicit portal-related paths only; never use `git add .` or `git clean -fd`.

## Review Focus

- Rapid focus → blur → focus must not leave a stale active class or stale portal target; owned by Task 1 tests for focus containment and cleanup.
- Escape during valid submit/result transition must close visual state without preventing the encoded navigation; owned by Task 1 and Task 5 route tests.
- Pointer movement through an active portal must combine forces without a local jump; cursor repulsion must win, velocity and displacement must remain bounded; owned by Task 3 tests.
- Reduced-motion or Animation OFF toggled while the portal is active must stop all moving effects and clear the target without disabling input/submit; owned by Task 6 tests.
- Empty or failed search while the portal is active must stay truthful, preserve status/empty semantics, and avoid leaving the visual transition stuck; owned by Task 5 and Task 7 regression tests.

## Locked file structure

The implementation uses the following existing paths and one focused new utility/component:

- Modify: `frontend/src/components/home/HeroSearch.jsx` — query, focus activation, Escape, typing pulse, and existing navigation.
- Create: `frontend/src/components/home/MysticalPortal.jsx` — presentational portal shell and decorative layers only.
- Create: `frontend/src/components/common/portalMotion.js` — mutable single-target store used by the existing particle loop; no React state.
- Modify: `frontend/src/components/common/AmbientGoldenParticles.jsx` — optional portal attraction, force priority, clamping, and cleanup.
- Modify: `frontend/src/pages/SearchPage.jsx` — preserve real API states and apply approved knowledge-card presentation classes only.
- Modify: `frontend/src/styles/globals.css` — portal aura/rings, typing pulse, result-card polish, responsive rules, theme variants, and reduced-motion rules.
- Create: `frontend/src/__tests__/mystical-portal.test.jsx` — portal activation, keyboard behavior, theme/motion smoke, and visual-layer contracts.
- Modify: `frontend/src/__tests__/search.test.jsx` — real API, navigation, loading/empty/error, and result accessibility coverage.
- Modify: `frontend/src/__tests__/ambient-golden-particles.test.jsx` — portal attraction/repulsion and bounded-motion coverage.
- Modify: `frontend/src/__tests__/hero.test.jsx` — homepage search regression if the existing hero test is the correct shared mount.
- Modify: `frontend/src/__tests__/preferences.test.jsx` only if the current preference test harness is needed to prove Animation OFF; do not change preference behavior.

No separate `PortalAura` or `KnowledgeResults` component is planned: aura/rings are decorative CSS in `MysticalPortal`, and `SearchPage` already has a clean result owner.

## Task 1: Portal activation state and HeroSearch focus behavior

**Files:**
- Create: `frontend/src/components/home/MysticalPortal.jsx`
- Modify: `frontend/src/components/home/HeroSearch.jsx`
- Test: `frontend/src/__tests__/mystical-portal.test.jsx`
- Regression: `frontend/src/__tests__/hero.test.jsx`

**Interfaces:**
- `MysticalPortal({ children, active, typing, submitting, reducedMotion, onEscape })` renders a focusable-boundary wrapper with `data-portal-state`, `data-portal-typing`, and `data-portal-motion` attributes. It does not own query or navigation.
- `HeroSearch` remains a no-prop component. It passes derived booleans to `MysticalPortal`, keeps its current submit logic, and exposes no new backend interface.

- [ ] **Step 1: Write failing tests** in `mystical-portal.test.jsx`:
  - render `HeroSearch` inside `MemoryRouter` with a location probe;
  - focus `#hero-search-input` and assert `[data-testid="mystical-portal"]` has `data-portal-state="active"`;
  - move focus between input, submit, and suggestion controls and assert the portal stays active while focus is inside its boundary;
  - press Escape while focused and assert the portal returns to `idle` and the input loses focus;
  - submit ` IELTS Writing Task 1 ` and assert the location is `/practice/search?q=IELTS%20Writing%20Task%201`;
  - submit whitespace and assert the location does not change and the existing Vietnamese alert appears.
- [ ] **Step 2: Run the focused RED test.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx`
  - Expected: FAIL because `MysticalPortal` and the portal state attributes do not exist.
- [ ] **Step 3: Implement the minimum state boundary.**
  - Add `MysticalPortal` with only decorative children and a `children` slot.
  - In `HeroSearch`, track `portalActive` from focus-within semantics, clear it when focus leaves the portal, and handle Escape by blurring the active control and clearing the flag.
  - Keep current trimming, validation copy, suggestion mapping, and React Router navigation unchanged.
- [ ] **Step 4: Run GREEN and regression tests.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx src/__tests__/hero.test.jsx`
  - Expected: PASS; existing hero behavior remains green.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/components/home/MysticalPortal.jsx frontend/src/components/home/HeroSearch.jsx frontend/src/__tests__/mystical-portal.test.jsx frontend/src/__tests__/hero.test.jsx`
  - Commit: `git commit -m "feat(search): add mystical portal activation state"`

## Task 2: Portal visual shell

**Files:**
- Modify: `frontend/src/components/home/MysticalPortal.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/mystical-portal.test.jsx`

**Interfaces:**
- `MysticalPortal) keeps the Task 1 props and adds stable decorative descendants with classes `mystical-portal-aura`, `mystical-portal-ring`, `mystical-portal-vignette`, and `mystical-portal-content`.
- CSS consumes existing `--surface-*`, `--text-*`, `--gold`, `--gold-light`, `--accent-soft`, `--focus`, `--shadow-*`, and `--motion-duration` tokens. New `--portal-*` variables are scoped to `.mystical-portal`.

- [ ] **Step 1: Write failing visual-contract tests.**
  - Assert the decorative descendants are present and `aria-hidden="true"`.
  - Assert the portal root and each decorative layer have `pointer-events: none` where appropriate; search controls remain reachable.
  - Set `data-theme="dark"` and assert the root exposes the dark portal state; set `data-theme="light"` and assert the light variant class/data state is present.
  - Assert `data-portal-state="active"` changes the state attribute without moving focus away from the input.
- [ ] **Step 2: Run RED.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx`
  - Expected: FAIL because the aura/ring/vignette structure and theme state are not implemented.
- [ ] **Step 3: Implement the shell and CSS.**
  - Add layered pseudo-elements/spans for aura, rings, and vignette; keep them non-interactive and outside the control DOM order.
  - Add a restrained focus ring, champagne edge light, radial aura, and bounded overflow. Use transform/opacity only for motion.
  - Add dark navy/ivory/champagne rules and light ivory/navy/antique-gold rules using theme tokens, not hard-coded dark-only text.
  - Use the spec timing range: focus ring approximately 240–520 ms; aura cycle approximately 8–14 s. Prefer existing motion tokens when they are present.
- [ ] **Step 4: Run GREEN.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/components/home/MysticalPortal.jsx frontend/src/styles/globals.css frontend/src/__tests__/mystical-portal.test.jsx`
  - Commit: `git commit -m "feat(search): add academic luxury portal shell"`

## Task 3: Ambient particle portal attraction

**Files:**
- Create: `frontend/src/components/common/portalMotion.js`
- Modify: `frontend/src/components/home/MysticalPortal.jsx`
- Modify: `frontend/src/components/common/AmbientGoldenParticles.jsx`
- Test: `frontend/src/__tests__/ambient-golden-particles.test.jsx`
- Test: `frontend/src/__tests__/mystical-portal.test.jsx`

**Interfaces:**
- `setPortalMotionTarget({ x, y, radius })) updates the single mutable target; `x), `y), and `radius) are viewport pixels and positive radius.
- `clearPortalMotionTarget()` marks the target inactive and resets coordinates.
- `getPortalMotionTarget()` returns `{ active: boolean, x: number, y: number, radius: number }` without causing a React render.
- `MysticalPortal) measures its bounded frame when active, publishes the center/radius, and clears it on close/unmount. Resize/scroll updates are debounced through one RAF.
- `AmbientGoldenParticles) keeps its existing props (none), reads the mutable target inside its existing motion loop, and writes only `--repel-x`/`--repel-y` CSS variables.

- [ ] **Step 1: Write failing particle tests.**
  - Activate the portal and assert the motion store becomes active with finite target coordinates.
  - Dispatch a pointer move near a particle and assert the resulting offset points away from the pointer, not toward the portal.
  - Dispatch a pointer move outside the repulsion radius and assert portal attraction can produce a non-zero bounded offset.
  - Assert every offset has magnitude at most the chosen maximum displacement (24 px).
  - Assert clearing the portal target returns particles to normal drift/zero DOM offset.
  - Spy on React render of the particle layer and assert pointer/RAF updates do not trigger a React state render.
- [ ] **Step 2: Run RED.**
  - Run: `cd frontend; npm test -- --run src/__tests__/ambient-golden-particles.test.jsx`
  - Expected: FAIL because no portal target store or attraction path exists.
- [ ] **Step 3: Implement one bounded motion loop.**
  - Add the mutable store with no subscriber-driven React state.
  - Extend the existing particle refs with mutable `offset`/`velocity` records. While animation is allowed and the portal or pointer is active, use one RAF loop to integrate attraction and repulsion, clamp velocity and displacement, and write CSS variables.
  - Give cursor repulsion priority locally: if a particle is inside the pointer repulsion radius, apply repulsion and suppress portal attraction for that particle; otherwise apply a small portal attraction toward the target.
  - Preserve the existing CSS drift as the inactive baseline, touch disabling, pointer cleanup, reduced-motion cleanup, and no layout writes.
  - Do not change particle count.
- [ ] **Step 4: Run GREEN and portal regression.**
  - Run: `cd frontend; npm test -- --run src/__tests__/ambient-golden-particles.test.jsx src/__tests__/mystical-portal.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/components/common/portalMotion.js frontend/src/components/common/AmbientGoldenParticles.jsx frontend/src/components/home/MysticalPortal.jsx frontend/src/__tests__/ambient-golden-particles.test.jsx frontend/src/__tests__/mystical-portal.test.jsx`
  - Commit: `git commit -m "feat(search): connect portal to ambient particles"`

## Task 4: Bounded typing pulse

**Files:**
- Modify: `frontend/src/components/home/HeroSearch.jsx`
- Modify: `frontend/src/components/home/MysticalPortal.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/mystical-portal.test.jsx`

**Interfaces:**
- `HeroSearch) sets `typing=true` on a non-empty input change and clears it with one managed timeout of approximately 280 ms; the timeout is replaced on the next keystroke and cleared on unmount.
- `MysticalPortal) consumes `typing) and exposes `data-portal-typing="active|idle"`.
- CSS uses one bounded `portal-typing-pulse` animation and never creates one particle or timer per keystroke.

- [ ] **Step 1: Write failing tests.**
  - Type three characters quickly and assert one active typing state, not three stacked nodes/timers.
  - Advance fake timers and assert the state returns to idle after the bounded pulse.
  - Assert reduced-motion and Animation OFF keep the portal static and do not apply the pulse class.
  - Unmount during typing and assert timers are cleared.
- [ ] **Step 2: Run RED.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx`
  - Expected: FAIL because typing state and pulse are absent.
- [ ] **Step 3: Implement the minimal debounced pulse.**
  - Add a single ref-backed timeout in `HeroSearch`; keep query updates and submit behavior unchanged.
  - Pass the derived flag to `MysticalPortal`; add a CSS-only, low-amplitude border/aura pulse. Do not alter particle data or create a React update loop.
- [ ] **Step 4: Run GREEN.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/components/home/HeroSearch.jsx frontend/src/components/home/MysticalPortal.jsx frontend/src/styles/globals.css frontend/src/__tests__/mystical-portal.test.jsx`
  - Commit: `git commit -m "feat(search): add bounded portal typing pulse"`

## Task 5: Real search results presentation

**Files:**
- Modify: `frontend/src/pages/SearchPage.jsx`
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/__tests__/search.test.jsx`

**Interfaces:**
- `searchPractice(query)` remains unchanged and remains the only data source.
- `SearchPage) keeps its current `query`, `loading`, `results`, and safe fallback behavior. It may add `data-search-state="loading|results|empty"` for styling/tests.
- Each result card continues to consume the existing result shape: `{ id, title, skill, description, route }`; no duplicate data is introduced.

- [ ] **Step 1: Write failing route/result tests.**
  - Mock `/api/practice/search` with a real array and assert the result title, skill, description, and supplied route render.
  - Hold the fetch promise and assert `Đang tìm bài luyện tập…` is exposed as `role="status"`.
  - Resolve an empty array and assert the truthful empty message with no result card.
  - Reject a non-writing query and assert no fabricated successful result appears.
  - Assert result title/action links are keyboard reachable and retain supplied hrefs.
  - Assert a query with spaces and Vietnamese characters is encoded once at the service boundary.
- [ ] **Step 2: Run RED.**
  - Run: `cd frontend; npm test -- --run src/__tests__/search.test.jsx`
  - Expected: FAIL only for the new state/accessibility/presentation assertions.
- [ ] **Step 3: Implement the approved knowledge-card treatment without changing data flow.**
  - Keep shared `GlassCard), add semantic state attributes/classes, and tighten hierarchy for skill, title, description, and action.
  - Keep loading and empty copy/status semantics. Do not add fake data or a new service.
  - Add subtle result-card transform/opacity transitions only when motion is allowed.
- [ ] **Step 4: Run GREEN and hero navigation regression.**
  - Run: `cd frontend; npm test -- --run src/__tests__/search.test.jsx src/__tests__/hero.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/pages/SearchPage.jsx frontend/src/styles/globals.css frontend/src/__tests__/search.test.jsx`
  - Commit: `git commit -m "feat(search): polish real knowledge result cards"`

## Task 6: Reduced motion, Animation OFF, themes, and mobile simplification

**Files:**
- Modify: `frontend/src/components/home/MysticalPortal.jsx`
- Modify: `frontend/src/components/common/AmbientGoldenParticles.jsx`
- Modify: `frontend/src/styles/globals.css`
- Modify: `frontend/src/__tests__/mystical-portal.test.jsx`
- Modify: `frontend/src/__tests__/ambient-golden-particles.test.jsx`
- Modify: `frontend/src/__tests__/preferences.test.jsx` only if needed for the existing preference harness

**Interfaces:**
- `MysticalPortal) derives `reducedMotion) from the existing optional preference context/system state and passes only a boolean visual flag.
- The existing `PreferenceProvider) remains unchanged; `preferences.reduceMotion === 'reduce') and system `prefers-reduced-motion` remain the sources of truth.
- The existing `data-pointer="coarse"`, `data-reduced-motion`, and theme attributes remain compatible.

- [ ] **Step 1: Write failing motion/responsive tests.**
  - With `writeGuestPreferences({ reduceMotion: 'reduce' })), assert the portal exposes static motion state and the particle layer has no active attraction/repulsion loop.
  - Toggle the existing Animation setting to OFF and assert the same static result while input, Escape, submit, and quick suggestions remain usable.
  - Set a coarse-pointer media query and assert the portal uses simplified static effects.
  - Assert light theme exposes visible antique-gold/bronze treatment and dark theme exposes champagne/navy treatment.
  - Assert portal decorative elements remain non-interactive and no fixed-width style causes horizontal overflow at the 375 layout contract.
- [ ] **Step 2: Run RED.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx src/__tests__/ambient-golden-particles.test.jsx`
  - Expected: FAIL for the new portal motion/theme/coarse-pointer assertions.
- [ ] **Step 3: Implement static fallbacks and responsive rules.**
  - Stop/clear the mutable portal target and RAF when reduced motion, Animation OFF, or coarse pointer is active.
  - Keep a static focus ring/aura and preserve all keyboard/touch search actions.
  - Add `@media (prefers-reduced-motion: reduce)` and `:root[data-reduced-motion='true']` rules for portal/typing/result transitions.
  - Add mobile simplification at the existing 760 px breakpoint; confirm 375 layout stacks controls without overflow.
- [ ] **Step 4: Run GREEN.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx src/__tests__/ambient-golden-particles.test.jsx src/__tests__/preferences.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Commit the task.**
  - Run: `git add frontend/src/components/home/MysticalPortal.jsx frontend/src/components/common/AmbientGoldenParticles.jsx frontend/src/styles/globals.css frontend/src/__tests__/mystical-portal.test.jsx frontend/src/__tests__/ambient-golden-particles.test.jsx frontend/src/__tests__/preferences.test.jsx`
  - Commit: `git commit -m "feat(search): respect motion and responsive portal states"`

## Task 7: Full integration and regression verification

**Files:**
- Modify only files proven necessary by Tasks 1–6.
- Tests: `frontend/src/__tests__/mystical-portal.test.jsx`, `frontend/src/__tests__/search.test.jsx`, `frontend/src/__tests__/hero.test.jsx`, `frontend/src/__tests__/ambient-golden-particles.test.jsx`, `frontend/src/__tests__/academic-luxury-2-accessibility.test.jsx`, `frontend/src/__tests__/shell-regressions.test.jsx`.

**Interfaces:**
- No new product interface. This task consumes the completed portal, motion store, search route, and existing preference/theme contracts.
- No backend or runtime data changes.

- [ ] **Step 1: Write failing integration assertions for the five Review Focus cases.**
  - Rapid focus/blur/focus clears and re-establishes exactly one active target.
  - Escape during submit does not change the encoded route.
  - Combined pointer/portal forces remain clamped and repulsion wins locally.
  - Motion preference changed while active clears moving state but leaves controls usable.
  - Empty and failed search leave truthful status/empty content and no stuck submitting state.
- [ ] **Step 2: Run RED against the focused integration tests.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx src/__tests__/search.test.jsx src/__tests__/ambient-golden-particles.test.jsx`
  - Expected: FAIL before the final integration assertions are implemented.
- [ ] **Step 3: Make only the smallest integration corrections.**
  - Reconcile stale event/timer cleanup, route state, or class contracts found by the tests. Do not add unrelated behavior and do not touch backend or Én/upload files.
- [ ] **Step 4: Run focused GREEN verification.**
  - Run: `cd frontend; npm test -- --run src/__tests__/mystical-portal.test.jsx src/__tests__/search.test.jsx src/__tests__/hero.test.jsx src/__tests__/ambient-golden-particles.test.jsx src/__tests__/academic-luxury-2-accessibility.test.jsx src/__tests__/shell-regressions.test.jsx`
  - Expected: PASS.
- [ ] **Step 5: Run fresh full frontend verification.**
  - Run: `cd frontend; npm test -- --run; npm run lint; npm run build`
  - Expected: all tests PASS, lint PASS, build PASS.
- [ ] **Step 6: Perform manual responsive and runtime verification.**
  - Check 1440, 1024, 768, and 375 px in dark/light themes; keyboard search; Enter/Space/Escape; direct search URL; API loading/results/empty/failure; reduced motion; Animation OFF; coarse pointer; no horizontal overflow; no overlap with HeroVisual, Hero CTA, header, footer, or Én.
  - Confirm no unexpected network requests beyond the existing search API and no console errors.
- [ ] **Step 7: Run repository diff verification without touching unrelated files.**
  - Run from repository root: `git diff --check` and `git status --short`.
  - Expected: no whitespace errors; only intended portal commits plus the pre-existing upload-trace/QA paths remain.
- [ ] **Step 8: Commit only if integration fixes were needed.**
  - Stage explicit portal files only.
  - Commit: `git commit -m "fix(search): finalize mystical portal integration"`
  - If no integration changes were needed, do not create an empty commit.

## Final verification and handoff

After the last portal commit, rerun:

- `git diff --check`
- `git status --short`
- `git log --oneline -12`

The implementer must report the final HEAD, exact test/lint/build output, responsive/theme checks, and any real limitations. The pre-existing DEV upload trace and `backend/backend/` artifacts must remain unmodified and unstaged.

