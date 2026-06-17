# SlideDo Android

This Android project reuses the desktop game's core Java logic from:

`../src/com/klotski/core`

Open this `android` folder in Android Studio, then run the `app` configuration on
an emulator or connected Android device.

## Current Features

- Native Android `Activity` and custom game board `View`
- Home screen on launch instead of opening directly into the board
- First-run onboarding with Skip and Start 3x3 actions
- Interactive Practice Tutorial for the first move, movable aligned-tile
  highlights, and whole-line slide practice
- Mode Select for 3x3, 4x4, and 5x5 games
- Visual How to Play, Beginner Guide, Quick Reminder, and Records screens
- Tap a tile in the same row/column as the blank space to slide one or more tiles
- Swipe a movable tile toward the blank space
- Whole-line slides animate all affected tiles together and count as one move
- Compact in-game controls with Undo, Restart, Menu, and Assist actions
- Lightweight Assist hint that highlights movable aligned tiles without moving
  the board
- Board, highlighted movable tiles, primary game controls, and settings switches
  expose accessibility descriptions for screen readers
- Manual Save and Load controls in the in-game menu
- Auto-save through `SharedPreferences`
- App-state persistence is isolated in `AndroidGameStore` for saves, settings,
  records, onboarding, and last selected size
- Activity state restoration, back-navigation decisions, shared UI primitives,
  and learning-content builders are split out of `MainActivity` for maintainable
  Android iteration
- Home screen construction, including Continue metadata presentation, is split
  into `AndroidHomeScreen`
- Game and Practice Tutorial screen construction is split into
  `AndroidGameScreen` and `AndroidTutorialScreen`; `MainActivity` still owns
  model state, command gates, and navigation callbacks
- Mode Select, Records, Settings, and Results construction is split into
  package-private builders while `MainActivity` owns navigation, persistence,
  settings application, and record-result text
- Saved-game metadata includes updated time, puzzle size, move count, elapsed
  time, and active/solved state; Home now surfaces this metadata next to
  Continue
- Release versioning is shared through the repository-root `version.properties`
  file
- Signed release APK/AAB builds are available through `build-release.bat`
- Rotation restore for the active game screen
- Settings for haptic feedback, reduced motion, reset saved game, and reset records
- Per-size best record tracking
- Results screen with Play Again, New Size, Home, and solver-assisted wording
- BFS, A*, and IDA* solver controls in Assist with warnings for expensive board sizes
- Solver-assisted completions do not overwrite player best records
- Android instrumentation coverage for the main navigation, Continue metadata,
  Activity state/navigation helpers, and gameplay flows
- Connected test helpers wait for the foreground app window and include a swipe
  fallback for long help screens to reduce slow-emulator false failures

## Build Notes

This repository includes a Gradle wrapper. Android Studio can open and sync the
project using the installed Android SDK at:

`C:\Users\LAB-606\AppData\Local\Android\Sdk`

For command-line builds, run:

```bat
build-debug.bat
```

To run the full local verification suite from the repository root:

```bat
verify.bat
```

The verification script runs shared core tests, desktop compilation, Android
assemble/lint, Android instrumentation test APK assembly, and public
Javadoc/doclint checks.

For Android build and lint only:

```bat
build-debug.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

For signed release APK/AAB generation:

```bat
build-release.bat
```

Release outputs are written to:

```text
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

For real Play Store builds, copy `release.properties.example` to
`release.properties` and configure the upload keystore. The script creates a
temporary test key only when no signing configuration is present so local release
verification can still run.

Play Store listing copy, privacy policy draft, Data Safety draft, asset
checklist, feature graphic source, and pre-launch matrix are tracked in
[`PLAY_STORE_READINESS.md`](PLAY_STORE_READINESS.md).
Store asset sources live under [`store-assets`](store-assets/); export and
review final upload files before Play Console submission.

To check the repo-side Play Store readiness files after building release
artifacts:

```bat
check-play-store-readiness.bat
```

The root `verify-release.bat` command runs this check after the Android release
and desktop package steps.

To run the shared desktop/core JUnit tests from the repository root:

```bat
android\gradlew.bat -p . test
```

To run Android instrumentation smoke tests on a connected emulator or device:

```bat
..\verify-connected.bat
```

The connected suite is expected to run against a healthy foreground emulator or
device. If an AVD system service crashes, restart the emulator and rerun the
suite before treating the failure as an app regression.

To capture a repeatable manual screenshot smoke set:

```bat
screenshot-smoke.bat
```

The script launches the app and stores PNG files under
`../screenshots/android/<version>` as you navigate through Home, Mode Select,
Game, How to Play, Settings, Records, and Results/current screen.

## Emulator Smoke Test Checklist

- Install `app/build/outputs/apk/debug/app-debug.apk`.
- Launch `com.klotski.android/.MainActivity`.
- If the app is not visible in the launcher, run `adb install -r app/build/outputs/apk/debug/app-debug.apk` and `adb shell am start -n com.klotski.android/.MainActivity`.
- Clear app data when checking first-run behavior, then confirm onboarding appears before normal play.
- Use Skip, Practice Tutorial, and Start 3x3 in onboarding; returning launches should go to Home.
- Confirm the app opens on Home, not directly on the board.
- Open Beginner Guide, Practice Tutorial, and How to Play from Home.
- In Practice Tutorial, tap the emphasized 6 for the first move, then tap the
  emphasized 5 to demonstrate a whole-line slide counted as one move.
- Open New Game, then start 3x3, 4x4, and 5x5 from Mode Select.
- Return Home and confirm Continue appears after a game has been saved.
- Tap adjacent and non-adjacent aligned tiles; a whole-line slide should count as one move.
- Undo after a whole-line slide; the entire gesture should restore in one step.
- Restart; moves and timer should reset without reshuffling.
- Save after a move from Menu, restart, then Load; board, move count, timer, and restart grid should be restored.
- Confirm saved-game metadata updates with the saved size, move count, elapsed
  time, and active/solved state.
- Open Menu, check Quick Reminder, and open Settings.
- Toggle haptics and reduced motion, then return to gameplay.
- Use Settings reset actions only after confirming the dialogs.
- Open Assist, choose Show Movable Tiles, and confirm the status explains the
  highlighted legal moves while the move count stays unchanged.
- With a screen reader or inspection tool, confirm the board describes its size,
  empty-cell position, row-by-row tile state, and highlighted movable-tile count.
- Confirm Undo, Restart, Assist, Menu, and Settings switches expose descriptive
  accessibility labels.
- Open Assist, choose an expensive solver such as BFS on 4x4, and confirm the warning dialog appears.
- Finish a game and confirm Results shows moves, time, record status, and Play Again/New Size/Home actions.
- Confirm solver-assisted completion reaches Results without updating player best records.
- Background and return to the app; autosave should preserve the current board.
- Rotate and return to portrait; the current game state should remain intact.

`build-debug.bat` uses Android Studio's bundled JBR when available, which avoids
Gradle compatibility issues with newer system Java versions.
