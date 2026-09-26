# AI3-B — Admin Review Workspace & Practice Bank Integration Implementation Plan

> For agentic workers:
> REQUIRED SUB-SKILL:
> superpowers:executing-plans or
> superpowers:subagent-driven-development

Goal: Implement the administrative review workspace, approval workflow, revision lifecycle, and practice bank hydration pipeline, taking validated practice drafts through mandatory human editorial review into the student-facing practice bank.

Architecture:
- Security & Admin Routing: REST controllers under `/api/admin/practice-generator/**` strictly gated by `UserRole.ADMIN` and `AuthInterceptor`.
- Human Review & Audit: Review actions (`APPROVE`, `REQUEST_REVISION`, `REJECT`) create immutable audit records in `practice_review_actions` and enforce explicit state machine transitions.
- Practice Bank Hydration: `PracticeBankHydrationService` maps approved sets to PostgreSQL practice tables (`practice_sets`, `practice_passages`, `practice_paragraphs`, `practice_questions`, `practice_provenance_meta`) for immediate consumption by `PracticeService`.
- Student API Isolation: Zero leakage of unapproved (`DRAFT`, `GENERATING`, `AUTO_VALIDATING`, `PENDING_REVIEW`, `NEEDS_REVISION`, `REJECTED`) items to student endpoints or catalogs.
- Frontend: Academic Luxury 2.0 Split-Screen Review Canvas with interactive passage/question linking, evidence highlight pins, similarity/validator callouts, and version diffing.

Tech Stack:
- Backend: Java 21, Spring Boot 3.3.x, Spring Data JDBC, Jackson JSON, PostgreSQL.
- Frontend: React 18, Vite, Tailwind CSS, Framer Motion, Lucide React, Academic Luxury 2.0 UI components (`GlassCard`, `Button`, `Surface`, `PageContainer`).
- Testing: JUnit 5, Mockito, Spring Boot MockMvc, Vitest, React Testing Library.

Spec:
- `docs/superpowers/specs/2026-09-27-ai-phase-3-practice-generator-design.md` (Approved & Hardened at commit `3c77b9e`)

Global Constraints:
- Plan-only mode: No product code implementation or database migration executions in this planning phase.
- Migration versioning: Migration versions are NOT pinned in this plan; version numbers are assigned at execution time from a coordinated non-conflicting range.
- Human approval invariant: Content cannot transition to `APPROVED` or hydrate the practice bank without an explicit human administrator action.
- Phase 2 boundary: AI Phase 3 publishes only approved content into the practice bank; Phase 3 has no dependency on adaptive learner modeling internals.
- UI styling: Must reuse Academic Luxury 2.0 design tokens and primitives from `d2e13fe` without creating redundant components.

Review Focus (Top 5 Highest-Risk Scenarios):
1. **Admin Role & Route Authorization**: Non-admin users and unauthenticated requests are strictly rejected with 403 Forbidden / 401 Unauthorized across all review routes.
2. **Student API Isolation & Zero Leakage**: Practice drafts in `PENDING_REVIEW`, `NEEDS_REVISION`, `DRAFT`, or `REJECTED` states are completely excluded from student practice queries and attempt submission endpoints.
3. **State Transition Validity**: Illegal transitions (e.g. attempting to approve a `DRAFT` or re-approving an already `APPROVED` set) throw explicit domain errors with 409 Conflict / 400 Bad Request.
4. **Version History & Audit Immutability**: Requesting revisions preserves previous version records in `generated_practice_versions` and creates immutable entries in `practice_review_actions`.
5. **Idempotent Bank Hydration**: Approving a practice set creates or updates the student practice bank records exactly once without duplicating question items, corrupting paragraph indices, or creating orphaned records.

---

### Task 1: Admin REST Controllers & Authorization Gate

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/controller/AdminPracticeGeneratorController.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/controller/AdminPracticeReviewController.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/GeneratedSetReviewPayload.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/ReviewActionRequest.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/ReviewActionResponse.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/auth/AuthInterceptor.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/auth/AuthWebMvcConfig.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/controller/AdminPracticeGeneratorControllerTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/controller/AdminPracticeReviewControllerSecurityTest.java`

Interfaces:
- Consumes: `AuthPrincipal`, `UserRole.ADMIN`, `GenerationJobOrchestrator`, `PracticeReviewService`
- Produces: REST endpoints for `/api/admin/practice-generator/sources`, `/blueprints/extract`, `/jobs`, `/sets/{setId}`, and `/sets/{setId}/review` with strict admin role verification.

Checklist:
- [ ] Step 1: Write failing security and MVC tests verifying that non-admin requests receive 403 Forbidden, unauthenticated requests receive 401 Unauthorized, and admin requests succeed.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=AdminPracticeGeneratorControllerTest,AdminPracticeReviewControllerSecurityTest`).
- [ ] Step 3: Implement `AdminPracticeGeneratorController`, `AdminPracticeReviewController`, and register path pattern `/api/admin/practice-generator/**` in `AuthInterceptor` and `AuthWebMvcConfig`.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=AdminPracticeGeneratorControllerTest,AdminPracticeReviewControllerSecurityTest`).
- [ ] Step 5: Commit with message `feat(generator): implement admin practice generator controllers and security gate`.

---

### Task 2: Review Actions & Audit Trail Service

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/PracticeReviewService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/PracticeAuditService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/PracticeReviewSummary.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/exception/InvalidReviewActionException.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/PracticeReviewServiceTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/PracticeAuditServiceTest.java`

Interfaces:
- Consumes: `GeneratedPracticeSetRepository`, `PracticeReviewAction`, `PracticeBankHydrationService`
- Produces: Handles review actions (`APPROVE`, `REQUEST_REVISION`, `REJECT`), verifies state preconditions, updates practice set state, and records immutable audit log.

Checklist:
- [ ] Step 1: Write failing unit tests for state verification on review actions (rejecting approval when state is `NEEDS_REVISION` or `GENERATING`, recording audit entries with timestamp and admin ID).
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=PracticeReviewServiceTest,PracticeAuditServiceTest`).
- [ ] Step 3: Implement `PracticeReviewService` and `PracticeAuditService` ensuring full transactional integrity for review state transitions.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=PracticeReviewServiceTest,PracticeAuditServiceTest`).
- [ ] Step 5: Commit with message `feat(generator): add practice review and audit logging services`.

---

### Task 3: Revision Lifecycle & Targeted Regeneration Engine

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/PracticeRevisionService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/RegenerateItemRequest.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/ManualEditSetRequest.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/dto/VersionComparisonDto.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/PracticeRevisionServiceTest.java`

Interfaces:
- Consumes: `GeneratedPracticeVersion`, `ReadingPracticeGeneratorService`, `ValidationPipelineEngine`
- Produces: Creates new immutable version `version_number = N + 1`, supports targeted question re-synthesis and manual editorial adjustments, and re-triggers validation.

Checklist:
- [ ] Step 1: Write failing tests for version incrementation, preserving previous version snapshots, re-running validators after item-level regeneration, and generating diff summaries.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=PracticeRevisionServiceTest`).
- [ ] Step 3: Implement `PracticeRevisionService` with targeted question regeneration and manual edit validation workflows.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=PracticeRevisionServiceTest`).
- [ ] Step 5: Commit with message `feat(generator): implement revision lifecycle and targeted regeneration engine`.

---

### Task 4: Practice Bank Hydration Service

Files:
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/PracticeBankHydrationService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/service/PracticeProvenanceRecord.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/PracticeProvenanceRepository.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/generator/repository/JdbcPracticeProvenanceRepository.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/generator/service/PracticeBankHydrationServiceTest.java`

Interfaces:
- Consumes: `GeneratedPracticeSet`, `GeneratedPracticeVersion`, `PracticeSet`, `PracticePassage`, `PracticeParagraph`, `PracticeQuestion`
- Produces: Hydrates approved generated sets into database practice tables with generated provenance metadata, ensuring idempotent execution.

Checklist:
- [ ] Step 1: Write failing unit tests verifying mapping from `GeneratedPracticeVersion` to `PracticeSet`, `PracticePassage`, `PracticeParagraph`, and `PracticeQuestion` records, and verifying provenance insertion.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=PracticeBankHydrationServiceTest`).
- [ ] Step 3: Implement `PracticeBankHydrationService` and `JdbcPracticeProvenanceRepository` with transactional upsert operations.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=PracticeBankHydrationServiceTest`).
- [ ] Step 5: Commit with message `feat(generator): implement practice bank hydration and provenance tracking`.

---

### Task 5: Student Boundary Enforcement & Catalog Federation

Files:
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/SyntheticPracticeCatalog.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/practice/PracticeService.java`
- Create: `backend/src/main/java/com/ieltsaitutor/practice/repository/DatabasePracticeCatalogStore.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/PracticeServiceStudentBoundaryTest.java`
- Test: `backend/src/test/java/com/ieltsaitutor/practice/SyntheticPracticeCatalogFederationTest.java`

Interfaces:
- Consumes: `SyntheticPracticeCatalog` default seeds + `DatabasePracticeCatalogStore`
- Produces: Combined student practice catalog that returns both hardcoded seed sets and dynamically approved generated sets, while strictly excluding non-approved drafts.

Checklist:
- [ ] Step 1: Write failing tests verifying that `PracticeService.sets("reading")` returns only approved sets, and that querying unapproved set IDs throws not-found exception.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=PracticeServiceStudentBoundaryTest,SyntheticPracticeCatalogFederationTest`).
- [ ] Step 3: Enhance `SyntheticPracticeCatalog` to query `DatabasePracticeCatalogStore` for approved sets while preserving synthetic fallbacks and deterministic scoring.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=PracticeServiceStudentBoundaryTest,SyntheticPracticeCatalogFederationTest`).
- [ ] Step 5: Commit with message `feat(practice): federate practice catalog with database approved bank and student boundary`.

---

### Task 6: Frontend Admin Generator API Client & State Hooks

Files:
- Create: `frontend/src/features/practice-generator/practiceGeneratorApi.js`
- Create: `frontend/src/features/practice-generator/usePracticeGeneratorJobs.js`
- Create: `frontend/src/features/practice-generator/usePracticeReviewSet.js`
- Create: `frontend/src/features/practice-generator/generatorStateConstants.js`
- Test: `frontend/src/__tests__/practiceGeneratorApi.test.js`

Interfaces:
- Consumes: Admin REST endpoints (`/api/admin/practice-generator/**`) with auth headers
- Produces: Type-safe JS API functions and React state hooks for jobs listing, source registration, blueprint extraction, draft retrieval, and review action dispatch.

Checklist:
- [ ] Step 1: Write failing Vitest tests for `practiceGeneratorApi.js` verifying request parameter formatting, error parsing, and auth token propagation.
- [ ] Step 2: Run and verify RED (`npm test -- src/__tests__/practiceGeneratorApi.test.js --run`).
- [ ] Step 3: Implement `practiceGeneratorApi.js`, `generatorStateConstants.js`, and React query/state hooks.
- [ ] Step 4: Run and verify GREEN (`npm test -- src/__tests__/practiceGeneratorApi.test.js --run`).
- [ ] Step 5: Commit with message `feat(frontend): add admin practice generator api client and state hooks`.

---

### Task 7: Frontend Admin Dashboard & Generation Wizard

Files:
- Create: `frontend/src/pages/AdminPracticeGeneratorPage.jsx`
- Create: `frontend/src/components/admin/generator/GenerationJobTable.jsx`
- Create: `frontend/src/components/admin/generator/NewGenerationWizardModal.jsx`
- Create: `frontend/src/components/admin/generator/SourceRegistrationTab.jsx`
- Create: `frontend/src/components/admin/generator/BlueprintSelectionTab.jsx`
- Modify: `frontend/src/App.jsx`
- Test: `frontend/src/__tests__/AdminPracticeGeneratorPage.test.jsx`
- Test: `frontend/src/__tests__/NewGenerationWizardModal.test.jsx`

Interfaces:
- Consumes: `usePracticeGeneratorJobs`, Academic Luxury 2.0 UI components (`PageContainer`, `GlassCard`, `Button`, `Surface`)
- Produces: `/admin/practice-generator` dashboard view with KPI counters, filterable jobs list, and 3-step generation creation wizard with rights verification warnings.

Checklist:
- [ ] Step 1: Write failing component tests for `AdminPracticeGeneratorPage` and `NewGenerationWizardModal` verifying job rendering, status badges, and source rights pre-validation.
- [ ] Step 2: Run and verify RED (`npm test -- src/__tests__/AdminPracticeGeneratorPage.test.jsx --run`).
- [ ] Step 3: Implement `AdminPracticeGeneratorPage`, `GenerationJobTable`, `NewGenerationWizardModal`, and register route `/admin/practice-generator` in `App.jsx`.
- [ ] Step 4: Run and verify GREEN (`npm test -- src/__tests__/AdminPracticeGeneratorPage.test.jsx src/__tests__/NewGenerationWizardModal.test.jsx --run`).
- [ ] Step 5: Commit with message `feat(frontend): implement admin practice generator dashboard and wizard`.

---

### Task 8: Frontend Academic Luxury 2.0 Split-Screen Review Canvas

Files:
- Create: `frontend/src/pages/AdminPracticeReviewPage.jsx`
- Create: `frontend/src/components/admin/review/PracticeReviewCanvas.jsx`
- Create: `frontend/src/components/admin/review/PassageReviewPane.jsx`
- Create: `frontend/src/components/admin/review/QuestionReviewPane.jsx`
- Create: `frontend/src/components/admin/review/ValidationReportDrawer.jsx`
- Create: `frontend/src/components/admin/review/SimilarityInspectorBanner.jsx`
- Create: `frontend/src/components/admin/review/VersionDiffModal.jsx`
- Create: `frontend/src/components/admin/review/ReviewActionToolbar.jsx`
- Modify: `frontend/src/App.jsx`
- Test: `frontend/src/__tests__/AdminPracticeReviewPage.test.jsx`
- Test: `frontend/src/__tests__/PracticeReviewCanvas.test.jsx`

Interfaces:
- Consumes: `usePracticeReviewSet`, `practiceGeneratorApi`, Academic Luxury 2.0 tokens
- Produces: `/admin/practice-generator/sets/:setId` review workspace featuring side-by-side passage and question inspection, synchronized evidence highlight pins, similarity warning callouts, validation breakdown drawer, version diffing, and review action modal (`Approve`, `Request Revision`, `Reject`).

Checklist:
- [ ] Step 1: Write failing component tests for `AdminPracticeReviewPage` and `PracticeReviewCanvas` verifying synchronized hover highlighting, validation findings display, and review action dispatch.
- [ ] Step 2: Run and verify RED (`npm test -- src/__tests__/AdminPracticeReviewPage.test.jsx --run`).
- [ ] Step 3: Implement `AdminPracticeReviewPage`, `PracticeReviewCanvas`, `PassageReviewPane`, `QuestionReviewPane`, `ValidationReportDrawer`, `SimilarityInspectorBanner`, and register route in `App.jsx`.
- [ ] Step 4: Run and verify GREEN (`npm test -- src/__tests__/AdminPracticeReviewPage.test.jsx src/__tests__/PracticeReviewCanvas.test.jsx --run`).
- [ ] Step 5: Commit with message `feat(frontend): implement academic luxury split-screen practice review canvas`.

---

### Task 9: End-to-End Admin Review & Bank Hydration Acceptance Suite

Files:
- Create: `backend/src/test/java/com/ieltsaitutor/practice/generator/AdminReviewAndBankHydrationAcceptanceTest.java`
- Create: `frontend/src/__tests__/AdminGeneratorFlowIntegration.test.jsx`

Interfaces:
- Consumes: Complete backend and frontend practice generation & review workflow
- Produces: Full end-to-end regression and acceptance tests verifying:
  1. Source registration with rights check
  2. Blueprint extraction & generation job completion
  3. Review payload inspection with similarity/difficulty heuristics
  4. Admin approval action execution
  5. Immediate practice bank hydration
  6. Successful student practice attempt submission on the approved set.

Checklist:
- [ ] Step 1: Write end-to-end integration tests verifying the entire lifecycle from source ingestion to student attempt submission.
- [ ] Step 2: Run and verify RED (`./mvnw test -Dtest=AdminReviewAndBankHydrationAcceptanceTest`).
- [ ] Step 3: Wire all components and execute end-to-end verification.
- [ ] Step 4: Run and verify GREEN (`./mvnw test -Dtest=AdminReviewAndBankHydrationAcceptanceTest` and `npm test -- src/__tests__/AdminGeneratorFlowIntegration.test.jsx --run`).
- [ ] Step 5: Commit with message `test(generator): add end-to-end admin review and bank hydration acceptance test suite`.
