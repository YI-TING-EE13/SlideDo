@echo off
setlocal enabledelayedexpansion
goto :main

:require_file
if exist "%~1" (
    echo OK: %~2
) else (
    echo FAIL: missing %~2: %~1
    set "FAIL=1"
)
exit /b 0

:require_text
findstr /C:"%~2" "%~1" >nul 2>nul
if errorlevel 1 (
    echo FAIL: missing %~3 in %~1
    set "FAIL=1"
) else (
    echo OK: %~3
)
exit /b 0

:main
set "ANDROID_ROOT=%~dp0"
set "ANDROID_ROOT=%ANDROID_ROOT:~0,-1%"
for %%i in ("%ANDROID_ROOT%\..") do set "REPO_ROOT=%%~fi"
set "VERSION_FILE=%REPO_ROOT%\version.properties"
set "MANIFEST=%ANDROID_ROOT%\app\src\main\AndroidManifest.xml"
set "FAIL=0"

echo Checking Android Play Store readiness files.

if exist "%VERSION_FILE%" (
    for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
    for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_CODE=" "%VERSION_FILE%"') do set "VERSION_CODE=%%b"
)

echo [1/7] Release metadata
call :require_file "%VERSION_FILE%" "version.properties"
if not defined VERSION_NAME (
    echo FAIL: VERSION_NAME is missing from version.properties.
    set "FAIL=1"
) else (
    echo OK: VERSION_NAME=%VERSION_NAME%
)
if not defined VERSION_CODE (
    echo FAIL: VERSION_CODE is missing from version.properties.
    set "FAIL=1"
) else (
    echo OK: VERSION_CODE=%VERSION_CODE%
)
if defined VERSION_NAME call :require_file "%REPO_ROOT%\release-notes\%VERSION_NAME%.md" "release notes for %VERSION_NAME%"

echo [2/7] Store readiness drafts
call :require_file "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "Play Store readiness draft"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "## Store Listing Draft" "store listing draft section"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "## Privacy Policy Draft" "privacy policy draft section"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "## Data Safety Draft" "Data Safety draft section"
findstr /C:"## Telemetry And Crash Reporting Decision" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing telemetry decision section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: telemetry decision section
findstr /C:"## Pre-launch Device Matrix" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing pre-launch matrix section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: pre-launch matrix section
findstr /C:"## Screenshot Review Worksheet" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing screenshot review worksheet section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: screenshot review worksheet section
findstr /C:"## Accessibility Review Worksheet" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing accessibility review worksheet section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: accessibility review worksheet section
findstr /C:"## Pre-launch Evidence Log" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing pre-launch evidence log section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: pre-launch evidence log section
findstr /C:"## Release Checklist" "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" >nul 2>nul
if errorlevel 1 (echo FAIL: missing release checklist section in %ANDROID_ROOT%\PLAY_STORE_READINESS.md& set "FAIL=1") else echo OK: release checklist section

echo [3/7] Launcher icons and manifest
call :require_file "%MANIFEST%" "AndroidManifest.xml"
call :require_text "%MANIFEST%" "@mipmap/ic_launcher" "manifest adaptive launcher icon"
call :require_text "%MANIFEST%" "android:roundIcon" "manifest round launcher icon"
call :require_file "%ANDROID_ROOT%\app\src\main\res\drawable\ic_launcher_foreground.xml" "adaptive icon foreground"
call :require_file "%ANDROID_ROOT%\app\src\main\res\mipmap-anydpi\ic_launcher.xml" "adaptive launcher icon"
call :require_file "%ANDROID_ROOT%\app\src\main\res\mipmap-anydpi\ic_launcher_round.xml" "round adaptive launcher icon"
call :require_file "%ANDROID_ROOT%\app\src\main\res\values\colors.xml" "launcher icon background color"

echo [4/7] Privacy and Data Safety assumptions
findstr /C:"android.permission.INTERNET" "%MANIFEST%" >nul 2>nul
if not errorlevel 1 (
    echo FAIL: AndroidManifest.xml declares INTERNET, but Data Safety draft says the app has no online services.
    set "FAIL=1"
) else (
    echo OK: AndroidManifest.xml does not declare INTERNET.
)
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "Decision for `0.2.0-beta.1`: do not include analytics, crash reporting," "first beta telemetry decision"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "telemetry, ads SDKs, accounts, cloud save, or third-party tracking." "first beta local-only telemetry scope"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "no ads, accounts, analytics, crash reporting, telemetry, cloud" "local-only product claim"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "Does the app collect or share user data? No." "Data Safety collection answer"

echo [5/7] Store asset sources and screenshot workflow
call :require_file "%ANDROID_ROOT%\store-assets\README.md" "store asset source notes"
call :require_file "%ANDROID_ROOT%\store-assets\feature-graphic-1024x500.svg" "feature graphic source"
call :require_file "%ANDROID_ROOT%\export-store-assets.bat" "store asset export workflow"
call :require_file "%REPO_ROOT%\tools\StoreAssetExporter.java" "store asset exporter source"
call :require_file "%REPO_ROOT%\generate-release-manifest.bat" "release manifest workflow"
call :require_file "%REPO_ROOT%\tools\WriteReleaseManifest.ps1" "release manifest writer"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "Feature graphic source:" "feature graphic source entry"
call :require_text "%ANDROID_ROOT%\store-assets\README.md" "feature-graphic-1024x500.svg" "feature graphic source documentation"
call :require_file "%ANDROID_ROOT%\screenshot-smoke.bat" "screenshot smoke workflow"
call :require_file "%ANDROID_ROOT%\check-screenshot-set.bat" "screenshot set verification workflow"
call :require_text "%ANDROID_ROOT%\check-screenshot-set.bat" "Manual review status: pending" "screenshot manifest review status"
call :require_text "%ANDROID_ROOT%\check-screenshot-set.bat" "Review checklist:" "screenshot manifest review checklist"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "01-home.png" "required Home screenshot entry"
call :require_text "%ANDROID_ROOT%\PLAY_STORE_READINESS.md" "07-results-or-current.png" "required Results/current screenshot entry"

echo [6/7] Release and store artifacts
call :require_file "%ANDROID_ROOT%\app\build\outputs\apk\release\app-release.apk" "release APK"
call :require_file "%ANDROID_ROOT%\app\build\outputs\bundle\release\app-release.aab" "release AAB"
if defined VERSION_NAME call :require_file "%REPO_ROOT%\dist\store-assets\android\%VERSION_NAME%\feature-graphic-1024x500.png" "exported feature graphic PNG"
if defined VERSION_NAME call :require_file "%REPO_ROOT%\dist\release-manifests\%VERSION_NAME%.txt" "release artifact manifest"

echo [7/7] Sensitive release files
git -C "%REPO_ROOT%" rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo WARN: Git is not available; skipped tracked secret checks.
) else (
    git -C "%REPO_ROOT%" ls-files --error-unmatch android/release.properties >nul 2>nul
    if not errorlevel 1 (
        echo FAIL: android/release.properties is tracked; release signing secrets must remain local.
        set "FAIL=1"
    ) else (
        echo OK: android/release.properties is not tracked.
    )

    set "TRACKED_KEYSTORE="
    for /f "delims=" %%f in ('git -C "%REPO_ROOT%" ls-files "*.jks" "*.keystore" 2^>nul') do (
        set "TRACKED_KEYSTORE=1"
        echo FAIL: tracked keystore-like file: %%f
    )
    if defined TRACKED_KEYSTORE (
        set "FAIL=1"
    ) else (
        echo OK: no tracked .jks or .keystore files.
    )
)

echo.
echo Manual blockers that this local check cannot complete:
echo   - Configure and verify the real Play upload key before store upload.
echo   - Publish and review the privacy policy URL.
echo   - Review the generated Play Store feature graphic upload file.
echo   - Capture, review, and select final Play Store screenshots.
echo   - Run manual TalkBack, touch-target, contrast, and reduced-motion review.
echo   - Run the pre-launch device matrix in PLAY_STORE_READINESS.md.

if "%FAIL%"=="0" (
    echo Play Store readiness file check passed.
) else (
    echo Play Store readiness file check failed.
)
exit /b %FAIL%
