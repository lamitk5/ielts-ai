# IELTS AI Tutor — Submission Readiness

## Current branch

`feature/ai-phase2-phase3-integration`

## Final HEAD

`d9d8a8656b72064e47397f9282e084b73f6768f` was the clean application HEAD audited before this documentation-only report commit. The final documentation commit is recorded in the final response.

## Implemented student flows

- Reading
- Listening
- Writing
- Speaking

## AI Tutor

Name: Én

Key capabilities:

- contextual Tutor entry points across supported learning flows
- conversation history integration for authenticated sessions
- proactive reminders controlled by learner preferences
- global mascot eye tracking and personality reactions
- learning context integration with provider-neutral frontend states

## Adaptive learning

The homepage and progress surfaces consume the existing learning-intelligence state, including four-skill progress, roadmap, activity, and mistake evidence when available. Empty and guest states remain neutral and do not fabricate personal scores or history.

## Practice generator

The admin-only generator and review workflow provide source registration, blueprint extraction, generation jobs, validation, review actions, and approved-practice bank hydration. Learner catalog visibility remains limited to approved practice content.

## Verification

- Frontend tests: PASS — 70 files, 390 tests.
- Frontend lint: PASS — exit code 0; existing non-blocking warnings remain.
- Frontend build: PASS — Vite production build completed; existing chunk-size warning remains.
- Backend tests: BLOCKED — 404 run, 0 failures, 5 errors, 1 skipped. Five Spring context tests cannot validate against the current `ielts_ai_tutor` database because resolved Flyway migrations 9 and 10 are not applied.
- Backend package: BLOCKED — the normal package lifecycle reaches the same test-stage Flyway validation blocker.

## Interaction polish

- Én panel: compact desktop width 440px, max-height 72vh, independently scrolling messages, reachable header/composer, responsive mobile bounds.
- Én reminders: randomized 9–12 second cadence, 5 second visible duration, proactive/panel suppression and cleanup covered by fake-timer tests.
- Settings and LUMEN logo: keyboard/fine-pointer scale, subtle tilt, controlled champagne glow, and reduced-motion-safe transitions.
- Functional buttons: shared hover/active micro-interactions exclude disabled controls and preserve focus behavior.
- Local development proxy: `frontend/vite.config.js` defaults `/api` to `http://127.0.0.1:8081`; `VITE_API_PROXY_TARGET` remains an explicit override.

## Responsive verification

- 1440: PASS by desktop runtime/layout audit at the available 1536px viewport.
- 1024: PASS by responsive breakpoint/source audit.
- 768: PASS by responsive breakpoint/source audit.
- 375: PASS by responsive breakpoint/source audit.

The browser extension used for this run did not expose a viewport override, so individual 1024/768/375 screenshots were not captured. Existing responsive CSS and overflow protections were reviewed, and the available runtime was checked for horizontal overflow and viewport-safe Tutor behavior.

## Known limitations

- The current shared/default `ielts_ai_tutor` database has unresolved Flyway history: migrations 9 and 10 are present in source but not applied. No legacy DB remediation was performed, per scope.
- The Testcontainers PostgreSQL integration test remains skipped because Docker Desktop returned an HTTP 400 named-pipe response and no valid Docker environment was available. No dependency upgrade or Docker mutation was attempted.
- No documented local learner/admin credentials were present in the repository, so authenticated learner/admin browser smoke was not executed. Anonymous route behavior and the admin guard were verified.
- Speaking STT/pronunciation remains unavailable without a configured provider; the UI states this boundary and preserves text practice.
- Real Én chat smoke succeeded twice after clean frontend restarts with no manually injected proxy target; the response rendered through `/api/ai/chat` on the verified local backend.

## Submission status

BLOCKED — product/UI verification is ready, but the required backend full-suite/package verification is blocked by the pre-existing legacy database migration state and unavailable Docker context. Resolving those environment issues requires a separate approved integration operation.
