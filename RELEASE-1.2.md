# TapToFlip 1.2 (version code 6)

- Target and compile SDK 36; update Fragment to 1.9.1.
- Redesign the home screen and add local records, daily goals, score sharing, and optional return reminders.
- Support rotation and resizable windows while retaining the current run.
- Restore the frog's original 88-pixel size limit. Only orientation changes pause a run; HUD and inset layout changes keep it running.
- Refresh frog rendering from the observable frame clock; stop the frame loop while paused or after game over.

## Validation

- 21 unit tests passed (17 GameEngine and 4 ReminderPolicy), including regression coverage for frog size and HUD resizing.
- Debug build succeeded; the updated debug APK was installed on Pixel 9 and gameplay was confirmed by the user.
- Earlier adaptive layout and frog rendering instrumentation tests passed for phone and tablet profiles on API 34.
- A signed 1.2 bundle with version code 6 and target SDK 36 was uploaded to Play Console by the user.

## Build and release

Use JDK 17 with the checked-in Gradle wrapper. Generate a signed release bundle with the existing upload key. Keep signing credentials and generated APK/AAB files outside commits. For the next release, increment versionCode above 6.
