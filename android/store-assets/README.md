# Android Store Assets

This directory contains version-controlled source assets for the Google Play
store listing. Generated upload files are written under `../../dist/` by the
release workflow and should not be committed.

## Feature Graphic

- Source: `feature-graphic-1024x500.svg`
- Generated PNG: `../../dist/store-assets/android/<version>/feature-graphic-1024x500.png`
- Final Play Console target: 1024x500 JPEG or 24-bit PNG without alpha.
- Alt text draft: `SlideDo sliding puzzle board with 3x3, 4x4, and 5x5 offline play modes.`

To export the PNG directly:

```bat
..\export-store-assets.bat
```

The root `verify-release.bat` command also runs this export before the Play
Store readiness check.

Before uploading the final feature graphic:

- Review the result at phone scale so the board and title remain legible.
- Keep the focal game board and title away from edge cutoff areas.
- Do not add Play badges, ranking claims, pricing claims, or time-sensitive copy.

## Screenshots

Use `../screenshot-smoke.bat` to capture repeatable app screenshots from a
connected emulator or device, then choose the final Play Console images from
`../../screenshots/android/<version>/`.
