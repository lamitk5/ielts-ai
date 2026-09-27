# Én Multimodal Attachments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement authenticated Én chat attachments for up to five real PDF, DOCX, TXT, PNG, JPG, JPEG, or WEBP files with durable ownership-scoped processing, real document/vision understanding, retry-safe chat, and truthful provenance.

**Architecture:** Extend the existing TutorAttachmentController, TutorAttachmentService,
ConversationService, TutorOrchestrator, RAG extraction/embedding boundaries, and
provider-neutral AiProviderRouter instead of creating parallel stacks. Store private
attachments in a separate controlled filesystem root with additive PostgreSQL
metadata/chunk/message-link tables, and send only READY attachment IDs through the
chat HTTP contract. The frontend uses an independent queue with at most two
concurrent uploads and keeps existing Én, Tutor, and text-only behavior intact.

**Tech Stack:** React 19 + Vite 8 + Vitest + Testing Library + oxlint; Spring Boot
3 + Spring MVC/WebFlux + Spring JDBC + PostgreSQL/pgvector + Flyway + Apache Tika
3.2.2; existing Gemini/Cloudflare/Groq provider adapters; Apache PDFBox 3.0.5
already resolved through the current Tika dependency graph for bounded PDF page
rendering.

**Spec:** docs/superpowers/specs/2026-09-28-en-multimodal-attachments-design.md

## Global Constraints

- Maximum five attachments per chat turn.
- Maximum 10 MiB per file.
- Supported formats are PDF, DOCX, TXT, PNG, JPG, JPEG, and WEBP.
- Image questions require a verified real vision-capable provider.
- Document questions use actual extracted PDF, DOCX, or TXT content.
- Mixed image + document turns preserve each file's source identity.
- Upload and chat are separate operations.
- Chat sends attachmentIds, not raw file blobs.
- Retrying chat reuses READY attachment IDs and never uploads again.
- Backend validation is authoritative; browser validation is advisory UX.
- Private attachment data never enters the approved admin RAG corpus.
- Retrieval is scoped by authenticated user, conversation, selected attachment IDs, READY state, and exact embedding space.
- Cloudflare and Gemini embeddings must never be mixed in one vector space.
- The current vector dimension is 768 with provider/model/dimension/version metadata.
- READY means the server completed preparation; it does not mean the model already inspected the file.
- Provider failure during chat must not mark a READY attachment FAILED.
- Scanned PDFs use text extraction first and bounded vision fallback, not blanket OCR.
- Scanned PDF processing is capped at 20 rendered pages per user operation.
- Attachment-derived context defaults to approximately 12,000 input tokens and must not be silently truncated.
- Frontend upload concurrency is at most two jobs.
- Ownership and conversation isolation are enforced from AuthPrincipal and server-side conversation scope.
- New storage uses generated keys below an application-controlled root; original filenames are never filesystem paths.
- Existing text-only Én behavior, provider-neutral response fields, auth, practice, RAG governance, and settings remain intact.
- New database work is additive only; historical Flyway migrations V1 through V29 are never edited, reset, or remediated.
- Automated tests mock external providers and consume zero live provider quota.
- No push, merge, deploy, or legacy/shared database modification is part of implementation.

## Review Focus

1. A valid-looking file with an extension/MIME/signature mismatch must be rejected before it can become READY; pin this in Task 3 with PDF, DOCX, PNG, and mismatched-magic tests.
2. A valid attachment UUID from another user or conversation must never be usable or enumerable; pin this in Task 5 and Task 19 with owned, foreign, guessed, REMOVED, and EXPIRED cases.
3. A required image turn must never fall through to a text-only provider or fabricate a vision answer; pin this in Task 10 and Task 11 with capability-routing and provider-payload assertions.
4. Retrying chat after a provider/network failure must reuse the same attachment IDs and make zero upload calls; pin this in Task 17 and Task 18 with upload-call counters.
5. A stale PROCESSING attachment must recover deterministically without an infinite retry loop or deleting valid sibling files; pin this in Task 6 and Task 20 with timeout, bounded-retry, and five-file recovery tests.

## Task 1: Attachment domain contract and additive schema

**Files:**
- Create: backend/src/main/resources/db/migration/V30__create_tutor_attachment_schema.sql
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentSchemaContractTest.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/AttachmentKind.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachment.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContract.java
- Modify: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java

**Interfaces:**
- Consumes: Current TutorAttachment record, TutorAttachmentContract allowlist, ai_conversations, ai_messages, app_users, and RAG vector(768) conventions.
- Produces: AttachmentKind { DOCUMENT, IMAGE }; expanded TutorAttachment metadata; AttachmentStatus { STORED, PROCESSING, READY, FAILED, REMOVED, EXPIRED } with compatibility mapping for current UPLOADED and IMAGE_READY; V30 tables ai_attachments, ai_attachment_chunks, and ai_message_attachments.

- [ ] Step 1: Write the failing schema/domain tests. Add tests named schemaDefinesPrivateAttachmentTables, schemaDefinesOwnerConversationStatusIndexes, schemaPreservesVectorMetadata, schemaDoesNotEditHistoricalMigrations, and attachmentRecordCarriesConversationAndStorageMetadata. Assert the migration contains all required columns, vector(768), owner/conversation/status indexes, message-link ordinal, and no edits to V1 through V29.
- [ ] Step 2: Run the focused test to verify RED. Run from backend: cmd /c .\mvnw.cmd -Dtest=TutorAttachmentSchemaContractTest test. Expected: FAIL because V30, AttachmentKind, and the new record fields do not exist.
- [ ] Step 3: Implement the additive domain and schema. Keep the current Java package and service names. Extend TutorAttachment with id, userId, conversationId, filename, sanitizedFilename, contentType, kind, sizeBytes, sha256, storageKey, status, processingErrorCode, processingStartedAt, processingAttempts, createdAt, updatedAt, and expiresAt. Add V30 without changing V1 through V29; add foreign keys, CHECK constraints, ownership/status indexes, exact-space indexes, and safe message-link constraints.
- [ ] Step 4: Run the focused test to verify GREEN. Run the same Maven command and expect all schema/domain assertions to pass.
- [ ] Step 5: Run nearby attachment regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentControllerTest,AcademicLuxuryCompatibilityTest test. Existing one-file compatibility assertions must remain green while new states are available.
- [ ] Step 6: Commit. Commit feat(attachments): add durable attachment domain and schema.

## Task 2: Controlled attachment storage

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentStorage.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/FileSystemTutorAttachmentStorage.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/FileSystemTutorAttachmentStorageTest.java
- Modify: backend/src/main/resources/application.properties
- Modify: backend/.env.example

**Interfaces:**
- Consumes: generated attachment storage keys and application configuration.
- Produces: TutorAttachmentStorage.store(InputStream input, String storageKey, long sizeBytes), open(String storageKey), delete(String storageKey), and exists(String storageKey).

- [ ] Step 1: Write the failing storage tests. Add usesGeneratedKeyBelowConfiguredRoot, rejectsTraversalKey, opensStoredBytes, deletesStoredBytes, and neverUsesOriginalFilenameAsPath. Assert the resolved path remains below a temporary configured root and raw paths are never returned by the API model.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=FileSystemTutorAttachmentStorageTest test. Expected: FAIL because the abstraction and implementation do not exist.
- [ ] Step 3: Implement the storage boundary. Add a normalized TUTOR_ATTACHMENT_STORAGE_ROOT configuration with default backend/data/tutor-attachments. Generate UUID-based keys in the service, resolve only normalized keys, stream writes, and use the same root check for open/delete. Keep the root separate from RAG_STORAGE_ROOT; do not add S3 or public file serving.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect all storage safety assertions to pass.
- [ ] Step 5: Run storage/RAG regressions. Run cmd /c .\mvnw.cmd -Dtest=FileSystemDocumentStorageServiceTest,FileSystemTutorAttachmentStorageTest test; the existing admin RAG storage root behavior must remain unchanged.
- [ ] Step 6: Commit. Commit feat(attachments): add controlled private file storage.

## Task 3: Server-authoritative validation

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentValidator.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentValidationResult.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentValidatorTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContract.java
- Modify: backend/src/main/resources/application.properties
- Modify: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java

**Interfaces:**
- Consumes: MultipartFile, TutorAttachmentContract, Apache Tika detection, ImageIO, and the authenticated request boundary.
- Produces: TutorAttachmentValidationResult validate(MultipartFile file), returning canonical filename, MIME, kind, byte size, checksum input, and safe error code/message.

- [ ] Step 1: Write the failing validator tests. Add acceptsPdfDocxTxtPngJpgJpegWebp, rejectsOversizedFile, rejectsEmptyFile, rejectsExtensionMimeMismatch, rejectsMagicByteMismatch, rejectsMalformedDocument, rejectsDecompressionBomb, rejectsActiveContent, rejectsTraversalFilename, and rejectsImageAbove25Megapixels. Assert 10 MiB is accepted at the exact boundary and 10 MiB plus one byte is rejected.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentValidatorTest test. Expected: FAIL because the validator and pixel guard are not defined.
- [ ] Step 3: Implement validation. Detect content server-side, verify PDF/DOCX/PNG/JPEG/WEBP signatures, decode text safely, sanitize display names, reject active content and generic ZIP, enforce 10 MiB and 25 megapixels, and return normalized errors without provider or filesystem details. Set Spring multipart max-file-size to 10MB and max-request-size to 50MB for the batch endpoint.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect every supported/unsupported boundary test to pass.
- [ ] Step 5: Run current attachment regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentControllerTest,AcademicLuxuryCompatibilityTest test. Existing safe upload and image capability tests must remain green.
- [ ] Step 6: Commit. Commit feat(attachments): enforce multimodal upload validation.

## Task 4: Durable repository and lifecycle persistence

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentRepository.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/JdbcTutorAttachmentRepository.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/JdbcTutorAttachmentRepositoryTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentService.java

**Interfaces:**
- Consumes: V30 ai_attachments schema, TutorAttachment record, TutorAttachmentStorage, and AuthPrincipal user IDs.
- Produces: save(TutorAttachment), findOwned(UUID userId, UUID conversationId, UUID attachmentId), findOwnedByIds(UUID userId, UUID conversationId, List<UUID> ids), updateStatus(UUID attachmentId, AttachmentStatus status, String errorCode), and findStaleProcessing(Instant cutoff).

- [ ] Step 1: Write failing repository/service tests. Add saveAndLoadDurableMetadata, filtersByOwnerAndConversation, updatesLifecycleStatus, findsStaleProcessing, and doesNotReturnRemovedOrExpiredForChat.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=JdbcTutorAttachmentRepositoryTest test. Expected: FAIL because the repository and JDBC mappings do not exist.
- [ ] Step 3: Implement durable persistence. Replace the ConcurrentHashMap authority in TutorAttachmentService with the repository while retaining existing public service naming and safe errors. Store only metadata in PostgreSQL and use generated storage keys for bytes.
- [ ] Step 4: Run GREEN. Run the focused Maven command against the repository test fixture and expect all ownership/status filters to pass.
- [ ] Step 5: Run conversation/attachment regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentControllerTest,ConversationServiceTest test.
- [ ] Step 6: Commit. Commit feat(attachments): persist private attachment metadata.

## Task 5: Conversation bootstrap and upload/status/remove API

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentBatchResponse.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentApiContractTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/ConversationService.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/ConversationController.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentController.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentService.java
- Modify: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java
- Modify: backend/src/test/java/com/ieltsaitutor/tutor/memory/ConversationControllerTest.java

**Interfaces:**
- Consumes: AuthPrincipal, ConversationService.create, TutorAttachmentValidator, TutorAttachmentStorage, and repository methods from Task 4.
- Produces: POST /api/ai/conversations for an owned ACTIVE conversation; POST /api/ai/attachments with conversationId and repeated files; GET /api/ai/attachments/{id}; authorized image preview through the same ownership boundary; DELETE /api/ai/attachments/{id}; response { attachments: List<TutorAttachment> }.

- [ ] Step 1: Write failing MVC tests. Add createsOwnedConversationWithoutCallingAI, uploadsOneToFiveFiles, rejectsSixFiles, returnsSafeMetadataAndStatus, getsOnlyOwnedAttachment, previewsOnlyOwnedImage, removesOwnedAttachment, usesAntiEnumerationSafeStatusForForeignId, and rejectsMissingOrForeignConversation.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentApiContractTest,TutorAttachmentControllerTest,ConversationControllerTest test. Expected: FAIL because batch parts, conversation bootstrap, and ownership scope are not implemented.
- [ ] Step 3: Implement the API. Keep /api/ai/attachments and current authentication interception. Accept canonical files[] plus the one-file compatibility alias, persist each item independently, return safe metadata, and never return storage paths. Require conversation ownership before attachment lookup.
- [ ] Step 4: Run GREEN. Run the same focused Maven command and expect the API contract tests to pass.
- [ ] Step 5: Run chat/auth regressions. Run cmd /c .\mvnw.cmd -Dtest=AiChatControllerTest,ConversationControllerTest,AuthControllerTest test.
- [ ] Step 6: Commit. Commit feat(attachments): expose scoped upload and lifecycle APIs.

## Task 6: Processing lifecycle and stale recovery

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentProcessingService.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentProcessingRecovery.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentProcessingProperties.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentCleanupService.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentProcessingServiceTest.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentProcessingRecoveryTest.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentCleanupServiceTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentService.java
- Modify: backend/src/main/resources/application.properties

**Interfaces:**
- Consumes: stored attachment IDs, repository lifecycle methods, and the later document/image processors.
- Produces: process(UUID attachmentId): void; recoverStale(Instant now): int; cleanupRemovedOrExpired(): int; configuration timeout five minutes and one bounded recovery attempt.

- [ ] Step 1: Write failing lifecycle tests. Add transitionsStoredToProcessingToReady, parserFailureBecomesFailed, providerFailureDoesNotInvalidateReady, staleProcessingBecomesFailedAfterBoundedRecovery, siblingFilesRemainValid, deletesUnreferencedRemovedBytes, retainsMessageReferencedBytes, and retainsActiveAttachmentBytes.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentProcessingServiceTest,TutorAttachmentProcessingRecoveryTest test. Expected: FAIL because processing orchestration and recovery do not exist.
- [ ] Step 3: Implement bounded processing and cleanup. Mark processing_started_at, delegate one file at a time to the processor boundary, record normalized error codes, and recover stale rows after five minutes without unbounded retries. Configure bounded worker concurrency and ensure upload success is independent per file. Run cleanup only for REMOVED or EXPIRED rows whose storage key has no active attachment or retained message reference; never delete bytes still needed by history.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect deterministic transitions and timeout tests to pass.
- [ ] Step 5: Run API regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentApiContractTest,TutorAttachmentControllerTest test.
- [ ] Step 6: Commit. Commit feat(attachments): add bounded processing lifecycle.

## Task 7: PDF, DOCX, and TXT extraction with provenance

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentDocumentProcessor.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentChunk.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentDocumentProcessorTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/rag/ingestion/TikaDocumentExtractor.java
- Modify: backend/src/main/java/com/ieltsaitutor/rag/ingestion/DocumentChunker.java
- Modify: backend/src/test/java/com/ieltsaitutor/rag/ingestion/TikaDocumentExtractorTest.java
- Modify: backend/src/test/java/com/ieltsaitutor/rag/ingestion/DocumentChunkerTest.java

**Interfaces:**
- Consumes: StoredDocument, DocumentExtractor, TikaDocumentExtractor, DocumentChunker, ChunkingOptions.defaults(), and private attachment storage.
- Produces: processDocument(TutorAttachment attachment): List<TutorAttachmentChunk> carrying attachmentId, filename, pageNumber, sectionLabel, chunkIndex, content, and tokenEstimate.

- [ ] Step 1: Write failing extraction tests. Add extractsTxtWithCharsetSafety, extractsDocxText, extractsPdfTextWithPageMetadata, returnsNeedsVisionForScannedPdf, and preservesChunkProvenance.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentDocumentProcessorTest,TikaDocumentExtractorTest,DocumentChunkerTest test. Expected: FAIL because the private adapter and provenance record do not exist.
- [ ] Step 3: Implement the adapter. Reuse Tika and the current DOCX fallback, adapt private files to StoredDocument, preserve filename/page/section/chunk index, and keep target 550/overlap 80 defaults.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect extraction/provenance tests to pass without altering admin RAG ingestion semantics.
- [ ] Step 5: Run RAG parser regressions. Run cmd /c .\mvnw.cmd -Dtest=TikaDocumentExtractorTest,DocumentChunkerTest,RagFailureModeTest test.
- [ ] Step 6: Commit. Commit feat(attachments): reuse document extraction with provenance.

## Task 8: Private chunks, embeddings, and retrieval

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentChunkRepository.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/JdbcTutorAttachmentChunkRepository.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentRetrievalService.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/RetrievedAttachmentChunk.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentRetrievalServiceTest.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentChunkRepositoryTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/rag/embedding/DefaultQueryEmbeddingService.java
- Modify: backend/src/main/java/com/ieltsaitutor/rag/retrieval/JdbcVectorRetrievalService.java

**Interfaces:**
- Consumes: TutorAttachmentChunk, EmbeddingProviderRouter, EmbeddingSpaceSelector, DefaultQueryEmbeddingService, and current vector retrieval conventions.
- Produces: retrieve(UUID userId, UUID conversationId, List<UUID> attachmentIds, String query): List<RetrievedAttachmentChunk>; defaults topK 8, max four chunks per file, diversified selected-file results.

- [ ] Step 1: Write failing private retrieval tests. Add filtersByOwnerConversationAndSelectedIds, excludesAdminCorpus, rejectsEmbeddingSpaceMismatch, limitsTopKToEight, capsSingleFileAtFour, andDiversifiesAcrossFiles.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentRetrievalServiceTest,TutorAttachmentChunkRepositoryTest test. Expected: FAIL because private chunk persistence and retrieval do not exist.
- [ ] Step 3: Implement private retrieval. Persist chunks and vectors with provider/model/dimension/version metadata, use exact 768-space matching, apply ownership/conversation/selected-ID/READY filters in SQL, and diversify/cap results before returning.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect all isolation and ranking assertions to pass.
- [ ] Step 5: Run embedding regressions. Run cmd /c .\mvnw.cmd -Dtest=EmbeddingProviderRouterTest,EmbeddingSpaceTest,EmbeddingProviderContractTest test.
- [ ] Step 6: Commit. Commit feat(attachments): add private vector retrieval.

## Task 9: Full-document summary and attachment context budget

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentQuestionMode.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/AttachmentChatScope.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/AttachmentContext.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/AttachmentRepresentation.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContextBudget.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContextBuilder.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentSummaryService.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContextBuilderTest.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentSummaryServiceTest.java
- Modify: backend/src/main/resources/application.properties

**Interfaces:**
- Consumes: RetrievedAttachmentChunk, TutorAttachmentRetrievalService, TutorAttachmentDocumentProcessor output, and 12,000-token default.
- Produces: build(AttachmentChatScope scope, String learnerQuestion): AttachmentContext; summarizeWholeDocument(UUID attachmentId): AttachmentRepresentation; compareDocuments(List<UUID> attachmentIds): List<AttachmentRepresentation>.

- [ ] Step 1: Write failing context tests. Add focusedQuestionUsesTopK, wholeDocumentUsesHierarchicalBatches, comparisonRepresentsEveryFile, preservesAttachmentProvenance, and neverSilentlyCutsTrailingText.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentContextBuilderTest,TutorAttachmentSummaryServiceTest test. Expected: FAIL because the modes, budget, and builders do not exist.
- [ ] Step 3: Implement context construction. Route focused questions to private retrieval, whole-file summaries to bounded partial-summary batches, and multi-file comparison to one representation per selected file. Enforce 12,000 attachment-derived tokens while preserving room for system/context/history/question/answer.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect mode, provenance, and budget assertions to pass.
- [ ] Step 5: Run RAG context regressions. Run cmd /c .\mvnw.cmd -Dtest=RagContextBuilderTest,RagChatServiceTest,TutorAttachmentContextBuilderTest test.
- [ ] Step 6: Commit. Commit feat(attachments): add bounded document context assembly.

## Task 10: Provider capability routing

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/model/AiAttachmentPart.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/routing/AiProviderMultimodalRoutingTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/provider/ProviderCapability.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/model/AiChatCommand.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/routing/AiProviderAdapter.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/routing/AiProviderRouter.java
- Modify: backend/src/test/java/com/ieltsaitutor/ai/routing/AiProviderRouterTest.java

**Interfaces:**
- Consumes: Existing AiProviderRouter order/health policy and provider-neutral AiChatCommand.
- Produces: ProviderCapability values CHAT, DOCUMENT_CONTEXT, and VISION_IMAGE; AiAttachmentPart(UUID attachmentId, String filename, String mediaType, AttachmentKind kind, Supplier<InputStream> openStream); AiChatCommand.requiredCapabilities() and attachments() with empty defaults for current constructors.

- [ ] Step 1: Write failing routing tests. Add imageTurnSkipsTextOnlyProvider, documentContextRequiresDeclaredCapability, textOnlyTurnKeepsExistingOrder, noVisionProviderReturnsSafeUnavailable, and providerFailureDoesNotChangeAttachmentStatus.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=AiProviderMultimodalRoutingTest,AiProviderRouterTest test. Expected: FAIL because capabilities and attachment parts are not modeled.
- [ ] Step 3: Implement additive routing. Keep existing CHAT behavior and constructors. Make the router require CHAT plus VISION_IMAGE for image turns and CHAT plus DOCUMENT_CONTEXT for document-context turns. Do not pass vendor-specific payloads to frontend code.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect text-only, image, unavailable, and failure-routing assertions to pass.
- [ ] Step 5: Run all provider regressions. Run cmd /c .\mvnw.cmd -Dtest=AiProviderRouterTest,AiProviderFallbackTest,GroqAiProviderTest,CloudflareAiProviderTest,GeminiAiProviderTest test.
- [ ] Step 6: Commit. Commit feat(ai): add provider-neutral multimodal capability routing.

## Task 11: Real image payload for verified vision provider

**Files:**
- Create: backend/src/test/java/com/ieltsaitutor/ai/provider/GeminiVisionProviderTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/provider/GeminiAiProvider.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/config/GeminiProperties.java
- Modify: backend/src/main/resources/application.properties
- Modify: backend/src/test/java/com/ieltsaitutor/ai/provider/GeminiAiProviderTest.java

**Interfaces:**
- Consumes: AiAttachmentPart from Task 10 and the configured Gemini model.
- Produces: Gemini request content containing actual validated image bytes/media type and a verified vision capability only when the current model configuration explicitly enables it.

- [ ] Step 1: Write failing provider tests. Add sendsActualImagePartNotFilenameOnly, preservesQuestionAndTrustedContext, rejectsVisionWhenCapabilityDisabled, and textOnlyRequestPayloadRemainsCompatible. Assert the serialized request contains image data from the fixture, not only filename or alt text.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=GeminiVisionProviderTest,GeminiAiProviderTest test. Expected: FAIL because GeminiAiProvider has no image content path or verified vision flag.
- [ ] Step 3: Implement real image input. Add a safe, non-secret vision capability property and extend Gemini request construction with actual image parts opened from controlled storage. Keep model configurable, API keys server-side, provider-neutral errors, timeout/retry behavior, and text-only payload compatibility.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect actual-image-payload and capability assertions to pass.
- [ ] Step 5: Run provider routing regressions. Run cmd /c .\mvnw.cmd -Dtest=GeminiVisionProviderTest,GeminiAiProviderTest,AiProviderMultimodalRoutingTest test.
- [ ] Step 6: Commit. Commit feat(ai): send verified real image input to Gemini.

## Task 12: Scanned PDF page rendering and vision fallback

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/PdfPageRenderer.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/PageRange.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/RenderedAttachmentPage.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/VisionContextResult.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/ScannedPdfVisionService.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/ScannedPdfVisionServiceTest.java
- Modify: backend/pom.xml
- Modify: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentDocumentProcessor.java
- Modify: backend/src/main/resources/application.properties

**Interfaces:**
- Consumes: Tika NEEDS_OCR result, Apache PDFBox 3.0.5 already in the current dependency tree, AiProviderRouter VISION_IMAGE capability, and the approved 20-page bound.
- Produces: renderRelevantPages(Path pdf, PageRange range): List<RenderedAttachmentPage>; analyzeScannedPdf(TutorAttachment attachment, String question): VisionContextResult.

- [ ] Step 1: Write failing fallback tests. Add textPdfSkipsVision, emptyPdfRendersActualPages, capsRenderedPagesAtTwenty, batchesLaterPagesWithoutSilence, and unavailableVisionReturnsTruthfulError.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=ScannedPdfVisionServiceTest test. Expected: FAIL because PDF page rendering and fallback orchestration do not exist.
- [ ] Step 3: Implement bounded fallback. Use PDFBox 3.0.5 already resolved by Tika; declare the direct dependency only to make the compile boundary explicit. Render pages in bounded batches, route actual rendered page images through VISION_IMAGE, retain page provenance, and return a truthful error when rendering or vision is unavailable.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect page cap, batch, provenance, and truthful-error assertions to pass.
- [ ] Step 5: Run extraction/provider regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentDocumentProcessorTest,ScannedPdfVisionServiceTest,GeminiVisionProviderTest test.
- [ ] Step 6: Commit. Commit feat(ai): add bounded scanned PDF vision fallback.

## Task 13: Attachment-aware chat request and orchestration

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentChatContext.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/dto/AiAttachmentSource.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentResolutionService.java
- Create: backend/src/test/java/com/ieltsaitutor/tutor/TutorAttachmentOrchestrationTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatRequest.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/service/AiChatService.java
- Modify: backend/src/main/java/com/ieltsaitutor/ai/dto/AiChatResponse.java

**Interfaces:**
- Consumes: attachmentIds from HTTP, AuthPrincipal, TutorAttachmentRepository, TutorAttachmentContextBuilder, ScannedPdfVisionService, and AiChatCommand.
- Produces: AiChatRequest.attachmentIds() as an optional List<UUID>; TutorAttachmentResolutionService.resolve(AuthPrincipal principal, UUID conversationId, List<UUID> ids): TutorAttachmentChatContext; additive AiChatResponse.attachmentSources.

- [ ] Step 1: Write failing orchestration tests. Add textOnlyRequestSkipsAttachmentResolution, rejectsMoreThanFiveIds, rejectsDuplicateIds, rejectsNonReadyAttachment, resolvesOwnedConversationScopedAttachments, returnsInsufficientEvidenceForWeakDocumentContext, and keepsProviderFailureAttachmentReady.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=TutorAttachmentOrchestrationTest,AiChatControllerTest test. Expected: FAIL because AiChatRequest has no IDs and TutorOrchestrator does not resolve attachments.
- [ ] Step 3: Implement the chat contract. Add attachmentIds at the end of AiChatRequest with compatibility constructors, resolve ownership/status before any provider call, construct private document/vision context, preserve deterministic Tutor tools, and route text-only requests through the existing path without embedding work.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect contract, authorization, insufficient-evidence, and provider-failure assertions to pass.
- [ ] Step 5: Run Tutor/RAG regressions. Run cmd /c .\mvnw.cmd -Dtest=TutorOrchestratorTest,TutorDeterministicIntentTest,TutorProviderBoundaryTest,RagChatServiceTest test.
- [ ] Step 6: Commit. Commit feat(ai): ground Tutor chat in private attachments.

## Task 14: Durable message attachment links and history

**Files:**
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentHistoryView.java
- Create: backend/src/test/java/com/ieltsaitutor/tutor/memory/ConversationAttachmentHistoryTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/AiMessage.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/JdbcConversationRepository.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/ConversationService.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/memory/ConversationController.java
- Modify: backend/src/main/java/com/ieltsaitutor/tutor/TutorOrchestrator.java

**Interfaces:**
- Consumes: successful chat attachment IDs and ai_message_attachments.
- Produces: ConversationDetail messages with safe TutorAttachmentHistoryView metadata; ConversationService.appendMessageWithAttachments(UUID userId, UUID conversationId, AiMessage message, List<UUID> attachmentIds).

- [ ] Step 1: Write failing history tests. Add linksReadyAttachmentsAfterSuccessfulSend, doesNotLinkFailedAttachments, reloadReturnsSafeMetadata, doesNotLoadBinaryBodies, messageAttachmentOrdinalIsStable, and conversationDeletionSchedulesUnreferencedCleanup.
- [ ] Step 2: Run RED. Run cmd /c .\mvnw.cmd -Dtest=ConversationAttachmentHistoryTest,ConversationControllerTest test. Expected: FAIL because AiMessage and JDBC history have no attachment relation.
- [ ] Step 3: Implement durable linking. Persist relation rows only after a successful chat message, return safe filename/kind/size/status/preview metadata, and keep binaries out of conversation history hydration. Enforce owner and conversation checks again in the repository transaction, and invoke the bounded cleanup boundary after conversation deletion without removing retained message bytes.
- [ ] Step 4: Run GREEN. Run the focused Maven command and expect link, reload, and no-binary assertions to pass.
- [ ] Step 5: Run memory/security regressions. Run cmd /c .\mvnw.cmd -Dtest=ConversationControllerTest,Phase2BConversationSecurityRegressionTest,ConversationServiceTest test.
- [ ] Step 6: Commit. Commit feat(attachments): persist attachment links in Tutor history.

## Task 15: Frontend attachment and chat API normalization

**Files:**
- Create: frontend/src/__tests__/tutor-multimodal-api.test.js
- Create: frontend/src/features/tutor/attachmentSourceSchema.js
- Modify: frontend/src/services/tutorAttachmentsApi.js
- Modify: frontend/src/services/aiTutorApi.js
- Modify: frontend/src/features/tutor/attachmentContract.js

**Interfaces:**
- Consumes: backend batch metadata, attachmentSources, existing auth session token, and current AiTutorApiError normalization.
- Produces: uploadAttachments(files, conversationId), getAttachment(id), deleteAttachment(id), sendTutorMessage({ message, context, history, conversationId, attachmentIds }), and normalized attachment source metadata.

- [ ] Step 1: Write failing client tests. Add uploadSendsRepeatedFilesAndConversationId, normalizesBatchAndLegacyOneFileResponses, sendsOnlyAttachmentIdsInChatJson, rejectsDuplicateIdsClientSide, normalizesAttachmentSources, and preservesExistingProviderErrors.
- [ ] Step 2: Run RED. Run npm test -- --run src/__tests__/tutor-multimodal-api.test.js. Expected: FAIL because batch upload and attachmentIds normalization are absent.
- [ ] Step 3: Implement API normalization. Keep the existing fetch/auth/error patterns, send multipart files and conversationId, retain stable existing attachment fields for callers, and ensure chat JSON contains IDs only.
- [ ] Step 4: Run GREEN. Run the focused Vitest command and expect all request/normalization assertions to pass.
- [ ] Step 5: Run attachment/API regressions. Run npm test -- --run src/__tests__/tutor-attachments.test.jsx src/__tests__/tutor-rag.test.jsx src/__tests__/tutor-memory.test.jsx.
- [ ] Step 6: Commit. Commit feat(frontend): add multimodal Tutor API contracts.

## Task 16: Multiple picker and attachment cards

**Files:**
- Create: frontend/src/__tests__/tutor-multimodal-picker.test.jsx
- Modify: frontend/src/components/tutor/AttachmentComposer.jsx
- Modify: frontend/src/components/tutor/AttachmentStatus.jsx
- Modify: frontend/src/features/tutor/attachmentContract.js
- Modify: frontend/src/styles/globals.css

**Interfaces:**
- Consumes: upload API normalization from Task 15 and the current AttachmentStatus presentation contract.
- Produces: AttachmentComposer multiple selection with exact accept values; AttachmentStatus list rendering image/document cards with filename, size, state, remove, and retry actions.

- [ ] Step 1: Write failing component tests. Add acceptsOneFile, acceptsFiveFiles, rejectsSixthSelection, rejectsUnsupportedType, rejectsOver10MiB, rendersFiveIndependentCards, rendersImageThumbnail, rendersDocumentIcon, exposesKeyboardRemoveRetryActions, and keepsReducedMotionAccessible.
- [ ] Step 2: Run RED. Run npm test -- --run src/__tests__/tutor-multimodal-picker.test.jsx. Expected: FAIL because the current composer selects only files[0] and disables itself after one active attachment.
- [ ] Step 3: Implement the picker/cards. Use multiple on the existing hidden input, reject excess files without uploading them, keep each card independent, preserve responsive/Academic Luxury styles, and use accessible live status labels.
- [ ] Step 4: Run GREEN. Run the focused Vitest command and expect picker/card assertions to pass.
- [ ] Step 5: Run UI/accessibility regressions. Run npm test -- --run src/__tests__/academic-luxury-2-accessibility.test.jsx src/__tests__/tutor-attachments.test.jsx src/__tests__/tutor-shell.test.jsx.
- [ ] Step 6: Commit. Commit feat(frontend): add multi-file En attachment cards.

## Task 17: Concurrent upload queue, retry, and removal

**Files:**
- Create: frontend/src/features/tutor/useTutorAttachmentQueue.js
- Create: frontend/src/__tests__/tutor-attachment-queue.test.jsx
- Modify: frontend/src/components/tutor/FloatingTutor.jsx
- Modify: frontend/src/services/tutorAttachmentsApi.js

**Interfaces:**
- Consumes: uploadAttachments, getAttachment, deleteAttachment, conversation bootstrap, and the five-card UI.
- Produces: useTutorAttachmentQueue({ conversationId, onError }) with state attachments, addFiles(files), retry(id), remove(id), readyIds, and isBusy; no more than two active upload promises.

- [ ] Step 1: Write failing queue tests. Add limitsConcurrencyToTwo, keepsFourSuccessesWhenOneFails, retriesOnlyFailedFile, removesOnlyRequestedFile, pollsProcessingToReady, releasesPreviewUrls, and chatRetryMakesNoUploadCall.
- [ ] Step 2: Run RED. Run npm test -- --run src/__tests__/tutor-attachment-queue.test.jsx. Expected: FAIL because the queue hook and independent state transitions do not exist.
- [ ] Step 3: Implement the queue. Use a bounded worker queue, retain successful records when a sibling fails, poll only PROCESSING items, keep FAILED items retryable, remove one item without resetting others, and clean object URLs on removal/unmount.
- [ ] Step 4: Run GREEN. Run the focused Vitest command and expect concurrency and retry assertions to pass.
- [ ] Step 5: Run Tutor regressions. Run npm test -- --run src/__tests__/tutor.test.jsx src/__tests__/tutor-request-state.test.jsx src/__tests__/tutor-attachments.test.jsx.
- [ ] Step 6: Commit. Commit feat(frontend): add bounded attachment upload queue.

## Task 18: Composer READY gating, send retry, and history rendering

**Files:**
- Create: frontend/src/__tests__/tutor-multimodal-chat.test.jsx
- Modify: frontend/src/components/tutor/TutorComposer.jsx
- Modify: frontend/src/components/tutor/FloatingTutor.jsx
- Modify: frontend/src/components/tutor/TutorMessage.jsx
- Modify: frontend/src/services/aiTutorApi.js
- Modify: frontend/src/styles/globals.css

**Interfaces:**
- Consumes: useTutorAttachmentQueue, sendTutorMessage attachmentIds, conversation history attachment metadata, and existing SourceChip/TutorGroundingBadge.
- Produces: send text-only or text plus READY IDs; clear disabled/wait state for VALIDATING/UPLOADING/PROCESSING; no silent FAILED omission; message attachment chips and attachmentSources rendering.

- [ ] Step 1: Write failing chat UI tests. Add textOnlySendUnchanged, waitsForReadyAttachments, refusesSilentFailedOmission, sendsAllReadyIdsInOrder, retriesChatWithoutUpload, reloadsAttachmentChips, rendersRealAttachmentProvenance, and keepsComposerKeyboardAccessible.
- [ ] Step 2: Run RED. Run npm test -- --run src/__tests__/tutor-multimodal-chat.test.jsx. Expected: FAIL because TutorComposer accepts only one attachment and FloatingTutor drops attachmentId before aiTutorApi sends.
- [ ] Step 3: Implement send/history behavior. Pass ordered READY IDs, block or wait while any selected item is not sendable, preserve pending attachments during provider failure, reuse IDs on retry, map history metadata to safe cards/chips, and never fetch binary bodies for history.
- [ ] Step 4: Run GREEN. Run the focused Vitest command and expect send, retry, provenance, and accessibility assertions to pass.
- [ ] Step 5: Run full Tutor/UI regressions. Run npm test -- --run src/__tests__/tutor.test.jsx src/__tests__/tutor-memory.test.jsx src/__tests__/tutor-references.test.jsx src/__tests__/tutor-multimodal-chat.test.jsx.
- [ ] Step 6: Commit. Commit feat(frontend): gate Tutor send on ready attachments.

## Task 19: Security, AI, and persistence regression suite

**Files:**
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentSecurityRegressionTest.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorMultimodalIntegrationTest.java
- Create: frontend/src/__tests__/tutor-multimodal-regression.test.jsx
- Modify: backend/src/test/java/com/ieltsaitutor/rag/RagSecurityRegressionTest.java
- Modify: backend/src/test/java/com/ieltsaitutor/tutor/memory/Phase2BConversationSecurityRegressionTest.java

**Interfaces:**
- Consumes: all attachment, private retrieval, provider, chat, and history boundaries from Tasks 1 through 18.
- Produces: deterministic regression evidence for cross-user isolation, cross-conversation isolation, READY enforcement, no fake vision, mixed turns, no-reupload retry, and history persistence.

- [ ] Step 1: Write failing integration/regression tests. Add userACannotUseUserBAttachment, conversationACannotUseConversationBAttachment, removedAndExpiredAreRejected, expiredSessionCannotAccessAttachment, imageUsesActualPixels, textOnlyProviderCannotReceiveVisionTurn, mixedImagePdfDocxPreservesSources, providerFailureKeepsAttachmentReady, retryUsesExistingIds, and reloadPreservesMetadata.
- [ ] Step 2: Run RED. Run backend cmd /c .\mvnw.cmd -Dtest=TutorAttachmentSecurityRegressionTest,TutorMultimodalIntegrationTest test and frontend npm test -- --run src/__tests__/tutor-multimodal-regression.test.jsx. Expected: newly added assertions fail until all earlier boundaries are wired together.
- [ ] Step 3: Implement only integration fixes exposed by these tests. Do not redesign unrelated flows, bypass ownership, add fake responses, or change admin RAG semantics. Use deterministic provider fakes and fixture files; do not call live providers in automated tests.
- [ ] Step 4: Run GREEN. Rerun both focused commands and require zero failures.
- [ ] Step 5: Run nearby security/provider regressions. Run backend cmd /c .\mvnw.cmd -Dtest=RagSecurityRegressionTest,Phase2BConversationSecurityRegressionTest,AiProviderMultimodalRoutingTest test and frontend npm test -- --run src/__tests__/phase2b-regression.test.jsx src/__tests__/tutor-multimodal-regression.test.jsx.
- [ ] Step 6: Commit. Commit test(ai): verify secure multimodal Tutor flows.

## Task 20: Final verification and runtime acceptance

**Files:**
- Create: docs/qa/2026-09-28-en-multimodal-attachments-acceptance.md
- Modify: only files required by a reproducible Important/Critical defect found during final verification.
- Test: docs/qa/2026-09-28-en-multimodal-attachments-acceptance.md plus the focused suites named below.

**Interfaces:**
- Consumes: approved spec, all task commits, clean QA database, local backend/frontend runtime, and existing inspect-flyway-state.ps1.
- Produces: evidence for automated verification, clean QA migration, real document/vision/mixed smoke, persistence/restart, security smoke, and clean working tree.

- [ ] Step 1: Re-read the approved spec and this plan. Map every spec section to Tasks 1–19. Confirm no task edits V1 through V29, admin RAG corpus, legacy/shared database, auth model, or text-only chat semantics.
- [ ] Step 2: Run frontend full verification. From frontend run npm test -- --run, npm run lint, and npm run build. Require Vitest exit 0, oxlint exit 0, and Vite build exit 0.
- [ ] Step 3: Run backend full verification. From backend run cmd /c .\mvnw.cmd test and cmd /c .\mvnw.cmd package. Record tests, failures, errors, skips, and any known Docker/Testcontainers environment limitation without changing dependencies speculatively.
- [ ] Step 4: Verify a clean QA migration. Use a newly provisioned QA database, never the legacy/shared database. Start the backend with explicit RAG_DB_NAME and existing provider-safe local configuration, then run scripts/db/inspect-flyway-state.ps1 -DatabaseUrl using the QA connection string without password query parameters. Require V1 through V30 success, ai_attachments, ai_attachment_chunks, ai_message_attachments, vector(768), and required indexes.
- [ ] Step 5: Run real local/QA acceptance. Upload five supported files, reject a sixth, reject an oversize file, ask about a unique fact in TXT/DOCX/PDF, ask “Ảnh này có gì?” with real JPG/PNG pixels, compare multiple documents, run image + PDF + DOCX mixed chat, exercise scanned-PDF fallback, and verify truthful errors when vision is unavailable.
- [ ] Step 6: Verify retry, persistence, and cleanup. Fail one upload while four succeed, retry only the failed file, retry chat without another upload, reload Én to verify metadata/chips, restart backend, verify history remains available, and prove removed/expired bytes are deleted only when no active or retained message reference remains.
- [ ] Step 7: Verify security and runtime hygiene. Use two local QA users and two conversations to prove foreign UUID denial and conversation isolation. Check browser console for uncaught errors/warnings, network for failed loops or duplicate uploads, no raw paths/provider errors, and no horizontal overflow in the existing 375/768/1024/1440 responsive checks.
- [ ] Step 8: Fix only reproducible Important/Critical defects. For each defect, add a failing regression test, verify RED, make the smallest fix, verify GREEN, and rerun the affected suite. Do not start another feature or alter approved scope.
- [ ] Step 9: Final repository checks. Run git diff --check, git status --short, and git log --oneline -12. Require no uncommitted changes, no legacy DB modification, no push, no merge, and no deploy. Do not create an empty verification commit.
- [ ] Step 10: Commit the acceptance record. Commit docs(qa): record En multimodal attachment acceptance after the acceptance document is complete; if an Important/Critical fix was required, include only that focused fix and its tests in the same task commit.

## Spec coverage map

- Purpose, supported formats, and non-goals: Tasks 3, 7, 10, 11, 12, 16.
- High-level upload/chat separation and ID-only contract: Tasks 5, 13, 15, 17, 18.
- Durable schema, storage, lifecycle, cleanup, and migration safety: Tasks 1, 2, 4, 5, 6, 14, 20.
- Server validation, ownership, conversation isolation, and truthful errors: Tasks 3, 5, 6, 13, 19, 20.
- Tika extraction, chunking, provenance, embeddings, and private retrieval: Tasks 7, 8, 9.
- Vision routing, actual image payloads, scanned PDF, and mixed turns: Tasks 10, 11, 12, 13, 19, 20.
- Context budget and document summarization: Task 9.
- Authorized image previews, retention, and cleanup: Tasks 5, 6, 14, 18, 20.
- Frontend picker, cards, queue, READY gating, retry, and history: Tasks 15, 16, 17, 18.
- Automated test strategy, acceptance cases, and final verification: Tasks 1–20.

## Verification command reference

Frontend:

    cd frontend
    npm test -- --run
    npm run lint
    npm run build

Backend:

    cd backend
    cmd /c .\mvnw.cmd test
    cmd /c .\mvnw.cmd package

Focused backend example:

    cd backend
    cmd /c .\mvnw.cmd -Dtest=TutorAttachmentValidatorTest test

Focused frontend example:

    cd frontend
    npm test -- --run src/__tests__/tutor-multimodal-chat.test.jsx

Database inspection, read-only:

    powershell -ExecutionPolicy Bypass -File .\scripts\db\inspect-flyway-state.ps1 -DatabaseUrl postgresql://ielts@localhost:5432/ielts_ai_en_multimodal_qa_20260928

The QA database must be newly provisioned and explicitly named for this feature.
The existing legacy/shared database is not a target of implementation or
verification.

## Commit sequence

1. feat(attachments): add durable attachment domain and schema
2. feat(attachments): add controlled private file storage
3. feat(attachments): enforce multimodal upload validation
4. feat(attachments): persist private attachment metadata
5. feat(attachments): expose scoped upload and lifecycle APIs
6. feat(attachments): add bounded processing lifecycle
7. feat(attachments): reuse document extraction with provenance
8. feat(attachments): add private vector retrieval
9. feat(attachments): add bounded document context assembly
10. feat(ai): add provider-neutral multimodal capability routing
11. feat(ai): send verified real image input to Gemini
12. feat(ai): add bounded scanned PDF vision fallback
13. feat(ai): ground Tutor chat in private attachments
14. feat(attachments): persist attachment links in Tutor history
15. feat(frontend): add multimodal Tutor API contracts
16. feat(frontend): add multi-file En attachment cards
17. feat(frontend): add bounded attachment upload queue
18. feat(frontend): gate Tutor send on ready attachments
19. test(ai): verify secure multimodal Tutor flows
20. docs(qa): record En multimodal attachment acceptance (plus any focused Task 20 defect fix).
