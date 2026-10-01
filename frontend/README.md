# IELTS AI Tutor frontend

The frontend consumes provider-independent AI and RAG response fields. Tutor citations use normalized `sourceId`, `title`, `section`, `version`, `page`, and `chunkId` values; vendor-specific provider payloads are ignored by the client. The app supports guest mode, persistent login/register sessions, explicit member demo mode, four skill practice routes, and fixture-backed search.

## Temporary RAG Admin CMS

Run the backend and frontend locally, then open `/admin/rag`. An authenticated ADMIN session is used automatically. For local compatibility, enter the value configured as `RAG_ADMIN_TOKEN`; it is kept in `sessionStorage` for the current browser session only and is sent as `X-Admin-Token`. The page supports upload, bounded preview, rights review, indexing, activation/deactivation, and recent ingestion job status, with visible loading and error states.

The intended workflow is upload → preview → approve → index → activate. The frontend does not approve content automatically and does not call Gemini or any vector provider directly.

```powershell
npm install
npm run dev
```

The Vite development server proxies `/api` to the local Spring Boot service.

## Main routes

`/`, `/assessment`, `/register`, `/login`, `/practice/reading`, `/practice/listening`, `/practice/writing`, `/practice/speaking`, `/practice/search?q=...`, and `/admin/rag`.

## Verification

```powershell
npm test
npm run lint
npm run build
```

This template provides a minimal setup to get React working in Vite with HMR and some Oxlint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the Oxlint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and Oxlint's TypeScript related rules in your project.
