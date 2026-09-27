# Student-ready MVP legacy database rehearsal

This runbook preserves the existing database. It is an inspection and clone-rehearsal procedure, not a reset or repair shortcut.

1. Verify a restorable backup with `scripts/db/verify-legacy-backup.ps1 -BackupFile <explicit-backup-file>` and retain the `pg_restore --list` output.
2. Create a separately named clone only after backup verification with `scripts/db/clone-legacy-db.ps1 -SourceDatabase <source> -CloneDatabase <clone> -AllowCreateClone`. Never use the source name as the clone name.
3. Inspect history and schema with `scripts/db/inspect-flyway-state.ps1 -DatabaseUrl <clone-url>`. The command must show the existing V1–V8/V28 state and must not edit `flyway_schema_history`.
4. Compare clean V1..V8 → V9 → V10 → V28 DDL with the clone using `pg_dump --schema-only --no-owner`, `information_schema.columns`, and the migration files. Record every pre-existing compatible table/column before running any rehearsal migration.
5. Run `scripts/db/rehearse-legacy-remediation.ps1 -CloneDatabaseUrl <clone-url> -ExpectedCloneDatabase <clone-name> -REHEARSAL_ONLY`. This script intentionally performs no remediation; any required compatibility step is reviewed and executed manually on the clone only after the comparison is complete.
6. Run preservation queries before and after rehearsal. For each table group, use `SELECT count(*)` and stable-ID samples for users/roles/sessions, RAG documents/versions/chunks, learning events/mistakes/profiles/roadmaps, generator sources/jobs/reviews/revisions/approved sets, Tutor conversations/messages, and learner attempts/activity. Also run the Flyway history query and save schema-only dumps.
7. Start the backend against the clone, call its health endpoint, stop it, start it again, and repeat the preservation queries. A restart failure or data mismatch blocks original remediation.
8. Temporary Flyway out-of-order is permitted only if the inspected clone exactly matches the approved V9/V10 missing-history condition, backup/clone/integrity/start/restart gates pass, the setting is scoped to one controlled run, and history is verified immediately afterward. Remove the setting before normal startup. Never permanently enable it.
9. Original database remediation is a separate human approval gate. This repository tooling never performs it and never drops, resets, truncates, or blindly edits history.
