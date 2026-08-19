# Android Regression Test Checklist

This checklist is the acceptance baseline for Android UI, gameplay, persistence,
and localization changes. Run every locale-dependent item once in English and
once in Traditional Chinese. Record the AVD, Android version, APK hash, locale,
and pass/fail result with any evidence before release.

## Test Matrix

| Dimension | Required coverage |
| --- | --- |
| Language | English (`en`, default) and Traditional Chinese (`zh-TW`) |
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

For two AVDs, run connected tests serially against one emulator at a time so
the package, preferences, and instrumentation lifecycle cannot interfere across
devices. Capture `adb devices -l`, the APK SHA-256, and the final test summary.

## A. Build, Resources, and Static Checks

- [ ] Shared core tests pass without changes to puzzle behavior.
- [ ] Debug APK assembles and Android lint reports no new errors.
- [ ] Public core/desktop Javadocs and Android API comments pass doclint.
- [ ] English and `values-zh-rTW` resources have matching string and plural keys.
- [ ] Every format string keeps compatible argument indexes and types in both locales.
- [ ] No user-visible English or Chinese text is hard-coded in Android Java sources.
- [ ] The locale registry declares `en` and `zh-TW`; adding a future locale requires one registry entry and translated resources.
- [ ] Generated builds, AVD files, local preferences, and machine-specific configuration remain untracked.

## B. Install, Launch, and Language Persistence

Run this section for English and Traditional Chinese unless the item explicitly
tests the fresh-install default.

- [ ] Fresh install exposes the SlideDo launcher icon and launches `MainActivity`.
- [ ] Fresh install starts in English regardless of the current device language.
- [ ] First launch shows onboarding in the active app language.
- [ ] Settings exposes a clearly labeled Language entry and shows the active language.
- [ ] Choosing English applies English to the entire current activity.
- [ ] Choosing Traditional Chinese applies Traditional Chinese to the entire current activity.
- [ ] Cancelling the language dialog leaves the language and current screen unchanged.
- [ ] Selecting the already-active language does not lose state or create duplicate navigation.
- [ ] Language choice survives activity recreation, process stop/start, and normal app relaunch.
- [ ] Language change preserves save data, records, haptic preference, reduced-motion preference, onboarding state, and last puzzle size.
- [ ] Language change from an active game preserves board, initial grid, move count, elapsed time, and the ability to undo/restart.
- [ ] Blank or unsupported stored language falls back safely to English.
- [ ] App name, dialogs, toasts, content descriptions, and plurals use the active app language.
- [ ] No major screen contains mixed-language labels after switching.

## C. Navigation and Major Screens

Run every item in both locales.

### Home and onboarding

- [ ] Home shows title, summary, Play/New Game, learning entries, Settings, Records, and Continue only when a valid save exists.
- [ ] New Game opens Mode Select; Back/Home returns to Home without creating a game.
- [ ] First-launch onboarding pages show correct progress and support Next, Back, Skip, Practice Tutorial, and Start 3x3.
- [ ] Beginner Guide can be reopened from Home after onboarding is complete.
- [ ] Home Continue summary correctly distinguishes active, saved, and solved state and displays size, moves, time, and age.

### How to Play and tutorial

- [ ] How to Play shows Goal, Tap, Whole-line slide, Swipe, Tools, Records, example boards, and Back.
- [ ] Practice Tutorial shows the expected highlighted tile and lesson progress.
- [ ] First tutorial tap advances the adjacent-move lesson.
- [ ] Whole-line tutorial tap advances to completion and counts the gesture as one move.
- [ ] Restart Lesson restores the tutorial board and progress.
- [ ] Start 3x3 and Home navigate to the expected destinations.

### Mode Select

- [ ] 3x3, 4x4, and 5x5 cards show size, difficulty, session guidance, record, and 3x3 recommendation.
- [ ] Each size card starts the requested board size and updates the last selected size.
- [ ] Mode Select content fits without clipped controls on the compact AVD in both locales.

### Settings and Records

- [ ] Haptic and Reduced Motion switches toggle, persist after relaunch, and keep localized accessibility descriptions.
- [ ] Reset Saved Game Cancel preserves the save; Reset clears it and shows a localized confirmation toast.
- [ ] Reset Records Cancel preserves all records; Reset clears 3x3, 4x4, and 5x5 records and shows a localized toast.
- [ ] Records shows the correct empty state or stored best for every size and explains player-only ranking.
- [ ] Back returns to the originating screen without stale or duplicate content.

## D. Gameplay and State Integrity

Run every item on 3x3, 4x4, and 5x5 in both locales where practical.

- [ ] Board starts from a valid scramble generated from the solved state.
- [ ] Tapping an adjacent aligned tile performs one legal move and increments moves by one.
- [ ] Swiping a movable tile toward the empty cell performs the expected move.
- [ ] Tapping a non-adjacent aligned tile slides the whole line, increments moves by one, and creates one undo snapshot.
- [ ] Tapping a non-aligned tile has no effect.
- [ ] Undo restores the exact previous board and decrements moves by one.
- [ ] Restart restores `initialGrid`, zeroes moves and elapsed time, and does not change board size.
- [ ] Input is ignored while board animation or solver playback is active.
- [ ] Menu Resume returns to the same board and timer.
- [ ] Save after a move, Restart, then Load restores size, grid, `initialGrid`, moves, elapsed time, active/solved state, and restart behavior.
- [ ] Load with no save shows the localized no-save toast and keeps the current game valid.
- [ ] Home saves the current puzzle; Continue restores it after relaunch.
- [ ] Rotation preserves screen, board, move count, elapsed time, hint state, and relevant dialog/screen state.
- [ ] Background/resume continues with a valid timer and no duplicate move or result.
- [ ] Game title, status, first-move hint, movable-tile hint, timer, best record, and button accessibility labels are localized.
- [ ] Board accessibility description includes size, empty-cell position, highlighted count, row values, and moving state in the active language.

## E. Assist, Solver, Results, and Records

Run in both locales. Use controlled saved boards when a deterministic result is
required.

- [ ] Assist opens Show Movable Tiles and Solver Tools.
- [ ] Show Movable Tiles highlights only legal aligned tiles and updates localized status/accessibility text.
- [ ] Solver Tools explains that assisted results do not replace player records.
- [ ] BFS on 4x4 or larger shows the localized performance warning; Close leaves the game untouched.
- [ ] A* and IDA* warnings appear for their documented large-board cases.
- [ ] A solver failure/timeout shows the localized failure dialog without corrupting the board.
- [ ] A found solution shows the localized move count and supports Close and Animate.
- [ ] Solver playback completes once, blocks conflicting input, and creates exactly one assisted Results transition.
- [ ] Player solve Results shows size, moves, time, and correct first/new/unchanged record message.
- [ ] Play Again starts the same size; New Size opens Mode Select; Home returns Home.
- [ ] Player best compares fewer moves first and lower time second.
- [ ] Solver-assisted completion is labeled assisted and never overwrites the player best.

## F. Visual and Accessibility Acceptance

Inspect every major screen in both locales on both AVD sizes.

- [ ] Home, onboarding, tutorial, Mode Select, How to Play, game, menus/dialogs, Settings, Records, and Results have no clipped, overlapping, ellipsized, or off-screen required controls.
- [ ] Text wraps naturally; fixed button rows remain tappable and readable in Traditional Chinese.
- [ ] Scrolling reaches all content and Back/Home controls on the compact AVD.
- [ ] Screen and board animations follow Reduced Motion; language changes do not alter motion behavior.
- [ ] Touch targets, contrast, selected/highlight states, and focus order remain usable.
- [ ] TalkBack-visible content descriptions are meaningful and localized; decorative visuals do not add noisy labels.
- [ ] Singular/plural move counts render correctly in English and validly in Traditional Chinese.
- [ ] App remains responsive after repeated language switches, rotations, and navigation loops.

## Acceptance Record

Do not mark the localization work complete until every applicable item above is
checked in both locales, failures are fixed and rerun, and any unavailable device
coverage is explicitly recorded as a limitation rather than reported as passed.

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
