# SPEC — Tehilim Daily
Anchors `## S<N>` match batches in PLAN.md. Language: Hebrew, RTL. Default locale he-IL.

## S0 Foundation
- Single-activity Compose app, Navigation-Compose, bottom navigation (5 tabs max): **היום** (Today), **קריאה** (Browse), **מועדפים**, **תזכורות**, **עוד** (history, settings, logs, about, what's new). Today = the default, opens straight on the current Hebrew-day chapters.
- Version shown in Settings/About (`versionName`) + What's New sheet (shown once per new version, reopenable). Content from `assets/whatsnew.json` (only the current version's bullets).
- `AppLogger`: levels D/I/W/E, tags, in-memory ring buffer + rolling file; screen to view (filter by level/tag, search), copy, share/download as .txt, clear, auto-clear after N days (default 7, settable). No sensitive names in logs unless the user opts in.
- Theme: Material 3, light/dark/system, dynamic color optional, curated "Judaica" palettes (gold/cream/navy, parchment, high-contrast). 

## S1 Data — local-first psalms
- Entities: `PsalmText(ref PK, chapter, fromVerse, toVerse, verses[json], fetchedAt)`.
- `PsalmsRepository.get(chapter, from?, to?)`: Room → else Sefaria (see skill sefaria-fetch) → store non-empty → return. Offline + missing ⇒ `PsalmError.Offline`.
- Settings: "הורד את כל התהילים לשימוש לא מקוון" (prefetch 150 with progress, resumable, Wi‑Fi only option), cache size, clear.
- A chapter's titles: Hebrew chapter name (פרק א׳…), verse numbers in letters, optional paragraph/stanza breaks for 119 (22 letters of 8 verses, heading showing the letter).

## S2 Calendar & schedules
- `HebrewDateProvider`: today's Hebrew date (day, month, year, leap), month length (29/30), day-of-week; navigate to any day/month. Optional switch at tzeit.
- Schedules (data in skill hebrew-calendar-reminders): `Sequential` (1–150), `Weekly` (7 days), `Monthly` (30 days; on 29-day months day 29 shows "כ״ט + ל׳").
- Display of a schedule day: header (day name / Hebrew date + Gregorian), list of chapters as chips with ≤ 15 chapters scrolling and **centering the active chip**, "פרק N מתוך M" position line, prev/next buttons (null-safe: omit the button at boundaries — never render a broken one), day selector (strip of days + date picker), jump to today, swipe between chapters. Each chip shows said ✓ state.

## S3 Reader UI
- Chapter view: large readable text (user font/size/line spacing), verse numbers (optional), nikud on/off, tap verse to highlight, keep screen on (opt), auto-scroll with speed control (opt), scroll-to-top on chapter change, sticky mini header with chapter + position, "mark as said" bar at the bottom of every day-based view, share/copy chapter.
- Views, all sharing one `ReaderScreen(context)` where context ∈ {Sequential, Weekday(d), MonthDay(n), Favorite(id), Person(id), Reminder(id), Search(q), Manual(ch)}; prev/next inside the context; going prev from first chapter of a day moves to previous day only for day contexts (not favorites/persons).
- **Manual-selection history** (in-memory, cleared on leaving the tab) of chapters visited via picker/search: shown as sorted chips.
- Chapter picker: grid of 150 letters (א…קנ) with said-today dots; Hebrew-letter (gematria) or numeric search.

## S4 Favorites & persons
- Favorite = `{id, name, chapters[] (expanded from singles and ranges), color/icon, createdAt, personId?}`. Add from a chapter (★ toggles a single-chapter favorite), or from the form: pick chapters/ranges (e.g. 20–24 + 91) with chip preview, reorder.
- Person = `{id, name, kind: REFUAH|ILUI_NISHMAT, motherName (לרפואה: "בן/בת פלוני בן/בת פלונית"), hebrewBirthDate?, hebrewDeathDate? (yahrzeit)}`. Name written highlighted in the reader header and widget. Optional shortcut sets: "שמו — אותיות השם" (psalm 119 verses spelling the name; each letter's 8-verse stanza) and "נשמה" sets (119 stanzas נ-ש-מ-ה) for ilui nishmat — selectable presets.
- Link: a favorite may be linked to 0..n persons and a person to 0..n favorites; the reader shows the person's name(s) and the proper opening/closing "יהי רצון" text (static texts, sources verified in docs, editable by the user).
- Lists: tabs inside מועדפים: הכל | לרפואה | לעילוי נשמת. Search, sort (manual/recent/name), drag to reorder, swipe to delete with undo, duplicate, archive.
- Yahrzeit: ilui nishmat with Hebrew death date creates optional annual reminder (the day before and the day of) — adjustable.

## S5 Said-tracking & history
- Per context and day: mark **נאמר** on any day-view (weekday, month-day, favorite, person). Stored as `SaidEntry(date, contextType, contextId, markedAt)`; chapter-level checks optional. A mark applies to the civil date; resets visually next day. Marked ⇒ today's reminders for that context are cancelled/suppressed.
- History screen: descending list of days (up to 30 days back, auto-purge older), per day the contexts said/not said, search/filter (by context, person, said/not said), tap a day to toggle/correct. Weekly streak summary (suggested extra).

## S6 Reminder engine
- Reminder = `{id, targetType: FAVORITE|PERSON|LINKED_GROUP|MONTH_DAY|WEEKDAY|SEQUENTIAL, targetId, times[] (≥1 per reminder, any number of reminders per target), repeat: ONCE|DAILY|WEEKLY(days)|MONTHLY_HEBREW(day)|YAHRZEIT, startDate?, endDate? (from–until, Gregorian or Hebrew), dueDate (one-off), alert: {notification: Boolean, sound: Boolean(+uri), vibrate: Boolean(+pattern)} (any combination, at least notification-or-sound-or-vibrate), snooze: {enabled, minutes[5,10,15,30,60], maxCount}, shabbatRule: BEFORE|AFTER|BOTH|SKIP|NORMAL, enabled}`.
- Linking: one reminder can cover a favorite, a person, or a favorite+person group.
- Scheduling: AlarmManager `setExactAndAllowWhileIdle` (USE_EXACT_ALARM/SCHEDULE_EXACT_ALARM with fallback to inexact and an explanation screen) — one next-occurrence alarm per reminder, rescheduled on fire, boot (`BOOT_COMPLETED`), timezone/date change, app update, and after any edit. WorkManager daily "repair" job as a safety net. Notifications channels per alert combination (Android channels fix sound/vibration) + POST_NOTIFICATIONS flow (API 33).
- Notification: title (Hebrew date + target name), body (chapters + person name), actions: **נאמר** (marks said), **נודניק** (snooze), **פתח**. Tapping opens the correct reader context. Suppressed if already marked said today. Snooze re-fires up to maxCount.
- Shabbat/Yom-Tov rule: see skill `hebrew-calendar-reminders` (≥30 min before candle lighting, ≥30 min after havdalah), from the offline Hebrew calendar + zmanim at the user's location (GPS opt-in or city list; default Jerusalem).
- Extra: "do not disturb" quiet hours, per-reminder test button, battery-optimization/exact-alarm health check screen, missed-reminder indicator.

## S7 Reminders UI
- Tab תזכורות: list grouped by target, next fire time (computed incl. Shabbat shift), on/off switch, swipe actions. Create/edit form as a stepper/bottom sheet: target → schedule → alerts → range/dates → Shabbat rule → preview "הפעמים הקרובים" (next 5 occurrences). "מתוך תזכורת" browse source: a list of reminders; tapping opens the reader with that reminder's chapters.

## S8 Widgets (Glance)
1. **Reader widget** (resizable, configurable): source ∈ favorite | month-day (auto-current or fixed) | weekday (auto/fixed) | refuah person | ilui nishmat person. Shows chapter text (scrollable list), chapter chip list with ◀ ▶ navigation, person name highlighted, **נאמר** toggle (auto-clears next day; hides/cancels that day's reminder).
2. **List widget**: favorites / לרפואה / לעילוי נשמת (also separate widget kinds). Item tap deep-links into the app on that favorite/person.
3. **Hebrew date widget** (small): Hebrew date + day-of-month; tap → app Monthly view on that date. **Weekday widget**: tap → weekday view.
4. Suggested extras: "Today's chapters" glanceable widget (chips + said ✓), "Next reminder" widget, streak widget.
- Widgets update on date change (`ACTION_DATE_CHANGED`, `TIMEZONE_CHANGED`, midnight alarm, tzeit if enabled), on data change in the app (same DB/Flow → `updateAll`), and on said-toggle. Config activity per widget. Dark/light/Material-You aware, rounded, accessible.
- Deep links: `tehilim://month/{hebrewDay}?m=...`, `tehilim://weekday/{d}`, `tehilim://favorite/{id}`, `tehilim://person/{id}`, `tehilim://chapter/{n}`.

## S9 Settings
Single settings home with sections: Appearance (theme light/dark/system, palette, background colors for light and dark, accent, font family [bundled Hebrew fonts: Frank Ruhl Libre, Heebo, David Libre, Assistant, Noto Serif Hebrew, system], font size slider 12–48sp, line height, nikud, verse numbers, text alignment), Reading (keep screen on, auto-scroll), Calendar (location/city, Israel/diaspora, day-switch at tzeit, candle-lighting minutes, havdalah method), Data (offline download, cache, backup/restore JSON export/import, clear all), Reminders (permissions health, default alerts, quiet hours, list of all reminders), Favorites management, Logs (viewer, copy, download, clear, auto-clear), About (version, What's New, licenses, Sefaria attribution & texts license).

## S10 Quality bars
- Security: no analytics/ads/trackers; personal names local only; `allowBackup` limited to a user-controlled export; optional biometric/app lock for names; network only to sefaria.org over HTTPS (cleartext disabled, no custom trust); minimal permissions (INTERNET, POST_NOTIFICATIONS, exact alarm, BOOT_COMPLETED, VIBRATE, location coarse opt-in); R8 on release; exported components minimized; widget/intent extras validated; dependency scan in CI.
- Performance: cold start < 1.5s; chapter render from Room < 100 ms; LazyColumn for text; baseline profile; no work on main thread; widgets cheap.
- A11y: TalkBack labels, font scale 200%, contrast AA, RTL mirrored, reduced motion respected.
- Tests: JUnit5 unit tests for schedules/date/reminder-shift/repository (fake API); Compose UI tests for key flows run on CI emulator in the final batches; screenshot checks for light/dark/RTL.

## Suggested additions (included in PLAN)
Yahrzeit auto-reminders; backup/restore; reading progress & streaks; share chapter as text/image; quick-chapter picker grid; reading position memory ("המשך מאיפה שהפסקתי"); biometric lock for names; app shortcuts (long-press icon → Today / Favorites); Quick Settings tile to open Today; "יהי רצון" prayers pre/post; Hebrew date + parasha line in header (offline).

## Addendum A (user requests, batch-0 planning)
### A1 Location for zmanim (S2/S9)
Settings → מיקום: (a) **אוטומטי** — coarse/fine location via platform `LocationManager` (runtime permission `ACCESS_COARSE_LOCATION`, one-shot, cached, refreshed when the user moves > 10 km or on demand; no Google Play Services dependency) with timezone/elevation from device; (b) **ידני** — pick from a built-in list of Israeli cities and major world Jewish communities (name, lat, lon, tz, elevation) with search; (c) **קואורדינטות מותאמות** (lat/lon/elevation). Fallback chain: manual choice → last good auto fix → Jerusalem. Israel/diaspora is derived from the chosen place (overridable). The screen shows the resolved place, candle-lighting & havdalah times for the next Shabbat as a sanity preview. Permission denial never blocks the app.
### A2 "יהי רצון" prayers (S4/S9)
Settings → תפילות: show before/after reading on/off (separately for before and after, and for refuah / ilui nishmat / general), **nusach picker** (e.g. אשכנז, ספרד, עדות המזרח, חב״ד — only texts whose source is verified and recorded in `assets/prayers/*.json` with `source` field), preview, and per-text font size. Texts are data files, never inline strings.
### A3 Content accuracy (global, critical)
- Source of truth for text = Sefaria Hebrew (Miqra according to the Masorah / default `he` version); record `versionTitle` and `fetchedAt` with each cached row. Show the source on the About screen.
- Integrity checks on every fetch: non-empty; verse count == Sefaria's reported `length` for whole chapters; range requests return exactly the requested verses (slice locally if Sefaria returns the whole chapter); no HTML residue; Hebrew letters present. A failed check ⇒ not cached, retry, user-visible error — never silently show partial text.
- Schedules are data with **golden tests**: monthly = 30 days covering chapters 1–150 each exactly once (119 split 1–96 (א–ל) / 97–176 (מ–ת)); weekly = 7 days covering 1–150 each once. Weekly split used: Sun 1–29, Mon 30–50, Tue 51–72, Wed 73–89, Thu 90–106, Fri 107–119, Shabbat 120–150 (cross-checked against the common Perek-Yomi/Tehillim weekly table; also verified against Sefaria in the CI probe).
- Never edit/normalize letters of the text beyond tag stripping and whitespace; nikud/ta'amim display is a *view* option, not a data change.
- CI job `sefaria-probe` fetches all 150 chapters, checks the above and uploads a report.
### A4 Bookmarks (סימניות) — ported from kodesh-app, extended (S11, batch 5b)
Port: save the exact reading position — context + chapter + **verse anchor + scroll offset** — by **long-press on a verse** (hold time configurable 0.25–1.5s) or via a "place bookmark" tap mode; a top bar in the reader shows "🔖 label «snippet» [עבור לסימניה] [✕]"; jumping restores context then scrolls so the same verse sits at the same height, with correction for late layout; a flag marks the bookmarked verse (long-press flag deletes, snackbar Undo); reminder on a bookmark (next open / every open / fixed time, once/daily); clear-all with confirm + Undo.
Upgrades (user asked for suggestions): multiple bookmarks (not one per tab) + one automatic **"המשך מהמקום שבו הפסקתי"** per context; label, color and note per bookmark; pin to top; bookmarks screen (search, sort, filter by color/context, swipe delete + undo, drag reorder); verse-range highlight; bookmark of a chapter in a favorite; share a verse as text; widgets: **Bookmark widget** (shows label + verse snippet, tap → exact position, ◀ ▶ between pins) and a "continue reading" widget; Today screen shows a "continue" card; bookmarks included in backup/restore; snippet always read from the local text (accuracy).

## Addendum B (owner decisions, 2026-10-11)
### B1 Name letters (confirmed custom)
Input: one or more names (joined into the full name). Output: for every distinct Hebrew letter of the full name, in order of first appearance, the 8-verse stanza of Psalm 119 for that letter (final letters = regular letters); then, optionally, the stanzas of נ-ש-מ-ה (always complete, even if the same stanza already appeared for the name). Above each stanza the reader shows the name ("נשמה" for the last four) with all occurrences of that stanza's letter highlighted. Saved as a favorite linked to a person (refuah / ilui nishmat); with a yahrzeit date a yearly reminder (target: favorite + person) is created and opens the reading. Implemented: `NameLetterPlan`, `NameLettersScreen`, `Repeat.YearlyHebrew`.

## Addendum C (owner feedback after first device test, 2026-10-12)
### C1 Reader layout, quick navigation, auto-scroll, bookmarks, widgets
- Text first: the reader screen gets one slim toolbar; every other area is collapsible (day panel, chapter strip) or hidden in focus mode. Day-selection and chapter-selection are separate, labelled strips.
- Auto-scroll behaves like kodesh-app (`js/autoscroll.js`): levels 1–15 at 8 px/s per level; pause/resume/stop; progress + ETA computed from real position; hold of N seconds on a new section; keeps going into the next chapter/day (setting); pauses on a deliberate vertical drag (setting); keeps the screen on (setting).
- Bookmarks (kodesh-app parity, ours are verse-based and multiple): add by long-press, add at the verse currently at the top of the screen, jump to recent/all from the toolbar, undo on delete, reminders on bookmarks (existing), widgets show/delete/add.
- Widgets: all data shown is rebuilt inside the Glance composition from live flows (never captured once in `provideGlance`); every configurable widget has the same style editor; reminder/bookmark badges open the matching screen; actions from widgets (page, said, bookmark toggle/delete, add) write to the same repositories as the app.

### C2 Commentary (פירוש)
Sefaria offers commentary on Psalms by Rashi, Metzudat David/Zion, Ibn Ezra, Radak, Malbim, Meiri, Steinsaltz, Alshich and others. The app supports seven (see `CommentarySource`). Fetch one commentator per chapter with the v3 text API (shape: `text[verse-1]` = list of comments); cache per (chapter, commentator); display under the verse (never alters the psalm text); attribution line under the chapter title; toggle in the reader toolbar, default off; source chosen in Settings. Ranges (e.g. 119:1-96) show only the commentary of the verses in the range.

### C3 Widget follow-ups (owner device test, 2026-10-13)
Arrows in RTL: right = previous, left = next (use direction-aware non-mirrored glyphs). Progress label "current – last (n/total)". Every reading widget must expose add / open-edit / delete (confirmed) bookmark controls and a bookmarks chip. A new chapter always starts at its top. One widget entry per source kind (month/week generic, favorite, refuah, ilui). Widgets can be added from inside the app via the pin-widget request.

### C4 Widget landing screen (owner request 2026-10-13)
Reading widgets start on a designed landing screen (default on, switchable per widget): app icon, Hebrew and Gregorian date, source title, chapter range of the day. Next → reading; previous from the first chapter → landing; new day → landing again. Position stays shared with the app (landing is widget-only state).

### C5 Widget bookmark UI (owner feedback 2026-10-13)
No permanent per-verse add buttons. A single toolbar button toggles add mode; bookmarked verses always show their marker with removal.
