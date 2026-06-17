@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "VERSION_FILE=%ROOT%\version.properties"

call :read_version
if not defined VERSION_NAME (
    echo VERSION_NAME was not found in %VERSION_FILE%.
    exit /b 1
)
if not defined VERSION_CODE (
    echo VERSION_CODE was not found in %VERSION_FILE%.
    exit /b 1
)

set "OUTPUT=%ROOT%\dist\release-manifests\%VERSION_NAME%.txt"
powershell -NoProfile -ExecutionPolicy Bypass -File "%ROOT%\tools\WriteReleaseManifest.ps1" -RepoRoot "%ROOT%" -VersionName "%VERSION_NAME%" -VersionCode "%VERSION_CODE%" -OutputPath "%OUTPUT%"
exit /b %ERRORLEVEL%

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_CODE=" "%VERSION_FILE%"') do set "VERSION_CODE=%%b"
exit /b 0
