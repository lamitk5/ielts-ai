# Student-Ready IELTS AI Tutor MVP — Architecture Design Specification

**Status:** Approved architecture direction; implementation intentionally not included in this document.

**Baseline:** `feature/ai-phase2-phase3-integration` at `75ee60637d848a235a7b8eed70b5a32b3ce6e4b5`.

**Product:** IELTS AI Tutor with React/Vite, Spring Boot, PostgreSQL + pgvector, Phase 2 Adaptive Learning, Phase 3 Practice Generator, RAG Tutor, existing Auth, Settings, Academic Luxury 2.0, and LUMEN Pixel Scholar.

## 1. Executive goal

The Student-Ready MVP gives a learner one reliable study loop:

```text
Login/Register
  → choose skill
  → browse approved practice
  → open practice detail
  → start/resume attempt
  → answer
  → submit
  → deterministic or AI-assisted feedback
  → persist result
  → emit learning evidence
  → update profile, weaknesses, and roadmap
  → ask Tutor about the exact task/context
  → continue or review after reload/restart
```

Reading, Listening, Writing, and Speaking are equal first-class learning areas. The MVP must support real study flows for all four without implying capabilities that are not configured or evidenced.

PostgreSQL is the source of truth for learner-owned data, approved generated practice, attempts, results, learning events, profiles, roadmaps, preferences, and authenticated Tutor conversations. React state is presentation state only. Provider-specific AI payloads never cross the frontend contract.

## 2. Explicit non-goals and product language

The MVP does not:

- impersonate an official IELTS examiner or claim official IELTS affiliation;
- claim an official IELTS band score; every AI-produced score is labelled **Band ước lượng**;
- fabricate speech-to-text, pronunciation, audio quality, or speaking assessment when no STT provider is configured;
- fabricate learner history, weaknesses, progress, or exam dates;
- treat a similarity threshold as a copyright guarantee;
- assume Cambridge, British Council, or other third-party content is licensed;
- replace the existing Phase 2, Phase 3, RAG, Auth, Tutor, or Settings architecture;
- redesign the whole application;
- reset, drop, or destructively rewrite the existing database.

When a provider, source, media file, or persistence operation is unavailable, the UI reports the unavailable state and preserves deterministic learning behavior. It never substitutes invented success data.

## 3. Architecture principles

### 3.1 One authority per concern

| Concern | Authoritative source | Frontend responsibility |
| --- | --- | --- |
| Auth/session/role | Spring Boot Auth and session contract | Render state, route UX, never authorize by itself |
| Approved generated practice | PostgreSQL Phase 3 records plus publication projection | Query catalog/detail; never show non-approved state |
| Learner attempts/results | PostgreSQL attempt records and skill-specific persisted submissions | Render, edit current draft, submit through API |
| Learning evidence | Phase 2 event/profile/roadmap services | Display normalized profile and roadmap data |
| Authenticated Tutor history | PostgreSQL conversation/message records | Render server history and transient optimistic state |
| Guest Tutor state | Current browser session only, bounded and non-durable | Render current tab only |
| Preferences | Authenticated server preference row; guest versioned local cache | Apply tokens and send normalized updates |
| Mascot animation | React/CSS runtime state | Visual interaction only; no product data authority |

### 3.2 Provider-neutral boundaries

The backend normalizes provider outcomes into the existing `AiChatResponse`, `AiGrounding`, `AiSource`, feedback, and failure contracts. The frontend branches only on normalized statuses such as `ANSWERED`, `APP_DATA`, `INSUFFICIENT_CONTEXT`, `INSUFFICIENT_EVIDENCE`, `FALLBACK`, `OUT_OF_SCOPE`, `AI_TIMEOUT`, and `AI_TEMPORARILY_UNAVAILABLE`.

### 3.3 Durable state rule

An operation that reports success must have a committed durable record or an explicit deterministic local-only state. A catalog publish, attempt submission, learning event, or authenticated conversation must not report success from a process-memory map.

## 4. Student learning flow

### 4.1 Home, dashboard, and catalog

1. The learner authenticates or registers through the existing Auth flow.
2. Home and Dashboard show only learner-owned progress. A guest sees neutral previews and an assessment CTA, never personal bands, mistakes, or exam data.
3. The learner chooses Reading, Listening, Writing, or Speaking.
4. The catalog API returns active, learner-visible practice entries. The server filters by `APPROVED` publication state and ownership-independent public visibility.
5. The learner opens a detail response containing a stable practice identifier, version, skill, title, instructions, and content allowed for that stage.
6. The learner starts a new attempt or resumes their own `IN_PROGRESS` attempt.
7. Answers and drafts are saved through attempt APIs. The client may optimistically render a pending save, but cannot mark a failed save as persisted.
8. Submission is atomic with validation of practice identity, version, ownership, and attempt status.
9. The result response includes deterministic scoring where available, normalized AI feedback where configured, and explicit unavailable states otherwise.
10. A successful completion emits an idempotent Phase 2 learning event and refreshes the learner profile/roadmap projection.
11. Tutor receives identifiers for the current practice and attempt and resolves trusted context on the server.

### 4.2 Reading

- Question answers and correctness are deterministic from the approved practice version.
- Score is deterministic and persisted with the attempt result.
- Explanations are available from stored approved content; an AI explanation is optional and provider-neutral.
- Cross-highlighting uses the existing setting and only highlights approved passage/question references.
- Countdown uses the existing setting. Expiry is handled by the client timer plus server-side submit validation; the server remains authoritative for submitted state.
- A question answer is never revealed before the exercise’s allowed review point.

### 4.3 Listening

- Answer validation and scoring are deterministic whenever the approved exercise contains the required answer data.
- Audio is rendered only when an approved, available media reference exists.
- A transcript or explanation is displayed only when stored and legally/technically available.
- Missing media produces an unavailable exercise state with a retry or alternate approved item; it does not show a fake player or fabricate listening results.

### 4.4 Writing

- The learner’s draft is saved as an owned attempt/submission and is recoverable after reload.
- Task 1 and Task 2 use the existing task metadata and criteria contract.
- AI feedback is requested through the provider-neutral backend contract and is stored as normalized feedback associated with the attempt/version.
- The response may contain criterion-level observations and `overallBandEstimate`; that value is always labelled **Band ước lượng** and is never described as official grading.
- If AI or RAG is unavailable, the draft and submission status remain durable and the UI reports feedback unavailable without inventing a score.
- Sources/citations are shown only when the feedback actually used governed RAG evidence.

### 4.5 Speaking

- Existing supported controls remain usable for text/manual practice and any configured recording flow.
- No STT, pronunciation, fluency, or band result is shown unless the required provider and response evidence exist.
- Without STT configuration, the UI offers a manual transcript/input path and reports `STT_NOT_CONFIGURED` for unsupported automatic analysis.
- Groq Whisper remains a future extension point, not an MVP dependency.

## 5. Persistent approved practice catalog

### 5.1 Source of truth and visibility boundary

The existing Phase 3 tables remain the content authority:

- `generated_practice_sets` stores set identity, skill, workflow state, current version, approval actor, and approval time.
- `generated_practice_versions` stores the versioned passage and question payloads.
- `practice_generation_sources`, `practice_generation_blueprints`, `practice_generation_jobs`, `generation_validation_results`, `practice_review_actions`, and `practice_provenance_meta` retain provenance, rights, validation, audit, and review history.

The implementation adds one durable publication projection named `practice_catalog_publications`. It does not copy passage or question content. It maps a published learner-visible identifier to one approved set/version and stores catalog visibility metadata:

```text
practice_catalog_publications
  published_set_id       VARCHAR(120) PRIMARY KEY
  generated_set_id       UUID NOT NULL REFERENCES generated_practice_sets(id)
  generated_version_id   UUID NOT NULL REFERENCES generated_practice_versions(id)
  skill                  VARCHAR(16) NOT NULL
  visibility_state       VARCHAR(16) NOT NULL  -- ACTIVE or INACTIVE
  published_at           TIMESTAMPTZ NOT NULL
  unpublished_at         TIMESTAMPTZ
  publication_revision   INTEGER NOT NULL
  created_at              TIMESTAMPTZ NOT NULL
  updated_at              TIMESTAMPTZ NOT NULL
```

Required constraints and indexes:

- `published_set_id` is unique and stable across a revision;
- `(generated_set_id, generated_version_id)` is unique;
- `skill` is restricted to `READING`, `LISTENING`, `WRITING`, `SPEAKING`;
- `visibility_state` is restricted to `ACTIVE`, `INACTIVE`;
- an active publication may reference only a generated set whose state is `APPROVED` and whose referenced version is its current approved version;
- indexes cover `(skill, visibility_state, published_at DESC)` and `(generated_set_id, publication_revision DESC)`.

The server rechecks approval state in every catalog query. The projection is not an authorization bypass and cannot make a draft visible by itself. The existing static deterministic seed catalog remains a versioned built-in catalog with `APPROVED` provenance; it is not represented as a generated database row and is never mixed with unapproved generated content.

### 5.2 Idempotent hydration/publication

Approval and publication are one transaction:

1. Lock the generated set and current version.
2. Require `APPROVED`, valid source rights, passing mandatory validation, and a non-null current version.
3. Derive the stable `published_set_id` from the generated set’s existing identifier; never generate a new learner identifier on retry.
4. Insert or update the publication projection using `(generated_set_id, generated_version_id)` as the idempotency key.
5. Mark prior publication versions for the same generated set `INACTIVE` when a new approved revision is published.
6. Write provenance with set, version, source, blueprint, approver, approval time, and revision.
7. Commit before returning success.

Retrying the same approval/reconciliation produces one active publication and one stable learner-visible identifier. A failed transaction leaves no learner-visible publication. No `ConcurrentHashMap` is used as the catalog’s durable store.

### 5.3 Catalog API contract

`GET /api/practice/catalog?skill=READING|LISTENING|WRITING|SPEAKING` returns:

```json
{
  "items": [
    {
      "practiceId": "reading-synth-1234abcd",
      "version": 1,
      "skill": "READING",
      "title": "Changing Realities of Polar Ecosystems",
      "summary": "...",
      "sourceType": "PROJECT_CREATED",
      "publishedAt": "2026-09-27T10:00:00Z"
    }
  ]
}
```

The endpoint returns only active approved publications and approved built-in seeds. It never returns draft state, internal validation payloads, raw provider data, private source content, or another learner’s data.

`GET /api/practice/catalog/{practiceId}` returns the selected approved version, task metadata, learner-safe content, source/provenance summary, and available media references. It returns `404` for unknown, inactive, unapproved, or unauthorized content without revealing which hidden state caused the rejection.

The existing learner practice endpoints remain compatible while their catalog implementation changes from process-memory lookup to the durable publication query. Generated content is not duplicated into a second practice-content model.

## 6. Attempt and result persistence

### 6.1 Attempt authority

Existing skill-specific tables remain the compatibility boundary, but attempt lifecycle fields become durable and server-authoritative:

- `learning_attempts` is extended for Reading/Listening objective attempts;
- `writing_submissions` is extended for Writing attempts;
- `speaking_attempts` is extended for Speaking attempts.

No second durable attempt history is created for the same skill. A future implementation adds a single migration, `V29__student_ready_mvp_persistence.sql`, after the legacy migration rehearsal described in Section 13. The migration is additive and supplies the missing lifecycle/version/reference fields.

For objective attempts, the resulting `learning_attempts` shape is:

```text
id                  UUID PRIMARY KEY
user_id             UUID FK app_users NOT NULL
skill               READING or LISTENING
set_id              VARCHAR(120) NOT NULL
practice_version    INTEGER NOT NULL
status              IN_PROGRESS | SUBMITTED | SCORED | FEEDBACK_READY | FAILED
started_at          TIMESTAMPTZ NOT NULL
submitted_at        TIMESTAMPTZ
elapsed_seconds     INTEGER
answer_payload      JSONB NOT NULL
result_payload      JSONB NOT NULL DEFAULT '{}'
feedback_ref        VARCHAR(160)
score               INTEGER nullable until SCORED
total               INTEGER nullable until SCORED
created_at          TIMESTAMPTZ NOT NULL
updated_at          TIMESTAMPTZ NOT NULL
```

The migration makes `score` and `total` nullable only for `IN_PROGRESS`/`SUBMITTED`, adds a status constraint, and preserves existing completed rows as `SCORED`. Existing readers continue to receive score/total for completed rows.

Writing adds `practice_version`, `status`, `started_at`, `submitted_at`, `elapsed_seconds`, `feedback_ref`, and normalized result metadata to `writing_submissions`. Speaking adds the same lifecycle/version fields to `speaking_attempts`; its existing `STT_NOT_CONFIGURED` state remains valid. Large passage/audio content is referenced by practice/version/media identifiers, not copied into attempts.

### 6.2 Attempt API

- `POST /api/practice/{practiceId}/attempts` starts or returns the authenticated learner’s resumable `IN_PROGRESS` attempt for the requested version.
- `GET /api/attempts/{attemptId}` returns the owned attempt, current answers/draft, status, and normalized result state.
- `PUT /api/attempts/{attemptId}/answers` saves a bounded answer/draft payload after ownership, version, and status checks.
- `POST /api/attempts/{attemptId}/submit` validates the final payload and atomically transitions the attempt to `SUBMITTED`, then deterministic scoring to `SCORED` where applicable.
- `GET /api/attempts/{attemptId}/result` returns the owned normalized result and feedback state.

Submitting an already submitted attempt is idempotent when the same final payload is provided and is rejected when the payload conflicts. An attempt cannot change practice ID/version after start. Resuming is allowed only for the owner and only from `IN_PROGRESS`.

### 6.3 Result contract

Every result carries:

```json
{
  "attemptId": "uuid",
  "status": "SCORED|FEEDBACK_READY|FAILED|UNAVAILABLE",
  "skill": "READING",
  "practiceId": "reading-synth-1234abcd",
  "practiceVersion": 1,
  "score": 8,
  "total": 10,
  "overallBandEstimate": null,
  "feedback": [],
  "grounding": { "status": "NOT_ENABLED", "ragEnabled": false, "sources": [] }
}
```

`overallBandEstimate` is nullable and is labelled **Band ước lượng** when present. Provider payloads, API keys, raw prompts, and internal errors are never stored or returned.

## 7. Phase 2 Adaptive Learning integration

The completion transaction writes the attempt/result first. After commit, an idempotent application service emits the existing Phase 2 learning event with:

```text
userId, eventType, skill, attemptId, practiceSetId, questionId,
sourceReference, payload, occurredAt, clientEventId
```

The event pipeline then updates mistakes, skill profile, learner profile, and active roadmap using the existing Phase 2 services. A repeated submit or event delivery cannot create a duplicate event because the existing user-scoped event idempotency keys remain authoritative.

Evidence rules:

- One isolated mistake does not create a durable weakness.
- Mistake categories must pass the existing confidence/evidence policy.
- `UNKNOWN` remains valid when evidence is insufficient.
- Profile and roadmap projections are refreshed from persisted evidence, never from client-supplied score/band claims.
- Proactive Tutor suggestions are emitted only when the existing evidence gate is satisfied.

The learner dashboard reads only the owner-scoped profile, skill profiles, issues, roadmap, and attempts. Guest pages render neutral previews and do not call learner history endpoints.

## 8. Tutor as a single source of truth

### 8.1 Conversation ownership

For authenticated learners, `ai_conversations`, `ai_messages`, and `ai_conversation_summaries` are the only durable conversation authority. The backend validates `conversationId` against the authenticated user before loading or appending messages. The existing ownership rule remains mandatory for every conversation read, append, archive, and context resolution.

The frontend retains only:

- the messages currently displayed;
- a pending optimistic message while a request is in flight;
- transient composer text, attachment state, loading, retry, and panel state.

When an authenticated conversation has a `conversationId`, the frontend sends no durable `history` array. The backend loads the bounded recent history and summary from PostgreSQL. The first authenticated message creates a conversation with an empty client history; the returned conversation ID is used for subsequent requests. The backend rejects or ignores client history for an owned conversation rather than concatenating it with stored history.

For guests, no conversation is persisted. The frontend may send at most the current tab’s bounded eight-message history; the server treats it as ephemeral input and never presents it as durable history. On reload, guest Tutor starts a fresh session.

### 8.2 Context identifiers and trusted resolution

Tutor requests use the existing normalized contract and may include:

```json
{
  "message": "Why was my answer incorrect?",
  "conversationId": "uuid-or-null",
  "context": {
    "skill": "READING",
    "practiceId": "reading-synth-1234abcd",
    "practiceVersion": 1,
    "attemptId": "uuid",
    "questionId": "reading-synth-1234abcd-q1"
  }
}
```

The server resolves practice/version, attempt ownership, question prompt, submitted answer, correct answer eligibility, result, profile, and roadmap from IDs. It ignores arbitrary client-pasted answer keys, scores, bands, protected context, or another learner’s identifiers. A missing or unauthorized context produces `INSUFFICIENT_CONTEXT` or `INVALID_CONVERSATION` according to the existing normalized error contract.

### 8.3 Tutor behavior

Tutor may explain, hint, clarify feedback, suggest the next practice, and discuss an evidence-backed mistake. It must not reveal an answer key before the exercise’s allowed point, fabricate evidence, claim official IELTS grading, expose another user’s data, or bypass status/ownership checks.

RAG is used only for source-dependent questions and preserves citation provenance. Generic messages such as `hello` do not embed or query RAG. Deterministic app-data questions use existing deterministic Tutor tools and do not call an LLM.

## 9. LUMEN Pixel Owl Scholar

The future visual implementation replaces the current human-like Pixel Scholar visual with a project-owned **LUMEN Pixel Owl Scholar** while preserving the launcher and Tutor behavior contract.

Visual requirements:

- premium pixel-art owl with warm ivory feathers and large expressive eyes;
- navy graduation cap, champagne-gold trim/tassel, and a small robe/book cue;
- restrained Academic Luxury palette, no arcade neon treatment and no stock image;
- separate pupil elements with visibly larger eye bounds so cursor movement is legible.

Interaction contract:

- desktop fine-pointer `pointermove` on the page updates normalized target coordinates;
- both pupils move toward the pointer and clamp inside their eye bounds;
- the mascot container remains anchored; only pupils and a very small head reaction may move;
- occasional idle blink remains active;
- click sequence is blink, reopen, subtle pixel bounce, then Tutor opens;
- hover adds restrained champagne shimmer and tooltip `Trợ giảng AI`;
- launcher exposes `aria-label="Mở Trợ giảng AI"` and supports Enter/Space;
- touch and coarse pointers keep pupils static;
- Animation OFF or `prefers-reduced-motion: reduce` disables decorative transitions while retaining click, keyboard, Tutor, and essential state changes;
- pointer listeners, RAF handles, timers, and cleanup are owned by the launcher and removed on unmount.

The owl has no access to learner data and cannot open a protected Tutor context without the normal Tutor flow.

## 10. Settings integration

The existing Settings contract remains the only preference surface:

- theme: `dark`, `light`, `system`;
- accent: `gold`, `sapphire`, `emerald`, `burgundy`, `violet`, `slate`;
- density: `spacious`, `default`, `compact`;
- motion: `system`, `reduce`, `allow`;
- font scale and language;
- proactive AI suggestions;
- cross-highlighting;
- countdown default;
- Reading and Writing split ratios `40`, `50`, `60`.

There is no slideshow setting. Settings are consumed by real learning workspaces, Tutor suggestions, cross-highlighting, countdown behavior, and the Academic Luxury token layer. Components consume semantic tokens, not direct accent branches. Reduced motion is respected by CSS and Framer Motion.

### 10.1 Language persistence decision

Authenticated language is added to the existing `user_preferences` row as a constrained `language` value (`VI` or `EN`) in the future additive migration. The existing preferences DTO, optimistic versioning, conflict handling, and `/api/user/preferences` endpoint are extended; no second language store is created.

Precedence is explicit:

```text
authenticated server preference
  > authenticated local account cache while hydration is pending
  > guest local versioned preference
  > default Vietnamese preference
```

On authenticated login, a successful server GET replaces the account cache. A local edit made before hydration is reconciled using the existing version/conflict policy. On logout, the account cache is cleared from active state and the guest preference is restored. Guest language remains local-only.

## 11. Admin client-side authorization

Backend authorization remains the security boundary. The frontend adds an `AdminRouteGuard` for UX:

- unauthenticated access redirects to `/login` with a return path;
- authenticated non-admin access renders the existing access-denied state and does not mount the admin page shell or start admin polling;
- authenticated admins may render `/admin/rag`, `/admin/practice-generator`, and `/admin/practice-generator/sets/:setId`;
- direct URL navigation and refresh use the same guard;
- the guard never replaces backend role checks, which continue to return `401/403` for unauthorized API requests.

Admin route data is fetched only after the guard confirms the current session role. The learner cannot see draft content through the catalog or admin UI.

## 12. Security and ownership rules

- Every learner attempt, result, draft, profile, issue, roadmap, preference, attachment, and conversation query filters by authenticated principal ownership.
- Admin generator/review/RAG operations require the existing `ADMIN` role at the backend.
- Catalog publication is readable to authenticated learners only when active and approved; guest catalog access may expose only explicitly public built-in summaries.
- Draft, generating, validation, revision, and rejected artifacts never enter learner catalog responses.
- Practice ID, version, attempt ID, question ID, role, status, score, answer key, and band are server-authoritative.
- Tutor context is resolved from trusted server-side records.
- Attachments are owned, size/type validated, stored through the existing attachment boundary, and never made public by client metadata.
- Error responses expose normalized product-safe messages, not raw SQL, provider, file-system, or credential details.
- API keys are environment configuration only and never enter database rows, browser bundles, logs, or responses.

## 13. Legacy database preservation and migration rehearsal

### 13.1 Known state

The shared/default database may contain V1–V8 and V28 while lacking V9/V10. A clean database applies:

```text
V1 → V2 → V3 → V4 → V5 → V6 → V7 → V8 → V9 → V10 → V28
```

The legacy database and its data must be preserved. The implementation must not drop it, reset it, edit `flyway_schema_history` arbitrarily, or assume that a clean database result proves legacy compatibility.

### 13.2 Mandatory rehearsal gates

Before any migration change reaches the original database:

1. Produce a full logical and physical backup and record its checksum and restore verification.
2. Restore a clone with a new database name and isolated credentials.
3. Capture row counts and representative checksums for users, roles, RAG documents/chunks, embedding metadata/vectors, learning attempts/events/mistakes/profiles/roadmaps, generated sets/versions/reviews/provenance, preferences, conversations/messages, writing submissions, and speaking attempts.
4. Inspect `flyway_schema_history`, actual V9/V10 SQL, V28 SQL, constraints, indexes, columns, and existing object definitions on the clone.
5. Compare every object/data effect of V9/V10 against the clone’s existing schema and data. A migration is safe only when missing objects can be added without conflicting names, constraints, indexes, or incompatible rows.
6. Rehearse remediation only on the clone. The default strategy is to apply V9 and V10 in their committed order before V28; if the clone already has V28, use the temporary out-of-order gate in Section 14 only after all checks pass.
7. Run Flyway validation/migrate twice, then start and restart the backend against the clone.
8. Run backend tests, package, representative auth/practice/profile/Tutor reads, catalog queries, approval visibility, attempt submit/resume, and Phase 2 event checks.
9. Re-run row-count, checksum, foreign-key, and learner/admin ownership checks against the pre-rehearsal capture.
10. Document evidence and obtain explicit release approval before applying the exact rehearsed remediation to the original database.

If any check differs unexpectedly, stop the rehearsal and preserve the clone for investigation. The original database is not touched until the clone passes every gate.

### 13.3 Compatibility procedure outcomes

The implementation must choose exactly one outcome based on observed clone evidence:

- **Normal ordered migration:** the clone lacks V9/V10 objects and accepts V9, V10, then V28; apply the same ordered sequence to compatible databases.
- **Validated one-time out-of-order remediation:** the clone already records V28, V9/V10 checksums match committed files, V9/V10 objects are absent or safely compatible, and all backup/startup/integrity checks pass; use temporary out-of-order for one migration run, then disable it and verify normal startup twice.
- **Compatibility migration:** V9/V10 conflict with objects/data already present; do not force the old files or edit history. Write a specifically reviewed compatibility migration that creates/adjusts only the missing safe objects and records the resulting schema contract, then rehearse it from backup on the clone.

No implementation may infer the outcome from migration filenames alone.

## 14. Temporary out-of-order safety gate

`flyway.out-of-order` remains disabled by default and after remediation. It may be enabled for one controlled clone/original migration run only when all of these are recorded:

- backup restore has been verified;
- V9 and V10 checksums equal the committed files;
- V9/V10 SQL has been compared with actual objects/data;
- no conflicting object, constraint, or index exists;
- the clone has passed Flyway validation, startup, restart, data integrity, auth, catalog, practice, profile, and ownership checks;
- the migration succeeds on a second startup with out-of-order disabled;
- the resulting schema and row-count capture match clean-database expectations.

If any gate fails, stop. Do not brute-force migration history and do not directly update `flyway_schema_history` without a documented validated reason.

## 15. Failure modes

| Failure | Required behavior |
| --- | --- |
| AI provider unavailable/429/timeout | Deterministic learning remains usable; Writing/Speaking feedback shows unavailable/retry state; no fabricated result |
| RAG insufficient context/evidence | Return normalized `INSUFFICIENT_CONTEXT` or `INSUFFICIENT_EVIDENCE`, empty/accurate sources, and no invented citation |
| Practice content missing/inactive | Return a clear unavailable/not-found state; do not fall back to unapproved or memory-only generated content |
| Attempt save failure | Keep the attempt pending/dirty, show save failure, and never report persisted submission success |
| Catalog database failure | Return a product-safe unavailable state; never claim a durable catalog result from an in-memory cache |
| Duplicate submit/retry | Use attempt/event idempotency keys and return the committed result without duplicate learning evidence |
| Session expires | Stop protected requests, preserve unsent composer/draft locally only where safe, and guide the learner to re-authenticate |
| Attachment rejected | Keep the composer usable, show normalized reason, and never upload or expose rejected content |

## 16. Expected implementation boundaries

Future implementation must extend these existing modules rather than create parallel subsystems:

### Backend

- practice catalog repository/service and learner catalog controller;
- Phase 3 publication/hydration transaction and provenance query;
- additive attempt lifecycle persistence and attempt/result controller;
- existing Reading/Listening/Writing/Speaking services and deterministic scoring;
- Phase 2 event/profile/roadmap integration;
- Tutor context resolver, conversation context assembly, and owned history handling;
- existing preferences repository/service/controller for language;
- shared Auth role/ownership helper and admin route contract;
- legacy migration compatibility rehearsal and the additive persistence migration.

### Frontend

- catalog and detail adapters;
- attempt/resume/save/submit state machine;
- result/review presentation and normalized feedback states;
- Tutor context adapter that sends IDs and stops duplicating authenticated history;
- Settings persistence consumer for authenticated language;
- admin route guard;
- LUMEN Pixel Owl Scholar launcher visual while preserving the current Tutor launcher contract.

The implementation must not add a second catalog content model, second conversation authority, second preference architecture, or vendor-specific AI response handling.

## 17. Test strategy

### Unit tests

- catalog publication eligibility and state transitions;
- approval/hydration idempotency;
- attempt lifecycle, ownership, version lock, save, submit, resume, and duplicate submit;
- deterministic Reading/Listening scoring;
- Writing/Speaking unavailable-state handling;
- Phase 2 evidence thresholds, mistake classification, and roadmap updates;
- Tutor context ownership and protected answer-key rules;
- server-authoritative authenticated history and bounded guest history;
- language preference normalization/version conflict behavior;
- admin guard states;
- Pixel Owl pupil clamping, blink/click sequence, reduced motion, keyboard, and touch fallback.

### Backend integration tests

- repository round trips for publication, attempts, results, preferences, and conversations;
- learner catalog excludes every non-approved Phase 3 state;
- approval creates one active durable publication and retry remains idempotent;
- attempt submit commits result and one learning event;
- reload/restart reads the same catalog, attempt, profile, roadmap, and conversation;
- foreign user and non-admin requests return `401/403` without data leakage;
- Tutor resolves context from IDs and ignores forged client scores/keys;
- provider/RAG failure contracts preserve deterministic flows.

### Database tests

- clean database applies V1 through V10 and V28, then the additive MVP migration;
- legacy clone backup/restore and migration rehearsal;
- temporary out-of-order gate evidence, if and only if required by clone state;
- Flyway validation and two restarts with normal out-of-order configuration disabled;
- before/after row counts and representative checksums for the key tables listed in Section 13;
- vector dimension/embedding-space metadata and existing RAG rows remain intact.

### Frontend tests

- catalog filtering/detail;
- attempt draft, resume, save failure, submit, result, and reload state;
- settings persistence and server-wins/authenticated language hydration;
- Tutor ID context and no duplicate authenticated history payload;
- admin guard direct navigation and polling suppression;
- Pixel Owl global eye tracking, pupil bounds, blink, click-to-Tutor, keyboard, reduced motion, and mobile static behavior;
- responsive layouts at 1440, 1024, 768, and 375 CSS viewports.

### E2E/smoke acceptance

Learner:

```text
register/login → choose each skill → open approved practice → start/resume
→ answer/save → submit → result/feedback → reload → progress remains
→ open contextual Tutor → verify ownership and normalized states
```

Admin:

```text
login as ADMIN → register/validate source rights → generate → review/revise
→ approve → verify one durable publication → learner sees approved item
→ restart backend → learner still sees the same item
```

No E2E test may call a live external AI provider by default. Provider calls use mocked contracts; explicitly separate manual live verification from automated tests.

## 18. Product acceptance criteria

The MVP is accepted only when all criteria below pass:

1. A learner can authenticate, choose any of the four skills, see approved practice, start/resume an owned attempt, submit, and receive the correct deterministic or normalized unavailable/AI-assisted result.
2. Reading and Listening scoring remains deterministic and survives reload/restart.
3. Writing preserves drafts/submissions and reports only provider-backed feedback or an explicit unavailable state with **Band ước lượng** for any estimate.
4. Speaking remains usable without fabricated STT/pronunciation behavior.
5. Completion updates Phase 2 evidence, profile, and roadmap according to existing gates; one mistake alone does not create a weakness.
6. Approved generated practice survives frontend reload, backend restart, and redeploy through PostgreSQL; all non-approved states remain invisible to learners.
7. Approval/hydration is idempotent and preserves provenance, source rights, version, and active/inactive publication state.
8. Authenticated Tutor history is server-authoritative, user-owned, bounded, and contextually resolved from trusted IDs. Generic chat skips RAG.
9. Admin UI is not mounted for non-admin users, while backend authorization remains authoritative.
10. Authenticated language preference persists through the existing preference contract; guest language remains local and precedence is deterministic.
11. LUMEN Pixel Owl Scholar is anchored, visibly tracks the cursor with clamped pupils on fine-pointer desktop, blinks, opens Tutor after click, supports keyboard access, and becomes static/decoratively reduced under touch or reduced motion.
12. The legacy database is preserved and passes clone rehearsal, migration validation, startup/restart, integrity, auth, practice, profile, and ownership checks before any original-database remediation.
13. No production code, database, live provider quota, or deployment is required to review this specification.

## 19. Design self-review checklist

- All sections contain concrete decisions.
- Durable catalog, attempts, preferences, and authenticated Tutor history each have one authority.
- The catalog projection does not duplicate practice content or expose non-approved states.
- AI score language, RAG grounding, STT limitations, and source rights are explicit.
- Migration strategy preserves legacy data, requires clone rehearsal, and gates temporary out-of-order use.
- Ownership, role, attachment, answer-key, status, and client-authority boundaries are explicit.
- Settings, reduced motion, accessibility, Pixel Owl interaction, and responsive behavior are implementation-testable.
- The document defines API shapes, lifecycle states, failure behavior, expected modules, tests, and acceptance criteria without requiring a future architecture decision.
