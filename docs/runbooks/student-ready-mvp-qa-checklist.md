# Student-Ready MVP QA Checklist

Use an explicit approved QA database clone. Never run migration rehearsal, data repair, reset, drop, truncate, or history edits against a shared development or production database.

## Backend

- [ ] Verify `git status --short` is clean before the run.
- [ ] Verify the configured QA database name and redact credentials from logs.
- [ ] Run `./mvnw.cmd test` and record any pre-existing Flyway or Testcontainers blocker.
- [ ] Run `./mvnw.cmd package`.
- [ ] Start, stop, and restart the backend against the approved QA clone.
- [ ] Confirm catalog publications, learner attempts/results, Speaking/Writing truthful boundaries, Phase 2 events, Tutor history, and authenticated preferences survive restart.
- [ ] Confirm inactive publications are absent from learner catalog responses.

## Learner

- [ ] Sign in as a learner and open the approved catalog.
- [ ] Open Reading and Listening, resume, save answers, submit, and reload the result.
- [ ] Open Writing, save a response, and verify provider-unavailable feedback is explicit without a fabricated band.
- [ ] Open Speaking, save manual transcript input, and verify missing STT is explicit without a fabricated transcript or score.
- [ ] Ask Tutor a generic question and a trusted contextual question; verify loading terminates and no answer keys or raw provider errors appear.

## Admin

- [ ] Sign in as an administrator.
- [ ] Generate, validate, review, approve, and publish a practice set.
- [ ] Restart the backend and verify the approved publication remains visible to learners.
- [ ] Confirm a learner cannot access admin generator/review APIs or shell routes.

## Frontend and accessibility

- [ ] Run `npm test -- --run`, `npm run lint`, and `npm run build`.
- [ ] Review 375, 768, 1024, and 1440 pixel layouts in dark and light themes.
- [ ] Check one H1 per public page, keyboard reachability, focus-visible states, reduced motion, no horizontal overflow, and no clipped Tutor composer controls.
- [ ] Confirm no live provider calls are required for deterministic Reading/Listening acceptance.
