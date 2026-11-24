# Changelog

## 0.18.0
- Release workflow also publishes stable-named copies (`tehilim-daily.apk` + `tehilim-daily.apk.sha256`) next to `tehilim-daily-<version>-b<code>.apk`, so a fixed link `releases/latest/download/tehilim-daily.apk` always serves the newest build (used by the OS APPs and Downs store). `ReleaseParser` prefers the build-numbered APK when a release carries both (tested).
- In-app updates (More ← עדכונים ושיתוף האפליקציה, SPEC C8): `UpdateController` asks `api.github.com/repos/ohadsam/Tehilim_Daily/releases/latest`, `ReleaseParser` accepts only a non-draft, non-prerelease release with an `.apk` asset served from `https://github.com/` (build number from `…-b<versionCode>.apk`, checksum from the asset `digest`), the APK is downloaded into `cache/updates` with progress, its SHA-256 is compared when GitHub supplies one, and `UpdateVerifier` requires the same package, a higher versionCode and the very same signing certificates before it is handed to the system installer (`REQUEST_INSTALL_PACKAGES`, FileProvider `updates/`). Daily silent check (switchable) + one prompt per new build ("later" is remembered).
- `release.yml` now also publishes a GitHub Release `v<version>-b<code>` with `tehilim-daily-<version>-b<code>.apk` and the What's New text — the source the app reads. **The repository must be public** for the unauthenticated update check to work (a private repo answers 404, shown as "no public release page").
- The "share this app" buttons moved into a reusable `ShareAppSection`, shown on the new screen and still in Settings. `PackageInfo.longVersionCode` replaced by `PackageInfoCompat` (the plain property needs API 28, minSdk is 26).

## 0.17.0
- Sharing & division (More ← שיתוף וחלוקה, SPEC C7): divide a chapter list between N people (`DivisionPlanner`: consecutive parts, balanced by verse count or by chapter count; Psalm 119 may be split at verse 96 only when needed and is rejoined when both halves land in one part) or share a free chapter list. Saved on the device (`SharedListsStore`, included in the backup), sent as a `.tehilim` file plus a message with a paste-able code via WhatsApp / e-mail / any app, or one part only.
- Receiving needs no server: the app opens `.tehilim` files (VIEW filters by type, extension and untyped files), accepts a shared chat message (SEND text/plain) and has a paste / open-file screen. The code (`TD1:<deflate+base64>:`, alphabet without `_ * ~`) is validated strictly (`SharePackage.validate`: chapter and verse ranges against the golden verse counts, size caps, newer-version check; Adler-32 rejects damaged codes). Import creates a favorite, or a person + linked favorite for refuah / ilui nishmat, never overwrites, skips exact duplicates and reuses an existing person.
- Sending a division one person at a time (`ShareDistributeSheet`): pick the app once, per-part send button with optional recipient name (greeting only, never inside the shared data), "send next" jumps to the first unsent part, sent marks and names are local bookkeeping in `SharedListsStore` (reset when the parts change). Receiving: besides "my part" / "all parts", free choice of any chapters out of the whole share (`importSelection`).
- Share an existing favorite or a person's list from the ⋮ menu in Favorites. `<queries>` for WhatsApp / Gmail so "send straight to" also works on Android 11+ (also fixes the app-share target detection). MainActivity is `singleTop`.

## 0.16.1
- Reading widget: "סגור" button returns to the landing screen (shown only while the landing screen is enabled).
- Share: the shared APK is always the build that is installed and running — copied fresh into the cache each time, verified with `getPackageArchiveInfo` against the installed `longVersionCode`, and named with the build number (`tehilim-daily-0.16.1-b<code>.apk`) so two builds of one version name can never be mistaken for each other.

## 0.16.0
- Free chapter lists ("א, ג, כג, ל, קיח", ranges, verse ranges) — already supported by `ChapterSetParser` in the favorite editor (now explicit in the hint and covered by a test); the refuah / ilui nishmat person editor gets a chapters field too: the list is stored as a favorite linked only to that person (`FavoritesState.personalFavorite`), so the reader, widgets and reminders work unchanged; clearing the field removes that list.

## 0.15.1
- Reading widget: bookmark controls moved out of the text. One header button (🔖＋) toggles "add bookmark" mode; only then the ＋ buttons appear next to the verses (adding one, paging, or tapping again hides them). Bookmarked verses always show just 🔖 (open/edit in the app) and ✕ (two-tap delete). In add mode an "הכל (N)" pill opens the full list.

## 0.15.0
- Reading widgets: optional landing screen (default on): app icon, Hebrew + Gregorian date, source title (+ person tag) and the chapter range of the day. Paging forward → saved/first chapter; previous on the first chapter → back to the landing; the "seen" marker is keyed by source + date so a new day shows the landing again. Setting in the widget's ⚙ screen. Pure transition logic `stepReader` (tested).

## 0.14.0 (owner device test of the widgets)
- Reading widget: arrows use non-mirrored triangles (▶/◀ with text variation selector) chosen by layout direction — the bidi-mirrored ‹ › were drawn backwards in RTL; every verse has an explicit bookmark control (＋🔖 add, 🔖 open/edit in the app, ✕ two-tap delete), 🔖 N chip always visible; progress label "current – last (n/total)" (`progressLabel`, tested); paging resets scroll (chapter-specific item ids + alternating tree shape, since Glance keeps list scroll across updates).
- New pickable widgets: reading of one favorite / one refuah name / one ilui nishmat name (same widget class, receiver decides which sources the config screen offers).
- In-app "add widget" screen (More + reader ⋮ menu): `AppWidgetManager.requestPinAppWidget` per widget with manual fallback text.
- Commentary colour setting (default: theme primary).

## 0.13.0
- Commentary on Psalms from Sefaria (verified live, tools/sefaria_commentary_probe.py): `GET /api/v3/texts/{Commentator} on Psalms {N}?version=hebrew` → `versions[0].text[verse-1]` = list of comments (flat string for Steinsaltz). v1 `texts` returns only the first verse section and `links?with_text=1` is 4 MB per chapter / times out, so neither is used.
- Reader: "פירוש" toggle (toolbar, favorites reader) shows the chosen commentary under each verse that has one, smaller, in the palette's primary colour, opening words bold; hide with a second tap. Commentators: Metzudat David (default), Rashi, Ibn Ezra, Radak, Malbim, Meiri, Steinsaltz (Settings).
- Room v3: `commentary_chapter` (migration V2_V3); local-first like the psalm text, only non-empty answers are stored; cleared with the text cache.

## 0.12.0
- Reader redesign (owner feedback from a device screenshot): one slim toolbar (Hebrew date + mode/weekday/Gregorian) replaces the stack of headers; the day-picker panel (mode switch + day strip) is collapsible and remembered; the chapter strip has its own labelled, collapsible row ("פרקי היום · פרק א׳ מתוך ט׳") so day navigation and chapter navigation can no longer be confused; one bottom row `previous | said | next`; focus mode (text only).
- Quick navigation: ⋮ menu (jump to date, browse all chapters, search, reminders, focus mode); 🔖 menu (add bookmark where I'm reading, 5 latest bookmarks, all bookmarks).
- Auto-scroll ported from kodesh-app: ▶ button when the text overflows; pause/resume, stop, speed 1–15 (8 px/s per level), % + time left, hold at a new section, hands over to the next chapter / day, pauses on a deliberate drag, keeps the screen on; defaults in Settings. Pure maths in `AutoScrollMath` (tested).
- Widgets: the reading widget now rebuilds its model INSIDE the composition from live flows (Glance reuses a running session on update(), so the old once-captured model never changed — the cause of the dead ‹ › buttons, "said" and bookmark taps); paging arrows are RTL-correct pill buttons; compact "said" pill; ⏰/🔖 badges (tap opens reminders / bookmarks) on every widget; per-widget look settings (font family/size/bold, alignment, spacing, colours, opacity, title, nikud/ta'amim/numbers) with live preview for ALL configurable widgets; bookmark widget = live list with two-tap delete, ➕, continue reading; favorites/refuah/ilui widgets: per-row said toggle and ➕ (opens the matching "add" editor).

## 0.11.1
- Fix: monthly division of Psalm 119 is verses 1–96 (stanzas א–ל) on day 25 and 97–176 (מ–ת) on day 26 (was 1–88 / 89–176).

## 0.11.1
- Fix: monthly division of Psalm 119 is verses 1–96 (stanzas א–ל) on day 25 and 97–176 (מ–ת) on day 26 (was 1–88 / 89–176). Schedule, golden tests, spec and skills updated.

## 0.11.0
- Name letters: full name(s) → Psalm 119 stanzas of each distinct letter (in order), then the four stanzas of נשמה; the reader shows the name above each stanza with its letter(s) highlighted. Saved as a favorite for a person (refuah / ilui nishmat).
- Yearly Hebrew-date reminders (yahrzeit; Adar → Adar II in leap years) that open the reading; also available in the reminder editor.
- DB v2 (migration `favorite.nameSpec`) — release builds never wipe data.

## 0.10.0
- In-place updates: one fixed signing key (`app/sideload.jks`) signs debug and release; versionCode = 100 + commit count, so every build upgrades the previous one without uninstalling.
- Share the app (APK + Hebrew install/update instructions) via WhatsApp, e-mail (optional zip for mail apps that block .apk) or any app.
- Settings → permission check (notifications, channels, exact alarms, battery optimisation, location) with one-tap fixes; re-checks on return.
- Reader widget: per-widget font size / colours / nikud / ta'amim / verse numbers or follow-the-app; ⚙ reconfigure; tap a verse number to bookmark; chapter position remembered per date and shared with the app; said + bookmarks synced.

## 0.9.0 — first feature-complete build
- Reading: monthly (Hebrew day, with the 29-day-month "כ״ט + ל׳" rule), weekly and sequential views; date navigation; swipe between chapters; chapter picker; full-text search; 119 split into its two schedule halves.
- Local-first psalms: Sefaria → Room cache, integrity-checked, prefetch-all, offline use.
- Favorites, refuah and ilui nishmat people with links; chapter/range input; name-letter and "נשמה" stanza helpers (pending halachic confirmation).
- Reminders: multiple times, daily/weekly/Hebrew-monthly/once, date ranges, snooze, sound/vibrate combinations, Shabbat/Yom Tov shifting (≥30 min before candle lighting / after havdalah) from an offline zmanim calculation; permission health card; live preview.
- Location: automatic (device) or manual (cities / coordinates) for zmanim.
- Said tracking, 30-day history with search, bookmarks (long-press a verse), continue-reading.
- Widgets (Glance): Hebrew date, weekday, configurable reader, favorites / refuah / ilui lists, bookmarks; refresh on data change and at midnight.
- Content accuracy: golden table of verse counts (2,527 verses, captured live from Sefaria) validated on every download; live `Sefaria probe` workflow found and fixed a bug that would have rejected every chapter (Sefaria's `length` is the book length).
- Day rollover setting: the day's psalms can turn over at midnight, sunset or nightfall (app, widgets, reminders, said marks).
- One-time offline download card; continue-reading chip; privacy-friendly lock-screen notifications.
- CI: unit tests, R8 release build, live Sefaria probe, emulator smoke/E2E tests with screenshots.
- Settings: theme/palette/backgrounds for light & dark, bundled Hebrew fonts, size/line-height, nikud/ta'amim toggles, prayers (verified-only), cache, backup/restore, logs.
