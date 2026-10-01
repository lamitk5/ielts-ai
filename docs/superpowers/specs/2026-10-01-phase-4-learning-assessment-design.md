# IELTS AI Tutor — Phase 4 Learning & Assessment Design

Status: architectural design only. This document does not change product code,
database migrations, runtime configuration, or deployment configuration.

## 1. Purpose

Phase 4 turns the existing practice, writing, and speaking flows into an
auditable learning-assessment system:

`LEARN → PRACTICE → AUTOSAVE → SUBMIT → SCORE → FEEDBACK → REVIEW → RETRY →
UPDATE PROGRESS → UPDATE ROADMAP`.

The phase must preserve the existing Academic Luxury product shell while making
submission state, scoring authority, feedback provenance, learner ownership,
and adaptive-learning events explicit.

## 2. Scope

Phase 4 covers:

- a unified submission domain for Reading, Listening, Writing, and Speaking;
- durable drafts, autosave, idempotent submission, attempt history, and
  recovery after refresh;
- deterministic Reading and Listening scoring;
- provider-neutral Writing evaluation with versioned feedback;
- safe Speaking submission with audio storage references and manual review;
- reusable learner result pages and submission history;
- separated AI and human review records;
- trusted integration with the existing Phase 2 learning intelligence;
- approved-version-only integration with the Phase 3 practice bank;
- Tutor context links that respect submission ownership;
- additive migration and compatibility strategy;
- backend, frontend, security, integration, and acceptance test design.

Phase 4 does not implement onboarding, diagnostics, Today's Plan, the Error
Notebook UI, or Mock Test orchestration. Those belong to Phase 5, although the
submission contracts are designed so Phase 5 can reuse them.

## 3. Existing Architecture Reuse

The current codebase already provides the following foundations:

| Existing capability | Current source of truth | Phase 4 treatment |
| --- | --- | --- |
| Auth and ownership | `AuthInterceptor`, `AuthPrincipal`, `app_users`, `auth_sessions` | Every submission query resolves the authenticated user server-side. |
| Objective attempts | `PracticeAttempt`, `AttemptService`, `learning_attempts` | Evolve behind a canonical submission service; retain compatibility endpoints while clients migrate. |
| Static and approved practice | `PracticeService`, `SyntheticPracticeCatalog`, `PracticePublication`, `practice_catalog_publications` | Submit only an active approved publication and pin its exact version. |
| Reading/Listening fixtures | `PracticeQuestion`, `PracticeSet`, `PracticeReview` | Move answer-key evaluation into the server result pipeline; never expose answer keys in learner payloads. |
| Writing | `WritingAssessmentService`, `WritingAttempt`, `WritingAssessment`, `writing_submissions` | Add submission state and versioned evaluation records without overwriting submitted text. |
| Speaking | `SpeakingService`, `SpeakingAttempt`, `SpeechToTextProvider`, `speaking_attempts` | Keep text-only/manual-review boundary; add safe media abstraction. |
| Drafts | `LearningDraft`, `LearningDraftService`, `learning_drafts` | Reuse for editor drafts where it is sufficient; link durable submission snapshots by owner and revision. |
| Adaptive intelligence | `LearningEvidencePipeline`, `LearningIntelligenceService`, `learning_events`, `learning_mistakes`, profiles, roadmaps | Consume trusted graded events; do not create a second adaptive engine. |
| Tutor | `TutorContextService`, `TutorOrchestrator`, `AiChatContext`, conversation ownership | Add result/mistake references only through an owned context resolver. |
| Admin practice review | Phase 3 generator/review services and role guard | Keep approval and learner visibility separate. |
| Frontend result shell | `PracticeResultPage`, `PracticeResultSummary`, workspace result states | Replace duplicated result fragments with a shared result contract incrementally. |

### Current gaps that the design must close

The current durable objective endpoint accepts `score`, `total`, and result
payload values from the browser. Phase 4 must remove those values from the
trusted submit command. The server loads the approved practice version and
answer key, calculates the score, and writes the result.

The current `learning_attempts.answer_payload` is useful for recovery but is
not sufficient for question-level evidence, mistake classification, or review
history. A normalized result projection is therefore required.

Writing currently stores an assessment payload with the submission, and
Speaking currently stores a transcript/audio filename boundary. Phase 4 adds
immutable submission/version records instead of treating those current payloads
as an evaluation history.

## 4. Submission Domain

### 4.1 Canonical aggregate

`PracticeSubmission` is the domain aggregate exposed by the submission service.
It has one stable ID for the learner's work, regardless of skill:

- `id`;
- authenticated `userId`;
- `skill` (`READING`, `LISTENING`, `WRITING`, `SPEAKING`);
- `practiceId` and immutable `practiceVersionId`;
- optional `publicationRevision`;
- `status`;
- `startedAt`, `lastSavedAt`, `submittedAt`, `scoredAt`;
- `durationSeconds` as server-calculated elapsed time, with client timing only
  treated as a display hint;
- `autosaveRevision` and optimistic-concurrency token;
- submit idempotency key and a unique owner-scoped idempotency constraint;
- failure code and retryability state when processing fails;
- created/updated timestamps.

The aggregate owns the lifecycle. Skill-specific records hold only data that
cannot be represented safely in the common record.

### 4.2 Minimal persistence set

The implementation must first map these concepts to the existing tables and
only create a new table when the existing shape cannot preserve the required
invariants:

1. `practice_submissions`: canonical owner, version, lifecycle, timing, and
   idempotency record. Existing `learning_attempts` rows are a compatibility
   source during migration and are never silently deleted.
2. `submission_answers`: immutable submitted answer snapshot, keyed by
   submission and question. This is needed to distinguish the final answer
   from autosave snapshots and to support review without trusting JSON supplied
   later by the browser.
3. `submission_question_results`: deterministic correctness, answer snapshot,
   answer-key reference, question type, evidence reference, explanation, and
   mistake classification for objective skills.
4. `writing_submission_versions`: immutable Version 1, Version 2, and later
   response text snapshots with parent version and timestamps.
5. `writing_evaluations`: provider-neutral AI evaluations, evaluation version,
   criteria, strengths, issues, suggestions, evidence excerpts, grounding,
   disclaimer, and status. It never replaces the writing text or a human
   review.
6. `speaking_submissions`: prompt, preparation/response timing metadata,
   transcript source, media reference, and safe processing state.
7. `submission_reviews`: human review records for Writing and Speaking, with
   reviewer, criteria, comments, timestamp, and review version. Human criteria
   are separate from AI criteria.

`learning_events`, `learning_mistakes`, learner profiles, and roadmaps remain
the adaptive read/write model. A separate duplicate adaptive-result table is
not introduced. If an existing table can carry one of the records above
without losing immutability or ownership, the implementation may reuse it and
must document that mapping before allocating a migration.

### 4.3 Compatibility rule

Existing `/api/attempts` and skill-specific attempt routes may remain as
compatibility adapters during rollout, but they must delegate to the canonical
submission service. They must not retain a second scoring implementation or a
client-authoritative score path.

## 5. Submission State Machine

### 5.1 Common states

- `DRAFT`: a record exists but has not started the timed learning session;
- `IN_PROGRESS`: learner may autosave answers/content;
- `SUBMITTED`: the final learner payload is accepted exactly once;
- `SCORING`: deterministic or AI evaluation is running;
- `GRADED`: a learner-facing result is available;
- `FAILED`: processing failed; the submitted payload remains recoverable and
  can be retried under policy.

### 5.2 Skill-specific states

- Writing may use `AI_EVALUATING` between `SUBMITTED` and `GRADED`.
- Speaking may use `PENDING_REVIEW` after a valid submission when human review
  is required.
- Speaking with no transcript and no configured STT remains a submitted/manual
  boundary state, not a fabricated score.

### 5.3 Legal transitions

| From | Allowed transition | Authority |
| --- | --- | --- |
| `DRAFT` | `IN_PROGRESS`, `FAILED` | Submission service |
| `IN_PROGRESS` | `SUBMITTED`, `FAILED` | Owner submit command |
| `SUBMITTED` | `SCORING`, `AI_EVALUATING`, `PENDING_REVIEW`, `GRADED` | Server workflow |
| `SCORING` | `GRADED`, `FAILED` | Deterministic scorer |
| `AI_EVALUATING` | `GRADED`, `FAILED` | AI evaluation worker/service |
| `PENDING_REVIEW` | `GRADED`, `FAILED` | Authorized reviewer |
| `FAILED` | retry processing, `SCORING`, `AI_EVALUATING`, `PENDING_REVIEW`, or terminal failure | Server retry policy |
| `GRADED` | no mutation of the submission; later retry creates a new evaluation/version where allowed | Server policy |

An invalid transition returns a structured conflict error. A duplicate submit
with the same owner, submission ID, idempotency key, and content hash returns
the original accepted result. A different payload after finalization returns a
conflict and never overwrites the first submission.

## 6. Autosave

Autosave is a durable recovery feature, not a submission. The client sends the
owned submission ID, changed fields, client revision, and an idempotency token.
The server verifies ownership and status, increments the server revision, and
returns the authoritative snapshot.

Rules:

- autosave is accepted only in `DRAFT` or `IN_PROGRESS`;
- updates use optimistic concurrency (`expectedRevision` or equivalent);
- an older revision receives a conflict with the latest safe snapshot;
- save failures leave the local draft marked unsynced and retryable;
- browser refresh rehydrates from the server, then reconciles newer local data
  only after an explicit conflict decision;
- autosave never updates score, answer key, result, or adaptive profile;
- the final submitted snapshot is immutable and separately retained.

The existing `learning_drafts` service may continue to serve editor drafts, but
the submission service must make the relationship between a draft and a
submission explicit and owner-scoped.

## 7. Reading Scoring

Reading scoring is deterministic and server-authoritative:

1. resolve the authenticated owner and submission;
2. load the exact approved practice publication/version recorded at start;
3. load the versioned question set and hidden answer key;
4. normalize answers according to the question-type policy;
5. calculate correct count, total, accuracy, unanswered count, and duration;
6. write one `submission_question_results` row per question;
7. classify mistakes only when the question type and evidence policy support it;
8. transition to `GRADED` and emit one trusted adaptive event.

Each question result contains the learner answer, correctness, answer-key
reference, question type, passage/evidence reference, explanation, and
classification confidence. The learner API can expose the correct answer after
submission because it is now a result, never during the active attempt.

If a band estimate is offered, it must use an explicitly approved, versioned
conversion policy with an `estimated` label and auditable policy ID. Until that
policy exists, show score, accuracy, and evidence without converting to a band.

## 8. Listening Scoring

Listening uses the same deterministic result pipeline as Reading. The exact
approved practice version and answer key are resolved server-side. Audio/media
availability is a separate capability state and must not change deterministic
answer scoring.

The result stores question-level answer and explanation evidence. If an audio
or transcript reference is unavailable, the UI says so; it does not imply that
unsupported media or transcription was used. Listening scoring never calls an
LLM.

## 9. Result Model

The common result response contains:

- submission ID, skill, practice/version identity, status, timestamps, and
  duration;
- score/total/accuracy when objective scoring exists;
- estimated band only when an approved conversion policy exists;
- question results where applicable;
- mistake references and evidence;
- feedback availability and retry state;
- links/actions that are authorized for this learner.

Learner result actions are:

- `Xem lại bài`;
- `Ôn câu sai`;
- `Hỏi Trợ giảng AI`;
- `Làm bài tương tự`;
- `Quay về lộ trình`.

The response must distinguish `SCORING`, `PENDING_REVIEW`, `FAILED`, and
`GRADED`; a network or AI failure must never be rendered as a successful grade.

## 10. Writing Assessment

Writing uses the existing AI provider abstraction and a provider-neutral
assessment contract. Task 1 criteria are:

- Task Achievement;
- Coherence & Cohesion;
- Lexical Resource;
- Grammatical Range & Accuracy.

Task 2 uses Task Response in place of Task Achievement, followed by the same
three criteria.

The server submits the exact immutable writing version plus task metadata to the
provider boundary. The normalized evaluation contract contains:

- `overallBandEstimate`;
- criteria keyed to the task type;
- strengths;
- issues;
- suggestions;
- evidence excerpts or cited response spans where available;
- `priorityImprovements`;
- grounding status;
- disclaimer;
- provider/model/evaluation policy version in private audit metadata.

The learner-facing copy is always:

> Band ước lượng bởi AI — Không phải điểm thi IELTS chính thức.

No exact certainty, examiner identity, or official equivalence is implied.
Provider timeout, malformed output, quota exhaustion, or unavailable provider
leaves the submission and original writing safe, records a retryable failure,
and does not invent criteria or a band.

## 11. Writing Versioning

Every submitted or explicitly saved version contains:

- submission ID and sequential version number;
- full response text snapshot;
- parent version ID;
- created timestamp;
- word count;
- evaluation IDs and status;
- criteria/feedback references.

Version comparison is server-generated from owned versions and reports changed
paragraphs, resolved issues, repeated issues, and criterion changes. The UI may
say an issue was resolved only when the evidence in the two evaluations
supports that statement. An absent evaluation is shown as unavailable, not as
an improvement or regression.

## 12. Speaking Submission

Safe V1 supports:

- prompt and prompt version;
- preparation timer and response timer metadata;
- browser recording where supported;
- audio upload through a validated media endpoint;
- storage reference and playback authorization;
- submission and recovery;
- transcript only when manually supplied or returned by a real configured STT
  provider;
- manual review state.

No pronunciation band, fluency audio score, accent score, or transcript is
created without real supporting capability. A manual transcript is labeled as
manual. A future Groq Whisper or other STT provider plugs into the existing
provider boundary and records provider/model/version/confidence when present;
it does not alter the V1 submission contract.

## 13. Human Review

Human review is a separate audited operation:

- reviewer identity and role;
- review timestamp and version;
- criterion values;
- human comment;
- decision/status;
- optional correction/review reason.

AI criteria and human criteria are stored separately. A human result does not
silently replace the AI evaluation. Learner presentation may show both with
clear labels and a policy-defined primary view.

## 14. Learner Results

The reusable result shell is organized in this order:

1. outcome and status;
2. what was done well;
3. main mistakes;
4. evidence and explanation;
5. what to improve;
6. next action;
7. Tutor handoff with owned submission context.

Reading/Listening show score, accuracy, time, question results, evidence,
explanation, and retry/similar-practice actions. Writing shows Band ước lượng,
criteria, strengths, issues, priority fixes, and version history. Speaking shows
submission state, audio when available, manual feedback when available, and a
transcript only when its source is real and labeled.

## 15. Submission History

The learner route `Bài làm của tôi` uses a paginated server query with filters:

- All;
- Reading;
- Listening;
- Writing;
- Speaking;
- Graded;
- Pending.

Each row includes date, skill, practice/task, status, score or Band ước lượng
when available, duration, and an authorized result link. It never exposes raw
admin states such as generator internals to learners.

## 16. Admin Review

Admin route `Bài nộp của học viên` is a role-protected, paginated view of
submissions the reviewer is authorized to inspect. Filters are Unreviewed,
Reviewed, Reading, Listening, Writing, and Speaking.

Admin detail resolves all records server-side and shows:

- learner submission and immutable versions;
- AI estimate/criteria/feedback, if present;
- human criteria and comment, if present;
- review history, reviewer, and timestamps;
- conflict state if another reviewer finalized a newer version.

Admin endpoints never become a shortcut around learner ownership checks, and
ordinary learners receive a denial even if they know a submission ID.

## 17. Adaptive Learning Integration

The trusted pipeline is:

`GRADED submission → LearningEvent → MistakeRecord/Issue → profile refresh →
roadmap reconciliation`.

Use the existing `LearningEvidencePipeline`, `LearningEventIngestionService`,
`LearningProfileService`, `WeaknessStrengthAnalyzer`, and roadmap services.
Extend their input contract to carry the canonical submission ID, exact
practice version, question evidence, and confidence.

Rules:

- autosaves and failed processing do not update the profile;
- deterministic Reading/Listening results may publish trusted item evidence;
- Writing publishes only available, validated assessment evidence;
- Speaking publishes completion/available transcript evidence, never invented
  pronunciation evidence;
- one mistake does not become a permanent weakness after one observation;
- `UNKNOWN` and `INSUFFICIENT_DATA` remain valid states;
- event publication is idempotent by submission/result version;
- adaptive refresh failure must not roll back a durable submission.

## 18. Practice Bank Integration

At start and submit, the server verifies the practice is exposed by an active
`practice_catalog_publications` row and pins `generated_set_id`,
`generated_version_id`, `published_set_id`, and publication revision.

Only `APPROVED` and active published content is attemptable. The following are
not learner-eligible: `DRAFT`, `GENERATING`, `AUTO_VALIDATING`,
`PENDING_REVIEW`, `NEEDS_REVISION`, and `REJECTED`.

If a later admin revision is published, existing submissions retain their
original version and answer key. New attempts resolve the new publication.

## 19. Tutor Integration

Result and mistake actions open the existing Tutor pipeline with a server-built
context containing only owned data:

- submission/result ID;
- skill and practice version;
- selected question or writing version;
- evidence/explanation already authorized to the learner;
- relevant adaptive context resolved by `TutorContextService`.

The browser may request a reference ID, but the server validates ownership and
reconstructs the context. Tutor answers remain provider-neutral, and Tutor
failure never changes a grade or submission state.

## 20. Data Model

### 20.1 Ownership and relationships

- `practice_submissions.user_id → app_users.id` with cascade policy reviewed
  against retention requirements;
- `practice_submissions.practice_version_id → approved publication/version`
  with restrict semantics so historical content cannot disappear silently;
- `submission_answers.submission_id → practice_submissions.id`;
- `submission_question_results.submission_id → practice_submissions.id`;
- `writing_submission_versions.submission_id → practice_submissions.id`;
- `writing_evaluations.version_id → writing_submission_versions.id`;
- `speaking_submissions.submission_id → practice_submissions.id`;
- `submission_reviews.submission_id` plus reviewer user ID;
- `learning_events.attempt_id` or equivalent canonical submission reference;
- existing mistakes/profile/roadmap records remain user-owned.

### 20.2 Keys, indexes, and audit

Required indexes include owner/status/time for history, owner/idempotency for
submit deduplication, submission/question for results, practice version for
audit, and reviewer/status for admin queues. Every mutable workflow record has
created/updated timestamps; state changes and human review actions have an
append-only audit record or equivalent event.

### 20.3 Retention

Retention is policy-driven. Deleting a user must remove or anonymize owned
answers, writing text, audio references, and Tutor context according to the
existing account-retention policy. Published practice and admin provenance are
not deleted merely because a learner account is removed.

## 21. APIs

The target API is a single submission contract with skill-specific projections:

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/api/submissions` | Start an owned submission for an approved version. |
| GET | `/api/submissions/{id}` | Read the owned authoritative snapshot. |
| PUT | `/api/submissions/{id}/draft` | Autosave a draft with revision guard. |
| POST | `/api/submissions/{id}/submit` | Finalize once; score/evaluate server-side. |
| GET | `/api/submissions/{id}/result` | Read a result or processing state. |
| GET | `/api/me/submissions` | Paginated learner history and filters. |
| GET | `/api/me/submissions/{id}/versions` | Owned Writing version history where relevant. |
| POST | `/api/submissions/{id}/audio` | Upload/attach validated Speaking audio. |
| GET | `/api/submissions/{id}/audio` | Stream only to the owner or authorized reviewer. |
| GET | `/api/admin/submissions` | Role-protected review queue. |
| GET | `/api/admin/submissions/{id}` | Role-protected detail. |
| POST | `/api/admin/submissions/{id}/review` | Add a separate human review version. |

Existing skill endpoints remain compatibility adapters during migration. New
clients must not send score, total, answer key, evaluation, reviewer, or owner
identity as trusted fields.

## 22. Security

The server enforces:

- authenticated owner lookup from the session principal;
- object-level authorization on every submission, version, result, draft, audio,
  mistake, and Tutor context request;
- approved practice visibility at start and submit;
- no client-supplied score, answer key, status, reviewer, or user ID authority;
- admin-only review operations with explicit role checks;
- signed/opaque media references that cannot expose arbitrary filesystem paths;
- request size, MIME, filename, and content validation;
- rate limits for submit, AI evaluation, audio, and review operations;
- safe error responses without provider payloads or stack traces.

The required negative tests are User A cannot read User B's submission,
writing, draft, result, audio, or Tutor context, and a learner cannot call
admin review endpoints.

## 23. File/Audio Handling

Writing response text is stored as text with length limits and escaped/rendered
as text. Speaking audio uses a storage abstraction, not a database blob by
default:

- allow-list project-supported audio MIME types and extensions;
- enforce a configured byte limit and duration limit;
- generate a server-owned storage key;
- scan/reject malformed content where the deployment supports it;
- never use the original filename as a path;
- authorize playback through the submission owner/reviewer;
- delete or retain by the account-retention policy.

No executable, arbitrary archive, or image upload is accepted by the Speaking
audio endpoint.

## 24. AI Provider Boundary

Reuse `AiProvider`, `AiProviderRouter`, provider health/cooldown, and the
existing provider-neutral `AiChatResult`. The Writing service receives a
normalized evaluation result, not Groq/Cloudflare/Gemini-specific JSON.

Deterministic objective scoring and state transitions must work with every AI
credential missing. Automated tests mock providers and spend zero live quota.
AI provider metadata is retained for audit but is not required by the frontend
contract.

## 25. Error Handling

| Failure | Durable behavior | Learner message |
| --- | --- | --- |
| Network loss during autosave | Keep last server snapshot; mark local draft unsynced; retry safely. | “Chưa đồng bộ — thử lại.” |
| Duplicate submit | Return the original result for the same idempotency key/content. | “Bài đã được nộp.” |
| Different submit after finalization | Keep original; return conflict. | “Lượt làm này đã khóa.” |
| AI timeout/429 | Preserve submitted text/version; mark retryable evaluation failure. | “Đánh giá AI đang tạm gián đoạn; bài của bạn vẫn được lưu.” |
| Deterministic scoring failure | Keep submission and queue/retry scoring; never show a partial success as final. | “Chưa thể tạo kết quả; thử lại.” |
| Audio upload failure | Keep text/timing draft; do not finalize missing audio silently. | “Âm thanh chưa được lưu.” |
| Expired session | No data mutation; re-authenticate and recover by owned ID. | “Phiên đăng nhập đã hết.” |
| Practice version unavailable | Reject new start; preserve existing submission pinned to its version. | “Bộ đề này không còn mở cho lượt mới.” |
| Review conflict | Do not overwrite newer human review. | “Bản đánh giá đã được cập nhật.” |
| Browser refresh | Rehydrate status and latest server snapshot. | Contextual status, never a blank page. |

## 26. Performance

Submission history, admin queues, question results, and reviews are paginated.
Results use bounded question/evidence payloads. Profile and roadmap pages use
summary endpoints. Audio is streamed on demand. AI evaluation is asynchronous
where latency warrants it. Indexes are owner/status/time and submission/version
focused; no endpoint loads all Tutor history or all learner mistakes by default.

## 27. Testing

### Unit and service tests

- every legal and illegal state transition;
- double submit with same and different idempotency keys;
- score calculation, blank answers, normalization, and total bounds;
- answer key never accepted from request;
- evidence/question result persistence;
- Writing Task 1/Task 2 criterion mapping and malformed AI response;
- Writing version parentage and comparison;
- Speaking without STT produces no transcript/score;
- retryable provider failure preserves original writing;
- adaptive event idempotency and `UNKNOWN` evidence behavior.

### Repository/controller/security tests

- owner-only get/save/submit/result/history;
- User A/User B isolation for every specialized record;
- approved-only practice and exact version pinning;
- admin role required for review;
- upload MIME/size/path safety;
- invalid transition and stale autosave revision responses.

### Frontend tests

- loading, autosave, conflict, submit, processing, graded, failed, and retry
  states;
- result shell for all four skills;
- estimated-label/disclaimer rendering;
- no answer key before submit;
- keyboard-accessible actions and no hover-only result information;
- Tutor handoff carries only the owned reference;
- responsive result/history/review layouts and reduced motion.

### Integration and acceptance

- learner starts Reading, refreshes, resumes, autosaves, submits, sees
  deterministic result, reviews a mistake, and retries;
- same flow for Listening without configured audio capability;
- Writing draft → multiple versions → AI evaluation → retry after timeout;
- Speaking text-only/manual path and audio ownership path;
- approved Phase 3 version can be attempted; every unapproved state is denied;
- graded result updates Phase 2 profile/roadmap once;
- admin reviews Writing/Speaking while learner remains unable to review as
  admin;
- no fake score/progress appears in a new learner account.

## 28. Migration Strategy

Migration work is additive and coordinated after the final main branch is
known. Do not reserve exact Flyway version numbers in this design. Allocate a
coordinated range after the current V30 baseline is confirmed in the
implementation branch.

The rollout order is:

1. add nullable/base tables and indexes;
2. deploy read/write adapters that can understand old and new records;
3. backfill existing learning attempts into canonical submissions with a
   documented source/version marker;
4. dual-read/verify counts and ownership in QA;
5. migrate frontend calls to the canonical API;
6. stop accepting client scores;
7. only later remove compatibility code after production evidence supports it.

No legacy database is modified by the design task. No migration drops existing
columns or deletes existing attempt/history data.

## 29. Risks

- existing synthetic practice and database publications have different shape;
  the canonical version resolver must normalize both without exposing answer
  keys;
- old attempts may lack exact practice versions, so backfill must mark their
  provenance and avoid pretending precision that does not exist;
- AI evaluations can be slow or unavailable; asynchronous retry and immutable
  writing versions are mandatory;
- audio retention and privacy require a deployment-specific storage policy;
- duplicate routes can create double adaptive events unless all adapters use
  one idempotent publisher;
- a large result payload can hurt mobile performance unless question details
  are paginated or bounded.

## 30. Non-Goals

This phase does not include payments, social/community features, a mobile app,
video classrooms, official IELTS scoring, automatic pronunciation grading
without real audio analysis, unreviewed generated practice exposure, or a
microservice migration.

## 31. Definition of Done

- one server-authoritative submission lifecycle exists for all four skills;
- autosave and finalization are durable, owned, and idempotent;
- Reading and Listening scores/evidence are deterministic and auditable;
- Writing text/version history is immutable and AI evaluations are labeled
  Band ước lượng, with human review separate;
- Speaking never fabricates STT or pronunciation scoring;
- only approved, version-pinned practice can be submitted;
- learner results, history, and admin review are paginated and authorized;
- trusted graded results feed existing Phase 2 exactly once;
- Tutor context is ownership-checked;
- all state, security, and error scenarios have automated coverage;
- migration is additive and uses coordinated, not hardcoded, future versions;
- frontend and backend acceptance flows pass with providers mocked.

## 32. Rulings

1. The browser is never a scoring authority.
2. A submission is immutable after finalization; corrections are new versions
   or new submissions, never silent overwrites.
3. Reading and Listening scoring are deterministic and do not call an LLM.
4. Writing uses `Band ước lượng bởi AI`; it is never presented as an official
   IELTS score.
5. AI and human review are separate records.
6. Speaking without real STT/audio analysis has no fabricated transcript,
   pronunciation band, fluency score, or accent score.
7. Only active approved practice versions are learner-eligible.
8. Adaptive updates consume trusted graded events through existing Phase 2
   services; no duplicate adaptive engine is allowed.
9. All object reads and Tutor handoffs are owner-checked server-side.
10. Migration IDs are allocated only after the final main baseline is fixed.
11. Phase 4 implementation is decomposed into 4A Submission Engine, 4B
    deterministic objective scoring, 4C Writing assessment/versioning, 4D
    Speaking/manual review, and 4E Results/admin/adaptive integration.
12. Safe parallel work is 4A→4B→4E in one lane and 4C→4D in another, with
    integration at the canonical submission/result contracts.

