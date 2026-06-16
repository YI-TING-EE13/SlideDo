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

The Android release script copies the version into the APK/AAB metadata through
Gradle. The desktop packaging script copies the matching release notes into the
desktop ZIP package.
