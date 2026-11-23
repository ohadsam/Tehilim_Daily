# Code map (read this instead of scanning the tree)
`app/src/main/java/com/ohadsam/tehilim/`
- `TehilimApp`, `AppContainer` — application + manual DI (repositories, prefs, scheduler, notifications).
- `MainActivity` — RTL root, theme from `ThemePrefs`, deep-link `route` extra → `AppRoot`.
- `domain/` — `PsalmModels` (PsalmRef/Verse/errors), `HebrewNumerals`, `TextDisplay` (nikud/ta'amim view), `RefLabels`, `schedule/PsalmSchedules` (monthly/weekly/sequential data).
- `calendar/` — `HebrewCalendar` (pure arithmetic calendar), `Holidays`, `SunCalc`, `HolyTimes` (candle lighting/havdalah, holy periods).
- `location/` — `Places`, `LocationResolver`, `LocationStore`, `DeviceLocation`.
- `data/` — `PsalmsRepository` (Room first, Sefaria fallback), `sefaria/SefariaApi+SefariaParser`, `FavoritesRepository`, `SaidRepository`, `BookmarksRepository`, `RemindersRepository`+`ReminderCodec`, `db/` (Room entities/DAOs), `prefs/` (Reader/Theme/Prayer/Log prefs).
- `favorites/` models, `ChapterSetParser`, `NameLetters`, `ChapterSummary`. `bookmarks/` model. `said/` models + `HistoryBuilder`. `search/PsalmSearch`.
- `reminders/` — `ReminderModels`, `ReminderPlanner` (pure; Shabbat/Yom Tov shifting), `ReminderContent`, `ReminderScheduler` (AlarmManager), receivers, `NotificationHelper`.
- `backup/` — `Backup` + `BackupRepository`. `prayers/` — verified-only prayer catalog (`assets/prayers.json`). `log/` — `AppLogger` ring buffer + file.
- `share/` — app sharing (`AppSharer`) and reading sharing: `SharePackage`/`ShareCodec` (file + `TD1:` code), `DivisionPlanner`, `ShareImporter`, `ShareSender`, `IncomingShare`, `SharedListsStore`; UI in `ui/share/`.
- `update/` — in-app updates: `UpdateModels` (GitHub release parser, state), `UpdateController` (check, download, install intent), `UpdateVerifier` (package / build / signature); UI in `ui/update/`.
- `widget/` — Glance widgets (live: model built inside the composition), receivers, `WidgetUpdater` (+ midnight alarm), config activities (`ReaderWidgetConfigActivity`, `WidgetStyleConfigActivity`), shared `WidgetStyleEditor`, `WidgetParts` (pills, badges, said pill).
- `ui/reader/` — `DayReader`, `ChapterPage`, `ReaderToolbar` (slim top bar + bookmark/overflow menus), `AutoScrollController` / `AutoScrollOverlay`; `domain/AutoScrollMath`, `data/prefs/AutoScrollPrefs`.
- `ui/` — `nav/AppRoot`, `today/`, `browse/`, `reader/` (`ChapterPage`, `DayReader`, `ChipStrip`, `SaidBar`), `favorites/`, `bookmarks/`, `reminders/`, `location/`, `history/`, `search/`, `settings/`, `logs/`, `more/`, `theme/`.
`tools/sefaria_probe.py` — live API contract check (CI). `docs/` — PLAN, SPEC, CHANGELOG.
