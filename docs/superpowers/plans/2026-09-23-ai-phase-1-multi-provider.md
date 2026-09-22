# AI Tutor Multi-Provider Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a provider-neutral, quota-conscious AI Tutor foundation with Groq as the primary chat provider, Cloudflare as the chat and embedding fallback/primary embedding provider, Gemini as the final chat and optional embedding fallback, trusted contextual Tutor orchestration, deterministic application-data tools, and frontend states that remain independent of vendor payloads.

**Architecture:** Preserve `POST /api/ai/chat` and the current normalized frontend contract. Introduce provider adapters behind `AiProviderRouter`, keep deterministic Tutor answers ahead of external calls, resolve context from authenticated server data through `TutorContextService`, route intents without an LLM, and isolate RAG vectors by an exact embedding-space profile. Keep Gemini's existing low-thinking behavior and the `vector(768)` schema contract. Optional providers are disabled when their configuration is absent; application startup does not require every provider.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring MVC/WebFlux `WebClient`, JDBC, Flyway, PostgreSQL/pgvector, JUnit 5, Mockito, Testcontainers where already available, React, Vite, Vitest, React Testing Library, existing Academic Luxury UI components.

**Spec:** `docs/superpowers/specs/2026-09-23-multi-provider-ai-tutor-design.md` at approved commit `89780e8`.

## Global Constraints

- Implement Phase AI-1 only. Do not add persistent conversation memory, conversation summarization, `conversationId` APIs, Groq Whisper/STT, audio Speaking workflows, Phase AI-2, deployment changes, or provider SDKs that are not required by the existing WebClient architecture.
- Preserve `POST /api/ai/chat`, its normalized response shape, existing RAG fields, current Tutor visual shell, source chips, insufficient-evidence behavior, loading behavior, keyboard accessibility, reduced-motion behavior, and frontend independence from vendor-specific payloads.
- Keep `GEMINI_MODEL` configurable with the current default `gemini-3.8-flash`. Preserve Gemini low thinking for normal Tutor chat. Never put provider keys in source, tests, fixtures, logs, screenshots, or documentation.
- Add configuration for `AI_PRIMARY_PROVIDER`, `AI_FALLBACK_PROVIDERS`, `GROQ_API_KEY`, `GROQ_CHAT_MODEL`, `GROQ_BASE_URL`, `GROQ_CONNECT_TIMEOUT`, `GROQ_RESPONSE_TIMEOUT`, `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_CHAT_MODEL`, `CLOUDFLARE_EMBEDDING_MODEL`, `CLOUDFLARE_BASE_URL`, existing Gemini settings, and `RAG_EMBEDDING_DIMENSION=768`.
- Use the default chat order Groq, Cloudflare, Gemini unless configuration explicitly changes the order. Missing configuration disables an adapter without preventing startup.
- Classify failures as transient, not configured, authentication, malformed response, invalid request, or content blocked. Rotate only for transient, missing, authentication, and malformed failures. Stop on invalid request and content blocked. After all eligible providers fail, return the existing controlled 503 AI-unavailable response.
- Allow at most one bounded local retry where the provider policy permits it, then advance once. Never retry a 429 storm or create an infinite fallback loop. Health thresholds, rolling window, cooldown, and half-open probe are configuration properties, with defaults of 3 transient failures, 60 seconds, 30 seconds, and one probe.
- Embedding selection and retrieval must match `embedding_provider`, `embedding_model`, `embedding_dimension`, and `embedding_version` exactly. Keep `rag_chunks.embedding` as `vector(768)`. Never compare or retrieve mixed vector spaces.
- Existing indexed RAG content without a complete embedding profile becomes `REINDEX_REQUIRED` and is not retrievable until explicitly reindexed and activated. Retain old chunks for rollback and switch the active version only after validation succeeds.
- `TutorContextService` may use only authenticated server-side ownership and practice data. Never trust client answer keys, scores, bands, progress values, private history, or user IDs. Deterministic tools must not call an external provider.
- Keep Writing's provider-neutral response contract and mark AI bands as estimates. Keep Speaking text/prompt context only; do not add audio upload or STT.
- All external provider tests use mocks or local HTTP fixtures. Live credentials are optional and are used only after local tests pass, with the minimum possible number of calls.
- Use focused commits in the task order below. Do not squash migration history, push, merge, or deploy.

## Review Focus

1. `AiProviderRouterTest` must prove a provider 400 stops rotation, while Groq 429/timeout advances to Cloudflare and Cloudflare 429/5xx advances to Gemini.
2. `ApplicationStartupWithoutOptionalProvidersTest` must prove the backend starts with only Gemini, only Groq, only Cloudflare, or no configured provider; the last case fails requests in a controlled way rather than failing startup.
3. `EmbeddingSpaceTest` and `RagSchemaMigrationTest` must prove exact four-field matching, no mixed vectors, and `REINDEX_REQUIRED` behavior for pre-metadata content.
4. `TutorContextServiceTest` and deterministic tool tests must prove forged client data and cross-user IDs cannot affect answers or explanations.
5. Frontend Tutor tests must prove fallback, timeout, unavailable, app-data, grounded, and insufficient-evidence states clear loading correctly and never render raw provider names or vendor payloads.

## Prerequisites and Working Agreement

- Start from `feature/phase-2b-rag` at a clean worktree with approved spec commit `89780e8` in history.
- Confirm Java 21, Node/npm, the existing Maven wrapper, PostgreSQL/pgvector, and Docker/Testcontainers are available. Local provider tests must not require network credentials.
- Do not add a provider SDK. Reuse Spring WebClient, existing Jackson configuration, existing exception handling, and the current test fixture style.
- Before each implementation task, add the named failing test and run its focused command. Record the failure as the missing interface, missing bean, missing configuration, or missing behavior described by that task. Then make the smallest production change that turns that test green and rerun the focused regression.
- Each task ends with the exact commit message listed in that task. A later task may amend a prior test only when the contract is intentionally extended and the focused regression remains green.

## Dependency Graph

```text
1 Configuration/capabilities
        |
        v
2 Router foundation -----> 3 Groq adapter -----> 4 Cloudflare chat adapter
        |                                             |
        +-----------------------> 5 failure policy/health <---- Gemini adapter

6 Cloudflare embedding ---> 7 embedding-space model/isolation ---> 8 V6 migration/reindex safety
                                                                  |
                                                                  v
                                                        RAG exact-space retrieval

9 trusted Tutor context ---> 10 deterministic tools ---> 11 rule-based intent router
                                                        |
                                                        v
                                              12 Tutor orchestration
                                             /       |       \
                                            /        |        \
                              13 Reading/Listening 14 Writing/Speaking
                                            \        |        /
                                             v       v       v
                                  15 frontend normalized states
                                             |
                                             v
                                  16 security/quota hardening
                                             |
                                             v
                                  17 regression/docs/live checks
```

The existing Gemini adapter remains the final chat adapter throughout Tasks 2–5. Task 8 is the only schema migration in this Phase AI-1 plan.

## Implementation Tasks

### Task 1: Configuration and provider capability model

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/ai/config/AiProviderProperties.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/provider/ProviderId.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/provider/ProviderCapability.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/provider/ProviderConfiguration.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/config/GeminiProperties.java` only where shared provider binding or default preservation requires it.
- Modify `backend/src/main/resources/application.properties` and `backend/.env.example` with the provider properties and safe defaults.
- Create `backend/src/test/java/com/ieltsaitutor/ai/config/AiProviderPropertiesTest.java`.
- Create `backend/src/test/java/com/ieltsaitutor/ai/config/ApplicationStartupWithoutOptionalProvidersTest.java`.

**Interfaces:**

- `ProviderId` has exactly `GROQ`, `CLOUDFLARE`, and `GEMINI`.
- `ProviderCapability` has at least `CHAT`, `EMBEDDING`, and `WRITING_ASSESSMENT`.
- `ProviderConfiguration` exposes provider ID, enabled state, configured model, capabilities, and a safe display identifier. It does not expose credentials.
- `AiProviderProperties` binds primary provider, ordered fallback IDs, health settings, and per-provider connection/response timeouts without embedding provider secrets in logs or `toString()`.

**Tests first:**

- [ ] Add a properties-binding test for all requested environment names, `gemini-3.8-flash`, and dimension `768`.
- [ ] Add startup tests proving absent Groq/Cloudflare keys disable those adapters while startup remains successful.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=AiProviderPropertiesTest,ApplicationStartupWithoutOptionalProvidersTest test"` and confirm RED because the binding and capability model do not exist.

**Implementation:**

- [ ] Bind environment variables using Spring configuration properties and preserve all existing Gemini/RAG names.
- [ ] Make provider presence a capability decision, not a bean-creation failure. Keep Gemini's existing model and LOW-thinking setting unchanged.
- [ ] Make the configured ordered list deterministic and reject duplicate provider IDs at configuration validation time.

**Verification and commit:**

- [ ] Rerun the focused command and the existing `GeminiAiProviderTest`.
- [ ] Commit with `feat: add AI provider capability configuration`.

### Task 2: Provider-neutral `AiProviderRouter` foundation

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/AiProviderRouter.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/AiProviderAdapter.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderAttempt.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/provider/AiProvider.java` to preserve its compatibility `chat(AiChatCommand)` method while allowing routing adapters.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/service/AiChatService.java` and Spring wiring so the router is the single chat entry point.
- Create `backend/src/test/java/com/ieltsaitutor/ai/routing/AiProviderRouterTest.java`.
- Update `backend/src/test/java/com/ieltsaitutor/ai/service/AiChatServiceTest.java` only for the provider-neutral seam.

**Interfaces:**

- `AiProviderAdapter` exposes `ProviderId id()`, `Set<ProviderCapability> capabilities()`, `boolean enabled()`, and `AiChatResult chat(AiChatCommand command)`.
- `AiProviderRouter` implements the existing `AiProvider` compatibility contract and exposes one normalized `chat(AiChatCommand)` method to application services.
- The initial router selects the configured order, skips disabled adapters, and returns only `AiChatResult` or the existing normalized `AiProviderException`; it never returns a raw vendor response.

**Tests first:**

- [ ] Add fake adapter tests for first-enabled-provider success, disabled-provider skipping, no-provider controlled error, and normalized result output.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=AiProviderRouterTest,AiChatServiceTest test"` and confirm RED because no router seam exists.

**Implementation:**

- [ ] Register the existing Gemini adapter through the router without changing Gemini request behavior.
- [ ] Keep controller and frontend contracts unchanged. Preserve request IDs and current exception status mapping.
- [ ] Ensure adapter ordering is configuration-driven rather than provider-name conditionals in `AiChatService`.

**Verification and commit:**

- [ ] Run the focused router/service tests plus `GeminiAiProviderTest`.
- [ ] Commit with `feat: add provider-neutral AI chat router`.

### Task 3: Groq chat provider

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/ai/config/GroqProperties.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/config/GroqWebClientConfig.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/provider/GroqAiProvider.java`.
- Modify provider registration so Groq is an `AiProviderAdapter` only when its key and model are configured.
- Create `backend/src/test/java/com/ieltsaitutor/ai/provider/GroqAiProviderTest.java`.

**Interfaces:**

- `GroqAiProvider` implements `AiProviderAdapter` with `ProviderId.GROQ` and `CHAT` capability.
- It sends the configurable `GROQ_CHAT_MODEL`, a bounded message list, and the normalized Tutor system instruction to the configured Groq OpenAI-compatible chat endpoint.
- It reads only the assistant text needed for `AiChatResult`; it does not leak the upstream JSON shape.

**Tests first:**

- [ ] Add local HTTP fixture tests for successful assistant text, empty/malformed choices, 400, 401/403, 429, 503, and timeout.
- [ ] Add a test proving an absent key makes the adapter disabled without constructing a request.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=GroqAiProviderTest test"` and confirm RED because the adapter and properties do not exist.

**Implementation:**

- [ ] Use WebClient with `Authorization: Bearer` from configuration, connection/response timeouts, bounded response parsing, and no credential logging.
- [ ] Map upstream failures to typed provider failures once Task 5 adds the taxonomy; before that, preserve the existing controlled exception boundary.
- [ ] Keep prompts provider-neutral and do not add a vendor field to `AiChatResponse`.

**Verification and commit:**

- [ ] Run `GroqAiProviderTest`, `AiProviderRouterTest`, and `GeminiAiProviderTest`.
- [ ] Commit with `feat: add Groq chat provider`.

### Task 4: Cloudflare chat provider

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/ai/config/CloudflareProperties.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/config/CloudflareWebClientConfig.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/provider/CloudflareAiProvider.java`.
- Modify provider registration for the Cloudflare chat capability.
- Create `backend/src/test/java/com/ieltsaitutor/ai/provider/CloudflareAiProviderTest.java`.

**Interfaces:**

- `CloudflareAiProvider` implements `AiProviderAdapter` with `ProviderId.CLOUDFLARE` and `CHAT` capability.
- It uses `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_BASE_URL`, and `CLOUDFLARE_CHAT_MODEL`; the endpoint path is composed centrally, not in controllers.
- It normalizes Cloudflare success responses containing the assistant text into `AiChatResult` and rejects malformed response bodies.

**Tests first:**

- [ ] Add local HTTP fixture tests for success, 400, 401/403, 429, 503, malformed output, and absent account/token/model configuration.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=CloudflareAiProviderTest test"` and confirm RED because the adapter and properties do not exist.

**Implementation:**

- [ ] Add the account authorization header required by the configured Cloudflare endpoint without printing it.
- [ ] Reuse the same normalized system instruction, history cap, request ID, timeout policy, and output limit as other chat adapters.
- [ ] Keep Cloudflare wire fields inside the adapter package.

**Verification and commit:**

- [ ] Run both provider fixture suites and the router compatibility suite.
- [ ] Commit with `feat: add Cloudflare chat provider`.

### Task 5: Fallback, error classification, and provider health

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderFailureCategory.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderFailure.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderRoutingPolicy.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderHealthRegistry.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderHealthSnapshot.java`.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/AiProviderTrace.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/exception/AiProviderException.java`, `AiExceptionHandler.java`, all three provider adapters, and `AiProviderRouter.java`.
- Create/update `backend/src/test/java/com/ieltsaitutor/ai/routing/AiProviderRouterTest.java` and `ProviderHealthRegistryTest.java`.

**Interfaces:**

- `ProviderFailureCategory` contains `TRANSIENT`, `NOT_CONFIGURED`, `AUTHENTICATION`, `MALFORMED_RESPONSE`, `INVALID_REQUEST`, and `CONTENT_BLOCKED`.
- `ProviderFailure` carries provider ID, category, safe public code, HTTP status, request ID, and retryability. It never carries a key, authorization header, full prompt, essay, audio, or raw vendor body.
- `ProviderRoutingPolicy` decides `advance`, `retry once`, or `stop` from the category and current attempt. It enforces one route visit per provider per request.
- `ProviderHealthRegistry` records transient failures in a rolling 60-second window, opens after 3 failures, cools down for 30 seconds, and allows one half-open probe.
- `AiProviderTrace` records UUID, selected route, provider attempt, duration, status, fallback count, and failure category for safe metrics/logging.

**Tests first:**

- [ ] Add explicit routing tests: Groq success makes no Cloudflare/Gemini call; Groq 429 and timeout advance to Cloudflare; Cloudflare unavailable/429/5xx advances to Gemini; Gemini 429 returns controlled 503 `AI_TEMPORARILY_UNAVAILABLE`.
- [ ] Add tests proving 400/invalid request and content blocked do not rotate, authentication/configuration failures advance, malformed output advances, and all unavailable providers fail quickly.
- [ ] Add tests proving no repeated 429, no infinite fallback loop, three transient failures open cooldown, one half-open probe is admitted, and a successful probe closes the circuit.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=AiProviderRouterTest,ProviderHealthRegistryTest,GroqAiProviderTest,CloudflareAiProviderTest,GeminiAiProviderTest test"` and confirm RED because typed policy and health are absent.

**Implementation:**

- [ ] Map HTTP status and transport errors in each adapter to the shared categories.
- [ ] Apply the policy once per request and preserve the original request ID through every fallback attempt.
- [ ] Return the existing safe 503 response after exhaustion. Do not expose “Groq”, “Cloudflare”, or “Gemini” in normal Tutor UI responses.
- [ ] Keep logs and metrics redacted and bounded; never log raw vendor payloads.

**Verification and commit:**

- [ ] Run the focused suite and the existing AI/RAG failure tests.
- [ ] Commit with `feat: add bounded provider fallback policy`.

### Task 6: Cloudflare embedding provider

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/rag/config/CloudflareEmbeddingProperties.java`.
- Create `backend/src/main/java/com/ieltsaitutor/rag/embedding/CloudflareEmbeddingProvider.java`.
- Modify `backend/src/main/java/com/ieltsaitutor/rag/embedding/EmbeddingProvider.java` only to expose the provider identity and embedding space needed by the router.
- Modify `backend/src/main/java/com/ieltsaitutor/rag/config/RagProperties.java` and `application.properties` for the configured embedding provider/model/version.
- Create `backend/src/test/java/com/ieltsaitutor/rag/embedding/CloudflareEmbeddingProviderTest.java`.

**Interfaces:**

- `CloudflareEmbeddingProvider` implements `EmbeddingProvider`, reports `ProviderId.CLOUDFLARE`, and returns a bounded `EmbeddingResult`.
- It uses `CLOUDFLARE_EMBEDDING_MODEL`, Cloudflare account/token/base URL, and the configured dimension `768`.
- A response with a dimension other than 768 raises a typed `RAG_EMBEDDING_DIMENSION_MISMATCH`; it is never padded, truncated, or silently accepted.

**Tests first:**

- [ ] Add fixture tests for one vector with 768 values, malformed output, an empty result, 429/503, invalid request, missing configuration, and a 769-value response.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=CloudflareEmbeddingProviderTest test"` and confirm RED because the provider and properties do not exist.

**Implementation:**

- [ ] Parse only the normalized vector values inside the adapter and preserve the existing `EmbeddingTask` semantics.
- [ ] Keep Gemini embedding available as an optional provider without changing existing model configuration names.
- [ ] Reuse WebClient timeout and redaction rules; do not add a vendor response model to RAG domain objects.

**Verification and commit:**

- [ ] Run the provider contract tests, existing Gemini embedding tests, and RAG failure tests.
- [ ] Commit with `feat: add Cloudflare embedding provider`.

### Task 7: Embedding metadata and vector-space isolation

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/rag/embedding/EmbeddingSpace.java`.
- Create `backend/src/main/java/com/ieltsaitutor/rag/embedding/EmbeddingSpaceSelector.java`.
- Create `backend/src/main/java/com/ieltsaitutor/rag/embedding/EmbeddingProviderRouter.java`.
- Modify `EmbeddingRequest`, `EmbeddingResult`, `EmbeddingTask`, `DefaultQueryEmbeddingService`, `RagQuery`, and `RetrievedChunk` to carry the selected exact space.
- Modify `VectorRetrievalService` and `JdbcVectorRetrievalService` signatures to require an `EmbeddingSpace` filter while keeping SQL-column work in Task 8.
- Create/update `backend/src/test/java/com/ieltsaitutor/rag/embedding/EmbeddingSpaceTest.java` and `EmbeddingProviderRouterTest.java`.

**Interfaces:**

- `EmbeddingSpace` is an immutable record of `provider`, `model`, `dimension`, and `version`, with a stable comparison key.
- `EmbeddingProviderRouter` selects Cloudflare as embedding primary and Gemini as optional fallback only when both providers use the exact requested space or the index lifecycle explicitly schedules reindexing.
- `VectorRetrievalService.search(RagQuery query, EmbeddingSpace space)` refuses a query when its space does not match the active index space.

**Tests first:**

- [ ] Add tests for equality across all four fields, dimension mismatch rejection, provider/model/version mismatch rejection, Cloudflare primary selection, Gemini fallback selection, and disabled-provider behavior.
- [ ] Add retrieval tests proving a result from another provider, model, dimension, or version is excluded.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=EmbeddingSpaceTest,EmbeddingProviderRouterTest,VectorRetrievalServiceTest test"` and confirm RED because exact-space plumbing does not exist.

**Implementation:**

- [ ] Carry the space from indexing and query embedding through RAG retrieval and context building.
- [ ] Keep `vector(768)` as the only supported physical dimension in this phase.
- [ ] Make fallback selection explicit and observable; do not silently mix Cloudflare and Gemini vectors in one search.

**Verification and commit:**

- [ ] Run focused embedding/retrieval suites and all existing RAG contract tests.
- [ ] Commit with `feat: model exact RAG embedding spaces`.

### Task 8: Migration and reindex safety

**Files:**

- Create `backend/src/main/resources/db/migration/V6__add_embedding_space_metadata.sql`.
- Modify RAG domain records/entities, ingestion services, indexing jobs, admin DTOs/controllers, `JdbcRagRepository`, `JdbcVectorRetrievalService`, and migration test fixtures so metadata is stored and filtered.
- Modify the RAG admin response to report `REINDEX_REQUIRED` without exposing secrets.
- Modify `backend/src/test/java/com/ieltsaitutor/rag/RagSchemaMigrationTest.java`, `RagRepositoryGovernanceTest.java`, `RagEndToEndTest.java`, and add `EmbeddingSpaceMigrationTest.java`.

**Interfaces:**

- V6 adds nullable `embedding_provider`, `embedding_model`, `embedding_dimension`, and `embedding_version` fields to the version/chunk metadata required by the current schema.
- V6 changes existing indexed versions without a complete profile to `REINDEX_REQUIRED`; old chunk rows remain for inspection and rollback, but retrieval excludes them.
- An index may become `INDEXED` only when all four fields are present, dimension equals 768, vector length is 768, and every chunk belongs to that exact space.
- Reindex writes new chunks and metadata in one controlled lifecycle, validates them, then switches the document's active version. A failed reindex leaves the old active version unchanged.

**Tests first:**

- [ ] Add migration tests for a fresh V1–V6 database, upgrade of a V1–V5 database, metadata columns, `REINDEX_REQUIRED`, retained old chunks, exact retrieval filtering, and successful reindex activation.
- [ ] Add a failure test proving a dimension/provider/model/version mismatch cannot mark a version indexed.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=RagSchemaMigrationTest,EmbeddingSpaceMigrationTest,RagRepositoryGovernanceTest,RagEndToEndTest test"` and confirm RED because V6 and the repository fields do not exist.

**Implementation:**

- [ ] Add only V6; do not rewrite or squash V1–V5 and do not add memory or Speaking metadata migrations.
- [ ] Update SQL predicates to require exact metadata equality before cosine-distance ordering.
- [ ] Preserve admin approval/index/activate semantics and make `REINDEX_REQUIRED` actionable through the existing admin workflow.

**Verification and commit:**

- [ ] Run the migration and RAG integration suites with the existing Docker/Testcontainers configuration.
- [ ] Commit with `feat: isolate RAG embedding metadata safely`.

### Task 9: Trusted `TutorContextService`

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/tutor/context/TutorContextRequest.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/context/TutorLearningContext.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/context/TutorContextService.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/context/DefaultTutorContextService.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/context/TutorContextException.java`.
- Modify practice, learning, writing, and speaking repository seams only where server-side lookup needs a stable owner-scoped ID.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/context/TutorContextServiceTest.java`.

**Interfaces:**

- `TutorContextService.resolve(AuthPrincipal principal, TutorContextRequest request)` returns a bounded `TutorLearningContext` or a safe absent/unauthorized result.
- `TutorContextRequest` contains only stable references such as skill, set/lesson ID, question/exercise ID, attempt ID, task type, and prompt ID.
- `TutorLearningContext` contains trusted server-resolved learner answer, correct answer, explanation, score/count, writing task metadata, speaking prompt metadata, progress, and bounded mistakes where authorized. It excludes arbitrary client-supplied answer keys and bands.

**Tests first:**

- [ ] Add tests for the authenticated owner resolving a Reading/Listening attempt, Writing submission, Speaking text attempt, and progress reference.
- [ ] Add tests proving cross-user IDs return no private data, unauthenticated private context is rejected, guest public context is bounded, forged client answers/answer keys/scores/bands are ignored, and context size is capped.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=TutorContextServiceTest test"` and confirm RED because the context service and trusted context model do not exist.

**Implementation:**

- [ ] Resolve ownership from `AuthPrincipal.userId()` and existing repository queries; never accept a user ID from the request as authority.
- [ ] Reuse server-side `SyntheticPracticeCatalog` answer keys and explanations for current Reading/Listening flows.
- [ ] Return only the minimum context needed for the selected intent and redact private data from traces.

**Verification and commit:**

- [ ] Run context tests plus auth, practice, writing, speaking, and progress regressions.
- [ ] Commit with `feat: add trusted Tutor context resolution`.

### Task 10: Deterministic Tutor tools

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/TutorTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/TutorToolResult.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/CurrentExerciseTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/SelectedAnswerTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/CorrectAnswerTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/ProgressTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/HistoryTool.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/tool/DeterministicTutorTools.java`.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/tool/DeterministicTutorToolsTest.java`.

**Interfaces:**

- `TutorTool` exposes `boolean supports(TutorIntent intent)` and `TutorToolResult execute(TutorLearningContext context)`.
- `TutorToolResult` uses the existing normalized Tutor response fields with `status=APP_DATA`, no external grounding claim, and safe structured text for the current client.
- The facade selects exactly one deterministic tool and never receives an `AiProvider` dependency.

**Tests first:**

- [ ] Add tests for current question, selected answer, trusted correct answer, score/count, progress, and history summary using server fixtures.
- [ ] Add a spy-provider test proving deterministic requests make zero external provider calls and zero embedding calls.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=DeterministicTutorToolsTest test"` and confirm RED because deterministic tools do not exist.

**Implementation:**

- [ ] Use only `TutorLearningContext`; do not parse client answer keys or calculate private progress from request values.
- [ ] Keep output bounded and localized to the current product copy; do not invent bands or results when data is absent.
- [ ] Return a safe unavailable/context-missing result for missing ownership instead of calling an LLM to guess.

**Verification and commit:**

- [ ] Run deterministic, context, and existing Tutor service tests.
- [ ] Commit with `feat: add deterministic Tutor application tools`.

### Task 11: Rule-based `TutorIntentRouter`

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/tutor/intent/TutorIntent.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/intent/TutorIntentRoute.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/intent/TutorIntentRouter.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/intent/DefaultTutorIntentRouter.java`.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/intent/TutorIntentRouterTest.java`.

**Interfaces:**

- `TutorIntent` contains `APP_DATA`, `GENERIC_CHAT`, `EXERCISE_EXPLANATION`, `RAG_EXPLANATION`, `WRITING_FEEDBACK`, `SPEAKING_FEEDBACK`, and `PROGRESS_HISTORY`.
- `TutorIntentRouter.route(AiChatRequest request, TutorLearningContext context)` is deterministic and returns a route plus whether external AI is permitted.
- Rule precedence is application data, progress/history, exercise explanation, writing feedback, speaking feedback, grounded RAG explanation, then generic chat.

**Tests first:**

- [ ] Add English and Vietnamese phrase tests for every intent, including ambiguous prompts where deterministic routes win.
- [ ] Add tests proving no LLM/classifier call is made and missing context cannot be upgraded to a fabricated external answer.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=TutorIntentRouterTest test"` and confirm RED because the intent model does not exist.

**Implementation:**

- [ ] Use normalized context, message, and stable request references only; avoid provider-specific classifications.
- [ ] Keep RAG routing compatible with the existing `RagIntentClassifier` while making Tutor intent the outer decision.
- [ ] Make the output stable enough for unit tests and safe to evolve without changing the frontend contract.

**Verification and commit:**

- [ ] Run intent, deterministic, and RAG classifier regressions.
- [ ] Commit with `feat: add deterministic Tutor intent routing`.

### Task 12: Contextual Tutor orchestration

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java`.
- Create `backend/src/main/java/com/ieltsaitutor/tutor/TutorResponse.java` only if the existing `AiChatResponse` cannot represent the normalized result without vendor fields.
- Modify `backend/src/main/java/com/ieltsaitutor/ai/service/AiChatService.java`, `AiChatController.java`, `AiChatRequest.java`, `AiChatCommand.java`, and the existing RAG chat service seam.
- Create/update `backend/src/test/java/com/ieltsaitutor/tutor/TutorOrchestratorTest.java` and `AiChatControllerTest.java`.

**Interfaces:**

- `TutorOrchestrator.handle(AuthPrincipal principal, AiChatRequest request)` is the sole end-to-end Tutor entry point.
- The orchestration order is context resolution, intent routing, deterministic tool, RAG/contextual explanation, then provider-neutral generic chat.
- `AiChatService` remains a compatibility facade delegating to the orchestrator; `AiChatController` passes the authenticated principal from the existing interceptor attribute.

**Tests first:**

- [ ] Add controller/service tests for deterministic application data, generic chat through the router, grounded RAG with trusted context, insufficient evidence, provider unavailable, and malformed provider output.
- [ ] Add a contract assertion that response JSON contains only the existing normalized fields and never raw provider payloads or provider names.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=TutorOrchestratorTest,AiChatControllerTest,AiChatServiceTest,RagChatServiceTest test"` and confirm RED because the orchestrator does not exist.

**Implementation:**

- [ ] Preserve current `/api/ai/chat` status codes and `sources`/`grounding` semantics, including `INSUFFICIENT_CONTEXT` and `INSUFFICIENT_EVIDENCE` with an empty source list.
- [ ] Reuse one bounded trusted evidence/context object across fallback providers; do not repeat retrieval on each fallback attempt.
- [ ] Keep no conversation memory or `conversationId` in this phase.

**Verification and commit:**

- [ ] Run focused orchestration/controller/RAG tests and the full backend AI suite.
- [ ] Commit with `feat: orchestrate contextual Tutor requests`.

### Task 13: Reading and Listening context integration

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/practice/PracticeController.java`, `PracticeService.java`, `PracticeAttemptResult.java`, and the practice repository/store only for stable owner-scoped attempt/question references.
- Modify `frontend/src/pages/PracticePage.jsx` and the existing Tutor context handoff used by that page.
- Modify `frontend/src/tests/practice.test.jsx` and create/update `backend/src/test/java/com/ieltsaitutor/practice/PracticeTutorContextTest.java`.

**Interfaces:**

- A submitted practice result exposes a stable attempt reference without exposing the answer key.
- Practice Tutor context sends skill, set/lesson ID, question ID, and owned attempt ID when available; the backend resolves answers, explanations, scores, and counts.
- Reading and Listening use the same first-class contextual contract and do not receive special visual or provider treatment.

**Tests first:**

- [ ] Add backend tests for Reading and Listening current question, selected answer, correct answer, and score/count through the trusted context service.
- [ ] Add a frontend test that the Tutor request carries stable practice references and never carries the server answer key.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=PracticeTutorContextTest,PracticeControllerTest test"` and `npm test -- --run src/tests/practice.test.jsx`; confirm RED because the stable context handoff is absent.

**Implementation:**

- [ ] Preserve existing practice submission behavior and route paths.
- [ ] Track only the active/current question and server attempt reference needed by Tutor context; do not duplicate catalog data into React.
- [ ] Keep unanswered or unauthorized context deterministic and safe rather than sending guessed data to an external provider.

**Verification and commit:**

- [ ] Run practice backend tests and the targeted frontend practice test.
- [ ] Commit with `feat: connect Reading and Listening Tutor context`.

### Task 14: Writing and Speaking context integration

**Files:**

- Modify `backend/src/main/java/com/ieltsaitutor/writing/WritingAssessmentService.java`, `WritingController.java`, `WritingAssessment.java`, and writing repository seams to use trusted context and the provider-neutral router.
- Modify `backend/src/main/java/com/ieltsaitutor/speaking/SpeakingService.java`, `SpeakingController.java`, and speaking repository seams for text/prompt context only.
- Modify `frontend/src/pages/WritingPage.jsx`, `frontend/src/pages/SpeakingPage.jsx`, `frontend/src/tests/writing.test.jsx`, and `frontend/src/tests/speaking.test.jsx`.
- Create/update `backend/src/test/java/com/ieltsaitutor/writing/WritingTutorContextTest.java` and `SpeakingTutorContextTest.java`.

**Interfaces:**

- Writing assessment calls the provider-neutral Tutor/orchestration contract with trusted Task 1/Task 2 metadata and preserves `overallBandEstimate`, criteria, strengths, issues, suggestions, citations, grounding, and disclaimer.
- Speaking Tutor context uses the owned prompt and saved text attempt; it does not introduce audio, STT, Groq Whisper, or an advanced speaking scorer.
- Missing/invalid provider output remains an unavailable assessment; no fake score is persisted.

**Tests first:**

- [ ] Add tests proving Task 1 uses task-type metadata, Task 2 uses task-type metadata, cross-user submissions are rejected, and invalid band/criteria JSON is unavailable.
- [ ] Add text-only Speaking context tests and frontend assertions for task/prompt references.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=WritingTutorContextTest,SpeakingTutorContextTest,WritingControllerTest,SpeakingControllerTest test"` and the two targeted frontend suites; confirm RED because these flows still bypass the contextual orchestration seam.

**Implementation:**

- [ ] Route Writing through `AiProviderRouter` without changing the frontend assessment contract.
- [ ] Keep AI score labels as estimates and preserve current authorization and validation rules.
- [ ] Keep Speaking's existing unavailable STT boundary intact and add only trusted text/prompt references.

**Verification and commit:**

- [ ] Run writing/speaking backend and frontend regressions.
- [ ] Commit with `feat: add contextual Writing and Speaking Tutor flows`.

### Task 15: Frontend provider-neutral failure, fallback, and context states

**Files:**

- Modify `frontend/src/services/aiTutorApi.js`.
- Modify `frontend/src/components/tutor/FloatingTutor.jsx`, `TutorPanel.jsx`, `TutorMessage.jsx`, `TutorGroundingBadge.jsx`, and `SourceChip.jsx` only where state normalization or presentation requires it.
- Modify `frontend/src/pages/PracticePage.jsx`, `WritingPage.jsx`, and `SpeakingPage.jsx` for context references.
- Modify `frontend/src/tests/tutor.test.jsx`, `tutor-rag.test.jsx`, `practice.test.jsx`, `writing.test.jsx`, and `speaking.test.jsx`.

**Interfaces:**

- `aiTutorApi` maps backend statuses to provider-neutral UI states: thinking, app-data, grounded, insufficient evidence, fallback, temporary unavailable, and timeout.
- The UI receives normalized `answer`, `status`, `grounding`, and `sources` only. It never renders provider IDs, raw error bodies, HTTP payloads, keys, or authorization details.
- Existing `SkeletonBlock`, source chips, keyboard close/focus behavior, mobile-safe panel behavior, and reduced-motion CSS remain the rendering primitives.

**Tests first:**

- [ ] Add tests for app-data answers, fallback answers without provider labels, 503 temporary unavailable with retry, timeout, grounded response with a source chip, insufficient evidence with no sources, and loading cleanup on every terminal state.
- [ ] Add tests proving context references are sent but answer keys and full private history are not.
- [ ] Add an accessibility test for `aria-busy`, focus-visible action controls, Escape close, and mobile panel bounds.
- [ ] Run `npm test -- --run src/tests/tutor.test.jsx src/tests/tutor-rag.test.jsx src/tests/practice.test.jsx src/tests/writing.test.jsx src/tests/speaking.test.jsx` and confirm RED for the new states before implementation.

**Implementation:**

- [ ] Extend the existing normalized error mapping without changing route paths or the Tutor shell.
- [ ] Ensure a rejected, timed-out, or unavailable request always clears loading and returns focus safely.
- [ ] Use provider-neutral copy such as “Trợ giảng AI đang tạm thời không khả dụng”; do not expose raw provider names.
- [ ] Preserve source provenance and insufficient-evidence presentation and avoid implying official IELTS affiliation.

**Verification and commit:**

- [ ] Run the targeted frontend suite, then the complete frontend test suite.
- [ ] Commit with `feat: add provider-neutral Tutor UI states`.

### Task 16: Security, quota, and observability hardening

**Files:**

- Create `backend/src/main/java/com/ieltsaitutor/tutor/security/TutorRateLimiter.java` and `TutorRateLimitDecision.java` if the existing application has no suitable bounded limiter.
- Create `backend/src/main/java/com/ieltsaitutor/ai/routing/ProviderRequestTrace.java` if `AiProviderTrace` from Task 5 does not cover request-level metrics.
- Modify `AiChatController.java`, `AiExceptionHandler.java`, `AuthInterceptor.java` only where request identity and limit checks need to be applied before external calls.
- Modify `application.properties`, `.env.example`, and safe logging/metrics wiring.
- Create `backend/src/test/java/com/ieltsaitutor/tutor/security/TutorQuotaPolicyTest.java` and `AiSecurityRegressionTest.java`.

**Interfaces:**

- Rate limits execute before provider or embedding calls, with configurable bounded defaults of 10 Tutor requests/minute for guests and 30/minute for authenticated users.
- Request traces include only UUID, route, provider attempt, duration, status, fallback count, and failure category. They exclude keys, auth headers, full prompts, essays, audio, raw payloads, and full source documents.
- Auth and ownership checks remain server-side and are enforced before private context is added to a prompt.

**Tests first:**

- [ ] Add tests proving a rate-limited request makes no external provider call, deterministic app-data requests bypass external quota, and authenticated ownership is required for private context.
- [ ] Add redaction tests for logs/trace objects and a fallback cooldown test proving repeated 429s do not multiply calls.
- [ ] Run `cmd /c ".\mvnw.cmd -Dtest=TutorQuotaPolicyTest,AiSecurityRegressionTest,AiProviderRouterTest test"` and confirm RED because the hardening seams do not exist.

**Implementation:**

- [ ] Use a bounded process-local limiter only if no existing shared limiter is present; make limits configurable and deterministic in tests.
- [ ] Keep the external-call budget finite per request and preserve request ID across router attempts.
- [ ] Keep secrets out of startup diagnostics, exception messages, trace attributes, and frontend responses.

**Verification and commit:**

- [ ] Run the focused security/quota suite and full backend AI/RAG/auth regression suites.
- [ ] Commit with `feat: harden Tutor provider quotas and tracing`.

### Task 17: Full regression, documentation, and minimal live verification

**Files:**

- Modify `backend/.env.example`, root/backend `README.md`, and the existing runbook/configuration documentation to describe provider order, optional configuration, V6 reindex state, safe failure behavior, and no-memory/no-STT Phase AI-1 boundaries.
- Create `backend/src/test/java/com/ieltsaitutor/ai/AiPhaseOneContractTest.java` for configuration and normalized contract assertions if existing tests do not cover them.
- Do not modify production source in this task unless a verification failure identifies a defect introduced by the preceding tasks.

**Tests first:**

- [ ] Add the contract test for startup without optional providers, normalized `/api/ai/chat` output, exact embedding-space metadata, bounded fallback count, and frontend-visible provider-neutral statuses.
- [ ] Run the focused contract test and confirm RED if any documented/normalized Phase AI-1 contract is still missing.

**Implementation:**

- [ ] Document exact environment names, default values, provider order, timeout/cooldown settings, V6 migration/reindex procedure, and safe 503 behavior without including credential values.
- [ ] Add no Phase AI-2 memory tables, no V8 Speaking metadata, no STT configuration, and no deployment automation.

**Verification:**

- [ ] Run backend unit/integration regression: `cmd /c ".\mvnw.cmd test"`.
- [ ] Run backend package verification: `cmd /c ".\mvnw.cmd package"`.
- [ ] Run frontend tests: `npm test`.
- [ ] Run frontend lint: `npm run lint`.
- [ ] Run frontend build: `npm run build`.
- [ ] Run a fresh V1–V6 migration against the existing PostgreSQL/pgvector setup and verify `REINDEX_REQUIRED`, exact-space retrieval, and activation rollback behavior.
- [ ] If credentials exist and all local checks pass, make the minimum live checks: one successful configured chat provider request, one fallback transition using a controlled failure fixture, and one embedding request. Do not retry 429 responses and do not make live checks a prerequisite for local correctness.
- [ ] Run `git diff --check`, `git status --short`, and `git log --oneline -12`. Confirm no secret-bearing files are tracked and no unrelated source changes are present.

**Verification and commit:**

- [ ] Self-review against every Global Constraint, Review Focus item, and final acceptance criterion.
- [ ] Commit with `docs: document AI Tutor multi-provider phase one`.

## External Credentials Needed During Implementation

- Local unit, contract, migration, and frontend verification requires no external provider credentials.
- Optional live chat verification requires `GROQ_API_KEY`, `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_API_TOKEN`, and `GEMINI_API_KEY` for whichever providers are intentionally enabled.
- Optional live embedding verification requires the Cloudflare account/token and `CLOUDFLARE_EMBEDDING_MODEL`, or the existing `GEMINI_API_KEY` and `GEMINI_EMBEDDING_MODEL` for Gemini fallback.
- Credentials are supplied through the process environment only. They are never committed, printed, copied into fixtures, or included in trace output.

## Migration Safety and Rollback Notes

- V1–V5 remain immutable. V6 is additive and is the only database migration planned for AI-1.
- V6 keeps old chunks and old active versions. Existing content without all four embedding metadata fields is marked `REINDEX_REQUIRED` and excluded from retrieval; no destructive delete is used.
- Indexing writes metadata and vectors together, validates dimension and exact space, and changes the active version only after the transaction succeeds. A failed job leaves the prior active version and retrieval space unchanged.
- Rollback before activation is a job retry or job cancellation. Rollback after activation is an explicit reactivation of the retained previous version after exact-space validation; it is not a migration rollback or a manual SQL delete.
- If V6 cannot apply cleanly to an existing database, stop the release, preserve the database, capture the migration error without secrets, and do not mark any new version indexed.
- Provider rollback is configuration-only: remove or disable the new provider, restore the prior ordered list, and keep Gemini as the compatible final adapter. No frontend contract change is required.

## Final Acceptance Criteria

- The backend starts when any optional provider is missing and returns a controlled unavailable response when no provider is usable.
- Groq success performs no fallback; Groq transient failure advances to Cloudflare; Cloudflare transient failure advances to Gemini; Gemini exhaustion returns controlled `AI_TEMPORARILY_UNAVAILABLE`; invalid requests and content blocks do not rotate.
- Health cooldown prevents repeated 429/5xx storms, fallback visits each provider at most once per request, and traces contain no secrets or raw payloads.
- Cloudflare embedding primary and Gemini embedding fallback reject dimension mismatch and select only exact provider/model/dimension/version spaces.
- V6 marks pre-metadata indexed content `REINDEX_REQUIRED`, retains old chunks, prevents mixed-space retrieval, and activates a reindexed version only after validation.
- Tutor context is resolved from authenticated server data, ownership is enforced, deterministic application-data answers make no external call, and client-forged answer keys/scores/bands do not affect output.
- Rule-based intent routing covers application data, generic chat, exercise explanation, RAG explanation, Writing feedback, Speaking text feedback, and progress/history without an LLM classifier.
- Reading, Listening, Writing, and Speaking preserve their current routes and normalized behavior; Writing outputs remain provider-neutral estimates; Speaking remains text/prompt-only.
- The existing frontend Tutor shell renders thinking, app-data, grounded, insufficient evidence, fallback, timeout, and temporary unavailable states with loading cleanup, source chips, keyboard access, mobile safety, and reduced-motion support. Raw provider payloads and provider names never reach normal UI copy.
- Backend tests, frontend tests, lint, build, migration verification, and package verification pass. Only focused AI-1 commits are present; nothing is pushed, merged, or deployed.

## Planned Commit Sequence

1. `feat: add AI provider capability configuration`
2. `feat: add provider-neutral AI chat router`
3. `feat: add Groq chat provider`
4. `feat: add Cloudflare chat provider`
5. `feat: add bounded provider fallback policy`
6. `feat: add Cloudflare embedding provider`
7. `feat: model exact RAG embedding spaces`
8. `feat: isolate RAG embedding metadata safely`
9. `feat: add trusted Tutor context resolution`
10. `feat: add deterministic Tutor application tools`
11. `feat: add deterministic Tutor intent routing`
12. `feat: orchestrate contextual Tutor requests`
13. `feat: connect Reading and Listening Tutor context`
14. `feat: add contextual Writing and Speaking Tutor flows`
15. `feat: add provider-neutral Tutor UI states`
16. `feat: harden Tutor provider quotas and tracing`
17. `docs: document AI Tutor multi-provider phase one`
