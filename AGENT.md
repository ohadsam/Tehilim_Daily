# Tehilim Daily – Agent Memory
**Last updated:** v0.9.0 (batches 0–9 implemented; batch 10 polish in progress)
**Stack:** Native Android (Kotlin, Compose, Glance), Sefaria API cached in Room, offline Hebrew calendar + zmanim.
Read order: CLAUDE.md → docs/PLAN.md → STRUCTURE.md → the batch's section in docs/SPEC.md.

## Recently done
- v0.14.0: widget follow-ups (SPEC C3). Unverified on a device: scroll-reset trick (item ids + alternating tree) and pin-widget flow — check after install.
- v0.18.0 (store availability): `release.yml` publishes `tehilim-daily.apk` + `.sha256` (stable names) beside the build-numbered APK; the OS APPs and Downs store (`ohadsam/os_app_store`) links `releases/latest/download/tehilim-daily.apk`. **Two owner decisions block public use:** (1) the repository is private, so the unauthenticated update check (`api.github.com/.../releases/latest`) and every download link answer 404 for everyone but the owner; (2) making it public exposes `app/sideload.jks` + password (see below). Options: rotate the key into `KEYSTORE_*` secrets (one reinstall) then make the repo public, or keep the source private and publish releases to a public repo (change `UpdateController.REPO`, add a token secret for release.yml).
- v0.18.0: in-app updates read the latest GitHub Release (public repo required). **Before the repo is made public:** `app/sideload.jks` and its password are committed, so anyone could build an APK that Android accepts as an update over the user's install (they would still have to get the user to install it; the in-app updater only ever fetches releases of this repo). Moving the key into Actions secrets (`KEYSTORE_*`, release.yml already supports it) fixes that but changes the signature once → everyone reinstalls one time. Owner decision.
- v0.17.0: sharing & division (SPEC C7). Not verifiable here: how WhatsApp / Gmail present a `.tehilim` attachment on the receiving phone (intent filters cover type, extension and octet-stream; the paste-code and "share message to the app" routes are the fallback). Ideas not built: QR code (needs a library), https link (needs a hosted domain + assetlinks).
- v0.12.0: reader redesign + auto-scroll + live widgets (SPEC C1). Widget lesson: never capture data once in `provideGlance` — collect flows inside `provideContent` (Glance reuses the session on update()).
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
