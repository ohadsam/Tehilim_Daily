# Tehilim Daily (תהילים יומי)

אפליקציית אנדרואיד נייטיב בעברית (RTL) להצגת פרקי תהילים — לפי סדר, יום בשבוע, יום בחודש העברי,
מועדפים (כולל לרפואת / לעילוי נשמת), תזכורות, חיפוש, וידג'טים. דגש: UI/UX ושימושיות.
**קרא קודם `docs/PLAN.md` (באצ'ים וסטטוס) ו-`docs/SPEC.md` (מפרט). אל תסרוק את הקוד לפני כן.**

## Stack
Kotlin, Jetpack Compose (Material 3), Glance (App Widgets), Hilt, Room, DataStore, WorkManager + AlarmManager,
Retrofit/OkHttp + kotlinx.serialization, android.icu `HebrewCalendar` (לוח עברי, ללא רשת), KosherJava Zmanim (זמני שבת/חג, ללא רשת).
Single `:app` module, package-by-feature. minSdk 26, targetSdk = latest stable. Package: `com.ohadsam.tehilim`.

## Package layout (grows by batch — do not pre-create empty packages)
```
data/sefaria/   SefariaApi, PsalmsRepository (Room-first, API fallback)
data/db/        Room entities/DAOs
data/prefs/     DataStore settings
domain/         models, use cases (schedules, hebrew date, reminders-calc)
calendar/       HebrewDateProvider, ShabbatHolidayTimes
reminders/      scheduler, receivers, notification builder
ui/<feature>/   one package per screen
widget/         Glance widgets
log/            AppLogger (ring buffer + file), export/copy
```

## Conventions
- UI strings only in `values/strings.xml` (Hebrew is the default locale; English optional). No hardcoded strings.
- Business logic never in composables; state hoisted; every screen has `@Preview` (light+dark) and empty/loading/error states.
- Psalm text: **always** `PsalmsRepository.get(ref)` — Room first, Sefaria fallback, store only non-empty Hebrew. Never call Sefaria from UI.
- Time/date logic is pure Kotlin taking `Clock`/`LocalDate` params so it is unit-testable.
- Reuse before duplicating; files small and single-purpose; comment the non-obvious *why* only.
- Every sheet/dialog/bottom-bar must respect RTL (`start`/`end`, never `left`/`right`) and font scale up to 200%.
- Touch targets ≥ 48dp; contrast WCAG AA in both themes; all icons have contentDescription.
- Log via `AppLogger` (levels, tags); never log personal names of favorites in plaintext exports unless the user opts in.

## Sandbox constraints (important)
Cloud sandbox has JDK + Gradle but **no Android SDK; dl.google.com / Maven Central / sefaria.org are blocked**.
No Gradle task runs locally. **GitHub Actions (`.github/workflows/android-ci.yml`) is the only build/test gate** — push, then
read CI results. Read Kotlin by eye before pushing; keep batches small so a CI failure is cheap to fix. Pure-Kotlin logic
(schedules, date math) should be in a module-free package so it can later move to a JVM-only test target.
Sefaria behaviour is taken from the proven implementation in `ohadsam/kodesh-app` (`js/utils.js` `sefariaText`, `js/tehilim.js`);
see `.claude/skills/sefaria-fetch/SKILL.md`. Never touch other repos (read-only reference only).

## Token efficiency
Follow `.claude/skills/batch-workflow/SKILL.md`: one batch at a time, read PLAN→the batch's spec section only, grep before reading,
targeted edits, update `docs/PLAN.md` status + `AGENT.md` at batch end, run `release-checklist` between batches.

## Release process
`.claude/skills/release-checklist/SKILL.md` after every batch (security, appearance, performance, UI/UX, usability emphasis).
Branch → push → CI green → PR/merge. Bump version (`versionName`/`versionCode`) and update the in-app What's New each user-visible batch.
