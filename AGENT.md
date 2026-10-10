# Tehilim Daily – Agent Memory
**Last updated:** planning phase (no app code yet)
**Stack:** Native Android (Kotlin, Compose, Glance), Sefaria API (cached in Room), offline Hebrew calendar.
Read order: CLAUDE.md → docs/PLAN.md → the batch's section in docs/SPEC.md.

## Recently Fixed / Done
- Planning: PLAN.md, SPEC.md, CLAUDE.md, skills (release-checklist, batch-workflow, sefaria-fetch, hebrew-calendar-reminders).

## Known Issues / Open Items
- Sandbox blocks Android SDK, Maven, and sefaria.org: all verification is via CI; Sefaria refs and weekly split to be verified by the CI `sefaria-probe` job.
- Halachic content to verify before ship: weekly split, "יהי רצון" texts, name-spelling stanza presets, tzeit/candle-lighting defaults.
