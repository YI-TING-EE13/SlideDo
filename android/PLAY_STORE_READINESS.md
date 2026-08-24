# SlideDo Play Store Readiness

This file tracks the Android store-submission materials that are not generated
by Gradle. Treat the text below as a product/release draft: review it before
publishing, and keep it aligned with the actual app behavior.

## Current Release Target

- Package name: `com.klotski.android`
- Version source: `../version.properties`
- Current version: `0.2.0-beta.1` (`versionCode` 2)
- Min SDK: 26
- Target SDK: 36
- Release artifacts:
  - `app/build/outputs/apk/release/app-release.apk`
  - `app/build/outputs/bundle/release/app-release.aab`

## Submission Blockers

- Configure a real Play upload key in `android/release.properties`,
  `SLIDEDO_RELEASE_*` environment variables, or Gradle
  `-Pslidedo.release.*` properties. The local temporary key is only for
  pipeline verification.
- Capture and review screenshots on the target device matrix before store
  submission.
- Publish a privacy policy URL and make sure it matches the Data Safety answers
  below.
- Run a manual accessibility pass covering TalkBack, touch targets, color
  contrast, and reduced motion.

## Telemetry And Crash Reporting Decision

Decision for `0.2.0-beta.1`: do not include analytics, crash reporting,
telemetry, ads SDKs, accounts, cloud save, or third-party tracking.

Rationale:

- Keep the first public Android beta local-only and simple to explain.
- Keep the Data Safety answers and privacy policy draft aligned with actual app
  behavior.
- Avoid collecting device, diagnostic, or usage data before there is a reviewed
  privacy policy URL and an explicit consent/product plan.

Release guardrail:

- Adding `android.permission.INTERNET`, analytics/crash SDK dependencies,
  telemetry events, cloud sync, ads, accounts, or third-party tracking requires
  updating this file, the privacy policy, Data Safety answers, store listing,
  release notes, and the release-readiness checks before shipment.

## Store Listing Draft

Short description:

```text
A clean sliding-number puzzle with fast touch controls, records, hints, and guided practice.
```

Full description:

```text
SlideDo is a focused sliding-number puzzle game built around clear movement,
quick retries, and satisfying whole-line slides.

Choose 3x3, 4x4, or 5x5 boards, continue saved games, learn the rules through
guided practice, and track your best records by puzzle size. Tap any tile in the
same row or column as the empty space to slide that line in one move. Undo,
restart, assist hints, and solver playback are available when you want to learn
or experiment.

Features:
- 3x3, 4x4, and 5x5 number sliding puzzles
- Home, mode select, guided practice, How to Play, settings, records, and results
- Whole-line slide input that counts as one move
- Undo, restart, manual save/load, and autosave
- Local best records by puzzle size
- Assist hints for movable tiles
- Solver playback for learning and experimentation
- Haptic and reduced-motion settings

SlideDo stores gameplay state and records locally on your device. The current
beta build has no ads, accounts, analytics, crash reporting, telemetry, cloud
save, or online services.
```

Suggested tags/categories:

- Puzzle
- Casual
- Brain training
- Offline

## Store Assets

- Adaptive launcher icon: present in `app/src/main/res/mipmap-anydpi/`, including
  a monochrome layer for themed icons.
- Round launcher icon: present in `app/src/main/res/mipmap-anydpi/`, including
  a monochrome layer for themed icons.
- Feature graphic source: present in `store-assets/feature-graphic-1024x500.svg`.
  `verify-release.bat` exports the upload PNG to
  `../dist/store-assets/android/<version>/feature-graphic-1024x500.png`.
  Review the generated 1024x500 image before Play Console upload.
- Phone screenshots: capture with `screenshot-smoke.bat`, then review
  `../screenshots/android/<version>/manifest.txt` and select the final Play
  Console images from that screenshot directory.
- Tablet screenshots: optional until tablet layout work starts.
- Promo video: not planned for first beta.

Recommended screenshot set:

1. Home with Continue metadata.
2. Mode Select showing 3x3, 4x4, and 5x5 choices with session guidance.
3. Active 3x3 game with compact controls.
4. Practice Tutorial highlighting a guided move.
5. Results screen with record wording.
6. Settings screen with haptic and reduced-motion controls.

## Privacy Policy Draft

Use this as source text for a public privacy policy page. The final policy
should include an effective date and contact address before publication.

```text
SlideDo Privacy Policy

SlideDo is an offline sliding puzzle game. The current Android beta does not
collect, transmit, sell, or share personal data.

The app stores gameplay data locally on your device, including saved puzzle
state, selected puzzle size, elapsed time, move count, settings, onboarding
state, and local best records. This data is used only to restore your game,
show records, and apply your preferences.

SlideDo does not include accounts, ads, analytics, cloud save, crash reporting,
or third-party tracking in the current beta.

Android system backup may back up app data according to your device and Google
account settings. SlideDo does not operate that backup service or receive the
backed-up data.

You can delete local saved-game data and local records from the Settings screen.
You can also remove all local app data through Android system settings by
clearing storage or uninstalling the app.

If future versions add analytics, crash reporting, cloud save, ads, accounts, or
network features, this policy and the Google Play Data Safety answers must be
updated before release.
```

## Data Safety Draft

These answers reflect the current app implementation: no network permission, no
accounts, no ads, no analytics, no crash reporting, and local-only gameplay
storage.

- Does the app collect or share user data? No.
- Data shared with third parties: No.
- Data collected by the developer: No.
- Data processed ephemerally: Not applicable.
- Data encrypted in transit: Not applicable because the app does not transmit
  user data.
- Users can request data deletion: Local data can be deleted in-app through
  Settings reset actions or through Android system app-storage controls.
- Independent security review: No.
- Children and families policy: Do not target children until the product and
  store listing are reviewed for that audience.

Important note:

Android Auto Backup can back up app data if enabled by the user's device
settings. This is an Android system/user-account feature, not developer data
collection by SlideDo. Re-check Play Console wording when filling the form.

## Pre-launch Device Matrix

Run `verify-connected.bat` and a manual smoke pass on at least:

| Device class | API | Orientation | Checks |
| --- | --- | --- | --- |
| Small phone | 26 or 27 | Portrait | Launch, Home, Mode Select, 3x3 play, save/load |
| Current phone | 35 or 36 | Portrait and landscape | Full connected suite, rotation, background/resume |
| Large phone | 35 or 36 | Portrait | 5x5 layout, Settings, Records, Results |
| Tablet/foldable preview | 35 or 36 | Portrait and landscape | Layout inspection only until tablet UX is designed |

Manual smoke checklist:

- Fresh install opens onboarding before normal Home.
- Home shows Continue metadata after saving a game.
- 3x3, 4x4, and 5x5 games start from Mode Select.
- Adjacent and whole-line moves behave correctly.
- Undo after a whole-line slide restores the whole gesture.
- Save, Restart, and Load restore the expected state and metadata.
- Practice Tutorial highlights the expected tile and advances after the
  whole-line slide.
- Settings switches persist and reset dialogs work.
- Solver-assisted completion reaches Results without overwriting player best
  records.
- TalkBack can identify the board and primary controls.
- Reduced motion disables board transition animation.

## Screenshot Review Worksheet

Record this review before uploading screenshots to Play Console:

- Version and build source: `0.2.0-beta.1` from `version.properties`.
- Screenshot directory:
  `../screenshots/android/0.2.0-beta.1/`.
- Required files: `01-home.png`, `02-mode-select.png`, `03-game-3x3.png`,
  `04-how-to-play.png`, `05-settings.png`, `06-records.png`, and
  `07-results-or-current.png`.
- `manifest.txt` exists and was generated by `check-screenshot-set.bat`.
- Screenshots show real app UI, not mockups or outdated flows.
- Text is readable and not clipped on the captured device.
- No personal data, emulator account notifications, debug overlays, or system
  dialogs are visible.
- The selected Play Console screenshots cover Home, Mode Select, gameplay,
  learning/help, Settings, Records, and Results/current gameplay.

## Accessibility Review Worksheet

Record this review before store submission:

- TalkBack announces the current screen and primary actions.
- TalkBack announces board size, empty-cell position, row-by-row state, and
  highlighted movable tiles.
- Home, Mode Select, Game, Settings, Records, Results, How to Play, and Practice
  Tutorial controls are reachable without relying on hidden gestures.
- Primary tap targets are comfortable on the smallest target phone.
- Board, buttons, highlighted tiles, and status text pass a manual contrast
  review on the target devices.
- Reduced motion disables board transition animation and does not change puzzle
  rules, move count, undo, save/load, or records.
- Haptic settings can be disabled and remain disabled after relaunch.

## Pre-launch Evidence Log

Record the final manual pass here or in release handoff notes:

- `verify.bat` result and date.
- `verify-connected.bat` result, date, device name, API level, and orientation.
- `verify-release.bat` result and date.
- Screenshot smoke directory and manifest path.
- Feature graphic PNG path reviewed for Play Console upload.
- Accessibility reviewer, date, and unresolved issues.
- Pre-launch device matrix reviewer, date, and unresolved issues.

## Release Checklist

Before uploading to Play Console:

- Run `verify.bat`.
- Run `verify-connected.bat` on a healthy emulator or device.
- Run `verify-release.bat`; it builds Android and desktop release artifacts,
  exports store assets, writes the SHA-256 release artifact manifest, and runs
  `android/check-play-store-readiness.bat`.
- Optionally run `check-play-store-readiness.bat` directly after release
  artifacts exist when only Play Store files changed.
- Confirm `app-release.aab` uses the real upload key, not the local temporary
  test key.
- Confirm `../dist/release-manifests/<version>.txt` matches the artifacts being
  uploaded or shared for public testing.
- Review the generated feature graphic from `../dist/store-assets/`.
- Capture and review the screenshot smoke set.
- Confirm `manifest.txt` exists for the screenshot smoke set and all required
  screenshots are readable.
- Update release notes under `../release-notes/`.
- Confirm the privacy policy URL is live.
- Confirm Data Safety answers still match the app behavior.
- Confirm no local-only files such as `android/release.properties` or keystores
  are tracked by Git.
