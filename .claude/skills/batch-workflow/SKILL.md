---
name: batch-workflow
description: How to run one development batch in Tehilim_Daily token-efficiently — read plan/spec slice, implement, test, review, update docs, run release-checklist. Use at the start of every batch ("start batch N", "continue the plan").
---
# Batch workflow
1. Read `docs/PLAN.md` → find the next unchecked batch. Read ONLY that batch's section in `docs/SPEC.md` (grep the `## S<N>` anchor).
2. If the spec for it is ambiguous, tighten the spec first (edit SPEC.md), not the code.
3. Implement in small commits. Write unit tests with the code (JUnit5/Turbine/MockK; pure logic first). No local Gradle: eyeball compile-correctness, push, read CI.
4. Reuse: grep for existing helpers before writing. Keep files < ~300 lines.
5. Self-review diff (architect/UI/UX/QA) → run the `release-checklist` skill. Fix findings before moving on.
6. Update: PLAN.md status, AGENT.md "Recently Fixed"/"Open items", What's New, STRUCTURE.md map if files added.
7. CI green → merge → next batch. Do not start the next batch with red CI.
Token rules: no full-file reads when grep + offset/limit works; no re-deriving decisions recorded in SPEC.md; summaries in docs, not chat.
