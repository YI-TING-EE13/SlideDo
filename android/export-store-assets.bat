@echo off
setlocal enabledelayedexpansion

set "ANDROID_ROOT=%~dp0"
set "ANDROID_ROOT=%ANDROID_ROOT:~0,-1%"
for %%i in ("%ANDROID_ROOT%\..") do set "REPO_ROOT=%%~fi"
set "VERSION_FILE=%REPO_ROOT%\version.properties"

call :read_version
if not defined VERSION_NAME (
    echo VERSION_NAME was not found in %VERSION_FILE%.
    exit /b 1
)

call :find_tool javac JAVAC_CMD
call :find_tool java JAVA_CMD
if not defined JAVAC_CMD (
    echo javac was not found. Install a JDK and try again.
    exit /b 1
)
if not defined JAVA_CMD (
    echo java was not found. Install a JDK and try again.
    exit /b 1
)

set "BUILD_ROOT=%REPO_ROOT%\build\store-assets"
set "CLASSES=%BUILD_ROOT%\classes"
set "OUTPUT_DIR=%REPO_ROOT%\dist\store-assets\android\%VERSION_NAME%"
set "FEATURE_GRAPHIC=%OUTPUT_DIR%\feature-graphic-1024x500.png"

if exist "%BUILD_ROOT%" rmdir /s /q "%BUILD_ROOT%"
if not exist "%CLASSES%" mkdir "%CLASSES%"

"%JAVAC_CMD%" -encoding UTF-8 -d "%CLASSES%" "%REPO_ROOT%\tools\StoreAssetExporter.java"
if errorlevel 1 exit /b 1

"%JAVA_CMD%" -cp "%CLASSES%" StoreAssetExporter "%FEATURE_GRAPHIC%"
if errorlevel 1 exit /b 1

exit /b 0

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
exit /b 0

:find_tool
set "%~2="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\%~1.exe" set "%~2=%JAVA_HOME%\bin\%~1.exe"
if not defined %~2 if exist "C:\Program Files\Java\jdk-25\bin\%~1.exe" set "%~2=C:\Program Files\Java\jdk-25\bin\%~1.exe"
if not defined %~2 if exist "C:\Program Files\Java\jdk-22\bin\%~1.exe" set "%~2=C:\Program Files\Java\jdk-22\bin\%~1.exe"
if not defined %~2 for /f "delims=" %%p in ('where %~1 2^>nul') do if not defined %~2 set "%~2=%%p"
exit /b 0
