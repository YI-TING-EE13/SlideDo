@echo off
setlocal enabledelayedexpansion

set "ANDROID_ROOT=%~dp0"
set "ANDROID_ROOT=%ANDROID_ROOT:~0,-1%"
for %%i in ("%ANDROID_ROOT%\..") do set "REPO_ROOT=%%~fi"
set "VERSION_FILE=%REPO_ROOT%\version.properties"
set "ADB=adb"

call :read_version
if not defined VERSION_NAME set "VERSION_NAME=local"

where adb >nul 2>nul
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo adb was not found on PATH or at %%LOCALAPPDATA%%\Android\Sdk\platform-tools\adb.exe.
        exit /b 1
    )
)

set "HAS_DEVICE="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" set "HAS_DEVICE=1"
)
if not defined HAS_DEVICE (
    echo No connected Android device or emulator is ready.
    exit /b 1
)

set "OUT_DIR=%REPO_ROOT%\screenshots\android\%VERSION_NAME%"
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

"%ADB%" shell am start -n com.klotski.android/.MainActivity >nul
timeout /t 2 /nobreak >nul
call :capture "01-home"

echo.
echo Navigate to Mode Select, then press any key.
pause >nul
call :capture "02-mode-select"

echo.
echo Start a 3x3 game, then press any key.
pause >nul
call :capture "03-game-3x3"

echo.
echo Open How to Play or Practice Tutorial, then press any key.
pause >nul
call :capture "04-how-to-play"

echo.
echo Open Settings, then press any key.
pause >nul
call :capture "05-settings"

echo.
echo Open Records, then press any key.
pause >nul
call :capture "06-records"

echo.
echo If a Results screen is available, open it now; otherwise leave the current screen. Press any key.
pause >nul
call :capture "07-results-or-current"

call "%ANDROID_ROOT%\check-screenshot-set.bat" "%OUT_DIR%"
if errorlevel 1 exit /b 1

echo Screenshot smoke captures written to:
echo   %OUT_DIR%
exit /b 0

:capture
set "NAME=%~1"
set "REMOTE=/sdcard/slidedo-%NAME%.png"
"%ADB%" shell screencap -p "%REMOTE%" >nul
"%ADB%" pull "%REMOTE%" "%OUT_DIR%\%NAME%.png" >nul
"%ADB%" shell rm "%REMOTE%" >nul 2>nul
if not exist "%OUT_DIR%\%NAME%.png" (
    echo Failed to capture %NAME%.
    exit /b 1
)
echo Captured %NAME%.png
exit /b 0

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
exit /b 0
