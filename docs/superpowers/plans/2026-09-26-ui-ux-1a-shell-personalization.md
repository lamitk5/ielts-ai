# UI/UX-1A Product Shell and Personalization Implementation Plan

> For agentic workers: REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox syntax for tracking.

**Goal:** Build the reusable authenticated product shell, preference system, auth gate, viewport-safe Tutor shell, and bounded attachment UX required by UI/UX Phase 1.

**Architecture:** Extend the existing React/Vite shell instead of introducing a second layout or state system. Preferences are exposed through a frontend PreferenceProvider, rendered through semantic CSS tokens, persisted to versioned local storage for guests and to a server-authoritative API for authenticated users. Tutor and attachment behavior remains provider-neutral; the backend derives user ownership from AuthPrincipal.

**Tech Stack:** React, React Router, Vite, Vitest, Testing Library, existing CSS token system, Lucide React, Spring Boot MVC, JDBC repositories, Flyway.

**Spec:** docs/superpowers/specs/2026-09-26-ui-ux-phase-1-learning-experience-design.md

## Global Constraints

- Preserve Academic Luxury, Playfair Display headings, Inter body/UI, and the existing shared Button, GlassCard, AnimatedSection, SectionTitle, and SkeletonBlock components.
- Do not add a 3D engine, large state manager, provider-specific frontend payloads, or a new styling system.
- Preferences use semantic tokens; components must not branch directly on accent names.
- Guest preferences use versioned local storage; authenticated preferences use asynchronous server autosave and server-authoritative responses.
- Server preferences win after login; no silent guest/account merge; logout clears account-scoped client state.
- Personalized Tutor requests and attachments require backend authentication; frontend AuthGate is not a security boundary.
- Attachment defaults are PDF, DOCX, TXT; 10 MiB per file; one active attachment per Tutor request; frontend validation is UX-only.
- UI/UX-1 does not implement automatic RAG ingestion, extraction, indexing, generation, persistent audio, STT, or Phase 3 content workflows.
- Tutor pending HTTP requests show one honest generic state; staged states require future emitted backend events and no fake timer.
- All asynchronous UI paths settle through response, timeout, cancel, retry, unavailable, or error.
- All new behavior must support 375px, 768px, 1024px, and 1440px+ layouts, keyboard access, focus-visible states, and reduced motion.
- External AI calls remain mocked in tests and consume zero provider quota.

## Review Focus

1. Account switch preference leak: switching users or logging out must remove account-scoped preferences, Tutor messages, attachments, and session snapshots; test the boundary in AuthProvider/preference integration.
2. Mobile Tutor composer clipping: the composer, send/cancel controls, and close control must remain reachable inside 100dvh-bounded mobile Tutor states; test at 375px and with a long message list.
3. Guest AI bypass: direct calls to /api/ai/chat and attachment endpoints without an authenticated principal must return normalized 401; test controller/interceptor behavior independently of AuthGate.
4. Malformed local storage: malformed, old-version, or storage-throwing preference records must fall back to defaults without blocking render; test parser and provider initialization.
5. False persistence success: failed authenticated preference saves must preserve the immediate preview but expose “Chưa đồng bộ” and retain the last confirmed server value; test success, conflict, and failure paths.

---

### Task 1: Define preference contract and semantic token foundation

Files:
- Create: frontend/src/features/preferences/preferenceDefaults.js
- Create: frontend/src/features/preferences/preferenceSchema.js
- Create: frontend/src/features/preferences/PreferenceProvider.jsx
- Create: frontend/src/features/preferences/preferenceStorage.js
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/preferences.test.jsx

Interfaces:
- Consumes: useAuth() from frontend/src/features/auth/AuthProvider.jsx; existing window.matchMedia test mock.
- Produces: PreferenceProvider, usePreferences(), DEFAULT_PREFERENCES, PREFERENCE_STORAGE_KEY, normalizePreferences(value), and applyPreferenceTokens(preferences).

- [ ] Step 1: Write failing tests for default values, allowlisted theme/accent/font/density/motion enums, ratios limited to 40 | 50 | 60, immediate preview state, and semantic token application without accent-name branching.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/preferences.test.jsx; expected failure because the preference module and provider do not exist.
- [ ] Step 3: Implement the minimal contract. Use a versioned schema with themeMode, accentPreset, fontScale, density, reduceMotion, proactiveAiEnabled, crossHighlightEnabled, timerDefaultEnabled, readingSplitRatio, and writingSplitRatio. Map the selected preset to semantic CSS custom properties through a token table, not component conditionals.
- [ ] Step 4: Run and verify GREEN with the targeted test; expected all preference contract tests pass.
- [ ] Step 5: Commit with git add frontend/src/features/preferences frontend/src/styles/globals.css frontend/src/__tests__/preferences.test.jsx && git commit -m "feat: add preference token foundation".

### Task 2: Implement guest and authenticated preference persistence

Files:
- Create: frontend/src/services/preferencesApi.js
- Modify: frontend/src/features/preferences/preferenceStorage.js
- Modify: frontend/src/features/preferences/PreferenceProvider.jsx
- Modify: frontend/src/features/auth/AuthProvider.jsx
- Test: frontend/src/__tests__/preferences-persistence.test.jsx
- Test: frontend/src/__tests__/auth.test.jsx

Interfaces:
- Consumes: GET /api/user/preferences, PUT /api/user/preferences; AuthProvider session and logout lifecycle.
- Produces: getPreferences(), savePreferences(preferences, version), clearAccountPreferenceCache(userId), and a provider status of idle | loading | saving | synced | unsynced | conflict.

- [ ] Step 1: Write failing tests for guest localStorage versioning, malformed records, storage exceptions, immediate apply, debounced authenticated save, server-wins login hydration, conflict reconciliation, failed-save “Chưa đồng bộ”, and logout cache clearing.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/preferences-persistence.test.jsx src/__tests__/auth.test.jsx; expected failures for missing persistence functions and provider behavior.
- [ ] Step 3: Implement guest localStorage and account-namespaced cache. Guest writes occur immediately. Authenticated changes update the preview immediately, debounce network writes where rapid presentation changes occur, send the full allowlisted record plus version, and replace local state with the authoritative response.
- [ ] Step 4: Update logout integration so account-scoped preference cache and Tutor/session stores are cleared without deleting public defaults. Do not merge guest settings silently into an account.
- [ ] Step 5: Run and verify GREEN with the targeted tests; expected all persistence and logout cases pass.
- [ ] Step 6: Commit with git add frontend/src/features/preferences frontend/src/services/preferencesApi.js frontend/src/features/auth/AuthProvider.jsx frontend/src/__tests__/preferences-persistence.test.jsx frontend/src/__tests__/auth.test.jsx && git commit -m "feat: persist learner preferences safely".

### Task 3: Add server-authoritative user preferences API

Files:
- Create: backend/src/main/java/com/ieltsaitutor/preferences/UserPreferences.java
- Create: backend/src/main/java/com/ieltsaitutor/preferences/UserPreferencesController.java
- Create: backend/src/main/java/com/ieltsaitutor/preferences/UserPreferencesService.java
- Create: backend/src/main/java/com/ieltsaitutor/preferences/UserPreferencesRepository.java
- Create: backend/src/main/java/com/ieltsaitutor/preferences/JdbcUserPreferencesRepository.java
- Create: backend/src/test/java/com/ieltsaitutor/preferences/UserPreferencesControllerTest.java
- Create: backend/src/test/java/com/ieltsaitutor/preferences/UserPreferencesServiceTest.java
- Plan-only migration target: backend/src/main/resources/db/migration/V7__create_user_preferences.sql
- Modify: backend/src/main/java/com/ieltsaitutor/auth/AuthInterceptor.java

Interfaces:
- Consumes: AuthPrincipal.userId() from backend/src/main/java/com/ieltsaitutor/auth/AuthPrincipal.java; JDBC patterns from JdbcAuthUserRepository and Flyway V2–V6 schemas.
- Produces: GET /api/user/preferences, PUT /api/user/preferences, UserPreferencesService.get(UUID), and UserPreferencesService.update(UUID, UserPreferences, long expectedVersion).

- [ ] Step 1: Write failing backend tests for unauthenticated 401, authenticated principal-derived ownership, default record creation, enum allowlist, ratios only 40/50/60, optimistic version conflict, and authoritative full-record response.
- [ ] Step 2: Run and verify RED with .\mvnw.cmd -Dtest=UserPreferencesControllerTest,UserPreferencesServiceTest test; expected compilation/test failure because the preference API does not exist.
- [ ] Step 3: Define the implementation contract around a future V7__create_user_preferences.sql table keyed by user_id with preference columns, version, updated_at, and a foreign key to app_users. The migration is planned here only and must not be created during this planning task.
- [ ] Step 4: Implement service/controller/repository behavior so the authenticated principal is the only user identity, invalid fields are rejected with normalized errors, and updates return the complete server record. Add /api/user/preferences to AuthInterceptor.requiresAuthentication.
- [ ] Step 5: Run and verify GREEN with the targeted Maven tests; expected ownership, validation, and conflict tests pass.
- [ ] Step 6: Commit with git add backend/src/main/java/com/ieltsaitutor/preferences backend/src/main/java/com/ieltsaitutor/auth/AuthInterceptor.java backend/src/test/java/com/ieltsaitutor/preferences && git commit -m "feat: add user preferences contract".

### Task 4: Build accessible SettingsDrawer

Files:
- Create: frontend/src/components/settings/SettingsButton.jsx
- Create: frontend/src/components/settings/SettingsDrawer.jsx
- Create: frontend/src/components/settings/PreferenceControlGroup.jsx
- Modify: frontend/src/components/layout/Navbar.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/settings-drawer.test.jsx

Interfaces:
- Consumes: usePreferences(); useAuth(); useLocation(); existing Navbar mobile-menu state and focus-visible CSS.
- Produces: SettingsButton, SettingsDrawer({ open, onClose, openerRef }), focus trap/return behavior, immediate preview controls, and reset confirmation.

- [ ] Step 1: Write failing tests for opening from the authenticated shell, dialog naming, Escape/backdrop close, focus trap, focus return, keyboard controls, immediate preview, reset confirmation, and mobile drawer semantics.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/settings-drawer.test.jsx; expected failure because the settings components do not exist.
- [ ] Step 3: Implement the drawer as a reusable dialog using existing buttons and tokens. Each preference change calls the provider immediately; reset requires confirmation. Show saving, synced, unsynced, and conflict status without introducing a Save button.
- [ ] Step 4: Add responsive CSS for right-side desktop drawer and mobile full-height/bottom-sheet presentation, including prefers-reduced-motion behavior and safe focus visibility.
- [ ] Step 5: Run and verify GREEN with the targeted test; expected keyboard and persistence status tests pass.
- [ ] Step 6: Commit with git add frontend/src/components/settings frontend/src/components/layout/Navbar.jsx frontend/src/styles/globals.css frontend/src/__tests__/settings-drawer.test.jsx && git commit -m "feat: add accessible settings drawer".

### Task 5: Add authenticated AuthGate for personalized Tutor

Files:
- Create: frontend/src/components/auth/AuthGate.jsx
- Create: frontend/src/components/auth/ProtectedAction.jsx
- Modify: frontend/src/components/tutor/FloatingTutor.jsx
- Modify: frontend/src/services/aiTutorApi.js
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/auth-gate.test.jsx

Interfaces:
- Consumes: useAuth(), useLocation(), useNavigate(), and existing FloatingTutor context.
- Produces: AuthGate({ children, reason, returnTo }), normalized AUTH_REQUIRED handling, and safe return paths containing only same-origin route state.

- [ ] Step 1: Write failing tests for guest Tutor opening, login/register actions preserving a safe return path, authenticated rendering, and rejection of an unsafe external return path.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/auth-gate.test.jsx; expected missing component/behavior failures.
- [ ] Step 3: Implement the UX gate so guests see an explanatory panel and cannot send a personalized request or attach private context. Keep existing public navigation intact.
- [ ] Step 4: Normalize 401 AUTH_REQUIRED in aiTutorApi.js without exposing backend/provider details; authenticated users retain existing generic/grounded chat behavior.
- [ ] Step 5: Run and verify GREEN with the targeted test and existing Tutor tests; expected guest and authenticated paths pass.
- [ ] Step 6: Commit with git add frontend/src/components/auth frontend/src/components/tutor/FloatingTutor.jsx frontend/src/services/aiTutorApi.js frontend/src/styles/globals.css frontend/src/__tests__/auth-gate.test.jsx && git commit -m "feat: gate personalized tutor access".

### Task 6: Establish the viewport-safe Tutor shell

Files:
- Create: frontend/src/components/tutor/TutorShell.jsx
- Create: frontend/src/components/tutor/ContextBadge.jsx
- Create: frontend/src/components/tutor/TutorComposer.jsx
- Create: frontend/src/components/tutor/TutorQuickActions.jsx
- Create: frontend/src/components/tutor/TutorMessageList.jsx
- Create: frontend/src/components/tutor/TimeoutRetry.jsx
- Modify: frontend/src/components/tutor/FloatingTutor.jsx
- Modify: frontend/src/components/tutor/TutorPanel.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/tutor-shell.test.jsx

Interfaces:
- Consumes: existing messages, loading, onSend, onClose, context, SourceChip, TutorMessageSkeleton, and normalized aiTutorApi results.
- Produces: TutorShell({ state, context, messages, requestState, onSend, onClose, onCancel, onRetry, onStateChange }); shell states CLOSED | COMPACT | STANDARD | EXPANDED | FULLSCREEN_DESKTOP | FULLSCREEN_MOBILE.

- [ ] Step 1: Write failing tests for all shell states, context badge, general-question mode, source chips, close/minimize/expand, Escape, focus return, timeout/retry/cancel/unavailable, bounded message history, and composer reachability at mobile viewport height.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/tutor-shell.test.jsx; expected failure because the shell components do not exist.
- [ ] Step 3: Implement the shell composition by extracting responsibilities from TutorPanel without changing the existing provider-neutral message contract. Keep TutorMessageSkeleton as the loading primitive and use app-owned quick actions only.
- [ ] Step 4: Implement stale-context behavior in the shell: show the trusted badge from app context, provide Hỏi chung/clear-context behavior, and mark context stale on navigation instead of silently reusing it.
- [ ] Step 5: Add viewport-safe CSS with an independently scrollable message body, max-height min(42rem, calc(100dvh - 1.5rem)), mobile 100dvh/safe-area handling, and a composer that remains visible.
- [ ] Step 6: Run and verify GREEN with the targeted shell tests and existing tutor.test.jsx, tutor-rag.test.jsx, and rag-accessibility.test.jsx.
- [ ] Step 7: Commit with git add frontend/src/components/tutor frontend/src/styles/globals.css frontend/src/__tests__/tutor-shell.test.jsx && git commit -m "feat: add responsive Tutor shell".

### Task 7: Add bounded attachment shell and normalized lifecycle

Files:
- Create: frontend/src/components/tutor/AttachmentComposer.jsx
- Create: frontend/src/components/tutor/AttachmentStatus.jsx
- Create: frontend/src/services/tutorAttachmentsApi.js
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentController.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachmentService.java
- Create: backend/src/main/java/com/ieltsaitutor/ai/attachment/TutorAttachment.java
- Create: backend/src/test/java/com/ieltsaitutor/ai/attachment/TutorAttachmentControllerTest.java
- Modify: backend/src/main/java/com/ieltsaitutor/auth/AuthInterceptor.java
- Modify: frontend/src/components/tutor/TutorComposer.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/tutor-attachments.test.jsx
- Plan-only migration target if durable metadata is required: backend/src/main/resources/db/migration/V8__create_tutor_attachments.sql

Interfaces:
- Consumes: AuthPrincipal.userId(), multipart request handling, TutorShell, and existing normalized error conventions.
- Produces: POST /api/ai/attachments, GET /api/ai/attachments/{id}, DELETE /api/ai/attachments/{id}; frontend lifecycle SELECTED | UPLOADING | UPLOADED | PROCESSING | READY | FAILED | REMOVED | EXPIRED.

- [ ] Step 1: Write failing frontend/backend tests for PDF/DOCX/TXT acceptance, 10 MiB boundary, one-active-file limit, HTML/executable rejection, normalized failure, ownership, and lifecycle rendering.
- [ ] Step 2: Run and verify RED with npm test -- --run src/__tests__/tutor-attachments.test.jsx and .\mvnw.cmd -Dtest=TutorAttachmentControllerTest test; expected failure because attachment components/API do not exist.
- [ ] Step 3: Define the service boundary. The browser sends only the selected file and safe metadata; server identity comes from AuthPrincipal; frontend checks are advisory; backend enforces MIME/content, size, ownership, and one active attachment.
- [ ] Step 4: Implement the normalized attachment lifecycle with remove/retry/cancel behavior. Do not invoke RAG ingestion, indexing, test generation, or Phase 3 workflows.
- [ ] Step 5: Add the authenticated backend endpoints and use a dedicated ownership-scoped attachment service/store. If durable metadata is needed, use the planned V8 migration target; do not modify migrations during this planning task.
- [ ] Step 6: Run and verify GREEN with both targeted test commands and existing AI/auth regression tests.
- [ ] Step 7: Commit with git add frontend/src/components/tutor frontend/src/services/tutorAttachmentsApi.js backend/src/main/java/com/ieltsaitutor/ai/attachment backend/src/main/java/com/ieltsaitutor/auth/AuthInterceptor.java frontend/src/styles/globals.css frontend/src/__tests__/tutor-attachments.test.jsx backend/src/test/java/com/ieltsaitutor/ai/attachment && git commit -m "feat: add bounded Tutor attachments".

### Task 8: Integrate shell foundations and verify responsive/accessibility boundaries

Files:
- Modify: frontend/src/App.jsx
- Modify: frontend/src/components/layout/AppLayout.jsx
- Modify: frontend/src/components/layout/Navbar.jsx
- Modify: frontend/src/components/tutor/FloatingTutor.jsx
- Modify: frontend/src/styles/globals.css
- Test: frontend/src/__tests__/app.test.jsx
- Test: frontend/src/__tests__/shared-ui.test.jsx
- Test: frontend/src/__tests__/rag-accessibility.test.jsx
- Test: backend/src/test/java/com/ieltsaitutor/auth/AuthApplicationStartupTest.java

Interfaces:
- Consumes: Tasks 1–7 contracts.
- Produces: one shell provider composition, no duplicate preference/Tutor state, and route-level responsive/accessibility verification.

- [ ] Step 1: Write failing integration tests for provider composition, authenticated Settings entry, guest Tutor gate, mobile nav coexistence, reduced motion, no horizontal overflow, and startup with optional AI credentials missing.
- [ ] Step 2: Run and verify RED with targeted frontend tests and the backend startup test; expected failures until shell integration is wired.
- [ ] Step 3: Integrate providers once near App.jsx/AppLayout without creating a second auth or theme provider. Preserve current route structure and homepage functionality.
- [ ] Step 4: Verify 375/768/1024/1440+ behavior with browser-capable checks when available and CSS/test assertions where visual tooling is unavailable.
- [ ] Step 5: Run and verify GREEN with npm test -- --run, npm run lint, npm run build, .\mvnw.cmd test, and .\mvnw.cmd package; expected no regression in existing AI/RAG/auth flows.
- [ ] Step 6: Commit with git add frontend/src backend/src && git commit -m "feat: integrate UI shell foundations".

## Plan-level Verification

Run from the repository root after all tasks:

- npm test -- --run
- npm run lint
- npm run build
- .\mvnw.cmd test
- .\mvnw.cmd package
- git diff --check

Do not run live provider calls. Do not implement AI Phase 2 or Phase 3.

