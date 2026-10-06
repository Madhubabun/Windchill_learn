# Windchill Mastery

An offline-first Android study app built from the source-mapped Windchill curriculum bundled in `app/src/main/assets/windchill_content.json`.

## Included

- 236 lesson records across 7 modules, with source IDs, source focus, release metadata, and curriculum tiers.
- Local search over lesson/module names, source focus, references, release labels, and status.
- Lesson completion, bookmarks, and an eight-item recently studied list persisted on device.
- Ten bundled multiple-choice checks with saved latest answers, score, and review of missed answers.
- Dark and light themes, portrait-first layouts, system-bar insets, and no account or server dependency.

## Source boundary

The app preserves the supplied curriculum data and its source references. Most records contain a topic title, source focus, status, and source IDs rather than complete lesson prose. The UI identifies that distinction and does not invent Windchill APIs, examples, configuration details, or other technical claims. The quiz explanations are the explanations included with the bundled checks.

## Build

Requirements: JDK 17 or newer and Android SDK 35. From this directory:

```sh
./gradlew --no-daemon :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. The same command can be run from Android Studio's Gradle tool window.

To validate the bundled lesson and quiz structure before building:

```sh
bash scripts/validate-content.sh
```

## App details

- Application ID: `com.madhubabu.windchillmastery`
- Minimum Android version: Android 8.0 (API 26)
- Target SDK: 35
- All curriculum and progress data is stored locally. The app declares no network permission.
