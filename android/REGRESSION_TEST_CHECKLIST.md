# Android Regression Test Checklist

This checklist is the acceptance baseline for Android UI, gameplay, persistence,
and localization changes. Run every locale-dependent item in English,
Traditional Chinese, and Japanese. Record the AVD, Android version, APK hash,
locale, and pass/fail result with any evidence before release.

## Test Matrix

| Dimension | Required coverage |
| --- | --- |
| Language | English (`en`, default), Traditional Chinese (`zh-TW`), and Japanese (`ja-JP`) |
| Device | Standard phone AVD and compact phone AVD |
| Android | At least the project target test image and one supported older image when available |
| App state | Fresh install, existing save and records, relaunch, rotation, background/resume |

English must remain the default after a fresh install even when the device uses
another language. A stored supported app language must override the device
language. A missing, blank, or unsupported stored language must fall back to
English without crashing.

## Evidence Commands

Run from the repository root unless noted otherwise.

```bat
android\gradlew.bat -p . test
cd android
build-debug.bat :app:assembleDebug :app:lintDebug :app:connectedDebugAndroidTest
adb shell am start -n com.klotski.android/.MainActivity
```

With both AVDs online, `verify-connected.bat` lets Gradle run the isolated test
package on each device. If either emulator becomes unstable, rerun serially with
an explicit device serial before classifying the result as an app failure.
Capture `adb devices -l`, the APK SHA-256, and the final test summary.

## A. Build, Resources, and Static Checks

- [ ] Shared core tests pass without changes to puzzle behavior.
- [ ] Debug APK assembles and Android lint reports no new errors.
- [ ] Public core/desktop Javadocs and Android API comments pass doclint.
- [ ] English, `values-zh-rTW`, and `values-ja-rJP` resources have matching
  string, plural, and string-array keys.
- [ ] Every format string keeps compatible argument indexes and types in all supported locales.
- [ ] No locale-specific user-visible text is hard-coded in Android Java sources.
- [ ] The locale registry declares `en`, `zh-TW`, and `ja-JP`; adding another locale requires one registry entry and translated resources.
- [ ] Generated builds, AVD files, local preferences, and machine-specific configuration remain untracked.

## B. Install, Launch, and Language Persistence

Run this section for English, Traditional Chinese, and Japanese unless the item
explicitly tests the fresh-install default.

- [ ] Fresh install exposes the SlideDo launcher icon and launches `MainActivity`.
- [ ] Fresh install starts in English regardless of the current device language.
- [ ] First launch shows onboarding in the active app language.
- [ ] Settings exposes a clearly labeled Language entry and shows the active language.
- [ ] Choosing English applies English to the entire current activity.
- [ ] Choosing Traditional Chinese applies Traditional Chinese to the entire current activity.
- [ ] Choosing Japanese applies Japanese to the entire current activity.
- [ ] Cancelling the language dialog leaves the language and current screen unchanged.
- [ ] Selecting the already-active language does not lose state or create duplicate navigation.
- [ ] Language choice survives activity recreation, process stop/start, and normal app relaunch.
- [ ] Language change preserves save data, records, haptic preference, reduced-motion preference, onboarding state, and last puzzle size.
- [ ] Language change from an active game preserves board, initial grid, move count, elapsed time, and the ability to undo/restart.
- [ ] Blank or unsupported stored language falls back safely to English.
- [ ] App name, dialogs, toasts, content descriptions, and plurals use the active app language.
- [ ] No major screen contains mixed-language labels after switching.

## C. Navigation and Major Screens

Run every item in all supported locales.

### Home and onboarding

- [ ] Home shows title, summary, Play/New Game, learning entries, Favorite
  Puzzles, Trends & Weekly Goal, Continuous Challenge, Settings, Records, and
  Continue only when a valid normal save exists.
- [ ] New Game opens Mode Select; Back/Home returns to Home without creating a game.
- [ ] First-launch onboarding pages show correct progress and support Next, Back, Skip, Practice Tutorial, and Start 3x3.
- [ ] Beginner Guide can be reopened from Home after onboarding is complete.
- [ ] With one save, Home Continue shows its state, size, difficulty, moves,
  time, and age; with multiple saves, it shows the count and opens a localized
  chooser containing each saved size, difficulty, state, moves, and time.

### Daily Calendar

- [ ] Home opens the current Daily Calendar rather than starting a board
  immediately; today and earlier dates are playable and future dates are
  disabled.
- [ ] Previous/Next month navigation uses the active locale, never advances
  beyond the current month, and preserves the selected month across rotation.
- [ ] Ready, completed, in-progress, missed, and future dates have readable
  visual states plus localized screen-reader status and action descriptions.
- [ ] Every date has an independent resumable board and assisted flag; opening
  or saving one date does not replace another daily date or a normal size slot.
- [ ] A valid legacy single-date daily save migrates once into its ISO-date slot
  without overwriting a newer dated save.
- [ ] Historical replay opens the deterministic puzzle for the selected date.
  Recompletion is idempotent, and completing an older date does not move the
  latest/current streak backward or replace a player best with assisted data.
- [ ] The full grid, legend, and Back action remain readable and reachable by
  scrolling at 720x1280 in English, Traditional Chinese, and Japanese.

### Favorite Puzzles

- [ ] Game Menu and Results can save the current exact starting board with a
  trimmed 1–40 character local name; saving the same size, difficulty, and
  `initialGrid` renames one identity instead of creating a duplicate.
- [ ] Home opens the library empty state or newest-first cards with localized
  size/difficulty detail and meaningful Replay, Rename, and Remove descriptions.
- [ ] Replay restores the byte-for-byte starting grid with zero moves and time,
  and isolated practice progress survives rotation and background/resume.
- [ ] Favorite practice never replaces any normal or daily save and never
  updates best records, completion history, lifetime statistics, or streaks.
- [ ] The library retains at most 50 valid identities, ignores malformed rows,
  and round-trips through owner-controlled backup/restore.
- [ ] Rename preserves identity and creation order. Remove deletes only that
  favorite and its practice progress. Reset Saved Games clears practice
  progress but keeps labels; Reset Records leaves both untouched.
- [ ] Empty and populated libraries, three-button cards, naming/removal dialogs,
  and Back remain readable and reachable at 720x1280 in all supported locales.

### Trends and Weekly Goal

- [ ] Home opens Trends & Weekly Goal and Back returns to Home without changing
  the active normal, daily, or favorite-practice game.
- [ ] Weekly progress counts only player completions from local Monday through
  today; assisted, stale, and future-dated entries are excluded.
- [ ] The owner can enter any target from 1 to 50. Blank, zero, negative, and
  greater-than-50 values show validation and do not replace the stored target.
- [ ] All nine size/difficulty scopes are selectable and persist independently
  from the last New Game selection.
- [ ] Trend input includes only player completions from the selected scope.
  Fewer than six results shows a recent average plus the remaining requirement;
  six or more compare equal recent and immediately preceding windows.
- [ ] Improving, steady, and declining move/time wording is localized and uses
  the five-percent steady threshold. Reset Records clears the source history
  while preserving the weekly target and selected scope.
- [ ] Goal and scope preferences round-trip through offline backup/restore.
  Goal, scope, comparison, empty state, dialogs, and Back remain reachable by
  scrolling at 720x1280 in all supported locales.

### Continuous Challenge

- [ ] Home starts or resumes a 3-, 5-, or 10-puzzle session in any of the nine
  size/difficulty scopes and shows the current position plus aggregate totals.
- [ ] Starting a session creates a valid first puzzle. Completing one puzzle
  opens Results with aggregate moves/time and Next Puzzle until the target is met.
- [ ] Home, relaunch, rotation, and background/resume preserve the exact current
  board, session target, position, aggregate totals, and assisted/player counts.
- [ ] Continuous progress is isolated from normal, daily, and favorite-practice
  saves. Starting or resuming it does not replace any other mode's current game.
- [ ] Every completed puzzle updates ordinary completion history and lifetime
  statistics exactly once; assisted puzzles never improve player best records.
- [ ] End Session requires confirmation, clears only continuous progress, and
  returns Home. Repeat Session resets aggregate totals and starts a fresh puzzle
  with the same target, size, and difficulty.
- [ ] Continuous progress round-trips through offline backup/restore. Reset Saved
  Games clears it, while Reset Records leaves the active session playable.
- [ ] Setup, Home summary, in-progress game, intermediate/final Results, and
  confirmation dialogs remain readable and reachable at 720x1280 in all locales.

### Move History and Redo

- [ ] Game controls expose localized Undo and Redo actions; each is disabled
  when its corresponding history is empty or input is locked.
- [ ] Move History shows completed and available Redo counts. Its numbered list
  is oldest-first, labels directions by empty-cell movement, and shows the latest
  50 completed actions while the complete history remains persisted.
- [ ] An adjacent move is one action. A non-adjacent whole-line slide is also one
  action and records its step count without splitting the gesture.
- [ ] Undo transfers exactly one completed action to Redo. Redo restores the
  exact board and one move count; a new valid move clears Redo.
- [ ] Restart clears both histories. Save/Load, rotation, background/resume, and
  offline backup/restore preserve both histories in normal, daily,
  favorite-practice, and continuous modes.
- [ ] A legacy save without history fields and a save with invalid history still
  restore the board safely with empty Undo/Redo histories.
- [ ] Move History pauses active play time. Board animation and solver playback
  prevent Undo/Redo conflicts.
- [ ] Portrait's two control rows and landscape's single row keep the status,
  board, and all actions readable and reachable at 720x1280 in every locale.

### How to Play and tutorial

- [ ] How to Play shows Goal, Tap, Whole-line slide, Swipe, Tools, Records, example boards, and Back.
- [ ] Practice Tutorial shows the expected highlighted tile and lesson progress.
- [ ] First tutorial tap advances the adjacent-move lesson.
- [ ] Whole-line tutorial tap advances to completion and counts the gesture as one move.
- [ ] Restart Lesson restores the tutorial board and progress.
- [ ] Start 3x3 and Home navigate to the expected destinations.

### Mode Select

- [ ] 3x3, 4x4, and 5x5 cards show size class, session guidance, Classic record, and 3x3 recommendation.
- [ ] Each size card opens Relaxed, Classic, and Challenge choices; tapping one starts the requested size/difficulty and updates both last selections.
- [ ] Difficulty changes scramble depth only. Equal size/difficulty/seed inputs reproduce the same solvable grid.
- [ ] Mode Select content fits without clipped controls on the compact AVD in all supported locales.

### Settings and Records

- [ ] Haptic and Reduced Motion switches toggle, persist after relaunch, and keep localized accessibility descriptions.
- [ ] Export backup opens Android's create-document picker with a timestamped
  `.json` name and writes every current save with action/redo history,
  continuous session, favorite, record, statistic, daily field, onboarding
  field, language, and preference.
- [ ] Import backup validates the entire document before showing the localized
  replacement dialog; Cancel preserves current data and Restore replaces it.
- [ ] A malformed, oversized, duplicate-key, or unsupported-version backup is
  rejected without partially replacing current preferences.
- [ ] Restored language, theme, sound, haptic, and Reduced Motion settings apply
  after Activity recreation; restored saves and records remain playable.
- [ ] Reset Saved Games Cancel preserves every size slot; Reset clears all 3x3,
  4x4, and 5x5 slots plus dated, favorite-practice, and continuous progress
  while preserving favorite labels, records, and settings, then shows a
  localized confirmation toast.
- [ ] Reset Records Cancel preserves all data; Reset clears every
  size/difficulty best, lifetime statistic, and recent-history entry while
  preserving saves/settings, then shows a localized toast.
- [ ] Records shows the correct empty state or stored best for all nine
  size/difficulty combinations and explains player-only ranking.
- [ ] Records shows separate overall player and assisted totals, a player-only
  average, and per-size/per-difficulty totals and averages.
- [ ] Recent completions are newest first, visibly distinguish player and
  assisted solves, and use the active locale for date, difficulty, moves, and
  time.
- [ ] Back returns to the originating screen without stale or duplicate content.

## D. Gameplay and State Integrity

Run every item on 3x3, 4x4, and 5x5 in all supported locales where practical.

- [ ] Board starts from a valid scramble generated from the solved state.
- [ ] Tapping an adjacent aligned tile performs one legal move and increments moves by one.
- [ ] Swiping a movable tile toward the empty cell performs the expected move.
- [ ] Tapping a non-adjacent aligned tile slides the whole line, increments moves by one, and creates one undo snapshot.
- [ ] Tapping a non-aligned tile has no effect.
- [ ] Undo restores the exact previous board and decrements moves by one.
- [ ] Redo after Undo restores the exact next board and increments moves by one;
  a new valid action after Undo clears Redo.
- [ ] Restart restores `initialGrid`, zeroes moves and elapsed time, clears Undo
  and Redo history, and does not change board size.
- [ ] Input is ignored while board animation or solver playback is active.
- [ ] Menu Resume returns to the same board; time spent in the menu is excluded from elapsed play time.
- [ ] Quick Reminder, Assist, Solver Tools, and solver dialogs keep elapsed play time paused until the final dialog closes.
- [ ] Save after a move, Restart, then Load restores the current-size slot's
  difficulty, grid, `initialGrid`, moves, elapsed time, active/solved state, and
  completed/redo histories and restart behavior without replacing another size.
- [ ] Load with no save shows the localized no-save toast and keeps the current game valid.
- [ ] Home saves the current puzzle; Continue restores it after relaunch. Three
  different boards can coexist in the 3x3, 4x4, and 5x5 slots.
- [ ] A legacy single-save payload migrates once into its matching size slot;
  an older legacy payload never overwrites a newer matching slot.
- [ ] Rotation preserves screen, board, move count, elapsed time, hint state, and relevant dialog/screen state.
- [ ] How to Play, Settings, Records, Mode Select, Results, and Home do not add time to an active saved game.
- [ ] Background/resume excludes away time and does not create a duplicate move or result.
- [ ] Game title, status, first-move hint, movable-tile hint, timer, best record, and button accessibility labels are localized.
- [ ] Board accessibility description includes size, empty-cell position, highlighted count, row values, and moving state in the active language.
- [ ] The board exposes one virtual accessibility child per cell. Each child
  announces tile/empty state plus row and column; only a currently movable tile
  is clickable, and activating a distant aligned tile performs one whole-line
  move through the shared model.

## E. Assist, Solver, Results, and Records

Run in all supported locales. Use controlled saved boards when a deterministic
result is required.

- [ ] Assist opens Show Movable Tiles and Solver Tools.
- [ ] Show Movable Tiles highlights only legal aligned tiles and updates localized status/accessibility text.
- [ ] Solver Tools explains that assisted results do not replace player records.
- [ ] BFS on 4x4 or larger shows the localized performance warning; Close leaves the game untouched.
- [ ] A* and IDA* warnings appear for their documented large-board cases.
- [ ] A solver failure/timeout shows the localized failure dialog without corrupting the board.
- [ ] A found solution shows the localized move count and supports Close and Animate.
- [ ] Solver playback completes once, blocks conflicting input, and creates exactly one assisted Results transition.
- [ ] Player solve Results shows size, difficulty, moves, time, and correct first/new/unchanged record message.
- [ ] Replay Puzzle restores the exact `initialGrid` at the same size/difficulty with zero moves and elapsed time; it does not reshuffle. New Size opens Mode Select; Home returns Home.
- [ ] After Results rotation/recreation, Replay Puzzle still restores the exact original starting board.
- [ ] Player best compares fewer moves first and lower time second.
- [ ] Solver-assisted completion is labeled assisted and never overwrites the player best.
- [ ] Every completed player or solver-assisted game adds exactly one history
  entry and updates the matching lifetime counters; Results
  rotation/recreation does not duplicate it.
- [ ] Recent history retains at most 50 local entries while lifetime counters
  continue beyond that limit; Records displays the newest 10 entries.

## F. Visual and Accessibility Acceptance

Inspect every major screen in all supported locales on both AVD sizes.

- [ ] Home, onboarding, tutorial, Mode Select, Daily Calendar, Favorite Puzzles,
  Trends & Weekly Goal, Continuous Challenge, How to Play, game, menus/dialogs,
  Move History, Settings, Records, and Results have no clipped, overlapping, ellipsized, or
  off-screen required controls.
- [ ] Text wraps naturally; fixed button rows remain tappable and readable in Traditional Chinese and Japanese.
- [ ] At 1.5x system font size, dense Home/Favorite action groups stack,
  Mode Select metadata remains complete, game actions are not clipped or
  ellipsized, and the compact 720x1280 board stays playable.
- [ ] At a 600dp-plus or 1600x2560 test window, scrollable content has a readable
  centered maximum width and larger gutters instead of stretching edge to edge.
- [ ] Scrolling reaches all content and Back/Home controls on the compact AVD.
- [ ] Screen and board animations follow Reduced Motion; language changes do not alter motion behavior.
- [ ] Touch targets, contrast, selected/highlight states, and focus order remain usable.
- [ ] All standard action controls measure at least 48dp. Button text and icons
  select black or white for at least 4.5:1 contrast on every Midnight/Ocean
  button surface; semantic text roles retain the same minimum on their surfaces.
- [ ] TalkBack-visible content descriptions are meaningful and localized; decorative visuals do not add noisy labels.
- [ ] Screen and section titles are accessibility headings. Game traversal is
  Home, Menu, status, board, Undo, Redo, Restart, then Assist.
- [ ] Singular/plural move counts render correctly in English and validly in Traditional Chinese and Japanese.
- [ ] App remains responsive after repeated language switches, rotations, and navigation loops.

## Acceptance Record

Do not mark the localization work complete until every applicable item above is
checked in all supported locales, failures are fixed and rerun, and any
unavailable device coverage is explicitly recorded as a limitation rather than
reported as passed.

### 2026-08-24 Personal Play 2.0 Stage 7 Adaptive and Accessibility pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `AFD32FC22888D17D02AA104542277516829121A3895737A3E01BB3A61C5C4760` |
| Local verification | Android debug/test APK assembly and lint passed without new compiler warnings; all 335 English, Traditional Chinese, and Japanese resource keys and format signatures match |
| Pixel_7 | All 108 Android tests passed on Android 15 at 1080x2400 in one serial run (7m59s); 0 failed, 0 skipped |
| `small_phone` | All 108 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (8m23s); 0 failed, 0 skipped |
| Adaptive layout | Central policy stacks dense rows from 1.3x text, keeps actions at wrap-content/48dp minimums, and centers bounded content on 600dp-plus windows; separate 1.5x runs passed on both AVDs and a 1600x2560 wide-window run passed on Pixel_7 |
| Accessibility semantics | Screen/section headings, explicit game traversal, localized per-cell virtual nodes, focus clearing, busy/unavailable state, and a playable whole-line accessibility click passed automated coverage |
| Contrast and visual review | Every Midnight/Ocean button surface receives black or white content at 4.5:1 or better; fresh/default and fresh/1.5x Android CLI screenshots on both display sizes showed no clipped required content |
| Runtime coverage | Existing navigation, gameplay, persistence, localization, rotation, background/resume, reduced-motion, and solver-lock flows remained green; the exact final APK installed and cold-launched with `MainActivity` resumed and an empty `AndroidRuntime:E` buffer on both AVDs |

Automated provider tests validate the semantics used by TalkBack, but a broader
manual pass with the actual TalkBack service remains a future public-release
gate rather than a claim of this owner-only stage.

### 2026-08-24 Personal Play 2.0 Stage 6 Move History and Redo pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `4875F3D3341C258ACB4651F049D40385139359123D92E43F65F24D0C01045704` |
| Local verification | All 50 shared tests passed; Android debug/test APK assembly, lint, desktop compile, and both Javadoc/doclint gates passed |
| Pixel_7 | All 102 Android tests passed on Android 15 at 1080x2400 in one serial run (779.487s); 0 failed, 0 skipped |
| `small_phone` | All 102 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (529.091s); 0 failed, 0 skipped |
| Shared action contract | Adjacent and whole-line gestures remain one immutable action; Undo/Redo ordering, new-action clearing, Restart clearing, and validated history reconstruction are covered by shared tests |
| Persistence and compatibility | Desktop JSON and every Android save mode preserve completed/redo histories; backup includes them; legacy or malformed histories fall back safely without rejecting the saved board |
| UI and runtime coverage | Availability-aware Undo/Redo, localized history, rotation, background/resume, solver locks, compact two-row portrait controls, and one-row landscape controls passed automated and manual review on both phone sizes; the exact final APK cold-launched with `MainActivity` resumed and empty `AndroidRuntime:E` buffers on both AVDs |

The dialog displays only the latest 50 completed actions for readability; the
model and save formats retain the complete valid history. Directions consistently
describe movement of the empty cell.

### 2026-08-24 Personal Play 2.0 Stage 5 Continuous Challenge pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `87CA4FA93A96481B2A59D1CB6D231C5C5DBD4C9C9A41DCEC03D6B54EA69AEE05` |
| Local verification | All 47 shared tests passed; Android debug/test APK assembly, lint, desktop compile, and both Javadoc/doclint gates passed |
| Pixel_7 | All 100 Android tests passed on Android 15 at 1080x2400 in one serial run (445.081s); 0 failed, 0 skipped |
| `small_phone` | All 100 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (505.349s); 0 failed, 0 skipped |
| Session contract | Targets are 3, 5, or 10 puzzles in one size/difficulty scope; aggregate totals and the exact current board persist in an isolated namespace and round-trip through backup |
| Android flow | Home starts/resumes the session; Results advances, repeats, or confirms ending; Reset Saved Games clears progress without changing records/settings |
| Record boundary | Every puzzle writes completion history and lifetime statistics once; assisted completions remain visible but cannot replace player bests |
| UI and runtime coverage | Setup, compact Home summary, intermediate/final Results, rotation, background/resume, and all three locales passed automated coverage on both phone sizes; the exact final APK cold-launched with `MainActivity` resumed and empty `AndroidRuntime:E` buffers on both AVDs |

Continuous Challenge does not replace normal, daily, or favorite-practice
saves. Ending a session discards only its current puzzle and aggregate totals;
repeating preserves its target and scope but creates a fresh session.

### 2026-08-23 Personal Play 2.0 Stage 3 Favorite Puzzles pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `D35B3CBA33E1C8A13B1BC35E165D7852C21EA0F5E5F466C8E3BCC3734FD04F83` |
| Local verification | All 40 shared tests passed; Android debug/test APK assembly, lint, desktop compile, and both Javadoc/doclint gates passed |
| Pixel_7 | All 91 Android tests passed on Android 15 at 1080x2400 in one serial run (8m52s); 0 failed, 0 skipped |
| `small_phone` | All 91 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (9m55s); 0 failed, 0 skipped |
| Identity and persistence | SHA-256 identity covers size, difficulty, and exact starting grid; duplicate saves rename; newest 50 valid entries survive backup; practice uses an isolated save namespace |
| Android flow | Game Menu/Results save a name; Home library supports Replay/Rename/Remove; exact replay reaches practice-only Results without changing normal/daily saves, records, statistics, or streaks |
| UI and runtime review | The complete library flow passed on both phone sizes; localized resources remained key/format compatible; both crash buffers were empty |

Favorite labels are local metadata and do not participate in puzzle identity.
Reset Saved Games removes active favorite practice progress while retaining the
library, and Reset Records never removes either favorite labels or progress.

### 2026-08-23 Personal Play 2.0 Stage 2 Daily Calendar pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `F10B1C056903576C5942F63DBD66AFAC7CA88857979E459F486DADD68706E1AB` |
| Local verification | All 39 shared tests passed; Android debug/test APK assembly and lint passed |
| Pixel_7 | All 84 Android tests passed on Android 15 at 1080x2400 in one serial run (7m36s); 0 failed, 0 skipped |
| `small_phone` | All 84 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (7m31s); 0 failed, 0 skipped |
| Persistence contract | Daily saves and assisted flags are isolated by ISO date; a valid legacy single-date slot migrates safely; completed-date history remains independent from latest-date streak calculation |
| Android flow | Home opens the calendar; current and historical dates open their deterministic board; future dates and future-month navigation are unavailable; selected month survives rotation |
| UI and runtime review | Pixel_7 and compact 720x1280 month grids kept readable touch targets, localized state descriptions, a scrollable legend/Back action, and no crash-buffer entries |

Historical completion is retained for calendar display, but only a completion
newer than the stored latest date may advance or reset the current streak.
Normal size saves, player-best rules, and shared movement behavior are unchanged.

### 2026-08-23 Personal Play 2.0 Stage 1 backup and restore pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `40FD389B702534A9C5578B4DFDC67D45730356ABFD9E0C8D9BA017FC4ADADE3F` |
| Local verification | All 37 shared tests passed; `verify.bat` passed desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | All 79 Android tests passed on Android 15 at 1080x2400 in one serial run (7m46s); 0 failed, 0 skipped |
| `small_phone` | All 79 Android tests passed on Android 16 / API 36.1 at 720x1280 in one serial run (7m10s); 0 failed, 0 skipped |
| Backup contract | Version 1 JSON preserves all SharedPreferences-compatible values, validates before replacement, bounds archive size/count/key length, rejects duplicate keys and unsupported types or versions, and restores missing preferences to app defaults |
| Android flow | Settings exposes Export and Import; system document pickers require no storage permission; Restore requires confirmation and recreates the Activity so imported language/theme/settings apply |
| UI and runtime review | Pixel_7 created and inspected a real exported JSON file and reached the import confirmation; compact Settings kept Export, Import, both reset actions, and Back readable and reachable; malformed JSON produced the localized error and did not change preferences; the automated Restore flow applied the archive and recreated the Activity; both crash buffers were empty |

The backup file is owner-controlled and local. Import replaces the complete
Android preference store after validation; it does not merge selected fields.
Cloud sync, scheduled backups, encryption, and public-store signing remain out
of scope for this stage.

### 2026-08-21 Stage 5 history and personal-statistics pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `C8D78B4CE1E295D20DA80D366BF2940106D9F1C0B9849918519865F435AB2180` |
| Local verification | All 33 shared tests passed; `verify.bat` passed desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | All 63 Android tests passed on Android 15 at 1080x2400 in six isolated batches (19 + 10 + 10 + 8 + 9 + 7); 0 failed, 0 skipped |
| `small_phone` | All 63 Android tests passed on Android 16 / API 36.1 at 720x1280 in the same six batches; 0 failed, 0 skipped |
| Final Stage 5 subset | The final installed APK passed 18/18 store, reset, localized-statistics, player/assisted, and Results-recreation tests on each AVD |
| Persistence contract | The newest 50 completions remain in newest-first history while per-size/per-difficulty lifetime counters continue beyond the limit; malformed or unsupported entries are ignored |
| Android flow | Every player or solver-assisted completion creates one history entry; only player solves update bests; Reset Records clears bests, history, and statistics without clearing saves/settings |
| UI and runtime review | Compact Records scrolled through totals, recent entries, all nine breakdown panels, and Back without clipping or overlap; fresh `AndroidRuntime` and crash buffers were empty after cold launch on both AVDs |

Records displays the newest 10 of the retained 50 entries. Player averages use
player-only move and elapsed-time totals; assisted completions remain visible in
separate counters and history labels. Results writes completion data before it
evaluates the player best, and Activity recreation does not repeat that write.

### 2026-08-20 Stage 4 per-size save pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `56C5B37A1C6E17F9E3EF57B3985E53627FF1D7EE528467D18CC4694CC4587518` |
| Local verification | All 33 shared tests passed; `verify.bat` passed desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | All 58 Android tests passed on Android 15 at 1080x2400 in five isolated batches (16 + 10 + 12 + 10 + 10); 0 failed, 0 skipped |
| `small_phone` | All 58 Android tests passed on Android 16 / API 36.1 at 720x1280 in the same five batches; 0 failed, 0 skipped |
| Persistence contract | Store coverage verifies independent 3x3, 4x4, and 5x5 slots, default/latest loading, legacy migration, newer-slot precedence, metadata ordering, and clearing every slot |
| Android flow | One save continues directly; multiple saves open the correct localized chooser; in-game Load is size-scoped; Results recreation reloads the result size; reset clears all slots but keeps records/settings |
| UI and runtime review | Compact Home and two-save chooser fit at 720x1280 without clipping or overlap; chooser details are readable; `AndroidRuntime:E` filters were empty on both AVDs |

Each slot uses a `save_<size>_` namespace. Reads lazily migrate the previous
single-save fields, then remove them so future writes and reset behavior use one
schema. The no-argument load path prefers the last selected size and otherwise
falls back to the newest valid slot.

### 2026-08-20 Stage 3 exact-puzzle replay pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `5722E65AF47727B2E270E564057D0340D3C376AAB2E7A27888929173A407BEB9` |
| Local verification | All 33 shared tests passed; `verify.bat` passed desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | All 52 Android tests passed on Android 15 at 1080x2400 in five isolated batches (12 + 10 + 10 + 10 + 10); 0 failed, 0 skipped |
| `small_phone` | All 52 Android tests passed on Android 16 / API 36.1 at 720x1280 in the same five isolated batches; 0 failed, 0 skipped |
| Replay contract | Shared coverage verifies restart after a win restores the byte-for-byte starting grid, difficulty, running state, zero moves, and zero elapsed time |
| Android flow | Results Replay Puzzle preserves exact board identity; rotation/recreation keeps the completed save available for the same replay; labels are verified in English, Traditional Chinese, and Japanese |
| UI and runtime review | Final installed Pixel onboarding and compact Home renders had no clipping or overlap; compact dialog scrolling passed; `AndroidRuntime:E` and `ActivityManager:E` filters were empty on both AVDs |

Replay Puzzle intentionally reuses the completed save's `initialGrid`; it never
calls the scramble path. Activity restoration reloads that completed model before
showing Results so configuration changes cannot silently replace puzzle identity.

### 2026-08-20 Stage 2 difficulty-selection pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `F9424A8612F040756E37F02BB042582A630AA979E2C2FCA8C835132C4C598E63` |
| Local verification | All 32 shared tests passed; `verify.bat` passed desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | 51/51 connected tests passed on Android 15 at 1080x2400; 0 failed, 0 errors, 0 skipped; XML time 337.606 seconds |
| `small_phone` | 51/51 connected tests passed on Android 16 / API 36.1 at 720x1280; 0 failed, 0 errors, 0 skipped; XML time 295.657 seconds |
| Core difficulty contract | Three tests cover stable IDs/increasing budgets, Classic compatibility fallback, and deterministic seeded grid reproduction; save tests cover Classic and Challenge round trips |
| Android persistence and flow | Store tests cover last difficulty, independent scoped records, and difficulty save metadata; flow tests cover English/Traditional Chinese/Japanese selection and Challenge Continue restoration |
| Manual UI review | All three choices were visible and tappable at 720x1280; Mode Select scrolled correctly and `4x4 · Challenge` remained a single-line game title with no clipped board or controls |
| Runtime review | `AndroidRuntime:E` logcat filters were empty on both AVDs after the full suite and manual review |

Difficulty is persisted using stable lowercase IDs. Saves or result state without
the new field default to Classic, and legacy size-only best records are exposed
as Classic records. The original `scramble(int)` path remains available and uses
Classic metadata, so desktop and older callers retain their previous behavior.

### 2026-08-20 Stage 1 active-play timer pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `E63E7163B8BB65549D155026E6BCAEBA206165CAE470A30D8E4E9AAD4E2B3329` |
| Local verification | `verify.bat` passed shared tests, desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates; the explicit `--warning-mode all` shared test run was warning-clean |
| Pixel_7 | 49/49 connected tests passed on Android 15 at 1080x2400; 0 failed, 0 errors, 0 skipped; XML time 336.642 seconds |
| `small_phone` | 49/49 connected tests passed on Android 16 / API 36.1 at 720x1280; 0 failed, 0 errors, 0 skipped; XML time 312.410 seconds |
| Core timer contract | Four deterministic tests cover pause/resume, idempotence, saved elapsed restoration, and solved-time exclusion of paused intervals |
| Android timing flows | Five connected tests cover the game menu, nested Quick Reminder, Assist, Settings, and background/resume |
| Manual UI and timing review | Complete Game Menu remained visible at 720x1280; a measured five-second menu stay was excluded from elapsed play time |
| Runtime review | `AndroidRuntime:E` logcat filters were empty on both AVDs after the final suite and manual launch |

Existing save fields remain compatible. The model restores stored elapsed
milliseconds and begins accumulating again only when Android presents an
interactive Game screen. This stage did not change puzzle movement, records,
solver-assisted protection, resources, dependencies, permissions, or the
manifest.

### 2026-08-19 English / Traditional Chinese pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `005BB55941BB6D84D16541806E489BA7098609E7D386205BE23685C1E4912365` |
| Local verification | `verify.bat` passed shared tests, desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | 41/41 connected tests passed on Android 15 at 1080x2400; 0 failed, 0 skipped |
| `small_phone` | 41/41 connected tests passed on Android 16 / API 36.1 at 720x1280; 0 failed, 0 skipped |
| Resource audit | 182/182 string/plural keys and all format placeholders matched |
| Source audit | No user-visible Java string literal, new dependency, permission, or gameplay-core change |
| Manual visual review | English onboarding and Traditional Chinese Home, Settings, Mode Select, and Game reviewed on 720x1280; the wrapped Restart label was corrected to `重來` and rechecked |
| Relaunch/runtime review | Traditional Chinese language and active save persisted after cold relaunch; inspected app process had no `AndroidRuntime` error |

The connected suite combines the established English regression flow with
explicit Traditional Chinese coverage for onboarding, tutorial, Home, Mode
Select, game/status/accessibility text, Quick Reminder, Assist, the 4x4 BFS
warning, How to Play, Settings/reset dialogs, Records, Results, language
switching, active-game preservation, and relaunch persistence. Static resource
parity covers every remaining message and plural without duplicating gameplay
logic per locale.

Limitations: this pass used two emulator profiles, not a physical Android device.
Accessibility was checked through localized content descriptions and UI
hierarchies, not a live TalkBack listening session. Real Play upload signing
remains intentionally deferred and is unrelated to debug localization
acceptance.

### 2026-08-19 Japanese expansion pass

| Evidence | Result |
| --- | --- |
| Debug APK SHA-256 | `F004AEF16AE76402873DF646F400C1B7008EE3FD638A040147FABB12131A6F9C` |
| Local verification | `verify.bat` passed shared tests, desktop compile, Android assemble/test APK/lint, and both Javadoc/doclint gates |
| Pixel_7 | 44/44 connected tests passed on Android 15 at 1080x2400; 0 failed, 0 skipped; final XML reported 235.197 seconds |
| `small_phone` | 44/44 connected tests passed on Android 16 / API 36.1 at 720x1280; 0 failed, 0 skipped |
| Resource audit | 183/183 string/plural keys matched across English, Traditional Chinese, and Japanese; all format placeholders matched |
| Source and scope audit | No locale-specific literal in production Android Java and no dependency, manifest, permission, or shared-core change |
| Manual visual review | Japanese Home, Settings, Mode Select, How to Play, and Game reviewed at 720x1280; Home Tutorial, Restart, and Assist were shortened to `チュートリアル`, `再挑戦`, and `ヒント`, then rechecked as single-line controls |
| Install/runtime review | Final APK installed and foregrounded in Japanese on the visible Pixel_7 AVD; launcher resolution and version `0.2.0-beta.1 (2)` matched; no `AndroidRuntime` error appeared after launch and language switching |

The three Japanese flow tests cover language switching, active-game
preservation, relaunch persistence, onboarding, tutorial, Home, Mode Select,
game status and accessibility text, Quick Reminder, Assist, the 4x4 BFS warning,
How to Play, Settings/reset dialogs, Records, and player Results. The existing
English and Traditional Chinese tests remained in the same 44-test suite.

Pixel_7 twice exited or went offline during automatic-GPU startup before any
test ran. The same Android 15 AVD completed startup with emulator software
graphics, then passed all 44 tests. The startup failure was limited to the AVD
graphics path; no app test failed. Physical-device, live TalkBack, and real Play
upload-signing acceptance remain pending.
