# AI3-A — Practice Generator & Validation Core Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

Goal: Build the secure backend practice generation pipeline from authorized source ingestion through blueprint extraction, AI synthesis, and multi-layer deterministic & heuristic validation up to the `PENDING_REVIEW` state.

Architecture:
- Pipeline: Authorized Source Document -> Deterministic Normalization -> Abstract Blueprint Extraction -> Novel Practice Generation via `AiProviderRouter` -> Multi-Layer Validation Suite -> `PENDING_REVIEW` (PASS/WARN) or `NEEDS_REVISION` (FAIL).
- Rights Governance: Hard gating enforcing `RightsStatus.APPROVED` before any generation job can be created or executed.
- Novelty & Similarity: Configurable, versioned similarity policy (e.g. `similarity-policy-v1.0-conservative`) treating n-gram, cosine embedding distance, and entity overlap as engineering heuristics rather than legal non-infringement claims.
- Validation Suite: Strict deterministic precedence (Structural, Answer Key, Evidence Span, Ambiguity) with advisory AI Critic and heuristic CEFR/Band difficulty proxies.
- Rollout: Reading skill first; modular architecture with generic extension points for Writing, Listening, and Speaking.

Tech Stack:
- Backend: Java 21, Spring Boot 3.3.x, Spring Data JDBC / JdbcClient, Jackson JSON, Apache Tika, PostgreSQL + pgvector.
- AI Layer: `AiProviderRouter`, `AiProvider`, `AiChatCommand`, `AiChatResult`.
- Testing: JUnit 5, Mockito, AssertJ, Spring Boot Test.

Spec:
- `docs/superpowers/specs/2026-09-27-ai-phase-3-practice-generator-design.md` (Approved & Hardened at commit `3c77b9e`)

Global Constraints:
- Plan-only mode: No product code implementation or database migration executions in this planning phase.
- Migration versioning: Migration versions are NOT pinned in this plan; version numbers are assigned at execution time from a coordinated non-conflicting range.
- Rights enforcement: Generation from non-APPROVED sources must be strictly blocked at service and controller boundaries.
- Validation precedence: Advisory AI Critic cannot override deterministic validator failures (`FAIL` always moves state to `NEEDS_REVISION`).
- Provider safety: All unit/integration tests must use mock AI providers; no live paid provider calls.

Review Focus (Top 5 Highest-Risk Scenarios):
1. **Rights Enforcement Gate**: Ingested sources with `PENDING_REVIEW`, `RESTRICTED`, or `REJECTED` rights status are hard-blocked from initiating generation.
2. **Provider Robustness & Fallback**: Malformed JSON or provider timeouts are handled gracefully via fallback routing and transient retry without corrupting state.
3. **Deterministic Superiority over AI Critic**: Any deterministic validator failure (`Structural`, `AnswerKey`, `EvidenceSpan`, `Ambiguity`, `Similarity`) forces `NEEDS_REVISION` regardless of positive AI Critic remarks.
4. **Heuristic Similarity Governance**: Similarity metrics (n-gram, cosine distance, entity overlap) are versioned, configurable heuristics that never assert statutory copyright guarantees.
5. **Idempotency & Version Integrity**: Failed generation or retries never duplicate version numbers, overwrite historical candidate versions, or leave orphaned records.

---

### Task 1: Domain Entities, Enums & Repository Contracts

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/PracticeGenerationSource.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/PracticeGenerationBlueprint.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/PracticeGenerationJob.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/GeneratedPracticeSet.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/GeneratedPracticeVersion.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/GenerationValidationResult.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/PracticeReviewAction.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/GenerationState.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/domain/ValidationStatus.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/PracticeGenerationSourceRepository.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/PracticeGenerationBlueprintRepository.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/PracticeGenerationJobRepository.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/GeneratedPracticeSetRepository.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/JdbcPracticeGenerationRepository.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/repository/JdbcPracticeGenerationRepositoryTest.java`

Interfaces:
- Consumes: `com.ieltsaitutor.rag.domain.RightsStatus`, `com.ieltsaitutor.rag.domain.Skill`, `com.ieltsaitutor.auth.AuthUser`
- Produces: Type-safe persistence records for sources, blueprints, jobs, versions, and validation records.

Checklist:
- [ ] Step 1: Write failing test `JdbcPracticeGenerationRepositoryTest` testing CRUD operations and relations for sources, blueprints, jobs, sets, versions, and validation results.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=JdbcPracticeGenerationRepositoryTest`).
- [ ] Step 3: Implement domain records, state enums (`GenerationState`, `ValidationStatus`), repository interfaces, and `JdbcPracticeGenerationRepository` using Spring `JdbcClient`.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=JdbcPracticeGenerationRepositoryTest`).
- [ ] Step 5: Commit with message `feat(generator): add practice generator domain entities and repository contracts`.

---

### Task 2: Source Normalization & Rights Gating Service

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/SourceNormalizationService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/SourceRightsGuard.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/RegisterSourceRequest.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/RegisterSourceResponse.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/exception/SourceRightsException.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/exception/SourceNormalizationException.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/SourceNormalizationServiceTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/SourceRightsGuardTest.java`

Interfaces:
- Consumes: `DocumentChecksumService`, `TikaDocumentExtractor`, `RightsStatus`
- Produces: Sanitized normalized source entity with computed SHA-256 checksum and rights status validation.

Checklist:
- [ ] Step 1: Write failing unit tests for text normalization (HTML stripping, control character removal, word count boundaries) and rights gating (blocking `PENDING_REVIEW`, `RESTRICTED`, `REJECTED`).
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=SourceNormalizationServiceTest,SourceRightsGuardTest`).
- [ ] Step 3: Implement `SourceNormalizationService` (handling PDF/DOCX/text, word counts 400-1200 for reading) and `SourceRightsGuard` enforcing strict `RightsStatus.APPROVED` check.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=SourceNormalizationServiceTest,SourceRightsGuardTest`).
- [ ] Step 5: Commit with message `feat(generator): add source normalization and rights guard services`.

---

### Task 3: Blueprint Specification & Extraction Engine

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/BlueprintSchema.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/PassageStructureSpec.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/ItemDistributionSpec.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/TaskTypeDistribution.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/BlueprintExtractorService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/blueprint/DefaultBlueprintCatalog.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/blueprint/BlueprintExtractorServiceTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/blueprint/BlueprintSchemaTest.java`

Interfaces:
- Consumes: Normalized source text, target band score, target skill (`READING`)
- Produces: Abstract `BlueprintSchema` JSON containing structural requirements (word counts, paragraph counts, question distribution: TFNG, MCQ, Summary Completion, Matching Headings).

Checklist:
- [ ] Step 1: Write failing tests verifying structural schema serialization, validation of item distributions, and deterministic blueprint extraction from source text.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=BlueprintExtractorServiceTest,BlueprintSchemaTest`).
- [ ] Step 3: Implement `BlueprintSchema` records with JSON Jackson mappings, default template catalog (Academic Reading Passage 1/2/3), and `BlueprintExtractorService`.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=BlueprintExtractorServiceTest,BlueprintSchemaTest`).
- [ ] Step 5: Commit with message `feat(generator): implement blueprint schema and extraction engine`.

---

### Task 4: Reading Generator Prompts & AI Synthesis Adapters

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/ReadingPromptBuilder.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/PracticeSynthesisParser.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/RawPassagePayload.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/RawQuestionPayload.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/RawPracticePackage.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/ai/ReadingPracticeGeneratorService.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/ai/ReadingPromptBuilderTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/ai/PracticeSynthesisParserTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/ai/ReadingPracticeGeneratorServiceTest.java`

Interfaces:
- Consumes: `BlueprintSchema`, `AiProviderRouter`, `AiChatCommand`
- Produces: `RawPracticePackage` with novel generated passage paragraphs, questions, options, answer keys, verbatim evidence spans, and explanations.

Checklist:
- [ ] Step 1: Write failing unit tests testing structured JSON prompt formatting, JSON response parsing with repair for markdown code fences, and full provider delegation.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=ReadingPromptBuilderTest,PracticeSynthesisParserTest,ReadingPracticeGeneratorServiceTest`).
- [ ] Step 3: Implement `ReadingPromptBuilder`, robust `PracticeSynthesisParser` (with JSON repair for backticks/trailing commas), and `ReadingPracticeGeneratorService`.
- [ ] Step 4: Run and verify GREEN with mock AI provider (`./mvnw test -Dtest=ReadingPromptBuilderTest,PracticeSynthesisParserTest,ReadingPracticeGeneratorServiceTest`).
- [ ] Step 5: Commit with message `feat(generator): add reading practice prompt builder and AI synthesis service`.

---

### Task 5: Generation Job Orchestrator & State Machine

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/GenerationStateMachine.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/GenerationJobOrchestrator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/CreateJobRequest.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/JobStatusResponse.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/exception/InvalidGenerationStateException.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/GenerationStateMachineTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/GenerationJobOrchestratorTest.java`

Interfaces:
- Consumes: `SourceRightsGuard`, `ReadingPracticeGeneratorService`, `ValidationPipelineEngine`
- Produces: Lifecycle orchestration (`DRAFT` -> `GENERATING` -> `AUTO_VALIDATING` -> `PENDING_REVIEW` | `NEEDS_REVISION`), background asynchronous execution, and version creation.

Checklist:
- [ ] Step 1: Write failing tests verifying state transition invariants (forbidden transitions throw `InvalidGenerationStateException`), rights pre-check failure, and job lifecycle execution.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=GenerationStateMachineTest,GenerationJobOrchestratorTest`).
- [ ] Step 3: Implement `GenerationStateMachine` with strict transition matrix and `GenerationJobOrchestrator` managing async execution and error transitions.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=GenerationStateMachineTest,GenerationJobOrchestratorTest`).
- [ ] Step 5: Commit with message `feat(generator): implement generation job orchestrator and state machine`.

---

### Task 6: Deterministic Validation Suite: Structural, Answer Key & Evidence Validators

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/PracticeValidator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/ValidationReport.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/ValidationFinding.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/StructuralValidator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/AnswerKeyValidator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/EvidenceSpanValidator.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/StructuralValidatorTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/AnswerKeyValidatorTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/EvidenceSpanValidatorTest.java`

Interfaces:
- Consumes: `RawPracticePackage`, `BlueprintSchema`
- Produces: Deterministic `ValidationReport` with `PASS`/`FAIL` findings for schema conformance, item counts, unique single-key validity, and exact verbatim evidence span matching in passage.

Checklist:
- [ ] Step 1: Write failing tests for structural mismatches, invalid answer keys (e.g. empty key, option index out of bounds), and evidence spans missing verbatim from the passage.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=StructuralValidatorTest,AnswerKeyValidatorTest,EvidenceSpanValidatorTest`).
- [ ] Step 3: Implement `StructuralValidator`, `AnswerKeyValidator`, and `EvidenceSpanValidator` with normalized substring matching and detailed finding locations.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=StructuralValidatorTest,AnswerKeyValidatorTest,EvidenceSpanValidatorTest`).
- [ ] Step 5: Commit with message `feat(generator): implement structural, answer key, and evidence span validators`.

---

### Task 7: Ambiguity & Difficulty Heuristic Validators

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/AmbiguityValidator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/DifficultyValidator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/ReadabilityMetricsCalculator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/AcademicWordListIndex.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/AmbiguityValidatorTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/DifficultyValidatorTest.java`

Interfaces:
- Consumes: `RawPracticePackage`, target band score
- Produces: `ValidationReport` evaluating distractor distinctiveness, single-answer exclusivity, Flesch-Kincaid / Lexile reading grade levels, and Academic Word List (AWL) lexical density proxies.

Checklist:
- [ ] Step 1: Write failing tests verifying detection of duplicate or overlapping distractors, and calculation of Flesch-Kincaid readability indices and CEFR C1 proxy alignment.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=AmbiguityValidatorTest,DifficultyValidatorTest`).
- [ ] Step 3: Implement `AmbiguityValidator`, `ReadabilityMetricsCalculator`, `AcademicWordListIndex`, and `DifficultyValidator` (explicitly documenting metrics as heuristic approximations).
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=AmbiguityValidatorTest,DifficultyValidatorTest`).
- [ ] Step 5: Commit with message `feat(generator): add ambiguity and difficulty heuristic validators`.

---

### Task 8: Versioned Similarity & Novelty Safeguard Engine

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/SimilarityPolicy.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/SimilarityPolicyRegistry.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/NGramOverlapCalculator.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/ContiguousSequenceDetector.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/EntityRetentionFilter.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/similarity/SimilarityValidator.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/similarity/NGramOverlapCalculatorTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/similarity/SimilarityValidatorTest.java`

Interfaces:
- Consumes: `RawPracticePackage`, normalized source text, approved bank texts, `SimilarityPolicy`
- Produces: `ValidationReport` with stamped `policyVersion`, 4-gram overlap %, contiguous token run count, entity retention %, and classification (`PASS`, `WARNING`, `FAIL`).

Checklist:
- [ ] Step 1: Write failing tests verifying 4-gram overlap calculation, contiguous word run detection (>= 8 words), entity retention checks, and versioned policy switching (`similarity-policy-v1.0-conservative`).
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=NGramOverlapCalculatorTest,SimilarityValidatorTest`).
- [ ] Step 3: Implement `SimilarityPolicy`, `NGramOverlapCalculator`, `ContiguousSequenceDetector`, `EntityRetentionFilter`, and `SimilarityValidator` (framing all outputs as configurable heuristics).
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=NGramOverlapCalculatorTest,SimilarityValidatorTest`).
- [ ] Step 5: Commit with message `feat(generator): implement versioned similarity and novelty safeguard engine`.

---

### Task 9: Advisory AI Critic & Validation Aggregation Engine

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/critic/AiCriticService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/critic/CriticReviewReport.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/ValidationPipelineEngine.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/validator/AggregateValidationDecision.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/critic/AiCriticServiceTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/validator/ValidationPipelineEngineTest.java`

Interfaces:
- Consumes: List of `PracticeValidator` implementations (`Structural`, `AnswerKey`, `EvidenceSpan`, `Ambiguity`, `Similarity`, `Difficulty`), `AiCriticService`
- Produces: Consolidated `AggregateValidationDecision` (`PASS`, `WARNING`, `FAIL`) and detailed audit report. Strict invariant: AI Critic cannot overrule deterministic `FAIL`.

Checklist:
- [ ] Step 1: Write failing tests verifying that deterministic validator failures force `FAIL` even if AI Critic returns positive remarks, and that only all-PASS/WARN yields `PENDING_REVIEW`.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=AiCriticServiceTest,ValidationPipelineEngineTest`).
- [ ] Step 3: Implement `AiCriticService` (advisory prompts via `AiProviderRouter`) and `ValidationPipelineEngine` aggregating all validator findings.
- [ ] Step 4: Run and verify GREEN with mock AI providers (`./mvnw test -Dtest=AiCriticServiceTest,ValidationPipelineEngineTest`).
- [ ] Step 5: Commit with message `feat(generator): add advisory AI critic and validation aggregation engine`.

---

### Task 10: End-to-End Generation & Validation Integration Test Suite

Files:
- Create: `backend/src/test/java/com/ieltsaitutor/practice/generator/PracticeGeneratorIntegrationTest.java`
- Create: `backend/src/test/java/com/ieltsaitutor/practice/generator/fixtures/PracticeGeneratorFixtures.java`

Interfaces:
- Consumes: Full generation & validation pipeline with mock AI provider
- Produces: Comprehensive automated regression test verifying source registration -> blueprint derivation -> generation -> multi-layer validation -> state assignment to `PENDING_REVIEW` or `NEEDS_REVISION`.

Checklist:
- [ ] Step 1: Write integration tests covering happy path (approved source -> valid generated Reading set -> `PENDING_REVIEW`), rights rejection path (unapproved source -> `400 BAD_REQUEST`), and deterministic validation failure path (corrupted span -> `NEEDS_REVISION`).
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=PracticeGeneratorIntegrationTest`).
- [ ] Step 3: Wire full Spring context beans and verify end-to-end execution without errors.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=PracticeGeneratorIntegrationTest`).
- [ ] Step 5: Commit with message `test(generator): add end-to-end practice generator and validation integration test suite`.
