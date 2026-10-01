# Academic Luxury 2.0 — Execution Index

Approved spec:

`docs/superpowers/specs/2026-09-26-academic-luxury-2-redesign-design.md`

Plans:

- `docs/superpowers/plans/2026-09-26-al2-a-design-system-shell.md` — 5 tasks
- `docs/superpowers/plans/2026-09-26-al2-b-home-dashboard-tutor.md` — 6 tasks
- `docs/superpowers/plans/2026-09-26-al2-c-learning-workspaces.md` — 6 tasks
- `docs/superpowers/plans/2026-09-26-al2-d-attachment-contract.md` — 5 tasks
- `docs/superpowers/plans/2026-09-26-al2-e-verification-hardening.md` — 4 tasks

Total implementation tasks: 26

Dependency graph:

`AL2-A → AL2-B → AL2-C / AL2-D → AL2-E`

Execution order:

1. Complete and review AL2-A.
2. Complete and review AL2-B.
3. Complete and review AL2-C.
4. Complete and review AL2-D.
5. Complete AL2-E as verification-only hardening.

For single-worktree execution, run AL2-C and AL2-D sequentially after AL2-B.

Global test gates:

- Frontend: `npm test -- --run`, `npm run lint`, `npm run build`
- Backend: `./mvnw.cmd test`, `./mvnw.cmd package`
- Repository: `git diff --check`, `git status --short`
- No live Groq, Cloudflare, or Gemini quota in automated checks.
- Verify Dark, Light, and System themes at 375, 768, 1024, and 1440+.

Stop conditions:

- Stop and review if the working tree contains unexpected changes.
- Stop if a task would require AI Phase 2/3, STT, multimodal vision, official IELTS scoring, a new provider, or a new migration.
- Stop if frontend/backend attachment rules diverge from the approved MIME, size, ownership, or one-active-per-request contract.
- Stop if a visual fix would change server-owned scoring, draft ownership, reference version safety, session continuity, or Speaking’s truthful local boundary.
