# Changelog

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
