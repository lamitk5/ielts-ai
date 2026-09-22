# IELTS AI Tutor

IELTS AI Tutor is a four-skill learning platform built with React/Vite, Spring Boot, PostgreSQL and pgvector. It keeps IELTS-style fixtures project-owned and synthetic, exposes provider-independent Tutor and Writing contracts, and uses Gemini only from the backend.

## Prerequisites

- Java 21
- Node.js 20+
- Docker Desktop with PostgreSQL and pgvector support

## Local development

```powershell
docker compose -f backend/docker-compose.yml up -d
cd backend
./mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Flyway applies migrations V1 through V5 on backend startup. Do not edit or squash an already-applied migration.

## Configuration

Copy the placeholders from `backend/.env.example` into the local environment. Keep `GEMINI_API_KEY` server-side only. `GEMINI_MODEL`, `GEMINI_EMBEDDING_MODEL`, and `RAG_EMBEDDING_DIMENSION` are configurable; the current vector schema is `vector(768)`.

Gemini quota errors are returned as a controlled temporary-unavailable response. Automated tests use deterministic providers and do not require a live Gemini quota.

## Product routes

- `/` — guest homepage; `/?demo=member` is explicit local demo data
- `/register`, `/login` — persistent auth session
- `/assessment` — four-skill entry flow
- `/practice/reading`, `/practice/listening`, `/practice/writing`, `/practice/speaking`
- `/practice/search?q=...` — fixture-backed practice search
- `/admin/rag` — authenticated ADMIN CMS, with a temporary local token compatibility path

Reading and Listening marking uses stored synthetic answer keys. Writing returns structured estimates only when the provider returns a valid contract. Speaking persists text input and reports the STT boundary without fabricating a transcript.

## Verification

```powershell
cd backend
cmd /c .\mvnw.cmd test
cmd /c .\mvnw.cmd package
cd ..\frontend
npm test -- --run
npm run lint
npm run build
```

The Testcontainers integration check may remain skipped when Docker Desktop returns the known local HTTP 400 metadata response; this is an environment limitation, not a mocked database path. Live Gemini checks are external and quota-dependent.
