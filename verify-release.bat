@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo [1/6] Android signed release APK and AAB
call "%ROOT%\android\build-release.bat"
if errorlevel 1 exit /b 1

echo [2/6] Desktop distributable package
call "%ROOT%\package-desktop.bat"
if errorlevel 1 exit /b 1

echo [3/6] Android store asset export
call "%ROOT%\android\export-store-assets.bat"
if errorlevel 1 exit /b 1

echo [4/6] Release artifact manifest
call "%ROOT%\generate-release-manifest.bat"
if errorlevel 1 exit /b 1

echo [5/6] Android Play Store readiness file check
call "%ROOT%\android\check-play-store-readiness.bat"
if errorlevel 1 exit /b 1

echo [6/6] Desktop public beta readiness file check
call "%ROOT%\check-desktop-beta-readiness.bat"
if errorlevel 1 exit /b 1

echo Release verification completed successfully.
exit /b 0
