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
- Manual Save and Load controls in the in-game menu
- Auto-save through `SharedPreferences`
- Rotation restore for the active game screen
- Settings for haptic feedback, reduced motion, reset saved game, and reset records
- Per-size best record tracking
- Results screen with Play Again, New Size, Home, and solver-assisted wording
- BFS, A*, and IDA* solver controls in Assist with warnings for expensive board sizes
- Solver-assisted completions do not overwrite player best records
- Android instrumentation coverage for the main navigation and gameplay flows

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
assemble/lint, and public Javadoc/doclint checks.

For Android build and lint only:

```bat
build-debug.bat :app:assembleDebug :app:lintDebug
```

To run the shared desktop/core JUnit tests from the repository root:

```bat
android\gradlew.bat -p . test
```

To run Android instrumentation smoke tests on a connected emulator or device:

```bat
build-debug.bat :app:connectedDebugAndroidTest
```

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
- Open Menu, check Quick Reminder, and open Settings.
- Toggle haptics and reduced motion, then return to gameplay.
- Use Settings reset actions only after confirming the dialogs.
- Open Assist, choose Show Movable Tiles, and confirm the status explains the
  highlighted legal moves while the move count stays unchanged.
- Open Assist, choose an expensive solver such as BFS on 4x4, and confirm the warning dialog appears.
- Finish a game and confirm Results shows moves, time, record status, and Play Again/New Size/Home actions.
- Confirm solver-assisted completion reaches Results without updating player best records.
- Background and return to the app; autosave should preserve the current board.
- Rotate and return to portrait; the current game state should remain intact.

`build-debug.bat` uses Android Studio's bundled JBR when available, which avoids
Gradle compatibility issues with newer system Java versions.
