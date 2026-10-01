# Phase 4 + Phase 5 Master Execution Index

This is an execution index, not an additional implementation plan. It is bound
to:

- `docs/superpowers/specs/2026-10-01-phase-4-learning-assessment-design.md`
- `docs/superpowers/specs/2026-10-01-phase-5-learner-journey-design.md`

Product base for this planning run: `origin/main` at `ebf6f56313faf57186f4285a2f7265e804193a6e`. The executor must recompute the base and migration head before implementation if main advances.

## Plan index

1. `2026-10-01-phase4-a-submission-engine.md` — canonical submission, ownership, version pinning, autosave, idempotent finalization, history, API security.
2. `2026-10-01-phase4-b-reading-listening-scoring.md` — deterministic objective scoring, question evidence, mistakes, trusted adaptive events, results.
3. `2026-10-01-phase4-c-writing-assessment-versioning.md` — immutable Writing versions, rubric-specific AI evaluation, retry-safe lifecycle, comparison.
4. `2026-10-01-phase4-d-speaking-submission-review.md` — safe audio/text submission, transcript source boundary, playback, manual review.
5. `2026-10-01-phase4-e-results-admin-adaptive-integration.md` — common learner results, history, admin review, Tutor actions, Phase 2 integration.
6. `2026-10-01-phase5-a-onboarding-diagnostic.md` — self-reported goals, versioned diagnostic, confidence, Estimated starting profile.
7. `2026-10-01-phase5-b-todays-plan-error-notebook.md` — bounded evidence-backed plan and mistake projection/actions.
8. `2026-10-01-phase5-c-full-mock-test.md` — resumable IELTS-style non-official mock composed from Phase 4.
9. `2026-10-01-phase5-d-search-saved-profile.md` — approved search, saved practices, profile/settings completion.

## Dependency graph

```text
                    4A
              Submission Engine
                     |
        +------------+-------------+
        |                          |
       4B                      4C -> 4D
 Reading/Listening             Writing/Speaking
        |                          |
        +-------------+------------+
                      |
                     4E
          Results/Admin/Adaptive
                      |
          +-----------+-----------+
          |           |           |
         5A          5B          5C
   Onboarding/   Today's Plan/   Mock Test
   Diagnostic    Error Notebook
          \           |           /
           +----------+----------+
                      |
                     5D
           Search/Saved/Profile
```

## Execution waves

### Wave 0 — canonical base and contracts

- Fetch latest `origin/main` and inspect active worktrees.
- Start from the final accepted Search + Typography product base before 5D.
- Confirm the approved specs and current highest Flyway migration.
- Do not touch active UI branches or the design worktree.

### Wave 1 — sequential foundation

- Execute 4A alone.
- Compute `H1` from the actual canonical base and allocate the next available
  migration range at execution time.
- Run backend tests/package, frontend tests/lint/build, fresh migration checks,
  security tests, and acceptance before freezing the integrated 4A HEAD.

### Wave 2 — bounded parallel skill lanes

After 4A is integrated, compute `H2` from that exact HEAD and reserve
non-overlapping migration ranges at runtime:

- Lane A: 4B objective scoring.
- Lane B: 4C Writing assessment/versioning → 4D Speaking submission/review.

The lanes share only the 4A submission/result contracts. They must not edit the
same migration files or duplicate lifecycle/scoring abstractions. If actual file
overlap appears during execution, serialize the affected task rather than
resolving it with a destructive merge.

### Wave 3 — integration

- Execute 4E after 4B, 4C, and 4D are reviewed and integrated.
- Run the full Phase 4 learner/admin/adaptive acceptance gate.
- Freeze the complete Phase 4 HEAD before branching Phase 5.

### Wave 4 — Phase 5

- Execute 5A first for onboarding/diagnostic contracts.
- 5B may follow 5A because Today's Plan can consume optional goal inputs; its
  evidence source remains Phase 2/4.
- 5C may run after 4E and can be parallel with 5B only when migration ranges
  and files do not overlap.
- Execute 5D last, only after accepted Search + Typography is integrated into
  the canonical product base.

## Migration policy

- Never pin exact future Flyway version numbers in implementation work before
  calculating the current head.
- Never renumber historical migrations.
- Use additive tables/columns/indexes and compatibility/backfill steps.
- Compute `H1` for 4A, freeze its resulting head, compute `H2` for Wave 2, and
  allocate non-overlapping ranges for parallel lanes.
- Allocate Phase 5 ranges only from the fully integrated Phase 4 head.
- Test both fresh database migration and upgrade/backfill behavior in isolated
  QA databases; legacy databases are not touched by planning or local tests.

## Common TDD and review gate

Every task in every plan must complete:

1. failing test;
2. focused RED confirmation with the correct reason;
3. minimal implementation;
4. focused GREEN confirmation;
5. independent review of Critical/Important risks;
6. focused commit before the next task.

The final task of each plan must run its cross-module acceptance tests. Before
any phase is called complete, run fresh backend tests/package, frontend tests/
lint/build, migration/diff checks, ownership/security checks, and relevant
runtime acceptance with external AI mocked unless a separately authorized live
test is requested.

## Required no-go checks

- No client-authoritative scores, answer keys, owner IDs, roles, or review data.
- No fake scores, fake STT, fake pronunciation, fake research metrics, or
  official IELTS claims.
- No duplicate adaptive engine, scoring engine, AI provider router, or Tutor
  memory system.
- No unapproved Phase 3 practice reaches learner search, save, start, or submit.
- No migration version is invented before execution-base inspection.
- No product code, migration, dependency, push, merge, or deployment is part
  of this planning commit.
