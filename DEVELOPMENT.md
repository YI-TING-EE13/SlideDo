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

- Latest verified implementation baseline before this accessibility pass:
  `15af6ac Add Android assist hint MVP`.
- This handoff guide is committed on top of that baseline; use `git log` to
  confirm the current `HEAD` in a future session.
- Working tree was clean after the handoff commit.
- `DEVELOPMENT.md` is now the primary continuity artifact; older planning notes
  were consolidated here.
- The Android app has completed the MVP pass for Home, Mode Select, onboarding,
  interactive Practice Tutorial, visual How to Play, Settings, Results, local
  records, save/load, lightweight Assist hints, board/control accessibility
  descriptions, and connected instrumentation coverage.
- The latest local verification pass was warning-clean for the previously noisy
  Gradle DSL deprecation, Java native-access warning, and Android Java
  deprecation note.

Start a new implementation session by checking:

```bat
git status --short
git log --oneline --decorate -5
```

Then run the smallest relevant verification for the planned change. For a broad
Android or documentation-sensitive change, use:

```bat
verify.bat
cd android
build-debug.bat :app:connectedDebugAndroidTest --warning-mode all --console plain
```

Recommended next product task after the Android accessibility MVP:

1. Add daily puzzle, progression loops, and achievement systems.
2. Add stronger completion feedback, such as celebration and record emphasis.
3. Add sound/theme settings after those systems exist.
4. Continue the broader accessibility pass with touch-target, color-contrast,
   TalkBack, and reduced-motion validation.

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
- First-run onboarding before normal play, with Skip and Start 3x3 actions.
- Interactive Practice Tutorial entry from Home and onboarding, using a guided
  first move plus a whole-line slide lesson.
- Continue when a valid save exists.
- Mode Select for 3x3, 4x4, and 5x5 games.
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
- Assist now starts with a lightweight movable-tile hint before solver actions.
- Board-level screen-reader summaries for game/tutorial board state, highlighted
  movable tiles, and primary game/settings controls.
- Manual Save/Load plus autosave through the in-game menu.
- Best records by puzzle size.
- BFS, A*, and IDA* solver controls behind Assist with expensive-operation warnings.
- Solver-assisted completion protection so player records are not overwritten.
- Settings for haptic feedback, reduced motion, reset saved game, and reset records.
- Results screen with player-record status and solver-assisted completion wording.
- Haptic feedback.
- Warning-clean local Gradle verification under `--warning-mode all` for the
  root tests and Android assemble/lint flow.

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
- Restart restores the initial post-scramble or loaded grid and clears undo history.
- Loading restores size, current grid, initial grid, move count, and elapsed milliseconds.
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
- [x] Records are separated by puzzle size.
- [x] Solver controls run off the UI thread and warn for expensive board sizes.

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
build-debug.bat :app:assembleDebug :app:lintDebug
```

Android instrumentation tests:

```bat
cd android
build-debug.bat :app:connectedDebugAndroidTest
```

Warning-clean Android instrumentation check:

```bat
cd android
build-debug.bat :app:connectedDebugAndroidTest --warning-mode all --console plain
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
   - Offer Play Again, New Size, and Home.
   - Keep solver-assisted completions visually distinct and do not record them as player bests.

### UX Engineering Direction

Current implementation approach:

- Keep one Android `Activity` for now with internal screen state for `HOME`,
  `ONBOARDING`, `TUTORIAL`, `MODE_SELECT`, `HOW_TO_PLAY`, `RECORDS`,
  `SETTINGS`, `RESULTS`, and `GAME`.
- Build separate private view-construction methods in `MainActivity` or small package-private screen classes before introducing a larger architecture.
- Keep `GameModel` unchanged for navigation work; the model should remain platform-independent.
- Keep `KlotskiView` focused on board rendering and gestures only.
- Store lightweight preferences for first-run state, settings, and last selected mode.
- Continue to use `SharedPreferences` for local mobile saves until there is a concrete need for a database.

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
- Home is understandable and appropriately simple. Continue, New Game, Beginner
  Guide, Practice Tutorial, How to Play, Settings, and Records are clear.
  Records still needs richer explanation for how records are earned and why
  assisted wins are excluded.
- Mode Select communicates size and rough difficulty, but it does not yet show
  expected session length, scramble difficulty, or recommended first choice for
  new players.
- How to Play now includes small static board examples for the solved goal,
  adjacent tap moves, and whole-line slides. It still needs richer visual
  examples for swipe, undo, restart, and solver-assisted completion rules.
- The in-game hierarchy is now much closer to a product app: board first,
  progress visible, Undo/Restart available, secondary actions behind Menu and
  Assist. The next UX risk is that all controls are still text buttons and the
  screen has limited visual personality.
- Navigation is reasonable for the current one-Activity architecture. Back
  behavior returns from Game to Home and from informational screens to the right
  context. Longer-term, the app needs a clearer navigation model before adding
  Daily Puzzle, progression, and deeper Stats surfaces.
- Current persistence is adequate for a simple casual game: one autosave, manual
  Save/Load, last selected mode, and per-size records. It is not yet a polished
  product save system because it lacks explicit resume metadata, multiple
  puzzle slots, save freshness display, and cloud/back-up strategy.
- The app does not yet have audio, themes, crash reporting, analytics, Play
  Store metadata, privacy disclosures, or release signing planning.
- As of this pass, SlideDo is no longer just a raw feature demo, but it is not
  yet a complete mobile game product. The core gameplay works; the missing work
  is mostly onboarding, polish, product systems, release readiness, and
  repeat-play motivation.

### Product Readiness Gaps

- First-run guided path is MVP-level: the new Practice Tutorial covers a first
  move and whole-line slide, but it is still not a full multi-step coached
  first game.
- Settings are MVP-level: haptic feedback, reduced motion, reset save, and reset
  records exist, but sound and theme controls still wait on those systems.
- Results are MVP-level: the post-game screen shows record status and next
  actions, but there is no celebration animation, sharing, or progression hook.
- Missing complete feedback system: no sound, celebratory animation, progression
  feedback, or differentiated assisted-completion treatment beyond Results text.
- Hint system is MVP-level: Assist can now highlight movable tiles without
  moving the board, but it does not yet suggest strategic progress toward a
  solve.
- Missing progression loops: no daily puzzle, streak, recent games, difficulty
  progression, achievements, or session goals.
- Accessibility is MVP-level: board summaries, settings switch descriptions, and
  primary game-control descriptions exist, but the app still needs a manual
  TalkBack pass, larger touch-target review, color-contrast review, and broader
  reduced-motion validation.
- Missing Play Store readiness systems: release signing, app icon polish,
  adaptive icons, screenshots, privacy policy, data safety notes, crash
  reporting, and versioning discipline.

## Roadmap And Planning

### Recommended Next Implementation Order

The next product milestone can move from first-time comprehension into
repeat-play motivation and polish. Recommended order:

1. Daily puzzle, progression loops, and achievement systems.
2. Stronger completion feedback, such as celebration and record emphasis.
3. Sound/theme settings after those systems exist.
4. Broader accessibility review with TalkBack, touch-target, color-contrast, and
   reduced-motion validation.

Rationale:

- The First-Run Onboarding, Practice Tutorial, lightweight Assist hint, Visual
  How to Play, Settings, Results, board/control accessibility descriptions, and
  local CI MVPs now cover the basic teaching, preference, completion, and
  quality paths.
- Remaining items are broader product systems rather than prerequisite app
  structure.

### Completed 2026-05-25 MVP Items

### 1. Add Android Instrumentation Tests

Priority: High

Status: Completed on 2026-05-25.

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
- Add sound effects toggle after audio exists.
- Add theme selection after at least one alternate theme exists.
- [x] Add reduced-motion preference before adding heavier animations.
- [x] Add reset local data actions for save and records, behind confirmation.

Recommended MVP scope:

- [x] Add Settings entry from Home and in-game Menu.
- [x] Store haptic feedback and reduced-motion preferences in `SharedPreferences`.
- [x] Apply haptic toggle to existing board/control feedback.
- [x] Add reset save and reset records actions behind confirmation dialogs.
- Defer sound and theme controls until those systems exist.

### 7. Add Results Screen

Priority: High

Status: Completed on 2026-05-25.

- [x] Replace the solved-game dialog with a full Results screen.
- [x] Show size, moves, time, previous best, and whether the player set a new best.
- [x] Show solver-assisted completions with distinct wording and no record write.
- [x] Offer Play Again, New Size, Home, and optionally Share later.

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
- [x] Run public Javadoc doclint checks where practical.

Recommended scope:

- [x] Start with a local one-command verification script if GitHub Actions is not
  configured yet.
- [x] CI should run shared core tests, desktop compile, Android assemble/lint, and
  API doclint.
- Connected Android instrumentation tests can remain manual at first unless a
  stable emulator runner is available in CI.
- Add release-readiness checks later: signed release build, versioning, and Play
  App Bundle generation.

### UX Improvement Directions

- Use icon-plus-text buttons for Home, Menu, Assist, Undo, Restart, Save, Load,
  and Settings once the app has a stable visual language.
- Strengthen Home as a game entry screen with a small playable board preview or
  animated tile motif instead of only text and buttons.
- Make Mode Select more informative: show best record, difficulty, estimated
  solve length, and recommended first mode.
- Make in-game controls feel compact and native: keep primary actions visible,
  move rare actions behind Menu, and avoid exposing solver terminology too early.
- Separate Assist from Solver. Assist should first offer one-step hints or
  highlight movable lines; full solver playback can remain in an advanced Tools
  area.
- Add stronger completion feedback: board settle animation, short celebration,
  record badge, and clear next action.
- Add clear empty-cell affordance and movable-tile hints for first-time players.
- Review typography and spacing on smaller emulator profiles, not only
  1080 x 2400.
- Continue accessibility review for custom board content with TalkBack, larger
  touch targets, contrast checks, and reduced-motion behavior.
- Add orientation and background/resume validation whenever screen state expands.

### Technical Improvement Directions

- Keep `GameModel` platform-independent and continue to route all puzzle-rule
  changes through shared core tests.
- The current one-Activity screen-state architecture is acceptable for the MVP,
  but `MainActivity` should be split into package-private screen builders or a
  small navigation controller before adding larger product systems.
- Add stable identifiers or test hooks for important Android controls so
  instrumentation tests do not rely on text or screen coordinates.
- Add a small Android state model for screen state, selected mode, first-run
  status, settings, and pending results.
- Move Android persistence helpers out of `MainActivity` as save, settings,
  records, results, and onboarding state continue to grow.
- Add explicit save metadata: saved size, moves, elapsed time, updated-at
  timestamp, and solved/active status for better Continue UI.
- Add release build checks before Google Play planning: signed release APK/AAB,
  minification decision, versionCode/versionName policy, and reproducible build
  commands.
- Add lint and instrumentation checks to CI before expanding visual complexity.
- Add screenshot-based smoke tests for Home, Mode Select, Game, How to Play,
  Settings, Results, and Records.

### Mid- And Long-Term Planning

Priority: Low to Medium

- Sound effects with mute toggle.
- Theme selection.
- Daily puzzle.
- Difficulty presets based on scramble depth.
- Lightweight hint system separate from full solver playback.
- Completion celebration.
- Accessibility labels and larger touch targets.
- Recent games or session history.
- Achievements for first solve, low-move solve, fast solve, and daily streak.
- Optional cloud backup only after local data and privacy expectations are clear.
- Tablet and foldable layouts after phone layout stabilizes.
- Localization after English UI copy and layout constraints are stable.

### Google Play Readiness Planning

- Prepare adaptive launcher icon, feature graphic, screenshots, and short/long
  store descriptions that show the actual game UI.
- Create a privacy policy before adding analytics, crash reporting, cloud save,
  ads, or account features.
- Define Data Safety answers based on actual collection behavior; keep local-only
  gameplay data local unless there is a clear product reason to sync it.
- Add crash reporting and basic performance telemetry only after privacy wording
  and user consent expectations are settled.
- Add release signing, Play App Bundle generation, versioning, and release notes
  workflow.
- Run pre-launch checks on multiple device sizes, API levels, rotations, and
  background/resume flows.
- Add accessibility review before store submission, including screen reader
  labels for the board and settings for reduced motion/audio.
- Decide whether solver features are player-facing, debug-only, or advanced
  tools before presenting screenshots or store copy.

## Development Log

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
