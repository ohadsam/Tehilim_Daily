# Changelog

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
