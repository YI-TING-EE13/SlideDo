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
    echo.
) > "%MANIFEST%"

call :require_png "01-home"
call :require_png "02-mode-select"
call :require_png "03-game-3x3"
call :require_png "04-how-to-play"
call :require_png "05-settings"
call :require_png "06-records"
call :require_png "07-results-or-current"

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
set "FILE=%OUT_DIR%\%NAME%.png"
if not exist "%FILE%" (
    echo FAIL: missing %NAME%.png
    echo MISSING %NAME%.png>>"%MANIFEST%"
    set "FAIL=1"
    exit /b 0
)

set "DIMENSION_FILE=%TEMP%\slidedo-screenshot-dimension-%RANDOM%-%RANDOM%.txt"
set "SLIDEDO_SCREENSHOT_FILE=%FILE%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName System.Drawing; $img=[System.Drawing.Image]::FromFile($env:SLIDEDO_SCREENSHOT_FILE); try { if ($img.Width -lt 320 -or $img.Height -lt 320) { throw \"Screenshot is too small: $($img.Width)x$($img.Height)\" }; '{0}x{1}' -f $img.Width,$img.Height } finally { $img.Dispose() }" > "%DIMENSION_FILE%"
if errorlevel 1 (
    echo FAIL: unreadable or invalid %NAME%.png
    echo INVALID %NAME%.png>>"%MANIFEST%"
    if exist "%DIMENSION_FILE%" del "%DIMENSION_FILE%" >nul 2>nul
    set "FAIL=1"
    exit /b 0
)

set /p DIMENSION=<"%DIMENSION_FILE%"
if exist "%DIMENSION_FILE%" del "%DIMENSION_FILE%" >nul 2>nul
echo OK: %NAME%.png %DIMENSION%
echo %NAME%.png %DIMENSION%>>"%MANIFEST%"
exit /b 0

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
exit /b 0
