# Task 2 implementation report

Implemented guest and authenticated preference persistence on `feature/ui-ux-phase-1` only.

## Changes

- Versioned, allowlisted guest preferences in local storage; malformed, old-version, and storage-error records fall back safely.
- Account-namespaced cache holds confirmed server preferences for startup rendering. GET hydration is authoritative and never merges guest settings. PUT sends the full allowlisted record and expected version. Edits preview immediately, debounce for 300 ms, serialize saves, and accept the returned server record.
- Provider exposes `idle | loading | saving | synced | unsynced | conflict`, the last confirmed preferences, and retry. Conflict refetches server state. Save failure keeps the preview and confirmed value separate.
- Logout and account switch clear the previous account preference cache. Authenticated subtree remounts on identity change, clearing in-memory Tutor and page/session state. Guest preferences remain.

## TDD evidence

- Initial RED: `npm test -- --run src/__tests__/preferences-persistence.test.jsx src/__tests__/auth.test.jsx` — 8 failed, 3 passed. Missing storage functions, provider hydration/status, and logout cache cleanup failed as expected.
- GREEN after minimal implementation: same command — 11 passed.
- Review RED for overlapping saves: targeted persistence run — 1 failed, 7 passed; second PUT started while the first was in flight.
- Review GREEN: targeted persistence + auth run — 12 passed.
- Account-switch RED: targeted auth run — 1 failed, 4 passed; old account cache remained.
- Final GREEN: targeted persistence + auth run — 13 passed; full frontend suite — 105 passed across 18 files; build passed; lint exited 0 with warnings.

## Self-review and concerns

- Checked that cache writes use only normalized preferences and that server responses cannot overwrite another identity after unmount.
- Task 3 has not implemented `/api/user/preferences`; authenticated hydration will report `unsynced` until that endpoint exists. Task 4 owns the visible Vietnamese status and retry controls.
- Lint still reports warnings, including effect-state and Fast Refresh warnings in the preference provider; no lint errors. The build reports the existing large-chunk advisory.

## Fix round — 2026-09-26

Addressed all findings in `task-2-review.md`, limited to Task 2 files.

- Authenticated edits remain dirty while the initial GET is pending. A successful GET becomes the confirmed base and the newest edit is saved against its version. If hydration fails while an edit is pending, hydration retries automatically and then saves that edit.
- Retry and save completions check the initiating account identity, request epoch, and mounted provider before mutating state or cache. Late GET results after logout or account switch are ignored.
- If an in-flight save fails after a newer edit was made, the newest snapshot is queued again after the request settles.
- Added an explicit browser-to-wire preference enum adapter using the planned uppercase server enums (`COMFORTABLE`, `REDUCED`, `ALLOWED`, etc.). Successful server records must contain every valid preference and a nonnegative integer version.
- Guest and account cache records are discarded when any required preference field is malformed.
- Regression coverage exercises delayed and failed hydration, stale retry after logout/account switch, a newer edit after failed PUT, uppercase wire mapping, malformed successful server responses, and malformed local records.

### Verification

- Initial regression run: targeted persistence/auth command failed on the newly added cases as expected (13 failing tests, 8 passing).
- Targeted after fix: `npm test -- --run src/__tests__/preferences-persistence.test.jsx src/__tests__/auth.test.jsx` — 21 passed.
- Full frontend suite: `npm test -- --run` — 113 passed across 18 files.
- `npm run lint` — exit 0; reports warnings, including React hook/ref warnings in `PreferenceProvider.jsx` and pre-existing warnings in other files.
- `npm run build` — exit 0; Vite reports its existing large-chunk advisory.
- `git diff --check` — passed.

Self-review confirmed the change is confined to preference persistence, its API/schema/storage adapters, their regression tests, and this report. No later task or backend behavior was implemented.
