---
name: release-checklist
description: Run this after every batch (or fix) in the Tehilim Daily repo (ohadsam/Tehilim_Daily), before considering it done. 4 persona code reviews (system architect, UI expert, UX expert, QA architect) with explicit security / appearance / performance / usability gates, version bump, What's New, docs, user guide + info buttons, skills, RTL/Hebrew check, permissions review, CI-green gate, merge. Invoke by name ("run the release checklist") or whenever a batch/change is being finished, shipped or wrapped up.
---

# Release checklist (Tehilim Daily)

Template origin: `ohadsam/extendedfilemanager`; adapted to this app (native Android, Kotlin + Compose + Glance, Hebrew/RTL, see `CLAUDE.md`).
Run between every batch of `docs/PLAN.md`. Do not skip or reorder steps — later steps assume earlier ones are done.
If a step finds nothing, say so explicitly and move on. Don't invent war stories; add real incidents to the relevant step as they happen (step 7).

## 1. Code review — 4 separate persona passes
1. **System architect.** Re-read every changed file. Package layout respected (`data/domain/ui/widget/reminders/calendar`)? Psalm text only through `PsalmsRepository` (Room first, Sefaria fallback, no empty/error rows stored)? Pure date/schedule logic free of Android deps and clock-injected? Any duplicated logic that an existing helper covers? Files/functions single-purpose? New dependency justified and pinned in `gradle/libs.versions.toml`? Background work in WorkManager/AlarmManager (not raw threads)? Non-obvious *why* commented, obvious code not?
2. **UI expert.** Compose conventions: state hoisted, no business logic in composables, `@Preview` for light+dark, Material 3 consistency, spacing/typography tokens, icons consistent, Glance widgets look right at min/max size, dark+light+dynamic color, 200% font scale, no clipped text. **Appearance gate:** does it look polished next to sibling screens?
3. **UX expert.** Discoverable and forgiving? Primary action is the easiest (tap = open; long-press/overflow = secondary)? Destructive actions confirmed or undoable (snackbar Undo) with specific wording? Empty / loading / error states present, with Hebrew copy and a retry? Touch targets ≥ 48dp? One-handed reach for key actions? **Usability gate:** can a first-time user mark a day as said in ≤ 2 taps?
4. **QA architect.** Unit tests for new logic (29/30-day months, leap years, weekday split boundaries, 119 split, Shabbat/YT shifting, repo cache hit/miss/offline, empty-response not cached, null prev/next at boundaries)? Edge cases (offline, first launch, midnight rollover, timezone change, reboot with pending reminders, process death, huge font)? **Performance gate:** nothing heavy on main thread, lists lazy, no needless recomposition, widget updates cheap, startup not regressed. **Security gate:** see step 5.

Fix every finding before moving on.

## 2. Version + What's New
- Bump `versionName`/`versionCode` in `app/build.gradle.kts` (features → minor, fixes → patch; versionCode every release).
- Replace `assets/whatsnew.json` with ONLY this version's user-facing bullets in Hebrew (delete old ones). The in-app version label and What's New sheet must show the new number.
- Append a short entry to `docs/CHANGELOG.md`.

## 3. Docs
`docs/PLAN.md` status; `docs/SPEC.md` if behavior diverged (fix the spec, not just the code); `CLAUDE.md` if a convention changed; `AGENT.md` (Recently fixed / Open items); `STRUCTURE.md` file map if files were added; `README.md` if user-facing.

## 4. Hebrew, RTL & localization
New strings in `values/strings.xml` (Hebrew default; no hardcoded text, no mixed-direction glitches — wrap Latin/digits in bidi-safe spans). Directional icons mirrored, `start/end` not `left/right`, widgets and notifications checked in RTL, Hebrew dates/letters formatted correctly (גרשיים), text with nikud renders with the chosen fonts.

## 5. User guide, info & security/permissions
- In-app help / info buttons: if behavior changed, update their copy (stale help is worse than none).
- Permissions: any new permission justified in-app and in the manifest comment; exact-alarm and notification flows degrade gracefully when denied.
- Data: personal names/dates stay local; nothing sensitive in logs/exports without opt-in; no cleartext traffic; exported components and deep links validate inputs; PendingIntents immutable; dependencies scanned (CI) — check the job ran clean.

## 6. Tests & CI
Add/extend unit tests with the code. The sandbox cannot run Gradle: "tests pass" means `android-ci.yml` is green on the pushed branch (ktlint, detekt, unit tests, assembleDebug). Push and read CI; never claim success from eyeballing. Note what still needs instrumented/emulator coverage.

## 7. Skills & this checklist
Do `.claude/skills/*` need updating? **Then ask explicitly whether this checklist itself needs a new/changed step** (a real gotcha, a recurring miss). Add concrete entries only for real incidents; otherwise say "no change".

Real incidents so far (keep concrete):
- A Room schema change needs `version` bump + a `Migrations` entry in the SAME commit (release builds never wipe data).
- Navigation/lifecycle calls after a `suspend` repository call must run on `Dispatchers.Main` — under Compose test dispatchers the continuation resumes off-main and crashes (`setCurrentState`).
- Any external-API assumption (e.g. Sefaria `length`) must be checked by the live probe, not by reading docs/memory.
- Schedule data (e.g. the 119 split) must be confirmed by the owner, not inferred from another repo or an unverified web summary.
- Glance widgets: data captured once in `provideGlance` is stale after `update()` (the session is reused and only recomposes) — collect flows / read `currentState` inside `provideContent`.
- The OS APPs and Downs store links `releases/latest/download/tehilim-daily.apk` (+ `.sha256`): keep those two stable file names in `release.yml`, and keep `ReleaseParser` preferring the build-numbered APK when a release carries both (tested).
- The in-app update check only works while the releases are public (private repo = 404 for everyone but the owner): before telling anyone to install, confirm the visibility decision recorded in `AGENT.md` and that `releases/latest` opens logged out.
- Sealed-class additions (e.g. `Repeat.YearlyHebrew`) must be handled in every `when` (planner, codec, list text, editor) — grep before pushing.

## 8. Merge
Feature branch → push → PR → CI green → merge to `main`. Resolve conflicts for real; never force-push over others' work.

## Store - update "OS APPs and Downs" (every shipped version)
The owner's app store (repo `ohadsam/os_app_store`, https://ohadsam.github.io/os_app_store/) must always show this app's latest version.
1. After the release is live (APK release / Pages deployed), find the app's entry in `web/data/apps.json` (its `source` block says which file here holds the version).
2. Preferred: trigger the store's sync - GitHub Actions -> "Sync catalog" -> Run workflow in `ohadsam/os_app_store` (or `mcp__github__actions_run_trigger` workflow `sync-catalog.yml`). It reads this repo's version + CHANGELOG and updates version, update date and what's-new.
3. If this session can write to the store repo instead: edit the entry (`version`, `updated`, `whatsNew` he + en, `links` if the APK name/URL changed, new `shots`), run `npm test` there, push to `main`.
4. If the change is user-visible, re-capture screenshots (`web/art/shots/<id>/`) from this app's e2e/journey images.
5. A changed APK file name, package id, min Android, platforms or install flow also changes the entry (`links.apk`, `apk.*`, `platforms`). Say in the report that the store was updated (or why not).

## Done means
All four passes ran and findings fixed (incl. security, appearance, performance, usability gates); version + What's New + changelog updated; docs and PLAN status updated; RTL/Hebrew and permissions reviewed; tests added and CI green; skills + checklist self-reviewed; merged (or PR ready).
