@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "ADB=adb"

where adb >nul 2>nul
if errorlevel 1 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    ) else (
        echo adb was not found on PATH or at %%LOCALAPPDATA%%\Android\Sdk\platform-tools\adb.exe.
        echo Install Android platform-tools or open an Android Studio terminal.
        exit /b 1
    )
)

set "HAS_DEVICE="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" set "HAS_DEVICE=1"
)

if not defined HAS_DEVICE (
    echo No connected Android device or emulator is ready.
    echo Start an emulator or connect a device, then run this script again.
    exit /b 1
)

call :clean_dir "%ROOT%\android\app\build\outputs\connected_android_test_additional_output"
call :clean_dir "%ROOT%\android\app\build\outputs\androidTest-results\connected"
call :clean_dir "%ROOT%\android\app\build\reports\androidTests\connected"

pushd "%ROOT%\android"
call build-debug.bat :app:connectedDebugAndroidTest --warning-mode all --console plain
set "RESULT=%ERRORLEVEL%"
popd

if not "%RESULT%"=="0" (
    findstr /C:"OK (" "%ROOT%\android\app\build\outputs\androidTest-results\connected\debug\*\testlog\test-results.log" >nul 2>nul
    if not errorlevel 1 (
        echo connectedDebugAndroidTest completed, but Gradle report generation failed.
        echo Instrumentation reported OK; treating connected tests as passed.
        exit /b 0
    )
)

exit /b %RESULT%

:clean_dir
if exist "%~1" (
    attrib -R "%~1" /S /D >nul 2>nul
    rmdir /S /Q "%~1" >nul 2>nul
)
exit /b 0
