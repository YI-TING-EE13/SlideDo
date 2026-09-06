# SlideDo Desktop / Android Parity Qualification

Status: owner-approved qualification baseline with Stages 1-3 session,
persistence, records, and completion-accounting behavior implemented and
verified; later stages remain planning-only. This document records current
implementation evidence and bounded follow-up work.

Qualification date: 2026-09-07
Repository: YI-TING-EE13/SlideDo
Authoritative branch: main
Expected and observed origin/main: faedda2af2d59632fe7443fb12d190e4bd2fc0d1

## Authority, scope, and evidence rules

The shared GameModel remains the canonical, platform-independent gameplay
source. Android is the completed Personal Play reference for player-facing
flows and local progression. Desktop is the Swing implementation currently
being qualified. Parity means equivalent player-visible outcomes and data
contracts, not copied Android lifecycle or widget mechanics.

The matrix uses these status values:

- PARITY: the meaningful behavior is present on both platforms and the
  observable contract is aligned.
- PARTIAL: a meaningful slice exists on both platforms, but a material
  behavior, scope, or persistence contract differs.
- MISSING: Android has a meaningful capability and Desktop has no verified
  equivalent.
- PLATFORM-SPECIFIC: the Android mechanism is inherently platform-specific;
  the matrix records the desktop-equivalent requirement separately rather than
  treating implementation mechanics as a parity defect.

Claims below are based on the current source, tests, and reviewed documents.
The existence of a shared-core type alone is not treated as Desktop support.
Existing Android behavior remains protected: valid-move scrambles, whole-line
one-action history, active-only elapsed time, exact initial-grid replay,
namespace isolation, and solver-assisted record protection.

## Qualification snapshot

- The required fetch and identity checks completed before editing. HEAD and
  origin/main both resolved to faedda2af2d59632fe7443fb12d190e4bd2fc0d1.
- The only pre-existing worktree change was the unrelated untracked
  SlideDo_Project_Development_Record.html. It was preserved and is not part
  of this initiative.
- The starting local CI contract passed after an environment-permission
  retry. It covered shared tests, Desktop compilation, both Javadoc gates,
  Android build/lint, release checks, and package checks. The Android SDK XML
  version warning was non-fatal.
- Desktop current truth is MainFrame, BoardPanel, DesktopHelpContent,
  DesktopHomeContent, DesktopResultContent, and SaveManager. Android current
  truth is MainActivity, AndroidGameStore, the Android screen builders,
  KlotskiView, and AndroidPersonalDataArchive.

## Android to Desktop feature-parity matrix

### Gameplay, identity, and session state

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| G1 | Move rules and whole-line actions | GameModel.move and GameModel.slideLineTo; Direction describes empty-cell travel; MoveAction stores direction and steps. | MainActivity and KlotskiView route taps, swipes, accessibility clicks, and solver playback through GameModel. | MainFrame and BoardPanel route mouse and arrow-key input through the same model methods. | PARITY | Preserve valid moves, whole-row or column slides as one move and one history action, and identical undo/redo outcomes. Save histories remain in the shared compact format; no completion is inferred from a view. | GameModelTest, Android MainActivityFlowTest, and desktop smoke. Keep the invariant in AGENTS.md and DEVELOPMENT.md. Lowest parity risk; the main risk is accidental rule duplication in a future Swing change. |
| G2 | Supported board sizes 3x3, 4x4, and 5x5 | GameModel supports square sizes and the existing solvers use the same state shape. | Android Mode Select and MainActivity begin 3x3, 4x4, or 5x5 games. | MainFrame Home and Game menu start all three sizes; records also list all three. | PARITY | A size selection must create a valid solved-order scramble and retain size through save, restart, replay, and results. Size is part of every future record and puzzle identity key. | PuzzleDifficultyTest, GameModelTest, Android mode-select tests, desktop compile and manual three-size smoke. No migration beyond rejecting unsupported sizes. |
| G3 | Relaxed, Classic, and Challenge difficulty selection | PuzzleDifficulty supplies stable IDs, multipliers, and size-dependent scramble depth; GameModel has seeded overloads. | AndroidModeSelectScreen and MainActivity.beginNewGame show and persist the selected difficulty. | MainFrame now presents all three presets for every supported size, creates the selected preset through DesktopGameFactory, and displays it in status/results. Existing SaveManager state already carries the model difficulty. | PARITY | Difficulty changes scramble depth only and is preserved by restart, exact replay, and existing Desktop save/load. Legacy state without a difficulty still defaults to Classic; record-scope migration remains a later issue. | DesktopSessionContractTest covers all sizes/presets; PuzzleDifficultyTest and full core tests remain the contract. The later persistence/records stages still own namespace and record migration. |
| G4 | Deterministic puzzle identity and reproducible scramble | GameModel.scramble(PuzzleDifficulty, seed) and PuzzleIdentity hash size, difficulty, and exact initialGrid. DailyChallenge and favorites build on this contract. | MainActivity uses difficulty-aware games, daily seeds, PuzzleIdentity for favorites, and exact starting boards for replay. | DesktopGameFactory delegates both randomized and seeded creation to GameModel; MainFrame retains exact initialGrid through PuzzleIdentity replay. No second scramble algorithm or seed format was added. | PARITY | Equal size, difficulty, and seed produce equal starting grids through the shared overload; identity is derived from the immutable initialGrid, not current progress. Desktop does not expose a separate seed control. | DesktopSessionContractTest compares all presets with a shared-core seeded model; PuzzleIdentityTest remains authoritative. No migration can recover a historical random seed, but existing initialGrid remains usable. |
| G5 | Results surface and exact Replay Puzzle | GameModel.restartCurrentGame and PuzzleIdentity.createGame provide exact starting-board restoration. GameResult carries Android result context. | AndroidResultsScreen and MainActivity show Replay Puzzle, New Size, Home, mode context, and assisted/record wording. | MainFrame.showResultsDialog now offers Replay Puzzle, New Size, and Home; replay builds a fresh model from PuzzleIdentity and preserves size/difficulty while clearing moves, time, and histories. | PARITY | Replay restores the completed initialGrid and is not a reshuffle or new record-eligible puzzle. A new-size action may intentionally start a new puzzle; richer mode context remains later scope. | DesktopSessionContractTest verifies exact grid, zero moves/time, difficulty, and empty histories; DesktopResultContentTest verifies difficulty text. |
| G6 | Active-play timer semantics | GameModel tracks active elapsed milliseconds with pauseTimer, resumeTimer, and getElapsedTime; win time is captured at completion. | MainActivity.syncGameTimerState pauses outside an interactive GAME screen, onPause, and while timer-pausing dialogs are shown. Saves store elapsed milliseconds. | MainFrame status uses model.getElapsedTime; Home, all JOptionPane dialogs, save/load, solver computation, and inactive-window events pass through the DesktopTimerPolicy gate. The Swing Timer only refreshes display. | PARITY | Only focused interactive play accumulates time. Modal/controller depth, solver ownership, Home, inactive windows, and solved state pause the shared model timer; restart and exact replay resume from zero. | DesktopTimerPolicy and DesktopSessionContractTest cover every gate without sleeps; GameModelTest covers deterministic elapsed-time behavior. Manual window focus/dialog smoke remains part of the Desktop acceptance gate. |
| G7 | Activity/window lifecycle and state restoration | AndroidActivityState serializes screen, return screen, mode flags, and transient navigation state; lifecycle is not a shared gameplay rule. | MainActivity onCreate, onSaveInstanceState, onPause, onResume, and onBack navigation preserve state and save active games. | MainFrame keeps card navigation in one JFrame, pauses on inactive/modal state, autosaves stable normal sessions on navigation/close, and restores the last valid slot through Home Continue after relaunch. | PLATFORM-SPECIFIC | Android lifecycle APIs remain Android-only. Desktop provides the equivalent pause/save/relaunch contract without pretending to reproduce Activity callbacks. | AndroidActivityStateTest and lifecycle instrumentation cover Android; SaveManagerTest covers durable recovery and CI compiles the Swing window-event wiring. Manual close/relaunch smoke remains a desktop acceptance follow-up. |
| G8 | Input outcome equivalence | GameModel is the single move path; Direction and slideLineTo define outcome independent of input device. | KlotskiView accepts tap, swipe, keyboard/accessibility actions through the controller, with busy and solved guards. | BoardPanel accepts aligned mouse click/release, swipe-like drag release, and arrow-key bindings; it uses the shared model and blocks while busy. | PARITY | Equivalent legal input must produce the same board, move count, action history, win, and assisted status. Input-specific affordances may differ. Invalid or non-aligned input must not mutate state. | GameModelTest, Android whole-line/accessibility tests, and the existing desktop smoke checklist. Keep input translation in views/controllers, never in a second rules engine. |
| G9 | Animation, queued solver playback, and input locks | GameObserver exposes move and line-move callbacks; the model reports state changes while views own animation. | KlotskiView tracks animation, queued directions, reduced motion, and busy state; MainActivity locks controller actions during solver work. | BoardPanel tracks animation, queued solver moves, pending win, and isBusy; MainFrame guards menu actions. | PARITY | No move may be accepted while an animation or solver queue is active; a whole-line animation must finish before Results. Reduced motion may change presentation but not state or move count. | Existing Android flow and desktop manual checks; add deterministic desktop animation/busy tests in the UI stage. Risk is asynchronous completion and duplicate win dialogs, not shared rules. |
| G10 | Independent normal saves and Continue selection | SaveManager.SaveData carries board, initial grid, difficulty, histories, elapsed, active, and solved fields. | AndroidGameStore has independent normal slots for sizes 3, 4, and 5 and renders metadata on Home. | SaveManager now exposes independent `klotski_save_3.json`, `klotski_save_4.json`, and `klotski_save_5.json` slots; MainFrame lists size/difficulty/moves/time metadata and offers a chooser. | PARITY | A size slot cannot replace another size; difficulty is retained as slot metadata until later record/progression namespaces require finer isolation. Continue restores the exact saved state. | SaveManagerTest covers all three slots and metadata; MainFrame uses the same atomic SaveManager path. Legacy migration remains non-destructive. |
| G11 | Autosave and background/close durability | Android lifecycle invokes saveGame on pause and state save; normal saves retain histories and elapsed time. | MainActivity.onPause and onSaveInstanceState save the active namespace. | MainFrame autosaves stable sessions on Home/navigation, window deactivation, and close; it skips transient BoardPanel animation and solver-owned state while manual Save remains available. Normal and dated Daily sessions route to their own atomic namespace. | PARITY | Autosave targets only the current normal or Daily date slot at safe lifecycle boundaries. Atomic writes retain a temporary candidate and backup recovery copy; Favorite/Continuous namespaces remain separate stages. | SaveManagerTest covers atomic replacement/failure, normal isolation, and dated daily persistence; MainFrame lifecycle wiring is compiled/Javadoc-checked. Manual close/relaunch smoke remains an acceptance follow-up. |
| G12 | Save metadata, action histories, and migration | SaveManager validates and reconstructs action and Redo histories from initialGrid; missing legacy fields default safely. | AndroidGameStore.putSave/readSavedGame persists size, grid, initialGrid, moves, elapsed, updatedAt, active, solved, difficulty, histories, and assisted state per namespace. | SaveManager persists the common metadata and both histories per size slot; malformed histories fall back safely through GameModel, and legacy JSON/.dat inputs migrate known fields without deleting sources or overwriting newer slots. | PARITY | Desktop save data retains every shared normal-game field. Schema version 3 is additive; unknown legacy fields remain in untouched source files, and future unsupported versions are rejected rather than silently rewritten. | SaveManagerTest covers JSON/.dat migration, missing fields, malformed history, newer-slot preservation, and backup recovery. Android remains the owner of mode-specific namespaces. |
| G13 | Undo and Redo semantics | GameModel.undo and redo move the latest completed MoveAction between stacks; a new valid action clears Redo; restart clears both. | Android Game screen buttons and AndroidGameStore persist both histories. | MainFrame Ctrl+Z/Ctrl+Y menu actions call the same model methods; BoardPanel redraws from model state. | PARITY | One whole-line action remains one undoable action; Redo reapplies the exact direction and step count. Saves and restarts preserve or clear stacks exactly as the shared contract requires. | GameModelTest, Android move-history tests, and desktop smoke. No new rules code; add desktop persistence assertions when per-size saves land. |
| G14 | Move History presentation and timer pause | MoveAction exposes direction and steps; history is bounded to the latest 50 in Android UI. | MainActivity.showMoveHistory pauses via showTimerPausingDialog, shows completed and Redo counts, and localizes directions. | MainFrame.showMoveHistoryDialog shows counts and latest 50 but leaves the model timer running while the modal dialog is open and is English-only. | PARTIAL | Desktop history must show completed and available Redo actions, preserve whole-line “one move” wording, pause active time for the dialog, and use the selected desktop locale when localization is added. | Add Swing dialog/timer tests and content assertions; reuse GameModel history tests. Risk is modal reentrancy and EDT timer coordination. |
| G15 | Restart current puzzle | GameModel.restartCurrentGame restores initialGrid, resets moves and histories, and restarts active timing. | MainActivity.restartCurrentGame is available from Game and pause menu and saves the restored state. | MainFrame.restartCurrentGame calls the same model method, retains the selected difficulty, and is disabled while BoardPanel or solver work is busy. | PARITY | Restart must retain the exact puzzle identity, size, and selected difficulty while clearing actions and resetting active elapsed time. It must not alter records, history, daily streaks, favorites, or continuous aggregates. | GameModelTest, Android flow tests, DesktopSessionContractTest, and desktop restart smoke. Timer display and lifecycle gates remain covered by G6. |
| G16 | Local best records by size and difficulty | SaveManager.BestRecord compares fewer moves then lower time; PuzzleDifficulty supplies the scope dimension. | AndroidGameStore stores best records per size and difficulty and refuses assisted wins. | SaveManager stores additive `size:difficulty` records; MainFrame and DesktopResultContent query the selected scope and keep assisted wins out of player bests. | PARITY | Desktop records key by both size and difficulty, use the shared comparison, and leave the player best unchanged for solver or strategic-hint assisted runs. Legacy size-only values are read deterministically as Classic and remain preserved until a better scoped value is written. | SaveManagerTest covers scope separation, tie-breaks, and legacy-source preservation; DesktopResultContentTest and MainFrame flow cover scoped result wording and assisted protection. |

### Progression, repeat play, and assistance

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | Completion history and lifetime statistics | PersonalTrend and WeeklyGoalProgress consume completion samples; GameModel supplies moves and active time but not the personal store. | MainActivity.showWinWhenReady records player and assisted completions; AndroidGameStore keeps bounded history, counts, moves, and time; Records and Trends render them. | SaveManager keeps a newest-first bounded history, persistent lifetime totals by size+difficulty, and completion ids; MainFrame claims one run before recording and the Records dialog renders scoped totals. Daily wins now use the same normal history/stats contract. | PARTIAL | Every normal, daily, and continuous completion must be recorded exactly once; favorite practice is excluded. Assisted completions remain visible in history and lifetime totals but never update player bests. Desktop normal and Daily history/statistics are verified; favorite/continuous attribution and Trends UI remain later stages. | SaveManagerTest covers idempotency, assisted/player totals, history round-trip, retention, and Daily completion routing; DesktopCompletionTrackerTest covers duplicate callbacks. Risk remains mode attribution until later namespaces are added. |
| P2 | Offline Daily Challenge | DailyChallenge defines fixed 4x4 Classic date-derived seed and createGame. | MainActivity.startDailyChallenge and AndroidGameStore dated daily saves implement the daily route. | MainFrame exposes a keyboard-reachable Daily Calendar route; SaveManager validates the shared date-derived 4x4 Classic initial grid and persists `klotski_daily_YYYY-MM-DD.json` plus an assistance marker. | PARITY | Desktop uses the same ISO date ID, fixed size/difficulty, deterministic board, future-date block, and per-date save namespace. Daily completion feeds the shared history/stats and streak rules but never replaces a normal save slot. | DailyChallengeTest, SaveManagerTest, and DesktopDailyContentTest cover identity, future rejection, save isolation, assisted marker, and Desktop state labels. Local ISO-date policy is explicit; timezone boundaries remain a caller-owned date choice. |
| P3 | Daily Calendar, historical replay, and streaks | DailyCalendarMonth supplies immutable Sunday-first month data and future clamping; daily streak state is store policy. | AndroidDailyCalendarScreen and MainActivity expose month navigation, historical replay, completion status, and latest-date streak updates. | MainFrame renders a Sunday-first modal calendar with previous/next month, completed/in-progress/missed/future state labels, historical replay/resume, and keyboard-focusable buttons; SaveManager keeps completed dates and latest/best streak state. | PARITY | Historical dates remain visible and replayable but cannot move the latest-date streak backward; future dates are disabled. Daily saves remain isolated by ISO date and Results reports the current/best streak. | DailyCalendarMonthTest, SaveManagerTest, and DesktopDailyContentTest cover month clamping, dated progress, gaps, duplicate/historical streak updates, and accessible labels. Manual Swing keyboard smoke remains a follow-up. |
| P4 | Favorite Puzzle library and exact identity | PuzzleIdentity is a stable SHA-256 over size, difficulty, and exact initialGrid. | AndroidFavoritesScreen and AndroidGameStore save up to 50 labeled favorites, deduplicate identity, and permit rename/remove. | Desktop has no favorite library, identity label, or favorite actions. | MISSING | Desktop favorites must retain the immutable starting board, size, difficulty, identity, label, and creation ordering. Saving the same identity updates the label instead of duplicating it. | PuzzleIdentityTest, Android favorites tests, and new desktop library tests. Risk is mutable-grid aliasing and maximum-list migration; copy the initial grid defensively. |
| P5 | Isolated favorite-practice progress | AndroidGameStore saveFavoriteRun/loadFavoriteRun validates identity and uses a separate prefix; MainActivity excludes favorite wins from records and personal history. | AndroidFavoritesScreen launches exact practice and Results returns to the library. | No favorite practice namespace or mode exists. | MISSING | Favorite practice may save and resume only its own board and histories. It must not overwrite normal, daily, or continuous saves, records, completion history, lifetime stats, or streaks. | AndroidGameStoreTest and MainActivityFlowTest provide isolation cases; add desktop cross-namespace tests. Risk is a mode flag leaking into generic save or win handlers. |
| P6 | Personal Trends and Weekly Goal | PersonalTrend and WeeklyGoalProgress are shared, deterministic calculation types. | AndroidTrendsScreen and AndroidGameStore persist size/difficulty scope, samples, and weekly target. | Desktop has no trend or goal view and no completion sample source. | MISSING | Desktop must consume the same player-completion samples and scope semantics, show insufficient-data states, and keep goals local/offline. Assisted and favorite exclusions must match Android. | PersonalTrendTest, WeeklyGoalProgressTest, Android trends tests, and new desktop presentation tests. Risk is inventing a second aggregation formula; reuse the core types. |
| P7 | Continuous Challenge and isolated session persistence | ContinuousChallenge models target 3, 5, or 10 and aggregate moves, time, and assisted state. | MainActivity and AndroidGameStore support nine size/difficulty scopes, resume/replace/end, next/repeat/end Results, and an isolated save. | Desktop has no continuous entry, aggregate state, or isolated session save. | MISSING | Desktop must keep exact current board and aggregate totals in a continuous namespace; normal, daily, and favorite saves remain untouched. Each puzzle contributes one history/stat sample and assisted runs never update player bests. | ContinuousChallengeTest, Android continuous instrumentation, and desktop aggregate/isolation tests. Risk is partial completion and replace/end confirmation semantics. |
| P8 | Movable-tile assist | GameModel exposes empty row/column; the view can derive aligned movable cells without mutation. | MainActivity.showMovableTilesHint and KlotskiView render presentation-only highlights and clear them on state changes. | MainFrame.showMovableTiles and BoardPanel.setHighlightedCells do the same for non-empty aligned cells. | PARITY | Highlighting must never move, count an action, invoke a solver, or update records; it clears on move, undo, restart, load, new game, and solver playback. | Android and desktop assist tests/smoke plus help-content tests. Keep this presentation-only contract explicit in both UI docs. |
| P9 | Strategic hint | StrategicHint computes a deterministic fixed-depth legal suggestion without mutating GameModel. | MainActivity.showStrategicHint displays the suggestion, marks the run assisted, and persists that state. | Desktop has no strategic hint entry or assisted flag for hints. | MISSING | Desktop must offer the same non-mutating hint contract, mark the run assisted before a hinted move can win, and preserve the best-record exclusion. | StrategicHintTest, Android hint/persistence tests, and new desktop hint/result tests. Risk is accidentally treating a visual movable highlight as a strategic hint or changing model state while previewing. |
| P10 | Solver tools, warnings, and playback UX | Solver, BfsSolver, AStarSolver, and IdaStarSolver are shared algorithms; GameObserver supports queued playback. | MainActivity Assist > Solver Tools warns by size, runs off the UI thread, locks input, and reports failure or playback. | MainFrame Solver menu runs the same three algorithms with SwingWorker, size warnings, and BoardPanel queue; menu placement and failure guidance differ. | PARTIAL | Desktop needs equivalent solver-tool grouping, size warnings, cancellation/failure handling, busy locks, and explicit assisted-run wording. Algorithm names and limits remain shared-core facts. | SolverTest, Android solver tests, desktop manual smoke, and focused SwingWorker tests. Risk is memory/time on 4x4 and 5x5; never weaken Android or core solver limits for visual parity. |
| P11 | Assisted-run record protection | SaveManager and AndroidGameStore both compare records only for eligible player runs; Android also tracks assisted completions. | MainActivity sets assistedSolveActive for strategic hints and solver playback and excludes it from best records. | MainFrame sets assistedSolveActive when solver moves are animated and excludes those wins from SaveManager.recordBest; it has no strategic-hint or stats coverage. | PARITY | The best-record protection itself is aligned for solver playback and must remain aligned for future hints. Broader history/stat accounting is tracked separately in P1 and is currently missing. | Desktop ResultContent tests and Android results/store tests; add a desktop assisted hint/solver matrix before claiming full progression parity. Risk is resetting the flag too early or allowing a new puzzle to inherit it. |

### Learning, navigation, settings, and presentation

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| L1 | Home, navigation, start, and continue flows | No new rules are needed; navigation is platform-specific controller state. | AndroidHomeScreen and MainActivity expose Home, daily, favorites, trends, continuous, onboarding, mode select, records, settings, and save metadata. | MainFrame opens a Home card with size buttons, Continue/Load, How to Play, Practice Tutorial, Records, and Preferences; no richer progression destinations exist. | PARTIAL | Desktop should expose every meaningful desktop destination in a discoverable order and make Continue show the correct slot or mode metadata. Android transitions remain native; Swing may use cards/dialogs. | MainActivityFlowTest and desktop home-content tests are the baseline; add Swing navigation tests. Risk is making Home navigation accidentally discard active elapsed state. |
| L2 | First-run onboarding and Beginner Guide | Puzzle rules are shared, but onboarding content is UI-only. | MainActivity and AndroidTutorialScreen provide four onboarding pages, Skip, Back, Tutorial, and Start 3x3; AndroidLearningContent supplies the guide. | No first-run onboarding or Beginner Guide exists; Practice Tutorial is a static help dialog. | MISSING | Desktop needs an optional first-run guide with a persisted seen flag, skip/reopen behavior, and a clear route to a first puzzle. It must not alter puzzle data or records. | Android onboarding tests and new desktop first-run/preferences tests. Risk is confusing a tutorial board with a record-eligible game. |
| L3 | Practice Tutorial | AndroidTutorialScreen and MainActivity use fixed tutorial boards and guided highlights; KlotskiView only renders hints. | Interactive first move, aligned tile, whole-line, and completion steps with progress and reset. | DesktopHelpContent.practiceTutorial is static explanatory text shown in a dialog; it does not accept moves or track tutorial state. | PARTIAL | Desktop may use Swing interaction, but must teach the same first move, aligned move, whole-line one-action behavior, and completion outcome without writing personal records. | Android tutorial tests, desktop help-content tests, and future Swing tutorial tests. Risk is duplicating movement logic; tutorial moves should still call GameModel. |
| L4 | How to Play and learning examples | AndroidLearningContent contains visual Goal, Tap, whole-line, swipe, tools, and records guidance. | Android How-to screen renders localized examples and links to learning surfaces. | DesktopHelpContent.howToPlay contains aligned-move, whole-line, assist, solver, and GameModel text in a plain dialog. | PARTIAL | Desktop content must remain behaviorally accurate and explain whole-line one-action history, assists, solver record protection, and replay. Visual layout can be native Swing. | Desktop help-copy tests and Android learning tests. Risk is stale parity wording; link this document and update the content when contracts change. |
| L5 | Quick Reminder during play | Android pause menu opens a compact localized reminder without losing the current game. | MainActivity.showQuickReminder is reachable from the Game pause menu and pauses active timing. | Desktop has no Quick Reminder command; How to Play is a general modal help route. | MISSING | Add an in-game reminder that pauses active time and returns to the same board without mutating state. It should be shorter than the full guide. | Add desktop dialog/timer tests and content assertions. Risk is forgetting the G6 modal pause rule. |
| L6 | Settings and preferences | AndroidGameStore persists language, theme, sound, haptic, reduced motion, onboarding, and reset choices. | AndroidSettingsScreen and MainActivity apply settings, recreate for locale/theme, and expose backup/reset. | MainFrame Preferences exposes only an in-memory reduced-motion checkbox; colors, sound, language, backup, and reset are absent. | PARTIAL | Desktop settings must persist the supported subset, clearly label platform-specific options, and never reset a save when only presentation settings change. | Android settings/store tests and new desktop preference persistence tests. Risk is applying a setting on the wrong thread or losing active state on recreate-like transitions. |
| L7 | Visual themes and contrast | AndroidVisualTheme and AndroidColorContrast define Midnight/Day palettes and 4.5:1 button contrast checks. | AndroidUi, AndroidUiPolicy, and KlotskiView apply the selected theme persistently. | BoardPanel and MainFrame use fixed colors and no theme selector; no automated contrast check exists. | MISSING | Provide at least the agreed theme set or explicitly scope a smaller desktop palette, keep text and controls readable, and persist the choice. Theme changes must not affect puzzle state. | AndroidAdaptiveUiTest and new desktop rendering/contrast smoke. Risk is Swing look-and-feel variance and color contrast regression. |
| L8 | Optional sound feedback | Sound is not a shared puzzle rule. | AndroidSoundFeedback and AndroidGameStore provide optional asset-free move, win, and error tones. | Desktop has no sound setting or feedback path. | MISSING | If meaningful for desktop, provide an opt-in local feedback channel with the same enabled/disabled and assisted semantics; do not make audio a prerequisite for play. | Add opt-in desktop smoke and settings tests; no core dependency. Risk is platform audio availability and test nondeterminism. |
| L9 | Reduced motion | Presentation-only policy; GameModel timing and actions remain unchanged. | AndroidGameStore persists reduced motion; AndroidMotion, AndroidUiPolicy, and KlotskiView apply it across transitions and board animation. | MainFrame and BoardPanel snap board animation when an in-memory flag is set; Home/dialog transitions and persistence are not covered. | PARTIAL | Persist the desktop preference, apply it to all parity-relevant transitions/animations, and keep busy and action semantics unchanged. | Android adaptive tests, desktop preference/animation tests, and manual 100/125/150% checks. Risk is conflating reduced motion with disabling input locks. |
| L10 | Localization | Core domain IDs are stable; Android resources carry localized presentation. | AndroidAppLocale and resources support English, Traditional Chinese, and Japanese and persist the selected tag. | Desktop strings are hardcoded English in MainFrame, BoardPanel, and helper content. | MISSING | Decide a desktop locale subset and localize all player-facing labels, history directions, status, settings, and result policy text. Preserve stable IDs and migrate no puzzle data for locale changes. | Android locale tests and new desktop resource/content tests. Risk is duplicating strings in Swing and making accessibility descriptions diverge from visible text. |
| L11 | Full personal backup and restore | AndroidPersonalDataArchive validates a versioned archive and AndroidGameStore replaces all personal preferences only after decode. | MainActivity uses the system picker for export/import and confirms full replacement. | Desktop can manually copy individual JSON files but has no archive, validation, or full replacement flow. | MISSING | Provide a versioned, validated Desktop personal-data archive covering all normal/mode saves, records, stats, history, settings, and favorites. Invalid input must leave existing data untouched. | AndroidGameStoreTest archive cases are the contract reference; add desktop archive round-trip, malformed, and replacement tests. Risk is cross-platform schema compatibility; keep Android implementation out of Desktop and define an explicit shared archive version. |
| L12 | Reset semantics | AndroidGameStore separates clear saved games, clear records, and full archive replacement while preserving unrelated namespaces as documented. | AndroidSettingsScreen/MainActivity expose reset saved games and reset records with confirmation. | Desktop has no reset UI; deleting a file manually has undefined scope and can affect legacy fallback behavior. | MISSING | Add explicit, confirmed reset operations with documented scope: saved-game namespaces versus records/stats/history/settings. Reset must not silently delete backups or unrelated user files. | Android reset/store tests and new desktop reset tests. Risk is destructive scope; use recoverable or clearly confirmed operations and preserve legacy files until migration is complete. |
| L13 | Accessibility semantics and playable board | AndroidUiPolicy establishes headings, focus order, 48dp targets, localized descriptions, and KlotskiView virtual per-cell actionable nodes. | AndroidMainActivity, AndroidUi, resource strings, and BoardAccessibilityProvider expose screen-reader movement for movable cells. | Swing buttons and labels provide basic keyboard/focus behavior, but BoardPanel is one painted surface with no virtual cell nodes or equivalent assistive-tech audit. | PARTIAL | Desktop must expose a navigable, actionable board model to supported assistive technologies, retain keyboard movement, announce busy/win/history state, and meet contrast/focus expectations. Android virtual-node mechanics remain platform-specific. | AndroidAdaptiveUiTest and Android flow tests; add desktop accessibility/manual keyboard and assistive-tech checks. Risk is Swing accessibility API behavior and painted-cell hit targets. |
| L14 | Adaptive and scaled layout | AndroidUiPolicy and ScreenLayout handle compact layouts, large text, safe insets, and 48dp controls. | AndroidAdaptiveUiTest covers compact AVD, large text, both themes, headings, and focus order. | BoardPanel scales the square board to its panel, but MainFrame is fixed at 600x700 and dense Home/dialog content has no large-font or wide-window acceptance. | PARTIAL | Desktop must remain usable at supported DPI/font scales and resizable windows, keep the board playable, and preserve complete actions without relying on Android dp rules. | Android adaptive tests plus desktop 100/125/150% and resize smoke; add focused layout assertions where stable. Risk is native Swing layout variability across platforms. |

### Platform-specific mechanics (not direct semantic parity defects)

| ID | Capability or mechanism | Canonical/shared-core support | Android source of truth | Desktop equivalent or current behavior | Status | Required boundary; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S1 | Haptic feedback | None; haptics are device output. | KlotskiView.performHapticFeedback and AndroidGameStore haptic preference. | No haptic hardware contract exists for Swing; optional keyboard/click or sound feedback is the desktop equivalent. | PLATFORM-SPECIFIC | Keep haptic setting Android-only unless a desktop device API is deliberately approved. A missing haptic pulse must never change moves, timer, or records. | Android settings/flow tests; document the desktop equivalent in Preferences rather than faking haptics. Risk is OS-specific hardware behavior. |
| S2 | Android system file picker | None; picker is an Android integration surface. | MainActivity ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT and onActivityResult wrap archive I/O. | Desktop needs a native Swing file chooser or explicit path selection for the same backup contract; current Desktop has none. | PLATFORM-SPECIFIC | The chooser mechanism differs, but archive validation, full-replacement confirmation, and no-change-on-invalid semantics must align with L11. | Android backup tests and desktop chooser/invalid-input smoke. Risk is permissions and cancellation; keep picker behavior outside shared core. |
| S3 | Touch gestures and virtual accessibility nodes | Shared move outcomes only; touch thresholds and virtual node IDs are UI mechanics. | KlotskiView touch threshold, swipe direction, AccessibilityNodeProvider, and virtual per-cell actions. | BoardPanel mouse/drag and keyboard input plus Swing accessibility components are the equivalent; current painted board lacks per-cell nodes. | PLATFORM-SPECIFIC | Do not copy Android virtual-node or touch code. Qualify equivalent legal outcomes, actionable focus, and announcements on desktop; input-specific thresholds may differ. | Android accessibility/whole-line tests and desktop mouse/keyboard/accessibility smoke. Risk is claiming semantic parity from coordinates alone. |

### Packaging and release-facing behavior

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R1 | Release/package behavior visible to players | Shared core and versioned data contracts must remain compatible across packages. | Android debug/release APK/AAB, Play-readiness checks, connected smoke, and Android README checklist. | Desktop ZIP and optional app-image package with user-data path notes; no signed installer and manual smoke/accessibility remain beta concerns. | PARTIAL | Both packages must launch the qualified flows, preserve user-data locations and migration behavior, and state platform-specific limitations. Signing, store assets, and installer choice remain release governance, not parity implementation. | ci.bat, DESKTOP_BETA_READINESS.md, android README/checklist, package scripts, and manual smoke. Risk is treating package/build evidence as runtime acceptance; require separate desktop and Android lifecycle/accessibility gates. |

## Matrix result

The 45 rows above are counted by the status cell, not by the number of source
files:

| Status | Count | Interpretation |
| --- | ---: | --- |
| PARITY | 15 | Shared rules, sizes, difficulty/session identity, exact replay, active timer, input outcomes, animation locks, undo/redo, restart, movable assist, scoped best records, assisted-result protection, dated Daily saves, calendar replay, and streak boundaries are evidenced on both platforms. |
| PARTIAL | 12 | The Desktop slice exists but differs materially in persistence namespace, records dimension, learning depth, settings, or adaptive/accessibility coverage. |
| MISSING | 14 | Android-only completed product capabilities have no verified Desktop equivalent, chiefly favorites/trends/continuous, onboarding, themes, sound, localization, backup, and reset. |
| PLATFORM-SPECIFIC | 4 | Android lifecycle, haptics, system picker, and touch/virtual-node mechanics require desktop equivalents rather than copied implementations. |

### Stage 1 verified implementation

The Stage 1 session-contract work is implemented on this branch and is limited
to the issue boundary:

- MainFrame offers Relaxed, Classic, and Challenge for every supported board
  size and routes creation through DesktopGameFactory and shared GameModel.
- Seeded Desktop construction delegates to GameModel.scramble; exact replay
  derives a PuzzleIdentity from the completed model and restores the same
  initial grid with zero moves, elapsed time, and action histories.
- Desktop status and Results use GameModel active elapsed milliseconds.
  DesktopTimerPolicy gates the model timer for Home, modal dialogs, solver
  ownership, and inactive windows.
- BoardPanel exposes a controller input lock during solver computation, while
  ordinary animation and whole-line action semantics remain unchanged.
- Focused tests cover all size/difficulty combinations, seeded equality,
  exact replay, timer gates, solver locking, and Results difficulty wording.

Persistence migration and per-difficulty records are implemented in the
dependent Stage 2 and Stage 3 updates; later mode namespaces remain assigned to
downstream issues.

Highest-risk gaps, in dependency order:

1. G10-G12 and G16: save/schema migration and record scope. A superficially
   working Desktop UI can still lose histories, difficulty scope, or a newer
   per-size slot.
2. P1-P7: progression namespaces and exactly-once completion accounting.
   Normal, daily, favorite-practice, and continuous state must stay isolated.
3. L13-L14 and S3: desktop accessibility and resizable/large-text behavior.
   Painted-board bounds are not evidence of actionable or screen-reader parity.
4. R1: packaging evidence is not lifecycle acceptance. Signed installers,
   store distribution, and manual release gates remain separately governed.

## Documentation coverage audit

### Current evidence and gaps

The scan covered 17 shared-core Java files, 5 Desktop UI files, and 30 Android
production Java files. Both core and UI packages generally have a documented
top-level type, and the principal public gameplay APIs already carry useful
English Javadocs. In particular, GameModel, SaveManager, PuzzleDifficulty,
PuzzleIdentity, DailyChallenge, DailyCalendarMonth, ContinuousChallenge,
PersonalTrend, WeeklyGoalProgress, MoveAction, GameObserver, Solver, and the
three solver classes expose the important domain vocabulary.

The coverage is not complete or mechanically measurable from the current
doclint gate:

1. Top-level types are mostly documented, but nested types and implementation
   helpers are uneven. SaveManager data holders and the AndroidGameStore
   value-types have useful comments; solver search nodes, Android controller
   state, and the KlotskiView BoardAccessibilityProvider do not consistently
   state ownership, lifecycle, or invariants.
2. Public and protected constructors/methods in GameModel, SaveManager,
   BoardPanel, MainFrame, KlotskiView, and MainActivity are mostly covered.
   Android screen-builder methods and AndroidGameStore's package-private
   persistence API have many signatures whose contracts are only implied by
   call sites. MainActivity's documented lifecycle methods do not make every
   navigation, namespace, or exactly-once completion side effect discoverable.
3. Public/protected fields are intentionally sparse. Constants and nested
   record-like fields that are exposed to tests or sibling packages need
   contract documentation for units, mutability, nullability, and stability;
   package-private constants such as archive versions and save-key prefixes
   need rationale because they define migration behavior.
4. Non-obvious private/package-private contracts are the largest gap:
   GameModel timer anchors and history reconstruction; SaveManager legacy
   fallback and record comparison; AndroidGameStore namespace isolation,
   archive replacement, date/streak rules, and favorite identity validation;
   MainActivity lifecycle/timer synchronization, pending-win exactly-once
   recording, solver locks, and mode routing; and BoardPanel/KlotskiView
   animation queues, deferred wins, and accessibility action dispatch.
5. Some comments are implementation-noise or can become stale: MainFrame
   “Initialize Model/View”, “Menus”, and “Timer for UI update” comments merely
   restate nearby code; several UI comments describe a current menu shape
   without stating the behavioral contract. The historical parity matrix in
   DEVELOPMENT.md also describes the narrow MVP as if the broader Android
   progression were already comparable. This document is the current
   qualification source; no Java comments were changed in this stage.
6. The existing core/Desktop and Android Javadoc commands use
   -public -Xdoclint:all. They prove syntax, links, and doclint validity for
   the selected public surface, not completeness of package-private contracts,
   behavior accuracy, or UI accessibility documentation.

### Professional standard for the later documentation stage

The later documentation stage should be contract-led, English, and reviewed
alongside behavior changes:

- Document every top-level and nested type that is part of a maintained
  surface. For public/protected constructors, methods, and meaningful fields,
  state purpose, parameters, return/throw behavior, nullability, mutation or
  ownership, units, thread/EDT requirements, and persistence impact where
  applicable.
- Document shared-core invariants explicitly: empty-cell Direction semantics,
  whole-line one-action history, deterministic seeded scrambles, initialGrid
  replay, active-only milliseconds, undo/redo clearing, malformed-history
  recovery, record ordering, solver limits, and namespace exclusions.
- Document non-obvious Android and Desktop package-private contracts at their
  boundary: lifecycle and main-thread rules, timer pause gates, pending-win
  exactly-once behavior, solver/input locks, archive validation, migration
  policy, mode-specific save routing, accessibility actions, and Swing EDT or
  modal-dialog effects.
- Treat nested value types and version/key constants as data contracts. Explain
  stable IDs, archive/save versions, bounds, ordering, and compatibility
  decisions; do not expose implementation details that are not relied upon.
- Remove comments that only paraphrase a line of code, and replace stale
  historical claims with links to the authoritative contract or roadmap.
  Keep user-facing README and release documentation synchronized in the later
  documentation-sync stage, not during this audit.
- Run both Javadoc gates, doclint, focused contract tests, and a lightweight
  review checklist that names uncovered public/protected and non-obvious
  package-private surfaces. Do not use comment-count percentages as an
  acceptance criterion.

## Bounded implementation roadmap

Every stage below is a separate owner gate. A stage may add focused tests and
English contract documentation, but it must not broaden into the next stage
without a new qualification review.

### Stage 1 - Desktop session contract

Status: implemented and verified in the current Stage 1 branch; the
completion gate below is retained as the acceptance contract for the merged
PR.

- Objective: add Desktop difficulty choice, deterministic puzzle identity,
  exact initial-grid replay from Results, and active-only timer behavior.
- Behavioral boundary: 3x3/4x4/5x5 remain supported; difficulty affects only
  valid-move scramble depth; Results Replay Puzzle restores the same starting
  board; Home, modal dialogs, inactive windows, and solver preparation pause
  elapsed time; restart remains exact and clears histories.
- Likely files/components: MainFrame, BoardPanel, DesktopResultContent,
  DesktopHomeContent, and only genuinely platform-neutral core contracts if a
  test demonstrates a gap. Do not make Android classes Desktop dependencies.
- Tests to add/change: deterministic GameModel/identity cases if needed;
  desktop difficulty/result flow tests; a controllable desktop timer test;
  whole-line, restart, and assisted-result regressions.
- Migration risk: legacy Desktop saves without difficulty default to Classic;
  old saves can derive identity from initialGrid but cannot recover an
  original random seed. Preserve the old result action until exact replay is
  proven.
- Validation: android\gradlew.bat -p . test; desktop compile; focused Swing
  tests/smoke; both Javadoc commands; git diff --check.
- Documentation affected: DEVELOPMENT parity status and the matrix; desktop
  help/result wording only when the implemented behavior is verified.
- Out of scope: per-size persistence, daily/favorite/continuous modes,
  localization, theme/audio, packaging, and Android behavior changes.
- Completion gate: repeatable size/difficulty/seed identity, exact replay,
  active elapsed-time assertions, and solver/hint record protection pass on
  Desktop without changing shared movement outcomes.

### Stage 2 - Desktop persistence and lifecycle

Status: implemented and verified on the current Stage 2 branch; the completion
gate below remains the acceptance contract for its protected PR.

- Objective: bring normal saves, Continue metadata, autosave, close/inactive
  handling, and action-history migration to the Android data contract.
- Behavioral boundary: independent 3x3/4x4/5x5 normal slots; save/load includes
  all shared metadata and both histories; autosave uses safe boundaries and
  recoverable writes; legacy JSON/.dat loads without discarding newer state.
- Likely files/components: SaveManager, MainFrame, DesktopHomeContent, and a
  Desktop-only persistence/lifecycle adapter if existing patterns require it.
- Tests to add/change: SaveManager round trips per size, malformed histories,
  legacy fallback, atomic-write failure, Continue metadata, close/reopen,
  modal pause, and autosave isolation tests.
- Migration risk: one-slot klotski_save.json and legacy serialized data need a
  non-destructive mapping; size-only records are not silently rewritten as
  multi-difficulty records.
- Validation: root core tests, desktop compile and persistence smoke, ci.bat,
  both Javadoc gates, and git diff --check.
- Documentation affected: SaveManager contract, Desktop data-path notes in
  DEVELOPMENT, and a migration note in the parity document.
- Out of scope: daily/favorite/continuous namespaces, full backup archive,
  records/statistics redesign, and release signing.
- Completion gate: restart/relaunch/close scenarios recover the exact active
  board, elapsed milliseconds, difficulty, and histories for every normal slot.

2026-09-07 Stage 2 evidence:

- `SaveManager` now uses one atomic JSON slot per supported normal size. The
  slot keeps difficulty as gameplay metadata, so changing a difficulty does not
  invent a second namespace that Android does not have; later records and mode
  stages own any finer scope.
- `MainFrame` exposes a metadata-backed Continue chooser and autosaves only at
  stable Home/navigation, inactive-window, and close boundaries. In-flight
  animation and solver-owned state are deliberately not written.
- Legacy `klotski_save.json` and `klotski_save.dat` are read from the configured
  user-data directory and project-root fallback. Migration copies known fields
  into the matching size slot only when it is absent or older, leaves the
  source untouched (including unknown fields), and defaults missing difficulty
  and histories to Classic/empty. Unsupported future JSON versions are rejected
  without rewriting the source.
- Atomic replacement retains `.tmp` and `.bak` recovery candidates. Focused
  tests cover slot isolation, metadata, migration, newer-slot preservation,
  malformed histories, recovery, and failed replacement behavior.

### Stage 3 verified implementation

The Stage 3 records/results/statistics work is implemented and verified within
the owner-approved issue boundary:

- `SaveManager` stores additive `size:difficulty` best records in
  `klotski_records_v2.json`. Existing size-only `klotski_records.json` values
  are read as Classic, remain untouched, and are copied into the scoped file
  only when a better Classic result is submitted. Lower moves rank first and
  equal moves use lower active time.
- Completion samples are persisted in `klotski_statistics.json` with a
  newest-first history capped at 50 entries, lifetime player/assisted totals,
  and a durable completion-id ledger. Repeated callbacks return no-op and do
  not increment totals. Assisted samples remain visible but never update a
  player best.
- `MainFrame` resets a completion scope for new, replayed, restarted, and
  loaded puzzles; `DesktopCompletionTracker` claims the first win callback;
  Results and Records query the same size+difficulty scope and expose the
  player/assisted counts.
- Focused tests cover scoped records, tie-breaks, legacy-source preservation,
  exactly-once history/stat updates, duplicate callback claims, and scoped
  Results wording. Shared tests and desktop compilation remain required gates.

### Stage 4 verified implementation

The Stage 4 Daily Challenge/calendar work is implemented within the approved
issue boundary:

- `SaveManager` validates shared `DailyChallenge` identity and stores one
  `klotski_daily_YYYY-MM-DD.json` file per local ISO date, with a separate
  assistance marker. Normal size slots and dated Daily slots cannot overwrite
  one another.
- Daily completion state is stored separately with completed-date membership,
  latest-date current streak, and best streak. A historical completion can add
  a missing date but cannot move the latest-date streak backward; duplicate and
  future-date completions are rejected.
- MainFrame exposes a keyboard-focusable Sunday-first calendar, previous/next
  month navigation, completed/in-progress/missed/future labels, exact dated
  replay/resume, and Daily Results streak copy. Daily wins reuse the Stage 3
  record/history/assisted-run contract.
- Focused tests cover deterministic identity, dated-save isolation, future
  rejection, streak gaps/historical replay, and accessible calendar labels.
  Manual Swing keyboard/close smoke remains an explicit acceptance follow-up.

### Stage 3 - Records, results, history, and statistics

- Objective: add size-plus-difficulty best records, completion history,
  lifetime statistics, and a full Desktop Results surface.
- Behavioral boundary: fewer moves then lower time comparison; exactly-once
  player and assisted completion samples; solver/strategic assisted runs
  never update player bests; favorite practice remains excluded when later
  modes are present.
- Likely files/components: SaveManager or a Desktop-only personal store,
  MainFrame, DesktopResultContent, DesktopHomeContent, and Records UI.
  Reuse shared PersonalTrend/WeeklyGoalProgress types rather than copying math.
- Tests to add/change: record-scope migration tests, normal/assisted result
  tests, duplicate win callback tests, completion history/stat round trips,
  and desktop records UI content tests.
- Migration risk: old size-only bests require an explicit Classic mapping or
  visible legacy archive; do not fabricate historical completion samples.
- Validation: core tests, desktop compile, focused UI/persistence tests,
  ci.bat, Javadocs, and manual results smoke.
- Documentation affected: records policy, result/replay wording, and
  assisted-run data contract in DEVELOPMENT and the parity document.
- Out of scope: daily dates, favorites, trends visualizations, continuous
  sessions, and package/distribution changes.
- Completion gate: every result path has the correct record text and exactly
  one history/stat update, with unchanged bests for assisted runs.

### Stage 4 - Daily Challenge and calendar

- Objective: provide the fixed daily puzzle, dated saves, calendar/history
  replay, future-date blocking, and streak semantics on Desktop.
- Behavioral boundary: 4x4 Classic date-derived seed; ISO-date namespace;
  historical replay visible but not streak-backdating; future dates disabled;
  daily state never replaces normal saves.
- Likely files/components: DailyChallenge, DailyCalendarMonth, Desktop
  controller/panels, and the Desktop personal store. Reuse existing core
  date/seed contracts.
- Tests to add/change: date-boundary, deterministic board, month clamp,
  historical replay, streak, and cross-namespace persistence tests.
- Migration risk: no prior Desktop daily data exists; choose an explicit new
  namespace and local-date/time-zone policy.
- Validation: core daily/calendar tests, desktop compile, focused UI/store
  tests, ci.bat, Javadocs, and manual date smoke using a controllable clock.
- Documentation affected: daily behavior and date/streak policy in
  DEVELOPMENT and desktop help.
- Out of scope: favorites, trends, continuous, backup archive, and release
  distribution.
- Completion gate: current and historical daily flows reproduce Android board
  identity and preserve all normal-save and streak invariants.

### Stage 5 - Favorites, Trends, and Continuous Challenge

- Objective: add the remaining isolated repeat-play systems in dependency
  order: exact favorite library and practice, personal trends/goals, then
  continuous sessions.
- Behavioral boundary: favorite identity is size+difficulty+initialGrid;
  practice is isolated; trends use only eligible completion samples; weekly
  goals use shared math; continuous supports 3/5/10 with fixed
  size/difficulty scope and exact current-board aggregate persistence.
- Likely files/components: PuzzleIdentity, PersonalTrend, WeeklyGoalProgress,
  ContinuousChallenge, Desktop personal store, and new Swing panels/dialogs.
- Tests to add/change: favorite deduplication/rename/remove/max-50 and
  isolation tests; trend/goal scope and insufficient-data tests; continuous
  resume/replace/end/aggregate/exactly-once tests.
- Migration risk: no Desktop namespaces exist; use additive versioned keys or
  files and never reinterpret normal or daily data as practice progress.
- Validation: all related core tests, desktop compile, focused store/UI tests,
  ci.bat, Javadocs, and manual cross-mode save checks.
- Documentation affected: mode isolation, records/stats eligibility, and
  Home/Results navigation in DEVELOPMENT and the parity document.
- Out of scope: Android implementation changes, cloud sync/accounts, ads,
  analytics, and public-store work.
- Completion gate: a matrix of normal, daily, favorite, and continuous
  cross-save operations proves no namespace overwrite and one sample per
  eligible completion.

### Stage 6 - Learning, navigation, and preferences

- Objective: close meaningful Home, onboarding, tutorial, How-to, Quick
  Reminder, settings, theme, sound, reduced-motion, localization, and reset
  gaps using native Swing presentation.
- Behavioral boundary: all learning and settings routes preserve the current
  puzzle and timer policy; tutorial/favorite practice cannot update records;
  reset scopes are explicit; desktop locale/theme choices are persistent.
- Likely files/components: MainFrame, BoardPanel, DesktopHomeContent,
  DesktopHelpContent, DesktopResultContent, new Swing screen/panel helpers,
  and Desktop personal settings storage.
- Tests to add/change: navigation and first-run persistence, tutorial no-record,
  modal timer pause, settings round-trip, locale/resource, contrast/theme,
  reduced-motion, and reset-scope tests.
- Migration risk: existing reduced-motion is in-memory; introduce a
  versioned preference without changing puzzle saves. Do not infer Android
  haptic behavior for Desktop.
- Validation: core tests unchanged unless a real shared gap is found; desktop
  compile, focused Swing tests, manual keyboard/mouse/DPI smoke, ci.bat,
  Javadocs, and git diff --check.
- Documentation affected: Desktop help/preferences and the status portions of
  DEVELOPMENT; README and release docs remain a later synchronization gate.
- Out of scope: virtual Android nodes, system picker implementation, signed
  installer, store assets, and cloud features.
- Completion gate: a fresh Desktop profile can discover, learn, play, pause,
  reset, and configure the app without data loss or record contamination.

### Stage 7 - Accessibility, adaptive behavior, and platform equivalents

- Objective: qualify desktop accessibility and resizable/large-font behavior
  and implement the desktop equivalents for Android lifecycle, picker, and
  input requirements.
- Behavioral boundary: Swing accessibility must make board actions and status
  discoverable; supported DPI/font/window sizes retain complete controls;
  native file chooser replaces Android picker mechanics while archive
  semantics remain shared; window inactivity/close maps to Android pause/save.
- Likely files/components: BoardPanel, MainFrame, desktop layout/accessibility
  helpers, persistence chooser, and packaging smoke harness.
- Tests to add/change: keyboard traversal, assistive-technology/manual
  accessibility, large-font and resize checks, window activation/close,
  chooser cancel/invalid archive, and input-equivalence tests.
- Migration risk: native Swing accessibility and OS window events vary; keep
  acceptance evidence separate by platform and do not promote coordinate or
  emulator evidence into desktop acceptance.
- Validation: desktop compile, manual 100/125/150% and resize smoke, archive
  tests, ci.bat, both Javadocs, and a documented Windows desktop accessibility
  pass.
- Documentation affected: desktop accessibility runbook, parity matrix, and
  beta readiness references only after behavior is actually qualified.
- Out of scope: Android accessibility rewrites, iOS/tablet claims, and visual
  pixel-diff equivalence.
- Completion gate: a reproducible desktop manual/automated evidence bundle
  demonstrates actionable board controls, complete focus order, pause/save
  lifecycle behavior, and valid backup cancellation/rejection.

### Stage 8 - Packaging and release qualification

- Objective: synchronize user-facing documentation and qualify Desktop ZIP,
  app-image or an explicitly approved installer alongside Android package
  behavior.
- Behavioral boundary: package launch, user-data paths, migration, update or
  reinstall retention, and platform-specific limitations are documented;
  signing and distribution remain separately approved release decisions.
- Likely files/components: package-desktop.bat, desktop package templates,
  DESKTOP_BETA_READINESS.md, android release checks, README and final docs.
- Tests to add/change: fresh install, upgrade/retention, uninstall/reinstall,
  launch, save migration, accessibility, and rollback checks in a recoverable
  Windows environment; Android connected evidence remains distinct.
- Migration risk: released tags/assets are immutable; use a candidate branch
  and never test destructive uninstall or rollback against the user's live
  data without a recoverable environment.
- Validation: ci.bat, package scripts, desktop beta checks, Android release
  checks, and manual lifecycle evidence. Do not treat static package output
  as runtime acceptance.
- Documentation affected: README, DESKTOP_BETA_READINESS.md, android README,
  regression checklist, and DEVELOPMENT only after the gates pass.
- Out of scope: store signing keys, public upload, accounts, cloud sync,
  analytics, ads, and any unapproved release/version changes.
- Completion gate: owner-approved package and lifecycle evidence is complete,
  documentation is synchronized, and the parity matrix is re-qualified.

### Stage 9 - Professional Java documentation

- Objective: apply the documentation standard above across core, Desktop, and
  Android after behavior contracts settle.
- Behavioral boundary: documentation only; no API or product behavior changes.
  Cover public/protected surfaces and non-obvious internal contracts without
  mechanical getter comments.
- Likely files/components: all Java files under src/com/klotski/core,
  src/com/klotski/ui, and android/app/src/main/java/com/klotski/android,
  plus synchronized engineering docs.
- Tests/validation: both Javadoc commands with -public -Xdoclint:all, link
  checks, contract-review checklist, and focused tests for any documented
  invariant that was previously unverified.
- Migration risk: stale claims are more dangerous than missing prose; review
  every changed contract against current tests and the final parity matrix.
- Documentation affected: Java Javadocs, DEVELOPMENT, README, Android README,
  regression and beta runbooks.
- Out of scope: changing implementation to make doc counts look complete and
  any public distribution work.
- Completion gate: maintainers can discover ownership, lifecycle, persistence,
  threading, migration, and assisted-record rules from authoritative docs,
  while doclint and the full verification contract remain green.

## Program guardrails and re-qualification

- Keep GameModel platform-independent and reuse shared domain types only when
  the contract is genuinely platform-neutral.
- Keep AndroidGameStore and Android UI classes out of Desktop dependencies.
- Preserve normal, daily, favorite-practice, and continuous namespaces and
  their distinct record/stat eligibility.
- Treat install, package, CI, connected-device, Swing, accessibility, and
  lifecycle evidence as separate gates; one platform cannot substitute for
  another.
- At the end of every stage, rerun the relevant matrix rows and update their
  status only from new code/test evidence. A stage is not complete because a
  menu or file exists.
- This qualification is intentionally stopped before implementation. An owner
  must approve the next bounded stage and any migration policy that affects
  existing Desktop saves.
