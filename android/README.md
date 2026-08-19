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
- Mode Select for 3x3, 4x4, and 5x5 games, followed by Relaxed, Classic, or
  Challenge shuffle-depth selection
- Difficulty-aware Continue metadata, compact game/results titles, and local
  best records scoped by size and difficulty; legacy data defaults to Classic
- Results Replay Puzzle restores the exact initial board, difficulty, and size
  while resetting moves and active-play time without reshuffling
- Short screen exit transitions and staggered content entrances across Home,
  onboarding, Practice Tutorial, Mode Select, How to Play, Settings, Records,
  Results, and Game
- Flat rounded surfaces, press ripples, grouped Home actions, clearer Settings
  data boundaries, and a structured Results summary
- Visual How to Play, Beginner Guide, Quick Reminder, and Records screens
- Tap a tile in the same row/column as the blank space to slide one or more tiles
- Swipe a movable tile toward the blank space
- Whole-line slides animate all affected tiles together and count as one move
- Compact in-game controls with Undo, Restart, Menu, and Assist actions
- Icon-plus-text Home, game, and Results actions, with compact game navigation
  kept on one line at 720x1280
- Outlined empty-cell affordance and a first-move prompt before the player moves
- Lightweight Assist hint that highlights movable aligned tiles without moving
  the board; BFS, A*, and IDA* are grouped one level deeper under Solver Tools
- Board, highlighted movable tiles, primary game controls, and settings switches
  expose accessibility descriptions for screen readers
- Manual Save and Load controls in the in-game menu
- Auto-save through `SharedPreferences`
- Active-play timing pauses for game dialogs, navigation outside Game, and app
  background time, then resumes only when the Game screen is interactive
- App-state persistence is isolated in `AndroidGameStore` for saves, settings,
  records, onboarding, app language, and last selected size
- Activity state restoration, back-navigation decisions, shared UI primitives,
  and learning-content builders are split out of `MainActivity` for maintainable
  Android iteration
- `AndroidMotion` owns presentation-only screen timing and interaction locking;
  navigation targets and state remain in `MainActivity`
- Home screen construction, including Continue metadata presentation, is split
  into `AndroidHomeScreen`
- Game and Practice Tutorial screen construction is split into
  `AndroidGameScreen` and `AndroidTutorialScreen`; `MainActivity` still owns
  model state, command gates, and navigation callbacks
- Mode Select, Records, Settings, and Results construction is split into
  package-private builders while `MainActivity` owns navigation, persistence,
  settings application, and record-result text
- Independent 3x3, 4x4, and 5x5 save slots include updated time, puzzle size,
  difficulty, move count, elapsed time, and active/solved state; Home opens a
  compact chooser when more than one slot exists
- Legacy single-save data migrates once into its matching size slot without
  replacing a newer slot
- Release versioning is shared through the repository-root `version.properties`
  file
- Signed release APK/AAB builds are available through `build-release.bat`
- Rotation restore for the active game screen
- Settings for persistent English, Traditional Chinese, and Japanese language selection,
  haptic feedback, reduced board/screen motion, reset all saved games, and
  reset records
- Per-size best record tracking
- Results screen with a completion-mark settle, Replay Puzzle, New Size, Home, and
  solver-assisted wording; Reduced motion skips the settle animation
- BFS, A*, and IDA* solver controls in advanced Solver Tools with warnings for
  expensive board sizes
- Solver-assisted completions do not overwrite player best records
- Android instrumentation coverage for the main navigation, Mode Select
  guidance, Continue metadata, Activity state/navigation helpers, and gameplay
  flows
- Connected test helpers wait for the foreground app window and include a swipe
  fallback for long help screens to reduce slow-emulator false failures

## Language Selection

English is the default app language regardless of device language. Players can
open Settings and choose **App language** to switch among English (`en`),
Traditional Chinese (`zh-TW`), and Japanese (`ja-JP`). The selection is stored
by `AndroidGameStore`, applied before `MainActivity` creates UI resources, and
survives activity recreation and app relaunch.

`AndroidAppLocale` is the language registry and context wrapper. To add another
language such as Korean:

1. Add one `LanguageOption` entry and its display-name resource.
2. Add a complete localized resource directory such as `values-ko`.
3. Keep every string/plural key and format placeholder compatible with the base
   English resources.
4. Run the full multilingual regression matrix.

## Build Notes

This repository includes a Gradle wrapper. Android Studio can open and sync the
project using the installed Android SDK at:

`%LOCALAPPDATA%\Android\Sdk`

If the Google Android CLI is installed, a second small-phone AVD can be created
and started without Android Studio:

```bat
android-cli.exe emulator create small_phone
android-cli.exe emulator start --cold small_phone
android-cli.exe emulator list --long
```

The accepted local `small_phone` profile uses Android 16 / API 36.1 at 720x1280
and 320 dpi. Use an explicit device serial for ADB or Gradle when more than one
emulator is connected.

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

The detailed English, Traditional Chinese, and Japanese acceptance matrix is maintained in
[`REGRESSION_TEST_CHECKLIST.md`](REGRESSION_TEST_CHECKLIST.md). Run it for every
locale or navigation/persistence change.

To run the same no-device gate used by GitHub Actions plus release readiness:

```bat
..\ci.bat
```

This runs `verify.bat` and `verify-release.bat`. CI release artifacts are
verification outputs and use the temporary signing key unless real Play upload
signing is explicitly configured.

Latest 2026-08-20 validation status: all 58 Android tests passed in five isolated
instrumentation batches on both the Pixel_7 AVD running Android 15 at 1080x2400
and the `small_phone` AVD running Android 16 / API 36.1 at 720x1280. The suite covers the
normal animated transition path, the Reduced motion bypass, English-default
locale isolation, persistent English, Traditional Chinese, and Japanese switching,
explicit Traditional Chinese and Japanese major-screen/dialog/result flows,
difficulty selection and persistence, independent per-size saves, legacy-save
migration, scoped best records, and active-play timer pausing across game menus,
nested dialogs, Assist, Settings, and background/resume. Exact-puzzle replay is
checked before and after Results rotation, including board identity and zeroed
run state.
CLI-captured manual review covered English onboarding, Traditional Chinese
Home/Settings/Mode Select/Game, and Japanese Home/Settings/Mode Select/How to
Play/Game on the compact AVD. The compact controls remained single-line after
shortening the Traditional Chinese Restart label to `重來` and the Japanese Home,
Restart, and Assist labels to `チュートリアル`, `再挑戦`, and `ヒント`. The final
Pixel_7 app process emitted no `AndroidRuntime` error.
The Stage 1 small-phone manual pass also kept the complete game menu visible and
confirmed that a five-second menu stay did not increase the play timer.
The Stage 2 small-phone manual pass confirmed that all three difficulty choices
are visible and tappable and that `4x4 · Challenge` stays on one line beside
Home and Menu. The Stage 3 final APK SHA-256 is
`5722E65AF47727B2E270E564057D0340D3C376AAB2E7A27888929173A407BEB9`.
The Stage 4 small-phone manual pass confirmed the two-save Home summary and
chooser fit at 720x1280 and display size, difficulty, state, moves, and time.
The Stage 4 final APK SHA-256 is
`56C5B37A1C6E17F9E3EF57B3985E53627FF1D7EE528467D18CC4694CC4587518`.

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
checklist, feature graphic source, screenshot review worksheet, accessibility
review worksheet, and pre-launch matrix are tracked in
[`PLAY_STORE_READINESS.md`](PLAY_STORE_READINESS.md).
The first public Android beta is intentionally local-only: no analytics, crash
reporting, telemetry, ads SDKs, accounts, cloud save, or third-party tracking.
Store asset sources live under [`store-assets`](store-assets/); export and
review final upload files before Play Console submission.

To export the feature graphic upload PNG:

```bat
export-store-assets.bat
```

To check the repo-side Play Store readiness files after building release
artifacts and the release manifest:

```bat
check-play-store-readiness.bat
```

The root `verify-release.bat` command exports store assets, writes
`../dist/release-manifests/<version>.txt`, and runs this check after the Android
release and desktop package steps. It also runs the desktop public beta
readiness check from the repository root.

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
Game, How to Play, Settings, Records, and Results/current screen. It then runs
`check-screenshot-set.bat` to verify that every expected PNG is readable and at
least 320x320, and writes `manifest.txt` beside the screenshots. The manifest
includes screenshot purpose labels and a manual review checklist for Play
Console handoff.

To verify an existing screenshot set without recapturing:

```bat
check-screenshot-set.bat ..\screenshots\android\0.2.0-beta.1
```

## Emulator Smoke Test Checklist

- Install `app/build/outputs/apk/debug/app-debug.apk`.
- Launch `com.klotski.android/.MainActivity`.
- If the app is not visible in the launcher, run `adb install -r app/build/outputs/apk/debug/app-debug.apk` and `adb shell am start -n com.klotski.android/.MainActivity`.
- Clear app data when checking first-run behavior, then confirm onboarding appears before normal play.
- Use Skip, Practice Tutorial, and Start 3x3 in onboarding; returning launches should go to Home.
- Confirm the app opens on Home, not directly on the board.
- Open Beginner Guide, Practice Tutorial, and How to Play from Home.
- Confirm screen changes fade the outgoing screen and reveal content in reading
  order without accepting a second navigation action during the exit.
- In Practice Tutorial, tap the emphasized 6 for the first move, then tap the
  emphasized 5 to demonstrate a whole-line slide counted as one move.
- Open New Game, then start 3x3, 4x4, and 5x5 from Mode Select. For each size,
  choose Relaxed, Classic, and Challenge and confirm the selected label appears
  in the compact game title.
- Return Home and confirm Continue appears after a game has been saved.
- Tap adjacent and non-adjacent aligned tiles; a whole-line slide should count as one move.
- Undo after a whole-line slide; the entire gesture should restore in one step.
- Restart; moves and timer should reset without reshuffling.
- Save after a move from Menu, restart, then Load; board, move count, timer, and restart grid should be restored.
- Confirm saved-game metadata updates with the saved size, difficulty, move
  count, elapsed time, and active/solved state.
- Open Menu, check Quick Reminder, and open Settings.
- Leave Menu, Quick Reminder, Assist, and Solver Tools open briefly; returning
  to Game must not add those intervals to the play timer.
- In Settings, switch to Traditional Chinese and Japanese in turn. After each
  switch, return to the active game and confirm the board, move count, timer,
  save, and navigation state remain intact.
- Relaunch the app after each selection and confirm the selected language
  persists; switch back to English and repeat the same major flow.
- Toggle haptics and Reduced motion, then return to gameplay. Reduced motion
  should skip both screen transitions and board movement animation.
- Use Settings reset actions only after confirming the dialogs.
- Open Assist, choose Show Movable Tiles, and confirm the status explains the
  highlighted legal moves while the move count stays unchanged.
- Confirm the empty cell has a visible outline/dot and the zero-move status
  explains the first aligned-tile interaction.
- With a screen reader or inspection tool, confirm the board describes its size,
  empty-cell position, row-by-row tile state, and highlighted movable-tile count.
- Confirm Undo, Restart, Assist, Menu, and Settings switches expose descriptive
  accessibility labels.
- Open Assist, confirm solver names are not shown at the first level, then open
  Solver Tools and confirm the record-safety explanation appears before BFS,
  A*, and IDA*.
- Choose an expensive solver such as BFS on 4x4 and confirm the warning dialog appears.
- Finish a game and confirm Results shows the completion mark, moves, time,
  record status, and Replay Puzzle/New Size/Home actions. Replay Puzzle must
  restore the identical starting board with zero moves and zero elapsed time.
- Rotate Results, then use Replay Puzzle and confirm the same starting board is
  still restored.
- Confirm solver-assisted completion reaches Results without updating player best records.
- Background and return to the app; autosave should preserve the current board
  and the away interval must not increase elapsed play time.
- Rotate and return to portrait; the current game state should remain intact.

`build-debug.bat` uses Android Studio's bundled JBR when available, which avoids
Gradle compatibility issues with newer system Java versions.
