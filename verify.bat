@echo off
setlocal enabledelayedexpansion

set "ROOT=%~dp0"
set "ROOT=%ROOT:~0,-1%"
set "TEMP_ROOT=%TEMP%\slidedo-ci"
set "DESKTOP_BIN=%TEMP_ROOT%\desktop-bin"
set "SOURCES=%TEMP_ROOT%\sources.txt"
set "JAVADOC_BIN=C:\Program Files\Java\jdk-25\bin\javadoc.exe"
set "JAVADOC_CMD=javadoc"

if exist "%JAVADOC_BIN%" set "JAVADOC_CMD=%JAVADOC_BIN%"

echo [1/5] Shared core tests
call "%ROOT%\android\gradlew.bat" -p "%ROOT%" test
if errorlevel 1 exit /b 1

echo [2/5] Desktop compile
if exist "%TEMP_ROOT%" rmdir /s /q "%TEMP_ROOT%"
mkdir "%DESKTOP_BIN%"
if errorlevel 1 exit /b 1
for /r "%ROOT%\src" %%f in (*.java) do (
    set "SOURCE_FILE=%%f"
    set "SOURCE_FILE=!SOURCE_FILE:\=/!"
    echo "!SOURCE_FILE!">>"%SOURCES%"
)
javac -encoding UTF-8 -d "%DESKTOP_BIN%" @"%SOURCES%"
if errorlevel 1 exit /b 1

echo [3/5] Public core/desktop Javadocs
"%JAVADOC_CMD%" -quiet -public -Xdoclint:all -encoding UTF-8 -charset UTF-8 -sourcepath "%ROOT%\src" -d "%TEMP_ROOT%\javadocs" com.klotski.core com.klotski.ui
if errorlevel 1 exit /b 1

echo [4/5] Android assemble, test APK, and lint
pushd "%ROOT%\android"
call build-debug.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
set "ANDROID_RESULT=%ERRORLEVEL%"
popd
if not "%ANDROID_RESULT%"=="0" exit /b %ANDROID_RESULT%

echo [5/5] Android API Javadocs
"%JAVADOC_CMD%" -quiet -public -Xdoclint:all -encoding UTF-8 -charset UTF-8 -classpath "%LOCALAPPDATA%\Android\Sdk\platforms\android-36\android.jar;%ROOT%\src" -sourcepath "%ROOT%\android\app\src\main\java;%ROOT%\src" -d "%TEMP_ROOT%\android-javadocs" com.klotski.android
if errorlevel 1 exit /b 1

echo Verification completed successfully.
