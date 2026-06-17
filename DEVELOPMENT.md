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

- Latest verified implementation baseline before this code-review/planning pass:
  `6867971 Extract Android game store and expand verification`.
- This handoff guide is committed on top of that baseline; use `git log` to
  confirm the current `HEAD` in a future session.
- Working tree was clean after the handoff commit.
- `DEVELOPMENT.md` is now the primary continuity artifact; older planning notes
  were consolidated here.
- The Android app has completed the MVP pass for Home, Mode Select, onboarding,
  interactive Practice Tutorial, visual How to Play, Settings, Results, local
  records, save/load, lightweight Assist hints, board/control accessibility
  descriptions, `AndroidGameStore` persistence separation, and connected
  instrumentation coverage.
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
verify-connected.bat
```

Recommended next product task after the code-review/planning pass:

1. Start a desktop/mobile parity pass so the desktop edition has player-facing
   content and flow aligned with the Android app.
2. Split larger Android screen construction into package-private builders only
   where it directly supports the parity work.
3. Add daily puzzle, progression loops, and achievement systems after both
   front ends have comparable product surfaces.
4. Add stronger completion feedback, sound/theme systems, and broader
   accessibility validation after the parity pass.

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
- Android app-state persistence through `AndroidGameStore` for saves, records,
  settings, onboarding state, and the last selected puzzle size.
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
- Store lightweight preferences and saves through `AndroidGameStore`, which
  remains backed by `SharedPreferences` until there is a concrete need for a
  database.

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
  product save system because it still lacks multiple puzzle slots, save
  freshness display in the UI, and cloud/back-up strategy. Save payloads now
  include explicit updated-at, size, moves, elapsed, active, and solved
  metadata for future Continue and release diagnostics.
- The app does not yet have audio, themes, crash reporting, analytics, feature
  graphic assets, broad store screenshots, or a published privacy policy URL.
  Release signing injection, versioning, release notes, adaptive launcher icons,
  and Play Store readiness drafts now exist.
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
- Missing Play Store readiness systems: real upload-key handoff, final feature
  graphic, reviewed store screenshots, published privacy policy URL, manual
  accessibility sign-off, optional crash reporting/privacy update, and broad
  versioning discipline after the first beta cycle.

## Roadmap And Planning

### Recommended Next Implementation Order

The next product milestone should first align the desktop and Android player
experience before adding repeat-play systems. Recommended order:

1. Desktop/mobile parity pass.
2. Targeted architecture split that supports parity work.
3. Daily puzzle, progression loops, and achievement systems.
4. Stronger completion feedback, sound/theme systems, and broader accessibility
   validation.

Rationale:

- Android now has the richer product surface. Desktop still has strong gameplay
  coverage but lacks Android's Home, onboarding/tutorial, How to Play, Assist
  hint presentation, Settings, and Results surfaces.
- Aligning desktop first keeps both front ends honest around the shared
  `GameModel` contract before adding daily/progression systems that would need
  to exist in both experiences.

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
| Mode selection | Mode Select starts 3x3, 4x4, and 5x5 games and shows best record summaries. | Home/Game menu starts 3x3, 4x4, and 5x5 games and Records shows best summaries. | Presentation differs, available choices match. | Android mode-select instrumentation; desktop compile/manual smoke. |
| Learning surfaces | First-run onboarding, visual How to Play, Quick Reminder, and interactive Practice Tutorial. | How to Play and Practice Tutorial dialogs use Android-aligned language. | Android remains more visual and interactive; desktop parity covers the same concepts. | Android onboarding/tutorial/how-to instrumentation; desktop help-content tests. |
| Touch/mouse movement | Tap/swipe aligned tiles; whole-line slide counts as one move and one undo snapshot. | Mouse click/release movement plus keyboard controls; whole-line slide uses shared model. | Input method differs by platform, rule outcome matches. | Shared core tests, Android whole-line instrumentation, desktop smoke. |
| Assist / hints | Assist menu can highlight movable tiles and offer solver playback. | Assist menu highlights movable tiles and supports solver playback. | Solver UI differs; solver-assisted wins do not update records on both platforms. | Android assist/results instrumentation; desktop result-copy tests. |
| Save/load metadata | `AndroidGameStore` persists size, grid, initial grid, moves, elapsed, updated-at, active, solved, records, settings, and onboarding state. | Desktop JSON save persists size, grid, initial grid, moves, elapsed, updated-at, active, and solved; records live in user-data path. | Android includes mobile-only settings/onboarding; shared gameplay metadata is aligned. | Android store instrumentation, root save metadata tests. |
| Settings / preferences | Haptic feedback, reduced motion, reset saved game, and reset records. | Reduced motion preference plus desktop records/save flows. | Haptics are Android-only; desktop has no vibration setting by design. | Android settings instrumentation; desktop preferences copy tests/manual smoke. |
| Records | Per-size local best records, fewer moves then lower time, solver-assisted protection. | Per-size local best records with the same comparison and solver-assisted protection. | Aligned. | Android records/results instrumentation; desktop result and records tests. |
| Results | Full Results screen with Play Again, New Size, Home, record status, and assisted wording. | Android-style Results dialog with Play Again, New Size, Home, record status, and assisted wording. | Surface type differs due to Swing dialogs vs Android screens, wording and actions align. | Android results instrumentation; desktop results copy tests. |
| Accessibility | Board summaries, settings switch descriptions, and primary control descriptions exist. | Basic Swing labels/dialog text exist, but no full assistive-tech audit. | Both platforms still need broader manual accessibility review before public release. | Android accessibility instrumentation plus manual TalkBack/desktop review. |
| Packaging / release | Debug build, connected tests, signed APK/AAB, Play readiness file check, screenshot smoke workflow. | Desktop ZIP and optional app-image package with user-data paths. | Android still needs real Play upload key and Play Console external assets; desktop package is not a signed installer. | `verify.bat`, `verify-connected.bat`, `verify-release.bat`, manual screenshot smoke. |

Parity conclusion for current beta:

- Android and desktop now expose comparable core gameplay, learning, records,
  settings/preferences, assist hints, save/load, and completion flows.
- Remaining gaps are release and platform polish, not shared puzzle behavior:
  Android needs real Play upload signing, final store assets, privacy-policy URL,
  manual accessibility/pre-launch passes, and optional crash reporting decision;
  desktop needs signed installer planning only if the beta moves beyond ZIP /
  app-image distribution.
- Future repeat-play systems such as daily puzzle, achievements, progression,
  and richer stats should be scoped for both front ends before implementation.

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
- Connected Android instrumentation tests still require a running emulator or
  device outside the one-command no-device verification path.
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
- Add stable identifiers or test hooks for important Android controls so
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
- Lightweight hint system separate from full solver playback.
- Completion celebration.
- Accessibility labels and larger touch targets.
- Recent games or session history.
- Achievements for first solve, low-move solve, fast solve, and daily streak.
- Optional cloud backup only after local data and privacy expectations are clear.
- Tablet and foldable layouts after phone layout stabilizes.
- Localization after English UI copy and layout constraints are stable.

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
- Review the generated feature graphic upload file, then capture and review
  screenshots that show the actual game UI.
- Publish a privacy policy URL before store submission and before adding
  analytics, crash reporting, cloud save, ads, or account features.
- Keep Data Safety answers aligned with actual collection behavior; keep
  local-only gameplay data local unless there is a clear product reason to sync
  it.
- Add crash reporting and basic performance telemetry only after privacy wording
  and user consent expectations are settled.
- [x] Add release signing injection, Play App Bundle generation, versioning, and
  release notes workflow.
- Replace temporary local release verification signing with the real Play upload
  key before store submission.
- Run pre-launch checks on multiple device sizes, API levels, rotations, and
  background/resume flows.
- Add accessibility review before store submission, including screen reader
  labels for the board and settings for reduced motion/audio.
- Decide whether solver features are player-facing, debug-only, or advanced
  tools before presenting screenshots or store copy.

## Development Log

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
  clearing common generated resource/package output directories before Gradle
  builds.

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
