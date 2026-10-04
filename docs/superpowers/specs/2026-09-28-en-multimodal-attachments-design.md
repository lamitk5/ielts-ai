# Én Multimodal Attachments — Design Specification

**Status:** approved design for a future implementation step

**Scope:** authenticated Én chat attachments only. This document defines the
frontend, backend, storage, processing, provider, persistence, security, and
verification contracts. It does not implement the feature, change source code,
create a migration, or change the existing AI response contract yet.

## 1. Purpose

The plus button inside Én will let an authenticated learner select up to five
files in one chat turn. Each file may be at most 10 MiB.

Supported formats:

- Documents: PDF, DOCX, TXT
- Images: PNG, JPG, JPEG, WEBP

Én must use actual file contents, not only a filename, browser MIME value, alt
text, or client-side preview.

Examples:

- Image: “Ảnh này có gì?”, “Giải thích biểu đồ này.”, or “Đọc đề Writing trong ảnh này.”
- Document: “Tóm tắt tài liệu này.”, “Trong tài liệu có nói về coherence không?”, or “So sánh 3 file này.”
- Mixed turn: an IELTS prompt image, a sample-answer PDF, and the learner's DOCX essay in one comparison question.

Image questions require a verified vision-capable provider. Document questions
must use text extracted from the actual PDF, DOCX, or TXT content. A successful
upload alone is never presented as proof that the model understood a file.

## 2. Non-goals

This feature does not include:

- Audio or video attachments.
- SVG, HTML, HTM, JavaScript, executables, scripts, arbitrary binaries, or ZIP uploads.
- Fake image understanding, filename-only reasoning, or frontend-only simulated uploads.
- Base64 file blobs inside the /api/ai/chat JSON request.
- A permanent personal file library.
- Cross-user or cross-conversation file sharing.
- Replacement of the admin-approved RAG knowledge-base ingestion flow.
- A blanket OCR-library pipeline as the default for scanned PDFs.
- Silent source truncation that changes document meaning.
- Re-uploading attachments when a chat request is retried.
- Changes to authentication, practice scoring, adaptive learning, admin RAG governance, or existing text-only Tutor behavior.

## 3. Current architecture and compatibility

The implementation extends existing Tutor and RAG boundaries rather than
creating a parallel stack.

### Existing frontend boundaries

- FloatingTutor.jsx owns open state, authenticated Tutor state, conversation state, and the current single attachment state.
- TutorComposer.jsx, AttachmentComposer.jsx, and AttachmentStatus.jsx render the current attachment shell.
- tutorAttachmentsApi.js calls /api/ai/attachments; aiTutorApi.js calls /api/ai/chat.
- features/tutor/attachmentContract.js owns the browser allowlist and 10 MiB client validation.
- TutorMessage.jsx already renders provider-neutral references, grounding, and source chips.

The future implementation replaces the single attachment state with a bounded
queue while preserving these component boundaries. The API module continues to
normalize backend responses so UI callers do not depend on provider or storage
payloads.

### Existing backend boundaries

- TutorAttachmentController owns /api/ai/attachments.
- TutorAttachmentService currently validates metadata and stores records in an in-memory ConcurrentHashMap. It currently enforces one active file and does not persist bytes or conversation scope.
- TutorAttachment and TutorAttachmentContract are the current project-compatible names for the attachment record and allowlist.
- AiChatController accepts AiChatRequest; TutorOrchestrator is the authenticated chat orchestration boundary.
- AiConversation, AiMessage, ConversationService, ConversationController, and JdbcConversationRepository are the existing conversation persistence boundaries.
- The current chat request has message, context, history, and conversationId. attachmentIds is added as an optional field while existing constructors and text-only callers remain valid.

### Existing RAG and provider boundaries

- DocumentExtractor, TikaDocumentExtractor, DocumentChunker, ChunkingOptions.defaults(), and DocumentStorageService are reused where their contracts fit.
- The current chunk defaults are target 550 tokens, overlap 80 tokens, and merge threshold 120 tokens.
- EmbeddingProviderRouter, EmbeddingSpaceSelector, EmbeddingProvider, and EmbeddingSpace remain the only embedding boundary.
- AiProvider, AiProviderAdapter, AiProviderRouter, ProviderCapability, and GeminiAiProvider remain the provider-neutral chat boundary.
- The current PostgreSQL vector dimension is 768. Attachment vectors use it only when provider, model, dimension, and version metadata match exactly.

### Compatibility rules

1. Generic text-only POST /api/ai/chat keeps its current behavior and response fields.
2. Chat carries IDs and metadata, never raw file bytes.
3. Existing status, answer, sources, grounding, references, and meta fields remain compatible. Attachment provenance is additive.
4. The current admin RAG storage root and tables remain separate from private learner attachment storage and tables.
5. Existing route protection and AuthPrincipal ownership checks remain authoritative.
6. Before the first upload for a new conversation, the frontend obtains an owned conversation through additive POST /api/ai/conversations. Existing GET, detail, and archive routes remain unchanged.

## 4. High-level architecture

The flow is:

    Learner
      -> Én plus button
      -> client validation
      -> independent queue, maximum 2 concurrent jobs
      -> POST /api/ai/attachments
      -> server validation and generated storage key
      -> ai_attachments metadata
      -> extraction, image validation, chunking, and embedding preparation
      -> READY attachment IDs
      -> POST /api/ai/chat with attachmentIds[]
      -> ownership, conversation, and READY resolution
      -> private retrieval and/or real vision content construction
      -> provider capability routing
      -> grounded answer with attachment provenance
      -> ai_messages plus ai_message_attachments history

Upload and chat are separate operations. A failed chat retry reuses the same
READY IDs and never sends file content again.

## 5. Frontend experience

### Selection

The plus button opens the native chooser with multiple selection enabled. The
centralized accept value is:

    .pdf,.docx,.txt,.png,.jpg,.jpeg,.webp

File six and later are rejected with:

> Chỉ có thể đính kèm tối đa 5 file.

A file greater than 10 MiB is rejected before upload with:

> Tệp vượt quá giới hạn 10 MB.

The backend repeats both checks.

### Cards and states

Each file has its own compact card. Images show a safe local thumbnail,
filename, size, state, and remove action. Documents show a type icon, filename,
size, state, and remove action. Cards stay near the composer.

Visible frontend states:

| Internal state | Learner-facing label |
|---|---|
| SELECTED | Đã chọn |
| VALIDATING | Đang kiểm tra |
| UPLOADING | Đang tải lên |
| PROCESSING | Đang đọc nội dung |
| READY | Sẵn sàng |
| FAILED | Không thể xử lý |

REMOVED and EXPIRED are backend lifecycle states. They are not shown as
confusing active upload states; history may show an unavailable chip.

### Send behavior

- Text-only messages remain sendable exactly as before.
- A message may contain text plus one to five READY attachment IDs.
- While any selected file is VALIDATING, UPLOADING, or PROCESSING, send is disabled or waits with a clear status and never silently omits that file.
- A FAILED file must be retried or removed before the selected files can be sent.
- After successful send, attachment IDs are linked to the persisted message and local preview URLs are released.
- Chat retry reuses IDs and makes zero upload calls.

### Frontend modules

The later implementation updates the existing modules:

- AttachmentComposer.jsx: multiple selection and max-five feedback.
- AttachmentStatus.jsx: five independent cards, previews, retry, and remove.
- TutorComposer.jsx: READY gating and attachment ID submission.
- FloatingTutor.jsx: queue ownership, conversation bootstrap, retry semantics, and history hydration.
- tutorAttachmentsApi.js: batch upload/status/delete normalization.
- attachmentContract.js: shared max-five, 10 MiB, extension, and MIME policy.
- aiTutorApi.js: optional attachmentIds and additive provenance normalization.

A focused useTutorAttachmentQueue.js helper may own the maximum-two queue. It
is not a general file manager.

## 6. HTTP contract

### Conversation bootstrap

    POST /api/ai/conversations
    Authorization: Bearer session-token
    Content-Type: application/json

The body contains the same optional learning context used by the current Tutor
conversation. The response contains the owned conversation ID. It creates an
ACTIVE conversation and does not call an AI provider.

### Upload

    POST /api/ai/attachments
    Authorization: Bearer session-token
    Content-Type: multipart/form-data

Parts:

- conversationId: required UUID owned by the authenticated user.
- files: one to five files, using repeated files parts.

The existing single file part remains a compatibility alias for one-file callers.
New frontend code uses files.

Canonical response:

    {
      "attachments": [
        {
          "id": "<uuid>",
          "conversationId": "<uuid>",
          "filename": "essay.docx",
          "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "sizeBytes": 245812,
          "kind": "DOCUMENT",
          "status": "PROCESSING",
          "capability": null
        }
      ]
    }

The response exposes safe metadata only. It never exposes a filesystem path,
storage root, internal exception, or provider credential. The frontend API
normalizes a one-item response into the current TutorAttachment shape for
existing component callers.

### Status and removal

    GET /api/ai/attachments/{attachmentId}
    DELETE /api/ai/attachments/{attachmentId}

Both routes require authenticated ownership. DELETE is logical removal first;
physical cleanup follows the storage policy.

### Chat extension

The current request is extended additively:

    {
      "message": "So sánh bài của tôi với đề trong ảnh và bài mẫu.",
      "context": { "skill": "WRITING" },
      "conversationId": "<uuid>",
      "attachmentIds": ["<uuid>", "<uuid>", "<uuid>"]
    }

Rules:

- attachmentIds is optional and has a maximum of five UUIDs.
- Duplicate IDs are rejected with a stable 400 error.
- Every ID must belong to the authenticated user and requested conversation.
- Every attachment must be READY, not REMOVED, and not EXPIRED.
- Attachment-bearing requests require authentication.
- The chat endpoint never accepts multipart content or base64 file data.

Normalized attachment provenance is returned additively as attachmentSources,
containing only files and chunks included in model context.

## 7. Lifecycle

### Frontend

    SELECTED -> VALIDATING -> UPLOADING -> PROCESSING -> READY
                                          \-> FAILED

The frontend may remove an item before send. Retry restarts only that failed
item.

### Persisted backend

    VALIDATING -> STORED -> PROCESSING -> READY
                             \-> FAILED

    READY -> REMOVED
    READY -> EXPIRED

The current UPLOADED and IMAGE_READY names are retained only as compatibility
mappings where needed. The durable source of truth uses STORED for received
bytes and PROCESSING until preparation completes. READY means the server has
completed required validation and preparation; it does not mean the model has
inspected the file.

An image can be READY for real vision chat without text chunks. A vision
provider failure during chat does not change the image to FAILED.

## 8. Database design

The implementation creates one additive Flyway migration after the current
historical chain. It does not edit V1 through V29. The intended migration is
V30__create_tutor_attachment_schema.sql; if the chain advances first, the next
unused number is used and recorded in the implementation commit.

### ai_attachments

Required fields:

| Field | Constraint or meaning |
|---|---|
| id | UUID primary key |
| owner_user_id | FK to app_users, never client-authoritative |
| conversation_id | FK to ai_conversations, not null |
| original_filename | bounded display provenance |
| sanitized_filename | safe display name, never a storage path |
| media_type | server-detected canonical MIME |
| attachment_kind | DOCUMENT or IMAGE |
| size_bytes | positive, max 10 MiB |
| sha256 | content checksum |
| storage_key | generated key below controlled root |
| status | STORED, PROCESSING, READY, FAILED, REMOVED, or EXPIRED |
| processing_error_code | normalized safe code |
| processing_started_at | stale-processing marker |
| processing_attempts | bounded retry count |
| created_at | creation time |
| updated_at | lifecycle update time |
| expires_at | nullable retention field |

Indexes cover owner, conversation, status, storage key, and checksum.

### ai_attachment_chunks

Required fields:

- id
- attachment_id
- chunk_index
- page_number nullable
- section_label nullable
- content
- token_estimate
- embedding vector(768) when retrieval uses vectors
- embedding_provider
- embedding_model
- embedding_dimension
- embedding_version
- created_at

The vector index is accompanied by ownership/scope indexes. Retrieval filters by
authorized attachment IDs before similarity ranking.

### ai_message_attachments

Required fields:

- message_id FK to ai_messages
- attachment_id FK to ai_attachments
- ordinal

The pair is unique and ordinal preserves selection order. Message deletion
cascades or schedules cleanup without deleting a file still referenced by
another retained message.

## 9. Binary storage

Private files use a new abstraction in the current attachment package:

    TutorAttachmentStorage
      store(stream, generatedKey, metadata)
      open(storageKey)
      delete(storageKey)
      exists(storageKey)

The local implementation stores below TUTOR_ATTACHMENT_STORAGE_ROOT, separate
from the admin RAG root used by FileSystemDocumentStorageService. Storage keys
are generated UUID-based values. Original filenames are never path inputs.
Every read verifies that the normalized path stays below the configured root.

The abstraction remains compatible with future object storage without adding S3
or another remote service now. Uploads stream where practical and avoid five
duplicated 10 MiB buffers.

## 10. Security and validation

The server independently validates every file and every chat attachment ID.

### File checks

- Maximum is exactly 10 * 1024 * 1024 bytes per file.
- A chat turn accepts at most five files.
- The authenticated principal comes from AuthInterceptor and AuthPrincipal.
- Extension, server MIME detection, signature, size, kind, filename safety, and storage key are checked.
- Browser Content-Type and original extension are advisory only.
- Empty files, separators, NUL characters, traversal, active content, and unsupported formats are rejected.
- DOCX is accepted only when its ZIP container has expected OpenXML markers; generic ZIP is rejected.
- PDF requires the PDF signature; PNG, JPEG, and WEBP require their respective signatures.
- Text is decoded through the existing safe extraction boundary.
- Decoded images default to a 25-megapixel limit.
- Original bytes remain in controlled storage; preprocessing never overwrites them.

Rejected extensions include .svg, .html, .htm, .js, .mjs, .exe, .dll, .bat,
.cmd, .ps1, .jar, and unsupported binary formats.

### Ownership and isolation

Every attachment used in chat must satisfy:

    owner_user_id == authenticatedPrincipal.userId
    conversation_id == requestedConversationId
    status == READY
    not REMOVED
    not EXPIRED
    id is in the current selected list

Conversation ownership is checked before attachment lookup. A guessed UUID uses
the existing anti-enumeration-safe 403/404 policy. Private attachment rows
never enter the public/admin RAG corpus.

## 11. Document extraction and chunking

The attachment processor adapts private files to StoredDocument and
DocumentExtractor rather than duplicating all parsers.

- TXT uses charset-safe extraction and conservative normalization.
- DOCX uses TikaDocumentExtractor and its existing DOCX fallback.
- PDF uses the existing Tika extraction path first.
- Filename, page, section, and chunk index are retained when available.
- Original semantic content is not rewritten before chunking.

DocumentChunker keeps ChunkingOptions.defaults(): target 550 tokens, overlap
80 tokens, and merge threshold 120 tokens. It persists chunk index, page,
section, token estimate, and attachment ID.

When embeddings are required, processing calls EmbeddingProviderRouter with the
exact selected EmbeddingSpace. A chunk is not retrieval-ready unless the vector
has 768 dimensions and complete provider, model, dimension, and version
metadata. Cloudflare and Gemini vectors are never mixed in one search space.

## 12. Retrieval, summaries, and context budget

For a focused question, retrieve only selected attachment chunks. Defaults are
global top-k 8, diversification across selected files, and at most four chunks
from one file unless the question clearly targets that file. Query construction
reuses VectorRetrievalService conventions but applies private attachment scope.

For a whole-document summary, process chunks into bounded partial summaries and
synthesize a final summary with filename identity. For multi-file comparison,
create one grounded representation per selected document before synthesis.

The default attachment-derived context budget is 12,000 input tokens, leaving
room for system instructions, conversation history, trusted exercise context,
the question, and the response. Provider adapters may apply a smaller safe
limit. Over-budget material is retrieved, batched, or hierarchically
summarized; arbitrary trailing text is never silently discarded.

## 13. Image and vision architecture

ProviderCapability is extended additively for VISION_IMAGE. DOCUMENT_CONTEXT
may be represented as an additional provider requirement while retaining CHAT
as the base capability.

For an image-bearing turn:

1. Resolve and validate selected image attachments.
2. Open controlled original or safe preprocessed representations.
3. Resolve selected document chunks.
4. Build one provider-neutral multimodal request with the question, trusted Tutor context, actual image content, and bounded document context.
5. Route only to an adapter advertising verified VISION_IMAGE support.

AiProviderRouter remains the routing boundary. A text-only adapter is never
selected for a required image turn. AiProviderAdapter and AiChatCommand are
extended additively with provider-neutral attachment content or an equivalent
internal request object. The HTTP request still contains IDs only.

Gemini receives VISION_IMAGE only after the configured current Gemini model and
adapter are explicitly verified for image input. A provider name alone is not
evidence. If no enabled adapter supports vision, chat returns a truthful
capability error and the attachment remains READY. Text-only chat skips all
image processing.

## 14. Scanned PDF fallback

PDF processing:

1. Attempt normal Tika extraction.
2. If meaningful text exists, use the document text pipeline.
3. If extraction is empty or scan-like, render relevant pages to bounded images.
4. Send those actual page images to a verified VISION_IMAGE provider.

The default safety limit is 20 rendered pages per user operation. Larger files
use bounded batches or summaries and never silently drop later pages. No
blanket local OCR library becomes the default. Rendering or vision failure is a
truthful processing/capability error, not a successful answer.

## 15. Mixed multimodal turns

This scenario is supported:

    writing-prompt.jpg
    sample.pdf
    my-essay.docx
    Question: “So sánh bài của tôi với đề trong ảnh và bài mẫu.”

The server resolves ownership and readiness, routes the image to vision,
retrieves PDF/DOCX context privately, builds one bounded request, and preserves
file identity in the answer provenance. A text-only provider must not receive
the turn as if the image were absent.

## 16. Provenance and history

Document-derived claims expose useful provenance when available:

    [my-essay.docx]
    [sample.pdf, trang 3]

Page and section values are emitted only when actually provided by extraction or
rendering. The system never fabricates a page or file citation and never claims
a source was used unless its content entered model context.

Attachment provenance is separate from public RAG sources and is returned in an
additive attachmentSources field containing attachment ID, safe filename,
page/section when known, and chunk index when useful.

ai_messages keeps existing content, status, grounding, and citations. The
ai_message_attachments relation makes attachments durable. Conversation detail
returns safe metadata for prior chips/cards. Reloading Én does not fetch full
binary bodies merely to render history. Image previews use an authorized
endpoint or safe short-lived representation, never a public path.

## 17. Processing, failure, and recovery

Each file is independent. If four of five files reach READY and one fails, the
four remain valid. Retry and remove affect only the failed item.

The attachment stores processing start time, bounded attempt count, and a safe
error code. A configurable five-minute timeout identifies stale PROCESSING
rows. Recovery marks them FAILED or performs one bounded requeue according to
the existing job infrastructure; it never creates an unlimited retry loop.

| Situation | Result |
|---|---|
| Client selects file six | No upload; max-five message |
| Server rejects validation | Normalized 400; no READY attachment |
| Parser fails | FAILED; retry/remove available |
| Embedding fails | Not READY for document retrieval; bounded failure |
| Vision provider fails during chat | Attachment stays READY; truthful temporary chat error |
| No vision provider exists | Truthful capability error; attachment stays READY |
| Chat times out | Retry same IDs; no upload |
| Session expires | No file access; re-authentication required |

User-facing messages:

- Chỉ có thể đính kèm tối đa 5 file.
- Tệp vượt quá giới hạn 10 MB.
- Định dạng tệp chưa được hỗ trợ.
- Không thể tải tệp lên. Thử lại.
- Không thể đọc nội dung tệp.
- Én chưa thể phân tích ảnh lúc này. Vui lòng thử lại.

Raw provider errors, paths, stack traces, and credentials never reach the
browser.

## 18. Retention and cleanup

Attachments remain available while their conversation exists unless explicitly
removed. expires_at is nullable and reserved for a configured future policy.

Conversation deletion cascades or schedules attachment-link cleanup. Storage
cleanup deletes bytes only when no retained message or active attachment
references the key. Removed or expired files are rejected in new chat requests.

## 19. Performance limits

- Frontend upload concurrency is at most two jobs.
- Backend streams where practical and avoids five duplicate 10 MiB buffers.
- Document processing uses bounded worker concurrency.
- Vision payloads respect provider limits and use bounded preprocessing.
- Indexes cover owner, conversation, status, links, checksum, and embedding-space filters.
- Full-document and scanned-PDF operations use bounded batches.
- Processing state cannot remain indefinitely PROCESSING.

## 20. Implementation sequence

1. Add failing contract tests for limits, API shapes, ownership, and conversation isolation.
2. Add the additive attachment migration, repository, storage abstraction, and durable lifecycle without changing text-only chat.
3. Add server MIME detection, signatures, filename safety, image pixel limits, and max-five handling.
4. Adapt Tika extraction and DocumentChunker for private chunks and exact embedding-space metadata.
5. Add the independent frontend queue, five cards, READY gating, retry, remove, and history rendering.
6. Add attachmentIds to chat and persist ai_message_attachments.
7. Add private retrieval, full-document summarization, provenance, and context budgeting.
8. Add provider-neutral vision capability routing and verified image input.
9. Add bounded scanned-PDF rendering and mixed image/document orchestration.
10. Run acceptance, full tests, package/build, security checks, and runtime smoke.

No step changes the admin RAG corpus, provider secrets, public API keys, or
historical migrations V1 through V29.

## 21. TDD test strategy

### Frontend

Cover:

- Plus button opens a multiple file chooser with the exact allowlist.
- One through five files are accepted; file six shows the max-five message.
- A file over 10 MiB is rejected before upload.
- Five independent cards render with image thumbnails or document icons.
- Every card has an accessible remove action.
- One failed upload retries without re-uploading successful files.
- At most two upload promises are active.
- Send waits for READY and never omits failed or processing files.
- Chat retry reuses IDs and makes zero upload calls.
- Reload renders attachment metadata without loading full bodies for chips.
- Responsive Én panel remains usable at 375, 768, 1024, and 1440.
- Keyboard focus, labels, announcements, and reduced motion remain accessible.

All frontend tests mock APIs and never call a live provider.

### Backend

Cover valid PDF, DOCX, TXT, PNG, JPG/JPEG, and WEBP; empty, oversized,
unsupported, extension/MIME mismatch, magic-byte mismatch, malformed documents,
malicious filenames, traversal, active content, and pixel-limit failures.

Also cover max-five, duplicate IDs, owner isolation, conversation isolation,
READY/REMOVED/EXPIRED enforcement, lifecycle persistence, checksum/storage,
message links, cleanup, stale PROCESSING recovery, parser outcomes, provenance,
exact vector dimension, and embedding-space isolation.

### AI and security

Cover unchanged text-only chat, vision-capable routing, text-only-provider
rejection for image turns, truthful no-vision errors, document retrieval,
whole-document summaries, multi-document comparison, mixed turns, scanned-PDF
fallback, budget handling, provider failure without invalidating READY files,
retry without re-upload, and provenance without fabricated pages/files.

Security tests cover User A versus User B, conversation A versus B, path
traversal, unsupported active content, and decompression/image guards. No
automated test consumes live provider quota.

## 22. Mandatory acceptance cases

1. Upload five supported files in one selection; all independently reach READY.
2. Select a sixth; it is rejected with the max-five message.
3. Select a file over 10 MiB; client and bypassed server request are rejected.
4. Attach a real PNG/JPG and ask “Ảnh này có gì?”; real vision succeeds or returns a truthful configured-capability error.
5. Attach TXT/DOCX/PDF containing a unique fact; Én answers from extracted content.
6. Attach multiple documents and ask for comparison; the response distinguishes files.
7. Attach image plus PDF plus DOCX; the real mixed route succeeds.
8. Submit a scanned PDF; bounded vision fallback succeeds or returns a truthful error.
9. Reload the browser; attachment metadata persists in conversation history.
10. Retry failed chat; no attachment upload occurs again.
11. Fail one of five uploads; retry/remove only that file and keep the other four.
12. User A attempts User B's UUID; request is denied.
13. Conversation A attempts Conversation B's attachment; request is denied.
14. Restart backend; durable metadata and history remain valid.

## 23. Definition of done

The feature is complete only when:

- Maximum five files works client-side and server-side.
- 10 MiB per file is enforced client-side and server-side.
- Supported types are validated by content, not only extension.
- Real image understanding works through a verified vision provider.
- Real PDF/DOCX/TXT understanding works through extracted content.
- Mixed turns work with distinct provenance.
- Metadata and history survive reload and backend restart.
- Retry does not re-upload.
- Ownership and conversation isolation tests pass.
- No fake AI response or fake upload state exists.
- Text-only Én chat remains intact.
- Frontend tests, lint, and build pass.
- Backend tests and package pass.
- Runtime smoke covers upload, document, image, mixed, retry, and denial cases.

## 24. Consistency self-review

The spec was checked against the approved constraints:

- The endpoint accepts one to five files while the frontend normally uses independent jobs with concurrency two.
- Five files and 10 MiB per file are consistent across selection, API, lifecycle, security, tests, and acceptance.
- Chat carries IDs only; binary content is resolved server-side.
- READY means processing is complete for the attachment capability, not that the model has inspected it.
- Processing failure is separate from provider failure; a READY file remains reusable after provider failure.
- Provenance is emitted only for content included in context and never fabricates page numbers.
- Private retrieval filters owner, conversation, selected IDs, status, and exact embedding space and never mixes with admin RAG.
- Existing TutorAttachmentService, TikaDocumentExtractor, DocumentChunker, EmbeddingProviderRouter, AiProviderRouter, ConversationService, and frontend API modules remain the integration boundaries.
- The scope is limited to Én multimodal chat attachments and does not include unrelated Tutor, learning, admin, authentication, or deployment changes.
