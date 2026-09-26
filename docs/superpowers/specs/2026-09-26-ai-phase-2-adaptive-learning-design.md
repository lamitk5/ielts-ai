# AI Phase 2 — Adaptive Learning Intelligence Design Specification

Date: 2026-09-26  
Status: Design only — approved for human review before implementation planning  
Repository: `ielts-ai-tutor`  
Branch reviewed: `feature/phase-2b-rag`

This document designs the next learning-intelligence layer for IELTS AI
Tutor. It does not implement code, create migrations, change API behavior,
call providers, or authorize AI Phase 3.

## 1. Purpose

AI Phase 2 evolves the current contextual Tutor into a personalized English
and IELTS learning assistant. It will understand trusted learning evidence —
attempts, answers, explanations, writing feedback, speaking text context,
mistake patterns, progress, goals, and recent activity — and use that
evidence to explain errors, suggest useful practice, discuss progress, and
maintain a small actionable roadmap.

The system of record remains the application database. Deterministic backend
services calculate learning facts. AI providers may explain, summarize, and
phrase advice, but may not invent learner scores, answer keys, history,
weaknesses, or roadmap evidence.

## 2. Current System Baseline

### 2.1 Backend and database

The project is a Java 21 Spring Boot modular monolith using Spring MVC,
WebFlux `WebClient`, JDBC, Flyway, PostgreSQL, and pgvector. Existing schema
migrations are V1–V6:

- `app_users` and `auth_sessions` provide bearer-session authentication and
  customer/admin roles.
- `learning_attempts` stores user-owned skill, score, total, set ID, answer
  payload, and timestamp.
- `learning_activity` stores user-owned activity type, reference, optional
  score, and timestamp.
- `writing_submissions` stores user-owned response text, word count,
  assessment payload, status, and timestamp.
- `speaking_attempts` stores user-owned prompt, optional text transcript,
  optional audio filename, status, optional band estimate, and timestamp.
- RAG tables store governed document versions, chunks, `vector(768)`
  embeddings, and V6 embedding-space metadata.

Reading and Listening currently use a synthetic server-side catalog and
persist authenticated attempts through `PracticeAttemptStore`. Review data
contains the selected answer, trusted correct answer, correctness, and
explanation at response time, while the persisted attempt stores the answer
payload and score. Writing stores provider-neutral assessment results and
labels AI-produced scores as `Band ước lượng`. Speaking deliberately has a
text-input boundary and reports `STT_NOT_CONFIGURED` when no transcript is
available; the application does not claim pronunciation analysis.

### 2.2 Existing Tutor and AI boundaries

The current request path is:

```text
AiChatController
  -> AiChatService compatibility facade
  -> TutorOrchestrator
       -> TutorContextService
       -> TutorIntentRouter
       -> DeterministicTutorTools
       -> RAG or provider-neutral AiProvider
```

`TutorContextService` resolves references against authenticated ownership and
the trusted practice catalog. `TutorContextRequest` contains IDs, not
client-provided answer keys, scores, or bands. `TutorLearningContext` removes
raw client data and bounds context size. `TutorIntentRouter` already routes
current-question, selected-answer, score, progress/history, exercise
explanation, Writing, Speaking, RAG, and generic requests. Deterministic
application-data requests bypass AI and RAG.

`AiProviderRouter` already owns the Phase AI-1 chat order:

```text
Groq -> Cloudflare Workers AI -> Gemini
```

`EmbeddingProviderRouter`, exact embedding-space metadata, RAG governance,
grounding validation, source provenance, insufficient-evidence behavior, and
normalized provider failures are existing boundaries to preserve. Phase 2
must not create a second provider architecture or expose vendor payloads.

### 2.3 Existing frontend boundaries

React/Vite uses React Router and provider-neutral service modules. The
homepage contains Hero, four skill cards, progress, Tutor preview, final CTA,
and `FloatingTutor`. `ProgressOverviewSection` already has guest preview and
member radar/countdown/mistake composition. Practice pages pass only bounded
skill/set/question/attempt references to `FloatingTutor`. Writing and
Speaking pages pass task/prompt/attempt references and display explicit
assessment/STT boundaries.

`aiTutorApi.js` normalizes response status, grounding, and source chips.
`FloatingTutor` and `TutorPanel` provide loading skeletons, retry, focus,
keyboard close, viewport-safe panel behavior, and provider-neutral errors.
`GlassCard`, `AnimatedSection`, `SectionTitle`, `SkeletonBlock`, Recharts,
and the Academic Luxury palette are the shared presentation primitives.

### 2.4 Reusable versus extendable components

Reusable without duplication:

- `AuthInterceptor`, `AuthPrincipal`, and user-scoped JDBC query patterns;
- `LearningRepository`, `LearningProgressService`, and existing attempt/
  activity records;
- `PracticeAttemptStore`, `SyntheticPracticeCatalog`, and server-side
  Reading/Listening answer validation;
- `WritingRepository`/`WritingAssessment` and `SpeakingRepository`/
  `SpeakingAttempt`;
- `TutorContextService`, `TutorOrchestrator`, `TutorIntentRouter`,
  `DeterministicTutorTools`, `RagChatService`, and `AiProviderRouter`;
- `aiTutorApi.js`, `FloatingTutor`, `TutorPanel`, `SkeletonBlock`, source
  chips, shared cards, section titles, and dashboard chart primitives.

Extend rather than replace:

- `TutorContextService` gains bounded profile, mistake, roadmap, and recent
  activity context resolvers.
- `TutorOrchestrator` gains scope, conversation, and learning-intelligence
  coordination while retaining deterministic-first routing.
- `TutorIntentRouter` gains progress/roadmap/mistake/scope intents without
  becoming an open-ended agent planner.
- `LearningProgressService` reads a materialized intelligence snapshot while
  keeping existing progress endpoints compatible.
- Practice, Writing, and Speaking application services emit validated
  learning events at domain boundaries.
- `HomePage`, member dashboard sections, and `FloatingTutor` consume new
  provider-neutral learning DTOs with existing loading and empty states.

New Phase 2 concepts are intentionally separate services: event recording,
mistake analysis, weakness/strength detection, trend calculation, roadmap
planning, conversation memory, and scope policy. No giant `TutorService` or
second progress system is introduced.

## 3. Goals

Phase 2 is designed to provide:

1. A server-side, user-scoped learning profile across all four skills.
2. Traceable mistake records with an explicit taxonomy and confidence.
3. Deterministic weakness, strength, and trend detection with configurable
   evidence thresholds.
4. A small structured roadmap whose priorities are data-derived.
5. Useful, cooldown-controlled proactive suggestions.
6. Persistent bounded Tutor conversations for authenticated users.
7. A deterministic scope guardrail for English/IELTS relevance.
8. Provider-neutral Tutor tools and frontend contracts.
9. Graceful operation with insufficient history or unavailable AI.
10. Clean interfaces for a future approved Adaptive Practice Generator.

## 4. Non-Goals

The following are explicitly outside this Phase 2 design:

- implementing an AI Practice Generator or publishing generated tests;
- automatic learner-visible AI-generated tests without Admin approval;
- autonomous browsing, general-purpose assistant behavior, or arbitrary
  database access by an LLM;
- official IELTS affiliation, official scoring, or guaranteed band outcomes;
- adaptive machine learning, reinforcement learning, recommendation neural
  networks, model training, or fine-tuning;
- a complete pronunciation engine, audio understanding, or invented STT;
- a second provider router, vector database, event-sourcing platform,
  microservice split, or Redis dependency;
- cross-user analytics, social features, or collaborative conversations;
- replacing PostgreSQL/pgvector or changing the established `vector(768)`
  embedding space.

## 5. Design Principles

### 5.1 Trusted data first

Application data is the source of truth. Client requests may contain stable
references, but never trusted answer keys, scores, bands, ownership, or
history. Every private reference is resolved with the authenticated user ID
bound in the repository query.

### 5.2 Deterministic first

The backend calculates accuracy, counts, rates, score conversion, category
frequency, thresholds, trends, completion, and roadmap priority. These facts
are returned as structured fields and can optionally be explained by AI.

### 5.3 LLM interprets, never adjudicates

LLM use is appropriate for natural-language explanations, concise summaries,
Writing feedback, and optional classification of evidence that deterministic
rules cannot classify. An LLM response cannot overwrite a trusted score,
answer key, ownership decision, weakness state, or roadmap priority.

### 5.4 User-scoped by construction

Every profile, event, mistake, issue, roadmap, conversation, message, draft,
and transcript belongs to exactly one user. Repository methods require a
user ID; service methods do not accept a user ID from the browser as an
authority.

### 5.5 Explainable outputs

Every weakness, strength, trend, and roadmap item exposes evidence counts,
time window, category, and source references. Natural-language reasons are
secondary presentation and never the only record of a decision.

### 5.6 Graceful degradation

Core practice, scoring, history, progress, and deterministic Tutor tools work
without any external provider. If AI explanation or summarization fails, the
structured result remains usable and the UI shows a bounded unavailable state.

## 6. Architecture Overview

```text
Practice / Writing / Speaking application service
        |
        +-- transactionally records trusted result
        +-- LearningEventService (deduplicated educational events)
        +-- MistakeAnalysisService (deterministic first)
        +-- LearningIntelligenceService
                +-- WeaknessDetectionService
                +-- StrengthDetectionService
                +-- TrendAnalysisService
                +-- RoadmapPlanner
        |
        +-- materialized profile/issue/roadmap snapshot

TutorController
  -> AiChatService compatibility facade
  -> TutorOrchestrator
       -> ScopePolicy
       -> TutorContextService
       -> deterministic Tutor tools
       -> ConversationMemory
       -> governed RAG when required
       -> existing AiProviderRouter only for natural language
       -> normalized provider-neutral TutorResponse
```

The modular monolith remains one deployable Spring Boot application and one
PostgreSQL database. A lightweight application event is preferable to a
message broker in this phase: the completed attempt and its educational
event commit atomically, and the intelligence snapshot can be recomputed
from durable evidence. A later job executor may process large backfills, but
Phase 2 does not require distributed infrastructure.

### 6.1 Source evidence and derived state

Persisted source evidence:

- validated attempt/result records already owned by the user;
- append-only educational events;
- traceable mistake records;
- normalized Writing assessment criteria/issues;
- text-only Speaking context where supplied by the learner;
- conversation messages and summaries;
- roadmap item lifecycle state.

Persisted derived snapshots:

- one profile summary per user;
- one skill profile per user and skill;
- current learning issues with evidence counters and state;
- current roadmap and item state.

All snapshots contain an `as_of` timestamp and can be rebuilt from source
evidence. No giant profile JSON blob is used. Small bounded JSON may hold
provider-neutral display metadata or a redacted event payload, but not the
entire learner history or raw provider response.

## 7. Student Learning Model

### 7.1 Profile concepts

`StudentLearningProfile` is the user-level summary:

```text
userId
totalPracticeAttempts
totalAnsweredQuestions
activeRoadmapItemCount
lastActivityAt
overallTrend
profileConfidence
asOf
updatedAt
```

`StudentSkillProfile` is one row per user and skill:

```text
userId
skill: READING | LISTENING | WRITING | SPEAKING
attemptCount
evaluatedItemCount
accuracyRate
latestBandEstimate          // nullable; always labeled as estimate
recentTrend
strengthCount
weaknessCount
lastAttemptAt
asOf
```

`StudentLearningIssue` represents a current or recently resolved pattern. It
uses `kind = WEAKNESS | STRENGTH`, a taxonomy category, lifecycle state,
confidence, evidence counters, and links to source records. This avoids
separate duplicate strength and weakness tables.

### 7.2 Derived-on-demand values

Exact percentages, trend deltas, recent windows, and roadmap priority scores
are derived by deterministic services from source records and configuration.
Snapshots are refreshed after meaningful learning events and can be refreshed
on a profile read when stale. The API never presents a cached value without
its `asOf` timestamp and evidence state.

### 7.3 Evidence states

Profile and issue APIs use explicit states:

```text
INSUFFICIENT_DATA
OBSERVATION
EMERGING
CONFIRMED
IMPROVING
STABLE
DECLINING
```

`INSUFFICIENT_DATA` is a valid outcome, not an error. A skill without enough
evidence is shown as “Chưa đủ dữ liệu”, never as a guessed band or weakness.

## 8. Learning Events

### 8.1 Purpose and event vocabulary

`LearningEvent` is a small append-only record of an educationally meaningful
state transition. The initial event vocabulary is:

```text
PRACTICE_STARTED
QUESTION_VIEWED
ANSWER_SELECTED
ANSWER_CHANGED
ANSWER_SUBMITTED
QUESTION_CORRECT
QUESTION_INCORRECT
PRACTICE_COMPLETED
HINT_REQUESTED
EXPLANATION_REQUESTED
WRITING_SUBMITTED
WRITING_FEEDBACK_RECEIVED
SPEAKING_SUBMITTED
ROADMAP_ITEM_STARTED
ROADMAP_ITEM_COMPLETED
```

Question-view events are emitted at a meaningful boundary, such as a
debounced view after a short dwell or an explicit current-question change.
Mouse movement, keystrokes, cursor position, and repeated render events are
not learning events.

### 8.2 Event schema

The proposed normalized shape is:

```text
LearningEvent
  id: UUID
  userId: UUID
  eventType: enum
  skill: enum
  sessionId: UUID nullable
  practiceSetId: string nullable
  attemptId: UUID nullable
  questionId: string nullable
  roadmapItemId: UUID nullable
  sourceReference: string nullable
  payload: bounded JSONB
  clientEventId: string nullable
  occurredAt: timestamptz
  recordedAt: timestamptz
```

`occurredAt` is accepted only within a bounded clock-skew window; the server
owns `recordedAt`. Payload is whitelisted per event type and excludes answer
keys, raw essays, audio, tokens, and provider payloads unless the event is a
server-side reference to a trusted record.

### 8.3 Idempotency and retention

The application creates server idempotency keys from the user, event type,
source reference, attempt revision, and question/session boundary. An optional
client event ID is stored but is never an ownership authority. A unique index
on `(user_id, client_event_id)` applies when a client ID is present; a second
unique constraint covers server-generated source-event keys.

Events are retained for 24 months after the user's last activity by default,
configurable through application policy. User deletion removes events and all
derived data. A retention job is a future operational component, not a
browser responsibility.

## 9. Mistake Taxonomy

### 9.1 Shared classification contract

```text
MistakeClassification
  category
  confidence: 0..1
  method: DETERMINISTIC | HEURISTIC | LLM_ASSISTED | UNKNOWN
  evidenceCode
  evidenceText: short, redacted
  questionType
  classifiedAt
```

`UNKNOWN`/`UNCLASSIFIED` is preferred over a forced guess. A classification
must reference the question, attempt, or assessment evidence that supports it.
Confidence is evidence confidence, not learner ability.

### 9.2 Reading categories

```text
FALSE_NOT_GIVEN_CONFUSION
TRUE_NOT_GIVEN_CONFUSION
PARAPHRASE_NOT_RECOGNIZED
KEYWORD_MATCHING_OVERRELIANCE
INFERENCE_ERROR
DETAIL_MISREAD
VOCABULARY_IN_CONTEXT
MATCHING_HEADING_MAIN_IDEA
SENTENCE_COMPLETION_GRAMMAR
WORD_LIMIT_VIOLATION
UNKNOWN_READING_ERROR
```

Deterministic classification uses trusted question type, answer key,
question metadata, explanation tags, selected option, and answer shape. For
example, a known `TRUE_FALSE_NOT_GIVEN` item with a wrong option can be
classified only when the question catalog supplies a validated error mapping;
otherwise it becomes `UNKNOWN_READING_ERROR`.

### 9.3 Listening categories

```text
DISTRACTOR_TRAP
SPELLING
PLURAL_SINGULAR
WORD_LIMIT
NUMBER_DATE
MISSED_PARAPHRASE
LOST_POSITION
DETAIL_MISHEARD
UNKNOWN_LISTENING_ERROR
```

Spelling, pluralization, numbers, dates, and word-limit violations are
deterministic when the answer format and key make them observable. Distractor
and position categories need question metadata or answer-order evidence.
Without that evidence, classification remains unknown.

### 9.4 Writing categories

Writing issues map to the four IELTS evaluation dimensions without claiming
official scoring:

```text
TASK_ACHIEVEMENT_RESPONSE
COHERENCE_COHESION
LEXICAL_RESOURCE
GRAMMATICAL_RANGE_ACCURACY
```

The persisted assessment criteria and issue labels provide the first signal.
An LLM may normalize an issue into one of these categories only when the
prompt includes the learner's own text and the bounded assessment evidence;
the result is marked `LLM_ASSISTED` and remains an estimate. The weakness
engine still requires recurrence across multiple submissions.

### 9.5 Speaking categories and boundary

With the current project boundary, Phase 2 can classify text-level issues:

```text
VOCABULARY_RANGE
GRAMMAR_ACCURACY
ANSWER_DEVELOPMENT
COHERENCE_FLUENCY_TEXT_PROXY
UNKNOWN_SPEAKING_ISSUE
```

These are learning-text observations, not pronunciation or audio findings.
No pronunciation, accent, fluency-from-audio, or phoneme claim is created
without a real STT/audio analysis capability. A future STT subphase may add a
separate evidence source and taxonomy extension.

### 9.6 Mistake record

Each `MistakeRecord` includes:

```text
id
userId
skill
practiceSetId nullable
attemptId nullable
questionId nullable
questionType nullable
learnerAnswerSnapshot nullable
correctAnswerRef nullable
category
classificationMethod
confidence
evidenceCode
evidenceText nullable
detectedAt
resolvedAt nullable
status: OPEN | IMPROVING | RESOLVED | UNKNOWN
```

The learner answer may be snapshotted to preserve what was submitted. The
correct answer is not duplicated as a free-form authority; `correctAnswerRef`
identifies a versioned question/catalog record that the server can resolve.
For Writing and Speaking it identifies the assessment or text evidence, not
an invented answer key.

## 10. Mistake Analysis Pipeline

The pipeline runs after a trusted educational boundary:

```text
validated attempt/submission
  -> normalized result evidence
  -> deterministic classifier
  -> bounded heuristic classifier when evidence is sufficient
  -> optional LLM interpretation only for evidence not deterministically classified
  -> MistakeRecord with method/confidence/evidence
  -> weakness/strength/trend refresh
  -> roadmap reconciliation
```

The LLM stage is optional and never blocks scoring or attempt persistence. It
receives a redacted evidence packet, not arbitrary database access. Its output
must match an allowlisted category and include a short evidence rationale.
Invalid, low-confidence, or unsupported output becomes `UNKNOWN`.

Duplicate mistake records are prevented by a unique source key such as
`(user_id, attempt_id, question_id, category, classification_revision)`.
Reclassification creates a new revision or updates the current record with
an audit timestamp; it does not silently rewrite historical evidence.

## 11. Weakness and Strength Detection

### 11.1 Weakness states

`WeaknessDetectionService` uses configurable windows and thresholds:

```text
learning.intelligence.recent-window-days = 30
learning.intelligence.minimum-attempts = 3
learning.intelligence.minimum-evaluated-items = 10
learning.intelligence.emerging-min-occurrences = 2
learning.intelligence.confirmed-min-occurrences = 3
learning.intelligence.confirmed-min-attempts = 2
learning.intelligence.confirmed-rate = 0.20
learning.intelligence.improvement-windows = 2
```

The values are design defaults, not hidden constants. They are validated at
startup and can be tuned through configuration or an explicit future admin
policy.

Recommended state strategy:

| Evidence | State |
|---|---|
| fewer than minimum items/attempts | `INSUFFICIENT_DATA` |
| one isolated occurrence | `OBSERVATION` |
| repeated category across the minimum recent evidence | `EMERGING` |
| recurrence across at least two attempts and configured error rate | `CONFIRMED` |
| confirmed issue with two improving windows and no recent recurrence | `IMPROVING` |
| no longer supported by the configured history window | resolved/archived |

One mistake never creates a confirmed weakness. The detector calculates
category rate among comparable evaluated items, recurrence across attempts,
recency weight, and the change against the preceding window. It separates
skill-wide performance from question-type/category performance so a small
sample in one task type does not label the whole skill.

### 11.2 Strength detection

`StrengthDetectionService` uses the same evidence guardrails and identifies
consistent accuracy, successful repeated performance, improvement from a
previous weak area, and completed target objectives. A strength requires the
configured minimum evidence and is never inferred from a single perfect
attempt. Strengths and weaknesses can coexist at different granularities,
for example strong overall Reading accuracy with a confirmed
`FALSE_NOT_GIVEN_CONFUSION` pattern.

### 11.3 Evidence and explainability

Each issue exposes:

```text
category
skill
state
confidence
occurrenceCount
attemptCount
evaluatedItemCount
recentRate
previousRate
firstDetectedAt
lastDetectedAt
evidenceReferences
```

The frontend may present a friendly sentence generated from these fields or
an optional AI explanation, but it cannot display a stronger claim than the
state and evidence support.

## 12. Performance Trends

`TrendAnalysisService` is deterministic. It compares comparable observations
using recent and prior windows, with recency weighting and minimum sample
guards. For score-bearing activities it uses normalized accuracy or the
existing band estimate as an estimate; it does not combine incomparable raw
Writing and Speaking data into an official overall band.

```text
IMPROVING
STABLE
DECLINING
INSUFFICIENT_DATA
```

Defaults require at least six comparable attempts or twenty evaluated items,
whichever configured policy applies to the skill. A delta inside the stable
band is `STABLE`; a meaningful positive/negative delta over the configured
window is `IMPROVING`/`DECLINING`. A tiny sample, mixed activity types, or
missing timestamps produces `INSUFFICIENT_DATA`.

Trend responses include the compared windows, sample sizes, and delta. An
LLM is never asked to guess the trend.

## 13. Personalized Learning Roadmap

### 13.1 Structured roadmap

`LearningRoadmap` is a server-owned structure, not a generated text blob:

```text
LearningRoadmap
  id
  userId
  status: ACTIVE | PAUSED | COMPLETED | ARCHIVED
  version
  generatedFromAsOf
  createdAt
  updatedAt

LearningRoadmapItem
  id
  roadmapId
  userId
  skill
  learningObjective
  targetErrorType nullable
  activityType
  targetQuestionType nullable
  priority
  estimatedWorkload
  status: NOT_STARTED | IN_PROGRESS | COMPLETED | SKIPPED
  reasonCode
  evidenceIssueId nullable
  evidenceSnapshot
  createdAt
  startedAt nullable
  completedAt nullable
```

An item has a stable objective identity such as
`READING/FALSE_NOT_GIVEN_CONFUSION/CONCEPT_REVIEW`, so a refresh reconciles
the existing item instead of producing a new item after every answer.

### 13.2 Deterministic planner

`RoadmapPlanner` ranks candidates using:

```text
severity        = issue state and error rate
recurrence      = repeated attempts/category occurrences
recency         = time-decayed recent evidence
skillGap        = comparable skill performance gap
unfinished      = bonus for active unfinished item
trend           = bonus when declining, penalty when improving
history         = learner's recent completed objectives
```

The weights are configuration-backed and the final priority is explainable.
The planner emits at most three to five active priorities. It preserves an
active item unless the supporting issue is resolved or changes materially;
this hysteresis prevents roadmap oscillation. A completed item is not
immediately recreated unless new evidence crosses the configured recurrence
threshold.

### 13.3 Activity sequence

The planner can express a small sequence without generating a test:

```text
concept review -> targeted practice -> mixed practice -> reassessment
```

Each step references an existing catalog/activity type. Phase 2 does not
generate new questions. The reason stores structured evidence such as issue
ID, occurrence count, rate, and date window; optional AI prose is a view over
that reason.

### 13.4 Update triggers

Roadmap reconciliation runs after:

- a completed Reading or Listening attempt;
- a Writing assessment becoming available;
- a Speaking text attempt being saved;
- a roadmap item completion;
- a bounded profile refresh request after the snapshot is stale.

It does not run after every keystroke, every chat message, or a failed
provider call. A refresh is idempotent and versioned by source evidence
`asOf`.

## 14. Proactive Tutor

Proactive suggestions are quiet, educational, and dismissible. They appear in
the dashboard or Tutor panel as a card/inline suggestion, not an intrusive
popup.

Example trigger:

```text
confirmed issue: FALSE_NOT_GIVEN_CONFUSION
recent recurrence: 3 mistakes across 2 attempts
no identical suggestion in configured cooldown
session suggestion count below maximum
```

Suggested policy:

```text
one suggestion per session by default
seven-day cooldown per issue/objective after dismissal
no identical suggestion while the issue has no new evidence
respect learner notification preference
```

The suggestion references the issue and offers a bounded action such as
“Xem giải thích nhanh” or “Luyện 10 câu mục tiêu”. It is not a claim that the
learner is permanently weak and does not count as a learning event until the
learner explicitly starts or dismisses it.

## 15. Conversation Memory

### 15.1 Storage model

Authenticated conversations use three normalized tables:

```text
ai_conversations
  id
  user_id
  skill nullable
  practice_set_id nullable
  attempt_id nullable
  question_id nullable
  title nullable
  status: ACTIVE | ARCHIVED
  created_at
  updated_at
  last_message_at

ai_messages
  id
  conversation_id
  sequence_no
  role: USER | ASSISTANT
  content
  response_status
  grounding_status
  citations_json bounded and normalized
  context_snapshot_json redacted and bounded
  created_at

ai_conversation_summaries
  id
  conversation_id
  revision
  summary_text
  covered_through_sequence
  created_at
```

Foreign keys and user-scoped repository methods enforce ownership. Unique
`(conversation_id, sequence_no)` preserves order. Indexes cover
`(user_id, updated_at DESC)` and `(conversation_id, sequence_no DESC)`.

Raw provider requests, authorization headers, hidden reasoning, embeddings,
full retrieved documents, and unnecessary personal data are not persisted.

### 15.2 Bounded memory policy

Defaults are:

```text
recent visible messages: 8
summary trigger: 12 unsummarized messages
summary maximum: 1,000 tokens
conversation retention cap: 200 visible messages
```

Each provider request receives the current trusted context, the active
summary, and the newest bounded messages. The full chat history is never
resent. Summary updates are replacement revisions and are validated for size,
ownership, and unsupported claims.

Long-term learning facts come from the Student Learning Model, not from a
conversation summary. If summarization fails, recent messages remain usable
and the next request can retry within a bounded policy. If the provider is
unavailable, conversation persistence still records a safe failure status
without fabricating an assistant answer.

### 15.3 Conversation lifecycle

The authenticated user may start a new conversation, archive it, or reset its
summary. Reset removes the active context window and summary for that
conversation but does not delete learning evidence. A stale exercise context
is re-resolved on each request; if the attempt/question no longer exists, the
Tutor explains that the exercise context is unavailable instead of using a
stale answer key.

Guest conversations remain ephemeral and cannot read or persist personal
progress, mistakes, drafts, or transcripts.

## 16. Tutor Scope Guardrail

`TutorScopePolicy` is a deterministic service invoked before expensive RAG or
provider calls. It returns:

```text
IN_SCOPE
AMBIGUOUS_ENGLISH_RELEVANT
OUT_OF_SCOPE
```

Allowed topics include IELTS, English grammar, vocabulary, academic English,
English conversation, Reading, Listening, Writing, Speaking, learning
strategy, learner progress, roadmap, and current exercises. A general topic
is in scope when the user explicitly asks to discuss it in English practice;
for example, “Talk with me about football in English” is allowed.

Rule-based signals detect unrelated programming, finance, unrelated
mathematics, and other general-assistant requests. Ambiguous messages are
allowed when they can reasonably support English learning. The policy does
not invoke another LLM merely to classify every message.

For out-of-scope requests, the Tutor returns a polite provider-free redirect:

> Mình tập trung vào tiếng Anh và IELTS. Nếu bạn muốn, mình có thể giúp bạn
> luyện từ vựng hoặc Speaking về chủ đề này.

User text is still treated as untrusted input. Scope classification does not
grant database or tool permissions.

## 17. Tutor Tools

`TutorOrchestrator` exposes bounded application tools, not arbitrary table
access:

### Read-only tools

```text
getCurrentExercise()
getCurrentQuestion()
getRecentMistakes(filter)
getWeaknessProfile()
getStrengthProfile()
getSkillProgress()
getPerformanceTrend(skill)
getLearningRoadmap()
getRecentAttempts(limit)
```

Each tool receives the authenticated principal and a server-resolved context.
It returns a bounded DTO with evidence metadata. It cannot read another user,
raw credentials, hidden answer keys outside the current authorized context,
or arbitrary SQL.

### Mutating tool

```text
markRoadmapItemComplete(itemId)
```

This operation is not performed solely because an LLM emitted a string. The
application validates ownership, item status, allowed completion state, and
the authenticated request before writing the item and a
`ROADMAP_ITEM_COMPLETED` event. The UI may call the same endpoint directly.

The current deterministic tools remain the first route for “what question,
what did I select, what score, what progress” requests. Phase 2 extends the
same pattern for profile and roadmap facts.

## 18. Data Model

The following are proposed additive tables. Exact SQL types and migration
details belong to a later approved implementation plan; no migration is
created by this document.

### 18.1 `learning_events`

Purpose: durable educational event evidence and idempotency source.

Important columns: `id`, `user_id`, `event_type`, `skill`, `session_id`,
`practice_set_id`, `attempt_id`, `question_id`, `roadmap_item_id`,
`source_reference`, bounded `payload_json`, `client_event_id`,
`server_event_key`, `occurred_at`, `recorded_at`.

Relationships: user cascade; nullable foreign keys to attempts and roadmap
items where the referenced domain is persistent.

Indexes: `(user_id, occurred_at DESC)`, `(user_id, event_type, occurred_at)`,
`(user_id, server_event_key)` unique, `(user_id, client_event_id)` unique when
present.

Retention: 24 months after last activity by default; deletion cascades.

### 18.2 `mistake_records`

Purpose: traceable question/submission-level learning issues.

Important columns: `id`, `user_id`, skill, question/set/attempt references,
question type, learner answer snapshot, `correct_answer_ref`, category,
classification method, confidence, evidence code/text, status, timestamps,
and a classification revision.

Indexes: `(user_id, skill, detected_at DESC)`,
`(user_id, category, detected_at DESC)`, source uniqueness by user/attempt/
question/category/revision.

Retention: follows source evidence; resolved records are retained long enough
to calculate trends and are deleted with the user.

### 18.3 `student_learning_profiles`

Purpose: one user-level materialized summary for fast dashboard/Tutor reads.

Important columns: `user_id` primary key, activity counters, active issue/item
counts, overall trend, confidence, `as_of`, `updated_at`.

Indexes: primary key only plus optional `updated_at` for stale refresh scans.
All values are rebuildable.

### 18.4 `student_skill_profiles`

Purpose: normalized per-user/per-skill summary.

Important columns: `user_id`, `skill`, attempts/items, accuracy, nullable
estimated band, trend, issue counts, last activity, `as_of`, `updated_at`.

Constraint: unique `(user_id, skill)` and an allowlisted four-skill check.
Index: `(user_id, updated_at DESC)`.

### 18.5 `student_learning_issues`

Purpose: current weakness and strength lifecycle state.

Important columns: `id`, `user_id`, `skill`, `kind`, category, state,
confidence, occurrence/attempt/item counters, recent and previous rates,
first/last detected timestamps, evidence issue references, `as_of`,
`resolved_at`.

Constraint: unique active issue identity per user, skill, kind, and category.
Indexes: `(user_id, state, priority)`, `(user_id, skill, last_detected_at)`,
`(user_id, category)`.

### 18.6 `learning_roadmaps` and `learning_roadmap_items`

Purpose: stable, small, user-owned plan and item lifecycle.

Roadmap columns: `id`, `user_id`, status, version, evidence `as_of`,
timestamps. Item columns are defined in Section 13.

Constraints: unique active roadmap per user; unique stable objective identity
within a roadmap; item ownership matches roadmap ownership.

Indexes: `(user_id, status, updated_at DESC)`, `(roadmap_id, status, priority)`.

Retention: archived roadmaps retained for the configured learner-history
window, then removed with supporting private data.

### 18.7 Conversation tables

The `ai_conversations`, `ai_messages`, and `ai_conversation_summaries` tables
are defined in Section 15. They are user-private, sequence-constrained, and
store only normalized visible content and bounded provenance.

## 19. API Design

All APIs are proposed provider-neutral JSON contracts. All member endpoints
derive the user from the authenticated session; a URL user ID is not accepted.

### 19.1 Learning intelligence

```text
GET /api/learning/profile
GET /api/learning/skills
GET /api/learning/mistakes?skill=&status=&limit=
GET /api/learning/issues
GET /api/learning/roadmap
POST /api/learning/roadmap/items/{itemId}/complete
GET /api/learning/activity
```

Responses include `state`, `asOf`, evidence counters, and explicit empty
states. `POST complete` returns the updated roadmap item and does not call an
LLM. Existing `/api/me/progress`, `/api/me/activity`, and `/api/me/attempts`
remain compatible during migration.

There is no unrestricted client event-ingestion endpoint. Domain endpoints
emit whitelisted events. If a future UI-only event is required, its endpoint
accepts only an allowlisted event type and server-validated references.

### 19.2 Conversations and chat

```text
GET  /api/ai/conversations
GET  /api/ai/conversations/{conversationId}
POST /api/ai/conversations/{conversationId}/archive
POST /api/ai/chat
```

`POST /api/ai/chat` keeps the existing `message`, `context`, and bounded
`history` fields. `conversationId` is an optional additive field for
authenticated persistence; existing clients can continue without it. The
server may return optional normalized conversation metadata without changing
the existing `status`, `answer`, `sources`, `grounding`, `meta`, and
`timestamp` fields.

The server ignores client answer keys, scores, bands, and arbitrary user IDs.
Private context is resolved from the authenticated principal and supplied
references.

### 19.3 Authorization contract

- unauthenticated users may use generic ephemeral Tutor chat and public
  catalog context;
- private progress, mistakes, roadmap, attempts, drafts, transcripts, and
  persisted conversations require authentication;
- every private ID is checked against the current principal before read or
  mutation;
- missing or cross-user references return a safe not-found/unauthorized
  result without leaking whether another user's record exists;
- Admin role does not automatically grant learner-private records through
  learner APIs; any aggregate admin view requires a separately authorized
  endpoint and explicit privacy policy.

## 20. Frontend UX

### 20.1 Dashboard additions

The member dashboard may add compact Academic Luxury widgets, reusing current
cards, skeletons, section titles, and chart patterns:

```text
TODAY'S FOCUS
Reading · FALSE / NOT GIVEN
Evidence-backed reason · Continue practice

YOUR PROGRESS
Reading ↑ · Listening → · Writing ↑ · Speaking Chưa đủ dữ liệu

COMMON MISTAKES
Category list with frequency, skill, and “Xem giải thích”

AI ROADMAP
Three to five structured items with status and evidence reason
```

No homepage overload is required. Guest users see an assessment CTA and
neutral preview, never personal bands, exam dates, mistakes, or roadmap data.

### 20.2 Tutor behavior

Tutor context follows the current route and bounded references. A learner can
ask for a current question explanation, a hint, progress, recent mistakes,
or the next roadmap item. Deterministic application facts render immediately
without an AI loading state. Natural-language explanation uses existing
loading skeletons, normalized errors, source chips where RAG is used, and
insufficient-context states.

### 20.3 Empty and insufficient states

The UI distinguishes:

- no attempts yet;
- one observation, not enough for a weakness;
- insufficient Speaking evidence because STT is not configured;
- no current exercise context;
- AI explanation temporarily unavailable;
- roadmap not yet generated.

These states use `SkeletonBlock` while loading, accessible status text, and
keyboard-reachable actions. They never use fake scores or pretend AI facts.

### 20.4 Accessibility and motion

New widgets use semantic headings, visible focus states, descriptive labels,
keyboard actions, reduced-motion-safe transitions, and responsive layouts at
375, 768, 1024, and 1440 widths. Charts provide a text/list equivalent so
trend and issue information is not hover-only.

## 21. AI Provider Usage

Phase 2 reuses the existing `AiProviderRouter` and its Groq → Cloudflare →
Gemini order. No provider-specific types cross the application boundary.

Allowed provider tasks:

- explain a trusted mistake and offer a hint;
- summarize already-calculated recurring issues;
- phrase a structured roadmap reason naturally;
- provide Writing feedback over the learner's submitted text;
- hold English-learning conversation;
- classify text evidence that deterministic rules cannot classify, with explicit uncertainty.

Forbidden provider tasks:

- calculate scores, percentages, completion, or trends;
- select the current question or answer key;
- determine ownership or authorization;
- decide whether a weakness is confirmed;
- invent roadmap priorities or historical attempts;
- query arbitrary tables or use provider-specific tool formats.

Provider prompts contain compact, redacted evidence and a system instruction
that user text is untrusted. They exclude API keys, unrelated personal data,
full history, unnecessary retrieved documents, and hidden reasoning. Existing
timeouts, fallback, 429 handling, cooldowns, safe error mapping, and no-live-
quota automated tests remain authoritative.

## 22. Security and Privacy

### 22.1 Ownership and IDOR prevention

Every repository query includes `user_id`. Services reject owner mismatch
before loading records. Conversation IDs, roadmap item IDs, mistake IDs,
attempt IDs, draft IDs, and transcript IDs are all opaque and still require
ownership checks. Tests must cover guessed IDs, copied IDs, and mismatched
skill/set references.

### 22.2 Data minimization

Only educationally useful fields are retained. Raw provider responses,
headers, keys, hidden reasoning, full RAG documents, and unnecessary profile
attributes are not stored. Essay text and learner-entered transcript are
private evidence and are sent to a provider only for an explicit task that
needs them, bounded to configured size.

### 22.3 Deletion and retention

User deletion cascades through sessions, attempts, events, mistakes,
profiles, issues, roadmaps, and conversations. Default retention is 24 months
for learning evidence and 12 months for conversations after the relevant
activity becomes inactive; both are configuration-backed and documented. A
future privacy endpoint may expose deletion/export status, but Phase 2 does
not expose cross-user analytics.

### 22.4 Prompt injection

Learner text, retrieved text, and summaries are data, not instructions. The
orchestrator chooses tools before provider generation. Tool outputs are
allowlisted structured facts. RAG retains existing approval, active-version,
embedding-space, grounding, citation, and insufficient-evidence governance.

## 23. Failure Handling

| Condition | Required behavior |
|---|---|
| insufficient history | show `INSUFFICIENT_DATA`; no weakness or band claim |
| missing exercise context | deterministic context-missing response; no guessed answer |
| incomplete attempt | do not create completion-based issue or roadmap evidence |
| deleted practice | mark context stale; preserve historical event reference without exposing deleted content |
| conflicting historical data | preserve source records, mark snapshot stale, recalculate deterministically |
| LLM unavailable | keep scoring/profile/roadmap facts; show safe explanation-unavailable state |
| explanation fails | return structured mistake and retry action; no infinite loading |
| roadmap succeeds, prose fails | render structured roadmap reason without prose |
| stale conversation context | re-resolve IDs; ask learner to reopen current exercise |
| duplicate event | idempotency constraint makes the second event a no-op |
| provider 429/timeout | existing bounded fallback and normalized error policy |
| unsupported scope | provider-free polite redirect |
| missing STT | retain text-only Speaking boundary; no transcript/pronunciation claim |

Core learning writes are transactional and do not depend on external AI. A
provider outage cannot undo a valid attempt or prevent deterministic progress
from being recorded.

## 24. Testing Strategy

All automated external-provider calls are mocked. No test consumes live quota,
requires provider credentials, or prints secrets.

### 24.1 Unit tests

- deterministic Reading and Listening mistake classification;
- Writing taxonomy normalization and Speaking text-only boundary;
- unknown/low-confidence classification behavior;
- weakness threshold/state transitions;
- strength detection and evidence minimums;
- recency weighting and trend windows;
- roadmap priority, hysteresis, maximum active items, and stable identity;
- proactive cooldown and dismissal rules;
- scope policy, ambiguous English topics, and polite redirect;
- conversation message bounds and summary replacement;
- provider-neutral Tutor tool contracts;
- deterministic tools never invoking AI/RAG.

### 24.2 Integration tests

- completed attempt creates one idempotent learning event;
- attempt review creates traceable mistake records;
- Writing/Speaking domain submissions update only the owning profile;
- events rebuild skill profiles and issues;
- roadmap persists and reconciles without oscillation;
- conversation/message/summary ownership and ordering;
- stale/deleted context is safe;
- existing V1–V6 schema remains compatible;
- RAG citations and embedding-space filters remain unchanged.

### 24.3 Security tests

- IDOR for every private endpoint and tool;
- cross-user attempt, mistake, roadmap, draft, transcript, and conversation
  access;
- browser cannot supply an answer key, score, band, or owner;
- prompt injection cannot invoke arbitrary tools or reveal hidden data;
- provider prompts contain no secrets and no unnecessary private fields;
- Admin endpoints do not silently bypass learner ownership.

### 24.4 Frontend tests

- profile, skill, mistake, roadmap, and proactive suggestion rendering;
- member and guest separation;
- insufficient-data and STT-not-configured states;
- deterministic Tutor responses avoid loading states when appropriate;
- roadmap completion success/error states;
- source chips and normalized AI fallback errors;
- keyboard/focus/reduced-motion behavior;
- responsive layout and no horizontal overflow.

## 25. Research and Evaluation Hooks

The architecture supports research measurements without claiming results.
Potential structured measures include:

- accuracy and improvement by skill and question type;
- reduction in repeated category errors;
- time between issue detection and improvement;
- roadmap item completion and reassessment outcomes;
- learner-perceived usefulness of explanations and suggestions;
- SUS and Likert feedback for Tutor/roadmap usability;
- behavior after feedback, such as starting targeted practice.

The distinction is explicit:

```text
system capability: the platform records, detects, prioritizes, and explains
research hypothesis: adaptive evidence-backed guidance may help learners
measured outcome: collected only through a defined evaluation protocol
```

No learning improvement, score gain, or treatment effect is claimed by this
specification.

## 26. AI Phase 3 Extension Points

Phase 2 exposes future interfaces without implementing a generator:

```text
getWeaknessProfile(userId)
getLearningPriorities(userId)
getTargetQuestionTypes(userId)
getRecentMistakePatterns(userId)
getSkillLevelEvidence(userId)
```

The later Adaptive Practice Generator must support reference upload/import,
structure analysis, new similar practice generation without copying,
structural/answer/evidence validation, ambiguity detection,
similarity/plagiarism safeguards, difficulty validation, optional AI critic,
revision loops, mandatory Admin review, Practice Bank persistence, deterministic
scoring, and full attempt/history/progress integration.

Its lifecycle is:

```text
DRAFT
 -> GENERATING
 -> AUTO_VALIDATING
 -> PENDING_REVIEW
 -> ADMIN REVIEW
      -> APPROVED
      -> NEEDS_REVISION
      -> REJECTED
```

There is never a direct `AI_GENERATED -> PUBLIC` transition. Only an
`APPROVED` Practice Bank record may become learner-visible. This is
architectural preparation only and is not Phase 2 implementation.

## 27. Migration and Compatibility Strategy

No migration is created by this document. A future implementation should use
additive Flyway migrations after the spec and implementation plan are
approved.

The compatibility rules are:

1. Keep existing V1–V6 tables, `vector(768)`, auth sessions, practice routes,
   Writing/Speaking routes, and `/api/ai/chat` fields compatible.
2. Add new intelligence tables rather than rewriting `learning_attempts` or
   `learning_activity` in place.
3. Backfill historical attempts as `HISTORICAL_IMPORT` events and create only
   evidence-supported mistakes; unknown classifications remain unknown.
4. Do not backfill a fake mistake, band, exam date, transcript, or official
   score.
5. Backfill profiles and roadmaps behind a feature flag; existing dashboard
   behavior remains available if the snapshot is absent.
6. Build indexes safely and verify ownership constraints before enabling new
   endpoints.
7. Preserve the existing RAG migration and embedding-space reindex safety.
8. Use additive DTO fields and frontend fallbacks so old clients continue to
   render normalized responses.

## 28. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| synthetic practice metadata limits classification | use explicit unknown state and versioned catalog metadata |
| Writing assessment quality varies by provider | store estimate/disclaimer, require recurrence, retain assessment evidence |
| Speaking has no STT | text-only categories; no pronunciation claim |
| roadmap churn confuses learners | stable objective identity, hysteresis, 3–5 active items |
| AI summary invents history | deterministic evidence packet, structured validation, database facts as authority |
| profile snapshots drift | `asOf`, rebuildable source records, stale refresh policy |
| event volume grows | whitelist events, retention, no mouse/keystroke logging |
| privacy exposure through prompts | user scoping, redaction, bounded context, no raw provider persistence |
| provider outage blocks learning | deterministic-first writes and tool routes |
| scope guardrail blocks valid English topics | explicit English-learning context makes ambiguous topics in scope |
| future generator bypasses review | lifecycle constraint requires Admin `APPROVED` before visibility |

## 29. Open Questions / Rulings

All design questions required for the next planning stage are closed. The
following rulings close the main ambiguities:

- Phase 2 uses a modular monolith and PostgreSQL; no microservice or Redis is
  required.
- Learning events are durable educational facts, not full event sourcing.
- Profiles/issues/roadmaps are rebuildable materialized snapshots over source
  evidence.
- A single mistake never confirms a weakness.
- `UNKNOWN` is a first-class classification and is safer than forced labels.
- LLM classification is optional and cannot alter deterministic decisions.
- Generic hello and deterministic application-data requests skip RAG and AI.
- Conversation memory is authenticated and bounded; guest memory is
  ephemeral.
- Speaking remains text-only until a separately approved STT capability exists.
- All Writing/Speaking scores remain `Band ước lượng` and never official.
- `/api/ai/chat` remains the compatibility endpoint with optional additive
  conversation metadata.
- Phase 3 generator work is not part of this specification's implementation.

## 30. Definition of Done for Future Implementation

Phase 2 implementation may be considered complete only when:

- user-owned learning events, mistakes, profiles, issues, and roadmaps are
  persisted with idempotency and rebuildable evidence;
- all four skills have explicit insufficient-data behavior;
- deterministic score, trend, threshold, and priority tests pass;
- contextual Tutor tools use server-side ownership and never trust browser
  answer keys or scores;
- conversation memory is bounded, user-owned, summarized, and resettable;
- scope guardrail prevents unrelated assistant behavior without blocking
  legitimate English-learning topics;
- provider routing remains Groq → Cloudflare → Gemini with no vendor payload
  in React;
- RAG governance, citations, embedding-space isolation, and 429/timeout
  fallback behavior remain green;
- core learning writes work with all providers unavailable;
- frontend member/guest, loading, empty, error, keyboard, reduced-motion,
  and responsive states are covered;
- security tests cover IDOR, prompt injection, answer-key boundaries, and
  provider data minimization;
- migrations are additive, reviewed, verified on fresh and existing data,
  and do not alter V6 embedding safety;
- no Phase 3 generator or public AI-generated test is implemented.

This definition describes a future implementation gate. It is not an
implementation plan and no product code is changed by this document.
