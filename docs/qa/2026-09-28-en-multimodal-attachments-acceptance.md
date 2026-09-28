# Én Multimodal Attachments — Acceptance Record

Date: 2026-09-28
Branch: `feature/ai-phase2-phase3-integration`
Scope: approved Én multimodal attachments plan only. No push, merge, deploy, or legacy/shared database remediation was performed.

## Scope map

- PASS — Tasks 1–19 cover supported formats, five-file/10 MiB limits, ID-only upload/chat separation, private storage, lifecycle, provenance, vision, scanned-PDF fallback, mixed turns, history, retry, ownership, conversation isolation, and frontend READY gating.
- PASS — No V1–V29 migration file was edited. V30 is additive. Existing admin RAG corpus, auth model, and text-only Tutor semantics were not changed.
- PASS — Automated provider tests use mocks and did not call live AI providers.

## Automated verification

- PASS — Frontend: `npm test -- --run`; 75 test files, 424 tests passed.
- PASS — Frontend lint: `npm run lint` exited 0; existing non-blocking warnings remain.
- PASS — Frontend production build: `npm run build` exited 0.
- BLOCKED — Backend `cmd /c .\\mvnw.cmd test`: 495 tests, 0 failures, 5 errors, 1 skipped. The 5 errors are `AuthApplicationStartupTest` (1) and `AcademicLuxuryCompatibilityTest` (4), all caused by Flyway validation against legacy `ielts_ai_tutor` where resolved V9 and V10 are not applied. `RagPostgresIntegrationTest` has one expected Testcontainers skip.
- BLOCKED — Backend `cmd /c .\\mvnw.cmd package`: test phase stops on the same five legacy-database context errors. `cmd /c .\\mvnw.cmd -DskipTests package` passed after the project backend process holding the target JAR was stopped.
- PASS — Focused startup regression after the Task 20 wiring fix: `ApplicationStartupWithoutOptionalProvidersTest`, 4/4 passed.
- PASS — `git diff --check`.

## Database and storage

- PASS — Fresh authorized QA database: `ielts_ai_en_multimodal_qa_20260928`.
- PASS — Spring startup validated and applied 13 migrations in order through v30: V1–V10, V28, V29, V30.
- PASS — `ai_attachments`, `ai_attachment_chunks`, and `ai_message_attachments` exist.
- PASS — Required attachment primary, ownership/conversation/status, storage, chunk, embedding-space, and message-ordinal indexes exist.
- PASS — `ai_attachment_chunks.embedding` is PostgreSQL `vector`; embedding provider/model/dimension/version metadata columns are present. The application contract enforces dimension 768.
- PASS — Private storage keys are generated from owner/conversation/attachment UUIDs; raw filesystem paths and provider errors are not exposed by the attachment API.
- PASS — Cleanup/reference behavior is covered by attachment lifecycle and repository tests. No legacy/shared database was modified.

## Attachment and Tutor acceptance

- PASS — Automated validation/API coverage verifies five-file batches, sixth-file rejection, 10 MiB rejection, all supported PDF/DOCX/TXT/PNG/JPG/JPEG/WEBP formats, and safe unsupported-type errors.
- PASS — Automated extraction, chunking, retrieval, context-budget, provenance, and grounded Tutor tests cover unique document facts and source citations.
- PASS — Real image bytes are routed through the provider-neutral vision capability boundary; Gemini payload tests verify inline image data without vendor-specific frontend coupling.
- PASS — Mixed image/document capability routing, attachment IDs, ordered history links, and source metadata are covered by backend and frontend regression suites.
- PASS — Scanned PDF fallback is page-bounded, batched, and reports remaining pages truthfully in focused tests.
- PASS — Unsupported source-dependent questions return insufficient evidence without provider calls in focused orchestration tests.
- PASS — Queue retry reuses only the failed file; chat retry reuses attachment IDs. Frontend queue tests verify bounded concurrency and polling.
- PASS — Reload/history metadata and source-chip contracts are covered without hydrating binary bodies.
- NOT RUN — Full live Gemini vision/document acceptance was not run to avoid external provider quota/network calls; mocked provider coverage passed.

## Security and runtime hygiene

- PASS — Ownership and conversation isolation are covered by backend security tests; foreign attachment UUIDs are rejected.
- PASS — Removed, expired, non-READY, and wrong-conversation attachments are rejected before provider routing.
- NOT CAPTURED — Browser console/network and exact 375/768/1024/1440 attachment runtime screenshots were not captured in this verification run.
- PASS — Frontend automated accessibility and responsive component regressions passed; no attachment-specific overflow regression was reported.

## Reproducible defects fixed in Task 20

- PASS — `FileSystemTutorAttachmentStorage` now explicitly selects its property-injected Spring constructor when a test-only `Path` constructor is also present.
- PASS — `TutorAttachmentRetrievalService` now derives its configured embedding space from the existing primary `EmbeddingProvider`; it no longer requires a non-bean `EmbeddingSpaceSelector`.
- PASS — Floating Tutor composer input receives focus on dialog mount, preserving open/close focus behavior; Tutor regression is 13/13 and the full frontend suite is green.

## Known blockers

- The default/shared `ielts_ai_tutor` database has an existing Flyway branch-order anomaly: V9 and V10 are resolved in source but not applied there. It was inspected only; no repair, reset, out-of-order migration, or history edit was performed.
- Testcontainers PostgreSQL integration remains skipped because the Docker Npipe client returned HTTP 400 with an empty Docker server response; no dependency or Docker configuration change was attempted.
- Live provider/runtime browser acceptance remains not run in this automated pass.

## Final repository state

- Task 20 acceptance record is ready to commit with the focused wiring and Tutor focus fixes.
- Push: not performed.
- Merge: not performed.
- Deploy: not performed.
