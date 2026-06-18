@echo off
setlocal enabledelayedexpansion

set "ANDROID_ROOT=%~dp0"
set "ANDROID_ROOT=%ANDROID_ROOT:~0,-1%"
for %%i in ("%ANDROID_ROOT%\..") do set "REPO_ROOT=%%~fi"
set "VERSION_FILE=%REPO_ROOT%\version.properties"
set "FAIL=0"

call :read_version
if not defined VERSION_NAME set "VERSION_NAME=local"

set "OUT_DIR=%~1"
if not defined OUT_DIR set "OUT_DIR=%REPO_ROOT%\screenshots\android\%VERSION_NAME%"
set "MANIFEST=%OUT_DIR%\manifest.txt"
for /f "delims=" %%t in ('powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz'"') do set "CHECKED_AT=%%t"

echo Checking Android screenshot smoke set:
echo   %OUT_DIR%

if not exist "%OUT_DIR%" (
    echo FAIL: screenshot directory does not exist.
    exit /b 1
)

(
    echo SlideDo Android screenshot smoke set
    echo Version: %VERSION_NAME%
    echo Checked: %CHECKED_AT%
    echo Manual review status: pending
    echo Reviewer:
    echo Review date:
    echo.
    echo Review checklist:
    echo - Screenshots show real app UI from this build.
    echo - Text is readable and not clipped.
    echo - No personal data, emulator notifications, debug overlays, or system dialogs are visible.
    echo - Selected Play Console images cover Home, Mode Select, gameplay, learning/help, Settings, Records, and Results/current gameplay.
    echo.
) > "%MANIFEST%"

call :require_png "01-home" "Home entry screen"
call :require_png "02-mode-select" "Mode Select with 3x3, 4x4, and 5x5 choices"
call :require_png "03-game-3x3" "Active 3x3 gameplay"
call :require_png "04-how-to-play" "How to Play or Practice Tutorial"
call :require_png "05-settings" "Settings"
call :require_png "06-records" "Records"
call :require_png "07-results-or-current" "Results screen or current gameplay fallback"

if "%FAIL%"=="0" (
    echo Screenshot smoke set check passed.
    echo Manifest written to:
    echo   %MANIFEST%
) else (
    echo Screenshot smoke set check failed.
)
exit /b %FAIL%

:require_png
set "NAME=%~1"
set "PURPOSE=%~2"
set "FILE=%OUT_DIR%\%NAME%.png"
if not exist "%FILE%" (
    echo FAIL: missing %NAME%.png
    echo MISSING %NAME%.png %PURPOSE%>>"%MANIFEST%"
    set "FAIL=1"
    exit /b 0
)

set "DIMENSION_FILE=%TEMP%\slidedo-screenshot-dimension-%RANDOM%-%RANDOM%.txt"
set "SLIDEDO_SCREENSHOT_FILE=%FILE%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile($env:SLIDEDO_SCREENSHOT_FILE); try { if ($img.Width -lt 320 -or $img.Height -lt 320) { throw \"Screenshot is too small: $($img.Width)x$($img.Height)\" }; '{0}x{1}' -f $img.Width,$img.Height } finally { $img.Dispose() }" > "%DIMENSION_FILE%"
if errorlevel 1 (
    echo FAIL: unreadable or invalid %NAME%.png
    echo INVALID %NAME%.png %PURPOSE%>>"%MANIFEST%"
    if exist "%DIMENSION_FILE%" del "%DIMENSION_FILE%" >nul 2>nul
    set "FAIL=1"
    exit /b 0
)

set /p DIMENSION=<"%DIMENSION_FILE%"
if exist "%DIMENSION_FILE%" del "%DIMENSION_FILE%" >nul 2>nul
echo OK: %NAME%.png %DIMENSION% - %PURPOSE%
echo %NAME%.png %DIMENSION% %PURPOSE%>>"%MANIFEST%"
exit /b 0

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
exit /b 0
