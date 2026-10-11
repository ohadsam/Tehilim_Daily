# Tehilim Daily – Agent Memory
**Last updated:** v0.9.0 (batches 0–9 implemented; batch 10 polish in progress)
**Stack:** Native Android (Kotlin, Compose, Glance), Sefaria API cached in Room, offline Hebrew calendar + zmanim.
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- v0.9.0: everything in docs/CHANGELOG.md. CI (Android CI + Sefaria probe) is the only build/test gate (sandbox has no SDK/network).
- v0.11.1: Psalm 119 monthly split fixed to 1–96 (א–ל) / 97–176 (מ–ת) per the owner (was 1–88 / 89–176, which also matched kodesh-app — that app has the same issue). Existing saved refs with old ranges stay valid.

## Known issues / open items (need the owner)
- **Signing key:** `app/sideload.jks` (password in app/build.gradle.kts) is committed on purpose so every CI build is an in-place update. It is NOT a store key: anyone with repo access can sign updates. For Play/store releases set `KEYSTORE_*` secrets (release.yml) — but then users must reinstall once, because the signature changes. Never change/delete the sideload key once users have installed it.
- **Prayers (יהי רצון):** mechanism + settings are in place, but `assets/prayers.json` has NO verified texts. Supply wording + source per nusach, then set `verified=true`.
- Name-letter / "נשמה" stanzas: confirmed by the owner (2026-10-11), see SPEC B1. A person's stored name is not re-synced into an existing name-letters favorite when edited.
- Tzeit / candle-lighting / day-rollover defaults (8.5°, 18 min or 40 in Jerusalem, midnight): approved by the owner 2026-10-11; all user-configurable.
- **Day rollover default = midnight** (matches kodesh-app); choose sunset/nightfall in Location settings. Said marks are keyed by the *logical* date, so switching the mode mid-way can make an old day look unmarked.
- Room schema is v1 and frozen at the first public release (debug builds wipe on mismatch; release must use migrations).
- `@Preview` functions (light/dark) are still missing for most screens — the emulator screenshots (ui-tests.yml) are the visual check for now.
- Widgets lay out in the system direction; on a non-Hebrew device locale text alignment inside widgets may differ from the app.
- Golden verse counts are in `PsalmVerseCounts` (from the probe). Re-run `Sefaria probe` if Sefaria's text version changes; a mismatch means the golden table or the text needs review.
- Not verifiable in this sandbox (needs a device): notification delivery timing, exact-alarm permission flow, widget visuals, TalkBack pass.
