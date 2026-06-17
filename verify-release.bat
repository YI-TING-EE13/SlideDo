@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo [1/4] Android signed release APK and AAB
call "%ROOT%\android\build-release.bat"
if errorlevel 1 exit /b 1

echo [2/4] Desktop distributable package
call "%ROOT%\package-desktop.bat"
if errorlevel 1 exit /b 1

echo [3/4] Android store asset export
call "%ROOT%\android\export-store-assets.bat"
if errorlevel 1 exit /b 1

echo [4/4] Android Play Store readiness file check
call "%ROOT%\android\check-play-store-readiness.bat"
if errorlevel 1 exit /b 1

echo Release verification completed successfully.
exit /b 0
