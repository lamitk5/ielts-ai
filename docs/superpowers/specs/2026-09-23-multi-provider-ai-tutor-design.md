# Multi-Provider AI Tutor Architecture Specification

Date: 2026-09-23  
Status: Design approved for implementation planning only  
Repository: `ielts-ai-tutor`  
Branch reviewed: `feature/phase-2b-rag`

This document defines the next architecture for a resilient, provider-neutral
IELTS Tutor. It is a design artifact only. It does not authorize production
implementation, dependency installation, provider-key changes, or a route
rewrite.

## 1. Current-state analysis

### Backend

The current backend is Spring Boot 4.1.1 on Java 21 with Spring MVC REST,
Spring WebFlux `WebClient`, Spring JDBC, PostgreSQL, Flyway, and pgvector.
The current application starts without a Gemini key; the provider reports a
normalized unavailable error only when a request needs Gemini.

The AI boundary is:

```text
AiChatController
  -> AiChatService
  -> RagChatService when the current classifier selects retrieval
  -> AiProvider / GeminiAiProvider
```

`AiProvider` exposes `chat(AiChatCommand)` and returns `AiChatResult`. The
command already carries a normalized learning context, a bounded client
history, a request ID, and an optional evidence string. `GeminiAiProvider`
owns the Gemini wire format, server-side key header, `gemini-3.8-flash`
configuration default, LOW thinking level, timeout, retry, malformed-response,
429, 503, and safe error mapping.

The RAG boundary is already separated into `EmbeddingProvider`, query
embedding, `VectorRetrievalService`, `RagContextBuilder`,
`GroundingValidator`, and `RagChatService`. RAG governance is enforced by SQL:
retrieval requires `APPROVED`, active, current-version, indexed, approved,
and post-approval data. Citations are filtered against retrieved source IDs.

The database currently contains:

- `rag_documents`, `rag_document_versions`, `rag_chunks`, and ingestion jobs;
- `vector(768)` chunk embeddings;
- `app_users` and bearer-token `auth_sessions`;
- `learning_attempts` and `learning_activity`;
- `writing_submissions`;
- `speaking_attempts` with optional text transcript and an explicit
  `STT_NOT_CONFIGURED` boundary.

There is no persistent AI conversation model. The current `/api/ai/chat`
request may carry at most eight history items from React, and
`AiChatService` does not yet resolve user, attempt, question, or progress
context from the authenticated server-side principal.

### Frontend

React uses the provider-neutral `/api/ai/chat` endpoint through
`aiTutorApi.js`. The Tutor UI renders application fields only: answer text,
grounding status, and normalized source chips. It already supports skeleton
loading, insufficient context, keyboard close/focus behavior, and the four
skill pages. `FloatingTutor` sends the most recent eight visible messages and
the page-provided context.

Reading and Listening currently use deterministic synthetic practice sets and
persist authenticated attempts. Writing persists provider-neutral assessment
records and displays `Band ước lượng`. Speaking persists text input while
clearly reporting that STT is not configured. The frontend has no provider
keys, provider URLs, or vendor payload assumptions.

### Configuration and operational findings

Gemini settings are environment-backed through `application.properties` and
`.env.example`. RAG defaults are top-k 5, minimum cosine similarity 0.72,
embedding batch size 32, and dimension 768. RAG administration uses a
temporary `X-Admin-Token` boundary. The existing Testcontainers integration
test is optional/skippable when the local Docker provider cannot be
initialized; this architecture does not make Testcontainers a runtime
dependency.

### Recent implementation patterns to preserve

- Constructor injection and small provider-neutral records are the dominant
  backend pattern.
- JDBC repositories enforce user ownership by binding `user_id` in every
  member-history query.
- Flyway owns schema changes.
- `SkeletonBlock` is the loading primitive for frontend data-heavy states.
- Existing error handlers return `{error:{code,message},timestamp}` and do not
  expose provider payloads or stack traces.

## 2. Goals

The target architecture must:

1. Keep the Tutor usable when any single AI provider is unavailable.
2. Use Groq as primary chat, Cloudflare Workers AI as chat fallback, and
   Gemini as the final chat fallback.
3. Use Cloudflare Workers AI as the preferred embedding provider and Gemini as
   an optional embedding fallback without mixing vector spaces.
4. Add Groq Whisper behind a provider-neutral speech-to-text boundary.
5. Answer application-known questions deterministically without an LLM call.
6. Resolve trusted learning context on the server from authenticated user and
   application IDs rather than trusting client-supplied answer keys.
7. Persist bounded, user-scoped conversation memory with summaries.
8. Preserve RAG governance, real provenance, insufficient evidence, and
   prompt-injection defenses.
9. Preserve the existing frontend Tutor visual language and normalize all
   provider responses before they reach React.
10. Let a teammate run one provider, any supported pair, or the full fallback
    chain by changing environment configuration only.

## 3. Non-goals

This design does not include:

- provider implementation, dependency installation, or live API verification;
- a rewrite of the existing React Tutor UI or Academic Luxury design system;
- an autonomous-agent framework, tool planner, chain-of-thought storage, or
  hidden reasoning UI;
- official IELTS affiliation or official score claims;
- automatic ingestion of copyrighted or unverified material;
- cross-user conversation search, social features, or collaborative chats;
- a full exam engine, browser-side answer-key authority, or fake transcript;
- replacing PostgreSQL/pgvector with a hosted vector database;
- changing `vector(768)` without a deliberate migration and reindex;
- removing the working Gemini adapter.

## 4. Architecture

The target request path is:

```text
React Tutor / skill page
  -> Spring Boot REST contract
  -> TutorOrchestrator
       -> authenticated user and request ownership
       -> TutorContextService
       -> TutorIntentRouter
       -> DeterministicTutorTools when application data is sufficient
       -> ConversationMemory for authenticated conversations
       -> RAG retrieval when evidence is required
       -> AiProviderRouter only when generation is required
  -> normalized TutorResponse
  -> React
```

The provider layer is:

```text
AiProviderRouter
  -> GroqAiProvider
  -> CloudflareAiProvider
  -> GeminiAiProvider

EmbeddingProviderRouter
  -> CloudflareEmbeddingProvider
  -> GeminiEmbeddingProvider

SpeechToTextProvider
  -> GroqWhisperProvider
```

The application owns intent, context, memory, source governance, error
normalization, and user ownership. Providers own only their own HTTP payload,
authentication header, model endpoint, response parsing, timeout, and
provider-specific failure translation.

`AiChatService` remains a compatibility facade during migration. The
controller continues to call the facade while the facade delegates to
`TutorOrchestrator`. This allows the provider router and context layers to be
introduced without changing the existing controller wiring in one commit.

### Proposed package boundaries

```text
com.ieltsaitutor.ai
  provider/          existing provider-neutral chat contract and adapters
  routing/           AiProviderRouter, health, retry and fallback policy
  model/             provider-neutral commands/results/failure categories

com.ieltsaitutor.tutor
  TutorOrchestrator
  TutorContextService
  TutorIntentRouter
  context/            trusted context records and resolvers
  intent/             intent enum and deterministic classification rules
  memory/             conversation repository, summary and bounded history
  tool/               deterministic application-data answers

com.ieltsaitutor.rag
  embedding/         provider router and embedding-space selection
  retrieval/         governed exact-profile vector retrieval
  chat/               evidence construction and citation validation

com.ieltsaitutor.speaking
  SpeechToTextProvider and GroqWhisperProvider
```

Each package communicates through application records and interfaces. A
provider adapter never imports a controller DTO, JDBC repository, or React
contract.

## 5. Components and interfaces

### `AiProvider`

Keep the current `AiProvider.chat(AiChatCommand)` compatibility method while
adding an internal adapter contract that supplies:

- stable provider ID (`GROQ`, `CLOUDFLARE`, `GEMINI`);
- enabled/configured state without exposing secrets;
- a provider-neutral response or typed failure;
- capability metadata for normal chat and structured Writing assessment.

The router, not business services, chooses an adapter. Existing Gemini tests
continue to exercise the adapter against a local HTTP stub.

### `AiProviderRouter`

`AiProviderRouter` implements the same application-facing chat interface so it
can replace direct `GeminiAiProvider` injection. It receives an ordered list
of configured adapters and a `ProviderRoutingPolicy`. It records request ID,
attempt count, provider duration, and outcome through a redacted metrics/log
interface.

The router returns only `AiChatResult` or a normalized `AiProviderException`.
It never returns Groq, Cloudflare, or Gemini JSON to a controller.

### `EmbeddingProviderRouter`

The router selects an `EmbeddingSpace` consisting of:

```text
provider
model
dimension
version
```

It can call Cloudflare first and Gemini second, but it passes the selected
space into indexing and retrieval. A query embedding is usable only against
chunks recorded in exactly the same space.

### `TutorOrchestrator`

The orchestrator is the sole application owner of the end-to-end Tutor flow.
It coordinates context resolution, intent routing, deterministic tools,
memory, RAG, generation, persistence, and response normalization. It does not
know vendor request schemas.

### `TutorContextService`

The service accepts an authenticated principal, trusted context references,
and the current request. It returns a `TutorLearningContext` with only records
owned by that user or publicly available catalog data. Missing references are
represented as absent context, not guessed values.

### `TutorIntentRouter`

The router returns a small enum and a route policy, not a free-form agent plan.
The initial policy is deterministic and rule-based. It does not make an LLM
call merely to decide whether another LLM should be called.

### `ConversationMemory`

The memory service loads the active summary plus a configured recent-message
window, persists visible user/assistant messages, and creates a replacement
summary when thresholds are reached. It rejects a conversation whose owner
does not match the authenticated principal.

### `SpeechToTextProvider`

The interface accepts validated audio metadata and a temporary file reference,
then returns a provider-neutral transcript result with status, text when
available, language metadata when available, and provider-neutral failure
code. It never returns a fabricated transcript.

## 6. Provider routing

### Chat order and configuration

The ordered chat policy is:

```text
Groq -> Cloudflare Workers AI -> Gemini
```

The application starts when none, one, or several provider keys are present.
An adapter with a missing required key or model is disabled and reported as
`NOT_CONFIGURED`; it does not prevent startup. The current Gemini default
remains `GEMINI_MODEL=gemini-3.8-flash` and LOW thinking remains inside the
Gemini adapter.

Configuration is environment-backed:

```text
GROQ_API_KEY
GROQ_CHAT_MODEL
GROQ_BASE_URL
GROQ_CONNECT_TIMEOUT
GROQ_RESPONSE_TIMEOUT

CLOUDFLARE_ACCOUNT_ID
CLOUDFLARE_API_TOKEN
CLOUDFLARE_CHAT_MODEL
CLOUDFLARE_BASE_URL
CLOUDFLARE_CONNECT_TIMEOUT
CLOUDFLARE_RESPONSE_TIMEOUT

GEMINI_API_KEY
GEMINI_MODEL
GEMINI_BASE_URL
GEMINI_CONNECT_TIMEOUT
GEMINI_RESPONSE_TIMEOUT
GEMINI_MAX_RETRIES
```

Groq and Cloudflare model values are required when that provider is enabled;
the design does not invent a model name. All URLs are configurable and keys
remain backend-only.

### Fallback policy

The provider failure taxonomy is:

- `TRANSIENT`: network failure, timeout, 429, or 5xx;
- `NOT_CONFIGURED`: missing key, account, model, or endpoint configuration;
- `AUTHENTICATION`: provider rejected credentials;
- `MALFORMED_RESPONSE`: provider returned an invalid response;
- `INVALID_REQUEST`: provider rejected a validly authenticated request with
  a 4xx client-error condition;
- `CONTENT_BLOCKED`: the provider refused content for a policy reason.

Routing decisions are:

| Failure | Fallback decision |
|---|---|
| success | return normalized answer |
| timeout, network, 429, 5xx | try the next healthy provider once |
| missing configuration | skip provider and try the next one |
| authentication/configuration rejection | mark provider unavailable for the request, then try the next one |
| malformed response | try the next provider once and record the adapter failure |
| invalid request | stop; do not blindly resend the same invalid request |
| content blocked | stop with a safe Tutor response; do not route around a provider safety decision |
| all providers unavailable | return HTTP 503 with `AI_TEMPORARILY_UNAVAILABLE` |

There is no repeated external retry for 429. Network and timeout retries are
bounded to one local retry inside an adapter only when the provider policy
allows it; after that, the router advances. A fallback provider receives the
same normalized command and no provider-specific payload.

### Health and circuit breaking

Health is process-local and observable through metrics, not exposed as raw
provider state to React. Each adapter has:

- a rolling failure threshold configured as 3 transient failures;
- a rolling window default of 60 seconds;
- an open cooldown default of 30 seconds;
- one half-open probe after cooldown.

These values are configuration properties, not literals in business logic. A
process restart resets health state. The router still performs a normal
fallback when a provider is open. Provider health never disables deterministic
application-data answers.

### Request tracing

Every request receives a UUID request ID. Logs record route category, provider
ID, duration, status, fallback count, and safe error category. Logs do not
record API keys, authorization headers, full prompts, essays, audio, raw
provider payloads, or full retrieved documents.

## 7. Learning context

`TutorContextService` is the trust boundary between React references and
server-side learning data.

### Context record

The resolved context contains, where authorized and available:

```text
userId, skill
practiceSetId, attemptId, questionId
passage/task reference, question type
learner answer, deterministic correct answer, result and explanation
writing task/draft reference
speaking part/prompt/transcript reference
recent mistakes, recent activity, skill progress
```

The request may send IDs from `AiChatContext`, but the service resolves them
from the catalog and repositories. It does not accept `correctAnswer`, band,
progress, or ownership as client-provided facts. The answer key comes only
from the server-side practice catalog or a trusted persisted attempt.

### Guest and member behavior

Guest chat may use the current page context for generic conversation, but it
has no persistent personal memory and cannot request private progress,
attempts, mistakes, drafts, or transcripts. A guest request for private data
returns a safe authentication-required answer without an LLM call.

Authenticated requests use `AuthInterceptor`'s `AuthPrincipal` and enforce
ownership before loading every attempt, conversation, writing submission, or
speaking record.

### Context freshness

The service resolves current data for every context-dependent request. It does
not trust a stale client snapshot when an attempt or question ID is supplied.
Only a bounded context summary is sent to a provider; raw answer keys and
unnecessary private history are excluded.

## 8. Conversation memory

Persistent memory is available for authenticated users. Guest requests remain
ephemeral and use only the current request plus the bounded legacy history
until the user signs in.

### Storage model

The proposed model is:

```text
ai_conversations
  id, user_id, skill, practice_set_id, attempt_id, question_id,
  title, status, created_at, updated_at, last_message_at

ai_messages
  id, conversation_id, sequence_no, role, content, response_status,
  grounding_status, citations_json, context_snapshot_json,
  created_at

ai_conversation_summaries
  id, conversation_id, revision, summary_text,
  covered_through_sequence, created_at
```

`user_id` is mandatory for persisted conversations. Foreign keys cascade only
from a user to its private conversation data; cross-user reads are rejected
before repository access. A unique `(conversation_id, sequence_no)` constraint
preserves ordering. Indexes cover `(user_id, updated_at desc)` and
`(conversation_id, sequence_no desc)`.

Only visible user messages, visible assistant answers, normalized status, and
validated citation snapshots are persisted. Hidden reasoning, provider
payloads, API headers, embeddings, and full retrieved documents are not
persisted.

### Memory policy

The configured defaults are:

```text
TUTOR_MEMORY_RECENT_MESSAGES=8
TUTOR_MEMORY_SUMMARY_TRIGGER_MESSAGES=12
TUTOR_MEMORY_SUMMARY_MAX_TOKENS=1000
TUTOR_MEMORY_MAX_CONVERSATION_MESSAGES=200
```

The memory service loads the active summary plus the latest eight visible
messages. When twelve unsummarized messages or the summary token budget is
exceeded, a bounded summarization operation replaces the covered range. The
summary is generated through the same provider router with a dedicated low
cost task instruction, then validated for size and ownership. If summarizing
fails, the conversation remains usable with the recent window and does not
resend the entire history.

The request body may continue accepting `history` for backward compatibility.
For an authenticated conversation ID, server memory is authoritative and the
client history is ignored except for compatibility telemetry. A client cannot
inject assistant messages into persistent memory by posting arbitrary history.

## 9. Deterministic Tutor tools

The first route decision is whether the application already knows the answer.
The following bounded tool set is sufficient:

| Tool | Trusted source | Example answer |
|---|---|---|
| `CurrentExerciseTool` | practice catalog + context IDs | current set/question/prompt |
| `SelectedAnswerTool` | authenticated attempt or current submitted answer | the learner's selected answer |
| `CorrectAnswerTool` | server-side catalog answer key | correct answer and stored explanation |
| `ProgressTool` | `LearningRepository` | latest four-skill progress |
| `HistoryTool` | owned learning/writing/speaking records | recent activity or mistakes |
| `WritingDraftTool` | owned writing submission/request draft | current draft metadata and text within a bounded limit |
| `SpeakingPromptTool` | speaking prompt catalog and owned attempt | part, prompt, and transcript status |

Deterministic tool results are returned directly with `grounding.status` set
to `APP_DATA` (or the compatible application-level equivalent) and no source
citations. They never call a chat provider.

Questions such as “What question am I doing?”, “What answer did I choose?”,
“What was my Reading score?”, and “Show my recent mistakes” use these tools.

Questions such as “Why is my answer wrong?” may first resolve the answer,
correct key, and stored explanation deterministically, then invoke RAG plus a
provider only for the pedagogical explanation. The tool result is included as
trusted application context, while the retrieved material remains explicitly
marked as untrusted evidence in the generation prompt.

## 10. RAG and embedding model isolation

### Governance preserved

Retrieval remains eligible only when all existing conditions hold:

```text
rights_status = APPROVED
active = true
current_version_id = version.id
index_status = INDEXED
approved_at is not null
indexed_at >= approved_at
```

Skill and language filters remain applied before similarity ranking. Rejected,
restricted, inactive, superseded, and not-yet-indexed material never reaches
the Tutor prompt.

### Embedding space

Each indexed document version records:

```text
embedding_provider
embedding_model
embedding_dimension
embedding_version
```

The composite embedding-space key is immutable for an indexed version. The
configured initial dimension remains exactly 768. Cloudflare must return 768
values for the selected model; otherwise the adapter returns
`RAG_EMBEDDING_DIMENSION_MISMATCH` and the version remains unindexed. Gemini
continues to validate its configured output in the same way.

The preferred configuration is:

```text
RAG_EMBEDDING_PROVIDER=CLOUDFLARE
CLOUDFLARE_EMBEDDING_MODEL is required when Cloudflare embeddings are enabled
GEMINI_EMBEDDING_MODEL is required when Gemini embedding fallback is enabled
RAG_EMBEDDING_DIMENSION=768
RAG_EMBEDDING_VERSION=v1
```

No model name is invented in the source code. A provider is enabled only when
its credentials and model are configured.

### Provider fallback without vector mixing

The embedding router may try Cloudflare first and Gemini second, but it never
uses a Gemini query vector against a Cloudflare-only index. It selects an exact
embedding space and adds exact provider/model/dimension/version filters to
retrieval.

If Cloudflare is unavailable and Gemini-indexed content exists for the same
skill and policy, the router may query that Gemini space. If no compatible
space exists, retrieval returns `RAG_EMBEDDING_UNAVAILABLE` or insufficient
evidence; it does not silently cross spaces or fabricate citations.

Indexing a document during Cloudflare outage may use Gemini only if the job
records Gemini's embedding space and the resulting version is independently
retrievable with Gemini. A later preferred-provider recovery does not mutate
that vector in place.

### Migration and reindex

The embedding metadata migration must:

1. add nullable provider/model/dimension/version fields to document versions;
2. mark existing indexed versions without trustworthy metadata as stale or
   not indexed for retrieval;
3. retain old chunks for audit rather than deleting them;
4. require explicit reindexing into a known embedding space;
5. switch `current_version_id` only after the new version has passed governance,
   dimension validation, and transactional chunk insertion.

Changing model, provider, preprocessing, or embedding version requires a new
version and reindex. The admin response shows the embedding space and
reindex-required state without exposing vectors or secrets.

### Grounded response and provenance

`RagContextBuilder` remains the only component that converts retrieved chunks
to bounded evidence. `GroundingValidator` emits citations only for chunks in
the current retrieval result. A provider fallback receives the same evidence;
provider selection never creates a citation. If evidence is absent or below
threshold, the response is:

```text
status = INSUFFICIENT_CONTEXT
grounding.status = INSUFFICIENT_EVIDENCE
grounding.ragEnabled = true
sources = []
```

Prompt injection text inside a source remains data inside a delimited evidence
block. It cannot override the system instruction or change the citation set.

## 11. Speaking STT

### Interface and provider

The application adds:

```text
SpeechToTextProvider.transcribe(ValidatedAudioRequest)
```

`GroqWhisperProvider` is the first implementation. It uses a backend-only
`GROQ_API_KEY`, a configured `GROQ_STT_MODEL`, timeout, and multipart provider
request. It returns a normalized `TranscriptResult`; Groq response fields do
not cross the controller boundary.

### Request flow

```text
browser MediaRecorder
  -> multipart speaking upload
  -> authenticated ownership check
  -> MIME/size/duration validation
  -> temporary file outside web root
  -> GroqWhisperProvider
  -> transcript validation
  -> speaking_attempts persistence
  -> normalized transcript/status to React
```

Accepted MIME types are `audio/webm`, `audio/ogg`, `audio/wav`, `audio/mpeg`,
and `audio/mp4`. The default maximum is 25 MiB and the default maximum
duration is 180 seconds; both are environment-configurable. Duration is
validated from trusted media metadata where available and from a bounded
server-side decoder check where required. The temporary file is deleted in a
`finally` path after provider completion or failure.

The upload endpoint requires an authenticated owner and accepts an attempt ID
created for that owner. It rejects path names, content types, oversized data,
and unknown attempts. An unsupported browser can keep the existing text-input
fallback. No transcript is created when STT fails.

Normalized states are `STT_PROCESSING`, `STT_COMPLETED`,
`STT_UNAVAILABLE`, `STT_INVALID_AUDIO`, and `STT_NOT_CONFIGURED`.

### Persistence

`speaking_attempts` gains status and provenance fields for transcript provider,
model, MIME type, byte size, duration, and safe provider-neutral error code.
Raw audio is temporary by default. Long-term audio storage is not part of this
phase; if introduced later it requires explicit retention and deletion policy.

## 12. Writing integration

The existing `WritingAssessment` contract remains provider-neutral:

```text
overallBandEstimate
criteria
strengths
issues
suggestions
citations
grounding
disclaimer
```

`WritingAssessmentService` delegates generation through `AiProviderRouter`,
not directly to Gemini. The service supplies the trusted Writing draft,
task type, and approved rubric evidence through the orchestrator. It parses
the same application result schema regardless of provider.

Task 1 uses `Task Achievement (TA)`; Task 2 uses `Task Response (TR)`. Both
use `Coherence & Cohesion (CC)`, `Lexical Resource (LR)`, and `Grammatical
Range & Accuracy (GRA)`. Invalid JSON, invalid band range, or missing
criteria produces `UNAVAILABLE` and never a fake estimate. Provider fallback
may be attempted for transient or malformed output; a 400-style invalid
request is not blindly repeated.

Persisted citations are snapshots of validated retrieved sources. The React
page continues to display `Band ước lượng` and the non-official disclaimer.

## 13. Database changes

All changes are Flyway migrations. Existing V1–V5 are not edited.

### V6: embedding space metadata

Add nullable fields to `rag_document_versions`:

```text
embedding_provider VARCHAR(32)
embedding_model VARCHAR(240)
embedding_dimension INTEGER
embedding_version VARCHAR(64)
```

Add a check that a fully indexed version has all four fields and dimension
768. Existing indexed versions without trustworthy metadata are marked not
retrievable until reindexed. Retrieval joins versions and filters the exact
active embedding space.

### V7: conversation memory

Create `ai_conversations`, `ai_messages`, and
`ai_conversation_summaries` with UUID IDs, UTC timestamps, user ownership,
skill/context references, visible message content, normalized status,
grounding status, bounded citation snapshots, and sequence ordering.

Foreign keys reference `app_users` and use restrictive behavior for learning
references so a conversation cannot silently point at a deleted private
record. A user index supports list/resume operations; a conversation index
supports recent-window retrieval.

### V8: speaking transcription metadata

Extend `speaking_attempts` with:

```text
transcription_status
transcription_provider
transcription_model
audio_mime_type
audio_size_bytes
audio_duration_ms
transcription_error_code
```

The migration permits the existing text-only boundary and does not require
stored audio paths.

### Optional operational metrics

Provider latency, fallback counts, and token estimates are emitted through a
backend metrics interface and structured logs first. They are not stored in a
new business table until retention and access requirements are approved.

## 14. API changes

### Existing endpoint preserved

`POST /api/ai/chat` remains the compatibility entry point. The request keeps:

```json
{
  "message": "...",
  "context": {
    "skill": "READING",
    "lessonId": "...",
    "exerciseId": "...",
    "questionId": "...",
    "taskType": "...",
    "errorLocation": "...",
    "selectedText": "..."
  },
  "history": []
}
```

An optional `conversationId` is added for authenticated persistent memory.
When present, it must belong to the authenticated principal. Existing clients
that omit it remain functional.

The response keeps `status`, `answer`, `sources`, `grounding`, `meta`, and
`timestamp`. `meta` adds nullable `messageId` and `conversationId`; it does
not expose provider ID or raw vendor metadata. `sources` remains compatible
with the current source-chip fields and may include nullable version, page,
and chunk ID.

Successful application statuses are `ANSWERED` and `INSUFFICIENT_CONTEXT`.
Temporary provider/RAG failures use HTTP 503 with the existing normalized
error envelope. Invalid input uses HTTP 400. No provider-specific status or
payload reaches React.

### Conversation endpoints

The smallest persistent-memory API is:

```text
POST /api/ai/conversations
GET  /api/ai/conversations
GET  /api/ai/conversations/{id}/messages
POST /api/ai/chat with conversationId
```

All four require authentication. List and message endpoints return bounded
visible content and normalized citations only. They enforce owner equality in
the service and repository.

### Speaking endpoint

Add an authenticated multipart endpoint under
`/api/practice/speaking/attempts/{id}/audio` for validated recording upload.
The existing JSON text attempt endpoint remains available for unsupported
browsers and deterministic tests.

### Admin RAG endpoints

The current `/api/admin/rag/**` surface and temporary token authorization
remain. New embedding-space metadata is shown in bounded document detail and
job status. No vector or provider secret is exposed.

## 15. Frontend behavior

The current Tutor components remain the visual shell. Only the service
normalization and state model expand.

### Tutor states

The UI supports:

- `idle`;
- `thinking` with `SkeletonBlock` and `aria-busy`;
- deterministic `app_data` answer;
- grounded answer with source chips;
- provider fallback answer without a provider label;
- `insufficient_context` with a clear next-step prompt;
- temporary AI unavailable with a retry action;
- STT processing and completed transcript;
- STT unavailable or invalid audio with a text fallback.

The UI never displays raw provider names, raw status payloads, stack traces,
API keys, or hidden reasoning. A provider fallback is communicated as a
normal Tutor response unless a user action is required.

### Context references

Skill pages send IDs such as skill, practice set, attempt, question, task, or
prompt references. They do not serialize the complete answer key, progress
object, or full history into the free-form message. The backend resolves those
references and returns the same provider-neutral response shape.

### Conversation UX

The existing floating panel may continue using an ephemeral session while the
conversation endpoint is introduced. Once connected, it stores only the
conversation ID and visible message list, then asks the backend for bounded
history. Reopening the panel never causes the frontend to resend an unbounded
transcript.

### Responsive and accessibility requirements

The current keyboard focus, Escape close, focus-visible styles, source-chip
labels, skeleton labels, and reduced-motion behavior remain. New states use
live regions without trapping focus on provider errors. Recording controls
have text labels and a non-audio fallback. Mobile upload and Tutor controls
remain viewport-safe.

## 16. Security

- Provider keys, account IDs, admin tokens, database credentials, and model
  endpoints remain server-side environment configuration.
- React never receives provider-specific payloads or embeds secrets in the
  bundle.
- `AuthPrincipal` is the source of authenticated user identity.
- Conversation, attempt, writing, speaking, progress, and draft reads require
  owner equality; unknown IDs return the same safe not-found/unauthorized
  behavior without cross-user disclosure.
- Client answer keys, bands, progress, citations, and ownership fields are
  never trusted.
- Multipart audio is validated by MIME, size, duration, and authenticated
  owner. Files are temporary and stored outside the web root.
- RAG admin actions remain restricted to `/api/admin/rag/**`; the existing
  token is never logged or placed in a URL.
- RAG evidence is delimited and labeled untrusted. Source text cannot change
  system instructions, tools, routing, or citations.
- Provider error responses are mapped to safe application messages. Logs use
  request IDs and redact prompts, essays, transcripts, tokens, secrets,
  headers, raw documents, and embeddings.
- Rate limits apply before external calls: default 10 Tutor requests per
  minute per guest IP and 30 per minute per authenticated user, configurable
  and enforced independently of provider quotas.

## 17. Quota and cost strategy

The cost policy is ordered by cheapest trusted answer:

1. resolve deterministic application data;
2. reuse active conversation summary plus recent messages;
3. retrieve at most configured top-k evidence, default 5;
4. avoid duplicate embedding and provider calls for the same request ID;
5. call one chat provider and fall back once per configured order;
6. record latency, route, approximate token usage, and fallback count.

The following defaults are configurable:

```text
RAG_TOP_K=5
RAG_MIN_SIMILARITY=0.72
TUTOR_MEMORY_RECENT_MESSAGES=8
TUTOR_MEMORY_SUMMARY_TRIGGER_MESSAGES=12
TUTOR_MAX_OUTPUT_TOKENS=1024
```

Safe cache candidates are approved public retrieval results keyed by normalized
query, skill/language filters, and exact embedding-space key with a short TTL,
and deterministic catalog responses keyed by immutable catalog version.

Personalized Writing feedback, progress answers, private attempts,
conversation responses, and STT transcripts are not cached by prompt text
alone. A cache entry is invalidated by document activation/deactivation,
embedding-version change, or catalog revision.

429 responses are not repeatedly retried. Provider cooldown and fallback
prevent quota storms. Requests that do not need generative AI never consume an
external provider quota.

## 18. Failure handling

### Deterministic route

If required application data is missing, stale, or unauthorized, return a
clear application-level message and do not ask an LLM to guess. The response
contains no fabricated score, answer key, or citation.

### Generic chat

The router attempts Groq, then Cloudflare, then Gemini according to health and
configuration. If all fail, HTTP 503 uses `AI_TEMPORARILY_UNAVAILABLE`; the
frontend shows a friendly retry state and does not remain in an indefinite
loading state.

### RAG route

- no eligible chunks: `INSUFFICIENT_CONTEXT`, `INSUFFICIENT_EVIDENCE`,
  `sources=[]`;
- embedding dimension/profile mismatch: controlled RAG unavailable state;
- provider failure after retrieval: safe temporary-unavailable response,
  `sources=[]` unless a complete grounded answer was actually produced;
- provider fallback after retrieval: reuse the same evidence and validate
  citations against the same retrieved chunk IDs;
- malformed answer: reject it, optionally try the next provider according to
  policy, and never expose invalid JSON or fabricated citations.

### STT route

Invalid audio returns a validation error. Provider timeout, 429, 5xx, or
missing configuration returns `STT_UNAVAILABLE` with no transcript persisted.
The UI offers text input. A provider response with empty or malformed text is
treated as failure, never as an empty successful transcript.

### Persistence failures

A failed message/conversation write is logged with request ID and surfaced as
a safe temporary-unavailable response. The system does not claim that a
message was saved when persistence failed. Database transactions keep an
assistant message and its normalized citation snapshot consistent.

## 19. Testing

All external providers are mocked in automated tests. Live calls remain
minimal manual verification and are never required for the normal test suite.

### Provider router

Tests cover:

- Groq success;
- Groq 429 or timeout then Cloudflare success;
- Groq and Cloudflare unavailable then Gemini success;
- all providers unavailable;
- malformed response fallback;
- invalid request with no unsafe fallback;
- missing provider configuration;
- circuit open/half-open behavior;
- no repeated 429 storm.

### Intent and context

Tests cover deterministic current-question, selected-answer, correct-answer,
progress, and history routes; generic chat; current-exercise explanation;
RAG/material questions; Writing feedback; Speaking feedback; and ownership
rejection. Tests prove a client-supplied answer key cannot override the server
catalog.

### Conversation memory

Tests cover recent-window limits, summary threshold, summary failure fallback,
message ordering, conversation owner isolation, maximum message retention,
and guest non-persistence.

### Embedding and RAG

Tests cover Cloudflare/Gemini adapter parsing, exact 768 dimensions,
dimension mismatch, provider/model/version isolation, reindex requirement,
governance filters, inactive exclusion, insufficient evidence, source
provenance, prompt-injection defense, and retrieval fallback only to a
compatible embedding space.

### STT

Tests cover successful transcript, malformed provider response, missing key,
timeout/429, invalid MIME, oversized upload, excessive duration, file cleanup,
ownership, and unsupported-browser text fallback.

### Writing and frontend

Backend tests verify provider-neutral Writing parsing, Task 1 versus Task 2
criterion labels, invalid band handling, disclaimer, and source snapshots.
Frontend tests verify deterministic answers, fallback without infinite loading,
grounded source chips, insufficient evidence, friendly unavailable state,
conversation ID handling, and STT processing/unavailable states. Existing
frontend tests must continue to pass without a backend process or live keys.

## 20. Migration sequence

The implementation is incremental and each step is independently testable:

1. **Provider-neutral routing foundation**: typed provider failures,
   adapter capability/health contract, `AiProviderRouter`, and compatibility
   injection in `AiChatService`.
2. **Groq chat**: environment properties, adapter, local HTTP contract tests,
   and disabled-without-key startup behavior.
3. **Cloudflare chat**: adapter, normalized response parsing, and fallback
   tests.
4. **Fallback policy**: ordered routing, circuit state, metrics, 429 policy,
   and safe all-unavailable errors; keep Gemini unchanged as final fallback.
5. **Embedding routing and metadata**: Cloudflare adapter, Gemini adapter
   registration, exact-space selection, V6 migration, and reindex enforcement.
6. **Trusted Tutor context**: principal-aware `TutorContextService` and
   ownership tests for practice, progress, Writing, and Speaking.
7. **Deterministic Tutor tools**: application-data route before generation,
   with no external calls for known answers.
8. **Conversation memory**: V7 migration, repository, summary policy,
   optional conversation ID, and compatibility facade behavior.
9. **Contextual RAG Tutor**: move existing RAG orchestration behind
   `TutorOrchestrator`, preserve governance/citations/insufficient evidence,
   and test provider fallback with fixed evidence.
10. **Groq Whisper STT**: interface, validation, temporary files, V8 metadata,
    and backend endpoint with mocked provider tests.
11. **Writing/Speaking integration**: route Writing through the provider
    router, persist normalized citations, and connect Speaking transcript
    status without changing score claims.
12. **Frontend state wiring**: extend API normalization and Tutor/Speaking
    states while preserving current components, accessibility, and reduced
    motion.
13. **Documentation and operational hardening**: update `.env.example`,
    README onboarding, redacted observability, security regression tests, and
    final targeted/full verification.

No step deletes Gemini. Each step leaves the application runnable with
Gemini-only configuration, Groq-only chat configuration where RAG is not
needed, Cloudflare-only supported configuration, or the full multi-provider
configuration.

## 21. Git and onboarding

The repository remains cloneable with no local secrets or generated provider
artifacts committed. `.env.example` documents names, safe sample values, and
which provider capability each key enables; it contains no usable key.

README onboarding will specify:

```text
git clone the repository
copy backend/.env.example to a local environment file
configure PostgreSQL/pgvector through docker compose
choose at least one chat provider
choose an embedding provider for RAG
run ./mvnw.cmd spring-boot:run from backend
run npm install and npm run dev from frontend
```

The README will explain four supported modes:

- Gemini-only: current Gemini chat plus Gemini embeddings;
- Groq-only chat: deterministic Tutor paths and Groq generation where RAG
  embedding data is already compatible;
- Cloudflare-supported mode: Cloudflare chat/embeddings with configured
  account and model values;
- multi-provider: Groq, Cloudflare, and Gemini fallback chain.

Provider setup is selected through environment variables. No Java source,
React source, route, or database credential needs editing. Local upload files,
audio, cache entries, and generated model data stay ignored by Git.

## 22. Acceptance criteria

The design is complete when implementation and verification demonstrate that:

1. The application starts with no Gemini key when another configured chat
   provider is available, and starts with all providers disabled while still
   serving deterministic/public routes.
2. Generic `hello` uses the configured provider order and falls back after a
   transient failure without exposing provider details.
3. 400 invalid requests do not trigger unsafe repeated fallback.
4. Deterministic current-question, answer, progress, and history questions do
   not call an external LLM.
5. Context-dependent explanations resolve trusted user-owned context on the
   server and cannot be supplied with a client-forged answer key.
6. Authenticated conversations load only the owning user's summary and recent
   messages; guest conversations are not persisted.
7. Conversation memory is bounded, summarized at configured thresholds, and
   never resends an unbounded transcript.
8. RAG retrieves only approved, active, current, indexed, post-approval
   versions in an exact embedding space.
9. Cloudflare and Gemini embeddings cannot be mixed silently; all indexed
   vectors remain dimension 768 and model/version metadata is queryable.
10. A model/provider change requires explicit reindexing before retrieval.
11. Grounded answers contain only citations from actual retrieved chunks;
    insufficient evidence returns no citations.
12. Writing results preserve the provider-neutral schema, Task 1/Task 2
    criterion distinction, estimate label, and disclaimer.
13. Speaking uploads validate ownership, MIME, size, duration, and lifecycle;
    STT failure never fabricates a transcript.
14. React receives only normalized application responses and supports thinking,
    grounded, application-data, insufficient-evidence, fallback, unavailable,
    and STT states without an indefinite spinner.
15. API keys, tokens, raw vendor payloads, full documents, embeddings, hidden
    reasoning, and private cross-user data never reach the browser or logs.
16. Unit, controller, provider-stub, ownership, RAG governance, embedding,
    STT, frontend, lint, and build verification pass without requiring live
    provider calls.
17. The implementation sequence can be delivered in focused commits without
    a one-shot rewrite of the existing AI/RAG subsystem.
