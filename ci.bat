@echo off
setlocal

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"

echo [1/3] Toolchain contract
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%ROOT%\verify-toolchain.ps1"
if errorlevel 1 exit /b 1

echo [2/3] Local verification
call "%ROOT%\verify.bat"
if errorlevel 1 exit /b 1

echo [3/3] Release readiness verification
call "%ROOT%\verify-release.bat"
if errorlevel 1 exit /b 1

echo CI verification completed successfully.
exit /b 0
