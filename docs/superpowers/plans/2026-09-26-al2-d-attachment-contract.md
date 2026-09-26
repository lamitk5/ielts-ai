# Academic Luxury 2.0 — Attachment Contract Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align the frontend and backend attachment contract so supported documents and images behave consistently and never imply unsupported vision capability.

**Architecture:** Keep the backend authoritative and authenticated. Define a small explicit contract module on the frontend and a backend allowlist/validator used by the existing attachment service. Preserve the current one-active-attachment policy and normalized error boundary; extend the response/lifecycle additively for images without changing Tutor chat payloads.

**Tech Stack:** React, Spring Boot MVC, MultipartFile, Vitest, JUnit/MockMvc, existing attachment service/API.

**Spec:** `docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

## Global Constraints

- Backend validation and ownership are authoritative; frontend validation is advisory UX.
- Accept PDF, DOCX, TXT, PNG, JPG, JPEG, and WEBP; maximum 10 MiB; one active attachment per Tutor request.
- Reject SVG, HTML, JS, executable/script files, arbitrary binaries, empty files, and unsafe paths.
- Image upload support is not vision. Use `IMAGE_READY` and `VISION_NOT_ENABLED` until a real multimodal provider exists.
- Do not add a provider, vision path, ingestion/indexing workflow, migration, or dependency.
- Preserve existing AuthPrincipal ownership, normalized errors, removal, expiry, and Tutor provider-neutral contract.

## Review Focus

1. A real JPG and WEBP must pass backend validation while spoofed or unsupported content fails.
2. A file just over 10 MiB must fail; exactly 10 MiB must pass if otherwise valid.
3. A second active file must follow the existing approved one-active behavior and never leak ownership.
4. Another authenticated account must not read or delete the first account’s attachment.
5. Accepted images must never render or return a claim that a provider visually analyzed them.

### Task 1: Define one frontend attachment contract and UX validator

**Files:**
- Create: `frontend/src/features/tutor/attachmentContract.js`
- Modify: `frontend/src/services/tutorAttachmentsApi.js`
- Modify: `frontend/src/components/tutor/AttachmentComposer.jsx`
- Modify: `frontend/src/components/tutor/AttachmentStatus.jsx`
- Test: `frontend/src/__tests__/tutor-attachments.test.jsx`

**Interfaces:**
- Consumes: existing `AttachmentApiError`, `AttachmentComposer`, `AttachmentStatus`, and upload endpoint.
- Produces: `ATTACHMENT_CONTRACT`, `ATTACHMENT_STATUS`, `validateAttachmentFile(file)`, `getAttachmentPresentation(metadata)`, and `uploadAttachment(file, { requestId, signal })` with documents/images, 10 MiB, and one-active-per-request semantics.

- [ ] **Step 1: Write failing tests** for accepted PDF/DOCX/TXT/PNG/JPG/JPEG/WEBP, exact/over size, unsupported/SVG/HTML/JS, image thumbnail metadata, document icon metadata, and friendly normalized errors.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/tutor-attachments.test.jsx`; expected failure for the shared contract and image-aware presentation.
- [ ] **Step 3: Implement the minimal contract module** and make the API/composer/status consume it rather than maintaining drifting extension/MIME arrays.
- [ ] **Step 4: Run GREEN** with targeted frontend tests and ensure `accept` mirrors the contract without treating it as security validation.
- [ ] **Step 5: Commit** with `git add frontend/src/features/tutor/attachmentContract.js frontend/src/services/tutorAttachmentsApi.js frontend/src/components/tutor/AttachmentComposer.jsx frontend/src/components/tutor/AttachmentStatus.jsx frontend/src/__tests__/tutor-attachments.test.jsx && git commit -m "feat: define shared attachment UX contract"`.

### Task 2: Extend backend MIME/content validation for images

**Files:**
- Create: `backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentContract.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentService.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentController.java`
- Modify: `backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachment.java`
- Modify: `backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java`

**Interfaces:**
- Consumes: authenticated `AuthPrincipal`, `MultipartFile`, current `TutorAttachmentService` lifecycle, and normalized `AuthException` errors.
- Produces: one backend allowlist for document/image extension and MIME agreement, safe image metadata/capability state, and unchanged ownership-scoped endpoints. `POST /api/ai/attachments` accepts a bounded opaque `requestId` form field; the service enforces one active attachment per `(authenticated userId, requestId)` without trusting a client user ID.

- [ ] **Step 1: Write failing backend tests** for JPG/WEBP success, exact 10 MiB boundary, oversize, spoofed MIME/extension, SVG/HTML/JS/executable rejection, empty files, and normalized error codes.
- [ ] **Step 2: Run RED**: `.\mvnw.cmd -Dtest=TutorAttachmentControllerTest test`; expected failure because backend currently rejects images.
- [ ] **Step 3: Implement the bounded contract/validator**; check size, extension/MIME consistency where available, safe filename metadata, authenticated ownership, and current one-active-file rule. Add no storage, migration, or provider behavior.
- [ ] **Step 4: Run GREEN** with targeted backend tests and existing auth/AI attachment regression tests.
- [ ] **Step 5: Commit** with `git add backend/src/main/java/com/ieltsaitutor/ai/attachment backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java && git commit -m "fix: align backend attachment MIME contract"`.

### Task 3: Add image-ready and no-vision lifecycle presentation

**Files:**
- Modify: `frontend/src/components/tutor/AttachmentStatus.jsx`
- Modify: `frontend/src/components/tutor/TutorComposer.jsx`
- Modify: `frontend/src/components/tutor/TutorShell.jsx`
- Modify: `frontend/src/services/tutorAttachmentsApi.js`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/tutor-attachments.test.jsx`
- Test: `frontend/src/__tests__/tutor-shell.test.jsx`

**Interfaces:**
- Consumes: backend attachment metadata/status, the bounded request identifier owned by the current Tutor composer, and existing Tutor composer callbacks.
- Produces: compact image thumbnail/document chip, retry/remove/expiry state, `IMAGE_READY`, and `VISION_NOT_ENABLED` copy without claiming visual analysis or changing the AI chat request schema.

- [ ] **Step 1: Write failing tests** for lifecycle transitions, image-ready copy, no-vision copy, one active attachment, retry/remove, and composer reachability at mobile height.
- [ ] **Step 2: Run RED**: `npm test -- --run src/__tests__/tutor-attachments.test.jsx src/__tests__/tutor-shell.test.jsx`.
- [ ] **Step 3: Implement additive lifecycle/presentation behavior**; normalize server errors to unsupported/too-large/failed/expired/unauthorized messages and keep upload separate from Tutor analysis.
- [ ] **Step 4: Run GREEN** with targeted tests plus existing Tutor/RAG accessibility tests.
- [ ] **Step 5: Commit** with `git add frontend/src/components/tutor frontend/src/services/tutorAttachmentsApi.js frontend/src/styles/globals.css frontend/src/__tests__/tutor-attachments.test.jsx frontend/src/__tests__/tutor-shell.test.jsx && git commit -m "feat: clarify image attachment capability"`.

### Task 4: Verify ownership and cross-layer upload behavior

**Files:**
- Modify: `frontend/src/__tests__/tutor-attachments.test.jsx`
- Modify: `backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java`
- Test: `frontend/src/__tests__/auth-gate.test.jsx`
- Test: `backend/src/test/java/com/ieltsaitutor/auth/AuthApplicationStartupTest.java`

**Interfaces:**
- Consumes: Tasks 1–3 attachment contract, AuthGate, AuthPrincipal ownership, and normalized errors.
- Produces: integration-level evidence for authenticated upload, cross-account denial, no provider leakage, and optional-credential startup.

- [ ] **Step 1: Write failing integration assertions** for JPG/WEBP real controller upload, second active attachment, account A/B isolation, guest AUTH_REQUIRED, and no-vision response semantics.
- [ ] **Step 2: Run RED** with targeted frontend/backend attachment/auth commands.
- [ ] **Step 3: Make only confirmed contract fixes**; do not add multimodal provider support or durable migration.
- [ ] **Step 4: Run GREEN**: targeted tests, `.\mvnw.cmd test`, and `npm test -- --run`.
- [ ] **Step 5: Commit** with `git add frontend/src backend/src && git commit -m "test: verify attachment contract boundaries"`.

### Task 5: Attachment responsive/accessibility gate

**Files:**
- Modify: `frontend/src/components/tutor/AttachmentComposer.jsx`
- Modify: `frontend/src/components/tutor/AttachmentStatus.jsx`
- Modify: `frontend/src/styles/globals.css`
- Test: `frontend/src/__tests__/tutor-attachments.test.jsx`
- Test: `frontend/src/__tests__/rag-accessibility.test.jsx`

**Interfaces:**
- Consumes: final attachment lifecycle and semantic tokens.
- Produces: verified keyboard, focus-visible, screen-reader status, reduced-motion, and 375/768/1024/1440 behavior.

- [ ] **Step 1: Write failing assertions** for labels, status announcements, remove/retry keyboard access, thumbnail alternative text, reduced-motion, and no horizontal overflow.
- [ ] **Step 2: Run RED** with targeted accessibility/attachment tests.
- [ ] **Step 3: Implement only accessibility/layout fixes**.
- [ ] **Step 4: Run GREEN**: `npm test -- --run`, `npm run lint`, `npm run build`, and manual responsive checks.
- [ ] **Step 5: Commit** with `git add frontend/src && git commit -m "test: harden attachment accessibility"`.

## Plan Verification

Run before AL2-E:

- `npm test -- --run`
- `npm run lint`
- `npm run build`
- `.\mvnw.cmd test`
- `.\mvnw.cmd package`
- `git diff --check`
