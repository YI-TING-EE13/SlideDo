# SlideDo - Number Klotski Puzzle
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Desktop UI](https://img.shields.io/badge/UI-Java%20Swing-4CAF50.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-green.svg)](https://developer.android.com/)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.5-02303A.svg)](https://gradle.org/)

**SlideDo** is a polished number Klotski / sliding puzzle game written in Java. It includes a desktop Swing edition and a native Android edition that share the same core puzzle model, move rules, save format, and solver interfaces.

The design goal is simple: make sliding numbered tiles feel fast, clear, and satisfying. The desktop version now opens on Home and supports mouse, keyboard, undo/redo, move history, restart, save/load, local records, a Records dialog, Preferences, Android-style Results, solver playback, How to Play, Practice Tutorial copy, and movable-tile assist hints. The Android edition adds a deterministic strategic next-move hint to its touch-first onboarding, tutorial, modes, settings, results, records, and compact game controls. Both editions explain that player records prefer fewer moves, break ties by faster time, and exclude assisted completions.

---

## Key Features

- **Shared Java Game Core**: `GameModel`, `Direction`, save data, records, and solvers are independent from Swing and Android.
- **Fluid Desktop Controls**:
  - Desktop opens on Home with 3x3, 4x4, 5x5, Continue/Load, How to Play, Practice Tutorial, Records, and Preferences.
  - Click a tile in the same row or column as the empty space.
  - Non-adjacent clicks slide the whole row or column in one synchronized animation.
  - Arrow keys move the empty space one step.
  - Movable tiles show a hand cursor on hover.
  - Assist > Show Movable Tiles highlights legal same-row or same-column choices without moving the board.
  - Help includes How to Play and Practice Tutorial dialogs aligned with the Android learning flow.
  - Preferences includes a reduced-motion option that snaps tile movement without changing puzzle rules or records.
  - Results show first-record, new-best, unchanged-best, and solver-assisted no-record wording.
- **Mobile-Ready Interaction Model**:
  - Android opens on Home with Continue, New Game, Beginner Guide, Practice Tutorial, How to Play, Settings, and Records.
  - First-run onboarding introduces the goal, tap/swipe input, whole-line slides, undo/restart, and record rules.
  - Practice Tutorial guides one first move, highlights movable aligned tiles, and demonstrates a whole-line slide through the shared model.
  - Mode Select starts 3x3, 4x4, or 5x5 games, then offers Relaxed, Classic,
    or Challenge shuffle depth. The movement rules stay identical.
  - Difficulty is preserved in Continue, the game/result screens, and local
    records; older saves and records remain available as Classic.
  - Visual How to Play examples and an in-game Quick Reminder explain the key movement rules.
  - Screen changes use a short exit followed by staggered content entrance;
    Reduced motion skips both screen transitions and board movement animation.
  - Home groups play, learning, and personal actions so the main path remains
    visible without giving every action the same visual weight.
  - Daily Challenge creates one reproducible offline 4x4 Classic puzzle for
    each device-local date. Its localized month calendar can resume or replay
    any earlier date from an independent slot, marks completion/progress/missed
    dates, blocks future dates, and tracks current and best streaks without a
    server.
  - Trends & Weekly Goal compares recent and previous player solves only within
    one selected size/difficulty and tracks a private Monday-to-Sunday target
    from 1 to 50 without analytics or network access.
  - Continuous Challenge chains 3, 5, or 10 puzzles at one selected size and
    difficulty. The isolated session can be exited and resumed, records each
    puzzle separately, protects player bests from assisted results, and shows
    aggregate move/time progress without changing normal, daily, or favorite
    practice saves.
  - Whole-line tap behavior maps naturally to touch screens.
  - One user gesture counts as one move.
  - Undo restores the entire previous user action.
  - Redo reapplies one undone action, and Move History lists completed actions
    plus the available Redo count using the empty-cell direction convention.
  - Android includes icon-plus-text controls, an outlined empty cell with a first-move prompt, menu-based save/load, autosave, persistent English, Traditional Chinese, and Japanese language selection, optional local sound feedback, Midnight/Ocean themes, haptic and reduced-motion settings, local best records, and a short completion-mark settle on Results.
  - Settings can export all Android saves, records, statistics, daily state, and
    preferences to a versioned local JSON document, then validate and restore a
    selected backup after explicit confirmation.
  - The play timer excludes time spent in game dialogs, non-game screens, and
    the background, then resumes when the Game screen becomes interactive.
  - Results offers Replay Puzzle, which restores the exact starting board and
    resets moves and active-play time without generating a new scramble.
  - Assist offers a strategic next-move hint and non-assisted Show Movable Tiles;
    BFS, A*, and IDA* remain under advanced Solver Tools. Strategic and solver
    assistance persist with the run and cannot replace player best records.
  - Android exposes board state, empty-cell position, highlighted movable tiles,
    and primary controls through localized accessibility descriptions. Each board
    cell is also a virtual screen-reader child, and movable tiles can be activated
    without touch coordinates.
  - Dense action groups stack for 1.3x-or-larger text, scrollable content is
    centered and bounded on wide windows, game focus order is explicit, action
    targets remain at least 48dp, and button text/icons automatically choose a
    foreground with at least 4.5:1 contrast in both themes.
- **Quality-of-Life Gameplay**:
  - Undo, Redo, and Move History.
  - Restart current puzzle without reshuffling.
  - Move counter and active-play timer with pause/resume persistence.
  - Local best records by puzzle size and difficulty.
  - JSON save/load with difficulty and legacy `.dat` save compatibility.
- **Guaranteed Solvable Puzzles**: New games are generated by applying valid
  moves from a solved board. Equal size, difficulty, and seed values can also
  reproduce the same starting board for future replay modes.
- **Solver Support**:
  - BFS for small 3x3 puzzles.
  - A* for guided search.
  - IDA* for lower-memory solving experiments.
- **Native Android Project**:
  - Gradle wrapper included.
  - Custom Android view.
  - Home, offline Daily Challenge, resumable Continuous Challenge, onboarding,
    interactive Practice Tutorial, Mode Select, visual How to Play, Settings,
    Records, Results, coordinated screen transitions, English, Traditional
    Chinese, and Japanese localization, icon-plus-text controls, empty-cell
    guidance, touch controls, synchronized whole-line animation, optional local
    tones, persistent Midnight/Ocean themes, haptics, autosave, manual
    save/load, action-history persistence, local JSON backup/restore, best
    records, Undo/Redo, Move History, Assist hints,
    actionable board accessibility nodes, adaptive large-text/wide-window
    layout, nested solver controls, and instrumentation
    tests.

---

## Installation

### Prerequisites

| Requirement | Recommended Version | Notes |
| :--- | :--- | :--- |
| Java JDK | 17 or newer | Desktop build uses `javac` directly. |
| Windows | 10 or newer | `run.bat` is provided for desktop play. |
| Android Studio | Current stable | Required for emulator/device workflows. |
| Android SDK | API 36 installed | Android app targets modern SDKs. |
| Android device/emulator | API 26+ | Required only for Android testing. |

The Android project includes its own Gradle wrapper under `android/`, so a global Gradle installation is not required.

---

## Desktop Usage

From the project root:

```bat
run.bat
```

The script compiles all Java files under `src/` into `bin/`, then launches:

```bat
java -cp bin com.klotski.ui.MainFrame
```

### Manual Desktop Build

```bat
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
java -cp bin com.klotski.ui.MainFrame
```

### Desktop Package

Build a distributable desktop ZIP package from the shared release version:

```bat
package-desktop.bat
```

The package is generated under:

```text
dist/desktop/SlideDo-<version>.zip
```

When a JDK with `jpackage` is available, the script also creates a Windows
app-image under `dist/desktop/app-image/SlideDo`. The ZIP includes
`SlideDo.jar`, `SlideDo.bat`, a tester-ready package README with runtime
requirements, smoke-test prompts, known limits, and the matching release notes.

### Desktop Controls

| Action | Control |
| :--- | :--- |
| Slide a row or column | Click any tile aligned with the empty space |
| Move one step | Arrow keys |
| New 3x3 / 4x4 / 5x5 | `Game` menu |
| Restart current puzzle | `Ctrl + R` |
| Undo | `Ctrl + Z` |
| Save | `Ctrl + S` |
| Load | `Ctrl + O` |
| Exit | `Ctrl + Q` |

### Desktop Save Files

The desktop edition writes local save and record files to the user-data
directory instead of the project root:

```text
Windows: %APPDATA%\SlideDo
Other platforms: <user.home>/.slidedo
```

The default filenames are:

```text
klotski_save.json
klotski_records.json
```

For portable test or beta builds, set the JVM property
`slidedo.data.dir=<path>` to override the directory. If older
`klotski_save.json`, `klotski_save.dat`, or `klotski_records.json` files exist
in the project root, the loader still reads them as a migration fallback.

---

## Android Usage

See [android/README.md](android/README.md) for Android-specific notes.

### Build Debug APK

```bat
cd android
build-debug.bat
```

Or run Gradle directly:

```bat
cd android
gradlew.bat :app:assembleDebug
```

The APK is generated at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

### Build Signed Release APK/AAB

The shared app version is stored in [version.properties](version.properties).
Every release must also have matching notes under `release-notes/<version>.md`.

```bat
android\build-release.bat
```

The release script builds:

```text
android/app/build/outputs/apk/release/app-release.apk
android/app/build/outputs/bundle/release/app-release.aab
```

For Play Store builds, copy
`android/release.properties.example` to `android/release.properties` and point
it at the real upload keystore. The same signing values can be supplied through
`SLIDEDO_RELEASE_*` environment variables or Gradle `-Pslidedo.release.*`
properties. If no signing configuration exists, the script creates a temporary
test upload key only to verify the pipeline; do not use that key for store
distribution.

### Install on a Device or Emulator

```bat
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.klotski.android/.MainActivity
```

The Android UI opens on Home, offers Continue when a valid save exists, and
starts new games through Mode Select. First-run onboarding appears before normal
play until the player skips it, starts 3x3, or opens Practice Tutorial. Practice
Tutorial uses a small guided 3x3 puzzle to teach the first move, highlight
movable aligned tiles, and demonstrate a whole-line slide. During gameplay, Undo
and Restart stay visible while Save/Load, Quick Reminder, and Settings live in
Menu. Assist offers Strategic Hint, a non-assisted Show Movable Tiles option,
and Solver Tools. The strategic hint recommends one adjacent tile without
moving the board and marks the run assisted; that state survives save, rotation,
Restart, and replay so assisted wins cannot overwrite player best records. BFS,
A*, and IDA* remain one level deeper under Solver Tools.

Home also opens **Favorite Puzzles**. Save the current exact starting board from
Game Menu or Results, give it a local name, then Replay, Rename, or Remove it in
the library. Favorite replay is an isolated practice run: rotation and
background saves remain available, but normal/daily saves, best records,
completion history, statistics, and streaks are not changed. The library keeps
the newest 50 identities and is included in offline backup/restore.

Home also opens **Trends & Weekly Goal**. Set a weekly target from 1 to 50
player solves, then choose any size and difficulty to compare recent average
moves and time with the immediately preceding equal-size sample window. A
comparison starts after six player solves; solver-assisted and favorite
practice results never influence either the trend or weekly progress.

English is the Android default even when the device uses another language. Open
Settings, choose **App language**, and select **English**, **繁體中文**, or
**日本語**. The selection is saved locally and remains active after the app is
restarted.

Settings also offers a **Sound feedback** switch, off by default, for short local
move and completion tones. **Visual theme** switches between the default
Midnight palette and Ocean. Both choices remain local and persist across app
relaunches without changing the current puzzle, saved games, or records.

Under **Local Data**, **Export backup** creates a versioned JSON document through
Android's system file picker. **Import backup** validates the selected document,
shows a replacement warning, and restores all Android saves, records,
statistics, daily progress, and settings only after confirmation. The backup
flow remains offline and does not request broad storage permission.

If the emulator does not show SlideDo in the launcher, reinstall and start it
directly:

```bat
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.klotski.android/.MainActivity
```

### Suggested Emulator Profile

For smooth local testing:

| Setting | Recommendation |
| :--- | :--- |
| Device | Pixel 7 or Pixel 8 |
| System image | Google APIs x86_64 |
| Android version | API 35 or API 36 |
| RAM | 2 GB minimum, 4 GB preferred |
| Graphics | Hardware / automatic |

---

## Architecture

```text
SlideDo/
  src/
    com/klotski/
      core/
        Direction.java       # Empty-tile movement directions
        GameModel.java       # Shared board state and rules
        GameObserver.java    # UI notification contract
        SaveManager.java     # Desktop JSON saves and records
        Solver.java          # Solver strategy interface
        BfsSolver.java       # Breadth-first solver
        AStarSolver.java     # A* solver
        IdaStarSolver.java   # Iterative deepening A* solver
      ui/
        MainFrame.java       # Swing application frame
        BoardPanel.java      # Swing board rendering and input
  android/
    app/                     # Native Android app
    gradle/wrapper/          # Gradle wrapper
  benchmark/                 # Deterministic shared-solver benchmark
  benchmark.bat              # Benchmark compile/run helper
  run.bat                    # Desktop compile/run helper
  build.gradle               # Root desktop/core test build
  README.md
  LICENSE
```

### Core Design

`GameModel` is the source of truth. It owns:

- Board values.
- Empty tile position.
- Move count.
- Active elapsed-time and pause/resume state.
- Undo snapshots.
- Restart state.
- Win detection.

Desktop and Android UI layers observe the model through `GameObserver`, keeping platform-specific rendering separate from puzzle rules.

### Move Semantics

`Direction` always describes where the **empty tile** moves. For example:

- `Direction.UP`: the empty space moves up; the numbered tile above it visually moves down.
- `Direction.RIGHT`: the empty space moves right; the numbered tile to the right visually moves left.

Whole-line slides are handled by `GameModel.slideLineTo(row, col)`. This method:

- Requires the selected tile to be aligned with the empty tile.
- Slides every tile between the selected tile and the empty tile.
- Counts as one move.
- Pushes one undo snapshot.
- Emits `onLineMove(dir, steps)` so the UI can animate all tiles together.

---

## Solver Notes

| Solver | Best For | Behavior |
| :--- | :--- | :--- |
| BFS | 3x3 | Finds shortest paths; boards up to 4x4 use compact packed states, but the search space still grows quickly. |
| A* | Small to medium boards | Uses Manhattan distance to prioritize promising states. |
| IDA* | Lower-memory experiments | Uses iterative deepening with Manhattan distance and linear conflict. |

Sliding puzzles become expensive very quickly. For a production mobile game, solvers are best treated as assistive or debug features rather than required gameplay.

---

## Testing

### Verified

- Automated JUnit 5 tests for shared core move, undo, save, records, and solver sanity behavior.
- Root Gradle test flow using the Android Gradle wrapper from the repository root.
- Desktop Java compilation with `javac`.
- `run.bat` desktop compile flow.
- Swing GUI smoke testing.
- Mouse click movement.
- Non-adjacent row/column sliding with synchronized animation.
- Undo after a whole-line slide.
- Desktop Home, Records, Preferences, Results, Help, and Assist parity text covered by focused JUnit tests.
- Save metadata round-trip coverage for updated time, size, moves, elapsed time, and active/solved state.
- Android debug build with Gradle.
- Signed Android release APK/AAB generation through `android\build-release.bat`.
- Desktop ZIP package and optional `jpackage` app-image generation through `package-desktop.bat`.
- Release notes/version matching through `version.properties` and `release-notes/<version>.md`.
- Android lint completes without fatal issues; the 2026-08-09 report contains
  21 pre-existing warnings that remain outside the solver performance scope.
- Android instrumentation tests for onboarding, Home launch, Practice Tutorial,
  Mode Select guidance and accessibility text, Continue metadata, How to Play,
  Settings, persistent English, Traditional Chinese, and Japanese language switching, Results,
  Assist hints, whole-line movement, undo/redo, move history, save/load
  persistence, app-state store behavior, Activity state/navigation helpers,
  rotation, screen-transition behavior, Reduced motion, and solver-assisted
  record protection, local completion history, lifetime statistics, and record
  reset behavior, versioned personal-data backup round trips, malformed backup
  rejection, and confirmed restore with Activity recreation.
- Connected Android instrumentation helpers now wait for the foreground app
  window, wait for activity controls to become interactable, use device-level
  board taps, and fall back to direct swipe scrolling for long content.
- The latest 2026-08-24 dual-AVD Android acceptance covers all 108 tests in one
  serial run on each profile: Pixel_7 (Android 15, 1080x2400) passed 108/108 in
  7m59s and `small_phone` (Android 16 / API 36.1, 720x1280) passed 108/108 in
  8m23s, with no failed or skipped tests. The suite
  includes English-default isolation from device locale, persistent language
  switching, active-game preservation, and explicit Traditional Chinese and
  Japanese major-screen, difficulty-selection, independent per-size saves,
  legacy-save migration, save/record scoping, solver-warning, exact-puzzle
  replay across Results rotation, Records, active-play timer-pause flows,
  player/assisted completion history, lifetime statistics, bounded retention,
  localized averages, full record reset, persistent sound/theme settings, and
  active-game preservation across theme recreation. The five backup tests cover
  complete data round trips, replacement of missing preferences with defaults,
  unsupported-version rejection without mutation, visible Settings actions,
  and confirmed restore followed by Activity recreation. Daily-calendar tests
  cover deterministic month boundaries, independent dated saves, legacy daily
  save migration, historical streak protection, month restoration across
  rotation, and opening the exact historical puzzle. Favorite tests cover
  stable exact-board identity, labels and duplicate renaming, 50-entry
  retention, malformed-row recovery, backup inclusion, isolated practice
  saves, reset/delete boundaries, Activity state, and a complete save/replay/
  practice-only Results flow. Trend/goal tests cover scope filtering,
  assisted-result exclusion, equal recent/previous windows, weekly boundaries,
  validated 1–50 targets, compact-screen scrolling, and goal/scope persistence.
  Continuous Challenge tests cover all supported board sizes, session and
  backup persistence, save isolation, per-puzzle record boundaries, compact
  start/resume/next/complete/end flows, and rotation-safe continuation. Move
  History/Redo tests cover adjacent and whole-line action identity, undo/redo
  ordering, new-action and Restart clearing, save/load and backup persistence,
  rotation, compact portrait controls, and landscape board space. Adaptive and
  accessibility tests cover large-text stacking, wide-window bounds, 48dp action
  targets, headings, explicit traversal, theme contrast, and playable virtual
  board cells. Separate 1.5x font runs passed on both profiles, and a 1600x2560
  wide-window run passed on Pixel_7.
- Latest local `ci.bat` run passed the no-device verification and release
  readiness gates.
- Android emulator smoke testing for install/launch, Home visibility, whole-line
  movement, undo/redo, Move History, restart, save/load, solver warning dialog,
  portrait/landscape layout, rotation, and background resume.

### Useful Commands

One-command local verification:

```bat
verify.bat
```

This compiles Android instrumentation test APKs as part of the no-device local
verification path.

Local CI gate:

```bat
ci.bat
```

This runs `verify.bat` and `verify-release.bat`. GitHub Actions uses the same
gate in `.github/workflows/ci.yml`; release APK/AAB artifacts produced there are
signed with the temporary verification key unless real signing secrets are
provided and must not be uploaded to stores.

Desktop compile:

```bat
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
```

Shared core tests:

```bat
android\gradlew.bat -p . test
```

Shared solver performance benchmark:

```bat
benchmark.bat
```

Use `benchmark.bat BFS` to measure only the player-visible BFS bottleneck. The
benchmark runs a fixed 31-move 3x3 position, validates the returned optimal
solution, and reports median latency and current-thread allocated bytes. The
reported timings are machine-specific diagnostics, not pass/fail thresholds.

Android build and lint:

```bat
cd android
build-debug.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Android instrumentation tests on a connected emulator or device:

```bat
verify-connected.bat
```

Release artifact and Play Store readiness verification:

```bat
verify-release.bat
```

This builds signed Android APK/AAB artifacts, the desktop package, exports store
listing assets to `dist/store-assets/`, writes SHA-256 release manifests under
`dist/release-manifests/`, and runs the Android Play Store plus desktop public
beta readiness file checks.

Repeatable Android screenshot smoke capture and PNG validation:

```bat
android\screenshot-smoke.bat
```

### Current Limitations

- Desktop UI tests are still manual/smoke-level.
- Connected Android instrumentation tests still require a running emulator or device.
- Pixel_7 and 720x1280 small-phone layouts have connected and visual acceptance
  evidence; a physical device, tablet/foldable, and lower-refresh profile still
  need the pre-launch visual pass.
- Screenshot smoke validates captured PNG readability and dimensions, but is
  not a pixel-diff gate.
- `MainActivity` has been reduced by extracting state/navigation/UI helpers and
  the major Android screen builders. Onboarding and How to Play remain
  in-activity learning flows unless they grow enough to justify another split.
- Solver performance is intentionally limited by timeouts.
- The desktop package is a ZIP plus optional app-image, not a signed installer.

---

## Development Notes

Development planning, Git workflow rules, desktop/Android behavior parity, and the current roadmap are maintained in [DEVELOPMENT.md](DEVELOPMENT.md).

### Java Runtime Notes

If Android Gradle fails because of a Java compatibility issue, use Android Studio's bundled runtime:

```bat
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
cd android
gradlew.bat :app:assembleDebug
```

### Code Documentation

Public core, desktop, and Android APIs use English Javadoc/API comments so the shared model and platform glue can be reused safely by future desktop, Android, or test code. The most important contracts are:

- `GameModel.move(Direction)`
- `GameModel.slideLineTo(int row, int col)`
- `GameObserver.onMove(Direction)`
- `GameObserver.onLineMove(Direction, int)`
- `Solver.solve(GameModel)`
- `SaveManager.saveGame(GameModel)`
- `SaveManager.loadGame()`
- `SaveManager.getDataDirectory()`
- `KlotskiView.enqueueMoves(List<Direction>)`
- `MainActivity.onGameWon(int, long)`

---

## Roadmap

- The Personal Play roadmap completed eight independently tested
  and committed stages: active-play timer pausing, difficulty selection,
  replaying the same puzzle, per-size saves, local history/statistics, an
  offline daily challenge, strategic hints, and sound/themes. All eight stages
  are implemented and verified.
  `DEVELOPMENT.md` owns the detailed acceptance criteria and status.
- Personal Play 2.0 is active. Stage 1 offline backup/restore, Stage 2 daily
  calendar/history replay, Stage 3 Favorite Puzzles, Stage 4 offline personal
  trends/custom weekly goals, Stage 5 Continuous Challenge, and Stage 6 Move
  History/Redo, and Stage 7 adaptive accessibility completed their two-AVD
  acceptance. Toolchain and CI maintenance remains the final future stage until
  its own verification and commit gate passes.
- Desktop/mobile player-facing parity MVP is complete for Home/start, Records, Preferences, Results, How to Play, Practice Tutorial, and Assist hints.
- Save files now include release-readiness metadata and desktop saves now live in the user-data directory.
- Signed Android release APK/AAB and desktop ZIP/app-image packaging scripts are available.
- Desktop public beta readiness notes and local package checks are tracked in
  `DESKTOP_BETA_READINESS.md` and `check-desktop-beta-readiness.bat`.
- Android Play Store readiness drafts, adaptive launcher icons, feature graphic
  source/export workflow, Data Safety notes, privacy policy draft, screenshot
  review worksheet, accessibility review worksheet, and pre-launch matrix are tracked in
  `android/PLAY_STORE_READINESS.md`.
- First public Android beta is intentionally local-only: no analytics, crash
  reporting, telemetry, ads SDKs, accounts, cloud save, or third-party
  tracking.
- The desktop/Android feature parity matrix is maintained in
  `DEVELOPMENT.md` under the Desktop/Mobile Parity Pass section.
- Public beta handoff reviews, store screenshots, the privacy-policy URL,
  download channels, and extracted desktop-package acceptance are deferred
  until the project owner chooses public distribution.
- Broaden TalkBack, larger-font, contrast, physical-device, and tablet/foldable
  acceptance before store submission.
- Split larger UI/controller code only where it supports a concrete feature or
  verification need.
- Run a broader manual TalkBack service review before any future public release;
  automated virtual-node play, touch targets, color contrast, headings, focus
  order, large text, and reduced-motion paths are already covered locally.
- Replace temporary release signing with a real Play upload key when store
  submission begins; it is intentionally deferred from the current push-ready
  milestone.
- Publish and review the privacy policy URL before store distribution.
- Complete the desktop beta manual smoke and accessibility reviews from the extracted ZIP before opening public testing.
- Consider moving desktop launch/package tasks fully into Gradle.

---

## Contributing

Contributions are welcome. Good areas to improve:

- Desktop and Android accessibility.
- Automated tests.
- Solver heuristics and performance.
- Difficulty calibration.
- Cross-device UI polish and animation tuning.
- Release packaging.

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## Acknowledgements

- Java Swing for the desktop prototype.
- Android SDK for the native mobile implementation.
- Gradle for repeatable Android builds.
- Classic sliding puzzle and number Klotski games for the timeless puzzle design.
