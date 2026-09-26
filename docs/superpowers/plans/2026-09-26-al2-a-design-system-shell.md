# Academic Luxury 2.0 — Design System & Product Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the first-class Academic Luxury 2.0 theme, typography, spacing, shared surface patterns, navigation, settings presentation, and accessibility foundation.

**Architecture:** Extend `PreferenceProvider` and `globals.css` with canonical semantic tokens while retaining compatibility aliases for existing consumers. Keep preference persistence, account isolation, and reduced-motion behavior in the existing provider. Reuse `Button`, `GlassCard`, `SectionTitle`, and the existing Settings drawer instead of introducing a parallel design system.

**Tech Stack:** React, Vite, CSS, Framer Motion, Lucide React, Vitest, Testing Library.

**Spec:** `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

## Global Constraints

- Academic Luxury 2.0 uses semantic tokens only for theme surfaces and state roles.
- Light Mode and Dark Mode are both first-class; Light Mode is warm ivory/cream, never a grey patch.
- Keep Playfair Display for editorial headings and Inter for body/UI/forms/data.
- Preserve auth, preferences, account isolation, reduced motion, keyboard access, and route behavior.
- Do not add a 3D engine, large animation library, or unnecessary dependency.
- Do not implement AI Phase 2/3, STT, fake vision, official IELTS scoring, or provider routing.
- Every production-changing task follows RED → minimal implementation → GREEN → commit.

## Review Focus

1. Repeated Light/Dark/System switching must remove stale token values and update every shared surface.
2. Every accent preset must keep Light-mode text, border, selected, status, and focus roles readable.
3. Closing Settings must remove the overlay, restore body scrolling, restore background inertness, and return focus.
4. Account preference isolation must survive logout and account switching without guest/account merging.
5. 200% text scaling must keep navigation, settings controls, and touch targets usable without horizontal overflow.

### Task 1: Migrate to canonical semantic theme tokens

**Files:**
- Modify: `frontend/src/features/preferences/PreferenceProvider.jsx`
- Modify: `frontend/src/features/preferences/preferenceSchema.js`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/preferences.test.jsx`
- Test: `frontend/src/__tests__/preferences-persistence.test.jsx`

**Interfaces:**
- Consumes: `DEFAULT_PREFERENCES`, `normalizePreferences`, `applyPreferenceTokens`, and existing preference persistence.
- Produces: canonical roles `--bg-page`, `--bg-section`, `--surface-1`, `--surface-2`, `--surface-elevated`, `--surface-interactive`, `--text-primary`, `--text-secondary`, `--text-muted`, `--text-inverse`, `--border-subtle`, `--border-strong`, `--accent`, `--accent-hover`, `--accent-soft`, `--focus-ring`, `--success`, `--warning`, `--danger`, `--shadow-sm`, `--shadow-md`, `--shadow-lg`, `--overlay`, and `--glass-bg`, plus compatibility aliases for current consumers.

- [ ] **Step 1: Write the failing tests** for all six accent presets in Light/Dark, System switching, role-specific token presence, no stale values after repeated switching, and preserved persisted preference behavior.
- [ ] **Step 2: Run the tests to verify RED**: `npm test -- --run src/__tests__/preferences.test.jsx src/__tests__/preferences-persistence.test.jsx`; expected failure for missing canonical roles and new theme matrix assertions.
- [ ] **Step 3: Implement the minimal token adapter** in `PreferenceProvider.jsx` and `globals.css`; keep theme selection in one place, map status/focus/selection roles independently, and retain aliases until consumers migrate.
- [ ] **Step 4: Run the targeted tests to verify GREEN** and confirm account-scoped persistence tests still pass.
- [ ] **Step 5: Commit** with `git add frontend/src/features/preferences frontend/src/styles/globals.css frontend/src/__tests__/preferences.test.jsx frontend/src/__tests__/preferences-persistence.test.jsx && git commit -m "feat: establish Academic Luxury semantic tokens"`.

### Task 2: Define typography, spacing, grid, and limited shared surfaces

**Files:**
- Create: `frontend/src/components/common/PageContainer.jsx`
- Create: `frontend/src/components/common/Surface.jsx`
- Modify: `frontend/src/components/common/Button.jsx`
- Modify: `frontend/src/components/common/GlassCard.jsx`
- Modify: `frontend/src/components/common/SectionTitle.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/shared-ui.test.jsx`

**Interfaces:**
- Consumes: canonical theme tokens and existing shared component APIs.
- Produces: `PageContainer({ children, className })` and `Surface({ as, variant, children, className })`; `variant` is a small allowlist such as `base | elevated | interactive`, not an accent/theme name.

- [ ] **Step 1: Write failing tests** for semantic surface variants, heading/body/label scale, responsive page gutters, focus-visible Button behavior, and no hardcoded theme surface dependency in the new primitives.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/shared-ui.test.jsx`; expected failure because the bounded surface primitives and token-backed assertions do not exist.
- [ ] **Step 3: Implement the minimum reusable layer**: add the page container and surface variant only where repeated layout/surface rules justify it; define responsive type scale, line-height, spacing scale, max width, gutters, card gaps, and section rhythm in CSS.
- [ ] **Step 4: Run GREEN**: `npm test -- --run src/__tests__/shared-ui.test.jsx` and `npm run lint`; verify the existing primitives retain their public props and reduced-motion behavior.
- [ ] **Step 5: Commit** with `git add frontend/src/components/common frontend/src/styles/globals.css frontend/src/__tests__/shared-ui.test.jsx && git commit -m "feat: add Academic Luxury layout primitives"`.

### Task 3: Refine header, mobile navigation, and account presentation

**Files:**
- Modify: `frontend/src/components/layout/Navbar.jsx`
- Modify: `frontend/src/components/layout/AppLayout.jsx`
- Modify: `frontend/src/components/layout/Footer.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/app.test.jsx`
- Test: `frontend/src/__tests__/auth.test.jsx`

**Interfaces:**
- Consumes: `useAuth`, `useLocation`, `SettingsButton`, `SettingsDrawer`, existing route links, and `PageContainer`/token roles.
- Produces: compact profile/account menu behavior with logout inside the menu, accessible mobile navigation, unchanged route/hash links, and no duplicated auth state.

- [ ] **Step 1: Write failing tests** for desktop links, authenticated profile menu/logout, guest login action, mobile menu open/close/focus, hash anchors, and no oversized logout pill as the primary authenticated control.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/app.test.jsx src/__tests__/auth.test.jsx`; expected failure for the new profile/menu semantics.
- [ ] **Step 3: Implement the minimal shell refinement** using existing auth and settings providers; preserve logout cleanup, route destinations, focus-visible states, and body overflow behavior.
- [ ] **Step 4: Run GREEN** with the targeted tests and `npm run lint`; confirm `/`, `/assessment`, practice routes, login, and settings remain reachable.
- [ ] **Step 5: Commit** with `git add frontend/src/components/layout frontend/src/styles/globals.css frontend/src/__tests__/app.test.jsx frontend/src/__tests__/auth.test.jsx && git commit -m "feat: refine Academic Luxury product shell"`.

### Task 4: Redesign Settings as a premium personalization surface

**Files:**
- Modify: `frontend/src/components/settings/SettingsDrawer.jsx`
- Modify: `frontend/src/components/settings/PreferenceControlGroup.jsx`
- Modify: `frontend/src/components/settings/SettingsButton.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/settings-drawer.test.jsx`
- Test: `frontend/src/__tests__/preferences.test.jsx`

**Interfaces:**
- Consumes: `usePreferences()` values/update/retry/status, `useAuth()`, existing dialog focus/inert behavior, and semantic tokens.
- Produces: labeled groups for Giao diện, Ngôn ngữ, Phông chữ, Mật độ, Chuyển động, Trợ giảng AI, Quyền riêng tư, and Đặt lại; visual Light/Dark/System previews; immediate apply/autosave status.

- [ ] **Step 1: Write failing tests** for theme preview cards, all six accents, motion/font/density controls, status copy, reset confirmation, background inertness, focus return, Escape, mobile sheet semantics, and account isolation.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/settings-drawer.test.jsx src/__tests__/preferences.test.jsx`; expected failure for the reorganized control hierarchy and visual preview assertions.
- [ ] **Step 3: Implement the drawer presentation** without changing persistence semantics: use semantic surfaces, keep controls keyboard reachable, preserve immediate apply/autosave, and make reset confirmation receive focus.
- [ ] **Step 4: Run GREEN** with targeted tests plus `npm run lint`; manually inspect Light/Dark at 200% text scale for clipping and horizontal overflow.
- [ ] **Step 5: Commit** with `git add frontend/src/components/settings frontend/src/styles/globals.css frontend/src/__tests__/settings-drawer.test.jsx frontend/src/__tests__/preferences.test.jsx && git commit -m "feat: redesign Academic Luxury settings"`.

### Task 5: Add the shell accessibility and theme matrix gate

**Files:**
- Modify: `frontend/src/App.jsx`
- Modify: `frontend/src/components/layout/AppLayout.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/rag-accessibility.test.jsx`
- Test: `frontend/src/__tests__/shared-ui.test.jsx`
- Test: `frontend/src/__tests__/app.test.jsx`

**Interfaces:**
- Consumes: Tasks 1–4 token, primitive, navigation, and settings contracts.
- Produces: verified shell-level theme switching, reduced-motion, focus-visible, landmark, 200% text, and overflow behavior without new product logic.

- [ ] **Step 1: Write failing integration assertions** for repeated theme switching, reduced-motion data state, landmarks, focus-visible selectors, settings overlay cleanup, and 200% text scaling layout assumptions.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/app.test.jsx src/__tests__/shared-ui.test.jsx src/__tests__/rag-accessibility.test.jsx`; expected failure for the new matrix coverage.
- [ ] **Step 3: Make only shell/token integration fixes**; do not redesign homepage, Tutor, or workspaces in this task.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, and `npm run build`; manually inspect 375/768/1024/1440 in Light/Dark/System.
- [ ] **Step 5: Commit** with `git add frontend/src frontend/src/__tests__ && git commit -m "test: harden Academic Luxury shell foundations"`.

## Plan Verification

Run after AL2-A before starting AL2-B:

- `npm test -- --run`
- `npm run lint`
- `npm run build`
- `git diff --check`
