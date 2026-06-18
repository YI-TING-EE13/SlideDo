# SlideDo Release Notes

Each release must have a Markdown file named after `VERSION_NAME` from the root
`version.properties` file.

Release verification expects:

```text
release-notes/<VERSION_NAME>.md
```

Before tagging or distributing a build:

1. Update `version.properties`.
2. Add or update the matching release-notes file.
3. Run `verify.bat`.
4. Run `verify-release.bat`.
5. If an emulator or device is available, run `verify-connected.bat`.
6. For Android store handoff, complete the worksheets in
   `android/PLAY_STORE_READINESS.md`.
7. For desktop public beta, complete `DESKTOP_BETA_READINESS.md`.

The Android release script copies the version into the APK/AAB metadata through
Gradle. The desktop packaging script copies the matching release notes into the
desktop ZIP package. Release verification checks both Android Play Store
readiness and desktop public beta readiness after artifacts are generated.
