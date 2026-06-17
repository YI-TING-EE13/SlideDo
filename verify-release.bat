@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo [1/5] Android signed release APK and AAB
call "%ROOT%\android\build-release.bat"
if errorlevel 1 exit /b 1

echo [2/5] Desktop distributable package
call "%ROOT%\package-desktop.bat"
if errorlevel 1 exit /b 1

echo [3/5] Android store asset export
call "%ROOT%\android\export-store-assets.bat"
if errorlevel 1 exit /b 1

echo [4/5] Release artifact manifest
call "%ROOT%\generate-release-manifest.bat"
if errorlevel 1 exit /b 1

echo [5/5] Android Play Store readiness file check
call "%ROOT%\android\check-play-store-readiness.bat"
if errorlevel 1 exit /b 1

echo Release verification completed successfully.
exit /b 0
