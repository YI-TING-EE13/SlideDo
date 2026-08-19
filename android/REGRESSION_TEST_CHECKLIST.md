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
- [ ] English, `values-zh-rTW`, and `values-ja-rJP` resources have matching string and plural keys.
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

- [ ] Home shows title, summary, Play/New Game, learning entries, Settings, Records, and Continue only when a valid save exists.
- [ ] New Game opens Mode Select; Back/Home returns to Home without creating a game.
- [ ] First-launch onboarding pages show correct progress and support Next, Back, Skip, Practice Tutorial, and Start 3x3.
- [ ] Beginner Guide can be reopened from Home after onboarding is complete.
- [ ] Home Continue summary correctly distinguishes active, saved, and solved state and displays size, difficulty, moves, time, and age.

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
- [ ] Reset Saved Game Cancel preserves the save; Reset clears it and shows a localized confirmation toast.
- [ ] Reset Records Cancel preserves all records; Reset clears every size/difficulty record and shows a localized toast.
- [ ] Records shows the correct empty state or stored best for all nine size/difficulty combinations and explains player-only ranking.
- [ ] Back returns to the originating screen without stale or duplicate content.

## D. Gameplay and State Integrity

Run every item on 3x3, 4x4, and 5x5 in all supported locales where practical.

- [ ] Board starts from a valid scramble generated from the solved state.
- [ ] Tapping an adjacent aligned tile performs one legal move and increments moves by one.
- [ ] Swiping a movable tile toward the empty cell performs the expected move.
- [ ] Tapping a non-adjacent aligned tile slides the whole line, increments moves by one, and creates one undo snapshot.
- [ ] Tapping a non-aligned tile has no effect.
- [ ] Undo restores the exact previous board and decrements moves by one.
- [ ] Restart restores `initialGrid`, zeroes moves and elapsed time, and does not change board size.
- [ ] Input is ignored while board animation or solver playback is active.
- [ ] Menu Resume returns to the same board; time spent in the menu is excluded from elapsed play time.
- [ ] Quick Reminder, Assist, Solver Tools, and solver dialogs keep elapsed play time paused until the final dialog closes.
- [ ] Save after a move, Restart, then Load restores size, difficulty, grid, `initialGrid`, moves, elapsed time, active/solved state, and restart behavior.
- [ ] Load with no save shows the localized no-save toast and keeps the current game valid.
- [ ] Home saves the current puzzle; Continue restores it after relaunch.
- [ ] Rotation preserves screen, board, move count, elapsed time, hint state, and relevant dialog/screen state.
- [ ] How to Play, Settings, Records, Mode Select, Results, and Home do not add time to an active saved game.
- [ ] Background/resume excludes away time and does not create a duplicate move or result.
- [ ] Game title, status, first-move hint, movable-tile hint, timer, best record, and button accessibility labels are localized.
- [ ] Board accessibility description includes size, empty-cell position, highlighted count, row values, and moving state in the active language.

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

## F. Visual and Accessibility Acceptance

Inspect every major screen in all supported locales on both AVD sizes.

- [ ] Home, onboarding, tutorial, Mode Select, How to Play, game, menus/dialogs, Settings, Records, and Results have no clipped, overlapping, ellipsized, or off-screen required controls.
- [ ] Text wraps naturally; fixed button rows remain tappable and readable in Traditional Chinese and Japanese.
- [ ] Scrolling reaches all content and Back/Home controls on the compact AVD.
- [ ] Screen and board animations follow Reduced Motion; language changes do not alter motion behavior.
- [ ] Touch targets, contrast, selected/highlight states, and focus order remain usable.
- [ ] TalkBack-visible content descriptions are meaningful and localized; decorative visuals do not add noisy labels.
- [ ] Singular/plural move counts render correctly in English and validly in Traditional Chinese and Japanese.
- [ ] App remains responsive after repeated language switches, rotations, and navigation loops.

## Acceptance Record

Do not mark the localization work complete until every applicable item above is
checked in all supported locales, failures are fixed and rerun, and any
unavailable device coverage is explicitly recorded as a limitation rather than
reported as passed.

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
