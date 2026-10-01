# Phase 2A — Spring Boot Gemini Tutor Chat

**Goal:** Replace the Phase 1 deterministic tutor response with a provider-independent REST contract backed by a server-side Gemini provider, without starting Phase 2B RAG or writing assessment work.

**Architecture:** React calls `/api/ai/chat` through the Vite proxy. Spring Boot validates and normalizes the request, delegates through `AiProvider`, and maps Gemini output and failures into the application contract. The provider owns all Google-specific HTTP details and secrets.

**Constraints:** Java 21, Maven, Spring Boot 4.1.1, Web MVC/WebFlux, validation, no database, auth, RAG, embeddings, scoring, or vendor payloads in React. Keep the existing Academic Luxury tutor UI, SkeletonBlock loading, keyboard behavior, insufficient-context state, and responsive behavior.

## Implementation sequence

1. **Backend scaffold and red tests**
   - Add a minimal Maven Spring Boot project, environment example, and safe ignore rules.
   - Write controller, service, and Gemini provider tests before production classes.
   - Expected: backend tests fail because the requested API classes do not exist yet.

2. **Backend contract and orchestration**
   - Add validated request/context/history records, normalized response/error records, controller advice, `AiProvider`, and `AiChatService`.
   - Normalize trimmed input, bound history, preserve the explicit insufficient-context response, and always return empty sources with `NOT_ENABLED` grounding in Phase 2A.
   - Expected: controller/service tests pass.

3. **Gemini provider**
   - Add `@ConfigurationProperties`, WebClient configuration, one central truthful tutor instruction, and `GeminiAiProvider`.
   - Map history to Gemini roles, send `x-goog-api-key` server-side, parse only answer text, and map validation/auth/transient/timeout/malformed failures without leaking provider details.
   - Expected: stub-server provider tests pass, including bounded transient retry.

4. **Frontend API integration**
   - Write/update tutor tests for fetch contract, loading, success, insufficient context, friendly errors, empty sources, duplicate-submit blocking, and keyboard submit.
   - Add `aiTutorApi.js`, the Vite `/api` proxy, and replace deterministic mock behavior in `FloatingTutor` while preserving existing UI structure.
   - Expected: targeted tutor tests fail before implementation, then pass; no frontend test depends on a backend process or real key.

5. **Documentation and verification**
   - Add concise local-run instructions and `.env.example` usage.
   - Run backend tests/package, frontend tests/lint/build, inspect git diff, and verify no frontend key/provider URL or Phase 2B artifacts were introduced.

## Review focus

- No raw Gemini request/response shape crosses the backend boundary.
- No fake Phase 2A citations or provider-specific state is rendered.
- Missing key starts the app but returns a safe temporary-unavailable error for chat.
- Retry is small and bounded; 400/auth/validation failures are not retried.
- One H1, routes, visuals, and Phase 1 data behavior remain untouched.
