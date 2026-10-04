# IELTS AI Tutor backend — integrated platform

This Spring Boot service exposes the provider-independent `POST /api/ai/chat` contract, persistent authentication, onboarding, four-skill practice/submission APIs, learner progress/history, Admin workflows, Tutor conversation memory and a governed RAG/attachment workflow. Documents remain pending review until an administrator approves rights, indexes the approved version, and activates it. Gemini, Groq and Cloudflare credentials stay on the server; automated tests use deterministic adapters.

PowerShell local run:

```powershell
cd backend
$env:GEMINI_API_KEY="your-real-key"
$env:GEMINI_MODEL="gemini-3.8-flash"
./mvnw.cmd spring-boot:run
```

Local RAG infrastructure:

```powershell
docker compose -f backend/docker-compose.yml up -d
```

The Compose service provides PostgreSQL with the `vector` extension. Flyway applies the additive migration chain currently present in `backend/src/main/resources/db/migration`, through V70. V6 preserves old chunks for audit/rollback but marks indexed content without an exact 768-dimensional provider/model/version profile as `REINDEX_REQUIRED`; retrieval excludes it until reindex and activation succeed. Set `RAG_DB_HOST`, `RAG_DB_PORT`, `RAG_DB_NAME`, `RAG_DB_USER`, and `RAG_DB_PASSWORD` when using a non-default database.

AI Phase 1 provider order is configured with `AI_PRIMARY_PROVIDER` and `AI_FALLBACK_PROVIDERS` (default Groq, Cloudflare, Gemini). Provider credentials and models are optional, and the service starts when any or all optional providers are absent. Chat fallback is bounded, health cooldown prevents repeated transient storms, and the frontend receives only normalized provider-neutral statuses. Tutor quota defaults are 10 guest requests/minute and 30 authenticated requests/minute; deterministic application-data answers do not call an external provider.

The current integrated branch includes persistent Tutor conversation history and attachment records. Speaking remains deliberately honest about provider boundaries: local recording/transcript review is supported, while pronunciation scoring is not claimed without a configured audio-capable provider. Deployment automation is outside this repository's local development scope.

Authentication endpoints are `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, and `/api/auth/me`. Protected learning writes use the Bearer session returned by login/register. Roles are `CUSTOMER` and `ADMIN`; RAG admin requests require an authenticated ADMIN session. The `X-Admin-Token` path remains only as a local compatibility adapter and should not be used as a production identity mechanism.

Manifest ingestion uses the same upload and extraction pipeline as the admin API and never approves or activates a source:

```powershell
$env:RAG_CLI="ingest"
$env:RAG_MANIFEST="backend/rag-data/manifest.yml"
./mvnw.cmd spring-boot:run
```

For the temporary local CMS, configure a token and start the backend:

```powershell
$env:RAG_ADMIN_TOKEN="local-admin-token"
./mvnw.cmd spring-boot:run
```

Open `/admin/rag` in the frontend. An authenticated ADMIN session is preferred; a token configured with `RAG_ADMIN_TOKEN` remains available for local compatibility. The operational sequence is upload → preview → approve rights → index → activate. Deactivate or restrict a source when it should no longer be eligible for Tutor retrieval. Tokens are never returned by the API.

Manual verification can exercise the lifecycle with the sample manifest under `backend/rag-data/`. Live provider verification requires credentials, quota, and a running frontend proxy; it is intentionally not treated as an automated test pass. Never retry a live 429 storm.

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend calls `/api/ai/chat`, `/api/practice/search`, the four practice APIs, Tutor conversation/attachment endpoints, progress/history and Admin endpoints. Vite proxies `/api` to `http://127.0.0.1:8081` by default, with `VITE_API_PROXY_TARGET` available as an explicit override. Only the backend calls AI providers. Use `backend/.env.example` as a placeholder reference. Never commit a real key or `.env` file.
