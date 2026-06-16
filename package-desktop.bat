@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "VERSION_FILE=%ROOT%\version.properties"

call :read_version
if not defined VERSION_NAME (
    echo VERSION_NAME was not found in %VERSION_FILE%.
    exit /b 1
)

call :find_tool javac JAVAC_CMD
call :find_tool jar JAR_CMD
call :find_tool_optional jpackage JPACKAGE_CMD

if not defined JAVAC_CMD (
    echo javac was not found. Install a JDK and try again.
    exit /b 1
)
if not defined JAR_CMD (
    echo jar was not found. Install a JDK and try again.
    exit /b 1
)

set "BUILD_ROOT=%ROOT%\build\desktop-package"
set "CLASSES=%BUILD_ROOT%\classes"
set "SOURCES=%BUILD_ROOT%\sources.txt"
set "MANIFEST=%BUILD_ROOT%\manifest.mf"
set "DIST_ROOT=%ROOT%\dist\desktop"
set "PACKAGE_DIR=%DIST_ROOT%\SlideDo-%VERSION_NAME%"
set "ZIP_FILE=%DIST_ROOT%\SlideDo-%VERSION_NAME%.zip"

if exist "%BUILD_ROOT%" rmdir /s /q "%BUILD_ROOT%"
if exist "%PACKAGE_DIR%" rmdir /s /q "%PACKAGE_DIR%"
if not exist "%CLASSES%" mkdir "%CLASSES%"
if not exist "%PACKAGE_DIR%" mkdir "%PACKAGE_DIR%"

for /r "%ROOT%\src" %%f in (*.java) do (
    set "SOURCE_FILE=%%~ff"
    set "SOURCE_FILE=!SOURCE_FILE:\=/!"
    echo "!SOURCE_FILE!">>"%SOURCES%"
)

"%JAVAC_CMD%" -encoding UTF-8 -d "%CLASSES%" @"%SOURCES%"
if errorlevel 1 exit /b 1

(
    echo Manifest-Version: 1.0
    echo Main-Class: com.klotski.ui.MainFrame
    echo Implementation-Version: %VERSION_NAME%
    echo.
) > "%MANIFEST%"

"%JAR_CMD%" --create --file "%PACKAGE_DIR%\SlideDo.jar" --manifest "%MANIFEST%" -C "%CLASSES%" .
if errorlevel 1 exit /b 1

(
    echo @echo off
    echo setlocal
    echo java -jar "%%~dp0SlideDo.jar" %%*
    echo endlocal
) > "%PACKAGE_DIR%\SlideDo.bat"

(
    echo SlideDo Desktop %VERSION_NAME%
    echo.
    echo Run SlideDo.bat to start the desktop app.
    echo.
    echo Saves and records are stored under %%APPDATA%%\SlideDo on Windows.
    echo For portable testing, run:
    echo   java -Dslidedo.data.dir=PATH_TO_DATA -jar SlideDo.jar
) > "%PACKAGE_DIR%\README.txt"

copy "%ROOT%\release-notes\%VERSION_NAME%.md" "%PACKAGE_DIR%\RELEASE_NOTES.md" >nul
if errorlevel 1 exit /b 1

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Force -Path '%PACKAGE_DIR%\*' -DestinationPath '%ZIP_FILE%'"
if errorlevel 1 exit /b 1

if /I not "%SKIP_JPACKAGE%"=="1" if defined JPACKAGE_CMD (
    set "APP_IMAGE_DEST=%DIST_ROOT%\app-image"
    if exist "!APP_IMAGE_DEST!\SlideDo" rmdir /s /q "!APP_IMAGE_DEST!\SlideDo"
    "%JPACKAGE_CMD%" --type app-image --name SlideDo --input "%PACKAGE_DIR%" --main-jar SlideDo.jar --main-class com.klotski.ui.MainFrame --dest "!APP_IMAGE_DEST!" --vendor "SlideDo"
    if errorlevel 1 exit /b 1
)

echo Desktop package artifacts:
echo   %PACKAGE_DIR%
echo   %ZIP_FILE%
if defined JPACKAGE_CMD if /I not "%SKIP_JPACKAGE%"=="1" echo   %DIST_ROOT%\app-image\SlideDo
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

:find_tool_optional
call :find_tool %~1 %~2
exit /b 0
