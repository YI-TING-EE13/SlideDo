# SlideDo Development Guide

This is the single source of truth for SlideDo development planning, feature behavior, product direction, and local workflow notes.

## Git Workflow

- This project uses local Git history to track development work from 2026-05-25 onward.
- Do not push to a remote repository until the project owner explicitly asks for it.
- Commit cohesive changes with clear messages after implementation and verification.
- Keep generated files, local runtime saves, IDE files, and machine-specific config out of Git.
- Keep repository line endings stable through `.gitattributes`.
- Do not rewrite history, squash, rebase, reset, or force-push unless the project owner explicitly requests it.
- Before release or remote publication, verify `.gitignore` and ensure local files such as `klotski_save.json`, `klotski_save.dat`, `klotski_records.json`, `bin/`, `build/`, and Android build output are not tracked.

## Next Session Bootstrap

Use this section as the first stop in a fresh Codex conversation.

Current handoff status:

- The Git `HEAD` plus the current working tree are authoritative. Use `git log`
  and `git status` before relying on an older hash in this development record.
- `DEVELOPMENT.md` is now the primary continuity artifact; older planning notes
  were consolidated here.
- The Android app has completed the MVP pass for Home, Mode Select, onboarding,
  interactive Practice Tutorial, visual How to Play, Settings, Results, local
  records, save/load, strategic and lightweight Assist hints, board/control accessibility
  descriptions, `AndroidGameStore` persistence separation, and connected
  instrumentation coverage.
- The Android UI polish pass now includes icon-plus-text player actions, an
  outlined empty cell with first-move guidance, nested advanced Solver Tools,
  and a short completion-mark settle animation with a Reduced motion bypass.
- Android now defaults to English independently of device language and offers
  persistent English, Traditional Chinese, and Japanese selection in Settings. The
  locale registry is isolated in `AndroidAppLocale` so another locale requires
  a registry entry and a complete translated resource directory, not navigation
  or gameplay changes.
- The latest local verification pass was warning-clean for the previously noisy
  Gradle DSL deprecation, Java native-access warning, and Android Java
  deprecation note.
- Personal Play 2.0 Stages 1 through 4 are complete. Android Settings owns
  versioned local backup/restore, while the Daily Calendar browses today and
  earlier deterministic puzzles with per-date saves and completion markers.
  Favorite Puzzles stores up to 50 owner-labeled exact starting boards and
  replays them as isolated practice without changing normal saves or records.
  Trends & Weekly Goal compares only player solves from one matching size and
  difficulty and keeps a configurable 1–50 solve target entirely offline.
- The latest dual-AVD acceptance covers all 94 Android tests in one serial run
  on each profile: Pixel_7 (Android 15, 1080x2400) and `small_phone` (Android
  16 / API 36.1, 720x1280) completed in 603.426s and 629.539s respectively, with no
  failed or skipped tests. Coverage
  includes persistent sound/theme preferences, active-game preservation across
  theme recreation, strategic-hint assistance persistence and player-best protection,
  the deterministic offline daily challenge and streak state,
  exact-puzzle replay before and after Results recreation, difficulty
  selection, independent per-size saves, legacy-save migration, scoped records,
  active-play timing, bounded completion history, lifetime statistics, and
  player/solver-assisted separation.

Start a new implementation session by checking:

```bat
git status --short
git log --oneline --decorate -5
```

Then run the smallest relevant verification for the planned change. For a broad
Android or documentation-sensitive change, use:

```bat
verify.bat
verify-connected.bat
```

The original eight-stage Personal Play roadmap in `Roadmap And Planning` is
implemented and verified. Personal Play 2.0 is now the active staged program;
Stages 1 through 4 are complete and Stage 5 is the next implementation gate.

Real Play upload signing is intentionally deferred until store submission. It
is not required for a local push-ready commit, and no Git remote or push is part
of the current handoff.

## Current Project State

SlideDo is a Java number Klotski / sliding puzzle game with:

- Shared Java game core in `src/com/klotski/core`.
- Desktop Swing UI in `src/com/klotski/ui`.
- Native Android UI in `android/app/src/main/java/com/klotski/android`.
- Root Gradle/JUnit 5 tests for shared core behavior.
- Android Gradle wrapper and debug APK build flow.
- English API comments/Javadocs for public core, desktop, and Android APIs.
- One-command local verification through `verify.bat`.

Desktop currently supports:

- 3x3, 4x4, and 5x5 games.
- Mouse click movement and swipe-like mouse release movement.
- Whole-row / whole-column slides when selecting a tile aligned with the empty cell.
- Synchronized whole-line animation.
- Keyboard arrow movement.
- Undo and restart.
- Save/load.
- Best records.
- Solver playback with BFS, A*, and IDA*.

Android currently supports:

- Home screen launch instead of opening directly into the board.
- One deterministic offline 4x4 Classic daily challenge per device-local date,
  with a localized month calendar, independent resumable saves by date,
  historical replay, persistent completion markers, idempotent completion, and
  current/best streak state. Future dates cannot be opened.
- A local Favorite Puzzles library for up to 50 named exact starting boards.
  Replays use isolated practice progress, survive Activity recreation, and do
  not replace normal/daily saves or update personal records and statistics.
- First-run onboarding before normal play, with Skip and Start 3x3 actions.
- Interactive Practice Tutorial entry from Home and onboarding, using a guided
  first move plus a whole-line slide lesson.
- Continue when a valid save exists.
- Mode Select for 3x3, 4x4, and 5x5 games.
- Relaxed, Classic, and Challenge scramble-depth choices after size selection;
  equal size, difficulty, and seed inputs reproduce the same solvable board.
- Visual How to Play and Records screens before or during gameplay.
- Beginner Guide re-entry from Home.
- 3x3, 4x4, and 5x5 games.
- Tap and swipe movement.
- Whole-line slide behavior through `GameModel.slideLineTo(row, col)`.
- Synchronized whole-line animation.
- Highlighted tutorial hints for movable same-row / same-column tiles, with
  emphasized targets for the first move and whole-line slide practice.
- One move and one undo snapshot per user gesture.
- Compact in-game controls with Undo, Restart, Menu, and Assist.
- Icon-plus-text Home and game actions that remain single-line on the 720x1280
  phone profile.
- An outlined empty cell and a first-move prompt that explain where legal tiles
  can slide without changing puzzle rules.
- Assist offers a deterministic strategic next-move hint and a lightweight
  movable-tile hint; BFS, A*, and IDA* live in a second-level Solver Tools
  surface with record-safety guidance. Strategic and solver assistance cannot
  replace player best records.
- Board-level screen-reader summaries for game/tutorial board state, highlighted
  movable tiles, and primary game/settings controls.
- Manual Save/Load plus autosave through the in-game menu.
- Best records by puzzle size and difficulty, with legacy size-only records
  treated as Classic; player and solver-assisted completions also feed separate
  lifetime totals and a newest-first local history.
- BFS, A*, and IDA* solver controls behind Assist with expensive-operation warnings.
- Solver-assisted completion protection so player records are not overwritten.
- Settings for app language, haptic feedback, reduced motion, reset all saved
  games, reset records, and owner-controlled JSON backup/restore through the
  Android system file picker.
- Android app-state persistence through `AndroidGameStore` for independent
  3x3, 4x4, and 5x5 saves, difficulty, scoped records, bounded completion
  history, lifetime statistics, settings, app language, onboarding state, and
  the last selected puzzle size/difficulty.
- Stable cross-platform favorite identity through `PuzzleIdentity`, whose ID
  includes size, difficulty, and every starting-grid value.
- Versioned backup validation through `AndroidPersonalDataArchive`, including
  bounded input, type checks, duplicate-key rejection, and full replacement
  only after the selected document has passed validation.
- Results screen with a completion-mark settle animation, player-record status,
  solver-assisted completion wording, and a Reduced motion bypass.
- Haptic feedback.
- Active-play timing that pauses for game dialogs, non-game screens, and app
  background time, then resumes when the Game screen is interactive again.
- Warning-clean local Gradle verification under `--warning-mode all` for the
  root tests and Android assemble/lint flow.
- Release verification now checks Android Play Store readiness and desktop
  public beta readiness after generating release artifacts.

## Behavioral Reference

The shared `GameModel` is the canonical source for puzzle rules. Desktop remains the reference UI for gameplay semantics, while Android should preserve the same outcomes even when the mobile presentation differs.

Core rules:

- Numbered tiles use positive integers.
- The empty cell is represented by `0`.
- Solved order is row-major with the empty cell in the last position.
- `Direction` describes where the empty cell moves, not where a numbered tile visually moves.
- `GameModel.move(Direction)` counts as one move while a game is running.
- `GameModel.slideLineTo(row, col)` moves every tile between the selected tile and empty cell, counts as one move, pushes one undo snapshot, and emits `GameObserver.onLineMove(dir, steps)`.
- Scrambling is generated by valid moves from the solved board.
- `PuzzleDifficulty` maps Relaxed, Classic, and Challenge to increasing valid-move
  scramble budgets. Seeded generation is deterministic; difficulty does not
  promise a particular optimal solution length.
- Restart restores the initial post-scramble or loaded grid and clears undo history.
- Loading restores size, difficulty, current grid, initial grid, move count, and elapsed milliseconds.
- Best records compare lower move count first, then lower time.
- Solver-assisted wins must not overwrite player best records.

Android parity checklist:

- [x] Fresh launch shows Home instead of the board.
- [x] Continue is shown only when a valid save exists.
- [x] New Game opens Mode Select before starting a board.
- [x] Practice Tutorial is reachable from Home and onboarding.
- [x] Practice Tutorial teaches one first move and one whole-line slide without
  duplicating move rules outside `GameModel`.
- [x] Assist exposes a lightweight movable-tile hint that highlights legal same-row
  / same-column choices without moving the board.
- [x] How to Play is reachable before gameplay.
- [x] Default game is 4x4.
- [x] 3x3, 4x4, and 5x5 are available.
- [x] Generated boards are solvable.
- [x] Tap aligned adjacent tile moves one tile.
- [x] Tap aligned non-adjacent tile moves the full row or column.
- [x] Full row/column slide counts as one move.
- [x] Full row/column slide animates all affected tiles together.
- [x] Undo after whole-line slide restores the previous board in one step.
- [x] Restart restores the original generated puzzle and resets moves/timer.
- [x] Save/resume preserves current grid, restart grid, moves, and elapsed time.
- [x] Rotation keeps the current game screen and restored board state.
- [x] Difficulty selection is available after choosing a board size.
- [x] Records are separated by puzzle size and difficulty.
- [x] Solver controls run off the UI thread and warn for expensive board sizes.
- [x] Assist keeps the lightweight hint at the first level and moves complete
  solver playback into the advanced Solver Tools level.
- [x] The empty cell has a visible affordance and a zero-move prompt explains
  the first legal interaction.
- [x] Key Home and in-game actions use tested icon-plus-text controls.
- [x] Results provide a short completion-mark settle animation and Reduced
  motion skips it.

## Verified Commands

Shared core tests:

```bat
android\gradlew.bat -p . test
```

One-command local verification:

```bat
verify.bat
```

Desktop compile:

```bat
if exist bin rmdir /s /q bin
mkdir bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin @sources.txt
```

Desktop launch:

```bat
run.bat
```

Android build and lint:

```bat
cd android
build-debug.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

Android instrumentation tests:

```bat
verify-connected.bat
```

Warning-clean Android instrumentation check:

```bat
verify-connected.bat
```

Android install and launch:

```bat
adb devices
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.klotski.android/.MainActivity
```

Public core/desktop Javadocs:

```bat
"C:\Program Files\Java\jdk-25\bin\javadoc.exe" -quiet -public -Xdoclint:all -encoding UTF-8 -charset UTF-8 -d %TEMP%\slidedo-javadocs src\com\klotski\core\*.java src\com\klotski\ui\*.java
```

Android API comments:

```bat
"C:\Program Files\Java\jdk-25\bin\javadoc.exe" -quiet -public -Xdoclint:all -encoding UTF-8 -charset UTF-8 -classpath "%LOCALAPPDATA%\Android\Sdk\platforms\android-36\android.jar;src" -sourcepath "android\app\src\main\java;src" -d %TEMP%\slidedo-android-javadocs android\app\src\main\java\com\klotski\android\*.java
```

Shared solver performance benchmark:

```bat
benchmark.bat
```

Use `benchmark.bat BFS` for an isolated BFS run.

## Solver Performance Baseline

`benchmark.bat` measures shared solver latency without adding a benchmark
dependency to the app. The harness uses the canonical hardest 3x3 position,
whose optimal solution has 31 moves. Every iteration verifies the solution
length, replays the path to a solved board, and confirms that the solver did not
mutate its input model.

The 2026-08-09 comparison used Windows, Java HotSpot 25.0.1, a 768 MiB fixed
heap, three independent JVM processes, two warmup iterations per process, and
seven measured iterations per process. Each value below is the median of the
three per-process medians.

| BFS metric | Before | After | Change |
| :--- | ---: | ---: | ---: |
| Median latency | 198.480 ms | 15.121 ms | 92.4% lower; 13.13x speedup |
| Median current-thread allocation | 191.257 MiB | 13.875 MiB | 92.7% lower |
| Returned solution length | 31 moves | 31 moves | Unchanged and optimal |

The baseline identified BFS state representation as the dominant shared-solver
bottleneck. The previous implementation copied a two-dimensional grid for each
candidate and created string keys during visited-state checks. `BfsSolver` now
packs boards up to 4x4 into one `long`, stores visited boards in a primitive
open-addressed set, uses `ArrayDeque` for the frontier, and reuses the fixed
direction array. The array-based path remains available for 5x5 compatibility.

These figures measure the shared JVM solver path, which Android and desktop both
call from background work. A later Android functional pass on the Pixel_7 AVD
running Android 15 completed all 32 connected instrumentation tests and an
assisted 3x3 BFS solve. That pass verifies Android integration, not equivalent
Android runtime benchmark figures. Android frame timing, battery use, and
whole-app memory remain unmeasured. Do not turn the JVM figures or the observed
startup times into CI timing thresholds.

## Product Direction: Common Game App Experience

The next product direction is to make SlideDo feel more like a complete casual game app instead of opening directly into a developer-style gameplay board.

Target experience:

- Users should first see a friendly entry screen, not the board.
- New users should understand the objective and controls before playing.
- Returning users should be able to continue quickly.
- Game actions should be organized around common mobile patterns: Home, Mode Select, How to Play, Settings, Play, Pause, Results, and Stats.
- The current full-control game screen should become the in-game screen, not the app's first impression.

### Recommended App Flow

1. Home screen:
   - App title and short tagline.
   - Primary action: Continue if a save exists, otherwise Play.
   - Secondary actions: New Game, How to Play, Settings, Records.
   - Keep the screen visual and game-like; do not present dense text first.

2. Mode selection:
   - Cards or rows for 3x3, 4x4, and 5x5.
   - Show best record for each size.
   - Include difficulty labels such as Easy, Classic, and Expert.
   - Start a new game only after the user chooses a mode.

3. How to Play:
   - Explain the goal: arrange numbers in order with the empty cell last.
   - Show tap behavior: tap any tile in the same row or column as the empty cell.
   - Show swipe behavior: swipe a movable tile toward the empty cell.
   - Explain undo, restart, save/load, and best records.
   - Keep content short and visual; avoid long paragraphs.

4. In-game screen:
   - Prioritize the board and current progress.
   - Keep moves, timer, and best record visible.
   - Move secondary controls into a pause/menu panel if the screen becomes crowded.
   - Keep solver actions behind an Assist or Tools menu so casual players are not distracted.

5. Pause / menu overlay:
   - Resume.
   - Restart.
   - New game.
   - Save.
   - How to Play.
   - Settings.
   - Return Home.

6. Win/results screen:
   - Show moves, time, and whether a new best record was achieved.
   - Offer Replay Puzzle, New Size, and Home.
   - Keep solver-assisted completions visually distinct and do not record them as player bests.

### UX Engineering Direction

Current implementation approach:

- Keep one Android `Activity` for now with internal screen state for `HOME`,
  `ONBOARDING`, `TUTORIAL`, `MODE_SELECT`, `HOW_TO_PLAY`, `RECORDS`,
  `SETTINGS`, `RESULTS`, and `GAME`.
- Build separate private view-construction methods in `MainActivity` or small package-private screen classes before introducing a larger architecture.
- Keep `GameModel` unchanged for navigation work; the model should remain platform-independent.
- Keep `KlotskiView` focused on board rendering and gestures only.
- Store lightweight preferences and saves through `AndroidGameStore`, which
  remains backed by `SharedPreferences` until there is a concrete need for a
  database.
- Apply the stored Android language through `AndroidAppLocale` in
  `MainActivity.attachBaseContext` before any screen reads resources. English
  remains the explicit fallback instead of inheriting the device locale.

UX acceptance criteria completed in the 2026-05-25 Android navigation pass:

- Fresh launch shows Home, not the board.
- Continue is visible only when a saved game exists.
- New Game leads to mode selection.
- How to Play is reachable before starting a game.
- First-run onboarding is available and can be reopened from Home.
- Practice Tutorial is available from Home and from the final onboarding page.
- Settings and Results are in-activity screens, not separate activities.
- In-game controls remain usable on a 1080 x 2400 emulator without overlap.
- Existing save/load, undo, restart, best records, and solver-assisted record protection still work.

## Product And UX Assessment

This section captures the current Android app experience from a product
engineering and UI/UX engineering perspective. It should be used as the product
planning layer above implementation tickets.

### Current Experience Snapshot

- First-run experience is improved because the app now starts with lightweight
  onboarding before normal play. It teaches the objective, tap/swipe movement,
  whole-line slides, undo/restart, and record rules. Practice Tutorial adds a
  narrow guided first puzzle that highlights movable aligned tiles, teaches one
  first move, and demonstrates a whole-line slide.
- Home separates play, learning, and personal actions. Continue/New Game remains
  the primary path while Beginner Guide, Practice Tutorial, How to Play,
  Settings, and Records use compact grouped actions.
  Records now explains that only player solves count, fewer moves rank first,
  ties use faster time, and assisted wins are excluded from best records.
- Mode Select communicates size, expected session length, the recommended first
  choice, and the Classic best record. Choosing a size opens explicit Relaxed,
  Classic, and Challenge scramble-depth choices without changing movement rules.
- How to Play now includes small static board examples for the solved goal,
  adjacent tap moves, and whole-line slides. It still needs richer visual
  examples for swipe, undo, restart, and solver-assisted completion rules.
- The in-game hierarchy keeps the board first, places status in a quiet surface,
  keeps Undo/Restart visible, and leaves secondary actions behind Menu and
  Assist. Icon-plus-text controls and localized accessibility descriptions are
  covered automatically; a manual TalkBack review remains future work.
- All top-level Android destinations now use one short exit and staggered
  entrance system. Reduced motion bypasses these transitions and board movement
  animation. Interactive surfaces also use ripple feedback.
- Navigation is reasonable for the current one-Activity architecture. Back
  behavior returns from Game to Home and from informational screens to the right
  context. Longer-term, the app needs a clearer navigation model before adding
  Daily Puzzle, progression, and deeper Stats surfaces.
- Current persistence is adequate for a personal casual game: independent 3x3,
  4x4, and 5x5 autosaves, current-size manual Save/Load, last selected
  size/difficulty, visible Continue choices, and size-and-difficulty records.
  It intentionally has no arbitrary extra slots, export, or cloud/back-up path.
  Save payloads include
  explicit updated-at, size, moves, elapsed, active, and solved metadata for
  Continue and release diagnostics.
- The app now has optional local audio and persistent Midnight/Ocean themes, but
  does not yet have reviewed store screenshots or a published privacy policy
  URL. First beta analytics, crash reporting,
  telemetry, ads SDKs, accounts, cloud save, and third-party tracking are
  intentionally deferred so the Android beta remains local-only. Release
  signing injection, versioning, release notes, adaptive launcher icons,
  feature graphic source/export, and Play Store readiness drafts now exist.
- As of this pass, SlideDo is no longer just a raw feature demo, but it is not
  yet a complete mobile game product. The core gameplay works; the missing work
  is mostly onboarding, polish, product systems, release readiness, and
  repeat-play motivation.

### Product Readiness Gaps

- First-run guided path is MVP-level: the new Practice Tutorial covers a first
  move and whole-line slide, but it is still not a full multi-step coached
  first game.
- Settings cover language, Midnight/Ocean themes, optional sound and haptic
  feedback, board/screen Reduced motion, reset save, and reset records.
- Results now uses a completion mark, grouped score summary, record status, and
  clear next actions. A bespoke celebration, sharing, and progression hook do
  not exist.
- The feedback system still lacks a bespoke win celebration, progression
  feedback, and visual treatment beyond Results copy for assisted completion.
- Strategic hints use deterministic fixed-depth lookahead and Manhattan
  distance. They provide useful local guidance but do not claim an optimal or
  complete solution path.
- Progression remains lightweight: daily puzzles, streaks, and recent games now
  exist, while achievements and session goals do not.
- Accessibility is MVP-level: board summaries, settings switch descriptions,
  primary game-control descriptions, 48dp action targets, and automated Reduced
  motion navigation coverage exist. The app still needs a manual TalkBack pass,
  color-contrast review, and cross-device motion review.
- Missing Play Store readiness systems: real upload-key handoff, reviewed
  feature graphic upload, reviewed store screenshots, published privacy policy
  URL, manual accessibility sign-off, optional future crash
  reporting/privacy update, and broad versioning discipline after the first
  beta cycle.
- Missing desktop public-beta readiness systems: public download page, signed
  installer decision, final desktop smoke sign-off, and final desktop
  accessibility sign-off.

## Roadmap And Planning

### Recommended Next Implementation Order

The project owner uses SlideDo primarily as a personal offline game. Public
store submission, upload signing, analytics, accounts, ads, cloud sync, and
store assets are not prerequisites for this program.

Every stage uses the same delivery gate:

1. Add one failing behavior test at an agreed public seam.
2. Implement the complete stage scope and required compatibility handling.
3. Run focused tests, `verify.bat`, the full connected Android suite on Pixel_7
   and `small_phone`, and manual UI/flow review when the stage changes Android UI.
4. Fix every required failure and rerun affected checks.
5. Update this guide, the applicable README, and the regression checklist.
6. Create one cohesive commit only after every required check passes.

| Stage | Goal | Required outcome | Status |
| ---: | --- | --- | --- |
| 1 | True timer pausing | Count active Game-screen time only; exclude dialogs, other screens, and background time while preserving save compatibility. | Completed and verified on 2026-08-20. |
| 2 | Difficulty selection | Add Relaxed, Classic, and Challenge choices backed by reproducible, solvable scramble definitions. | Completed and verified on 2026-08-20. |
| 3 | Replay the same puzzle | Let Results restart the exact starting board without generating another scramble. | Completed and verified on 2026-08-20. |
| 4 | Per-size saves | Preserve independent 3x3, 4x4, and 5x5 active games and expose the correct Continue choices. | Completed and verified on 2026-08-20. |
| 5 | History and personal stats | Store bounded local completion history and show meaningful per-size and per-difficulty summaries. | Completed and verified on 2026-08-21. |
| 6 | Offline daily challenge | Generate one deterministic local puzzle per date and record completion/streak state without a server. | Completed and verified on 2026-08-22. |
| 7 | Strategic hint | Suggest a useful next move, mark the game assisted, and preserve player-record protection. | Completed and verified on 2026-08-22. |
| 8 | Sound and themes | Add optional local sound feedback and selectable visual themes with persistent settings and accessibility-safe defaults. | Completed and verified on 2026-08-22. |

### Personal Play 2.0

The second personal-play program keeps the same test, documentation, and commit
gate. Each stage must preserve shared puzzle rules, deterministic puzzle
identity, save compatibility, active-play timing, and assisted-record
protection before the next stage begins.

| Stage | Goal | Required outcome | Status |
| ---: | --- | --- | --- |
| 1 | Android offline backup and restore | Export every Android save, record, statistic, daily field, and setting to a versioned local document; validate and confirm before complete replacement. | Completed and verified on 2026-08-23. |
| 2 | Daily challenge calendar and history replay | Browse completed and missed local dates and replay any deterministic historical daily puzzle without changing the current-date streak twice. | Completed and verified on 2026-08-23. |
| 3 | Favorite puzzle library | Bookmark exact puzzle identities, label them locally, and replay a favorite without reshuffling or altering normal saves. | Completed and verified on 2026-08-23. |
| 4 | Personal trends and custom goals | Show local time/move trends and owner-defined goals without analytics or network services. | Completed and verified on 2026-08-24. |
| 5 | Continuous challenge mode | Chain completed puzzles into a local session with clear progress, exit, resume, and record boundaries. | Planned; next stage. |
| 6 | Move history and Redo | Expose the current run's action history and add Redo while preserving whole-line one-action semantics, Restart, Save/Load, and solver input locks. | Planned. |
| 7 | Adaptive and accessibility reinforcement | Improve compact/large-screen layout behavior, larger-text resilience, focus order, TalkBack descriptions, contrast, and reduced-motion coverage. | Planned. |
| 8 | Toolchain and CI maintenance | Refresh supported Android/Gradle tooling, keep Windows and GitHub CI reproducible, and document warnings or compatibility migrations. | Planned. |

Shared puzzle rules, deterministic puzzle identity, elapsed milliseconds, save
compatibility, and assisted-record protection remain core contracts. Android
may receive the player-facing flow first, but reusable puzzle and persistence
rules stay outside Android view code.

### Next Phase: Desktop/Mobile Parity Pass

Priority: High

Recommended MVP scope:

- [x] Add a desktop entry/home surface or equivalent start panel that exposes New
  Game, Continue/Load, How to Play, Records, and Settings/Preferences in the
  same conceptual order as Android.
- [x] Add desktop How to Play / Practice Tutorial content that matches Android's
  first move, movable aligned tiles, and whole-line slide teaching.
- [x] Add a desktop Assist hint entry that highlights movable same-row /
  same-column tiles without moving the model, counting a move, or invoking a
  solver.
- [x] Add a desktop Results surface or post-win panel with wording consistent with
  Android, including solver-assisted record protection.
- Keep all puzzle behavior routed through `GameModel`; desktop parity work
  should be UI/presentation plus tests.

2026-06-10 first parity slice:

- Desktop now has `Assist > Show Movable Tiles`, which highlights every
  non-empty tile aligned with the empty cell. The highlight is presentation-only
  and clears on move, undo, restart, load, new game, or solver playback.
- Desktop now has `Help > How to Play` and `Help > Practice Tutorial` dialogs
  using Android-aligned language for first moves, movable aligned tiles,
  whole-line slides, and solver-assisted record protection.
- Added focused JUnit coverage for the desktop help copy so the parity terms do
  not disappear during later UI refactors.

2026-06-11 second parity slice:

- Desktop now opens on a Home/start surface instead of directly entering the
  board.
- Home and the Game menu expose 3x3, 4x4, 5x5, Continue/Load, How to Play,
  Practice Tutorial, Records, and Preferences.
- Records summarizes local 3x3, 4x4, and 5x5 best records while preserving the
  solver-assisted record rule.
- Preferences adds reduced motion for desktop tile animation as presentation
  state only.

2026-06-11 third parity slice:

- Desktop now replaces the generic win dialog with an Android-style Results
  dialog after the final board animation completes.
- Results show the solved/assisted subtitle, puzzle size, moves/time, first
  record, new best, unchanged best, or solver-assisted no-record wording.
- Results offer Play Again, New Size, and Home actions for desktop parity with
  Android's completion flow.
- Desktop/Mobile Parity Pass MVP scope is now complete. Next implementation
  should move to the targeted architecture split that supports future shared
  progression work.

Acceptance criteria:

- Desktop and Android expose comparable player-facing learning and completion
  flows, even if the layout differs by platform.
- Existing desktop mouse, keyboard, save/load, records, solver playback, undo,
  restart, and whole-line slide behavior still work.
- Shared core tests remain unchanged unless a real rule gap is found.
- `verify.bat` passes, and desktop compile/Javadocs remain warning-clean.

Desktop/Android feature parity matrix:

| Area | Android status | Desktop status | Platform difference / next step | Verification |
| --- | --- | --- | --- | --- |
| Shared puzzle rules | Uses shared `GameModel`; `move(Direction)` and `slideLineTo(row, col)` remain the only rule path. | Uses the same shared `GameModel`. | No known rule gap. Keep future puzzle behavior in shared core tests. | Root Gradle tests, connected Android whole-line/undo tests, desktop compile. |
| Home / start | Native Home with Continue metadata, New Game, Beginner Guide, Practice Tutorial, How to Play, Settings, and Records. | Swing Home/start with New Game, Continue/Load, How to Play, Practice Tutorial, Records, and Preferences. | Android has a richer first-run beginner guide; desktop has equivalent help entry but no paged onboarding. This is acceptable for beta. | Android instrumentation Home tests; desktop home copy tests. |
| Mode selection | Mode Select starts 3x3, 4x4, and 5x5 games with difficulty labels, expected session length, first-puzzle guidance, and best record summaries. | Home/Game menu starts 3x3, 4x4, and 5x5 games and Records shows best summaries. | Android has richer pre-game guidance; available choices and record summaries match. | Android mode-select instrumentation; desktop compile/manual smoke. |
| Learning surfaces | First-run onboarding, visual How to Play, Quick Reminder, and interactive Practice Tutorial. | How to Play and Practice Tutorial dialogs use Android-aligned language. | Android remains more visual and interactive; desktop parity covers the same concepts. | Android onboarding/tutorial/how-to instrumentation; desktop help-content tests. |
| Touch/mouse movement | Tap/swipe aligned tiles; whole-line slide counts as one move and one undo snapshot. | Mouse click/release movement plus keyboard controls; whole-line slide uses shared model. | Input method differs by platform, rule outcome matches. | Shared core tests, Android whole-line instrumentation, desktop smoke. |
| Assist / hints | Assist can suggest one strategic adjacent move, highlight all movable tiles, or offer solver playback. | Assist highlights movable tiles and supports solver playback. | Strategic guidance is Android-first; strategic- and solver-assisted wins do not update Android player records. | Android strategic-hint/persistence/results instrumentation; desktop result-copy tests. |
| Save/load metadata | `AndroidGameStore` persists independent 3x3, 4x4, and 5x5 slots with size, grid, initial grid, moves, elapsed, updated-at, active, solved, and difficulty; it migrates the legacy single save without replacing a newer matching slot. | Desktop JSON save persists one size, grid, initial grid, moves, elapsed, updated-at, active, and solved; records live in user-data path. | Shared gameplay metadata is aligned; Android adds per-size slots and mobile-only settings/onboarding. | Android store instrumentation, root save metadata tests. |
| Settings / preferences | Persistent English, Traditional Chinese, and Japanese language selection, haptic feedback, reduced motion, reset all saved games, and reset records. | Reduced motion preference plus desktop records/save flows. | App-language and haptics are Android-only; desktop currently remains English. | Android locale/store/settings instrumentation; desktop preferences copy tests/manual smoke. |
| Records | Per-size local best records, fewer moves then lower time, solver-assisted protection, and player-facing policy explanation. | Per-size local best records with the same comparison, solver-assisted protection, and policy explanation. | Aligned. | Android records/results instrumentation; desktop result and records tests. |
| Results | Full Results screen with exact-board Replay Puzzle, New Size, Home, record status, and assisted wording. | Android-style Results dialog with Play Again, New Size, Home, record status, and assisted wording. | Android now replays the same starting board for the Personal Play roadmap; desktop retains its new-puzzle action. | Android replay/results instrumentation; desktop results copy tests. |
| Accessibility | Board summaries, settings switch descriptions, and primary control descriptions exist. | Basic Swing labels/dialog text exist, but no full assistive-tech audit. | Both platforms still need broader manual accessibility review before public release. | Android accessibility instrumentation plus manual TalkBack/desktop review. |
| Packaging / release | Debug build, connected tests, signed APK/AAB, Play readiness file check, screenshot smoke workflow. | Desktop ZIP and optional app-image package with user-data paths plus a desktop beta readiness check. | Android still needs real Play upload key and Play Console external assets; desktop package is not a signed installer. | `verify.bat`, `verify-connected.bat`, `verify-release.bat`, manual screenshot smoke, desktop beta smoke checklist. |

Parity conclusion for current beta:

- Android and desktop now expose comparable core gameplay, learning, records,
  settings/preferences, assist hints, save/load, and completion flows.
- Remaining gaps are release and platform polish, not shared puzzle behavior:
  Android needs real Play upload signing, final store assets, privacy-policy URL,
  and manual accessibility/pre-launch passes; desktop needs signed installer
  planning only if the beta moves beyond ZIP / app-image distribution.
- Future repeat-play systems such as achievements and session goals should be
  scoped deliberately; the current owner-only daily flow is Android-first.

### Completed 2026-05-25 MVP Items

### 1. Add Android Instrumentation Tests

Priority: High

Status: Completed on 2026-05-25; sound and theme additions completed on 2026-08-22.

- [x] Launch default Home screen.
- [x] Navigate Home -> Mode Select -> Game.
- [x] Continue from an existing save.
- [x] Open How to Play.
- [x] Verify whole-line move count and undo after entering Game.
- [x] Verify save/load persistence and rotation behavior.

Implementation notes:

- The Android app now exposes stable resource IDs for main navigation and game
  controls so instrumentation tests do not rely on visible text or coordinates.
- Instrumentation tests use the real `MainActivity`, shared preferences, and
  `KlotskiView` touch dispatch to cover app-level behavior while avoiding
  emulator launcher/input-sink flakiness.
- Activity recreation now restores the active screen and reloads saved game
  state when rotating during gameplay or an in-game informational screen.

### 2. Add First-Run Onboarding

Priority: High

Status: Completed on 2026-05-25.

- [x] Add a lightweight first-run state in `SharedPreferences`.
- [x] Show a short onboarding flow before the first game, with Skip and Start 3x3
  actions.
- [x] Recommend 3x3 as the first puzzle for new players.
- [x] Teach goal, tap, whole-line slide, swipe, undo, restart, and records.
- [x] Re-open onboarding from Home.

MVP flow:

1. On first launch, show onboarding before the player starts their first game.
2. Page 1: objective, ordered numbers, empty cell in the bottom-right corner.
3. Page 2: tap a tile in the same row or column as the empty cell.
4. Page 3: whole-line slide, where farther aligned tiles slide as one move.
5. Page 4: Undo, Restart, Records, and solver-assisted records not counting as
   player bests.
6. Skip marks onboarding as seen and returns to Home.
7. Start 3x3 marks onboarding as seen and starts a 3x3 game.

Acceptance criteria:

- [x] First install or cleared app data shows onboarding before normal play.
- [x] Returning users do not see onboarding automatically after Skip or Start 3x3.
- [x] Home exposes a way to re-open onboarding or the same beginner guide.
- [x] Instrumentation tests cover first launch onboarding, Skip persistence, Start
  3x3 navigation, and re-opening the guide.
- [x] The implementation keeps `GameModel` unchanged and confines first-run state to
  Android UI/preferences code.

### 3. Replace Text-Only How To Play With Visual Learning

Priority: High

Status: Completed on 2026-05-25.

- [x] Add small board diagrams or inline demo panels for tap and whole-line slides.
- [x] Show the empty cell explicitly in each example.
- [x] Make the examples short enough to scan before starting a game.
- [x] Add a compact reminder from the in-game Menu.
- [x] Keep all puzzle-rule explanations aligned with `GameModel` semantics.

Recommended scope:

- [x] Reuse the onboarding teaching content where possible.
- [x] Add small static board examples before building a fully interactive tutorial.
- [x] Prioritize visual clarity for whole-line slide because it is the least obvious
  mechanic for new players.
- [x] Add instrumentation or screenshot smoke coverage after stable IDs exist for
  the visual examples.

### 4. Add Android Interactive Tutorial MVP

Priority: High

Status: Completed on 2026-05-26.

- [x] Add a Home entry for Practice Tutorial.
- [x] Add a Practice Tutorial action on the final onboarding page while keeping
  the direct Start 3x3 path available.
- [x] Use a fixed 3x3 guided model state for the first move and a second fixed
  state for whole-line slide practice.
- [x] Highlight movable same-row / same-column tiles as presentation only.
- [x] Emphasize one target tile per lesson: 6 for the first move, 5 for the
  whole-line slide.
- [x] Keep board gestures routed through `KlotskiView` and
  `GameModel.slideLineTo(row, col)` instead of copying move execution rules into
  Android UI code.
- [x] Add instrumentation coverage for Home -> Practice Tutorial, first move,
  whole-line slide completion, and Start 3x3 handoff.

MVP flow:

1. Home or onboarding opens Practice Tutorial.
2. Lesson 1 shows a near-solved 3x3 board, highlights aligned movable tiles, and
   asks the player to tap tile 6.
3. After the move, Lesson 2 resets to a whole-line teaching board and asks the
   player to tap tile 5.
4. The whole row slides through `GameModel.slideLineTo(row, col)`, the status
   confirms it counted as one move, and the player can start a normal 3x3 game.

### 5. Add Lightweight Assist Hints

Priority: High

Status: Completed on 2026-05-26.

- [x] Add Show Movable Tiles as the first Assist action before solver options.
- [x] Highlight the current same-row / same-column movable tiles without changing
  the model state.
- [x] Keep the hint presentation-only: no move count change, no automatic move,
  no solver path, and no record impact.
- [x] Clear the hint after the next player move, whole-line slide, undo, restart,
  load, or screen rebuild.
- [x] Add instrumentation coverage for opening Assist, showing the hint, verifying
  the move count stays unchanged, and clearing the hint after a move.

MVP behavior:

1. In Game, Assist opens with Show Movable Tiles before BFS, A*, and IDA*.
2. The hint highlights every non-empty tile aligned with the empty cell.
3. The status line explains that highlighted tiles can slide into the empty cell.
4. The player still decides which highlighted tile to tap or swipe; the shared
   `GameModel` remains the only rule source for executing the move.

### 6. Add Settings

Priority: High

Status: Completed on 2026-05-25.

- [x] Add haptic feedback toggle.
- [x] Add sound effects toggle after audio exists.
- [x] Add theme selection after at least one alternate theme exists.
- [x] Add reduced-motion preference before adding heavier animations.
- [x] Add reset local data actions for save and records, behind confirmation.

Recommended MVP scope:

- [x] Add Settings entry from Home and in-game Menu.
- [x] Store haptic, sound, visual-theme, and reduced-motion preferences in
  `SharedPreferences`.
- [x] Apply haptic toggle to existing board/control feedback.
- [x] Add reset save and reset records actions behind confirmation dialogs.
- [x] Keep sound off by default and apply the persisted visual palette without
  changing gameplay state, saves, or records.

### 7. Add Results Screen

Priority: High

Status: Completed on 2026-05-25.

- [x] Replace the solved-game dialog with a full Results screen.
- [x] Show size, moves, time, previous best, and whether the player set a new best.
- [x] Show solver-assisted completions with distinct wording and no record write.
- [x] Offer Replay Puzzle, New Size, Home, and optionally Share later.

Recommended MVP scope:

- [x] Replace the current solved dialog with an in-activity Results screen.
- [x] Preserve record comparison rules: fewer moves first, then lower time.
- [x] Keep solver-assisted completions visibly separate and excluded from player
  best records.
- [x] Add instrumentation coverage for normal win result navigation and
  solver-assisted no-record behavior after the screen exists.

### 8. Add CI For Build Quality

Priority: Medium

Status: Completed on 2026-05-25.

- [x] Run root JUnit tests.
- [x] Compile desktop Java.
- [x] Run Android assemble/lint.
- [x] Compile Android instrumentation test APKs in the local verification path.
- [x] Run public Javadoc doclint checks where practical.

Recommended scope:

- [x] Start with a local one-command verification script if GitHub Actions is not
  configured yet.
- [x] CI should run shared core tests, desktop compile, Android assemble/lint, and
  API doclint.
- [x] `verify.bat` now also assembles the Android instrumentation test APK so
  test source compilation is checked without requiring an emulator.
- [x] `verify-connected.bat` runs connected instrumentation tests when a device
  or emulator is available.
- [x] Add `ci.bat` as the no-device local/CI gate that runs `verify.bat` and
  `verify-release.bat` together.
- [x] Add `.github/workflows/ci.yml` so GitHub Actions runs the same no-device
  build, lint, Javadoc, release artifact, and Play readiness checks on Windows.
- Connected Android instrumentation tests still require a running emulator or
  device outside the one-command no-device verification path.
- [x] Add release-readiness checks for signed release builds, versioning, Play
  App Bundle generation, desktop packaging, and store asset/readiness files.

### UX Improvement Directions

- [x] Add consistent top-level exit/entrance motion, pressed-state ripples, and
  a Reduced motion bypass shared by every Android destination.
- [x] Group Home actions by play, learning, and personal intent instead of
  presenting every action as an equal full-width button.
- [x] Use icon-plus-text buttons for the Home surface and the persistent Home,
  Menu, Assist, Undo, and Restart game controls.
- Add icons to secondary Save/Load actions only if the current platform menu is
  replaced with a custom surface; the present text list remains intentionally
  lightweight.
- Strengthen Home as a game entry screen with a small playable board preview or
  animated tile motif instead of only text and buttons.
- [x] Make Mode Select more informative: show best record, difficulty,
  estimated session length, and recommended first mode.
- [x] Make in-game controls feel compact and native: keep primary actions visible,
  move rare actions behind Menu, and avoid exposing solver terminology too early.
- [x] Separate Assist from Solver. Assist first offers one-step hints or
  highlight movable lines; full solver playback can remain in an advanced Tools
  area.
- [x] Add an immediate completion mark, grouped result summary, record status,
  and clear next actions.
- [x] Add a short completion-mark celebration after the board settles, with no
  delay or scale animation when Reduced motion is enabled.
- [x] Add clear empty-cell affordance and movable-tile hints for first-time players.
- [x] Review typography and spacing on a 720x1280, 320 dpi phone profile in
  addition to the 1080x2400 Pixel_7 profile.
- Continue accessibility review for custom board content with TalkBack, larger
  touch targets, contrast checks, and reduced-motion behavior.
- Add orientation and background/resume validation whenever screen state expands.

### Technical Improvement Directions

- Keep `GameModel` platform-independent and continue to route all puzzle-rule
  changes through shared core tests.
- [x] Split the first Android architecture layer out of `MainActivity`:
  Activity state serialization, back-navigation decisions, shared UI primitives,
  result/pending-win models, and learning-content builders now live in
  package-private helpers.
- [x] Split Android Home screen construction into `AndroidHomeScreen`, including
  Continue metadata presentation and Home navigation callbacks.
- [x] Split Android Game and Practice Tutorial screen construction into
  `AndroidGameScreen` and `AndroidTutorialScreen`, keeping model updates,
  highlights, and command acceptance in `MainActivity`.
- [x] Split Android Mode Select, Settings, Records, and Results screen
  construction into package-private builders.
- Continue architecture cleanup by extracting onboarding and How to Play only
  if those learning flows grow beyond their current small controller surface.
- [x] Harden connected instrumentation helpers for slow emulator launches and
  How to Play scrolling so the suite is less dependent on transient AVD timing.
- [x] Add stable identifiers or test hooks for important Android controls so
  instrumentation tests do not rely on text or screen coordinates.
- [x] Add a small Android Activity state model for screen, info return target,
  first-run/tutorial page state, game-started state, and pending results.
- Continue the architecture split started by `AndroidGameStore`; persistence,
  Activity state, navigation decisions, shared UI primitives, learning content,
  and the major Android screen builders are now outside `MainActivity`, while
  onboarding and How to Play remain small in-activity learning flows.
- [x] Add explicit save metadata: saved size, moves, elapsed time, updated-at
  timestamp, and solved/active status.
- [x] Surface Android save metadata in the Home Continue UI so players can see
  the saved board size, move count, elapsed time, active/solved state, and save
  freshness before resuming.
- [x] Add release build checks before Google Play planning: signed release
  APK/AAB generation, release signing injection, versionCode/versionName policy,
  release notes checks, and reproducible release commands.
- Decide whether release builds should stay unminified for beta or enable
  minification/resource shrinking after a focused release-size and stack-trace
  review.
- Add lint and instrumentation checks to CI before expanding visual complexity.
- [x] Add a repeatable screenshot smoke workflow for Home, Mode Select, Game,
  How to Play, Settings, Results/current, and Records.
- [x] Add screenshot set verification so the manual capture workflow validates
  readable PNG outputs and writes a manifest for the captured set.
- Replace the manual screenshot capture workflow with automated screenshot
  tests or pixel-diff review once navigation builders are split and stable.

### Mid- And Long-Term Planning

Priority: Low to Medium

- Sound effects with mute toggle.
- Theme selection.
- Daily puzzle.
- Difficulty presets based on scramble depth.
- Accessibility labels and larger touch targets.
- Recent games or session history.
- Achievements for first solve, low-move solve, fast solve, and daily streak.
- Optional cloud backup only after local data and privacy expectations are clear.
- Tablet and foldable layouts after phone layout stabilizes.
- Add more locales only when beta feedback identifies demand; Android currently
  supports English, Traditional Chinese, and Japanese.

### Google Play Readiness Planning

- [x] Prepare adaptive launcher icon resources.
- [x] Draft short/long store descriptions, privacy policy text, Data Safety
  answers, store asset checklist, and pre-launch matrix in
  `android/PLAY_STORE_READINESS.md`.
- [x] Add `android/check-play-store-readiness.bat` and wire it into
  `verify-release.bat` so repo-side store files, adaptive icons, privacy/Data
  Safety assumptions, release artifacts, screenshot workflow, and tracked-secret
  checks are verified together.
- [x] Add a version-controlled Play Store feature graphic source under
  `android/store-assets/`, with local readiness checks for the source file and
  asset notes.
- [x] Add a repeatable Play Store feature graphic PNG export workflow and wire
  it into `verify-release.bat`.
- [x] Add a release artifact manifest with SHA-256 hashes for Android, desktop,
  store asset, and release-note handoff files.
- [x] Decide the first beta telemetry posture: `0.2.0-beta.1` ships without
  analytics, crash reporting, telemetry, ads SDKs, accounts, cloud save, or
  third-party tracking, and the readiness check now verifies that decision.
- [x] Add screenshot review, accessibility review, and pre-launch evidence
  worksheets to the Play Store readiness handoff, and verify their presence in
  `android/check-play-store-readiness.bat`.
- Review the generated feature graphic upload file, then capture and review
  screenshots that show the actual game UI.
- Publish a privacy policy URL before store submission and before adding
  analytics, crash reporting, cloud save, ads, or account features.
- Keep Data Safety answers aligned with actual collection behavior; keep
  local-only gameplay data local unless there is a clear product reason to sync
  it.
- Add crash reporting and basic performance telemetry only after privacy
  wording, Data Safety answers, release notes, and user consent expectations
  are settled.
- [x] Add release signing injection, Play App Bundle generation, versioning, and
  release notes workflow.
- Replace temporary local release verification signing with the real Play upload
  key when store submission begins; this is intentionally deferred from the
  current push-ready milestone.
- [x] Establish an initial multi-AVD pre-launch baseline on Android 15 / 1080x2400
  and Android 16 / 720x1280, including rotation and background/resume coverage.
- Expand the pre-launch matrix to a physical device and a tablet or foldable
  profile before store submission.
- Add accessibility review before store submission, including screen reader
  labels for the board and settings for reduced motion/audio.
- Decide whether solver features are player-facing, debug-only, or advanced
  tools before presenting screenshots or store copy.

### Desktop Public Beta Planning

- [x] Add `DESKTOP_BETA_READINESS.md` with current beta target, public beta
  blockers, package contents, tester instructions, manual desktop smoke
  checklist, accessibility review, and release checklist.
- [x] Add `check-desktop-beta-readiness.bat` to verify desktop readiness docs,
  package artifacts, ZIP contents, release notes, package README instructions,
  and that generated desktop packages/local saves are not tracked.
- [x] Wire the desktop public beta readiness check into `verify-release.bat`
  after package generation and release manifest creation.
- [x] Expand the generated desktop ZIP `README.txt` with runtime requirements,
  tester smoke prompts, save-location notes, and known limits, then verify that
  content in the desktop readiness check.
- Choose the public beta download page and issue-reporting channel.
- Decide whether the first desktop public beta ships as ZIP/app-image only or
  needs a signed installer before wider distribution.
- Run the final desktop smoke checklist from the extracted ZIP.
- Run the final desktop accessibility review.

## Development Log

### 2026-08-24

- Completed Personal Play 2.0 Stage 4 Personal Trends and Custom Goals.
  `PersonalTrend` compares newest-first player completions only within one
  matching size and difficulty. Six results start an equal three-versus-three
  comparison; larger histories expand to two five-result windows. Moves and
  elapsed time use a five-percent steady band, while assisted and favorite
  practice results remain excluded. `WeeklyGoalProgress` defines a local
  Monday-through-Sunday target from 1 to 50 and ignores stale, future, and
  assisted completions.
- Android Home now opens the localized Trends & Weekly Goal screen. The screen
  shows current-week progress, accepts a validated owner goal, remembers one of
  nine comparison scopes, presents recent averages or like-for-like trend
  percentages, and remains fully reachable by scrolling on the compact AVD.
  Goal and scope preferences participate automatically in versioned offline
  backup/restore; Reset Records clears the source completion history without
  changing the owner's target.
- Added four shared trend/goal tests, two Android persistence tests, and one
  complete UI flow covering progress, comparison, goal editing, scope changes,
  empty state, and Back navigation. All 45 shared tests passed. In final serial
  runs, Pixel_7 passed 94/94 Android tests in 603.426s and `small_phone` passed
  94/94 in 629.539s, with no failures or skips; both post-launch crash buffers
  were empty. All 299 localized resource keys and format signatures match. The
  final debug APK SHA-256 is
  `C5D3691C58C6A356C2E7BD18D9589E436DA4A995C7372CC3C7FEAF8FEEF7C0AF`.

### 2026-08-23

- Completed Personal Play 2.0 Stage 3 Favorite Puzzles. The shared
  `PuzzleIdentity` derives a stable SHA-256 identity from board size,
  difficulty, and the exact defensive-copy starting grid. Android stores up to
  50 owner-named favorites, renames duplicate identities instead of adding a
  second row, ignores malformed entries, includes the library in offline
  backup/restore, and removes a deleted favorite's isolated practice state.
- Home opens the localized favorite library; Game Menu and Results can save or
  rename the current exact puzzle. Library cards support Replay, Rename, and
  Remove with localized accessibility descriptions. Favorite replay uses an
  isolated save namespace for rotation/background continuity, never replaces a
  normal or daily save, and intentionally skips completion history, lifetime
  statistics, daily streaks, and best-record updates. Reset Saved Games clears
  practice progress but preserves the named library; Reset Records also leaves
  favorites intact.
- Added two shared identity tests, five favorite-store tests plus backup
  coverage, one Activity-state test, and one full UI flow from naming through
  exact replay and practice-only Results. All 40 shared tests passed. Pixel_7
  passed 91/91 Android tests in 8m52s and `small_phone` passed 91/91 in 9m55s,
  with no failures or skips; both crash buffers were empty. The final debug APK
  SHA-256 is `D35B3CBA33E1C8A13B1BC35E165D7852C21EA0F5E5F466C8E3BCC3734FD04F83`.

- Completed Personal Play 2.0 Stage 2 Daily Calendar and history replay.
  `DailyCalendarMonth` supplies a Sunday-first, future-bounded month model, and
  Android renders localized previous/next navigation, date status, and
  screen-reader actions for ready, completed, in-progress, missed, and future
  dates. The selected month survives Activity recreation.
- Daily boards now use independent versioned preference slots by ISO date. A
  valid legacy single-date daily save migrates into its dated slot without
  overwriting newer data. Historical completion remains visible but cannot
  move the latest-date streak backward; completing the same date stays
  idempotent and assisted completions remain excluded from player bests.
- Added two shared month-model tests, three daily-store tests, one Activity-state
  test, and one end-to-end calendar replay flow. The full Android suite now
  contains 84 tests. Manual Android CLI review confirmed the complete month
  grid and scrollable Back action at 720x1280, disabled future navigation, and
  readable status colors and symbols. Pixel_7 passed 84/84 in 7m36s and
  `small_phone` passed 84/84 in 7m31s; both crash buffers were empty. The final
  debug APK SHA-256 is
  `F10B1C056903576C5942F63DBD66AFAC7CA88857979E459F486DADD68706E1AB`.

- Completed Personal Play 2.0 Stage 1 Android offline backup and restore.
  `AndroidPersonalDataArchive` writes version 1 JSON for every
  SharedPreferences-compatible value and rejects malformed, oversized,
  duplicate-key, unsupported-type, and unsupported-version documents before
  `AndroidGameStore` replaces the preference store.
- Added Settings Export backup and Import backup actions in English,
  Traditional Chinese, and Japanese. Android's create/open document pickers own
  file access, so SlideDo adds no network or storage permission. Import shows a
  complete-replacement warning, applies restored state, then recreates the
  Activity so language and presentation settings take effect.
- Added five instrumentation tests for complete saves/records/settings round
  trip, missing-preference replacement, invalid-version no-mutation behavior,
  visible Settings actions, and confirmed restore with Activity recreation.
  `verify.bat` passed all 37 shared tests, desktop compilation, Android
  assemble/test APK/lint, and both Javadoc/doclint gates.
- Pixel_7 passed all 79 Android tests in one serial run on Android 15 at
  1080x2400 in 7m46s. `small_phone` passed the same 79 tests in one serial run
  on Android 16 / API 36.1 at 720x1280 in 7m10s. Neither run failed or skipped a
  test.
- Manual Android CLI review covered real export/import system pickers, the
  replacement confirmation, compact Settings reachability, malformed JSON
  rejection without preference mutation, and empty crash buffers on both AVDs.
  The final debug APK SHA-256 is
  `40FD389B702534A9C5578B4DFDC67D45730356ABFD9E0C8D9BA017FC4ADADE3F`.

### 2026-08-22

- Repaired the Windows CI gate after the first GitHub-hosted run exposed two
  environment assumptions. `verify.bat` now locates Android API 36 through a
  valid `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or local SDK fallback and reports a
  direct setup error when none contains `android.jar`. The release manifest
  writer now computes SHA-256 through the .NET cryptography API instead of
  depending on PowerShell module autoloading. Focused failure/recovery checks,
  an independent `certutil` hash comparison, and the full `ci.bat` gate passed.

- Completed Stage 8 sound and themes. Android Settings now offers optional local
  move/completion tones, off by default, and persistent Midnight/Ocean palettes.
  `AndroidSoundFeedback` lazily owns the platform tone generator and suppresses
  per-step solver playback; no network, media asset, or permission is required.
- Theme colors are presentation-only roles in `AndroidUi` and `KlotskiView`.
  Changing themes recreates the Activity after saving, preserving the active
  board, initial board, moves, timer, difficulty, assistance, saves, and records.
- Added one persistence test and one end-to-end Settings/theme flow. The Android
  suite now contains 74 tests. Pixel_7 passed all 74 in five isolated shards
  after a no-snapshot cold boot recovered an AVD system crash. `small_phone`
  covered all 74: its full run passed 73 and one transient Home-window-focus
  timeout passed immediately when rerun alone.
- Manual review confirmed Midnight and Ocean Settings, the Ocean Home/game
  palette, checked sound state, and the 720x1280 scrollable Settings flow remain
  readable, non-overlapping, and reachable. Root Gradle 8/9 tests also pass after
  declaring the JUnit Platform launcher explicitly. The final warning-clean
  `verify.bat` pass completed, and the debug APK SHA-256 is
  `3B64AC1323E5B2B6195829633730EF4731C3283FBB4C922672DDD3D719573ABA`.

- Completed Stage 7 strategic hints. The shared `StrategicHint` service uses a
  deterministic four-ply search with Manhattan-distance evaluation to select
  one legal adjacent tile without mutating `GameModel` or increasing moves.
- Android Assist now presents Strategic Hint, Show Movable Tiles, and Solver
  Tools as separate choices. A strategic hint highlights only its recommended
  tile and clearly marks the run assisted; the lightweight movable-tile hint
  remains presentation-only and does not mark assistance.
- Assisted state is stored independently with normal and daily saves and
  survives rotation, Load, Restart, and assisted Results replay. Starting a new
  puzzle clears it. Assisted completions still enter local history but cannot
  replace a player best.
- Added two shared-core tests, one persistence test, and one end-to-end Android
  flow covering no board mutation, deterministic selection, save/rotation,
  Restart, Results replay, and player-best protection. The Android suite now
  contains 72 tests.
- `small_phone` passed all 72 tests. Pixel_7 passed the same 72-test coverage in
  five isolated shards after a CLI cold boot repaired stale emulator window
  focus; one unrelated localized Records flow transiently lost focus in a shard
  and passed immediately when rerun alone. Manual 1080x2400 Japanese and
  720x1280 English review confirmed the three-option Assist dialog, highlighted
  tile, assistance warning, and bottom controls remain unclipped.
- The final warning-clean `verify.bat` pass covered 37 shared/desktop tests,
  desktop compilation, public core/desktop and Android API Javadocs, debug and
  test APK assembly, and Android lint. The debug APK SHA-256 is
  `E6067CDAB19D36B7CC8EB33826DDA60065A62627C52F4B34BEDA28B57C6B0248`.

- Completed Stage 6 offline Daily Challenge. `DailyChallenge` maps a local
  ISO-8601 calendar date to a versioned deterministic seed and creates one 4x4
  Classic puzzle entirely through shared `GameModel` scramble rules.
- Android Home now shows the date, Ready/In progress/Completed state, and current
  and best streaks. Daily games use an independent resumable save; a solved daily
  game replays its original board, while normal 3x3/4x4/5x5 saves remain intact.
- Daily completion is idempotent by date. Consecutive dates extend the streak, a
  gap resets the current streak, and the best is preserved. Daily completions
  still feed ordinary history/statistics, while solver-assisted wins retain the
  existing player-best protection.
- Reset Saved Games clears the daily board without erasing streak records. Reset
  Records clears daily completion/streak state without erasing the daily board.
  Activity and Results state retain the daily date across recreation.
- Added two shared-core tests, four daily persistence/reset tests, one daily
  Activity-state test, and a visible Home-to-Daily-to-Results flow. The Android
  connected suite now contains 70 tests.
- Compact-screen regression exposed that the new Home content pushed localized
  primary actions below the first viewport. Removing the redundant goal summary
  and shortening daily status copy kept all primary Home actions visible at
  720x1280. Timing tests now sample after dialogs are visible, so they measure
  paused dialog time rather than active navigation latency.
- `verify.bat` passed. The final `small_phone` suite passed 70/70; the Pixel_7
  full run passed 69/70 before the timing-test synchronization correction, and
  all three corrected dialog-timing tests then passed on both AVDs. Manual review
  confirmed identical dated boards and unclipped Home/Game layouts on 1080x2400
  and 720x1280. Both cold launches resumed `MainActivity` with empty crash
  buffers. The final debug APK SHA-256 is
  `EF92467A66D3F453FF95DE00122C68B5B092A63CADAE59B10ED73EBCCB050499`.

### 2026-08-21

- Completed Stage 5 local completion history and personal statistics.
  `AndroidGameStore` retains the newest 50 player or solver-assisted
  completions while lifetime counters continue independently for all nine
  size/difficulty scopes. Each record stores completion time, size, difficulty,
  moves, elapsed milliseconds, and assisted state.
- Records now shows overall player and assisted totals, player-only averages,
  the newest 10 completion entries, and per-scope bests, totals, and averages.
  English, Traditional Chinese, and Japanese resources use locale-aware dates,
  move plurals, difficulty names, and player/assisted labels.
- Results records each completion exactly once before evaluating the player
  best. Solver-assisted completions contribute to history and assisted totals
  but cannot replace a player best. Reset Records clears bests, history, and
  lifetime statistics while preserving saves and settings.
- Added three persistence tests for scope isolation, the 50-entry retention
  boundary, unbounded lifetime counters, and full record reset. Added connected
  flows for player/assisted history, localized statistics, record reset, and
  Results recreation without duplicate history writes.
- Connected-test review exposed two harness races on the compact AVD. Button
  clicks now wait for a visible, enabled, focused activity view. Board gestures
  now use screen coordinates and UIAutomator device clicks instead of injecting
  `MotionEvent` objects directly into the View. The two-step tutorial passed five
  consecutive compact-AVD repetitions after the change.
- Final Stage 5 evidence: all 33 shared tests and `verify.bat` passed. All 63
  Android tests passed on Pixel_7 and `small_phone` in six isolated batches
  (19 + 10 + 10 + 8 + 9 + 7), with zero failures or skips. The final installed
  APK also passed the 18-test Stage 5 subset on both AVDs. Manual 720x1280
  review reached the totals, recent entries, all nine breakdown panels, and Back
  without clipping or overlap. Fresh post-launch `AndroidRuntime` and crash
  buffers were empty on both AVDs, and the final debug APK SHA-256 is
  `C8D78B4CE1E295D20DA80D366BF2940106D9F1C0B9849918519865F435AB2180`.

### 2026-08-20

- Replaced the public-beta-first roadmap with an eight-stage Personal Play
  program. Every stage now requires focused tests, the local verification gate,
  dual-AVD connected regression, documentation sync, and its own commit before
  the next stage begins.
- Completed Stage 1 active-play timing. `GameModel` now accumulates elapsed
  milliseconds across idempotent pause/resume transitions and retains elapsed
  time when a puzzle is solved or restored from an existing save.
- Android pauses the model timer while the game menu, Quick Reminder, Assist,
  Solver Tools, and solver dialogs are open; while navigation is outside the
  Game screen; and while the Activity is backgrounded. The timer resumes only
  when the Game screen is interactive.
- Added four deterministic core timer tests and five connected Android timing
  flows covering game-menu, nested-dialog, Assist, Settings, and
  background/resume behavior.
- `verify.bat` passed. The final connected suite passed 49/49 tests with no
  failures, errors, or skips on both Pixel_7 (Android 15, 1080x2400) and
  `small_phone` (Android 16 / API 36.1, 720x1280). Manual small-phone review
  confirmed the full game menu remains visible and a five-second menu stay does
  not contribute to the displayed play timer.
- Completed Stage 2 difficulty selection. The shared `PuzzleDifficulty` enum
  defines Relaxed, Classic, and Challenge scramble budgets with stable IDs;
  `GameModel` supports deterministic seeded scrambles while preserving the
  original random Classic API and solvability-by-valid-moves contract.
- Android now asks for difficulty after board size, retains it in saves,
  Continue metadata, game/results state, and the last-selection preference, and
  scopes best records by size and difficulty. Legacy saves and size-only records
  remain available as Classic.
- The first connected UI attempt exposed an Android `AlertDialog` limitation:
  combining a message and single-choice list hid the choices on both AVDs. The
  explanation moved to Mode Select and a single tap on a difficulty now starts
  the game. Manual 720x1280 review also shortened the game title so
  `4x4 · Challenge` remains on one line.
- Added three shared difficulty tests, expanded save round-trip assertions, and
  added Android coverage for difficulty preferences, scoped records, Continue
  restoration, localized selection, and Activity result restoration.
- Final Stage 2 evidence: 32 shared tests and `verify.bat` passed; the connected
  suite passed 51/51 with zero failures, errors, or skips on Pixel_7 (Android 15,
  1080x2400; 337.606 seconds) and `small_phone` (Android 16 / API 36.1,
  720x1280; 295.657 seconds). Manual small-phone inspection found no clipping or
  overlap, and both final runtime error filters were empty.
- Completed Stage 3 exact-puzzle replay. Results now offers Replay Puzzle and
  restores the saved `initialGrid`, difficulty, and size while resetting moves,
  elapsed time, assisted state, and pending result state. It does not generate a
  new scramble.
- Results recreation now reloads the completed saved model before rendering, so
  rotating or recreating Results retains the exact starting board needed by
  Replay Puzzle. English, Traditional Chinese, and Japanese labels are covered.
- Added a deterministic shared restart-after-win contract test and two connected
  replay flows covering exact board identity, zeroed run state, and Results
  rotation. The UI harness now waits for board animation completion, treats a
  completed transition away from the board as idle, waits for portrait recovery,
  and scrolls compact-screen dialog content before interaction.
- Final Stage 3 evidence: all 33 shared tests and `verify.bat` passed. All 52
  Android tests passed on both Pixel_7 and `small_phone` in five isolated batches
  (12 + 10 + 10 + 10 + 10) with zero failures or skips. The final debug APK hash
  is `5722E65AF47727B2E270E564057D0340D3C376AAB2E7A27888929173A407BEB9`;
  installed-app visual review found no clipping, and both runtime error filters
  were empty.
- Completed Stage 4 per-size saves. `AndroidGameStore` now keeps independent
  3x3, 4x4, and 5x5 payloads, exposes ordered metadata for Home, scopes in-game
  Load to the current board size, and reloads the result size during Results
  recreation.
- Home continues a sole save directly and opens a compact size chooser when
  several saves exist. The chooser shows localized size, difficulty, state,
  moves, and time. Reset Saved Games clears all three slots while leaving
  records and settings intact.
- Legacy single-save fields migrate lazily into their matching size slot and
  are removed after migration. An older legacy payload cannot overwrite a newer
  matching slot.
- Added store coverage for all three independent slots, default/latest loading,
  migration and precedence, metadata ordering, and full reset. Added connected
  flows for multiple-save selection, English/Traditional Chinese/Japanese copy,
  and reset-all behavior.
- Final Stage 4 evidence: all 33 shared tests and `verify.bat` passed. All 58
  Android tests passed on Pixel_7 and `small_phone` in five isolated batches
  (16 + 10 + 12 + 10 + 10) with zero failures or skips. Manual 720x1280 review
  found no clipping or overlap in Home or the two-save chooser, both
  `AndroidRuntime:E` filters were empty, and the final debug APK hash is
  `56C5B37A1C6E17F9E3EF57B3985E53627FF1D7EE528467D18CC4694CC4587518`.

### 2026-08-19

- Audited the Android screen builders, all user-visible resources,
  `AndroidGameStore` persistence schema, and connected-test seams before
  implementation, then created `android/REGRESSION_TEST_CHECKLIST.md` as the
  initial English and Traditional Chinese acceptance baseline.
- Added `AndroidAppLocale`, which applies the stored app language from
  `MainActivity.attachBaseContext`. English is an explicit default independent
  of device language; Settings can switch between English and Traditional
  Chinese, and `AndroidGameStore` persists the selected tag.
- Added a complete `values-zh-rTW` resource set. All 182 string/plural keys and
  their format placeholders match the base English resources, and the Android
  Java source scan found no user-visible hard-coded copy.
- Added test-first persistence and end-to-end localization coverage for default
  English, unsupported-language fallback, language switching, activity
  recreation, relaunch, active-game preservation, Traditional Chinese major
  screens/dialogs, BFS warning safety, player Results, and Records.
- Ran the final 41-test connected suite on Pixel_7 (Android 15, 1080x2400) and
  `small_phone` (Android 16 / API 36.1, 720x1280): both passed with zero
  failures and zero skips.
- Reviewed CLI-captured English onboarding and Traditional Chinese Home,
  Settings, Mode Select, and Game screens on the 720x1280 AVD. Shortened the
  localized Restart control to `重來` after the first capture exposed a wrapped
  label, then confirmed all compact game controls stayed on one line. Relaunch
  retained language/save state and the inspected app process had no
  `AndroidRuntime` error.
- Extended `AndroidAppLocale` with `ja-JP` and added a complete
  `values-ja-rJP` resource set. English, Traditional Chinese, and Japanese now
  expose the same 183 string/plural keys with compatible format placeholders.
- Added Japanese instrumentation coverage for locale persistence, active-game
  preservation, relaunch, major screens and dialogs, board/control
  accessibility text, solver warnings, player Results, and Records.
- Ran the final no-device verification and the 44-test connected suite on both
  Pixel_7 (Android 15, 1080x2400) and `small_phone` (Android 16 / API 36.1,
  720x1280). Both connected runs completed with zero failures and zero skips.
- Reviewed Japanese Home, Settings, Mode Select, How to Play, and Game captures
  on the compact AVD. The first capture exposed wrapped Home Tutorial, Restart,
  and Assist labels; shortening them to `チュートリアル`, `再挑戦`, and `ヒント`
  restored single-line compact controls. The final debug APK remained installed
  and foregrounded in Japanese on the visible Pixel_7 AVD.

### 2026-08-10

- Created a second AVD with the Android CLI: `small_phone`, Android 16 / API
  36.1, 720x1280 at 320 dpi and 1024 MiB RAM. Kept the existing Pixel_7 Android
  15 / 1080x2400 AVD as the larger-phone regression target.
- Added icon-plus-text player actions across Home, persistent game controls, and
  Results. Compact Home/Menu controls are forced to one line on the small-phone
  profile.
- Added an outlined empty-cell marker and a zero-move first-interaction prompt,
  while leaving movement and save/record behavior in the existing model paths.
- Split Assist into Show Movable Tiles and a second-level Solver Tools surface.
  The advanced surface explains that assisted results never replace player
  records before offering BFS, A*, and IDA*.
- Added a short Results completion-mark settle animation and an immediate
  Reduced motion path, with connected coverage for the completion view and
  animation bypass.
- Ran the complete connected suite on both AVDs: all 36 tests passed on
  `small_phone` and all 36 passed on `Pixel_7`, with no failures or skips.
- Reviewed CLI-captured onboarding, Home, Mode Select, Game, Assist, Solver
  Tools, and assisted Results screens. A 23-move BFS solution reached Results
  without creating a player record; both phone layouts remained readable and
  unclipped after the compact-control correction.
- Sampled five warm Home-to-Mode-to-Home cycles on `small_phone`: 345 frames,
  13 janky frames (3.77%), 0 missed-vsync events, 0 slow UI-thread frames, and
  no `AndroidRuntime:E` entry. This is an emulator diagnostic, not a physical
  device performance guarantee.
- Left real Play upload signing, remote creation, and push intentionally
  deferred. The repository milestone is a verified local push-ready commit.

### 2026-08-09

- Added `AndroidMotion` and routed all nine top-level Android destinations
  through a short exit plus staggered entrance sequence. The transition locks
  outgoing interactions until `MainActivity` installs the destination view.
- Extended Reduced motion from board-only movement to both board movement and
  screen transitions. Added instrumentation coverage for the normal deferred
  transition and the synchronous Reduced motion path.
- Refined the Android visual hierarchy with grouped Home actions, a recommended
  Mode Select card, quiet status surfaces, destructive Settings grouping,
  press ripples, responsive menu content width, and a structured Results card.
- Ran `verify-connected.bat` with `ANDROID_SERIAL=emulator-5554`; the Pixel_7
  AVD on Android 15 passed all 35 tests with 0 failures, errors, or skips. The
  final XML from that run reported 143.205 seconds of test time.
- Recorded and reviewed the onboarding-to-Home transition frame by frame. A
  manual 3x3 BFS flow found a 19-move solution, animated it through Results,
  preserved the assisted-record rule, and produced no Android runtime error.
- Sampled five warm Home-to-Mode-to-Home cycles on the Pixel_7 AVD after
  grouping header motion and removing decorative scale transforms: 355 frames,
  15 janky frames (4.23%), 0 missed-vsync events, and one slow UI-thread frame.
  This is an emulator diagnostic, not a cross-device performance guarantee.
- Added `benchmark.bat` and a dependency-free solver benchmark that validates a
  fixed optimal 31-move 3x3 workload while reporting median latency and
  current-thread allocation.
- Measured BFS at 198.480 ms and 191.257 MiB allocated before optimization,
  using the median of three independent JVM-process medians.
- Replaced BFS 3x3/4x4 grid copies and string visited keys with a packed `long`
  board, primitive open-addressed visited set, `ArrayDeque` frontier, and cached
  direction array. Kept the array-based 5x5 compatibility path.
- Repeated the same benchmark after optimization: BFS measured 15.121 ms and
  13.875 MiB allocated, reducing median latency by 92.4% and allocation by
  92.7% while retaining the 31-move optimal solution.
- Added focused BFS regression coverage for the canonical hardest 3x3 board,
  input immutability, the packed 4x4 tile-value boundary, and 5x5 fallback.
- Re-ran `verify-connected.bat` on the Pixel_7 AVD running Android 15 after the
  BFS optimization. All 32 instrumentation tests passed with no failures or
  skips; the retained final XML reports 118.391 seconds of test time.
- Completed a clean-install manual emulator smoke flow covering first-run
  onboarding, Home, all three mode choices, a non-adjacent whole-line move,
  Undo restoration, an assisted 3x3 BFS solve through Results, the 4x4 BFS
  warning, and background/resume state preservation. The smoke log contained
  no app fatal exception or ANR.
- The clean launch reported 575 ms and hot resume reported 48 ms on this AVD.
  These are single observations, not startup performance baselines. Android
  frame timing, battery use, and whole-app memory remain unmeasured.

### 2026-06-18

- Hardened Android connected-test verification after the slow AVD exposed
  generated report/output directory locks. `android/build-debug.bat` now clears
  connected test additional output, connected test results, and connected test
  reports before Gradle runs.
- Updated `MainActivityFlowTest.navigateHomeToModeSelectToGame` so the Mode
  Select guidance regression is verified through the Activity view hierarchy for
  stable IDs, text, and content descriptions. This avoids false failures when
  UiAutomator does not expose non-essential container nodes on a busy emulator.
- Re-ran the targeted Mode Select connected instrumentation test on
  `emulator-5554`; the flow now passes.
- Re-ran the full connected instrumentation suite on `emulator-5554`; all 32
  tests passed after replacing fragile root-container waits with visible board
  or Activity hierarchy checks where appropriate.
- Re-ran `ci.bat`; the no-device verification and release readiness gate passed
  after the connected-test hardening work.
- Locked the first beta Android telemetry posture as local-only: no analytics,
  crash reporting, telemetry, ads SDKs, accounts, cloud save, or third-party
  tracking. The Play Store readiness check now verifies the decision text and
  local-only store claim before release handoff.
- Hardened the Android rotation instrumentation check so it refreshes the
  resumed `MainActivity` after configuration changes before asserting the game
  board. A targeted rerun of `rotationKeepsCurrentGameScreen` passed, followed
  by the full `verify-connected.bat` suite passing all 32 tests on
  `emulator-5554`.
- Added desktop public beta readiness tracking and a release gate for the
  desktop package handoff. `verify-release.bat` now runs both Android Play Store
  readiness and desktop public beta readiness checks.
- Expanded Android Play Store readiness with screenshot review, accessibility
  review, and pre-launch evidence worksheets so the remaining store work can be
  recorded consistently after manual review.
- Expanded the generated desktop package README so public beta testers get
  runtime requirements, smoke-test prompts, save-location notes, and known
  limits inside the ZIP itself.
- Expanded Android screenshot smoke manifests with screenshot purpose labels and
  manual review checklist placeholders for Play Console handoff.

### 2026-06-17

- Continued Android-first architecture work by extracting screen state
  serialization to `AndroidActivityState`, back-navigation decisions to
  `AndroidNavigation`, shared view primitives to `AndroidUi`, and onboarding /
  How to Play learning cards to `AndroidLearningContent`.
- Moved `Screen`, `ScreenLayout`, `PendingWin`, `GameResult`,
  `SettingChangeListener`, and `ViewParentRemover` out of `MainActivity` as
  package-private Android helpers.
- Reduced `MainActivity` from 1863 lines to roughly 1320 lines while preserving
  existing resource IDs, navigation flows, and `GameModel` as the only puzzle
  rule source.
- Added instrumentation regression coverage for Activity state round-trips and
  back-navigation decisions.
- Surfaced saved-game metadata on Android Home under Continue, including active
  / solved state, puzzle size, moves, elapsed seconds, and save age. Added
  instrumentation coverage for current metadata and legacy save fallback text.
- Continued screen-builder extraction by moving Android Home construction and
  Continue metadata formatting into `AndroidHomeScreen`, leaving `MainActivity`
  to coordinate state reset and navigation callbacks.
- Moved Android Game and Practice Tutorial view construction into
  `AndroidGameScreen` and `AndroidTutorialScreen`. `MainActivity` now keeps the
  lifecycle, `GameModel` updates, tutorial highlights, and command gates, while
  the new builders own view hierarchy, stable IDs, and presentation callbacks.
- Moved Android Mode Select, Records, Settings, and Results view construction
  into `AndroidModeSelectScreen`, `AndroidRecordsScreen`,
  `AndroidSettingsScreen`, and `AndroidResultsScreen`. The activity still owns
  navigation decisions, persistence writes, settings application, and record
  comparison text.
- Added Android adaptive and round launcher icon resources, switched the
  manifest icon to the adaptive `@mipmap/ic_launcher`, and documented Play
  Store readiness in `android/PLAY_STORE_READINESS.md` with listing copy,
  privacy policy draft, Data Safety draft, screenshot guidance, and a
  pre-launch test matrix.
- Added `android/check-play-store-readiness.bat` and wired it into
  `verify-release.bat` after Android and desktop release artifacts are built.
  The check validates repo-side Play Store drafts, adaptive icon resources,
  Data Safety assumptions, release artifacts, screenshot workflow, and that
  release signing secrets are not tracked.
- Added `android/store-assets/` with a version-controlled Play Store feature
  graphic source and store asset notes, then extended the Play Store readiness
  check to require those source assets.
- Added `android/export-store-assets.bat` and `tools/StoreAssetExporter.java`
  so `verify-release.bat` generates the Play Store feature graphic PNG under
  `dist/store-assets/android/<version>/` before running readiness checks.
- Hardened `MainActivityFlowTest` launch and scrolling helpers after slow AVD
  runs exposed false failures. The connected suite now waits on the foreground
  app window and uses a swipe fallback when UiAutomator does not expose a
  scrollable container.
- Hardened `android/build-debug.bat` against Windows file-lock false failures by
  clearing common generated resource/package and javac class output directories
  before Gradle builds.
- Improved Android Mode Select release polish by adding expected session length,
  first-puzzle guidance, richer card content descriptions, and stable text IDs
  for instrumentation coverage.

### 2026-06-16

- Added explicit save metadata across desktop JSON saves and Android
  `SharedPreferences` saves: `updatedAt`, size, moves, elapsed time, active
  state, and solved state.
- Preserved compatibility with older desktop root files and older Android saves
  that do not contain the new metadata fields.
- Moved default desktop save and records storage out of the project root into
  the user-data directory, with `slidedo.data.dir` as an override for tests and
  portable beta packages.
- Added root JUnit and Android instrumentation coverage for save metadata
  round-trips.
- Hardened `verify-connected.bat` so it clears stale connected-test output
  directories before running and can recover from Windows report-generation
  access issues when instrumentation itself reports success.
- Added shared release version metadata in `version.properties`, Android
  release signing configuration, `android/build-release.bat`, and
  `verify-release.bat` so signed release APK/AAB artifacts can be generated
  repeatably. The local fallback signing key is for pipeline verification only.
- Added desktop release packaging through `package-desktop.bat`, producing a
  ZIP package and, when `jpackage` is available, a Windows app-image.
- Added version-matched release notes under `release-notes/` and included the
  notes in desktop packages.
- Added `android/screenshot-smoke.bat` as a repeatable manual capture workflow
  for Home, Mode Select, Game, How to Play, Settings, Records, and
  Results/current screen.
- Added `android/check-screenshot-set.bat`; screenshot smoke now validates the
  expected PNG files and writes a manifest next to the captured screenshots.
- Added `ci.bat` and `.github/workflows/ci.yml` so the no-device local/remote
  CI gate runs verification plus release readiness checks with artifact upload.
- Added release artifact manifest generation so `verify-release.bat` writes
  `dist/release-manifests/<version>.txt` with SHA-256 hashes for the Android,
  desktop, store asset, and release-note handoff files.
- Clarified Android and desktop Records copy so both surfaces explain that only
  player solves count, fewer moves rank first, ties use faster time, and
  assist/solver completions do not replace records.
- Hardened Android instrumentation app launch retries after a slow AVD exposed
  `startActivitySync` idle timeouts unrelated to app behavior.

### 2026-06-11

- Replaced the desktop generic solved dialog with an Android-style Results
  dialog that preserves animation timing, player best-record updates, and
  solver-assisted no-record behavior.
- Added desktop Results copy tests for first record, new best, unchanged best,
  and assisted completion cases.
- Added the desktop Home/start surface for the Swing edition. Desktop now opens
  on Home instead of directly entering the board, with entries for 3x3, 4x4,
  5x5, Continue/Load, How to Play, Practice Tutorial, Records, and Preferences.
- Added a desktop Records dialog for 3x3, 4x4, and 5x5 best records, including
  copy that preserves solver-assisted record protection.
- Added a desktop Preferences dialog with a reduced-motion option that snaps
  tile movement without changing puzzle rules, move counts, save data, or
  records.
- Added focused tests for desktop home Records and Preferences copy.

### 2026-06-10

- Started the Desktop/Mobile Parity Pass by adding desktop Assist movable-tile
  highlights plus How to Play and Practice Tutorial menu dialogs. The highlight
  path is view-only and still executes moves exclusively through
  `GameModel.slideLineTo(row, col)`.
- Added focused desktop help-content tests and updated roadmap state for the
  remaining parity surfaces.
- Completed a post-architecture code review of `AndroidGameStore`,
  `MainActivity` persistence call sites, `verify-connected.bat`, and the current
  desktop/mobile parity roadmap. No blocking defects were found.
- Added small clarifying comments for Android save compatibility and record
  ordering so future changes do not accidentally break restart-grid fallback or
  best-record comparison semantics.
- The latest Android Studio emulator connected test flow on `emulator-5554`
  passed 26/26 instrumentation tests, then the debug app was reinstalled and
  left running on the emulator for manual testing.
- Replanned the next implementation phase around desktop/mobile parity before
  daily puzzle, progression, achievement, sound/theme, or broader release
  readiness work.

### 2026-06-09

- Added the Android accessibility MVP for custom board content: `KlotskiView`
  now exposes board size, empty-cell position, row-by-row tile state, active
  highlighted movable tiles, and busy state through its content description.
- Added content descriptions for primary in-game controls and settings switches,
  plus a polite live region for gameplay status changes.
- Extended Android instrumentation coverage for board/control accessibility
  descriptions, settings switch descriptions, and Assist hint description
  updates.
- Updated README and Android README status notes after the accessibility pass.
- Split Android app-state persistence out of `MainActivity` into
  `AndroidGameStore`, covering saves, records, settings, onboarding state, and
  the last selected puzzle size while keeping gameplay rules in `GameModel`.
- Added focused instrumentation coverage for `AndroidGameStore` save round-trip,
  invalid save handling, record comparison, settings, onboarding, and last-size
  behavior.
- Expanded local CI verification so `verify.bat` also assembles the Android
  instrumentation test APK, and added `verify-connected.bat` as the emulator /
  device runtime test entry point.

### 2026-05-26

- Implemented the Android Interactive Tutorial MVP as a Practice Tutorial screen
  reachable from Home and the final onboarding page.
- Added render-only board highlights in `KlotskiView` for movable aligned tiles
  and emphasized tutorial targets, while keeping move execution in
  `GameModel.slideLineTo(row, col)`.
- Added instrumentation coverage for the guided first move, whole-line slide
  lesson, completion status, and Start 3x3 handoff.
- Updated README and Android README to describe the Practice Tutorial flow and
  manual smoke-test expectations.
- Added a lightweight Assist hint that highlights movable aligned tiles without
  moving the board, changing move count, invoking solvers, or affecting records.
- Added instrumentation coverage for the Assist hint flow and updated docs after
  the hint MVP.

### 2026-05-25

- Consolidated development planning and desktop/Android feature reference into `DEVELOPMENT.md`.
- Removed the old `DESKTOP_FEATURE_SPEC.md` and `NEXT_STEPS.md` pointer files after consolidation.
- Recorded local Git workflow rule: initialize Git locally, commit cohesive changes, and do not push until explicitly requested.
- Planned the next product direction: a common casual-game app experience with Home, Mode Select, How to Play, Settings, Game, Pause, and Results surfaces.
- Implemented the Android product-style navigation pass: Home launch, Continue from save, Mode Select, How to Play, Records, compact in-game controls, Menu, and Assist.
- Kept `GameModel` unchanged for navigation work and added a `KlotskiView` busy-state callback so controls visibly disable during animation and solver work.
- Added product engineering and UI/UX engineering assessment notes to turn the Android roadmap into a product-planning document, including onboarding, Settings, Results, visual learning, technical improvements, and Google Play readiness planning.
- Added Android instrumentation smoke tests for Home, Mode Select, Continue,
  How to Play, whole-line move/undo, save/load persistence, and rotation.
- Added stable Android view IDs for key screens and controls, plus Activity
  screen-state restoration so rotation preserves the current game screen.
- Expanded the next-stage roadmap with recommended implementation order,
  onboarding MVP acceptance criteria, visual tutorial scope, Settings MVP,
  Results screen scope, and CI/release-readiness direction.
- Implemented the First-Run Onboarding MVP with a four-page beginner flow,
  persistent Skip/Start 3x3 state, Home re-entry, and instrumentation coverage.
- Replaced the text-only How to Play surface with visual 3x3 teaching examples
  for the goal, tap movement, and whole-line slides, plus a compact in-game
  Quick Reminder and instrumentation coverage.
- Implemented the Settings MVP with Home/Menu entry points, haptic and
  reduced-motion toggles, reset-save/reset-records confirmation flows, and
  instrumentation coverage.
- Replaced the solved-game dialog with a Results screen that shows player
  record status, assisted-completion wording, and Play Again/New Size/Home
  actions.
- Added `verify.bat` as the local CI entry point for shared tests, desktop
  compile, Android assemble/lint, and public Javadocs.
- Refreshed README, Android README, and development planning notes after the
  Android onboarding/settings/results MVP pass.
- Removed local verification warning noise by modernizing Android Gradle DSL
  assignments, enabling native access for Gradle wrapper JVMs, and treating
  Android Java deprecation warnings explicitly.

### 2026-05-24

- Added root Gradle/JUnit test setup for shared core behavior.
- Added tests for `GameModel`, `SaveManager`, and solver sanity behavior.
- Aligned Android whole-line slide behavior with desktop semantics.
- Added Android Save/Load and solver controls.
- Added English API comments and updated documentation.
