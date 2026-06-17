# Android Store Assets

This directory contains version-controlled source assets for the Google Play
store listing. Final upload files should still be exported, reviewed, and kept
with the release handoff artifacts rather than committed if they are generated.

## Feature Graphic

- Source: `feature-graphic-1024x500.svg`
- Final Play Console target: 1024x500 JPEG or 24-bit PNG without alpha.
- Alt text draft: `SlideDo sliding puzzle board with 3x3, 4x4, and 5x5 offline play modes.`

Before uploading the final feature graphic:

- Export the SVG to a 1024x500 PNG or JPEG.
- Review the result at phone scale so the board and title remain legible.
- Keep the focal game board and title away from edge cutoff areas.
- Do not add Play badges, ranking claims, pricing claims, or time-sensitive copy.

## Screenshots

Use `../screenshot-smoke.bat` to capture repeatable app screenshots from a
connected emulator or device, then choose the final Play Console images from
`../../screenshots/android/<version>/`.
