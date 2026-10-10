# Tehilim Daily – Agent Memory
**Last updated:** v0.9.0 (batches 0–9 implemented; batch 10 polish in progress)
**Stack:** Native Android (Kotlin, Compose, Glance), Sefaria API cached in Room, offline Hebrew calendar + zmanim.
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- v0.9.0: everything in docs/CHANGELOG.md. CI (Android CI + Sefaria probe) is the only build/test gate (sandbox has no SDK/network).

## Known issues / open items (need the owner)
- **Prayers (יהי רצון):** mechanism + settings are in place, but `assets/prayers.json` has NO verified texts. Supply wording + source per nusach, then set `verified=true`.
- **Name-letter / "נשמה" stanzas of Psalm 119:** widespread custom, implemented as an optional helper — confirm before release.
- **Tzeit / candle-lighting defaults:** 8.5° and 18 min (40 in Jerusalem area) — confirm minhag; both are user-configurable.
- Widgets lay out in the system direction; on a non-Hebrew device locale text alignment inside widgets may differ from the app.
- Sefaria probe results (`VERSE_COUNTS`) should be turned into a golden table once the CI run is read.
- Not verifiable in this sandbox (needs a device): notification delivery timing, exact-alarm permission flow, widget visuals, TalkBack pass.
