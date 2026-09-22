# IELTS AI Tutor backend — Phase 2B RAG

This Spring Boot service exposes the provider-independent `POST /api/ai/chat` contract and a governed RAG workflow. Documents remain pending review until an administrator approves rights, indexes the approved version, and activates it. Gemini credentials stay on the server; automated tests use deterministic adapters.

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

The Compose service provides PostgreSQL with the `vector` extension. Flyway creates the four RAG tables and vector index when the backend starts. Set `RAG_DB_HOST`, `RAG_DB_PORT`, `RAG_DB_NAME`, `RAG_DB_USER`, and `RAG_DB_PASSWORD` when using a non-default database.

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

Open `/admin/rag` in the frontend and enter the token. The operational sequence is upload → preview → approve rights → index → activate. Deactivate a source when it should no longer be eligible for Tutor retrieval. The token is sent only in the `X-Admin-Token` header and is not returned by the API.

Manual verification can exercise the lifecycle with the sample manifest under `backend/rag-data/`. Live Gemini browser verification requires credentials, quota, and a running frontend proxy; it is intentionally not treated as an automated test pass.

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend calls `/api/ai/chat`; Vite proxies `/api` to `http://localhost:8080`, and only the backend calls Gemini. Use `backend/.env.example` as a placeholder reference. Never commit a real key or `.env` file.
