# AI Phase 2B — Tutor Memory and Adaptive Experience Implementation Plan

> Execution contract for `superpowers:subagent-driven-development`. This document is planning-only; it does not authorize product implementation in this task.

**Goal:** Integrate AI Tutor conversation memory and adaptive learning context into the existing provider-neutral Tutor while preserving deterministic answers, RAG governance, guest safety, ownership, and the Academic Luxury frontend shell.

**Architecture:** Add authenticated conversation persistence and bounded summaries behind the existing `AiChatController -> AiChatService -> TutorOrchestrator` path. The server resolves learning context and conversation ownership; the browser sends references only. Deterministic app-data tools and scope routing run before RAG/provider calls. Guest conversations remain ephemeral, while authenticated conversations are user-scoped and explicitly archived.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC, JDBC, Flyway, PostgreSQL/pgvector-compatible existing RAG schema, Jackson, existing provider-neutral `AiProvider`/`RagChatService`, React, React Router, Vitest, Testing Library, existing Academic Luxury components and SkeletonBlock.

**Spec:** `docs/superpowers/specs/2026-09-26-ai-phase-2-adaptive-learning-design.md` at approved commit `6f29720`.

## Global Constraints

- Implement AI Phase 2B only and depend on the AI2-A learning contracts. Do not implement Phase 3 generator classes, generated tests, persistent audio/STT, pronunciation analysis, official scores, real auth changes, provider SDKs, or redesign the Tutor shell.
- Preserve `POST /api/ai/chat`, normalized response fields, RAG source chips/citations, insufficient-context behavior, current provider router/fallback/timeout semantics, deterministic application-data answers, and existing route compatibility.
- Frontend code must remain vendor-neutral: no Groq/Cloudflare/Gemini payload shapes, keys, provider names, or raw errors. External providers are mocked in all automated tests and use zero live quota.
- Server-side trusted context is authoritative. Reject forged answer keys, scores, bands, user IDs, conversation IDs, attempt IDs, roadmap IDs, and private history. Every conversation/message/summary query includes authenticated `user_id`.
- Guest history is memory-only and is never persisted as authenticated memory. Login/logout/account switch must clear or rebind client state safely; never merge guest and member histories implicitly.
- Conversation history is bounded: recent visible messages, summary trigger, summary token cap, retention cap, bounded citations/context snapshots. Never persist raw provider requests, headers, reasoning, embeddings, full documents, or unnecessary PII.
- Deterministic current question, selected answer, score, progress, roadmap completion, and similar app-data requests never call an LLM. Generic `hello` skips RAG/embedding but may use the configured chat provider.
- Unsupported scope returns the exact approved redirect; ambiguous English-relevant requests remain bounded and provider-neutral. Missing evidence returns structured `INSUFFICIENT_CONTEXT`/`INSUFFICIENT_EVIDENCE`, not fabricated learning intelligence.
- Migration versions are not pinned. Inspect the merged/base migration directory at implementation time and allocate each additive Flyway version through integration coordination.

## Review Focus

1. Conversation leakage between users.
2. Frontend forged context/answer key rejected.
3. Guest history not persisted as authenticated memory.
4. Proactive Tutor only fires from real evidence/preference.
5. Dashboard never fabricates missing learning intelligence.

## Existing Integration Points

- Backend chat entry: `backend/src/main/java/com/ieltsaitutor/ai/controller/AiChatController.java`, `ai/service/AiChatService.java`, `ai/dto/AiChatRequest.java`, `AiChatResponse.java`.
- Tutor path: `backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java`, `tutor/context/TutorContextRequest.java`, `DefaultTutorContextService.java`, `tutor/intent/DefaultTutorIntentRouter.java`, `tutor/tool/DeterministicTutorTools.java` and existing tool classes.
- RAG/provider contracts: existing `backend/src/main/java/com/ieltsaitutor/rag/**` and `backend/src/main/java/com/ieltsaitutor/ai/provider/**`; do not expose provider DTOs to React.
- Auth: `backend/src/main/java/com/ieltsaitutor/auth/AuthPrincipal.java`, `AuthInterceptor.java`, logout/session behavior and security regressions.
- Frontend Tutor: `frontend/src/services/aiTutorApi.js`, `frontend/src/components/tutor/FloatingTutor.jsx`, `TutorPanel.jsx`, `TutorShell.jsx`, `TutorMessage*.jsx`, `TutorGroundingBadge.jsx`, `frontend/src/features/learning/**`, `HomePage.jsx`, and existing workspace pages.
- Frontend learning widgets: `frontend/src/components/learning/TodaysFocusCard.jsx`, `RoadmapWidget.jsx`, `CommonMistakesPanel.jsx`, `SkillEnergyGrid.jsx`, `frontend/src/components/home/ProgressOverviewSection.jsx`.

## Dependency Graph

```text
1 conversation persistence -> 2 bounded history/summary -> 3 trusted adaptive context/tools
                                                        |                 |
                                                        +-------> 4 scope/intent/orchestration
                                                                          |
                                          5 proactive evidence/preference policy
                                                                          |
                                      6 member dashboard + Tutor UI state
                                                                          |
                                      7 security and account-switch hardening
                                                                          |
                                      8 end-to-end/regression verification
```

## Implementation Tasks

### Task 1 — Add ownership-safe conversation persistence and additive chat reference

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/tutor/memory/AiConversation.java`, `AiMessage.java`, `AiConversationSummary.java`, `ConversationStatus.java`, `ConversationRepository.java`, `JdbcConversationRepository.java`, `ConversationService.java`.
- Create `backend/src/main/resources/db/migration/<coordinated-version>__create_tutor_conversation_memory.sql` at implementation time after inspecting the merged migration directory.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatRequest.java` additively for nullable `conversationId`; preserve existing JSON clients.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/memory/ConversationServiceTest.java`, `JdbcConversationRepositoryTest.java`, `ConversationSecurityTest.java`, and update `backend/src/test/java/com/ieltsaitutor/ai/dto/AiTutorReferenceTest.java`.

**RED:** Assert authenticated user-scoped create/read/archive, message sequence, normalized citations/context snapshot bounds, foreign conversation rejection, nullable additive request field, and guest non-persistence. Run focused tests; they fail because memory classes/schema are absent.

**Implement:** Add bounded domain records/repository/service and schema with user ownership, status, timestamps, indexes, and deletion/retention rules. Resolve `conversationId` only for the authenticated principal; ignore/reject private conversation references for guests according to the existing normalized error contract.

**GREEN:** Rerun focused tests and existing AI controller/security tests. Commit `feat: add ownership-safe Tutor conversation memory`.

### Task 2 — Enforce bounded history, summaries, archive, and failure behavior

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/tutor/memory/ConversationContextAssembler.java`, `ConversationSummaryService.java`, `ConversationRetentionService.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/controller/AiChatController.java` only for additive conversation list/detail/archive routes if needed.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/memory/ConversationContextAssemblerTest.java`, `ConversationSummaryServiceTest.java`, `ConversationControllerTest.java`.

**RED:** Test recent visible message limit 8, summary trigger at 12 unsummarized, summary max 1000 tokens, retention cap 200, assistant/user status persistence, archive behavior, provider-unavailable summary fallback, and no storage of raw provider payloads/reasoning. Run focused tests; they fail because assembly/summarization is absent.

**Implement:** Assemble bounded context from stored messages and latest valid summary, make summarization provider-neutral and optional, persist only normalized outputs, expose `GET /api/ai/conversations`, `GET /api/ai/conversations/{id}`, and `POST /api/ai/conversations/{id}/archive` with ownership checks.

**GREEN:** Rerun focused memory/controller tests and existing chat regression. Commit `feat: bound Tutor conversation context`.

### Task 3 — Resolve trusted adaptive context and deterministic Tutor tools

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/tutor/adaptive/AdaptiveTutorContextResolver.java`, `AdaptiveTutorContext.java`, `AdaptiveTutorContextPolicy.java`.
- Extend `backend/src/main/java/com/ieltsaitutor/tutor/tool/DeterministicTutorTools.java` and existing tool classes with the spec tool operations: current exercise/question, recent mistakes, weakness/strength profile, skill progress, trend, roadmap, recent attempts, and validated roadmap completion.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/adaptive/AdaptiveTutorContextResolverTest.java`, `AdaptiveTutorToolsSecurityTest.java`.

**RED:** Test server-side resolution from AI2-A services, foreign attempt/roadmap/conversation rejection, forged client score/key/band ignored, deterministic answer path with zero provider calls, empty/insufficient evidence, and completion ownership/status/event validation. Run focused tests; they fail because adaptive resolver/tool wiring is absent.

**Implement:** Resolve only authenticated user-owned records and trusted catalog content, call deterministic tools before RAG/provider, and return structured provider-neutral data. Keep tools read-only except the explicitly validated roadmap completion mutation.

**GREEN:** Rerun focused tests plus practice/writing/speaking Tutor context and deterministic tool tests. Commit `feat: add trusted adaptive Tutor context`.

### Task 4 — Integrate conversation memory, scope policy, and orchestration

**Files:**
- Modify `backend/src/main/java/com/ieltsaitutor/tutor/intent/DefaultTutorIntentRouter.java`, `TutorIntent.java`, `TutorIntentRoute.java` only as needed for `IN_SCOPE`, `AMBIGUOUS_ENGLISH_RELEVANT`, `OUT_OF_SCOPE` and deterministic-first routing.
- Modify `backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java`, `backend/src/main/java/com/ieltsaitutor/ai/service/AiChatService.java`, `backend/src/main/java/com/ieltsaitutor/ai/controller/AiChatController.java` to assemble trusted context/history, persist normalized messages, and preserve response contracts.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/TutorMemoryOrchestrationTest.java`, `TutorScopePolicyTest.java`, `TutorProviderBoundaryTest.java`.

**RED:** Assert generic `hello` does not trigger RAG/embedding, deterministic current-answer/progress requests do not call AI, unsupported redirect is provider-free, grounded requests use only server context, authenticated messages persist, guest messages remain ephemeral, and existing 429/timeout/insufficient-evidence statuses remain intact. Focused tests fail because orchestration is not memory-aware.

**Implement:** Add additive conversation handling around the existing orchestrator, preserve provider-neutral DTO mapping and fallback policy, store assistant status/citations/context snapshot only after normalized response, and ensure failures clear loading at the API boundary without leaking raw provider errors.

**GREEN:** Run focused orchestration tests and all existing Tutor/AiChat/RAG security regressions. Commit `feat: integrate adaptive memory into Tutor orchestration`.

### Task 5 — Add evidence-gated proactive Tutor policy

**Files:**
- Create `backend/src/main/java/com/ieltsaitutor/tutor/proactive/ProactiveTutorPolicy.java`, `ProactiveTutorSuggestion.java`, `ProactiveTutorPreferenceService.java`.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/proactive/ProactiveTutorPolicyTest.java`, `ProactiveTutorSecurityTest.java`.

**RED:** Test one quiet/dismissible suggestion per session, 7-day dismissal cooldown per issue/objective, no identical suggestion without new evidence, notification preference respect, no suggestion from missing/insufficient evidence, user ownership, and no learning event until explicit start/dismiss. Run focused tests; they fail because policy is absent.

**Implement:** Derive suggestions only from AI2-A issue/roadmap evidence and stored preference, return a normalized suggestion DTO, and keep proactive behavior server-policy driven and dismissible. Do not add fake data or provider calls.

**GREEN:** Rerun policy/security tests and profile/roadmap regressions. Commit `feat: gate proactive Tutor suggestions on evidence`.

### Task 6 — Connect member dashboard intelligence and guest-safe frontend state

**Files:**
- Modify `frontend/src/services/learningIntelligenceApi.js`, `frontend/src/features/learning/learningIntelligenceAdapters.js`, `frontend/src/features/learning/learningIntelligenceState.js` only to consume normalized AI2-A responses and explicit empty/loading/error states.
- Modify `frontend/src/components/home/ProgressOverviewSection.jsx`, `frontend/src/components/learning/TodaysFocusCard.jsx`, `RoadmapWidget.jsx`, `CommonMistakesPanel.jsx`, `SkillEnergyGrid.jsx` as needed; preserve current Academic Luxury components and responsive behavior.
- Create/update `frontend/src/__tests__/adaptive-dashboard.test.jsx`.

**RED:** Test member-only loading/profile/roadmap/mistake states, all four skills, empty/insufficient evidence without fake bands/mistakes/countdown, guest assessment CTA and neutral preview, accessible links/focus, and no fabricated missing intelligence. Run `npm test -- --run src/__tests__/adaptive-dashboard.test.jsx`; it fails because the API payload/state contract is not complete.

**Implement:** Normalize server responses through the existing learning facade, render factual data only when present, use `SkeletonBlock` for loading, and preserve the existing 2×2 skills/Academic Luxury composition and mobile layout. Do not embed provider names or backend DTO assumptions in presentation components.

**GREEN:** Rerun targeted frontend tests, then the existing homepage/dashboard suite. Commit `feat: connect adaptive dashboard states`.

### Task 7 — Connect Tutor UI, guest/member lifecycle, and fallback UX

**Files:**
- Modify `frontend/src/services/aiTutorApi.js`, `frontend/src/components/tutor/FloatingTutor.jsx`, `TutorPanel.jsx`, `TutorShell.jsx`, `TutorMessage*.jsx`, `TutorGroundingBadge.jsx`, and the existing auth provider/session boundary only as needed.
- Create/update `frontend/src/__tests__/tutor-memory.test.jsx`, `frontend/src/__tests__/tutor-account-switch.test.jsx`.

**RED:** Test bounded history/conversation ID handling, source chips and citations, `INSUFFICIENT_CONTEXT`/`INSUFFICIENT_EVIDENCE`, provider-neutral fallback/429/timeout/retry states, guest memory reset, logout/account switch clearing, keyboard dialog use, mobile viewport safety, reduced motion, and complete response rendering without truncation. Run focused Vitest tests; they fail because memory lifecycle is not wired.

**Implement:** Keep the current Tutor shell and accessibility, add only normalized conversation state and lifecycle clearing, render skeleton/error/source states through existing primitives, and make send/retry controls recover from every terminal response. Never trust or render client context as authoritative.

**GREEN:** Rerun targeted Tutor tests and full frontend tests. Commit `feat: harden adaptive Tutor frontend lifecycle`.

### Task 8 — Security, compatibility, and whole-phase regression

**Files:**
- Create `backend/src/test/java/com/ieltsaitutor/tutor/memory/Phase2BConversationSecurityRegressionTest.java`, `Phase2BCompatibilityRegressionTest.java`.
- Create/update `frontend/src/__tests__/phase2b-regression.test.jsx`.
- Update only existing configuration/docs if required: `backend/src/main/resources/application.properties`, `.env.example`, or the relevant frontend contract document; no secrets.

**RED:** Add a matrix for conversation IDOR, context ownership, logout/account switch, guest non-persistence, prompt injection treated as data, RAG citation provenance, no provider-specific frontend payload, no infinite loading, all existing routes, and no Phase 3/generator scope. Run backend and frontend focused suites; they fail until the complete integration is stable.

**Implement:** Fix only Important/Critical integration findings, preserve migration compatibility and existing V1–V8 data, add bounded observability without secrets, and document the AI2-A/AI2-B boundary. Do not change provider order or architecture.

**GREEN:** Run `cd backend; .\\mvnw.cmd test; .\\mvnw.cmd package`, then `cd frontend; npm test -- --run; npm run lint; npm run build`, and `git diff --check`. Commit `test: harden adaptive Tutor integration`.

## AI2-B Completion Contract

- Authenticated conversation memory is user-scoped, bounded, archiveable, and never merges guest history.
- Deterministic tools and AI2-A intelligence remain authoritative for app data; AI only explains approved context.
- Generic chat skips RAG/embedding; grounded chat preserves normalized citations and source chips.
- Dashboard and Tutor never invent missing learning intelligence.
- No provider payloads, secrets, Phase 3 generators, STT, or official-score claims cross the frontend boundary.
