# AI Phase 3 — AI Practice Generator & Admin Approval Pipeline Architectural Design Specification

Date: 2026-09-27  
Status: Design only — prepared for human review before implementation planning  
Repository: `ielts-ai-tutor`  
Branch: `design/ai-phase3-practice-generator`  
Base commit: `ab4e9bf`  

This document specifies the architectural design for AI Phase 3: the AI Practice Generator and Admin Review Pipeline for IELTS AI Tutor. It defines how source materials are ingested with strict copyright governance, converted into abstract educational blueprints, transformed into substantively novel practice sets, deterministically validated across multiple quality gates, and submitted to mandatory human admin review and approval before entering the learner practice bank.

---

## 1. Purpose

The objective of AI Phase 3 is to build an administrative generation and editorial pipeline that creates high-quality, pedagogically sound, and legally safe IELTS-style practice materials across the four core language skills (Reading, Writing, Listening, Speaking).

Key goals:
1. **Editorial Quality & Validity**: Practice sets must strictly reflect IELTS task formats, cognitive demands, and difficulty distributions without claiming official IELTS status.
2. **Copyright & Rights Safety**: Ingested sources must have explicit copyright governance. The pipeline enforces structural blueprinting and substantive novelty so that copyrighted expression is never copied or redistributed.
3. **Mandatory Human Approval Gate**: No AI-generated question, passage, prompt, or rubric may enter the learner-facing practice bank or adaptive recommendation pool without explicit review and approval by an authorized administrator.
4. **Deterministic Validation**: Automated validation suites verify structural completeness, single-key validity, evidence support, ambiguity absence, and lexical/semantic divergence before human reviewers are asked to review.
5. **Clean System Integration**: Approved practice items seamlessly hydrate the existing `PracticeService`, deterministic scoring engine, and future AI Phase 2 Adaptive Learning system without creating duplicate practice runtimes.

---

## 2. Scope

### In-Scope
- Administrative endpoints and workflows for source registration, blueprint derivation, practice generation, validation, revision, and approval.
- Copyright governance model for source materials compatible with project RAG governance.
- Blueprint-first generation pipeline extracting structural schemas and generating entirely new passages, questions, options, answer keys, and explanations.
- Multi-layer validation pipeline (Structural, Answer Key, Evidence Span, Ambiguity, Substantive Novelty / Similarity, Difficulty, and AI Critic).
- Strict state machine governing generation job and practice set lifecycles (`DRAFT` -> `GENERATING` -> `AUTO_VALIDATING` -> `PENDING_REVIEW` -> `APPROVED` | `NEEDS_REVISION` | `REJECTED`).
- Versioned revision loop enabling targeted regeneration and item-level editing while preserving immutable revision history.
- Practice bank hydration mechanism mapping approved generated sets to existing repository constructs (`PracticeSet`, `PracticeQuestion`, `PracticePassage`, `PracticeParagraph`).
- Skill rollout plan: Phase 3A (Reading - primary deterministic validation), Phase 3B (Writing), Phase 3C (Listening), Phase 3D (Speaking).
- Provider-neutral AI execution using the existing `AiProviderRouter` and `AiProvider` abstractions.
- Admin UI architecture specified in semantic components ready for Academic Luxury 2.0 integration.

### Out-of-Scope
- AI Phase 2 Adaptive Learning student modeling and diagnostic recommendation engine (Phase 3 provides the approved bank Phase 2 queries).
- Text-to-speech (TTS) synthesis or audio streaming for Listening (handled in future media pipelines).
- Automated speech recognition (STT) or pronunciation scoring for Speaking (handled in future speech pipelines).
- Automated public publishing without human approval.
- Web scraping or unauthorized ingestion of commercial test banks (Cambridge IELTS 1–18, British Council proprietary tests).
- Microservice decomposition or provider subsystem rewrite.

---

## 3. Product Workflow

```text
+-----------------------------------------------------------------------------------------+
|                                1. SOURCE REGISTRATION                                   |
| Admin uploads file (PDF/DOCX/TXT), pastes text, or selects approved project document.  |
| Source assigned Rights Status: PENDING_REVIEW, APPROVED, RESTRICTED, or REJECTED.        |
+-------------------------------------------+---------------------------------------------+
                                            |
                                            v (Rights == APPROVED only)
+-----------------------------------------------------------------------------------------+
|                               2. BLUEPRINT EXTRACTION                                   |
| Source analyzed structurally: skill, task types, item counts, topic domain, lexical &   |
| syntactic demand, reasoning patterns. Stored as reusable educational Blueprint Schema.   |
+-------------------------------------------+---------------------------------------------+
                                            |
                                            v
+-----------------------------------------------------------------------------------------+
|                             3. NOVEL CONTENT GENERATION                                 |
| Provider-neutral generator uses Blueprint + domain context to create NEW passage/prompt,|
| questions, options, single deterministic answer keys, and verbatim evidence citations.   |
+-------------------------------------------+---------------------------------------------+
                                            |
                                            v
+-----------------------------------------------------------------------------------------+
|                              4. AUTOMATED VALIDATION                                    |
| - Structural Validator: schema, IDs, counts, field completeness                        |
| - Answer Validator: single key format, valid option indices                             |
| - Evidence Validator: answer verbatim supported in generated text                       |
| - Ambiguity Validator: distractor plausibility vs uniqueness of key                     |
| - Similarity Validator: n-gram & cosine distance vs source & bank (rejects duplicates)  |
| - Difficulty Validator: readability & CEFR/Band alignment                               |
| - AI Critic: pedagogical coherence check (non-blocking advisory)                        |
+-------------------------------------------+---------------------------------------------+
                                            |
                         +------------------+------------------+
                         |                                     |
                [Validation FAIL]                     [Validation PASS / WARN]
                         |                                     |
                         v                                     v
                  (NEEDS_REVISION)                      (PENDING_REVIEW)
                         |                                     |
                         v                                     v
+-------------------------------------------+  +------------------------------------------+
|         5. REVISION LOOP                  |  |         6. HUMAN ADMIN REVIEW            |
| Targeted section/item regeneration        |  | Admin inspects side-by-side:             |
| or manual prompt correction.              |  | - Generated text, items & answer keys    |
| Re-runs validation pipeline.              |  | - Evidence spans & explanation logic     |
| New immutable version created.            |  | - Validation reports & similarity flags  |
+-------------------------------------------+  | - Source lineage & model provenance      |
                                               +-------------------+----------------------+
                                                                   |
                                          +------------------------+----------------------+
                                          |                        |                      |
                                      [APPROVE]            [REQUEST REVISION]          [REJECT]
                                          |                        |                      |
                                          v                        v                      v
                                     (APPROVED)             (NEEDS_REVISION)          (REJECTED)
                                          |
                                          v
+-----------------------------------------------------------------------------------------+
|                             7. PRACTICE BANK HYDRATION                                  |
| Approved practice set is mapped to PostgreSQL practice tables.                          |
| Becomes immediately accessible to deterministic PracticeService and AI Phase 2.        |
+-----------------------------------------------------------------------------------------+
```

---

## 4. Architectural Principles

1. **Editorial Separation of Concerns**: AI proposes, validators evaluate, human decides. The system cannot publish autonomously under any condition.
2. **Structural Preservation vs. Expressive Novelty**: Blueprints capture pedagogical forms (question distributions, argumentation styles, distractor models). Content generation creates novel semantic topics, vocabulary, and paragraph sequences.
3. **Deterministic Superiority**: Deterministic code checks (JSON schema validation, regex syntax checks, exact string matching in evidence spans, option exclusivity) take strict precedence over AI critic evaluations.
4. **Zero-Leakage Student Boundary**: Unapproved items (`DRAFT`, `GENERATING`, `AUTO_VALIDATING`, `PENDING_REVIEW`, `NEEDS_REVISION`, `REJECTED`) exist in isolated administrative schemas/tables. Student APIs query only `APPROVED` records.
5. **Full Lineage & Auditability**: Every generated item retains immutable links to its generator version, model ID, prompt template version, blueprint ID, source document ID, validation run, and approving admin user ID with timestamps.
6. **Stateless Provider Interoperability**: Generation uses the established `AiProviderRouter` fallback policy (`Groq` -> `Cloudflare` -> `Gemini`) with structured JSON schema outputs and strict response parsers.

---

## 5. Source Rights & Governance

### 5.1 Rights State Model
Source materials ingested into the practice generator inherit the project's standardized RAG rights model:

| Rights Status | Meaning | Practice Generation Permitted? |
|---|---|---|
| `PENDING_REVIEW` | Source uploaded, awaiting legal/editorial verification | **NO** (Blocked) |
| `APPROVED` | Source verified as public domain, CC-BY, project-owned, or licensed | **YES** |
| `RESTRICTED` | Source has partial or internal rights; prohibited for generation | **NO** (Blocked) |
| `REJECTED` | Source flagged as copyrighted (e.g. Cambridge, BC, commercial book) | **NO** (Blocked) |

### 5.2 Source Provenance Metadata
Every registered source record requires:
- `id`: UUID primary key.
- `title`: Human-readable identifier.
- `source_type`: `USER_UPLOAD`, `PASTED_TEXT`, `RAG_DOCUMENT_REF`, `PUBLIC_DOMAIN_CORPUS`.
- `author` / `organization`: Originating entity.
- `rights_status`: Enum (`PENDING_REVIEW`, `APPROVED`, `RESTRICTED`, `REJECTED`).
- `license_note`: Detailed legal basis (e.g., "Project original", "Creative Commons CC-BY 4.0", "Public Domain news article").
- `checksum`: SHA-256 hash of the normalized source text.
- `created_by`: Admin user UUID.
- `created_at` / `updated_at`: Timestamps.

### 5.3 Hard Governance Enforcement
- Any attempt to launch a generation job with a source whose `rights_status != 'APPROVED'` triggers an immediate `400 BAD_REQUEST` with error code `SOURCE_RIGHTS_NOT_APPROVED`.
- Source texts are hashed upon creation. If a source's text matches a known blocklist checksum of commercial exams, it is automatically marked `REJECTED`.

---

## 6. Source Normalization

Raw sources (PDF, DOCX, plain text, or RAG document references) undergo strict deterministic normalization:
1. **Extraction**: Text extracted via Apache Tika / plain text decoders (reusing RAG ingestion utilities).
2. **Sanitization**: Strip HTML, malicious scripts, byte-order marks, control characters, and non-printable sequences.
3. **Structural Segmentation**: Divide source text into paragraphs, headings, tables, or dialogue turns with calculated character/token offsets.
4. **Length & Density Guardrails**:
   - Reading: Minimum 400 words, maximum 1,200 words per passage.
   - Writing: Task 1 (minimum 50 words prompt/data), Task 2 (minimum 30 words prompt).
   - Listening: Minimum 250 words, maximum 900 words per script section.
   - Speaking: Minimum 20 words per topic prompt.
5. **Artifact Normalization**: Cleaned text stored as `normalized_content` with an immutable SHA-256 hash.

---

## 7. Blueprint Model

A **Blueprint** is an abstract educational specification derived from a source or constructed directly by an admin. It contains no expressive prose from the source, only structural, pedagogical, and difficulty requirements.

### 7.1 Blueprint Schema Structure (JSONB)
```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "blueprintId": "bp-reading-019283",
  "skill": "READING",
  "targetBand": 7.0,
  "topicCategory": "NATURAL_SCIENCES",
  "passageStructure": {
    "targetWordCount": 750,
    "paragraphCount": 6,
    "rhetoricalPattern": "CHRONOLOGICAL_DISCOVERY_AND_EVALUATION",
    "lexicalDensity": "ACADEMIC_CEFR_C1"
  },
  "itemDistribution": [
    {
      "taskType": "TRUE_FALSE_NOT_GIVEN",
      "count": 4,
      "targetParagraphs": [1, 2, 3],
      "cognitiveSkill": "FACTUAL_DETAIL_VERIFICATION"
    },
    {
      "taskType": "MULTIPLE_CHOICE",
      "count": 3,
      "targetParagraphs": [4, 5, 6],
      "cognitiveSkill": "INFERENCE_AND_MAIN_IDEA"
    },
    {
      "taskType": "SUMMARY_COMPLETION",
      "count": 3,
      "targetParagraphs": [5, 6],
      "cognitiveSkill": "SYNTACTIC_AND_LEXICAL_SYNONYMY"
    }
  ],
  "reasoningRules": {
    "requireVerbatimEvidenceSpan": true,
    "disallowAmbiguousDistractors": true,
    "maxAnswerLengthWords": 3
  }
}
```

### 7.2 Derivation vs. Manual Creation
- **Auto-Derivation**: Admin inputs an approved source document -> `BlueprintExtractorService` analyzes question types, difficulty, lexical CEFR levels, and discourse flow -> generates candidate Blueprint.
- **Manual Construction**: Admin creates a Blueprint directly from a template catalog (e.g. "IELTS Reading Passage 2 - Academic Research Profile").

---

## 8. Generation Pipeline

Generation executes asynchronously through bounded pipeline stages:

```text
[Generation Job Started]
       |
       v
[Stage 1: Topic & Context Synthesizer]
Selects a fresh domain scenario matching Blueprint topicCategory (e.g., "Marine bio-acoustics in Arctic ecosystems").
       |
       v
[Stage 2: Passage / Script Generator]
Generates novel reading passage or listening transcript adhering to word count, paragraph structure, and CEFR C1 vocabulary.
       |
       v
[Stage 3: Question & Key Synthesizer]
For each item in itemDistribution:
  - Generates prompt
  - Generates options (if applicable)
  - Identifies target evidence span in generated passage
  - Defines unambiguous correct answer key
  - Formulates pedagogical explanation referencing evidence span
       |
       v
[Stage 4: Package Aggregation]
Combines generated passage and items into a candidate GeneratedPracticeSet entity.
       |
       v
[Stage 5: Auto-Validation Trigger]
Transfers entity state to AUTO_VALIDATING.
```

### 8.1 Prompt Engineering & System Instructions
- Prompts use structured few-shot examples with strict JSON output formatting.
- System instructions mandate that all questions must be 100% resolvable *solely* from the generated passage text, with zero reliance on outside factual knowledge.
- Instructions prohibit verbatim copying from any input source or reference text.

---

## 9. Novelty / Similarity Safeguards

To detect potential memorization, shallow paraphrasing, or structural leakage from source materials, practice generation employs a multi-tiered novelty safeguard. 

> [!IMPORTANT]
> **Heuristic Nature of Similarity Metrics**:
> Numerical thresholds (such as 4-gram overlap percentages, maximum contiguous token runs, or embedding cosine distances) are **initial conservative engineering heuristics**, NOT universal or legal proofs of copyright safety or non-infringement. A low similarity score does NOT guarantee legal non-infringement, nor does it replace administrative due diligence.
> 
> All similarity thresholds are:
> 1. **Configurable**: Managed via environment and policy definitions rather than hardcoded in business logic.
> 2. **Versioned**: Assigned a formal policy version (e.g. `similarity-policy-v1.0-conservative`) stamped on every evaluation record.
> 3. **Auditable**: Stored alongside exact token matching offsets in `generation_validation_results`.
> 4. **Independently Adjustable**: Calibrated separately by skill and content type (e.g., higher n-gram tolerance for standard IELTS formulaic instructions vs. near-zero tolerance for reading passage bodies).
> 
> **Mandatory Review Invariant**: Regardless of whether similarity metrics return zero or near-zero overlap, human admin review and approval remain strictly mandatory.

```text
+-----------------------------------------------------------------------------------------+
|                  MULTI-LAYER NOVELTY SAFEGUARD (VERSIONED POLICY)                       |
+-----------------------------------------------------------------------------------------+
| Layer 1: Lexical Jaccard & N-Gram Overlap Check                                         |
| - Compares generated passage against ingested source text and approved practice bank.   |
| - Baseline Heuristic: 4-gram token overlap (default threshold: < 3.0% of total tokens). |
| - Contiguous Run Guard: Configurable sequence limit (default: >= 8 contiguous matching  |
|   words triggers WARNING or FAIL depending on policy version).                          |
+-----------------------------------------------------------------------------------------+
| Layer 2: Vector Cosine Semantic Distance Analysis                                       |
| - Generates 768-dim embeddings across 300-word sliding windows of generated text.       |
| - Baseline Heuristic: Max cosine similarity to source chunks (default warning: > 0.82). |
| - Serves as an advisory thematic proximity signal to alert reviewers of parallel flows. |
+-----------------------------------------------------------------------------------------+
| Layer 3: Entity & Named-Anchor Filter                                                   |
| - Extracts proper nouns, geographical locations, researcher names, and specific dates.  |
| - Default Heuristic: Reusing > 20% of unique source named entities (excluding standard  |
|   scientific domain terms like "DNA" or "Antarctica") flags a policy WARNING.           |
+-----------------------------------------------------------------------------------------+
| Layer 4: Distractor & Key Distinctiveness Check                                         |
| - Compares generated options and answer keys against source question items.             |
| - Identical question stem + option set matches trigger an automatic FAIL.               |
+-----------------------------------------------------------------------------------------+
| Layer 5: Reviewer Novelty Report                                                        |
| - Admin review UI visually highlights n-gram matching spans in amber callouts.          |
| - Displays full audit metrics, active policy version, and nearest bank reference texts. |
+-----------------------------------------------------------------------------------------+
```

### 9.1 Similarity Policy Outcomes

The Similarity Validator evaluates candidate drafts against the active policy version and produces one of three outcomes:
- **PASS**: All similarity metrics fall comfortably below configured warning thresholds.
- **WARNING**: Metrics exceed warning threshold but remain below fatal limits (e.g., slight overlap on standard academic terminology). The item enters `PENDING_REVIEW` with an amber advisory banner requiring explicit reviewer sign-off.
- **FAIL**: Metrics breach hard policy ceilings (e.g., contiguous sentence copy or high paragraph overlap). The draft transitions to `NEEDS_REVISION` for mandatory regeneration or editing.

---

## 10. Validation Pipeline

Validation runs deterministically on every generated draft before human review. Each validator produces a structured report containing `validatorName`, `policyVersion`, `status` (`PASS`, `WARNING`, `FAIL`), `metrics`, and `findings`.

```text
                                [Generated Practice Draft]
                                            |
                +---------------------------+---------------------------+
                |                           |                           |
                v                           v                           v
     [Structural Validator]         [Answer Validator]          [Evidence Validator]
     - Schema conformance           - Valid key format          - Evidence span exists
     - Item counts match BP         - 1 correct option for MCQ   - Span verbatim in text
     - Required fields populated    - No empty/blank keys       - Span logically justifies key
                |                           |                           |
                +---------------------------+---------------------------+
                |                           |                           |
                v                           v                           v
     [Ambiguity Validator]        [Similarity Validator]      [Difficulty Validator]
     - Distractors distinct        - Versioned policy checks   - Heuristic CEFR/Band proxy
     - Only 1 viable answer        - N-gram overlap check      - Lexile / Flesch-Kincaid
     - Clear TFNG logic            - Vector cosine distance    - Academic vocabulary index
                |                           |                           |
                +---------------------------+---------------------------+
                                            |
                                            v
                                   [Optional AI Critic]
                                   - Qualitative review
                                   - Pedagogical flow check
                                   - Non-blocking advisory flags
                                            |
                                            v
                                 [Aggregate Gate Decision]
```

### 10.1 Difficulty & CEFR / Band Estimation Principles
- Automated difficulty, Lexile/Flesch-Kincaid readability metrics, and CEFR/Band estimations are **heuristic algorithmic approximations** based on lexical frequency lists (e.g., Academic Word List, CEFR vocabulary profiles) and syntactic complexity indices.
- These metrics must NOT be claimed as certified or standardized IELTS band scores. They serve solely as development and editorial guides unless formally calibrated against qualified human assessment benchmark data.

### 10.2 Validation Gate Decision Rules
- **FAIL**: If any validator logs a `FAIL` severity:
  - Practice state set to `NEEDS_REVISION`.
  - Admin notified of deterministic errors; practice blocked from `PENDING_REVIEW` until resolved.
- **WARNING**: If validators log only `WARNING` or `INFO` (e.g., minor readability proxy deviation or advisory n-gram cluster):
  - Practice state set to `PENDING_REVIEW`.
  - Reviewer UI displays warning banners requiring explicit reviewer acknowledgment before approval.
- **PASS**: All validators return `INFO`/`PASS`:
  - Practice state set to `PENDING_REVIEW` with green verification badge.

---

## 11. State Machine

The lifecycle of practice generation jobs and generated practice sets is governed by a strict finite state machine:

```text
                               +-----------------+
                               |      DRAFT      |
                               +--------+--------+
                                        | (Admin triggers generation)
                                        v
                               +-----------------+
                               |   GENERATING    |
                               +--------+--------+
                                        | (AI synthesis complete)
                                        v
                               +-----------------+
                               | AUTO_VALIDATING |
                               +--------+--------+
                                        |
                 +----------------------+----------------------+
                 |                                             |
         (Validation PASS)                             (Validation FAIL)
                 |                                             |
                 v                                             v
        +-----------------+                           +-----------------+
+------>| PENDING_REVIEW  |                           | NEEDS_REVISION  |<-----+
|       +--------+--------+                           +--------+--------+      |
|                |                                             |               |
|       +--------+--------+                                    | (Regenerate   |
|       |                 |                                    |  or Edit)     |
|   [APPROVE]         [REJECT]                                 +---------------+
|       |                 |
|       v                 v
|  +---------+      +----------+
|  |APPROVED |      | REJECTED |
|  +----+----+      +----------+
|       |
|       v (Hydrate Bank)
|  [PRACTICE BANK]
|
+--- [Revision requested by Admin]
```

### 11.1 Permitted Transitions Table

| Current State | Target State | Trigger / Action | Authorization |
|---|---|---|---|
| `DRAFT` | `GENERATING` | Admin submits source/blueprint for generation | `ADMIN` |
| `GENERATING` | `AUTO_VALIDATING` | Background generator job finishes payload synthesis | `SYSTEM` |
| `GENERATING` | `DRAFT` | Generation timeout or provider fatal failure | `SYSTEM` / `ADMIN` |
| `AUTO_VALIDATING` | `PENDING_REVIEW` | All validators return PASS or WARNING | `SYSTEM` |
| `AUTO_VALIDATING` | `NEEDS_REVISION` | One or more validators return FAIL | `SYSTEM` |
| `PENDING_REVIEW` | `APPROVED` | Admin explicitly approves practice set | `ADMIN` |
| `PENDING_REVIEW` | `NEEDS_REVISION` | Admin requests adjustments with reviewer notes | `ADMIN` |
| `PENDING_REVIEW` | `REJECTED` | Admin rejects practice set permanently | `ADMIN` |
| `NEEDS_REVISION` | `GENERATING` | Admin triggers targeted or full regeneration | `ADMIN` |
| `NEEDS_REVISION` | `AUTO_VALIDATING` | Admin applies manual edits and requests re-validation | `ADMIN` |
| `NEEDS_REVISION` | `REJECTED` | Admin abandons revision and marks rejected | `ADMIN` |
| `APPROVED` | `REJECTED` | Post-approval decommission / removal | `ADMIN` |
| `REJECTED` | *Terminal* | Archival state; cannot transition to active bank | — |

**Illegal Transition Invariants**:
- Under no circumstances can `GENERATING`, `AUTO_VALIDATING`, `DRAFT`, or `PENDING_REVIEW` transition directly to `PRACTICE BANK` or public visibility.
- Direct transition `AI_GENERATED -> PUBLIC` is architecturally impossible.

---

## 12. Review & Approval

### 12.1 Editorial Inspection Surface
When a practice enters `PENDING_REVIEW`, the Admin Review Workspace presents:
1. **Side-by-Side Reviewer Layout**:
   - Left Pane: Generated Reading Passage or Listening Transcript with interactive highlightable paragraphs.
   - Right Pane: Questions, options, single answer key, explanation, and mapped evidence span.
2. **Evidence Linking**: Clicking a question highlights the corresponding evidence span in the passage text.
3. **Novelty & Similarity Inspector**: Visual comparison against source document and nearest bank matches, highlighting any n-gram clusters.
4. **Validation Summary**: Complete breakdown of all 6 validators, detailing checks passed and specific warnings.
5. **Model Lineage**: Generator version, AI model/provider used, prompt template version, generation latency, and token consumption.

### 12.2 Admin Review Actions
- **APPROVE**: Marks state as `APPROVED`. Triggers immediate background practice bank hydration.
- **REQUEST REVISION**: Requires reviewer comments specifying flawed questions or passage paragraphs. Transitions to `NEEDS_REVISION`.
- **REJECT**: Requires a rejection reason category (e.g. `PEDAGOGICAL_DEFECT`, `SIMILARITY_CONCERN`, `AMBIGUOUS_ITEMS`, `UNSUITABLE_TOPIC`). Transitions to `REJECTED`.

### 12.3 Immutable Audit Trail
Every review action records:
- Action UUID.
- Practice Set UUID and Version ID.
- Admin User UUID and email.
- Previous state and New state.
- Reviewer notes / change annotations.
- Timestamp with timezone.

---

## 13. Revision / Versioning

### 13.1 Version Immutability
- Practice generation sets follow an append-only version model (`generated_practice_versions`).
- When revision is triggered, the existing version (e.g. Version 1) is locked. A new candidate (Version 2) is created.
- Admins can toggle diff views between Version 1 and Version 2 in the review interface.

### 13.2 Targeted vs. Full Regeneration
- **Full Regeneration**: Completely re-synthesizes passage and questions from the Blueprint.
- **Targeted Item Regeneration**: Keeps approved passage text intact; re-synthesizes only specific failed questions (e.g. Question 3 with ambiguous distractors).
- **Manual Admin Inline Edit**: Admin fixes typos, adjusts distractor wording, or clarifies explanation text directly, followed by automated re-validation.

---

## 14. Practice Bank Integration

Once a practice set achieves `APPROVED` status, the `PracticeBankHydrationService` maps it into the production practice store:

```text
[APPROVED GeneratedPracticeSet]
          |
          v
[PracticeBankHydrationService]
          |
          +---> INSERT / UPSERT INTO practice_sets (id, skill, title, description, ...)
          +---> INSERT / UPSERT INTO practice_passages (set_id, title, content, ...)
          +---> INSERT / UPSERT INTO practice_paragraphs (passage_id, paragraph_index, text, ...)
          +---> INSERT / UPSERT INTO practice_questions (set_id, prompt, options, answer_key, explanation, ...)
          +---> INSERT INTO practice_provenance_meta (set_id, generation_job_id, blueprint_id, approver_id, approved_at)
          |
          v
[Active in PracticeService & SyntheticPracticeCatalog]
```

### 14.1 Runtime Compatibility
- Approved sets are fully compatible with existing `PracticeSet`, `PracticeQuestion`, `PracticePassage`, and `PracticeParagraph` records.
- Deterministic attempt evaluation (`JdbcPracticeAttemptStore`, `PracticeAttemptResult`) processes generated questions identically to existing foundation sets with zero client-side logic divergence.
- Student endpoints receive standard practice payloads with zero exposure of underlying generation prompts, source documents, or AI critic logs.

---

## 15. Skill Rollout: Reading (Phase 3A)

Reading is the primary focus of initial Phase 3 implementation due to its rich deterministic validation characteristics.

### Supported Question Formats
1. **Multiple Choice (Standard 4-Option)**: Single correct key, 3 plausible but passage-refutable distractors.
2. **True / False / Not Given (TFNG)**:
   - *True*: Explicitly affirmed by passage evidence span.
   - *False*: Directly contradicted by passage evidence span.
   - *Not Given*: Plausible concept but neither affirmed nor contradicted in the text.
3. **Matching Headings**: Set of paragraph headings (including 1–2 unused distractors) mapped to numbered paragraphs.
4. **Matching Information**: Locating specific facts or claims across paragraphs.
5. **Sentence / Summary Completion**: Blank filling with strict word count limits (e.g., "NO MORE THAN TWO WORDS") verified directly from passage text.
6. **Short Answer Questions**: Direct factual questions answered using exact words from the passage.

---

## 16. Skill Extension: Writing (Phase 3B)

Phase 3B extends the generator to academic writing tasks:

### Task 1 (Academic Data / Process Description)
- Generates structured synthetic datasets (tables, bar charts, line graphs, process diagrams) with strict internal consistency.
- Generates Task 1 prompt specifying standard IELTS instructions ("Summarise the information by selecting and reporting the main features...").
- Generates a reference model response (Band 8+ standard) with comprehensive overview, grouping, and accurate data citations for admin review.
- Prohibits copying published charts or copyrighted survey data.

### Task 2 (Discursive Essay Prompts)
- Generates novel prompts across standard IELTS essay types (Agree/Disagree, Discuss Both Views, Problem-Solution, Two-Part Question).
- Generates sample essay outlines, topical vocabulary lists, and scoring rubrics aligned with public IELTS band descriptors (Task Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy).

---

## 17. Skill Extension: Listening (Phase 3C)

Phase 3C designs audio-script generation ready for future media binding:

### Architecture
- **Script Generation**: Multi-speaker conversational scripts (Sections 1 & 3) and monologues (Sections 2 & 4).
- **Timecoded Evidence Mapping**: Questions mapped to specific script turns and character offset spans.
- **Accompanying Tasks**: Form completion, table completion, map labeling, multiple choice.
- **TTS Decoupling**: Phase 3 outputs the structured dialogue transcript, speaker tags, and timecode metadata. Media recording/synthesis occurs in dedicated downstream media jobs.

---

## 18. Skill Extension: Speaking (Phase 3D)

Phase 3D generates comprehensive three-part Speaking interview sets:

### Architecture
- **Part 1 (Introduction & Familiar Topics)**: 3 topical clusters with 4 questions each (e.g. "Daily Routine", "Public Parks", "Technology in Education").
- **Part 2 (Individual Long Turn / Cue Card)**: Topic card with 1 central prompt, 4 guiding bullet points, and 1-minute prep / 2-minute response constraints.
- **Part 3 (Two-Way Discussion)**: 4–6 abstract, thematic extension questions building upon the Part 2 topic.
- **Safety**: Generates prompts only. No AI examiner impersonation; explicit labeling as practice questions.

---

## 19. Data Model

The PostgreSQL schema integrates seamlessly into existing Flyway migrations (e.g. `V9__create_practice_generator_schema.sql`).

```text
+------------------------------+       +------------------------------------+
|  practice_generation_sources |       |  practice_generation_blueprints    |
|------------------------------|       |------------------------------------|
| id (PK, UUID)                |<--+   | id (PK, UUID)                      |<--+
| title (VARCHAR)              |   |   | skill (VARCHAR)                    |   |
| source_type (VARCHAR)        |   |   | title (VARCHAR)                    |   |
| author (VARCHAR)             |   |   | target_band (NUMERIC)              |   |
| rights_status (VARCHAR)      |   |   | blueprint_schema (JSONB)           |   |
| license_note (TEXT)          |   |   | created_by (UUID)                  |   |
| normalized_content (TEXT)    |   |   +------------------------------------+   |
| checksum (CHAR(64))          |   |                                            |
| created_by (UUID)            |   |                                            |
+------------------------------+   |                                            |
                                   |                                            |
+----------------------------------+---+                                        |
|      practice_generation_jobs        |                                        |
|--------------------------------------|                                        |
| id (PK, UUID)                        |                                        |
| source_id (FK, UUID) ----------------+                                        |
| blueprint_id (FK, UUID) ------------------------------------------------------+
| status (VARCHAR)                     |
| skill (VARCHAR)                      |
| model_id (VARCHAR)                   |
| prompt_template_version (VARCHAR)    |
| created_by (UUID)                    |
| started_at (TIMESTAMPTZ)             |
| completed_at (TIMESTAMPTZ)           |
+------------------+-------------------+
                   | 1
                   |
                   | has many
                   v *
+--------------------------------------+       +------------------------------------+
|       generated_practice_sets        |       |    generated_practice_versions     |
|--------------------------------------| 1   * |------------------------------------|
| id (PK, UUID)                        |<----->| id (PK, UUID)                      |
| job_id (FK, UUID)                    |       | set_id (FK, UUID)                  |
| skill (VARCHAR)                      |       | version_number (INT)               |
| current_version_id (UUID)            |       | passage_content (JSONB)            |
| state (VARCHAR)                      |       | questions_payload (JSONB)          |
| published_set_id (VARCHAR)           |       | created_at (TIMESTAMPTZ)           |
| approved_by (UUID)                   |       +-----------------+------------------+
| approved_at (TIMESTAMPTZ)            |                         | 1
+--------------------------------------+                         |
                   | 1                                           | has many
                   | has many                                    v *
                   v *                         +------------------------------------+
+--------------------------------------+       |    generation_validation_results   |
|       practice_review_actions        |       |------------------------------------|
|--------------------------------------|       | id (PK, UUID)                      |
| id (PK, UUID)                        |       | version_id (FK, UUID)              |
| set_id (FK, UUID)                    |       | validator_name (VARCHAR)           |
| version_id (FK, UUID)                |       | status (VARCHAR: PASS, WARN, FAIL) |
| admin_id (UUID)                      |       | findings (JSONB)                   |
| action (VARCHAR: APPROVE/REVISION/..) |       | executed_at (TIMESTAMPTZ)          |
| reviewer_notes (TEXT)                |       +------------------------------------+
| created_at (TIMESTAMPTZ)             |
+--------------------------------------+
```

### 19.1 Table Schema Details

#### `practice_generation_sources`
- `id` UUID PRIMARY KEY
- `title` VARCHAR(240) NOT NULL
- `source_type` VARCHAR(40) NOT NULL
- `author` VARCHAR(240)
- `rights_status` VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW' CHECK (rights_status IN ('PENDING_REVIEW', 'APPROVED', 'RESTRICTED', 'REJECTED'))
- `license_note` TEXT NOT NULL DEFAULT ''
- `normalized_content` TEXT NOT NULL
- `checksum` CHAR(64) NOT NULL UNIQUE
- `created_by` UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT
- `created_at` TIMESTAMPTZ NOT NULL
- `updated_at` TIMESTAMPTZ NOT NULL

#### `practice_generation_blueprints`
- `id` UUID PRIMARY KEY
- `skill` VARCHAR(16) NOT NULL CHECK (skill IN ('READING', 'LISTENING', 'WRITING', 'SPEAKING'))
- `title` VARCHAR(240) NOT NULL
- `target_band` NUMERIC(3,1) NOT NULL
- `blueprint_schema` JSONB NOT NULL
- `created_by` UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT
- `created_at` TIMESTAMPTZ NOT NULL
- `updated_at` TIMESTAMPTZ NOT NULL

#### `practice_generation_jobs`
- `id` UUID PRIMARY KEY
- `source_id` UUID NOT NULL REFERENCES practice_generation_sources(id) ON DELETE RESTRICT
- `blueprint_id` UUID NOT NULL REFERENCES practice_generation_blueprints(id) ON DELETE RESTRICT
- `skill` VARCHAR(16) NOT NULL
- `status` VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'GENERATING', 'VALIDATING', 'COMPLETED', 'FAILED'))
- `model_id` VARCHAR(80) NOT NULL
- `prompt_template_version` VARCHAR(40) NOT NULL
- `error_message` VARCHAR(2000)
- `created_by` UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT
- `started_at` TIMESTAMPTZ
- `completed_at` TIMESTAMPTZ
- `created_at` TIMESTAMPTZ NOT NULL

#### `generated_practice_sets`
- `id` UUID PRIMARY KEY
- `job_id` UUID NOT NULL REFERENCES practice_generation_jobs(id) ON DELETE RESTRICT
- `skill` VARCHAR(16) NOT NULL
- `title` VARCHAR(240) NOT NULL
- `current_version_id` UUID
- `state` VARCHAR(24) NOT NULL CHECK (state IN ('DRAFT', 'GENERATING', 'AUTO_VALIDATING', 'PENDING_REVIEW', 'APPROVED', 'NEEDS_REVISION', 'REJECTED'))
- `published_set_id` VARCHAR(120) UNIQUE
- `approved_by` UUID REFERENCES app_users(id) ON DELETE SET NULL
- `approved_at` TIMESTAMPTZ
- `created_at` TIMESTAMPTZ NOT NULL
- `updated_at` TIMESTAMPTZ NOT NULL

#### `generated_practice_versions`
- `id` UUID PRIMARY KEY
- `set_id` UUID NOT NULL REFERENCES generated_practice_sets(id) ON DELETE CASCADE
- `version_number` INT NOT NULL
- `passage_content` JSONB NOT NULL
- `questions_payload` JSONB NOT NULL
- `novelty_report` JSONB NOT NULL DEFAULT '{}'::jsonb
- `created_at` TIMESTAMPTZ NOT NULL,
- CONSTRAINT generated_practice_versions_uq UNIQUE (set_id, version_number)

#### `generation_validation_results`
- `id` UUID PRIMARY KEY
- `version_id` UUID NOT NULL REFERENCES generated_practice_versions(id) ON DELETE CASCADE
- `validator_name` VARCHAR(64) NOT NULL
- `status` VARCHAR(16) NOT NULL CHECK (status IN ('PASS', 'WARNING', 'FAIL'))
- `findings` JSONB NOT NULL DEFAULT '[]'::jsonb
- `executed_at` TIMESTAMPTZ NOT NULL

#### `practice_review_actions`
- `id` UUID PRIMARY KEY
- `set_id` UUID NOT NULL REFERENCES generated_practice_sets(id) ON DELETE CASCADE
- `version_id` UUID NOT NULL REFERENCES generated_practice_versions(id) ON DELETE CASCADE
- `admin_id` UUID NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT
- `action` VARCHAR(24) NOT NULL CHECK (action IN ('APPROVE', 'REQUEST_REVISION', 'REJECT'))
- `reviewer_notes` TEXT NOT NULL DEFAULT ''
- `created_at` TIMESTAMPTZ NOT NULL

---

## 20. API Contracts

All endpoints reside under `/api/admin/practice-generator` and require an authenticated session with `UserRole.ADMIN`.

### 20.1 Register Source Material
- **POST** `/api/admin/practice-generator/sources`
- **Request (Multipart or JSON)**:
  ```json
  {
    "title": "Ecological Study: Microplastic Deposition",
    "sourceType": "PASTED_TEXT",
    "author": "Dr. E. Vance",
    "rightsStatus": "APPROVED",
    "licenseNote": "CC-BY 4.0 Open Access Article",
    "content": "Recent surveys in sub-polar estuaries reveal unexpected microplastic accumulation..."
  }
  ```
- **Response (201 Created)**:
  ```json
  {
    "id": "e89c091a-7b3b-4638-8fa1-0182649b1a03",
    "title": "Ecological Study: Microplastic Deposition",
    "rightsStatus": "APPROVED",
    "wordCount": 820,
    "checksum": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "createdAt": "2026-09-27T10:15:30Z"
  }
  ```

### 20.2 Extract / Create Blueprint
- **POST** `/api/admin/practice-generator/blueprints/extract`
- **Request**:
  ```json
  {
    "sourceId": "e89c091a-7b3b-4638-8fa1-0182649b1a03",
    "skill": "READING",
    "targetBand": 7.5
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "blueprintId": "bp-98124b61-23fa",
    "skill": "READING",
    "title": "Derived Blueprint: Microplastic Deposition",
    "targetBand": 7.5,
    "blueprintSchema": {
      "topicCategory": "ENVIRONMENTAL_SCIENCE",
      "passageStructure": { "targetWordCount": 800, "paragraphCount": 6 },
      "itemDistribution": [
        { "taskType": "TRUE_FALSE_NOT_GIVEN", "count": 4 },
        { "taskType": "MULTIPLE_CHOICE", "count": 3 }
      ]
    }
  }
  ```

### 20.3 Trigger Practice Generation Job
- **POST** `/api/admin/practice-generator/jobs`
- **Request**:
  ```json
  {
    "sourceId": "e89c091a-7b3b-4638-8fa1-0182649b1a03",
    "blueprintId": "bp-98124b61-23fa",
    "skill": "READING"
  }
  ```
- **Response (202 Accepted)**:
  ```json
  {
    "jobId": "job-77182a93-84bc",
    "status": "GENERATING",
    "createdAt": "2026-09-27T10:16:00Z"
  }
  ```

### 20.4 Get Practice Set Review Payload
- **GET** `/api/admin/practice-generator/sets/{setId}`
- **Response (200 OK)**:
  ```json
  {
    "id": "set-11928374-bbac",
    "skill": "READING",
    "title": "Arctic Estuarine Plastic Dynamics",
    "state": "PENDING_REVIEW",
    "currentVersion": {
      "versionNumber": 1,
      "passage": {
        "title": "Arctic Estuarine Plastic Dynamics",
        "paragraphs": [
          { "id": "p1", "text": "In high-latitude river deltas, sediment sampling has uncovered..." },
          { "id": "p2", "text": "Contrary to initial hypotheses that sea ice prevents settling..." }
        ]
      },
      "questions": [
        {
          "id": "q1",
          "taskType": "TRUE_FALSE_NOT_GIVEN",
          "prompt": "Sea ice completely stops plastic particulates from settling on the riverbed.",
          "options": ["TRUE", "FALSE", "NOT GIVEN"],
          "answerKey": "FALSE",
          "evidenceSpan": "Contrary to initial hypotheses that sea ice prevents settling",
          "explanation": "Paragraph 2 states that sea ice does not prevent particle settling, making the statement FALSE."
        }
      ],
      "validationSummary": {
        "overallStatus": "PASS",
        "results": [
          { "validator": "StructuralValidator", "status": "PASS", "findings": [] },
          { "validator": "EvidenceValidator", "status": "PASS", "findings": [] },
          {
            "validator": "SimilarityValidator",
            "policyVersion": "similarity-policy-v1.0-conservative",
            "status": "PASS",
            "findings": [
              {
                "metric": "4gram_overlap",
                "value": 0.008,
                "threshold": 0.030,
                "heuristicEvaluation": "Below configured warning threshold"
              }
            ]
          },
          {
            "validator": "DifficultyValidator",
            "policyVersion": "difficulty-heuristic-v1.0",
            "status": "PASS",
            "findings": [
              {
                "metric": "flesch_kincaid_grade",
                "value": 11.4,
                "targetRange": [10.0, 13.0],
                "heuristicEvaluation": "Approximate CEFR C1 proxy alignment"
              }
            ]
          }
        ]
      }
    }
  }
  ```

### 20.5 Review Actions: Approve / Revision / Reject
- **POST** `/api/admin/practice-generator/sets/{setId}/review`
- **Request (Approve)**:
  ```json
  {
    "action": "APPROVE",
    "reviewerNotes": "Passage and questions verified. Excellent distractor quality."
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "setId": "set-11928374-bbac",
    "state": "APPROVED",
    "publishedSetId": "reading-gen-arctic-estuarine-plastic-dynamics",
    "approvedAt": "2026-09-27T10:20:00Z"
  }
  ```

---

## 21. Admin UX

The Admin Experience integrates cleanly with the Academic Luxury 2.0 design language:

1. **Generation Dashboard (`/admin/practice-generator`)**:
   - Status counters for `Pending Review`, `In Progress`, `Needs Revision`, and `Approved This Week`.
   - Filterable table of all generation jobs with state badges, skill tags, and validation pills.
2. **New Generation Modal**:
   - 3-step wizard: (1) Select/Upload Approved Source, (2) Choose/Customize Blueprint, (3) Select Skill and Initiate Generation.
   - Immediate validation feedback if selected source has unapproved rights.
3. **Review & Approval Canvas (`/admin/practice-generator/sets/:setId`)**:
   - Split-screen reading/question canvas with synchronized hover and evidence pinpoints.
   - Amber warning callouts for any minor validator flags.
   - Reviewer toolbar with quick actions: `Approve`, `Request Revision`, `Edit Inline`, `Reject`.
   - Version history sidebar allowing instantaneous comparison of previous iterations.

---

## 22. Security

1. **Authorization Barrier**: Every generation, source registration, blueprint, and review route requires `UserRole.ADMIN` enforced by `AuthInterceptor`. Non-admins receive `403 FORBIDDEN`.
2. **Student API Boundary Isolation**: Public practice queries (`GET /api/practice/sets`) filter strictly on `published = true AND approved = true`. Unapproved practice tables are never queried by customer controllers.
3. **File & Content Guardrails**:
   - Source file uploads restricted to `application/pdf`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, and `text/plain`. Max 15 MiB per file.
   - Plaintext sanitization prevents stored XSS in passages or explanations.
4. **Credential Isolation**: AI provider API keys reside exclusively in backend environment variables and `ProviderConfigurationRegistry`; never passed to frontend or recorded in job metadata.

---

## 23. Error Handling

| Failure Scenario | Immediate System Action | Resulting State |
|---|---|---|
| AI Provider Timeout / 5xx | Router attempts fallback (`Groq` -> `Cloudflare` -> `Gemini`). If all fail, marks job `FAILED`. | `GENERATING` -> `DRAFT` (Retriable) |
| Malformed JSON from Provider | Regex parser attempts repair. If invalid, logs failure and triggers single retry. | `GENERATING` -> `DRAFT` |
| Source Rights Unapproved | Job creation rejected with `400 Bad Request`. | Blocked from creation |
| Structural Validator Failure | Validator logs exact missing field. | `AUTO_VALIDATING` -> `NEEDS_REVISION` |
| Evidence Span Mismatch | Answer key evidence not found verbatim in passage. | `AUTO_VALIDATING` -> `NEEDS_REVISION` |
| High Similarity Policy Breach | Validator logs token overlap spans against source/bank per active policy. | `AUTO_VALIDATING` -> `NEEDS_REVISION` (if FAIL) or `PENDING_REVIEW` (if WARNING with required reviewer sign-off) |
| Ambiguous Distractor Detected | Ambiguity validator flags multiple viable answers. | `AUTO_VALIDATING` -> `NEEDS_REVISION` |
| Admin Rejection | Review action logged with mandatory reason. | `PENDING_REVIEW` -> `REJECTED` |

---

## 24. Observability / Audit

1. **Generation Metrics & Tracing**:
   - Generation latency per stage (Passage, Questions, Validation).
   - Provider token count and cost tracking per generation job.
   - Validator pass/fail rates aggregated by question type and model ID.
2. **Structured Logging**:
   - All state transitions logged with `[JOB_ID]`, `[SET_ID]`, `[ADMIN_ID]`, `[FROM_STATE]`, `[TO_STATE]`.
   - Security audit logs on every rights status alteration and review decision.
3. **Zero Data Leakage in Logs**: Source content and generation prompts truncated in application logs; authorization tokens completely masked.

---

## 25. AI Provider Boundary

- Generation operations utilize the existing `com.ieltsaitutor.ai.provider.AiProvider` interface and `AiProviderRouter`.
- No secondary or external provider framework is introduced.
- Prompt builders generate standardized `AiChatCommand` requests specifying `AiChatContext` with skill and generation parameters.
- Provider responses are parsed through dedicated type-safe record adapters (`PracticeGenerationResult`).

---

## 26. RAG Integration Boundary

- Practice Generation and Retrieval-Augmented Generation (RAG) share governance principles but remain loosely coupled:
  - **Shared**: Document rights states (`PENDING_REVIEW`, `APPROVED`, `RESTRICTED`, `REJECTED`) and document metadata models.
  - **Decoupled**: Practice generation does *not* execute embedding vector search during question synthesis; it receives an explicit normalized source document and derives an abstract blueprint.

---

## 27. AI Phase 2 Integration Boundary

AI Phase 2 (Adaptive Learning Intelligence) interacts with AI Phase 3 strictly through the approved practice catalog:

```text
[AI Phase 2: Adaptive Engine]
           |
           | 1. Learner struggles with "Reading: Matching Headings" (Band 6.5)
           |
           v
[Query: PracticeBankService]
           | (SELECT * FROM practice_sets WHERE skill = 'READING' AND task_type = 'MATCHING_HEADINGS' AND is_approved = true)
           v
[APPROVED Practice Bank]
           |
           | 2. Returns verified, admin-approved generated practice set
           v
[Delivered to Student]
```

**Critical Invariant**: AI Phase 2 must never directly trigger real-time AI generation for student consumption without passing through Phase 3's administrative approval pipeline.

---

## 28. Testing Strategy

1. **State Machine Verification**:
   - Property-based tests verifying all forbidden transitions (e.g. `GENERATING -> APPROVED`, `DRAFT -> PENDING_REVIEW`) throw `IllegalStateException`.
2. **Rights Governance Tests**:
   - Unit tests confirming generation jobs reject unapproved, restricted, or rejected sources.
3. **Deterministic Validator Tests**:
   - Unit tests for `StructuralValidator`, `AnswerValidator`, `EvidenceValidator`, and `SimilarityValidator` with deliberately corrupted inputs (missing evidence spans, duplicate distractors, high n-gram overlap).
4. **Security & Role Tests**:
   - Security tests verifying non-admin users cannot access any `/api/admin/practice-generator/**` endpoints.
   - Verification that unapproved practice sets never appear in student practice catalog queries.
5. **Mock Provider Testing**:
   - All unit and integration test suites run against mock `AiProvider` stubs; zero external network calls or paid API quota consumed during builds.
6. **Similarity Policy Calibration Tests**:
   - Unit tests verifying that changing policy version thresholds (e.g. from conservative 3% to relaxed 5%) dynamically alters `PASS`/`WARN`/`FAIL` classifications without altering the immutable state transition rules or bypassing mandatory human approval.

---

## 29. Migration & Compatibility

- Database changes will be introduced in Flyway migration `V9__create_practice_generator_schema.sql`.
- Existing practice attempt tables (`learning_attempts`, `learning_activity`) remain 100% backward compatible.
- `SyntheticPracticeCatalog` will be enhanced to load from both default synthetic seeds and the approved dynamic database practice store.

---

## 30. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| AI hallucinating invalid answer keys | High | Deterministic `EvidenceValidator` requires verbatim substring existence in passage before review stage. |
| Accidental ingestion of copyrighted exams | Critical | Hard rights gating (`rights_status = APPROVED` mandatory), SHA-256 blocklist checksums, multi-layer heuristic similarity screening, and mandatory human administrative review. |
| Inconsistent task difficulty | Medium | Lexical/syntactic density analyzer in `DifficultyValidator` (heuristic CEFR/Band proxies) combined with mandatory human expert review. |
| Flawed or multiple plausible distractors | Medium | `AmbiguityValidator` cross-checks distractors against evidence sentences to verify distinct falsifiability. |

---

## 31. Non-Goals

1. **No Real-Time In-Session Generation for Students**: Students never receive freshly generated unreviewed questions during a test.
2. **No Claim of Official IELTS Ownership**: All generated materials explicitly labeled as IELTS-style practice materials.
3. **No Automatic Web Crawling**: Sources must be explicitly uploaded and authorized by an administrator.
4. **No Synthetic Audio Generation in Phase 3**: Listening audio files will be produced in a future dedicated audio media pipeline.

---

## 32. Definition of Done

The AI Phase 3 specification is complete when:
1. All 33 required architectural sections are documented in depth with zero ambiguous placeholders.
2. Source rights governance, substantive novelty safeguards, multi-layer validation, state machine transitions, and practice bank hydration are formally specified.
3. The boundary between AI Phase 3 (Generator/Review) and AI Phase 2 (Adaptive Student Intelligence) is unambiguously established.
4. Data models and API contracts align with existing repository Flyway/Spring Boot conventions.
5. The document is committed on `design/ai-phase3-practice-generator` in the dedicated worktree for human review.

---

## 33. Open Questions & Rulings

### Decision 1: Practice Bank Storage Strategy
- **Ruling**: Store approved generated practices in PostgreSQL relational tables alongside existing synthetic catalogs rather than creating a separate CMS database. Reuses `PracticeSet`, `PracticeQuestion`, `PracticePassage`, and `PracticeParagraph` records directly.

### Decision 2: TFNG / Yes-No-Not-Given Grounding Rules
- **Ruling**: For True/False/Not Given questions, the generator must provide an explicit passage evidence sentence for *True* and *False* items. For *Not Given* items, it must identify the topical distractor sentence in the passage that discusses the general subject without affirming the specific claim.

### Decision 3: AI Critic Authority
- **Ruling**: The AI Critic is strictly advisory. It can produce warnings or recommendations for the admin reviewer, but cannot override deterministic validator failures or grant approval autonomously.

### Decision 4: Open Questions for Implementation Planning
- None. All architectural boundaries, data models, validators, state machine rules, and security gates are resolved. Ready for human design review.

### Decision 5: Configurable Similarity & Difficulty Heuristics
- **Ruling**: Automated similarity metrics (n-gram overlap, vector cosine distance, entity retention) and difficulty metrics (CEFR levels, Lexile, Flesch-Kincaid) are defined as versioned, configurable engineering heuristics rather than absolute legal or psychometric guarantees. Low similarity scores do not constitute legal proof of non-infringement; human administrative review and rights verification remain the authoritative editorial gate.
