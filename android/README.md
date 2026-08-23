# SlideDo Android

This Android project reuses the desktop game's core Java logic from:

`../src/com/klotski/core`

Open this `android` folder in Android Studio, then run the `app` configuration on
an emulator or connected Android device.

## Current Features

- Native Android `Activity` and custom game board `View`
- Home screen on launch instead of opening directly into the board
- Localized Daily Calendar for today and earlier deterministic puzzles, with
  completed/in-progress/missed status, future-date blocking, historical replay,
  and an independently resumable save for every date
- Local Favorite Puzzles library for up to 50 owner-named exact starting
  boards, with Replay, Rename, Remove, backup inclusion, and isolated practice
  progress that never replaces normal/daily saves or personal records
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
- Deterministic Strategic Hint that highlights one recommended adjacent tile
  without moving the board, plus a non-assisted movable-tile hint; BFS, A*, and
  IDA* are grouped one level deeper under Solver Tools
- Optional short local tones for tile moves and puzzle completion, disabled by
  default, plus persistent Midnight and Ocean visual themes
- Versioned offline JSON export/import for all Android saves, records,
  statistics, daily state, onboarding, language, and preferences through the
  Android system file picker
- Board, highlighted movable tiles, primary game controls, and settings switches
  expose accessibility descriptions for screen readers
- Manual Save and Load controls in the in-game menu
- Auto-save through `SharedPreferences`
- Active-play timing pauses for game dialogs, navigation outside Game, and app
  background time, then resumes only when the Game screen is interactive
- App-state persistence is isolated in `AndroidGameStore` for saves, settings,
  best records, completion history, personal statistics, onboarding, app
  language, and last selected size/difficulty
- `AndroidPersonalDataArchive` validates the complete versioned backup before
  `AndroidGameStore` replaces any preferences; malformed or unsupported files
  are rejected without partial import
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
- Mode Select, Favorite Puzzles, Records, Settings, and Results construction is split into
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
- Settings for persistent English, Traditional Chinese, and Japanese language
  selection, Midnight/Ocean visual themes, optional sound and haptic feedback,
  reduced board/screen motion, offline backup/restore, reset all saved games,
  and reset records
- Per-size and per-difficulty best records plus local lifetime completion
  totals, player averages, and newest-first recent history; assisted
  completions are counted separately and cannot replace player bests
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

Latest 2026-08-23 validation status: all 91 Android tests passed in one serial
run on each emulator profile: Pixel_7 passed 91/91 on Android 15 at 1080x2400
in 8m52s and `small_phone` passed 91/91 on Android 16 / API 36.1 at 720x1280
in 9m55s. Neither run reported a failed or skipped test. The suite covers the
normal animated transition path, the Reduced motion bypass, English-default
locale isolation, persistent English, Traditional Chinese, and Japanese switching,
explicit Traditional Chinese and Japanese major-screen/dialog/result flows,
difficulty selection and persistence, independent per-size saves, legacy-save
migration, scoped best records, and active-play timer pausing across game menus,
nested dialogs, Assist, Settings, and background/resume. Exact-puzzle replay is
checked before and after Results rotation, including board identity and zeroed
run state. The suite also verifies bounded completion history, lifetime
statistics, player/assisted separation, localized Records summaries, full
record reset, and duplicate prevention during Results recreation.
Stage 8 adds a sound preference that defaults off, asset-free move/completion
tones, persistent Midnight/Ocean palettes, and active-game preservation when a
theme recreates the Activity. Automated checks cover defaults, persistence,
settings accessibility text, enabled-sound gameplay, and unchanged board state.
Stage 7 adds deterministic strategic guidance, persisted assisted state for
normal and daily saves, and player-best protection across rotation, Restart,
Load, and Results replay. Stage 6 adds a deterministic offline 4x4 Classic
puzzle for each device-local date, a daily save isolated from the three normal
size slots, idempotent completion tracking, and current/best streaks. Reset
Saved Games clears the daily board but preserves streak records; Reset Records
clears daily completion and streak state but preserves the daily board.
Personal Play 2.0 Stage 1 adds versioned owner-controlled JSON backup and
restore without a network dependency or storage permission. Five automated
tests cover full persistence round trip, removal of preferences absent from the
archive, invalid-version no-mutation behavior, visible Settings actions, and a
confirmed restore followed by Activity recreation. Manual review verified both
system file pickers, the replacement dialog, malformed-file error handling, and
the complete compact Settings data section. The Stage 1 debug APK SHA-256 is
`40FD389B702534A9C5578B4DFDC67D45730356ABFD9E0C8D9BA017FC4ADADE3F`.
Personal Play 2.0 Stage 2 adds the month calendar and per-date daily save slots.
The calendar retains completed dates, distinguishes active and missed dates,
disables future dates, preserves its selected month through rotation, and opens
the deterministic puzzle for the chosen date. Completing an older date records
that completion without replacing the current/latest streak boundary. Valid
legacy single-date daily saves migrate automatically.
The Stage 2 final debug APK SHA-256 is
`F10B1C056903576C5942F63DBD66AFAC7CA88857979E459F486DADD68706E1AB`.
Personal Play 2.0 Stage 3 adds Favorite Puzzles to Home, Game Menu, and Results.
`PuzzleIdentity` includes size, difficulty, and every starting-grid value, so
the same exact puzzle renames one existing favorite instead of duplicating it.
The newest 50 named entries are kept locally and included in backup/restore.
Favorite replay persists its own current run for rotation and background
continuity, while normal/daily saves, best records, completion history,
statistics, and streaks remain unchanged. Reset Saved Games clears favorite
practice progress but keeps the named library; deleting a favorite removes
only that entry and its practice progress. The Stage 3 final debug APK SHA-256
is `D35B3CBA33E1C8A13B1BC35E165D7852C21EA0F5E5F466C8E3BCC3734FD04F83`.
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
The Stage 5 small-phone manual pass reached overall totals, recent completions,
all nine size/difficulty breakdown panels, and Back without clipping or overlap.
The newest 50 entries remain in local storage, Records displays the newest 10,
and lifetime statistics continue beyond the history limit. The Stage 5 final
APK SHA-256 is
`C8D78B4CE1E295D20DA80D366BF2940106D9F1C0B9849918519865F435AB2180`.
The Stage 6 compact-phone pass confirmed that Daily Challenge, Play, Beginner
Guide, Practice Tutorial, How to Play, Settings, and Records remain reachable
without clipping. Pixel_7 and `small_phone` generated the same dated board, and
the Daily game title, HUD, board, and controls fit both profiles.
The Stage 6 final debug APK SHA-256 is
`EF92467A66D3F453FF95DE00122C68B5B092A63CADAE59B10ED73EBCCB050499`.
Stage 8 manual review covered both Settings palettes and the Ocean game board at
1080x2400, plus the complete scrollable Midnight Settings flow at 720x1280. All
theme, sound, Reduced motion, local-data, and Back controls remained readable
and reachable without overlap. The Stage 8 final debug APK SHA-256 is
`3B64AC1323E5B2B6195829633730EF4731C3283FBB4C922672DDD3D719573ABA`.

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
- Confirm Sound feedback starts off, enable it, and verify a short tone for a
  player tile move and puzzle completion. Solver playback should not emit a tone
  for every automated step.
- Switch Visual theme between Midnight and Ocean. Confirm Home, Settings, and
  the board update, then relaunch and verify the selected theme, active board,
  move count, timer, saves, and records are unchanged.
- In Settings, export a backup through the system file picker. Change at least
  one setting or save, import the backup, review the replacement warning, and
  confirm the exported state returns after Restore.
- Cancel an import from both the system picker and the replacement dialog;
  confirm the current saves and settings remain unchanged.
- Select a malformed or unsupported JSON document and confirm SlideDo rejects
  it with a localized error without replacing existing preferences.
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
- Open Assist and choose Strategic Hint. Confirm exactly one adjacent tile is
  highlighted, the board and move count stay unchanged, and the assisted-run
  warning remains after rotation and Restart. Complete or replay the puzzle and
  confirm the run cannot replace a player best.
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
- From Home, open Daily Calendar on both emulator profiles, select today, and
  confirm the date, 4x4 Classic board, and starting layout match.
- Make a daily move, return Home, and select today again; confirm the dated
  board resumes without replacing the normal 4x4 save.
- Navigate to an earlier month, rotate, and confirm the month remains selected.
  Open an earlier date, then return and verify that today's dated save is still
  intact. Future dates and Next on the current month must remain unavailable.
- Complete the daily puzzle and confirm Results shows the daily completion and
  streak. Replay it and confirm the streak does not increment twice for the same
  date.

`build-debug.bat` uses Android Studio's bundled JBR when available, which avoids
Gradle compatibility issues with newer system Java versions.
