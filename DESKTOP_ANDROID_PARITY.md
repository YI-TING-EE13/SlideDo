# SlideDo Desktop / Android Parity Qualification

Status: owner-approved qualification baseline with Stages 1-7 session,
persistence, records, Daily, Favorites, Trends, Weekly Goal, and Continuous
behavior plus Desktop learning/preferences/accessibility implemented; Desktop
Stage 8 archive and package qualification is implemented with automated
evidence and the required extracted-package GUI/DPI gate is recorded as
owner-reported manual acceptance on 2026-09-08. Codex did not execute the GUI
tests. Stage 9 has completed the contract-led Java/API and documentation audit
without executable behavior changes. Screen-reader certification remains NOT
CLAIMED. This document records current implementation evidence and bounded
follow-up work.

Qualification date: 2026-09-08
Repository: YI-TING-EE13/SlideDo
Authoritative integration branch: protected `main`
Stage 9 qualification branch: `codex/professional-docs-final-sync`
Stage 8 historical branch point: 4762999889acbb0ef89d5783ed42baaee57ebdf9
Stage 9 starting `origin/main`: b90f2813854ec630d74784b13aa5b21d8af9f473

## Authority, scope, and evidence rules

The shared `GameModel` and other platform-independent core types remain the
canonical sources for shared gameplay and domain semantics. Android and Desktop
are separate platform implementations of lifecycle, UI, and persistence. Parity
means equivalent player-visible outcomes and data contracts, not copied Android
or Swing lifecycle/widget mechanics and not a blanket UI reference.

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

- The required fetch and identity checks completed before editing. Stage 9
  started with HEAD and origin/main both resolved to
  b90f2813854ec630d74784b13aa5b21d8af9f473.
- The previous 4762999889acbb0ef89d5783ed42baaee57ebdf9 identity is retained
  above as the historical Stage 8 branch point, not as the current baseline.
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

Repair qualification for PR #25 (2026-09-07):

- L10 was expanded from a registry-only check to an audited material-flow
  catalog. MainFrame and the Desktop content helpers now route normal
  Save/Load, Home/status, Daily, Favorites, Trends/Weekly Goal, Continuous,
  learning/tutorial, Results, preference descriptions, and accessibility copy
  through explicit English, Traditional Chinese, and Japanese entries.
- Normal-save assisted migration is fail-closed only where provenance is
  ambiguous: missing `assisted` in an unsolved v1-v3 normal payload remains
  false/eligible because the pre-v4 autosave gate did not persist solver-owned
  or busy sessions; missing `assisted` in a solved v1-v3 normal payload becomes
  true/assisted. Explicit v4 true/false values are preserved. Daily, Favorite
  Practice, and Continuous sidecar/meta markers are loaded through their
  existing isolated paths and remain authoritative.
- Focused tests include `DesktopLocaleCoverageTest`,
  `DesktopMaterialContentTest`, and `SaveManagerTest` cases for JSON/.dat
  solved/unsolved migration, explicit v4 values, and isolated sidecar/meta
  compatibility. The owner-reported 2026-09-07 manual acceptance covers the
  extracted ZIP launch, 100%/125%/150% scaling, larger-text/adaptive behavior,
  keyboard/focus behavior, and the pre-Stage-7 mouse contract. This repair did
  not independently rerun that GUI gate; screen-reader certification remains
  NOT CLAIMED.

Stage 8 archive and release qualification (2026-09-07):

- `DesktopPersonalDataArchive` defines Desktop-only format
  `slidedo-desktop-personal-data` version 1. Entries are deterministic by
  logical basename and carry byte length, SHA-256, and Base64 payload; the
  archive is bounded to 512 entries, 128-character identifiers, 1 MiB per
  entry, 4 MiB decoded total, and 8 MiB encoded UTF-8.
- The explicit registry covers three normal slots, project-root and data-dir
  legacy JSON/serialized fallback sources, scoped and legacy records, reset masks, statistics/history, every
  dated Daily save and assistance marker, Daily progress, Favorites and each
  validated Favorite Practice run/marker, preferences, and Continuous
  metadata/current/assistance. Atomic `.tmp`/`.bak` siblings are classified as
  managed recovery files but are not exported; each logical entry resolves by
  the loader's canonical -> `.tmp` -> `.bak` precedence, including when the
  canonical bytes are corrupt. A valid Continuous snapshot requires coherent
  metadata/current ownership and a compatible assistance sidecar.
  Generated packages and all unmanaged files are excluded.
- Decode rejects malformed JSON, unknown fields/entries, duplicate entries,
  traversal/absolute/drive-letter/alternate-separator IDs, unsupported format
  or version, size/digest mismatch, and bounds violations. Candidate state is
  validated through SaveManager's board, difficulty, action/Redo history,
  favorite identity, Daily identity, Continuous aggregate, preference,
  records/statistics, reset-marker, and assisted-sidecar checks before any
  replacement.
- Restore is full replacement of the managed namespace: absent entries and
  stale recovery siblings are removed, unmanaged files remain, and the durable
  saved-games reset marker masks every external normal-save fallback whenever
  the archive has no imported legacy source. A recoverable previous snapshot is
  used for rollback on write/delete/rename or final-validation failure. If
  rollback fails, the transaction and previous snapshot are retained and a
  recovery-required exception identifies the location; if post-success cleanup
  fails, a distinct cleanup warning preserves the recovery directory instead of
  claiming a clean transaction. Legacy-only profiles remain loadable and their
  source files are not rewritten or deleted.
- Export and Restore paths use canonical component comparisons to reject managed
  files, `.tmp`/`.bak` siblings, project-root legacy fallbacks, and active
  restore transactions. Owner archives are written through a sibling temporary
  file before replacement, so an export-write failure cannot mutate managed
  personal data. A successful restore invalidates any older Preferences editor
  generation before restored settings are applied.
- Preferences exposes keyboard-reachable Export Personal Data and Restore
  Personal Data controls. Restore validates before confirmation, treats Cancel
  and invalid input as no-ops, blocks while animation/solver state is busy,
  and rebuilds MainFrame/controller state after success so autosave cannot
  overwrite the imported archive. The three supported Desktop locales contain
  explicit backup strings.
- Automated package checks enforce the exact four-file ZIP whitelist and the
  generated README documents launch, Java 17+, user-data location, backup
  replacement semantics, local owner-controlled scope, solver limits, and the
  unsigned ZIP limitation.
- Owner-reported final packaged-Windows acceptance (2026-09-08) is PASS for
  the final extracted ZIP candidate: clean `SlideDo.bat` launch outside the
  development tree; an isolated Windows APPDATA/profile; visible and
  keyboard-reachable Export/Restore controls with working Space/Enter,
  chooser-open, and cancellation behavior; valid export without disturbing
  active state; explicit full-replacement restore with restored preferences and
  save state, removal of post-backup state, and no stale Preferences writeback;
  relaunch retention; invalid-archive and chooser-cancel no-mutation
  behavior; managed-path collision rejection without overwriting the normal
  save; and usable, visible, non-overlapping backup controls, focus, and
  chooser at 100%, 125%, and 150% Windows scaling. No exact temporary profile
  path is claimed. This is owner-reported evidence, not Codex GUI execution;
  the accepted Stage 7 board/mouse evidence remains separate.

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
| G12 | Save metadata, action histories, and migration | SaveManager validates and reconstructs action and Redo histories from initialGrid; missing legacy fields default safely. | AndroidGameStore.putSave/readSavedGame persists size, grid, initialGrid, moves, elapsed, updatedAt, active, solved, difficulty, histories, and assisted state per namespace. | SaveManager persists the common metadata, both histories, and additive assisted eligibility bit per size slot; malformed histories fall back safely through GameModel, and legacy JSON/.dat inputs migrate known fields without deleting sources or overwriting newer slots. | PARITY | Desktop save data retains every shared normal-game field. Schema version 4 is additive: explicit assisted values are preserved; a v1-v3 normal save missing provenance stays unassisted only when unsolved, while a solved payload fails closed as assisted. Daily, Favorite Practice, and Continuous sidecar/meta markers remain authoritative and are not changed by this normal-save rule. | SaveManagerTest covers JSON/.dat migration, solved/unsolved missing-assisted behavior, explicit v4 values, isolated sidecar/meta compatibility, malformed history, newer-slot preservation, and backup recovery. Android remains the owner of mode-specific namespaces. |
| G13 | Undo and Redo semantics | GameModel.undo and redo move the latest completed MoveAction between stacks; a new valid action clears Redo; restart clears both. | Android Game screen buttons and AndroidGameStore persist both histories. | MainFrame Ctrl+Z/Ctrl+Y menu actions call the same model methods; BoardPanel redraws from model state. | PARITY | One whole-line action remains one undoable action; Redo reapplies the exact direction and step count. Saves and restarts preserve or clear stacks exactly as the shared contract requires. | GameModelTest, Android move-history tests, and desktop smoke. No new rules code; add desktop persistence assertions when per-size saves land. |
| G14 | Move History presentation and timer pause | MoveAction exposes direction and steps; history is bounded to the latest 50 in Android UI. | MainActivity.showMoveHistory pauses via showTimerPausingDialog, shows completed and Redo counts, and localizes directions. | MainFrame.showMoveHistoryDialog retains the existing depth-based modal timer pause and uses a deterministic formatter for counts, latest 50 completed actions, empty-cell directions, whole-line one-action wording, and English/Traditional Chinese/Japanese copy. | PARITY | Desktop history is presentation-only: opening it does not mutate model history, persisted action ordering remains oldest-first, and modal depth pauses active elapsed time without a second pause mechanism. | DesktopHistoryContentTest and DesktopTimerPolicy/SessionContractTest; full shared/Desktop suite. Manual packaged modal smoke remains a separate GUI gate. |
| G15 | Restart current puzzle | GameModel.restartCurrentGame restores initialGrid, resets moves and histories, and restarts active timing. | MainActivity.restartCurrentGame is available from Game and pause menu and saves the restored state. | MainFrame.restartCurrentGame calls the same model method, retains the selected difficulty, and is disabled while BoardPanel or solver work is busy. | PARITY | Restart must retain the exact puzzle identity, size, and selected difficulty while clearing actions and resetting active elapsed time. It must not alter records, history, daily streaks, favorites, or continuous aggregates. | GameModelTest, Android flow tests, DesktopSessionContractTest, and desktop restart smoke. Timer display and lifecycle gates remain covered by G6. |
| G16 | Local best records by size and difficulty | SaveManager.BestRecord compares fewer moves then lower time; PuzzleDifficulty supplies the scope dimension. | AndroidGameStore stores best records per size and difficulty and refuses assisted wins. | SaveManager stores additive `size:difficulty` records; MainFrame and DesktopResultContent query the selected scope and keep assisted wins out of player bests. | PARITY | Desktop records key by both size and difficulty, use the shared comparison, and leave the player best unchanged for solver or strategic-hint assisted runs. Legacy size-only values are read deterministically as Classic and remain preserved until a better scoped value is written. | SaveManagerTest covers scope separation, tie-breaks, and legacy-source preservation; DesktopResultContentTest and MainFrame flow cover scoped result wording and assisted protection. |

### Progression, repeat play, and assistance

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| P1 | Completion history and lifetime statistics | PersonalTrend and WeeklyGoalProgress consume completion samples; GameModel supplies moves and active time but not the personal store. | MainActivity.showWinWhenReady records player and assisted completions; AndroidGameStore keeps bounded history, counts, moves, and time; Records and Trends render them. | SaveManager keeps a newest-first bounded history, persistent lifetime totals by size+difficulty, and completion ids; MainFrame claims one run before recording, excludes Favorite Practice, and accounts Continuous puzzles through the same scoped store. Records and Trends render the resulting player/assisted totals. | PARITY | Every normal, Daily, and Continuous completion is recorded exactly once; Favorite Practice is excluded. Assisted completions remain visible in history and lifetime totals but never update player bests. | SaveManagerTest covers idempotency, assisted/player totals, history retention, Daily/Favorite/Continuous isolation, and trend filtering; DesktopCompletionTrackerTest covers duplicate callbacks. |
| P2 | Offline Daily Challenge | DailyChallenge defines fixed 4x4 Classic date-derived seed and createGame. | MainActivity.startDailyChallenge and AndroidGameStore dated daily saves implement the daily route. | MainFrame exposes a keyboard-reachable Daily Calendar route; SaveManager validates the shared date-derived 4x4 Classic initial grid and persists `klotski_daily_YYYY-MM-DD.json` plus an assistance marker. | PARITY | Desktop uses the same ISO date ID, fixed size/difficulty, deterministic board, future-date block, and per-date save namespace. Daily completion feeds the shared history/stats and streak rules but never replaces a normal save slot. | DailyChallengeTest, SaveManagerTest, and DesktopDailyContentTest cover identity, future rejection, save isolation, assisted marker, and Desktop state labels. Local ISO-date policy is explicit; timezone boundaries remain a caller-owned date choice. |
| P3 | Daily Calendar, historical replay, and streaks | DailyCalendarMonth supplies immutable Sunday-first month data and future clamping; daily streak state is store policy. | AndroidDailyCalendarScreen and MainActivity expose month navigation, historical replay, completion status, and latest-date streak updates. | MainFrame renders a Sunday-first modal calendar with previous/next month, completed/in-progress/missed/future state labels, historical replay/resume, and keyboard-focusable buttons; SaveManager keeps completed dates and latest/best streak state. | PARITY | Historical dates remain visible and replayable but cannot move the latest-date streak backward; future dates are disabled. Daily saves remain isolated by ISO date and Results reports the current/best streak. | DailyCalendarMonthTest, SaveManagerTest, and DesktopDailyContentTest cover month clamping, dated progress, gaps, duplicate/historical streak updates, and accessible labels. Manual Swing keyboard smoke remains a follow-up. |
| P4 | Favorite Puzzle library and exact identity | PuzzleIdentity is a stable SHA-256 over size, difficulty, and exact initialGrid. | AndroidFavoritesScreen and AndroidGameStore save up to 50 labeled favorites, deduplicate identity, and permit rename/remove. | SaveManager stores up to 50 exact `FavoritePuzzle` identities in `klotski_favorites.json`; MainFrame exposes save/list/replay/rename/delete actions and uses defensive starting-grid copies. | PARITY | Favorite identity includes size, difficulty, and exact initialGrid; saving the same identity updates its label instead of duplicating it. | PuzzleIdentityTest, SaveManagerTest favorite deduplication/isolation cases, and DesktopFavoriteContentTest. |
| P5 | Isolated favorite-practice progress | AndroidGameStore saveFavoriteRun/loadFavoriteRun validates identity and uses a separate prefix; MainActivity excludes favorite wins from records and personal history. | AndroidFavoritesScreen launches exact practice and Results returns to the library. | SaveManager stores validated practice state in `klotski_favorite_<identity>.json`; MainFrame routes autosave/results to that namespace and excludes Favorite Practice from records, completion history, statistics, and Daily streaks. | PARITY | Favorite practice may save and resume only its own board and histories; it cannot overwrite normal, Daily, or Continuous state. | SaveManagerTest cross-namespace and assisted-marker cases; DesktopResultContentTest and DesktopFavoriteContentTest cover exclusion copy. |
| P6 | Personal Trends and Weekly Goal | PersonalTrend and WeeklyGoalProgress are shared, deterministic calculation types. | AndroidTrendsScreen and AndroidGameStore persist size/difficulty scope, samples, and weekly target. | SaveManager filters non-assisted completion history by size/difficulty, delegates calculations to shared `PersonalTrend`/`WeeklyGoalProgress`, and persists scope/target in `klotski_personal_preferences.json`; MainFrame exposes summary, scope, and target dialogs. | PARITY | Desktop shows insufficient-data states and keeps goals local/offline; assisted and Favorite Practice results are excluded. | SaveManagerTest verifies scope, persistence, and exclusion; DesktopPersonalPlayContentTest covers summary formatting. |
| P7 | Continuous Challenge and isolated session persistence | ContinuousChallenge models target 3, 5, or 10 and aggregate moves, time, and assisted state. | MainActivity and AndroidGameStore support nine size/difficulty scopes, resume/replace/end, next/repeat/end Results, and an isolated save. | SaveManager stores exact current board plus aggregate in `klotski_continuous_meta.json` and `klotski_continuous_current.json`; MainFrame exposes start/resume/next/end paths for fixed size/difficulty scopes. Each claimed puzzle updates shared history/stats once, while assisted runs remain out of player bests. | PARITY | Normal, Daily, and Favorite namespaces remain untouched; ending Continuous deletes only its files, while Records/statistics files remain independent. | SaveManagerTest round-trip/clear/isolation and `ContinuousChallengeTest`; DesktopPersonalPlayContentTest covers aggregate copy. |
| P8 | Movable-tile assist | GameModel exposes empty row/column; the view can derive aligned movable cells without mutation. | MainActivity.showMovableTilesHint and KlotskiView render presentation-only highlights and clear them on state changes. | MainFrame.showMovableTiles and BoardPanel.setHighlightedCells do the same for non-empty aligned cells. | PARITY | Highlighting must never move, count an action, invoke a solver, or update records; it clears on move, undo, restart, load, new game, and solver playback. | Android and desktop assist tests/smoke plus help-content tests. Keep this presentation-only contract explicit in both UI docs. |
| P9 | Strategic hint | StrategicHint computes a deterministic fixed-depth legal suggestion without mutating GameModel. | MainActivity.showStrategicHint displays the suggestion, marks the run assisted, and persists that state. | MainFrame Assist > Strategic Hint calls the shared StrategicHint service, highlights one recommendation without moving the model, marks the run assisted before continuation, and persists the marker through normal and isolated mode saves. | PARITY | Identical state yields the same legal suggestion; grid, move count, elapsed state, action/Redo history, and initial puzzle identity remain unchanged by computation. Assisted completions remain in history/statistics where the mode permits but cannot replace player bests. | StrategicHintTest, SaveManager assisted round-trip/migration tests, DesktopSessionContractTest, and Android hint/store coverage. |
| P10 | Solver tools, warnings, and playback UX | Solver, BfsSolver, AStarSolver, and IdaStarSolver are shared algorithms; GameObserver supports queued playback. | MainActivity Assist > Solver Tools warns by size, runs off the UI thread, locks input, and reports failure or playback. | MainFrame Solver Tools groups BFS/A*/IDA*, applies deterministic size warnings, executes only through SwingWorker, locks BoardPanel input, offers cooperative cancellation, reports failure/no-solution guidance, and marks assistance only when playback is accepted. | PARITY | Solver search never runs on the EDT or mutates the live model; interruption checks are cooperative and no forced thread termination is used. Queue playback remains the single completion path. | DesktopSolverPolicyTest, SolverTest interruption coverage, existing busy/child mouse tests, full shared/Desktop suite, and Android solver coverage. |
| P11 | Assisted-run record protection | SaveManager and AndroidGameStore both compare records only for eligible player runs; Android also tracks assisted completions. | MainActivity sets assistedSolveActive for strategic hints and solver playback and excludes it from best records. | MainFrame sets assistedSolveActive when solver moves are animated and excludes those wins from SaveManager.recordBest; it has no strategic-hint or stats coverage. | PARITY | The best-record protection itself is aligned for solver playback and must remain aligned for future hints. Broader history/stat accounting is tracked separately in P1 and is currently missing. | Desktop ResultContent tests and Android results/store tests; add a desktop assisted hint/solver matrix before claiming full progression parity. Risk is resetting the flag too early or allowing a new puzzle to inherit it. |

### Learning, navigation, settings, and presentation

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| L1 | Home, navigation, start, and continue flows | No new rules are needed; navigation is platform-specific controller state. | AndroidHomeScreen and MainActivity expose Home, daily, favorites, trends, continuous, onboarding, mode select, records, settings, and save metadata. | MainFrame Home and Game menu expose normal sizes, Continue/Load, Daily Calendar, Favorites, Trends/Weekly Goal, Continuous Challenge, Records, Preferences, Beginner Guide, and Quick Reminder; mode dialogs retain isolated saves while Home autosaves stable state. | PARITY | Desktop transitions remain native Swing cards/dialogs, but all completed Desktop-supported destinations are discoverable without changing save or record contracts. | MainFrame compile/Javadocs, SaveManager isolation tests, and DesktopPersonalPlayContentTest cover the routes; manual Swing cross-mode smoke remains an acceptance follow-up. |
| L2 | First-run onboarding and Beginner Guide | Puzzle rules are shared, but onboarding content is UI-only. | MainActivity and AndroidTutorialScreen provide four onboarding pages, Skip, Back, Tutorial, and Start 3x3; AndroidLearningContent supplies the guide. | MainFrame opens a four-page first-run guide from a persisted `onboardingSeen` flag, supports Skip/Back/Next/Tutorial/Start 3x3, and allows reopening from Help/Home without writing game data. | PARITY | The tutorial board is isolated; leaving the guide marks only onboarding preference state and cannot create a record or overwrite a save. | DesktopLearningContentTest, DesktopLocaleTest, SaveManager preference/reset tests, and MainFrame compile/Javadocs. Risk remains only native Swing modal presentation. |
| L3 | Practice Tutorial | AndroidTutorialScreen and MainActivity use fixed tutorial boards and guided highlights; KlotskiView only renders hints. | Interactive first move, aligned tile, whole-line, and completion steps with progress and reset. | MainFrame opens an isolated BoardPanel/GameModel lesson with reset, guided milestone text, whole-line detection, and Start 3x3; no completion tracker or personal store is attached. | PARITY | Tutorial teaches first move, aligned move, whole-line one-action behavior, and completion without writing personal records. | DesktopTutorialProgressTest, DesktopLearningContentTest, and focused compile/Javadocs; model history remains the rule source. |
| L4 | How to Play and learning examples | AndroidLearningContent contains visual Goal, Tap, whole-line, swipe, tools, and records guidance. | Android How-to screen renders localized examples and links to learning surfaces. | DesktopLearningContent/HelpContent documents goal, aligned movement, whole-line one-action history, assist, solver record protection, and exact replay in a native dialog. | PARITY | Content is behaviorally accurate; Swing text replaces Android visual resources without copying Android resource files. | DesktopHelpContentTest plus localized learning-content tests; update this copy when shared contracts change. |
| L5 | Quick Reminder during play | Android pause menu opens a compact localized reminder without losing the current game. | MainActivity.showQuickReminder is reachable from the Game pause menu and pauses active timing. | Game > Quick Reminder opens the compact localized reminder through `runWithPausedTimer`, then returns to the same board/mode. | PARITY | Reminder is shorter than the full guide and mutates neither model state nor records. | DesktopLearningContentTest and existing DesktopTimerPolicy/modal wiring; manual in-game dialog smoke remains useful. |
| L6 | Settings and preferences | AndroidGameStore persists language, theme, sound, haptic, reduced motion, onboarding, and reset choices. | AndroidSettingsScreen and MainActivity apply settings, recreate for locale/theme, and expose backup/reset. | SaveManager persists the Desktop subset: language (`en`/`zh-TW`/`ja-JP`), Midnight/Ocean theme, sound feedback, reduced motion, onboarding seen, plus confirmed saved-game and records/statistics resets. | PARITY | Haptics and backup remain platform-specific; presentation changes rebuild Swing controls without changing puzzle rules, saves, or records. | SaveManager preference/reset tests, DesktopLocale/Theme tests, MainFrame compile/Javadocs. |
| L7 | Visual themes and contrast | AndroidVisualTheme and AndroidColorContrast define Midnight/Day palettes and 4.5:1 button contrast checks. | AndroidUi, AndroidUiPolicy, and KlotskiView apply the selected theme persistently. | DesktopTheme supplies persisted Midnight/Ocean palettes to Home and BoardPanel; numbered tiles and text retain high-contrast colors and theme changes are presentation-only. | PARITY | Desktop palette names are native equivalents; no Android color resource dependency is introduced. | DesktopThemeTest, BoardPanel compile/Javadocs, and the owner-reported Stage 7 packaged review; screen-reader certification remains NOT CLAIMED. |
| L8 | Optional sound feedback | Sound is not a shared puzzle rule. | AndroidSoundFeedback and AndroidGameStore provide optional asset-free move, win, and error tones. | SaveManager persists an opt-in sound flag; MainFrame emits native Toolkit move/win beeps only when enabled, with no audio prerequisite. | PARITY | Audio remains presentation-only and cannot change moves, timing, assisted state, or records. | SaveManager preference test and DesktopSoundFeedback compile; audio device behavior remains manual/platform-specific. |
| L9 | Reduced motion | Presentation-only policy; GameModel timing and actions remain unchanged. | AndroidGameStore persists reduced motion; AndroidMotion, AndroidUiPolicy, and KlotskiView apply it across transitions and board animation. | SaveManager persists reduced motion; MainFrame applies it to BoardPanel and preserves busy/action semantics while preference changes rebuild the native shell. | PARITY | Reduced motion changes animation presentation only; it does not disable input locks or alter rules/records. | SaveManager preference test, BoardPanel API, desktop compile/Javadocs, and the owner-reported Stage 7 adaptive review. |
| L10 | Localization | Core domain IDs are stable; Android resources carry localized presentation. | AndroidAppLocale and resources support English, Traditional Chinese, and Japanese and persist the selected tag. | DesktopLocale now supplies explicit material-flow keys for Move History, Strategic Hint, Solver Tools/warnings/results/cancellation, assisted results/status, normal Save/Load, Home, Records, Daily, Favorites, Trends/Weekly Goal, Continuous, learning/tutorial, difficulty, preferences, and BoardPanel accessibility semantics in all three supported tags. MainFrame and the Desktop content helpers route audited player-facing copy through this catalog; stable IDs and technical solver/theme names remain English by design. | PARITY | Locale changes rebuild the Swing shell and preserve stable IDs/data; no Android resources or locale-dependent persisted IDs are introduced. | DesktopLocaleCoverageTest requires every registered material key to be explicit and nonblank in each locale; DesktopMaterialContentTest exercises localized save/status, Daily, Favorites, Trends, Continuous, Results, Records, and learning output; MainFrame/helper literal audit is recorded with the repair evidence. |
| L11 | Full personal backup and restore | AndroidPersonalDataArchive validates a versioned archive and AndroidGameStore replaces all personal preferences only after decode. | MainActivity uses the system picker for export/import and confirms full replacement. | `DesktopPersonalDataArchive` exports a Desktop-specific version-1 archive through Preferences, resolves logical recovery candidates, validates every managed namespace and assisted/reset/legacy dependency, and performs full replacement with stale-state deletion, durable legacy masks, an archived project-root fallback boundary, unmanaged-file preservation, recoverable rollback, collision-safe atomic export, and stale-editor invalidation. MainFrame treats ordinary restore and cleanup-warning restore as successful imports that reconcile state; rollback failure enters a localized recovery-required fail-safe that retains recovery paths and disables gameplay and all controller persistence. | PARITY | Invalid input and Cancel are no-ops. A legacy-only profile remains loadable and migratable from the restored data directory without combining with process-root legacy files; ordinary pre-archive root migration remains available. Cleanup warnings name retained transaction data without claiming failure; rollback failure retains a previous snapshot and reports recovery required without deleting it. The archive deliberately does not claim Android schema interchange; both platforms keep complete but platform-specific local formats. | `DesktopPersonalDataArchiveTest`, `SaveManagerTest`, `DesktopPreferencesEditorGuardTest`, `DesktopRestoreOutcomePolicyTest`, DesktopLocale coverage, package README/readiness checks, Android archive tests as companion evidence, and the owner-reported 2026-09-08 extracted-package backup/restore gate. Screen-reader certification remains NOT CLAIMED. |
| L12 | Reset semantics | AndroidGameStore separates clear saved games, clear records, and full archive replacement while preserving unrelated namespaces as documented. | AndroidSettingsScreen/MainActivity expose reset saved games and reset records with confirmation. | Preferences exposes confirmed `clearSavedGames` and `clearRecords` actions. The first removes normal/Daily/Favorite Practice/Continuous saves but preserves favorites, records, stats, Daily progress, and preferences; the second clears scoped records/statistics/Daily progress, masks legacy records, and preserves active Continuous files. | PARITY | Reset scopes are explicit and tested; legacy record sources are left untouched but cannot resurrect a cleared value. | SaveManager preference/reset test and MainFrame confirmation wiring; full archive replacement is covered by L11/Stage 8 archive tests. |
| L13 | Accessibility semantics and playable board | AndroidUiPolicy establishes headings, focus order, 48dp targets, localized descriptions, and KlotskiView virtual per-cell actionable nodes. | AndroidMainActivity, AndroidUi, resource strings, and BoardAccessibilityProvider expose screen-reader movement for movable cells. | BoardPanel exposes one focusable Swing button per cell with accessible names/descriptions, row-major Tab order, Space/Enter activation, arrow-key movement, and visible focus borders; MainFrame labels status, menus, Home actions, and learning controls. | PARITY | Desktop uses Swing roles and actionable child controls rather than Android virtual-node APIs. Automated Swing tests cover the controls, and the owner-reported 2026-09-07 packaged Windows gate covered keyboard/focus and pre-Stage-7 mouse behavior. This repair did not independently rerun that gate; screen-reader certification remains NOT CLAIMED. | DesktopAdaptivePolicyTest and DesktopSessionContractTest cover cell counts, names, focusability, contrast, and child-directed mouse regressions; DESKTOP_BETA_READINESS.md records the owner-reported manual acceptance separately from automated evidence. |
| L14 | Adaptive and scaled layout | AndroidUiPolicy and ScreenLayout handle compact layouts, large text, safe insets, and 48dp controls. | AndroidAdaptiveUiTest covers compact AVD, large text, both themes, headings, and focus order. | MainFrame has a minimum usable size, a vertically scrollable Home card, resizable learning dialogs, stacked compact size actions, and a BoardPanel that recomputes square cell bounds as the window changes. | PARITY | The owner-reported 2026-09-07 packaged Windows gate covered 100%, 125%, and 150% scaling plus larger-text/adaptive behavior. This repair did not independently rerun that gate; Swing layout evidence remains separate from Android dp evidence. | DesktopAdaptivePolicyTest covers minimum size and theme contrast; DESKTOP_BETA_READINESS.md records the owner-reported scaling/adaptive acceptance separately from automated package evidence. |

### Platform-specific mechanics (not direct semantic parity defects)

| ID | Capability or mechanism | Canonical/shared-core support | Android source of truth | Desktop equivalent or current behavior | Status | Required boundary; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| S1 | Haptic feedback | None; haptics are device output. | KlotskiView.performHapticFeedback and AndroidGameStore haptic preference. | No haptic hardware contract exists for Swing; optional keyboard/click or sound feedback is the desktop equivalent. | PLATFORM-SPECIFIC | Keep haptic setting Android-only unless a desktop device API is deliberately approved. A missing haptic pulse must never change moves, timer, or records. | Android settings/flow tests; document the desktop equivalent in Preferences rather than faking haptics. Risk is OS-specific hardware behavior. |
| S2 | Android system file picker | None; picker is an Android integration surface. | MainActivity ACTION_CREATE_DOCUMENT/ACTION_OPEN_DOCUMENT and onActivityResult wrap archive I/O. | Desktop uses a native Swing `JFileChooser` from Preferences for the same versioned archive contract. | PLATFORM-SPECIFIC | The chooser mechanism differs, but archive validation, full-replacement confirmation, and no-change-on-invalid semantics align with L11. | Android backup tests and desktop chooser/invalid-input smoke. Risk is permissions and cancellation; keep picker behavior outside shared core. |
| S3 | Touch gestures and virtual accessibility nodes | Shared move outcomes only; touch thresholds and virtual node IDs are UI mechanics. | KlotskiView touch threshold, swipe direction, AccessibilityNodeProvider, and virtual per-cell actions. | BoardPanel keeps mouse/drag input and adds native Swing child buttons for keyboard/accessibility actions; the input mechanism remains desktop-specific while legal outcomes use GameModel. | PLATFORM-SPECIFIC | Do not copy Android virtual-node or touch code. Qualify equivalent legal outcomes and actionable focus separately by platform; input-specific thresholds may differ. | Android accessibility/whole-line tests and DesktopAdaptivePolicyTest/DesktopSessionContractTest plus the packaged keyboard checklist. |

### Packaging and release-facing behavior

| ID | Capability | Canonical/shared-core support | Android source of truth | Desktop source and verified current behavior | Status | Required parity; persistence and record semantics | Tests, documentation, dependencies, and risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| R1 | Release/package behavior visible to players | Shared core and versioned data contracts must remain compatible across packages. | Android debug/release APK/AAB, Play-readiness checks, connected smoke, and Android README checklist. | Desktop ZIP and optional app-image package with user-data path notes; the final extracted ZIP launch, backup/restore, cancellation/invalid-input, collision, relaunch-retention, and 100%/125%/150% scaling checks were accepted by the owner on 2026-09-08. The package remains unsigned and local. | PARITY | Both packages launch the qualified flows, preserve user-data locations and migration behavior, and state platform-specific limitations. Signing, store assets, and installer/public-distribution choice remain release governance, not parity implementation. | ci.bat, DESKTOP_BETA_READINESS.md, android README/checklist, package scripts, automated archive/package tests, and owner-reported extracted-Windows runtime evidence. Screen-reader certification remains NOT CLAIMED. |

## Matrix result

The 45 rows above are counted by the status cell, not by the number of source
files:

| Status | Count | Interpretation |
| --- | ---: | --- |
| PARITY | 41 | Shared rules, sizes, difficulty/session identity, exact replay, active timer, input outcomes, animation locks, undo/redo, restart, movable and strategic assist, scoped best records, assisted-result protection, Daily, Favorite Practice, Trends/Weekly Goal, Continuous behavior, Move History, Solver Tools, localization, Home routes, learning flows, Quick Reminder, Desktop preferences, themes, sound, reduced motion, full personal backup/restore, accessibility semantics, playable-board behavior, adaptive/scaled layout, and owner-accepted extracted-package release behavior are evidenced on both platforms. |
| PARTIAL | 0 | No counted Android-backed capability remains partially qualified. The R1 extracted-package runtime gate is recorded as owner-reported PASS; signed installers, public distribution, and screen-reader certification remain separate limitations. |
| MISSING | 0 | No currently counted Android-backed capability is missing a Desktop implementation. |
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
3. Desktop accessibility metadata, keyboard actions, and the owner-reported
   packaged keyboard/focus/mouse gate are recorded separately from
   screen-reader certification, which is intentionally not claimed.
4. R1: package build, exact ZIP contents, static readiness evidence, and the
   owner-reported extracted-package GUI/DPI gate are recorded separately from
   Android lifecycle evidence. The owner gate passed on 2026-09-08; signed
   installers and store distribution remain separately governed.

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
    qualification source; Stage 6 added contract comments for the new
    Desktop preference, theme, locale, learning, and tutorial APIs.
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

### Stage 5 verified implementation

The Stage 5 Personal Play work is implemented and verified within the approved
issue boundary:

- `SaveManager` stores up to 50 exact `FavoritePuzzle` identities in
  `klotski_favorites.json`. Labels can be renamed, entries can be removed, and
  the same size/difficulty/initial-grid identity is deduplicated. Practice
  progress and its assistance marker use `klotski_favorite_<identity>` files.
- Favorite Practice routes Desktop autosave and Results through that isolated
  namespace. Its completions never update normal or Daily saves, records,
  completion history, lifetime statistics, or Daily streaks.
- Trends and Weekly Goal reuse the shared `PersonalTrend` and
  `WeeklyGoalProgress` calculations. Player-only samples are filtered by the
  selected size/difficulty; target and scope are persisted in
  `klotski_personal_preferences.json`. MainFrame exposes summary, scope, and
  target dialogs with insufficient-data wording.
- Continuous Challenge supports 3, 5, and 10 puzzles at one fixed
  size/difficulty. Current board and aggregate are stored separately in
  `klotski_continuous_meta.json` and `klotski_continuous_current.json`.
  Start/resume/next/end routes are exposed from Home and Game. Each claimed
  puzzle contributes one shared completion sample; assisted puzzles do not
  replace player bests, and ending the session removes only continuous files.
- Focused tests cover favorite identity/deduplication/removal and namespace
  isolation, trend/goal scope and exclusion, continuous round-trip/clear, and
  the new Desktop presentation copy. Manual Swing cross-mode smoke remains an
  explicit acceptance follow-up.

### Stage 6 verified implementation

The Stage 6 learning, navigation, and preferences work is implemented and
verified within the approved issue boundary:

- `DesktopLearningContent` supplies four localized onboarding pages, interactive
  practice copy, How to Play guidance, and a compact Quick Reminder. The
  onboarding flag is persisted independently from game data; the practice
  board uses an isolated `GameModel` and `DesktopTutorialProgress` state machine
  so it cannot create completion history or records.
- MainFrame Home/Help/Game routes now expose Beginner Guide, Practice Tutorial,
  Quick Reminder, and all previously completed Personal Play destinations. The
  reminder and learning dialogs use the existing timer-pause boundary and
  return to the same active mode.
- `SaveManager` persists reduced motion, optional sound, `midnight`/`ocean`
  theme, `en`/`zh-TW`/`ja-JP` language, and onboarding completion in the
  existing personal-preferences JSON namespace. `BoardPanel` applies the
  selected palette without touching model state; optional Toolkit feedback is
  presentation-only.
- Preferences provides explicit confirmations for saved-game reset and records
  reset. Saved-game reset removes normal/Daily/Favorite Practice/Continuous
  progress and legacy normal save candidates while preserving favorite labels,
  records/statistics, Daily history, and preferences. Its reset marker also
  prevents a project-root legacy fallback from resurrecting cleared games.
  Records reset clears scoped records/statistics/Daily
  progress, masks legacy size-only records, and preserves active Continuous
  state. Full archive backup/restore remains a later stage.
- Focused tests cover locale/theme catalogs, onboarding/learning copy, tutorial
  milestone state, preference round-trip, reset-domain isolation, and legacy
  record masking. Root tests and desktop compile/Javadocs are required gates;
  manual Swing DPI/accessibility smoke remains Stage 7 evidence.

### Stage 7 implementation and automated verification

The Stage 7 accessibility and adaptive-layout work is implemented within the
approved issue boundary. Automated verification is green. A production-
equivalent app-image is observable through the Windows automation surface; its
default-scale keyboard, dialog, theme, persistence, and maximize/restore flows
passed. The owner subsequently reported PASS for the required extracted ZIP
`SlideDo.bat` launch, 100%/125%/150% scaling, larger-text/adaptive behavior,
keyboard/focus behavior, and pre-Stage-7 mouse checks on 2026-09-07. The
owner-reported manual acceptance is separate from automated evidence; the PR
#25 repair did not independently rerun that gate.

- `BoardPanel` keeps the painted board as the visual source of truth while
  exposing one native Swing `JButton` child per cell. Each child has an
  accessible name/description, row-major Tab traversal, Space/Enter activation,
  a visible theme-aware focus border, and the existing arrow-key move bindings.
  `AccessibleCellButton` translates its mouse events back to the established
  `BoardPanel` press/release/click/move/swipe path and bypasses default mouse
  button dispatch, so the keyboard/action path remains available without a
  duplicate mouse action. The child action still calls
  `GameModel.slideLineTo`; it does not duplicate puzzle rules.
- `MainFrame` labels the status, Home, menus, learning dialogs, tutorial board,
  preference controls, and reset actions for Swing accessibility. Home uses a
  vertical scroll viewport, compact size actions, and a predictable first
  focus target. Learning dialogs are resizable and scroll their copy; the
  minimum frame target is 460x560 pixels.
- `DesktopAdaptivePolicy` centralizes the minimum window/focus-target contract
  and contrast-ratio checks. Both persisted Desktop themes now meet the tested
  normal-text tile/home thresholds and the focus-indicator threshold without
  touching model, timer, records, or persistence semantics.
- Focused tests cover the nine/16/25-cell accessibility surface, cell names and
  focusability, minimum-window policy, known contrast ratios, theme contrast,
  existing session behavior, and mouse events dispatched to the actual child
  controls (including single-action, invalid-input, cursor, drag/release, and
  completion-callback contracts). `DESKTOP_BETA_READINESS.md` records the
  automated evidence and the owner-reported manual acceptance separately;
  screen-reader certification remains NOT CLAIMED.

The Stage 7 implementation did not add the native file chooser or archive
format. Stage 8 now supplies the Desktop chooser/archive contract. The
owner-reported extracted-package GUI/DPI execution is recorded in the Stage 8
evidence below; it was not executed by Codex, and screen-reader certification
remains unclaimed.

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

Status: implemented and verified on `main` after the protected Stage 6 merge;
the completion gate below remains historical acceptance context.

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

Status: implementation complete with automated verification and owner-reported
manual acceptance for the packaged GUI/DPI gate on 2026-09-07. The PR #25
repair did not independently rerun that gate, and screen-reader certification
remains NOT CLAIMED. Stage 8 archive/package implementation is now complete;
the owner-reported Stage 8 extracted-package gate is recorded separately below.

- Objective: qualify desktop accessibility and resizable/large-font behavior
  without copying Android virtual-node mechanics.
- Behavioral boundary: Swing accessibility makes board actions, status, menus,
  learning dialogs, and settings discoverable; supported DPI/font/window sizes
  retain complete controls; existing window inactivity/close behavior continues
  to map to Desktop pause/save policy.
- Implemented components: BoardPanel cell controls, MainFrame focus and dialog
  metadata, DesktopAdaptivePolicy, contrast-safe DesktopTheme palettes, and
  the documented beta accessibility checklist.
- Verification: focused accessibility/contrast and child-directed mouse
  regression tests, desktop compile, Javadocs, `git diff --check`, and `ci.bat`
  pass. The production-equivalent app-image manual review passed the
  default-scale keyboard/dialog/theme and persistence subset. The owner also
  reported PASS for the extracted ZIP launch, 100%/125%/150% resize/DPI,
  larger-text/adaptive, keyboard/focus, and pre-Stage-7 mouse checks on
  2026-09-07. The PR #25 repair did not independently rerun that gate;
  screen-reader certification is not claimed. Stage 8 now owns the separate
  archive/chooser and package GUI evidence.
- Out of scope: Android accessibility rewrites, iOS/tablet claims, and visual
  pixel-diff equivalence.
- Completion gate: a reproducible desktop evidence bundle demonstrates
  actionable board controls and complete keyboard focus order while preserving
  pause/save, rules, records, and preferences.

### Stage 8 - Packaging and release qualification

- Objective: synchronize user-facing documentation and qualify the Desktop
  personal-data archive plus ZIP package alongside Android package behavior.
- Implementation status: the version-1 Desktop archive, explicit namespace
  registry, bounded codec, inner semantic validation, logical recovery
  resolution, full replacement, durable legacy suppression, project-root
  fallback isolation, legacy-only preservation, recoverable rollback
  transaction, collision-safe export, stale Preferences-editor invalidation,
  localized Swing chooser controls, and exact ZIP whitelist are implemented
  and covered by headless tests/static gates.
- Restore outcomes are explicit: ordinary success and cleanup-warning success
  both invalidate stale editor generations and reconcile the imported target;
  a recovery-required rollback failure retains both recovery paths, invalidates
  the editor, and locks gameplay, focus-loss/window-close/manual/mode/
  Preferences/export persistence until restart or an owner-led recovery.
- Behavioral boundary: package launch, user-data paths, migration, update or
  reinstall retention, and platform-specific limitations are documented;
  signing and distribution remain separately approved release decisions.
- Likely files/components: package-desktop.bat, desktop package templates,
  DESKTOP_BETA_READINESS.md, android release checks, README and final docs.
- Tests to add/change: fresh install, upgrade/retention, uninstall/reinstall,
  launch, save migration, accessibility, archive replacement, recovery-source
  precedence, project-root/data-dir legacy provenance, collision policy,
  Continuous cross-file ownership, stale-editor invalidation, restore outcome
  policy, and rollback checks in a recoverable Windows environment;
  Android connected evidence remains distinct. Headless archive/rollback tests
  are complete for replacement writes, managed deletes, final validation,
  rollback failure retention, and cleanup warnings.
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
  Current status is automated/archive qualified plus owner-reported PASS for
  the extracted Windows GUI/DPI run on 2026-09-08. Codex did not execute the
  GUI tests; the package remains unsigned/local and screen-reader certification
  remains NOT CLAIMED.

### Stage 9 - Professional Java documentation

Status: completed on 2026-09-08 as a documentation-only qualification from
`b90f2813854ec630d74784b13aa5b21d8af9f473`. No executable Java behavior, API
signature, test, resource, build, CI, dependency, toolchain, package, version,
or release behavior changed.

- Audited all 71 Java files under `src/com/klotski/core`, `src/com/klotski/ui`,
  and `android/app/src/main/java/com/klotski/android`; changed Java files add
  only contract Javadocs or non-obvious invariant comments.
- Documented shared-core ownership, action history and timer semantics,
  persistence/recovery and namespace boundaries, assisted-record eligibility,
  Swing EDT/input-lock/accessibility ownership, and Android lifecycle,
  SharedPreferences, archive, recreation, and virtual-node boundaries.
- Synchronized current governance and user-facing/release documents. The
  current three-locale resource check has 331 common keys; the dated Stage 7
  result of 335 keys remains historical evidence rather than being rewritten.
- Recounted all 45 matrix rows: 41 PARITY, 0 PARTIAL, 0 MISSING, and 4
  PLATFORM-SPECIFIC (G7, S1, S2, S3). Stage 7 and Stage 8 owner-reported
  GUI/DPI evidence remains separate from automated checks, and screen-reader
  certification remains NOT CLAIMED.
- Required public `-Xdoclint:all` Javadoc gates passed with zero errors and
  zero warnings. `verify-toolchain.ps1`, `verify.bat`, `package-desktop.bat`,
  `check-desktop-beta-readiness.bat`, `verify-release.bat`, and `ci.bat` all
  passed locally, including shared tests, Desktop compile, Android assemble,
  test APK, lint, package whitelist, release-readiness, and artifact checks.
  The repository's supported contract remains JDK 17 / AGP 8.13.2 /
  Gradle 8.14.5 / SDK 36 / build-tools 36.0.0; the existing local helper
  fallback to an installed JDK 25 for Javadoc/package commands was not changed
  because Stage 9 is documentation-only. The first pushed Stage 9 documentation
  head `97551a479d3473b38220503e985ff35c933ae6ef` passed Actions run
  `34150736345`; that historical run is not a final-head claim. Exact
  merge-candidate GitHub Actions status is verified externally from PR #27 after
  the final documentation commit. Public distribution and signing remain
  deferred.

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
- Stages 1-8 archive/package implementation is recorded with automated
  evidence plus the owner-reported Stage 7 and Stage 8 packaged GUI/DPI
  acceptance. Stage 9 is complete; umbrella Issue #4 remains open for
  independent final acceptance.
