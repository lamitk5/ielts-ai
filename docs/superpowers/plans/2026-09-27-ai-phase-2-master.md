# AI Phase 2 — Adaptive Learning Master Execution Plan

> This is an execution map, not a third implementation plan. The detailed TDD contracts live in the two phase plans linked below.

## Goal

Deliver the approved AI Phase 2 adaptive learning experience on top of the Academic Luxury 2.0 product base, with deterministic learning intelligence first and a trusted, bounded Tutor memory layer second.

## Authority and Base

- Product base: `d2e13fe` (Academic Luxury 2.0 completed product HEAD).
- Approved design authority: `docs/superpowers/specs/2026-09-26-ai-phase-2-adaptive-learning-design.md`, approved commit `6f29720`.
- Execution worktree: `C:\Users\haida\.codex\worktrees\ai-phase2-adaptive\ielts-ai-tutor`.
- Branch: `feature/ai-phase2-adaptive`.
- Existing V1–V8 migrations, RAG `vector(768)`, current provider router, practice contracts, auth/session behavior, and Academic Luxury UI are preserved.

## Plans and Task Counts

1. `docs/superpowers/plans/2026-09-27-ai2-a-adaptive-learning-core.md` — 8 TDD tasks.
2. `docs/superpowers/plans/2026-09-27-ai2-b-tutor-memory-integration.md` — 8 TDD tasks.

Total: 16 independently reviewable implementation tasks. Each task has exact current integration paths, a RED test command and expected failure, minimal implementation scope, a GREEN command, and a focused commit.

## Execution Order

```text
AI2-A Task 1 domain/schema boundary
  -> Task 2 event ledger/idempotency
  -> Task 3 deterministic evidence
  -> Task 4 profile/issue/trend
  -> Task 5 roadmap
  -> Task 6 APIs/security
  -> Task 7 source integration
  -> Task 8 full AI2-A regression
       |
       v
AI2-B Task 1 conversation persistence
  -> Task 2 bounded memory
  -> Task 3 trusted adaptive tools
  -> Task 4 orchestration/scope/RAG
  -> Task 5 proactive policy
  -> Task 6 dashboard
  -> Task 7 Tutor lifecycle
  -> Task 8 full AI2-B regression
```

AI2-B may begin design review while AI2-A is under review, but implementation must consume the tested AI2-A contracts rather than duplicate learning logic. Each task follows: RED test → verify the stated failure → minimal implementation → GREEN → implementer self-review → independent review → fix Important/Critical findings → commit.

## Cross-Plan Integration Boundaries

- `LearningEventIngestionService` is the only normalized evidence-ingestion boundary; practice, writing, speaking, and roadmap completion publish into it.
- AI2-A learning APIs are server-owned and provider-neutral; AI2-B `AdaptiveTutorContextResolver` consumes them and never reconstructs profiles in the Tutor layer.
- `TutorOrchestrator` remains the single route for chat. Deterministic tools and scope policy precede RAG and provider calls.
- `AiChatRequest.conversationId` is additive and nullable; existing clients continue to work.
- React receives normalized profile, issue, roadmap, grounding, citation, loading, and error states only.
- Guest state is ephemeral; authenticated memory is user-scoped; logout/account switch clears client state and never merges identities.
- Phase 3 starts later at an approved practice-bank adapter/service boundary. No generator classes or generated tests are introduced here.

## Shared Verification Gates

- Backend unit/repository/service/controller/security/integration tests mock providers and use zero live quota.
- Frontend component/route/accessibility tests cover member, guest, loading, empty, insufficient evidence, fallback, timeout, retry, reduced motion, mobile, and account switch.
- Fresh and existing database verification uses the merged migration directory. Migration versions are reserved for parallel execution coordination and must not be hardcoded in either plan.
- Before completion: backend tests and package, frontend tests/lint/build, `git diff --check`, security review, no secrets, no raw provider payloads, no fake learning data, and a clean worktree.

## Explicit Non-Goals

- No Phase 3 practice generator or generated question bank.
- No real STT/audio pronunciation pipeline.
- No official IELTS scoring or claims.
- No provider replacement, provider-specific frontend contract, or live API quota in automated verification.
- No push, merge, deployment, or architecture redesign.
