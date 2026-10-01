# Student-Ready IELTS AI Tutor MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the current Phase 2/Phase 3 application into a restart-safe, student-ready MVP with a durable approved-practice catalog, persistent attempts/results, truthful four-skill flows, authenticated adaptive-learning and Tutor continuity, persisted preferences, a client admin guard, and the approved LUMEN Pixel Owl Scholar without changing the existing provider-neutral or RAG architecture.

**Architecture:** PostgreSQL remains the source of truth for approved practice publication, attempts/results, learning events, authenticated Tutor conversations/messages, and authenticated preferences. Existing Phase 3 generated-practice tables remain the authoring, review, provenance, and version authority; a publication projection exposes only approved active content. Existing Phase 2 owns learning events, evidence thresholds, mistake taxonomy, profiles, roadmaps, and proactive policy. The backend resolves all ownership and Tutor context IDs. The frontend owns rendering, transient composer state, route presentation, and optimistic state only.

**Tech Stack:** Java Spring Boot, Flyway, PostgreSQL/pgvector, JDBC repositories, JUnit, React, Vite, React Router, Vitest, Testing Library, Recharts, Framer Motion, existing Academic Luxury/LUMEN design system.

**Spec:** `docs/superpowers/specs/2026-09-27-student-ready-mvp-design.md`

## Global Constraints

- [ ] Keep the existing Auth, Reading, Listening, Writing, Speaking, Phase 2 adaptive-learning, Phase 3 generator/review, RAG, Tutor, Settings, theme, and provider-neutral API architecture unless a task below names the exact boundary being extended.
- [ ] Make all schema changes additive in `V29__student_ready_mvp_persistence.sql`, after the legacy rehearsal gate in Task A; never reset, drop, delete, or blindly edit production history or user data.
- [ ] Use the existing QA database clone for implementation verification; never use the inconsistent shared development database for rehearsal or migration proof.
- [ ] Treat PostgreSQL as the only durable authority for published practice, attempts/results, learning events, authenticated Tutor history, and authenticated preferences; do not introduce a second durable frontend or process-memory authority.
- [ ] Keep generated source, generated set/version, validation, review, provenance, rights, and revision history in the existing Phase 3 tables; the learner catalog contains only a publication projection.
- [ ] Keep Reading, Listening, Writing, and Speaking equally first-class while making capability boundaries truthful: no fake audio, transcript, STT, pronunciation score, or unsupported AI score.
- [ ] Keep deterministic current-question, selected-answer, score, progress, attempt-state, and catalog requests free of LLM calls and embeddings.
- [ ] Keep generic Tutor messages from triggering RAG or embedding work; grounded Tutor context is resolved server-side from trusted IDs and ownership.
- [ ] Never expose provider-specific payloads, API keys, raw provider exceptions, answer keys, or hidden scores to the browser.
- [ ] Preserve `UNKNOWN`, evidence thresholds, ownership checks, guest/member isolation, retry behavior, reduced motion, keyboard accessibility, and no-infinite-loading guarantees.
- [ ] Run the owning targeted RED/GREEN command before the relevant implementation, then run the relevant backend or frontend full regression before committing that task.
- [ ] Use the existing QA credentials/configuration only through the repository's existing conventions. Tests mock external providers and consume zero live AI quota.

## Review Focus

| Highest-risk condition | Owning task | Required proof |
| --- | --- | --- |
| Legacy V1–V8/V28 database diverges from clean V1..V8→V9→V10→V28 assumptions | Task A | Clone rehearsal script plus `LegacySchemaCompatibilityTest`, row-count/checksum preservation, Flyway history inspection, startup/restart check |
| Duplicate submit after timeout or browser retry | Task C | `AttemptSubmissionIdempotencyTest` proves same final payload returns one result and conflicting payload is rejected |
| Stale practice version during an in-progress attempt | Task C | `AttemptVersionBindingTest` proves the bound version is used and replacement publication cannot change the attempt |
| Backend restart during a learner workflow | Task O | `StudentReadyRestartE2ETest` resumes the persisted attempt and reloads result/progress after restart |
| AI provider/RAG unavailable while deterministic flow must remain usable | Tasks L and O | `WritingProviderUnavailableTest` keeps a truthful unavailable assessment; `DeterministicPracticeWithoutAiE2ETest` completes Reading/Listening and `TutorUnavailableFallbackE2ETest` prevents infinite loading |
| Admin approval must survive restart and appear to learners only when active | Tasks B and O | `PracticePublicationRestartTest` and admin-to-learner E2E verify publication visibility and inactive exclusion |
| Tutor history must not be duplicated or client-authoritative | Task E | `TutorHistorySourceOfTruthTest` verifies bounded backend history on refresh and rejects client-supplied durable history |
| Trusted Tutor context must not accept forged answer keys/scores | Task F | `TutorContextOwnershipSecurityTest` and `TutorContextTamperingTest` verify server-side resolution and ownership |
| User must not render privileged admin UI or call privileged APIs | Task H | `AdminRouteGuardTest` and `AdminAuthorizationRegressionTest` verify both client shell suppression and backend denial |
| Pixel Owl pointer tracking must remain bounded and accessible | Task N | `PixelOwlInteractionTest` covers clamping, keyboard, touch, blink, reduced motion, and Tutor-open ordering |

---

## Task A — Legacy database preservation and rehearsal tooling

**Files:**

- Add `scripts/db/verify-legacy-backup.ps1`.
- Add `scripts/db/clone-legacy-db.ps1`.
- Add `scripts/db/inspect-flyway-state.ps1`.
- Add `scripts/db/rehearse-legacy-remediation.ps1`.
- Add `docs/runbooks/student-ready-mvp-legacy-db-rehearsal.md`.
- Add `backend/src/test/java/com/ieltsaitutor/database/LegacySchemaCompatibilityTest.java`.
- Reuse, without rewriting, `backend/src/main/resources/db/migration/V9__create_adaptive_learning_core.sql`, `V10__create_tutor_conversation_memory.sql`, `V28__create_practice_generator_schema.sql`, and the Flyway configuration.

**Interfaces consumed:** PostgreSQL connection settings, `flyway_schema_history`, existing migration files V1–V10/V28, `pg_dump`, `pg_restore`, `psql`, backend health endpoint, and the existing QA database name.

**Interfaces produced:** A non-destructive rehearsal workflow that emits backup verification, clone name, applied migration list, schema comparison, preserved-table checks, Flyway result, startup result, restart result, and an explicit original-DB remediation gate. It must not mutate the original database.

**Steps:**

- [ ] Write `LegacySchemaCompatibilityTest` with assertions for legacy `users`, roles/sessions, RAG document/version/chunk tables, Phase 2 event/mistake/profile/roadmap tables, Phase 3 source/job/review/revision/approved-set tables, Tutor conversation/message tables, learner attempt/activity tables, and the expected absence/presence of V9/V10 effects.
- [ ] Run `.\mvnw.cmd -Dtest=LegacySchemaCompatibilityTest test` and record RED as the new test/tooling contract being absent or the legacy schema comparison not yet available; do not connect the test to the original database.
- [ ] Implement the four scripts so the operator must supply explicit source and clone database names, verifies a fresh backup exists and is restorable, clones rather than drops, records `flyway_schema_history`, compares V9/V10 DDL effects to the actual legacy schema, rehearses the compatibility procedure on the clone, runs integrity queries, starts the backend against the clone, restarts it, and prints no secrets.
- [ ] Encode the safety gate in the runbook: only after backup restore, clone rehearsal, integrity checks, startup/restart, and row-preservation checks pass may an operator approve original-DB remediation; temporary `outOfOrder=true` is allowed only for the exact inspected V9/V10/V28 condition, one controlled run, immediate history verification, and explicit removal, never as a permanent setting.
- [ ] Run the script in dry-run/inspection mode against the existing QA clone and execute `.\mvnw.cmd -Dtest=LegacySchemaCompatibilityTest test`; expected GREEN includes no destructive operation, no reset/delete, no blind history edit, and all preservation assertions passing.
- [ ] Run `.\mvnw.cmd test` and `git diff --check`.
- [ ] Commit only the rehearsal tooling and runbook with `chore(db): add legacy preservation rehearsal`.

**Data-preservation checks:** Before and after the clone procedure record counts and stable identifiers for users, roles/auth sessions, RAG documents/versions/chunks, Phase 2 events/mistakes/profiles/roadmaps, Phase 3 sources/jobs/reviews/revisions/approved practice, Tutor conversations/messages, and existing learner attempts/activity. Compare `flyway_schema_history` and confirm backend startup and restart against the clone.

**Inspection commands:** The runbook must execute `psql "$QA_DB_URL" -v ON_ERROR_STOP=1 -c "SELECT installed_rank,version,description,success FROM flyway_schema_history ORDER BY installed_rank;"`, `psql "$QA_DB_URL" -v ON_ERROR_STOP=1 -c "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"`, and one count query per preservation group. It must save `pg_dump --schema-only --no-owner "$QA_DB_URL"` output for the clone comparison, compare `information_schema.columns` for the V9/V10 target tables, and run the same queries after rehearsal. Secrets and full connection URLs must be redacted from logs.

## Task B — Durable approved practice catalog

**Files:**

- Add `backend/src/main/resources/db/migration/V29__student_ready_mvp_persistence.sql` with additive publication and attempt/preference columns described in the spec; do not alter prior migrations.
- Add `backend/src/main/java/com/ieltsaitutor/practice/catalog/PracticePublication.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/catalog/PracticePublicationRepository.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/catalog/JdbcPracticePublicationRepository.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogService.java`.
- Add/modify `backend/src/main/java/com/ieltsaitutor/practice/PracticeController.java`, `PracticeService.java`, `PracticeSet.java`, and `DatabasePracticeCatalogStore.java` to consume the publication projection.
- Add `backend/src/test/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogRepositoryTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/practice/catalog/ApprovedPracticeCatalogVisibilityTest.java`.
- Extend `backend/src/test/java/com/ieltsaitutor/practice/generator/AdminReviewAndBankHydrationAcceptanceTest.java`.

**Interfaces consumed:** Existing `GeneratedPracticeSet`, `GeneratedPracticeVersion`, validation/review/provenance repositories, `PracticeBankHydrationService`, `PracticeSet` read model, existing `GET /api/practice/{skill}/sets` and detail routes, and built-in deterministic content for skills without a generator.

**Interfaces produced:** `PracticePublicationRepository.publishApproved(...)`, `findActiveBySkill(...)`, and `findActiveById(...)`; `ApprovedPracticeCatalogService.list(skill)` and `get(skill,id)`; learner responses containing only approved active practice and a stable `practiceVersion` binding.

**V29 schema contract:** Create `practice_catalog_publications` with `published_set_id`, generated set/version references, skill, active flag, publication revision, provenance reference, created/updated timestamps, and unique active publication constraints. Add only additive lifecycle/version/result/idempotency fields required by Tasks C and D to the existing attempt tables, plus the authenticated language field in the existing preferences row. Do not copy passage, question, answer, source, or review content into a second catalog table.

**Steps:**

- [ ] Write repository and visibility tests proving an approved active version is visible, draft/pending/rejected/inactive versions are excluded, provenance/revision/version identifiers survive, and repeated hydration is idempotent.
- [ ] Run `.\mvnw.cmd -Dtest=ApprovedPracticeCatalogRepositoryTest,ApprovedPracticeCatalogVisibilityTest test` and record RED because the durable publication table/repository/service does not exist.
- [ ] Add V29 publication projection tables/constraints/indexes without duplicating passage/question content; add the approved publication row only from an approved generated version or project-owned built-in deterministic content, with unique source/version and active-state constraints.
- [ ] Replace process-memory learner catalog reads with the JDBC publication repository while preserving existing compatibility endpoints and answer-key exclusion.
- [ ] Connect the existing admin approval/hydration path to idempotent publication upsert and inactive previous publication handling; never publish unapproved content.
- [ ] Run the targeted tests and `.\mvnw.cmd -Dtest=AdminReviewAndBankHydrationAcceptanceTest test`; expected GREEN proves restart-independent catalog visibility and inactive exclusion.
- [ ] Run `.\mvnw.cmd test` and commit `feat(practice): persist approved catalog publications`.

## Task C — Persistent learner attempts and results

**Files:**

- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/PracticeAttempt.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptStatus.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptRepository.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/JdbcAttemptRepository.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptService.java`.
- Add `backend/src/main/java/com/ieltsaitutor/practice/attempt/AttemptController.java`.
- Add DTOs under `backend/src/main/java/com/ieltsaitutor/practice/attempt/dto/` for create, answer update, submit, result, and conflict/error responses.
- Modify `backend/src/main/java/com/ieltsaitutor/practice/JdbcPracticeAttemptStore.java` and existing writing/speaking repositories only through explicit adapters to the new attempt contract.
- Add `backend/src/test/java/com/ieltsaitutor/practice/attempt/AttemptLifecycleTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/practice/attempt/AttemptSubmissionIdempotencyTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/practice/attempt/AttemptOwnershipSecurityTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/practice/attempt/AttemptVersionBindingTest.java`.

**Interfaces consumed:** Published practice ID/version, authenticated principal, deterministic answer evaluation, existing skill-specific persistence, and `learning_attempts`/`learning_activity` compatibility data.

**Interfaces produced:** `POST /api/practice/{practiceId}/attempts`, `GET /api/attempts/{attemptId}`, `PUT /api/attempts/{attemptId}/answers`, `POST /api/attempts/{attemptId}/submit`, and `GET /api/attempts/{attemptId}/result`; states `IN_PROGRESS → SUBMITTED → SCORED → FEEDBACK_READY`, with `FAILED` only for processing failure.

**Steps:**

- [ ] Write lifecycle, ownership, version-binding, immutable-submission, resume, same-payload idempotency, conflicting-retry rejection, and failure-transition tests.
- [ ] Run `.\mvnw.cmd -Dtest=AttemptLifecycleTest,AttemptSubmissionIdempotencyTest,AttemptOwnershipSecurityTest,AttemptVersionBindingTest test` and record RED because the new attempt endpoints/state contract is absent.
- [ ] Add V29 attempt lifecycle/version/answer/result fields and constraints to the existing skill-specific tables without removing legacy columns or data; use stable attempt IDs and unique finalization/idempotency keys.
- [ ] Implement the repository and service with ownership checks, immutable submitted payloads, bound practice version, transactional result persistence, retry-safe finalization, and safe failure status.
- [ ] Keep deterministic Reading/Listening scoring in the backend; route Writing/Speaking result persistence through truthful assessment contracts without inventing unsupported scores.
- [ ] Run targeted tests and `.\mvnw.cmd -Dtest=PracticeServiceStudentBoundaryTest,SyntheticPracticeCatalogFederationTest test`; expected GREEN proves old endpoints remain compatible.
- [ ] Run `.\mvnw.cmd test` and commit `feat(practice): persist learner attempts and results`.

## Task D — Phase 2 learning-event integration

**Files:**

- Add `backend/src/main/java/com/ieltsaitutor/learning/intelligence/AttemptLearningEventPublisher.java`.
- Add `backend/src/main/java/com/ieltsaitutor/learning/intelligence/AttemptLearningEvent.java` if the existing event payload cannot represent the stable attempt/result reference.
- Modify `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningEvidencePipeline.java`, `LearningEventIngestionService.java`, `LearningEventRepository.java`, `JdbcLearningEventRepository.java`, and `JdbcPracticeAttemptStore.java`.
- Add `backend/src/test/java/com/ieltsaitutor/learning/intelligence/AttemptLearningEventIntegrationTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/learning/intelligence/AttemptEventIdempotencyTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/learning/intelligence/Phase2OwnershipAndUnknownRegressionTest.java`.

**Interfaces consumed:** Finalized attempt/result transaction, current `LearningEventType`, evidence pipeline, mistake classifier, student profiles, skill profiles, issue records, roadmaps, and existing ownership rules.

**Interfaces produced:** One idempotent final-attempt event per finalized attempt, durable before the attempt transaction commits; existing Phase 2 projections consume that event and remain retryable without duplicate taxonomy/profile/roadmap engines.

**Steps:**

- [ ] Write tests proving a finalized attempt writes one durable learning event before commit, replay is idempotent, downstream projection failure is retryable, guest data is isolated, ownership is enforced, and `UNKNOWN` plus evidence thresholds remain unchanged.
- [ ] Run `.\mvnw.cmd -Dtest=AttemptLearningEventIntegrationTest,AttemptEventIdempotencyTest,Phase2OwnershipAndUnknownRegressionTest test` and record RED because attempt finalization does not yet publish the required stable event contract.
- [ ] Implement the publisher through the existing repository/pipeline transaction boundary; use the attempt ID, skill, practice/version, score/result reference, and structured answer evidence as the event payload.
- [ ] Remove best-effort swallowing for the durable event write while retaining safe retry behavior for projections; do not introduce a second profile, mistake taxonomy, roadmap, or proactive engine.
- [ ] Run targeted tests and existing Phase 2 suites including `LearningEvidencePipelineTest`, `LearningIntelligenceServiceTest`, and `LearningProfileServiceTest`; expected GREEN preserves all existing semantics.
- [ ] Run `.\mvnw.cmd test` and commit `feat(learning): integrate finalized attempts with phase2 evidence`.

## Task E — Tutor history backend source of truth

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/ai/controller/AiChatController.java`, `backend/src/main/java/com/ieltsaitutor/ai/service/AiChatService.java`, `backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatRequest.java`, and conversation DTOs.
- Reuse/extend `backend/src/main/java/com/ieltsaitutor/tutor/memory/` and the existing `ai_conversations`, `ai_messages`, `ai_conversation_summaries` repositories/services; add only the missing bounded-history repository interface/implementation under that package.
- Modify `frontend/src/services/aiTutorApi.js`, `frontend/src/components/tutor/FloatingTutor.jsx`, `frontend/src/components/tutor/TutorMessageList.jsx`, and `frontend/src/features/tutor/` only for transient state and server reload.
- Add `backend/src/test/java/com/ieltsaitutor/tutor/memory/TutorHistorySourceOfTruthTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/tutor/memory/TutorHistoryOwnershipTest.java`.
- Extend `frontend/src/__tests__/tutor-memory.test.jsx`.

**Interfaces consumed:** Existing conversation CRUD/archive endpoints, authenticated principal, backend bounded memory tables, provider-neutral `AiChatRequest`/`AiChatResponse`, and the current Tutor panel/composer contracts.

**Interfaces produced:** With `conversationId`, backend loads bounded durable history and persists the new user/assistant messages; authenticated frontend refresh loads summary/messages from the server and sends no durable client history. Guest chat keeps only bounded ephemeral history.

**Steps:**

- [ ] Write backend tests rejecting client-supplied durable history, enforcing conversation ownership, bounding loaded history, persisting assistant/error state, and reloading the same conversation after a simulated refresh; add frontend test that refreshes from API rather than rebuilding durable history from local messages.
- [ ] Run `.\mvnw.cmd -Dtest=TutorHistorySourceOfTruthTest,TutorHistoryOwnershipTest test` and `npm test -- --run src/__tests__/tutor-memory.test.jsx`; record RED because the current request forwards frontend `messages` as history for authenticated conversations.
- [ ] Make the backend conversation repository the authority; accept only the trusted conversation ID and current user message, load bounded messages server-side, and persist response/error status atomically with existing retry semantics.
- [ ] Remove authenticated durable-history forwarding from `FloatingTutor`; retain local optimistic rendering, composer, pending, loading, retry, attachment, and panel state only.
- [ ] Run targeted tests plus `npm test -- --run src/__tests__/tutor-memory.test.jsx src/__tests__/tutor-request-state.test.jsx src/__tests__/tutor-attachments.test.jsx` and `.\mvnw.cmd test`.
- [ ] Commit `feat(tutor): make backend conversation history authoritative`.

## Task F — Trusted contextual Tutor integration

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/tutor/context/TutorContextRequest.java`, `TutorLearningContext.java`, `DefaultTutorContextService.java`, `TutorContextService.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/tutor/intent/DefaultTutorIntentRouter.java`, `TutorIntentRouter.java`, `backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java`, and `backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatContext.java`.
- Add context resolvers under `backend/src/main/java/com/ieltsaitutor/tutor/context/` for practice/version/attempt/question ownership.
- Modify `frontend/src/features/tutor/TutorReferenceResolver.js`, `TutorReferenceRegistry.js`, `frontend/src/services/aiTutorApi.js`, and the four skill pages only to send trusted IDs.
- Add `backend/src/test/java/com/ieltsaitutor/tutor/context/TutorContextOwnershipSecurityTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/tutor/context/TutorContextTamperingTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/tutor/TutorDeterministicIntentTest.java`.
- Extend `frontend/src/__tests__/tutor-references.test.jsx` and `frontend/src/__tests__/tutor-rag.test.jsx`.

**Interfaces consumed:** `conversationId`, `skill`, `practiceId`, `practiceVersion`, `attemptId`, and `questionId`; published catalog, attempts/results, Phase 2 profile/mistakes/roadmap, RAG grounding, deterministic Tutor tools, provider-neutral AI response, and current reference registry.

**Interfaces produced:** A server-resolved `TutorLearningContext` containing only owned, current, permitted context; deterministic intents for current question, selected answer, score, progress, and navigation; contextual AI/RAG orchestration for explanatory questions; grounded/insufficient-evidence response states with citations.

**Steps:**

- [ ] Write RED tests proving forged answer keys/scores are ignored/rejected, cross-user IDs return an authorization-safe response, stale versions are rejected, generic `hello` skips RAG/embedding, deterministic app-data questions skip LLM, and grounded questions use only retrieved context.
- [ ] Run `.\mvnw.cmd -Dtest=TutorContextOwnershipSecurityTest,TutorContextTamperingTest,TutorDeterministicIntentTest test` and `npm test -- --run src/__tests__/tutor-references.test.jsx src/__tests__/tutor-rag.test.jsx`; record the expected missing trust boundary/route behavior.
- [ ] Implement server-side ID resolution and ownership checks; pass resolved context to existing orchestration without accepting client answer keys, scores, bands, raw content, or provider payloads.
- [ ] Route deterministic intents directly to deterministic tools; route generic chat directly through normal provider selection; route context-dependent explanation through existing RAG governance and citation contracts.
- [ ] Add frontend response-state rendering for `ANSWERED`, `INSUFFICIENT_CONTEXT`, `INSUFFICIENT_EVIDENCE`, provider-unavailable, retryable, and complete loading/error termination without vendor coupling.
- [ ] Run targeted backend/frontend tests and the existing Tutor/RAG regression suites; expected GREEN preserves source chips, trust states, insufficient context, and no infinite loading.
- [ ] Run `.\mvnw.cmd test` and `npm test -- --run`; commit `feat(tutor): add trusted contextual orchestration`.

## Task G — Authenticated preference persistence

**Files:**

- Add the language field/compatibility constraint in `V29__student_ready_mvp_persistence.sql` using the existing `user_preferences` row.
- Modify `backend/src/main/java/com/ieltsaitutor/preferences/UserPreferences.java`, `JdbcUserPreferencesRepository.java`, `UserPreferencesService.java`, and `UserPreferencesController.java`.
- Modify `frontend/src/features/preferences/preferenceSchema.js`, `preferenceDefaults.js`, `preferencesApi.js`, `PreferenceProvider.jsx`, `preferenceStorage.js`, and `uiTranslations.js`.
- Add `backend/src/test/java/com/ieltsaitutor/preferences/AuthenticatedLanguagePreferenceTest.java`.
- Extend `frontend/src/__tests__/preferences-persistence.test.jsx` and `frontend/src/__tests__/preferences.test.jsx`.

**Interfaces consumed:** Existing authenticated GET/PUT `/api/user/preferences`, guest local storage, current theme/accent/density/reduced-motion/font/proactive/cross-highlighting/countdown/split-ratio settings.

**Interfaces produced:** Authenticated language is loaded/saved server-side with the existing preference envelope; guest language remains local. Existing preference fields and invalid-value normalization remain compatible.

**Steps:**

- [ ] Write backend and frontend RED tests for authenticated load/save/refresh, guest local fallback, invalid language normalization, and preservation of all existing settings fields.
- [ ] Run `.\mvnw.cmd -Dtest=AuthenticatedLanguagePreferenceTest test` and `npm test -- --run src/__tests__/preferences-persistence.test.jsx src/__tests__/preferences.test.jsx`; record RED for the missing language persistence field/adapter.
- [ ] Add the language field to the existing schema/DTO/repository and wire it through the existing provider without replacing Settings or local guest behavior.
- [ ] Run targeted tests and `npm test -- --run src/__tests__/preferences*.test.jsx src/__tests__/settings-drawer.test.jsx`; expected GREEN proves theme, accent, density, animation, font size, proactive AI, cross-highlighting, countdown, and 40/60, 50/50, 60/40 split settings survive.
- [ ] Run `.\mvnw.cmd test` and commit `feat(settings): persist authenticated language preference`.

## Task H — Admin client route guard

**Files:**

- Add `frontend/src/features/auth/RequireAdminRoute.jsx`.
- Add `frontend/src/features/auth/authorization.js` if a small provider-neutral role predicate is needed.
- Modify `frontend/src/App.jsx` and existing admin route imports only to wrap `/admin/rag`, `/admin/practice-generator`, and `/admin/practice-generator/sets/:setId`.
- Add `frontend/src/__tests__/AdminRouteGuard.test.jsx`.
- Extend `frontend/src/__tests__/auth-gate.test.jsx` and `frontend/src/__tests__/AdminGeneratorFlowIntegration.test.jsx`.
- Extend backend authorization coverage in `backend/src/test/java/com/ieltsaitutor/auth/` or the existing admin controller tests without weakening backend security.

**Interfaces consumed:** Existing `/api/auth/me` session/role response, React Router location/navigation, current admin pages, and backend admin authorization/interceptors.

**Interfaces produced:** Non-admin users render neither the privileged admin page shell nor privileged content; authenticated admins reach the current pages; unauthenticated users follow existing login behavior. Backend remains the authority and continues denying non-admin API calls.

**Steps:**

- [ ] Write RED tests that render each admin route as guest, learner, and admin; assert no admin shell for guest/learner, correct redirect/pending behavior, and unchanged admin rendering for admin. Add a backend test that learner API calls remain denied.
- [ ] Run `npm test -- --run src/__tests__/AdminRouteGuard.test.jsx src/__tests__/auth-gate.test.jsx` and the relevant backend authorization test; record RED because routes currently render admin pages without a client role guard.
- [ ] Implement one reusable route guard around existing admin routes, using the existing auth state and a safe loading state; do not add role escalation or public admin registration.
- [ ] Run targeted tests and `npm test -- --run src/__tests__/Admin*.test.jsx src/__tests__/auth*.test.jsx`; expected GREEN preserves backend denial and admin flows.
- [ ] Run `.\mvnw.cmd test`, `npm test -- --run`, and commit `fix(frontend): guard admin routes by role`.

## Task I — Learner catalog, detail, attempt, and result frontend

**Files:**

- Add `frontend/src/services/practiceCatalogApi.js` and `frontend/src/services/attemptsApi.js`.
- Add `frontend/src/pages/PracticeCatalogPage.jsx`.
- Add `frontend/src/pages/PracticeDetailPage.jsx`.
- Add `frontend/src/pages/PracticeAttemptPage.jsx`.
- Add `frontend/src/pages/PracticeResultPage.jsx`.
- Add `frontend/src/components/practice/PracticeCatalogCard.jsx`, `PracticeAttemptStatus.jsx`, `PracticeResultSummary.jsx`, and `PracticeErrorState.jsx`.
- Modify `frontend/src/App.jsx`, `frontend/src/pages/HomePage.jsx`, and existing shared navigation/CTA links to use the new routes while preserving compatibility routes.
- Add `frontend/src/__tests__/practice-catalog.test.jsx`.
- Add `frontend/src/__tests__/practice-attempt.test.jsx`.
- Add `frontend/src/__tests__/practice-result.test.jsx`.

**Interfaces consumed:** Approved catalog endpoints, attempt lifecycle endpoints, authenticated session, skill/version/attempt/result DTOs, existing GlassCard/Button/SectionTitle/AnimatedSection/shared workspace components.

**Interfaces produced:** Keyboard-accessible catalog/detail/start/resume/result screens; visible skill, version, status, deterministic score/result, unavailable assessment state, retry/resume controls, and no answer-key leakage.

**Steps:**

- [ ] Write RED tests for active-only catalog rendering, four-skill navigation, start/resume, answer save, submit, result reload, ownership-safe errors, immutable submitted state, complete loading/error paths, and keyboard interaction.
- [ ] Run `npm test -- --run src/__tests__/practice-catalog.test.jsx src/__tests__/practice-attempt.test.jsx src/__tests__/practice-result.test.jsx`; record RED because the learner catalog/detail/attempt/result pages and API adapters do not exist.
- [ ] Implement provider-neutral API adapters and route pages using existing shared components; keep transient draft state separate from server attempt authority and preserve Academic Luxury styling.
- [ ] Run targeted tests and `npm test -- --run src/__tests__/practice*.test.jsx src/__tests__/reading-workspace.test.jsx src/__tests__/split-workspace.test.jsx`; expected GREEN proves compatibility with existing practice UI.
- [ ] Run `npm run lint`, `npm run build`, and commit `feat(frontend): add durable learner practice flow`.

## Task J — Reading end-to-end flow

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/practice/PracticeService.java`, `PracticeController.java`, and Reading read-model adapters only to use the persistent catalog/attempt services.
- Modify `frontend/src/pages/PracticePage.jsx`, `frontend/src/components/workspace/SplitLearningWorkspace.jsx`, `ReadingPassagePane`, `ReadingQuestionPane`, and their existing adapters.
- Add `backend/src/test/java/com/ieltsaitutor/practice/ReadingAttemptEndToEndTest.java`.
- Add `frontend/src/__tests__/reading-attempt-flow.test.jsx`.

**Interfaces consumed:** Persistent Reading publication, bound version, deterministic evaluator, answer-save/submit/result APIs, cross-highlighting references, countdown settings, and trusted Tutor context IDs.

**Interfaces produced:** Reading browse/start/resume/answer/submit/result flow with deterministic scoring, countdown behavior, cross-highlighting, persisted progress, and contextual Tutor launch carrying IDs only.

**Steps:**

- [ ] Write RED backend/frontend tests for passage/question rendering, answer persistence/reload, timer pause/expiry semantics, deterministic score, immutable submission, cross-highlighting, progress event, and Tutor context.
- [ ] Run `.\mvnw.cmd -Dtest=ReadingAttemptEndToEndTest test` and `npm test -- --run src/__tests__/reading-attempt-flow.test.jsx`; record RED for the new durable attempt flow.
- [ ] Adapt the existing Reading workspace to the new attempt contract without moving scoring to the browser or exposing answer keys.
- [ ] Run targeted tests plus `.\mvnw.cmd -Dtest=PracticeServiceStudentBoundaryTest test` and `npm test -- --run src/__tests__/practice.test.jsx src/__tests__/reading-workspace.test.jsx src/__tests__/cross-highlighting.test.jsx`; expected GREEN preserves current Reading behavior.
- [ ] Run `.\mvnw.cmd test`, `npm run lint`, `npm run build`, and commit `feat(reading): complete persistent learner flow`.

## Task K — Listening truthful end-to-end flow

**Files:**

- Add/modify `backend/src/main/java/com/ieltsaitutor/listening/ListeningPracticeService.java`, repository/adapters, and `ListeningPracticeController.java` only if the existing implementation has no durable catalog adapter.
- Modify `frontend/src/pages/PracticePage.jsx` and add `frontend/src/components/listening/ListeningPracticeWorkspace.jsx`.
- Add `backend/src/test/java/com/ieltsaitutor/listening/ListeningAttemptEndToEndTest.java`.
- Add `frontend/src/__tests__/listening-attempt-flow.test.jsx`.

**Interfaces consumed:** Approved Listening publication, attempt lifecycle, deterministic scoring, existing media capability contract, authenticated Tutor context, and shared practice workspace components.

**Interfaces produced:** Listening start/resume/submit/result behavior that uses real project-owned media when available and otherwise exposes a truthful unavailable-media state; it must not fabricate audio or transcript capability.

**Steps:**

- [ ] Write RED tests for approved-only Listening catalog, deterministic answer scoring, persistence/resume, media absence state, no fake transcript/media, progress event, and contextual Tutor IDs.
- [ ] Run `.\mvnw.cmd -Dtest=ListeningAttemptEndToEndTest test` and `npm test -- --run src/__tests__/listening-attempt-flow.test.jsx`; record RED because the current page uses fixture/synthetic text and has no durable end-to-end contract.
- [ ] Implement the truthful Listening workspace against the shared attempt APIs; keep the “audio not configured” boundary explicit where no approved media exists.
- [ ] Run targeted tests plus `npm test -- --run src/__tests__/practice.test.jsx src/__tests__/practice-catalog.test.jsx`; expected GREEN keeps Listening first-class without fake capability.
- [ ] Run `.\mvnw.cmd test`, `npm run lint`, `npm run build`, and commit `feat(listening): complete truthful learner flow`.

## Task L — Writing end-to-end flow

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/writing/WritingController.java`, `WritingAssessmentService.java`, `WritingRepository.java`, `JdbcWritingRepository.java`, `WritingTask.java`, and assessment DTOs.
- Modify `frontend/src/pages/WritingPage.jsx`, `frontend/src/components/workspace/WritingEditorPane.jsx`, `WritingPromptPane.jsx`, and existing draft API adapters only for the persistent attempt contract.
- Add `backend/src/test/java/com/ieltsaitutor/writing/WritingAttemptEndToEndTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/writing/WritingProviderUnavailableTest.java`.
- Add `frontend/src/__tests__/writing-attempt-flow.test.jsx`.

**Interfaces consumed:** Writing Task 1/Task 2 prompts, drafts, attempt lifecycle, AI provider-neutral assessment response, and existing criteria parser.

**Interfaces produced:** Writing Task 1 uses Task Achievement, CC, LR, GRA; Task 2 uses Task Response, CC, LR, GRA. Scores are always labeled `Band ước lượng`; missing/invalid provider output yields unavailable assessment with preserved submission and retryable state.

**Steps:**

- [ ] Write RED tests for task type criteria, draft save/reload, submit immutability, provider success parsing, invalid response, provider timeout/429/unavailable, estimated-band labeling, and no fabricated feedback.
- [ ] Run `.\mvnw.cmd -Dtest=WritingAttemptEndToEndTest,WritingProviderUnavailableTest test` and `npm test -- --run src/__tests__/writing-attempt-flow.test.jsx`; record RED for the unified persistent attempt/feedback contract.
- [ ] Adapt existing Writing assessment/repository to persist the attempt binding and truthful feedback state while retaining current draft conflict semantics and no live-provider dependency in tests.
- [ ] Run targeted tests plus `npm test -- --run src/__tests__/writing*.test.jsx src/__tests__/writing-drafts.test.jsx`; expected GREEN proves Task 1/2 criteria and provider failure handling.
- [ ] Run `.\mvnw.cmd test`, `npm run lint`, `npm run build`, and commit `feat(writing): persist truthful assessment attempts`.

## Task M — Speaking truthful supported flow

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingController.java`, `SpeakingService.java`, `SpeakingRepository.java`, `JdbcSpeakingRepository.java`, and `SpeakingAttempt.java` only to bind persistent attempt/version/context state.
- Modify `frontend/src/pages/SpeakingPage.jsx`, `frontend/src/components/speaking/SpeakingRoom.jsx`, `SpeakingPromptCard.jsx`, `SpeakingTimer.jsx`, and existing audio/transcript controls.
- Add `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingAttemptEndToEndTest.java`.
- Add `backend/src/test/java/com/ieltsaitutor/speaking/SpeakingCapabilityBoundaryTest.java`.
- Add `frontend/src/__tests__/speaking-attempt-flow.test.jsx`.

**Interfaces consumed:** Approved Speaking prompts, transcript/audio attempt persistence, existing optional `SpeechToTextProvider`, timer, draft/history UI, and Tutor context IDs.

**Interfaces produced:** Manual transcript flow remains usable; audio is stored/processed only when the existing capability exists; absent STT is explicit; no fake transcript, pronunciation score, or band is emitted. Future Groq Whisper is not required.

**Steps:**

- [ ] Write RED tests for prompt load, manual transcript save/submit, optional audio capability, `STT_NOT_CONFIGURED`, absence of pronunciation claims, persistent result/history, and Tutor context.
- [ ] Run `.\mvnw.cmd -Dtest=SpeakingAttemptEndToEndTest,SpeakingCapabilityBoundaryTest test` and `npm test -- --run src/__tests__/speaking-attempt-flow.test.jsx`; record RED for the unified attempt/result behavior.
- [ ] Adapt the existing Speaking service/UI to the persistent attempt contract and truthful capability states without inventing media analysis.
- [ ] Run targeted tests plus `npm test -- --run src/__tests__/speaking*.test.jsx src/__tests__/speaking-room.test.jsx`; expected GREEN preserves timer, recording, transcript, and history behavior.
- [ ] Run `.\mvnw.cmd test`, `npm run lint`, `npm run build`, and commit `feat(speaking): complete truthful supported flow`.

## Task N — LUMEN Pixel Owl Scholar

**Files:**

- Replace the visual implementation in `frontend/src/components/tutor/LumenPixelScholarMascot.jsx` with the approved project-owned inline SVG/CSS/React pixel owl while preserving the public launcher contract.
- Modify `frontend/src/components/tutor/AiTutorMascotLauncher.jsx` only to connect global page-wide pointer tracking, bounded pupil variables, head reaction, keyboard/touch fallback, click blink-before-open, tooltip, and reduced-motion behavior.
- Modify the existing mascot styles in `frontend/src/styles/globals.css` or the owning component stylesheet; do not add a new illustration dependency.
- Add `frontend/src/__tests__/pixel-owl-scholar.test.jsx`.
- Extend `frontend/src/__tests__/tutor-shell.test.jsx`, `tutor.test.jsx`, and `ui-polish.test.jsx`.

**Interfaces consumed:** Existing `AiTutorMascotLauncher` props/callbacks, global pointer event behavior, `PreferenceProvider` reduced-motion setting, theme tokens, Tutor panel open callback, keyboard focus, and touch pointer fallback.

**Interfaces produced:** Crisp pixel owl with warm ivory face, navy cap/body, champagne trim/tassel, large expressive pupils that clamp inside eye bounds, subtle head reaction, idle blink, click blink before Tutor open then bounce, tooltip, keyboard activation, touch-safe static fallback, and no motion when reduced motion is enabled.

**Steps:**

- [ ] Write RED tests asserting owl structure/labels, pointer left/right/up/down changes bounded pupil variables, mascot anchor does not translate with pointer, keyboard Enter/Space opens Tutor, touch remains usable, idle/click blink ordering, tooltip, and reduced-motion behavior.
- [ ] Run `npm test -- --run src/__tests__/pixel-owl-scholar.test.jsx src/__tests__/tutor-shell.test.jsx`; record RED because the current mascot is not the approved Pixel Owl Scholar structure.
- [ ] Implement the inline pixel owl and connect the existing launcher state without changing Tutor behavior, Settings, Header, Auth, or route contracts.
- [ ] Run targeted tests and `npm test -- --run src/__tests__/tutor*.test.jsx src/__tests__/ui-polish.test.jsx src/__tests__/preferences*.test.jsx`; expected GREEN covers accessibility, motion, and Tutor opening.
- [ ] Run `npm run lint`, `npm run build`, and commit `feat(ui): add lumen pixel owl scholar`.

## Task O — End-to-end, restart, and legacy-database verification

**Files:**

- Add backend integration tests under `backend/src/test/java/com/ieltsaitutor/acceptance/StudentReadyLearnerFlowE2ETest.java`, `StudentReadyAdminFlowE2ETest.java`, `StudentReadyRestartE2ETest.java`, and `DeterministicPracticeWithoutAiE2ETest.java`.
- Add frontend/browser tests under `frontend/src/__tests__/student-ready-learner-flow.test.jsx`, `student-ready-admin-flow.test.jsx`, and `student-ready-role-flow.test.jsx`.
- Add `docs/runbooks/student-ready-mvp-qa-checklist.md`.
- Reuse Task A scripts and the existing QA database; do not add a new migration or seed that mutates the shared development database.

**Interfaces consumed:** All approved catalog/attempt/result endpoints, admin generator/review/approve endpoints, auth/session/role endpoints, Phase 2 progress endpoints, Tutor conversation/context endpoints, settings endpoints, and the existing backend/frontend start commands.

**Interfaces produced:** Evidence-backed acceptance for learner, admin, role, restart, and AI-unavailable paths.

**Steps:**

- [ ] Write RED acceptance tests for learner login → skill → approved catalog → start → save/reload → answer → submit → result → progress → backend restart → persisted result/progress → contextual Tutor question.
- [ ] Write RED acceptance tests for admin login → generate → validate → review → approve → backend restart → learner sees approved practice, plus learner UI/API denial for admin routes.
- [ ] Write RED tests that mock all AI providers/RAG as unavailable and still complete deterministic Reading/Listening; assert Writing/Speaking truthful unavailable boundaries and no infinite loading.
- [ ] Run the backend/frontend targeted acceptance commands and record RED against missing end-to-end contracts.
- [ ] Implement only the wiring required by Tasks B–N to satisfy these acceptance tests; do not add new architecture or live-provider dependencies.
- [ ] Run the full QA sequence against the existing cloned/approved QA database: `.\mvnw.cmd test`, `.\mvnw.cmd package`, `npm test -- --run`, `npm run lint`, `npm run build`, `git diff --check`, and the browser smoke suite.
- [ ] Run backend shutdown/startup and repeat catalog, attempt, result, progress, approved-publication, and Tutor-history assertions; record Flyway version and V29 vector/metadata state without changing legacy data.
- [ ] Commit `test: verify student-ready learner and admin flows`.

## Task P — Final regression, review, and handoff

**Files:**

- Review all files changed by Tasks A–O; no new production file is permitted outside the task ownership lists.
- Update `docs/runbooks/student-ready-mvp-qa-checklist.md` only for verified commands/results, not for unresolved placeholders.

**Interfaces consumed:** The complete implementation, spec, plan, existing test suites, migration history, QA database rehearsal evidence, and git history.

**Interfaces produced:** A clean, reviewable branch with no unresolved Important/Critical findings and a reproducible student-ready MVP verification report.

**Steps:**

- [ ] Run `git status --short`, `git diff --check`, `git log --oneline -20`, and inspect every changed file for scope, ownership, security, and duplicate-authority violations.
- [ ] Run `.\mvnw.cmd test`, `.\mvnw.cmd package`, `npm test -- --run`, `npm run lint`, and `npm run build`; expected GREEN with no live quota use.
- [ ] Re-run the Review Focus checks: clone-only legacy rehearsal, duplicate submit, stale version, restart continuity, AI/RAG unavailable deterministic flow, admin publication restart, Tutor history trust, context tampering, admin route/API denial, and Pixel Owl bounded interaction.
- [ ] Verify exactly one H1 per public page, focus-visible states, ARIA labels, reduced motion, no horizontal overflow at 375/768/1024/1440, no fake claims, no raw provider errors, and no answer-key leakage.
- [ ] Confirm Phase 1/2/3 scope boundaries: no real auth replacement, no new real Spring API provider contract, no PostgreSQL/pgvector redesign, no STT requirement, no unsupported full IELTS module, and no AI Phase 2/3 feature expansion beyond this MVP contract.
- [ ] Fix only confirmed Critical/Important findings with a new RED test first; do not polish unrelated visuals or change approved architecture.
- [ ] Run the complete verification sequence again after any fix and require `git diff --check` plus a clean worktree.
- [ ] Commit the final verified implementation as `chore: finalize student-ready mvp regression` only when all checks pass.

## Plan Self-Review

- [ ] Coverage check: Tasks A–P map directly to every phase in the approved spec and requested implementation order.
- [ ] File check: every production behavior task names exact existing or new backend/frontend/test/runbook files.
- [ ] Contract check: every persistence, API, UI, Tutor, Phase 2, and admin boundary names consumed and produced interfaces.
- [ ] TDD check: every production behavior task has a named RED test command, expected failure reason, minimal implementation step, targeted GREEN command, relevant regression, and commit message.
- [ ] Safety check: legacy original DB remediation is gated behind backup, clone, schema comparison, rehearsal, integrity, startup, restart, and explicit approval; no reset/drop/history rewrite appears in the implementation sequence.
- [ ] Authority check: publication, attempts, learning events, Tutor history, and authenticated preferences each have one durable authority; transient frontend state is explicitly non-authoritative.
- [ ] Truthfulness check: four skills, estimates, STT, RAG, provider failures, and deterministic tools are bounded by the approved capability contracts.
- [ ] Review Focus check: every highest-risk condition in the plan header maps to an actual owning test.
- [ ] Scope check: no production code, migration, database, push, merge, or deployment is performed while creating this plan.
