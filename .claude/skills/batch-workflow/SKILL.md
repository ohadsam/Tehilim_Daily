---
name: batch-workflow
description: How to run one development batch in Tehilim_Daily token-efficiently — read plan/spec slice, implement, test, review, update docs, run release-checklist. Use at the start of every batch ("start batch N", "continue the plan").
---
# Batch workflow
1. Read `docs/PLAN.md` → find the next unchecked batch. Read ONLY that batch's section in `docs/SPEC.md` (grep the `## S<N>` anchor).
2. If the spec for it is ambiguous, tighten the spec first (edit SPEC.md), not the code.
3. Implement in small commits. Write unit tests with the code (JUnit4/Turbine/MockK; pure logic first). No local Gradle: eyeball compile-correctness, push, read CI.
4. Reuse: grep for existing helpers before writing. Keep files < ~300 lines.
5. Self-review diff (architect/UI/UX/QA) → run the `release-checklist` skill. Fix findings before moving on.
6. Update: PLAN.md status, AGENT.md "Recently Fixed"/"Open items", What's New, STRUCTURE.md map if files added.
7. CI green → merge → next batch. Do not start the next batch with red CI.
## CI feedback loop (the only build/test gate)
- Push, then `gh run list --limit 3` / `gh run watch <id> --exit-status` (blocking, cheap). `gh` cannot download logs (403).
- Logs: `mcp__github__get_job_logs` with a large `tail_lines` and `return_content` saves the log to a file when it is too big — then `grep`/python the file (look for `e: file`, `FAILED`, `What went wrong`). Never read the whole log.
- Compile errors show up in one pass; fix all, push once. KSP occasionally flakes (`Expected start of the object '{', but had 'EOF'`) — re-run, don't "fix".
- Manual workflows (use `mcp__github__actions_run_trigger`): `release.yml` (R8 release APK/AAB), `ui-tests.yml` (emulator smoke tests + half-size JPEG screenshots printed to the log between `SHOTS_BEGIN/SHOTS_END`; decode the base64 chunks and view with Read), `sefaria-probe.yml` (live API contract).
- Live external behavior (Sefaria) must be verified by a probe, not assumed: the sandbox cannot reach it.
Token rules: no full-file reads when grep + offset/limit works; no re-deriving decisions recorded in SPEC.md; summaries in docs, not chat.
