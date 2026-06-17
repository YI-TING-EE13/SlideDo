@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo [1/2] Local verification
call "%ROOT%\verify.bat"
if errorlevel 1 exit /b 1

echo [2/2] Release readiness verification
call "%ROOT%\verify-release.bat"
if errorlevel 1 exit /b 1

echo CI verification completed successfully.
exit /b 0
