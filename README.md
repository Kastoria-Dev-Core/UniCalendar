# UniCalendar

UniCalendar is a lightweight, responsive Android calendar application built in Kotlin. It provides local calendar capabilities and an extensible schedule parsing engine designed to automatically fetch, parse, and synchronize university lecture timetables and exam programs into the user's personal calendar.

## Architecture & Technology Stack

- **UI Framework**: Modern declarative UI with Jetpack Compose and Material 3
- **Language**: Kotlin 2.0 with Kotlin Coroutines and StateFlow
- **Persistence**: Room Database (SQLite) for fast, reactive offline caching
- **Networking & Ingestion**: OkHttp with Jsoup for institutional web scraping and an RFC 5545 compliant parser for iCalendar (`.ics`) feeds
- **Build System**: Gradle 8.11 with Kotlin DSL and Version Catalogs (`libs.versions.toml`)
- **Automated CI/CD**: GitHub Actions workflow for building release/debug APKs and automating GitHub Releases

## Project Structure

```
app/src/main/java/org/kastoriadev/unicalendar/
├── UniCalendarApp.kt                # Application entry point & service locator
├── data/
│   ├── local/                       # Room entity, DAO, converters, and database
│   ├── parser/                      # HTML schedule parser, ICS parser, and factory
│   ├── repository/                  # Repository implementation
│   └── sync/                        # Network schedule fetch and synchronization manager
├── domain/
│   ├── model/                       # Domain models (CalendarEvent, EventType, SyncResult)
│   └── repository/                  # Repository contracts
└── ui/
    ├── MainActivity.kt              # Main host activity
    ├── calendar/                    # Screen, ViewModel, and UI state
    │   └── components/              # Month grid, day cell, headers, dialogs, agenda list
    └── theme/                       # Color palette, Typography, and Material 3 theme
```

## Building the Project

### Prerequisites
- Android SDK (API 35 installed)
- JDK 17 or JDK 21

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```

## University Schedule Ingestion

The app includes a dedicated timetable synchronization module:
- **Web scraping**: Parses university HTML schedules (tables, lists) extracting course titles, lecture/lab categorization, time ranges, and classroom locations.
- **iCalendar feeds**: Ingests standard `.ics` / `.ical` calendar URLs published by academic portals or LMS platforms (Moodle, Open eClass).
- **Extensible interface**: Additional custom faculty scrapers can implement the `ScheduleParser` interface and register in `ScheduleParserFactory`.

## Automated APK Builds & GitHub Releases

Continuous integration and automated delivery are configured in `.github/workflows/build-and-release.yml`:

1. **Pull Requests & Commits to `main`**: Runs tests and generates debug APK artifacts for download in GitHub Actions.
2. **Releases & Versioning**:
   - Pushing a version tag triggers automated APK compilation, checksum generation, and publishing a GitHub Release with the APK assets attached:
     ```bash
     git tag v1.0.0
     git push origin v1.0.0
     ```
   - Releases can also be triggered manually via GitHub Actions **Run workflow** (`workflow_dispatch`).
