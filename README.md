# תהילים יומי · Tehilim Daily

Native Android app (Kotlin, Jetpack Compose, Glance widgets) for reading Tehilim — by Hebrew day-of-month, weekday, in order,
favorites (refuah / ilui nishmat), with reminders that respect Shabbat and Yom Tov, bookmarks, said-tracking and widgets.
Hebrew, right-to-left. Psalm text comes from [Sefaria](https://www.sefaria.org) and is cached on the device.

- Plan & status: [`docs/PLAN.md`](docs/PLAN.md) · Specification: [`docs/SPEC.md`](docs/SPEC.md) · History: [`docs/CHANGELOG.md`](docs/CHANGELOG.md)
- Working agreements for contributors/agents: [`CLAUDE.md`](CLAUDE.md), [`AGENT.md`](AGENT.md), [`STRUCTURE.md`](STRUCTURE.md)
- Build: GitHub Actions (`.github/workflows/android-ci.yml`) runs ktlint, detekt, unit tests and `assembleDebug`; the debug APK is an artifact.
- Content accuracy is verified live by the `Sefaria probe` workflow (all 150 chapters, verse counts, integrity).

Local build: JDK 17 + Android SDK, then `./gradlew testDebugUnitTest assembleDebug`.
