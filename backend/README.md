# IELTS AI Tutor backend — Phase 2A

This Spring Boot service exposes the provider-independent `POST /api/ai/chat` contract and keeps Gemini credentials on the server. Phase 2A does not include RAG, database access, embeddings, authentication, or writing band evaluation.

PowerShell local run:

```powershell
cd backend
$env:GEMINI_API_KEY="your-real-key"
$env:GEMINI_MODEL="gemini-2.5-flash"
./mvnw.cmd spring-boot:run
```

In another terminal:

```powershell
cd frontend
npm install
npm run dev
```

The frontend calls `/api/ai/chat`; Vite proxies `/api` to `http://localhost:8080`, and only the backend calls Gemini. Use `backend/.env.example` as a placeholder reference. Never commit a real key or `.env` file.
