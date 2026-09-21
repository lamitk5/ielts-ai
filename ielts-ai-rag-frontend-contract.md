# IELTS AI Tutor — RAG ↔ Frontend Integration Contract

Date: 2026-09-19  
Status: Working contract for cross-agent collaboration  
Project: **Nghiên cứu và xây dựng Nền tảng Web Trợ giảng AI hỗ trợ luyện thi IELTS 4 kỹ năng trực tuyến**

## 1. Purpose

This document aligns the RAG/research work with the React/Spring Boot product architecture so Gemini, ChatGPT, Codex, and OpenCode do not produce incompatible implementations.

- Gemini owns domain/rubric/RAG research proposals and candidate source metadata.
- ChatGPT owns application architecture, frontend UX, normalized API contracts, and implementation task boundaries.
- Codex implements the approved frontend tasks.
- OpenCode reviews/fixes the current task only.

Phase 1 is presentation-only. Real RAG integration belongs to a later phase.

## 2. Grounding pipeline

```text
Learner input
  → React UI
  → Spring Boot REST API
  → AI Orchestrator
  → skill/task/rubric metadata filter
  → embedding + retrieval
  → PostgreSQL + pgvector
  → top-k approved chunks
  → rubric-aware prompt
  → LLM / speech service
  → JSON schema validation
  → output guardrail
  → normalized application response
  → React UI
```

`topK` is configurable. Initial experiments may use 3–5 chunks after metadata filtering.

RAG is described as a mechanism for grounding, traceability, and reducing hallucination risk. It must not be presented as eliminating hallucinations.

## 3. Source-rights rule

Candidate sources must be classified before ingestion. Each chunk should preserve metadata:

```text
sourceId
sourceTitle
sourceType
skill
taskType
rubricCriterion
rightsStatus
version
section
```

Suggested `rightsStatus` values:

```text
public_official
licensed
permission_granted
project_created
learner_consented
unverified
restricted
```

Only approved statuses may enter the production/research knowledge base.

Do not claim that Cambridge IELTS books or internal British Council materials have been ingested unless the research team has documented the right to use those exact materials.

## 4. Prompt policy

Do not fabricate an examiner biography or credentials.

Preferred system-role pattern:

```text
You are an IELTS Writing assessment assistant.
Evaluate the learner submission only against the supplied rubric and retrieved evidence.
Do not invent criteria, citations, source claims, or examiner credentials.
If the provided evidence is insufficient, return insufficient_context.
Return only data conforming to the required JSON schema.
```

Writing Task 1 and Task 2 must not hard-code the same first criterion label:

- Task 1: Task Achievement (TA)
- Task 2: Task Response (TR)
- Both: Coherence & Cohesion (CC), Lexical Resource (LR), Grammatical Range & Accuracy (GRA)

## 5. Normalized tutor response

Spring Boot should normalize provider output before React receives it.

```json
{
  "messageId": "msg_123",
  "content": "...",
  "grounding": {
    "status": "grounded",
    "sourceCount": 3
  },
  "citations": [
    {
      "sourceId": "rubric-writing-task2-v1",
      "title": "Rubric Writing Task 2",
      "section": "Task Response"
    }
  ]
}
```

Supported `grounding.status` values:

```text
retrieving
generating
grounded
insufficient_context
error
```

The frontend shows high-level processing status only. It must not expose chain-of-thought or hidden model reasoning.

## 6. Normalized Writing assessment response

```json
{
  "assessmentType": "writing_task_2",
  "overallBandEstimate": 6.5,
  "criteria": [
    {
      "code": "TR",
      "label": "Task Response",
      "bandEstimate": 6.5,
      "summary": "..."
    },
    {
      "code": "CC",
      "label": "Coherence & Cohesion",
      "bandEstimate": 6.0,
      "summary": "..."
    },
    {
      "code": "LR",
      "label": "Lexical Resource",
      "bandEstimate": 6.5,
      "summary": "..."
    },
    {
      "code": "GRA",
      "label": "Grammatical Range & Accuracy",
      "bandEstimate": 6.0,
      "summary": "..."
    }
  ],
  "strengths": ["..."],
  "issues": [
    {
      "category": "grammar",
      "location": "paragraph-2",
      "explanation": "...",
      "suggestion": "..."
    }
  ],
  "suggestions": ["..."],
  "citations": [
    {
      "sourceId": "rubric-writing-task2-v1",
      "title": "Rubric Writing Task 2",
      "section": "Task Response"
    }
  ],
  "grounding": {
    "status": "grounded",
    "sourceCount": 4
  },
  "disclaimer": "Band điểm do AI ước lượng nhằm hỗ trợ luyện tập, không phải kết quả IELTS chính thức."
}
```

React must not consume raw Gemini/Groq/Azure/SambaNova payloads directly.

## 7. Phase 1 UI preparation

Phase 1 implements mock UI only:

- Floating AI Tutor button and panel
- Skeleton loading
- `grounded` badge
- generic source chips
- `insufficient_context` state
- no vector DB
- no embedding
- no AI API call
- no real examiner score
- no unverified third-party source claim

Mock source labels should remain generic, for example:

```text
Rubric Writing Task 2
Hướng dẫn cải thiện lập luận
Nguồn tham chiếu 01
```

## 8. Experimental evaluation

Proposed research design:

- approximately 100 diverse Writing/Speaking samples
- at least 2 independent qualified human graders
- AI grades the same samples
- compare with MAE and Pearson/Spearman
- add an agreement measure such as weighted Cohen's Kappa or ICC where appropriate
- report human–human agreement as the baseline

A target such as `85% of AI scores within ±0.5 band of the human reference` is a hypothesis/target to test, not a result to claim in advance.

## 9. Ownership boundary

### Gemini may deliver

- source candidate list + rights metadata proposal
- rubric structure
- chunking/embedding experiment proposal
- retrieval evaluation method
- prompt variants
- experimental protocol

### Gemini must not independently redefine

- React component API
- frontend response schema
- route structure
- design system
- Phase 1 scope

### Codex may implement now

Only Phase 0/1 tasks from the approved implementation plan. RAG remains mocked behind the application-level contract until its dedicated phase.
