# UI/UX-1B Task 1 Report

## Scope delivered

- Added `SplitLearningWorkspace`, `WorkspaceDivider`, `WorkspacePresetControls`, and `MobileWorkspaceTabs` as shared Reading/Writing layout primitives.
- Added the workspace preference adapter, reusing 1A's `usePreferences()` API and the existing `readingSplitRatio` / `writingSplitRatio` keys.
- Exported the canonical 40/50/60 workspace ratio presets from the preference schema and reused them in normalization, controls, divider behavior, and persistence.
- Added responsive, independent scroll panes; pointer resizing; separator keyboard controls; mounted mobile tab panels; reduced-motion-safe styling; and accessible focus/selection states.
- Did not integrate Reading or Writing pages or implement later 1B/Phase 2 work.

## Verification evidence

- RED: `npm test -- --run src/__tests__/split-workspace.test.jsx` failed because the workspace modules did not yet exist (`Failed to resolve import ... WorkspaceDivider`). Vitest reported 0 tests collected, the expected missing-component failure before implementation.
- GREEN: the same targeted command passed: 1 test file, 9 tests passed.
- Targeted lint: `npx oxlint src/components/workspace src/features/workspace/workspacePreferences.js src/features/preferences/preferenceSchema.js src/__tests__/split-workspace.test.jsx` completed with no warnings or errors.
- `git diff --check` completed successfully.

## Review notes

- Mobile tab changes keep both pane and caller-owned content nodes mounted, preserving their state and scroll position.
- The desktop layout is used at 1024px and above. Smaller viewports and the explicit `mobileMode` use real labeled tabs.
- Split ratios stay within 40, 50, or 60 percent for the left pane. Keyboard Arrow keys move between presets; Home/End select the bounds.
- Full frontend/backend suites were not run because this task requested targeted verification only.

## Review fix round

- Cleared drag-only grid ratios on pointer release, pointer cancellation, and unmount. The controlled preference ratio again drives the grid after a preset or keyboard change.
- Pointer movement now reads the grid bounds once at drag start and coalesces visual writes into one `requestAnimationFrame`; pending frames are cancelled on release/cancel/unmount.
- Mobile panes use viewport-bounded, independently scrollable regions. Each pane's scroll offset is stored while active and restored on return; scroll events from hidden panes cannot overwrite the saved offset.
- Desktop panes now expose labeled `region` semantics and do not carry mobile `tabpanel`/`hidden` state. Mobile semantics and tab hiding apply only when the workspace is in tab mode.
- RED: the first expanded targeted run failed 5 tests covering desktop semantics, absent RAF batching, drag reconciliation, cancellation, and unbounded mobile scrollports.
- A stronger unmount assertion then exposed that React detaches the separator ref before effect cleanup; retaining the grid ref through the drag fixed clearing an already-painted transient ratio on unmount. Cancellation and unmount tests now cover both an applied visual frame and a subsequent pending frame.
- GREEN: `npm test -- --run src/__tests__/split-workspace.test.jsx` passed 15/15.
- Targeted oxlint completed without warnings or errors. `git diff --check` passed.
- `npm run build` succeeded. Vite still reports the existing 774 kB minified JavaScript chunk warning (>500 kB).
- Browser layout check with long passage content in forced mobile-tab mode confirmed an independently scrolling passage pane. After scrolling to paragraphs 30–36, switching to Questions, and returning, the passage resumed at paragraphs 30–36. The host browser remained 1280×720; the mobile mode was forced through the workspace's existing `mobileMode` prop. The temporary preview page was removed afterward.
- No Reading/Writing page integration or later 1B work was added.
