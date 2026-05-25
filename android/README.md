# SlideDo Android

This Android project reuses the desktop game's core Java logic from:

`../src/com/klotski/core`

Open this `android` folder in Android Studio, then run the `app` configuration on
an emulator or connected Android device.

## Current Features

- Native Android `Activity` and custom game board `View`
- Home screen on launch instead of opening directly into the board
- Mode Select for 3x3, 4x4, and 5x5 games
- How to Play and Records screens reachable before gameplay
- Tap a tile in the same row/column as the blank space to slide one or more tiles
- Swipe a movable tile toward the blank space
- Whole-line slides animate all affected tiles together and count as one move
- Compact in-game controls with Undo, Restart, Menu, and Assist actions
- Manual Save and Load controls in the in-game menu
- Auto-save through `SharedPreferences`
- Rotation restore for the active game screen
- Per-size best record tracking
- BFS, A*, and IDA* solver controls in Assist with warnings for expensive board sizes
- Solver-assisted completions do not overwrite player best records

## Build Notes

This repository includes a Gradle wrapper. Android Studio can open and sync the
project using the installed Android SDK at:

`C:\Users\LAB-606\AppData\Local\Android\Sdk`

For command-line builds, run:

```bat
build-debug.bat
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
- Confirm the app opens on Home, not directly on the board.
- Open How to Play from Home.
- Open New Game, then start 3x3, 4x4, and 5x5 from Mode Select.
- Return Home and confirm Continue appears after a game has been saved.
- Tap adjacent and non-adjacent aligned tiles; a whole-line slide should count as one move.
- Undo after a whole-line slide; the entire gesture should restore in one step.
- Restart; moves and timer should reset without reshuffling.
- Save after a move from Menu, restart, then Load; board, move count, timer, and restart grid should be restored.
- Open Assist, choose an expensive solver such as BFS on 4x4, and confirm the warning dialog appears.
- Background and return to the app; autosave should preserve the current board.
- Rotate and return to portrait; the current game state should remain intact.

`build-debug.bat` uses Android Studio's bundled JBR when available, which avoids
Gradle compatibility issues with newer system Java versions.
