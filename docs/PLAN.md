# PLAN — Tehilim Daily
Status legend: [ ] todo · [~] in progress · [x] done. After each batch: tests green on CI → `release-checklist` → bump version + What's New → update this file.
Constraint: no local Gradle (see CLAUDE.md); CI is the gate. Keep batches small (~1 day of work each).

| # | Batch | Spec | Version | Status |
|---|---|---|---|---|
| 0 | Foundation: Gradle project, version catalog, CI workflow, theme, nav shell (RTL), AppLogger skeleton, What's New + version display | S0 | 0.1.0 | [ ] |
| 1 | Local-first psalms: Sefaria client, Room, `PsalmsRepository`, prefetch-all, cache settings | S1 | 0.2.0 | [ ] |
| 2 | Hebrew calendar + schedules (sequential/weekly/monthly, 29-day rule) + location (auto/manual, A1) — pure Kotlin + unit tests | S2 | 0.3.0 | [ ] |
| 3 | Reader UI + day views (chips, prev/next, date navigation, picker, manual history) | S2,S3 | 0.4.0 | [ ] |
| 4 | Favorites + persons (refuah / ilui nishmat) + links | S4 | 0.5.0 | [ ] |
| 5 | Search + said-tracking + history screen (30 days, search) | S3,S5 | 0.6.0 | [ ] |
| 5b | Bookmarks (סימניות) + continue-reading (spec A4) | A4 | 0.6.5 | [ ] |
| 6 | Reminder engine: scheduling, receivers, notification actions, snooze, boot/TZ repair, Shabbat/YT shifting (zmanim) | S6 | 0.7.0 | [ ] |
| 7 | Reminders UI + "from reminder" browse source + permission health | S7 | 0.8.0 | [ ] |
| 8 | Widgets (reader, lists, Hebrew date, weekday, bookmark, continue-reading + extras), deep links, auto-update | S8 | 0.9.0 | [ ] |
| 9 | Settings home, appearance (fonts, sizes, light/dark colors), logs UI (view/copy/download/clear/auto-clear), backup/restore | S9 | 0.10.0 | [ ] |
| 10 | Polish & hardening: a11y, perf (baseline profile), security review, shortcuts/tile, UI tests on emulator, release signing + artifacts | S10 | 1.0.0 | [ ] |

## Per-batch routine (also in skill `batch-workflow`)
1) read spec slice 2) implement + unit tests 3) push, CI green 4) 4-persona review (`release-checklist`) 5) version + What's New + docs 6) merge.

## CI (batch 0)
`.github/workflows/android-ci.yml`: JDK 17/21, setup-gradle, `ktlintCheck`, `detekt`, `testDebugUnitTest`, `assembleDebug` → upload debug APK artifact; release workflow on tag builds signed AAB/APK (signing via secrets). Nightly/manual `sefaria-probe` job validates refs and the weekly split against the live API.

## Autonomy
User approved continuous batch-by-batch development without asking for approval (2026-10-10). Open questions/suggestions go in AGENT.md → Open Items and the batch summary, not as blockers.

## Open decisions (defaults chosen; tell me to change)
- DI: manual `AppContainer` (no Hilt, fewer blind-compile risks); tests JUnit4; lint non-blocking until stabilized.
- Package `com.ohadsam.tehilim`, app name "תהילים יומי".
- Zmanim via KosherJava (offline) rather than Hebcal API; default location Jerusalem, Israel schedule.
- Hebrew day switches at midnight by default (tzeit optional).
- Fonts bundled (OFL) in `res/font`.
