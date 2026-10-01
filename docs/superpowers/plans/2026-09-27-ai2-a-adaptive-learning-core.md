# AI Phase 2A — Adaptive Learning Core Implementation Plan

> Execution contract for `superpowers:subagent-driven-development`. This document is planning-only; it does not authorize product implementation in this task.

**Goal:** Build the server-owned adaptive learning intelligence layer that turns validated practice evidence into explainable mistakes, skill profiles, trends, and a deterministic 3–5 item roadmap without changing the existing practice contracts.

**Architecture:** Additive modular-monolith services behind the existing learning, practice, writing, speaking, auth, and Tutor boundaries. PostgreSQL is the source of truth. A normalized event ledger feeds deterministic classifiers and aggregators; LLM assistance is optional, bounded, allowlisted, and never adjudicates correctness. Every read and write is scoped by the authenticated user.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, JDBC/NamedParameterJdbcTemplate, Flyway, PostgreSQL/pgvector-compatible schema, Jackson, JUnit 5, Mockito, Spring MockMvc, existing practice/writing/speaking services and auth interceptor.

**Spec:** `docs/superpowers/specs/2026-09-26-ai-phase-2-adaptive-learning-design.md` at approved commit `6f29720`.

## Global Constraints

- Implement AI Phase 2A only. Do not implement persistent Tutor memory, proactive Tutor UI, conversation APIs, generated practice, generator classes, generated tests, STT, pronunciation analysis, official scores, or Phase 3.
- Preserve the existing `POST /api/practice/{skill}/attempts`, writing assessment, speaking text-only, `/api/me/*`, Tutor, RAG, auth, and provider-neutral frontend contracts.
- Deterministic scoring and server-side answer keys remain authoritative. An AI classification may explain evidence but cannot change correctness, score, band estimate, or completion state.
- Use the existing four first-class skills. Any AI-produced band remains nullable and explicitly an estimate; no fake value is created when evidence is insufficient.
- Event payloads are allowlisted and bounded. Never store answer keys, raw essays, audio, provider requests, reasoning, embeddings, secrets, or unnecessary PII.
- Every repository query and mutation includes authenticated `user_id`; reject IDOR, forged user IDs, foreign attempt/question/set/roadmap references, and stale ownership snapshots.
- Duplicate/replayed events are idempotent. One mistake cannot confirm a weakness; confirmation requires recurrence across attempts and the spec thresholds.
- Guest state is ephemeral and contains no authenticated learning profile. Historical backfill may use only explicitly supported historical evidence and must be marked `HISTORICAL_IMPORT`, never fabricated.
- External AI providers are mocked in automated tests and consume zero live quota. Missing credentials never prevent startup.
- Inspect the merged/base migration directory at implementation time and allocate each additive Flyway version through integration coordination. Do not hardcode a migration number in this plan or implementation branch.

## Review Focus

1. One mistake must not become a permanent weakness.
2. Duplicate/replayed event must not double-count incorrectly.
3. Deterministic answer result cannot be overridden by AI classification.
4. Another user’s evidence cannot affect profile.
5. Insufficient evidence returns `UNKNOWN`/empty state.

## Existing Integration Points

- Practice source: `backend/src/main/java/com/ieltsaitutor/practice/PracticeService.java`, `PracticeController.java`, `JdbcPracticeAttemptStore.java`, `SyntheticPracticeCatalog.java`, `PracticeAttemptResult.java`, `PracticeAttemptSnapshot.java`.
- Learning source: `backend/src/main/java/com/ieltsaitutor/learning/LearningRepository.java`, `JdbcLearningRepository.java`, `LearningProgressService.java`, `LearningController.java`.
- Writing source: `backend/src/main/java/com/ieltsaitutor/writing/WritingAssessmentService.java`, `WritingRepository.java`, `JdbcWritingRepository.java`, `WritingAssessment.java`.
- Speaking source: `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingService.java`, `SpeakingRepository.java`, `SpeakingController.java`, `SpeakingAttempt.java`; retain the text-only boundary.
- Auth and ownership: `backend/src/main/java/com/ieltsaitutor/auth/AuthPrincipal.java`, `AuthInterceptor.java`, existing controller tests and security regression tests.
- Existing frontend learning facade: `frontend/src/services/learningIntelligenceApi.js`, `frontend/src/features/learning/learningIntelligenceAdapters.js`, `frontend/src/features/learning/learningIntelligenceState.js`; do not bypass it with provider-specific payloads.

## Dependency Graph

```text
1 schema/domain contracts -> 2 event ledger/idempotency -> 3 evidence classifiers
                                      |                         |
                                      +---------------> 4 profile/issue/trend aggregation
                                                            |
                                          5 deterministic roadmap/reconciliation
                                                            |
                                             6 ownership-safe learning APIs
                                                            |
                                  7 practice/writing/speaking event integration
                                                            |
                                             8 regression/security verification
```

## Implementation Tasks

### Task 1 — Define adaptive-learning domain contracts and additive persistence boundary

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningEvent.java`, `LearningEventType.java`, `EvidenceState.java`, `IssueKind.java`, `IssueStatus.java`, `MistakeMethod.java`, `MistakeStatus.java`, `RoadmapStatus.java`, `RoadmapItemStatus.java`.
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/StudentLearningProfile.java`, `StudentSkillProfile.java`, `StudentLearningIssue.java`, `MistakeRecord.java`, `LearningRoadmap.java`, `LearningRoadmapItem.java`.
- Create `backend/src/main/resources/db/migration/<coordinated-version>__create_adaptive_learning_core.sql` at implementation time, after inspecting the merged migration directory.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/AdaptiveLearningDomainContractTest.java` and `AdaptiveLearningSchemaMigrationTest.java`.

**RED:** Add tests asserting the spec fields, enum values, nullable estimate/issue references, bounded JSON metadata, user ownership columns, event idempotency key, and additive tables/indexes. Run `cd backend; .\\mvnw.cmd -Dtest=AdaptiveLearningDomainContractTest,AdaptiveLearningSchemaMigrationTest test`; it must fail because the domain/schema does not exist.

**Implement:** Add immutable records/value objects and the additive schema for events, mistakes, profiles, skill profiles, issues, roadmaps, roadmap items, and supporting indexes/constraints. Keep vectors and existing V1–V8 untouched. Use coordinated migration allocation rather than a pinned version.

**GREEN:** Rerun the focused Maven command, then the existing learning/practice migration tests. Commit `feat: add adaptive learning core contracts`.

### Task 2 — Persist normalized learning events with idempotency and ownership

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningEventRepository.java`, `JdbcLearningEventRepository.java`, `LearningEventIngestionService.java`, `LearningEventRequest.java`, `LearningEventPayloadPolicy.java`.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/LearningEventIngestionServiceTest.java`, `JdbcLearningEventRepositoryTest.java`.

**RED:** Test accepted allowlisted events, server user ownership, bounded occurred-at skew, generated source keys, `(user_id, client_event_id)` replay no-op, duplicate source event no-op, rejection of raw answer keys/essay/audio/secrets, and cross-user event access. Run the two focused test classes; they fail because the service/repository is absent.

**Implement:** Normalize requests from authenticated callers, clamp/reject invalid timestamps according to the spec, filter payload keys and size, generate server metadata, persist atomically, and return an idempotent result without double-counting. Do not accept a client user ID as authority.

**GREEN:** Rerun focused tests and `LearningRepositoryTest`/security regressions. Commit `feat: add idempotent learning event ledger`.

### Task 3 — Classify deterministic evidence without changing scoring

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/MistakeClassifier.java`, `DeterministicMistakeClassifier.java`, `HeuristicMistakeClassifier.java`, `MistakeClassification.java`, `EvidenceNormalizer.java`, `MistakeRecordService.java`.
- Modify only the narrow result hooks in `backend/src/main/java/com/ieltsaitutor/practice/PracticeService.java`, `PracticeAttemptResult.java`, `PracticeReview.java` if needed; preserve answer verification.
- Create tests under `backend/src/test/java/com/ieltsaitutor/learning/intelligence/DeterministicMistakeClassifierTest.java`, `MistakeRecordServiceTest.java`, and update `backend/src/test/java/com/ieltsaitutor/practice/PracticeServiceTest.java` only for the additive event boundary.

**RED:** Assert Reading/Listening deterministic taxonomy mappings, Writing four criterion categories, Speaking text-only categories, unknown fallback, low-confidence behavior, duplicate classification revision, and unchanged score/correctness. Run focused tests; expected failure is missing classifier/service.

**Implement:** Normalize supported answer evidence, run deterministic classification first, use bounded heuristics only with enough evidence, optionally expose an interface for later AI assistance without calling it in this task, persist redacted records, and label unsupported/ambiguous cases `UNKNOWN`. Never use classification to override the server result.

**GREEN:** Run classifier, practice, writing, speaking, and Tutor context tests. Commit `feat: classify adaptive learning evidence deterministically`.

### Task 4 — Aggregate profiles, issues, strengths, and trends from evidence

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningIntelligenceRepository.java`, `JdbcLearningIntelligenceRepository.java`, `LearningProfileService.java`, `WeaknessStrengthAnalyzer.java`, `TrendAnalyzer.java`, `LearningIntelligenceProperties.java`.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/LearningProfileServiceTest.java`, `WeaknessStrengthAnalyzerTest.java`, `TrendAnalyzerTest.java`.

**RED:** Test the exact default thresholds from the spec, recurrence across attempts, recency window, rate/evaluated-item requirements, one-off non-confirmation, improvement windows, six-attempt/twenty-item trend minimum, four skills, `INSUFFICIENT_DATA`, and user isolation. Focused tests fail because aggregation is absent.

**Implement:** Query only the authenticated user’s normalized evidence, calculate deterministic snapshots and explainable evidence references, preserve nullable estimates, and persist/recalculate atomically. Expose configuration for the exact defaults without inventing data.

**GREEN:** Rerun focused tests plus repository integration tests against the existing test database profile. Commit `feat: add adaptive profile and trend aggregation`.

### Task 5 — Plan and reconcile deterministic 3–5 item roadmaps

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningRoadmapPlanner.java`, `RoadmapReconciliationService.java`, `RoadmapCompletionService.java`.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/LearningRoadmapPlannerTest.java`, `RoadmapReconciliationServiceTest.java`, `RoadmapCompletionServiceTest.java`.

**RED:** Assert severity/recurrence/recency/skill-gap/unfinished/trend/history ranking, stable objective identity, hysteresis, maximum 3–5 active priorities, sequence concept review → targeted practice → mixed practice → reassessment, no generated practice, ownership/status validation, and `ROADMAP_ITEM_COMPLETED` emission. Run focused tests; they fail because the planner does not exist.

**Implement:** Build a deterministic planner over stored issues/profiles, reconcile without churn, preserve completed history, validate item ownership/status server-side, and trigger only from approved evidence events. No LLM is needed to rank or complete an item.

**GREEN:** Run roadmap tests and learning event regressions. Commit `feat: add deterministic adaptive roadmap`.

### Task 6 — Expose authenticated learning intelligence APIs safely

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningIntelligenceController.java`, `LearningIntelligenceResponseMapper.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/learning/LearningController.java` only to preserve compatible `/api/me/*` behavior if required.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/LearningIntelligenceControllerTest.java`, `LearningIntelligenceSecurityTest.java`.

**RED:** Add MockMvc tests for `GET /api/learning/profile`, `/skills`, `/mistakes`, `/issues`, `/roadmap`, `/activity`, and `POST /api/learning/roadmap/items/{itemId}/complete`. Assert 401/403 behavior, query filters/limits, no foreign-user data, no secrets/raw payload, stable empty/insufficient responses, and compatibility with existing `/api/me/progress`, `/api/me/activity`, `/api/me/attempts`. Run focused tests; routes fail because the controller is absent.

**Implement:** Add authenticated routes using the existing `AuthInterceptor` principal, route every lookup through user-scoped services, validate path/query inputs, map only provider-neutral bounded DTOs, and keep guest requests unauthenticated/ephemeral rather than returning fake profile data.

**GREEN:** Rerun focused controller/security tests and existing learning controller tests. Commit `feat: expose ownership-safe learning intelligence APIs`.

### Task 7 — Integrate approved evidence sources and historical import boundary

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/learning/intelligence/LearningEvidencePipeline.java`, `HistoricalLearningImportService.java`, `LearningIntelligenceRefreshTrigger.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/practice/JdbcPracticeAttemptStore.java`, `backend/src/main/java/com/ieltsaitutor/writing/WritingAssessmentService.java`, `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingService.java`, and the narrow completion boundaries in `LearningProgressService.java`.
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/LearningEvidencePipelineTest.java`, `HistoricalLearningImportServiceTest.java`, `LearningSourceIntegrationTest.java`.

**RED:** Verify completed Reading/Listening, Writing assessment, Speaking text save, and roadmap completion emit normalized events once; incomplete attempts do not create completion evidence; historical import is explicit and marked; refresh failures do not break scoring/persistence. Run focused tests; they fail because source hooks are absent.

**Implement:** Add transactional after-save hooks or application services at existing boundaries, preserve current response timing and contracts, enqueue/recalculate deterministically, and make historical import opt-in with no fabricated evidence. Do not add audio/STT or generated exercises.

**GREEN:** Run source integration tests and all existing practice/writing/speaking tests. Commit `feat: connect adaptive intelligence to learning evidence`.

### Task 8 — AI2-A regression, migration safety, and documentation boundary

**Files:**
- Create `backend/src/test/java/com/ieltsaitutor/learning/intelligence/AdaptiveLearningRegressionTest.java`, `AdaptiveLearningMigrationCompatibilityTest.java`, `AdaptiveLearningQuotaIsolationTest.java`.
- Update only implementation-facing configuration/docs if needed: `backend/src/main/resources/application.properties`, `.env.example` or existing configuration documentation; do not add secrets.

**RED:** Add a regression matrix covering fresh migration, existing V1–V8 database compatibility, duplicate events, IDOR, no-provider startup, provider mocks with zero live quota, guest/auth separation, deterministic score preservation, all four skills, and empty/insufficient evidence. Run the focused matrix; it fails until the complete path is wired.

**Implement:** Fix only integration defects found by the matrix, document feature flags/defaults and recalculation operations, and ensure the AI2-A contract is stable for AI2-B. Do not pin migration numbers or change provider implementations.

**GREEN:** Run `cd backend; .\\mvnw.cmd test` and `.\\mvnw.cmd package`, then `git diff --check`. Commit `test: harden adaptive learning core regression`.

## AI2-A Handoff Contract to AI2-B

- Server DTOs expose only normalized, user-owned profile/skill/issue/roadmap data with explicit empty/insufficient states.
- `GET /api/learning/*` is authenticated and cannot be satisfied from client-supplied scores, IDs, or context.
- Deterministic tool inputs can consume the profile/roadmap services without calling an LLM.
- Practice, Writing, Speaking, and existing Tutor flows remain backward-compatible.
- No conversation persistence, proactive notifications, or Phase 3 generation is included.
