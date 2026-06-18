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

:require_zip_entry
set "ZIP_PATH=%~1"
set "ENTRY_NAME=%~2"
set "SLIDEDO_ZIP_PATH=%ZIP_PATH%"
set "SLIDEDO_ENTRY_NAME=%ENTRY_NAME%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.IO.Compression.FileSystem; $zip=[System.IO.Compression.ZipFile]::OpenRead($env:SLIDEDO_ZIP_PATH); try { if (-not ($zip.Entries | Where-Object { [System.IO.Path]::GetFileName($_.FullName) -eq $env:SLIDEDO_ENTRY_NAME })) { throw ('missing ' + $env:SLIDEDO_ENTRY_NAME) } } finally { $zip.Dispose() }" >nul 2>nul
if errorlevel 1 (
    echo FAIL: desktop ZIP is missing %ENTRY_NAME%.
    set "FAIL=1"
) else (
    echo OK: desktop ZIP contains %ENTRY_NAME%.
)
exit /b 0

:main
set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "VERSION_FILE=%ROOT%\version.properties"
set "READINESS=%ROOT%\DESKTOP_BETA_READINESS.md"
set "FAIL=0"

echo Checking desktop public beta readiness files.

if exist "%VERSION_FILE%" (
    for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
)

if not defined VERSION_NAME (
    echo FAIL: VERSION_NAME is missing from version.properties.
    set "FAIL=1"
    set "VERSION_NAME=unknown"
) else (
    echo OK: VERSION_NAME=%VERSION_NAME%
)

set "PACKAGE_DIR=%ROOT%\dist\desktop\SlideDo-%VERSION_NAME%"
set "ZIP_FILE=%ROOT%\dist\desktop\SlideDo-%VERSION_NAME%.zip"

echo [1/5] Desktop readiness document
call :require_file "%READINESS%" "desktop beta readiness draft"
call :require_text "%READINESS%" "## Current Beta Target" "current beta target section"
call :require_text "%READINESS%" "## Public Beta Blockers" "public beta blockers section"
call :require_text "%READINESS%" "## Package Contents" "package contents section"
call :require_text "%READINESS%" "## Tester Instructions Draft" "tester instructions draft section"
call :require_text "%READINESS%" "## Manual Desktop Smoke Checklist" "manual desktop smoke checklist section"
call :require_text "%READINESS%" "## Manual Desktop Accessibility Review" "manual desktop accessibility review section"
call :require_text "%READINESS%" "## Release Checklist" "release checklist section"

echo [2/5] Package artifacts
call :require_file "%ZIP_FILE%" "desktop ZIP package"
call :require_file "%PACKAGE_DIR%\SlideDo.jar" "desktop package jar"
call :require_file "%PACKAGE_DIR%\SlideDo.bat" "desktop launcher script"
call :require_file "%PACKAGE_DIR%\README.txt" "desktop package README"
call :require_file "%PACKAGE_DIR%\RELEASE_NOTES.md" "desktop package release notes"

echo [3/5] Package text
call :require_text "%PACKAGE_DIR%\README.txt" "Run SlideDo.bat to start the desktop app." "desktop package start instruction"
call :require_text "%PACKAGE_DIR%\README.txt" "Java 17 or newer available on PATH" "desktop Java runtime instruction"
call :require_text "%PACKAGE_DIR%\README.txt" "Saves and records are stored under" "desktop save location instruction"
call :require_text "%PACKAGE_DIR%\README.txt" "\SlideDo on Windows." "desktop Windows save directory"
call :require_text "%PACKAGE_DIR%\README.txt" "What to test:" "desktop package tester checklist"
call :require_text "%PACKAGE_DIR%\README.txt" "Known limits:" "desktop package known limits"
call :require_text "%PACKAGE_DIR%\RELEASE_NOTES.md" "# SlideDo %VERSION_NAME%" "desktop package release notes version"

echo [4/5] ZIP contents
if exist "%ZIP_FILE%" (
    call :require_zip_entry "%ZIP_FILE%" "SlideDo.jar"
    call :require_zip_entry "%ZIP_FILE%" "SlideDo.bat"
    call :require_zip_entry "%ZIP_FILE%" "README.txt"
    call :require_zip_entry "%ZIP_FILE%" "RELEASE_NOTES.md"
)

echo [5/5] Sensitive local files
git -C "%ROOT%" rev-parse --is-inside-work-tree >nul 2>nul
if errorlevel 1 (
    echo WARN: Git is not available; skipped tracked desktop local-data checks.
) else (
    set "TRACKED_LOCAL_DATA="
    for /f "delims=" %%f in ('git -C "%ROOT%" ls-files "klotski_save.json" "klotski_save.dat" "klotski_records.json" "dist/desktop/*.zip" "dist/desktop/SlideDo-*/*" 2^>nul') do (
        set "TRACKED_LOCAL_DATA=1"
        echo FAIL: tracked local/generated desktop file: %%f
    )
    if defined TRACKED_LOCAL_DATA (
        set "FAIL=1"
    ) else (
        echo OK: no tracked desktop local saves or generated desktop packages.
    )
)

echo.
echo Manual blockers that this local check cannot complete:
echo   - Choose the public beta download page.
echo   - Decide whether a signed installer is required before wider distribution.
echo   - Run the manual desktop smoke checklist from DESKTOP_BETA_READINESS.md.
echo   - Run the manual desktop accessibility review from DESKTOP_BETA_READINESS.md.

if "%FAIL%"=="0" (
    echo Desktop public beta readiness file check passed.
) else (
    echo Desktop public beta readiness file check failed.
)
exit /b %FAIL%
