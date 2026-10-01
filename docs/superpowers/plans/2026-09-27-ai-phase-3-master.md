# AI Phase 3 — Practice Generator & Admin Approval Pipeline Master Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

Authoritative Base Commit: `d2e13fe` (Academic Luxury 2.0 HEAD)  
Approved & Hardened Spec Commit: `3c77b9ee98a5ff113bb68b151b194670d691e327`  
Spec Document: [`2026-09-27-ai-phase-3-practice-generator-design.md`](../specs/2026-09-27-ai-phase-3-practice-generator-design.md)  
Target Worktree: `C:\Users\haida\.codex\worktrees\ai-phase3-generator\ielts-ai-tutor`  
Target Branch: `feature/ai-phase3-generator`  

---

## 1. Executive Summary

AI Phase 3 implements an administrative generation, multi-layer validation, editorial review, and practice bank integration pipeline. It enables administrators to create novel, pedagogically sound, and legally safe IELTS-style practice materials while maintaining strict copyright governance, multi-layer quality controls, and a mandatory human approval gate.

The implementation is partitioned into exactly two sequential, independently reviewable execution plans:
1. **AI3-A: Generator & Validation Core** (10 tasks) — Backend source rights gating, text normalization, blueprint schemas, AI prompt synthesis, state machine orchestration, deterministic validation (structural, answer key, evidence span), heuristic ambiguity and difficulty checks, versioned similarity safeguards, and advisory AI Critic.
2. **AI3-B: Admin Review Workspace & Practice Bank Integration** (9 tasks) — Admin-only REST controllers, review actions (`APPROVE`, `REQUEST_REVISION`, `REJECT`), revision lifecycle, immutable audit trail, practice bank hydration, student API isolation, and the frontend Academic Luxury 2.0 Split-Screen Review Canvas.

Total Execution Tasks: **19 tasks**.

---

## 2. Core Architectural Boundaries

### 2.1 Mandatory Human Approval Gate
Generated practice materials follow an irreversible boundary:
```text
AUTHORIZED SOURCE -> ABSTRACT BLUEPRINT -> NOVEL PRACTICE SYNTHESIS -> MULTI-LAYER VALIDATION -> PENDING_REVIEW -> [HUMAN ADMIN APPROVAL] -> PRACTICE BANK
```
- Direct transitions `AI_GENERATED -> STUDENT/PUBLIC` are architecturally impossible.
- Unapproved items (`DRAFT`, `GENERATING`, `AUTO_VALIDATING`, `PENDING_REVIEW`, `NEEDS_REVISION`, `REJECTED`) exist solely in isolated admin records and are never returned by student catalog or practice APIs.

### 2.2 Clean AI Phase 2 Integration Boundary
- AI Phase 2 (Adaptive Learning Intelligence) and AI Phase 3 (Practice Generator) are completely decoupled.
- Phase 3 publishes only **APPROVED** content into the standard practice bank (`practice_sets`, `practice_passages`, `practice_paragraphs`, `practice_questions`).
- Phase 2 queries the approved practice bank by skill, question type, and difficulty without any internal dependency on Phase 3 generation prompts, source documents, or AI critic logs.
- Phase 3 has zero knowledge of learner weakness analytics or student state modeling.

### 2.3 Database Migration Coordination
- Migration version numbers are **NOT PINNED** inside these implementation plans (e.g. no hardcoded `V9`, `V10`).
- Exact migration version numbers are assigned at execution time from a coordinated, non-conflicting range to support safe parallel execution alongside AI Phase 2 development.

---

## 3. Plan Index & Task Breakdown

### Part A: Generator & Validation Core ([AI3-A Plan](./2026-09-27-ai3-a-generator-validation-core.md))
- **Task 1**: Domain Entities, Enums & Repository Contracts (Sources, Blueprints, Jobs, Versions, Validation Results, Review Actions)
- **Task 2**: Source Normalization & Rights Gating Service (`SourceRightsGuard` enforcing `RightsStatus.APPROVED`)
- **Task 3**: Blueprint Specification & Extraction Engine (`BlueprintSchema`, task distributions for Reading)
- **Task 4**: Reading Generator Prompts & AI Synthesis Adapters (`ReadingPracticeGeneratorService`, JSON repair parser)
- **Task 5**: Generation Job Orchestrator & State Machine (`GenerationJobOrchestrator`, `GenerationStateMachine`)
- **Task 6**: Deterministic Validation Suite: Structural, Answer Key & Evidence Validators
- **Task 7**: Ambiguity & Difficulty Heuristic Validators (CEFR/Band approximation proxies, AWL index)
- **Task 8**: Versioned Similarity & Novelty Safeguard Engine (`SimilarityPolicy`, n-gram overlap, contiguous run detector)
- **Task 9**: Advisory AI Critic & Validation Aggregation Engine (`AiCriticService`, `ValidationPipelineEngine`)
- **Task 10**: End-to-End Generation & Validation Integration Test Suite

### Part B: Admin Review & Practice Bank Integration ([AI3-B Plan](./2026-09-27-ai3-b-admin-review-bank-integration.md))
- **Task 1**: Admin REST Controllers & Authorization Gate (`AdminPracticeGeneratorController`, `AuthInterceptor` admin pattern)
- **Task 2**: Review Actions & Audit Trail Service (`PracticeReviewService`, `PracticeAuditService`)
- **Task 3**: Revision Lifecycle & Targeted Regeneration Engine (`PracticeRevisionService`, version diffing)
- **Task 4**: Practice Bank Hydration Service (`PracticeBankHydrationService`, mapping approved sets to database practice tables)
- **Task 5**: Student Boundary Enforcement & Catalog Federation (`SyntheticPracticeCatalog` DB federation)
- **Task 6**: Frontend Admin Generator API Client & State Hooks (`practiceGeneratorApi.js`, query hooks)
- **Task 7**: Frontend Admin Dashboard & Generation Wizard (`AdminPracticeGeneratorPage.jsx`, `NewGenerationWizardModal.jsx`)
- **Task 8**: Frontend Academic Luxury 2.0 Split-Screen Review Canvas (`PracticeReviewCanvas.jsx`, synchronized evidence pins)
- **Task 9**: End-to-End Admin Review & Bank Hydration Acceptance Suite

---

## 4. Verification & Acceptance Criteria

1. **Rights Enforcement**: Attempting to generate from any source with `rights_status != 'APPROVED'` fails immediately with HTTP 400.
2. **Deterministic Superiority**: Any deterministic validator failure flags `NEEDS_REVISION`; advisory AI Critic comments cannot overrule deterministic failures.
3. **Similarity Heuristic Governance**: All similarity metrics (n-gram, cosine distance, contiguous runs) are evaluated against active versioned policies and documented as non-statutory engineering heuristics.
4. **Zero Student Leakage**: Only `APPROVED` sets are queryable via `/api/practice/sets`; non-approved drafts are completely invisible to non-admin users.
5. **Practice Compatibility**: Approved sets run seamlessly in `PracticeService`, support deterministic scoring, and record student attempts identically to seed sets.
6. **Code Quality**: Clean build, zero compiler warnings, 100% test pass rate across frontend and backend suites, strictly honoring Academic Luxury 2.0 design tokens.
