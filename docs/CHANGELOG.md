# Changelog

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
