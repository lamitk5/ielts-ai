# Phase 4D — Speaking Submission and Review Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

**Goal:** Provide a safe Speaking submission and manual-review path without fabricating STT, pronunciation, fluency, or accent scores.

**Architecture:** Speaking uses the 4A canonical submission and stores prompt/version, timing, transcript source, and a server-owned audio reference. The provider boundary remains an extension point for future STT; V1 supports real recording/upload, manual transcript, text-only fallback, playback, and review.

**Tech Stack:** Spring Boot, Java 21, JDBC/PostgreSQL, existing `SpeechToTextProvider`, filesystem/storage abstraction, multipart validation, JUnit 5, React/Vite.

**Spec:** `docs/superpowers/specs/2026-10-01-phase-4-learning-assessment-design.md`

**Global Constraints:** No fake STT or audio analysis. Audio is never addressed by arbitrary user filename/path. Every media read, transcript, submission, and review is owner/role authorized; tests use local fixtures and provider mocks.

**Review Focus:**

1. No transcript is invented when no STT/manual transcript exists.
2. User A cannot access User B's audio.
3. Unsafe audio/file type is rejected.
4. Manual reviewer results remain separate from future AI/STT metadata.
5. Failed upload does not corrupt the submission lifecycle.

### Task 1: Map Speaking prompts and timing state to 4A submissions

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingSubmission.java`, `SpeakingSubmissionState.java`, `SpeakingPromptVersion.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingAttempt.java`, `SpeakingService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingSubmissionStateTest.java`

**Interfaces:**
- Consumes: canonical submission ID, prompt/version, preparation/response timestamps, transcript source
- Produces: safe `IN_PROGRESS`, `SUBMITTED`, `PENDING_REVIEW`, `GRADED`, or `FAILED` state with timing metadata

- [ ] Step 1: Write failing tests for prompt version pinning, preparation/response timing, text-only submission, and legal state transitions.
- [ ] Step 2: Run focused tests and verify RED because the current Speaking record has no canonical version/timing lifecycle.
- [ ] Step 3: Add the smallest domain mapping and delegate lifecycle authority to 4A.
- [ ] Step 4: Run Speaking and submission tests and verify GREEN.
- [ ] Step 5: Commit with `feat(speaking): map submissions to canonical lifecycle`.

### Task 2: Add safe audio validation and storage references

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingAudioStorage.java`, `SpeakingAudioValidator.java`, `SpeakingAudioReference.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/ai/attachment/FileSystemTutorAttachmentStorage.java` only if a generic safe storage primitive is intentionally reused; otherwise keep Speaking storage separate
- Create: one additive migration for audio metadata, allocated from the execution base
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingAudioValidatorTest.java`, `SpeakingAudioStorageTest.java`

**Interfaces:**
- Consumes: multipart audio, authenticated owner, configured MIME/size/duration limits
- Produces: server-generated storage key, validated metadata, or safe rejection; never an executable/path-derived file

- [ ] Step 1: Write failing tests for supported audio MIME, extension mismatch, size/duration limit, empty/malformed content, and path traversal.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement allow-list validation and owner-keyed storage abstraction with cleanup on failure.
- [ ] Step 4: Run storage/validation tests and verify GREEN.
- [ ] Step 5: Commit with `feat(speaking): add safe audio storage boundary`.

### Task 3: Implement upload, playback, and ownership-protected media APIs

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingAudioController.java`, DTOs, and media error mapping
- Modify: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingController.java`
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingAudioControllerSecurityTest.java`

**Interfaces:**
- Consumes: owner-authenticated upload/playback requests and canonical submission ID
- Produces: upload status/reference, authorized stream, and no media access for another learner or unprivileged route

- [ ] Step 1: Write failing MVC/security tests for owner access, User A/User B isolation, invalid media, missing submission, and failed storage cleanup.
- [ ] Step 2: Run focused tests and verify RED.
- [ ] Step 3: Implement thin controller over validator/storage/service; do not expose filesystem paths.
- [ ] Step 4: Run controller/security tests and verify GREEN.
- [ ] Step 5: Commit with `feat(speaking): expose protected audio endpoints`.

### Task 4: Add transcript source and future STT extension contract

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/TranscriptSource.java`, `SpeakingTranscriptService.java`, `SpeechToTextResult.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/speaking/SpeechToTextProvider.java`, `UnavailableSpeechToTextProvider.java`, `SpeakingService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingTranscriptBoundaryTest.java`

**Interfaces:**
- Consumes: manual text or a real configured `SpeechToTextProvider` result
- Produces: transcript plus source/provider/model/version/confidence when real; explicit unavailable state otherwise

- [ ] Step 1: Write failing tests proving empty audio creates no transcript, manual text is labeled, and unavailable STT is not treated as success.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement the source metadata boundary and keep the current unavailable provider honest.
- [ ] Step 4: Run Speaking provider tests and verify GREEN.
- [ ] Step 5: Commit with `feat(speaking): formalize transcript source boundary`.

### Task 5: Add manual review with separate reviewer records

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingReview.java`, `SpeakingReviewRepository.java`, `JdbcSpeakingReviewRepository.java`, `SpeakingReviewController.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/auth/UserRole.java` only if the existing admin/teacher role contract requires a named reviewer capability
- Create: one additive migration for review/audit records
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingReviewSecurityTest.java`, `SpeakingReviewServiceTest.java`

**Interfaces:**
- Consumes: approved reviewer, owned speaking submission, manual criteria/comment, review version
- Produces: `PENDING_REVIEW` → `GRADED` manual result, separate from AI/STT metadata, with reviewer/timestamp audit

- [ ] Step 1: Write failing tests for admin-only review, duplicate review conflict, separate human fields, and learner result visibility.
- [ ] Step 2: Run tests and verify RED.
- [ ] Step 3: Implement service/repository/controller with explicit role and transition checks.
- [ ] Step 4: Run security/service tests and verify GREEN.
- [ ] Step 5: Commit with `feat(speaking): add audited manual review`.

### Task 6: Update Speaking workspace and review states

**Files:**
- Modify: `frontend/src/pages/SpeakingPage.jsx`, `frontend/src/features/speaking/speakingApi.js`, `frontend/src/components/speaking/SpeakingRoom.jsx`, `frontend/src/components/speaking/SpeakingPromptCard.jsx`, `frontend/src/components/speaking/LocalAudioVisualizer.jsx`
- Create: `frontend/src/components/results/SpeakingResultShell.jsx`
- Test: `frontend/src/__tests__/speaking-submission-review.test.jsx`

**Interfaces:**
- Consumes: prompt/timer/upload/submission/review status and transcript source DTOs
- Produces: accessible preparation/response controls, truthful audio/text fallback, playback, pending/manual result, and no fake score controls

- [ ] Step 1: Write failing UI/API tests for timer states, upload failure, text-only submission, unavailable STT, pending review, and keyboard controls.
- [ ] Step 2: Run targeted Vitest and verify RED.
- [ ] Step 3: Implement the smallest UI change using existing workspace components and reduced-motion conventions.
- [ ] Step 4: Run Speaking and workspace regressions and verify GREEN.
- [ ] Step 5: Commit with `feat(ui): add speaking submission review states`.

### Task 7: Verify the safe Speaking flow end to end

**Files:**
- Modify: only 4D files needed by acceptance failures
- Test: `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingAttemptEndToEndTest.java`, `frontend/src/__tests__/speaking-attempt-flow.test.jsx`

**Interfaces:**
- Consumes: local supported audio fixture, manual transcript fixture, unavailable STT provider, two authenticated users
- Produces: start → record/upload → submit → playback/review with complete ownership and no invented analysis

- [ ] Step 1: Write failing acceptance coverage for failed upload recovery, User A/User B media isolation, and no-STT behavior.
- [ ] Step 2: Run acceptance tests and verify RED before final wiring.
- [ ] Step 3: Make minimal integration fixes only.
- [ ] Step 4: Run focused backend/frontend tests and verify GREEN.
- [ ] Step 5: Commit with `test(speaking): verify safe submission review flow`.
