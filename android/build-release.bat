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
if not defined VERSION_CODE (
    echo VERSION_CODE was not found in %VERSION_FILE%.
    exit /b 1
)

set "RELEASE_NOTES=%REPO_ROOT%\release-notes\%VERSION_NAME%.md"
if not exist "%RELEASE_NOTES%" (
    echo Missing release notes: %RELEASE_NOTES%
    exit /b 1
)

set "GRADLE_SIGNING_ARGS="
if not defined SLIDEDO_RELEASE_KEYSTORE if not exist "%ANDROID_ROOT%\release.properties" (
    call :find_tool keytool KEYTOOL_CMD
    if not defined KEYTOOL_CMD (
        echo keytool was not found. Configure a real signing key or install a JDK.
        exit /b 1
    )

    set "TEMP_SIGNING_DIR=%TEMP%\slidedo-release-signing"
    set "TEMP_KEYSTORE=!TEMP_SIGNING_DIR!\slidedo-test-upload.jks"
    if not exist "!TEMP_SIGNING_DIR!" mkdir "!TEMP_SIGNING_DIR!"
    if not exist "!TEMP_KEYSTORE!" (
        echo Creating temporary test upload keystore for release verification.
        "!KEYTOOL_CMD!" -genkeypair -v -keystore "!TEMP_KEYSTORE!" -storepass slidedo-release-test -keypass slidedo-release-test -alias slidedo-upload -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=SlideDo Test Upload,O=SlideDo,C=TW"
        if errorlevel 1 exit /b 1
    )
    set "GRADLE_SIGNING_ARGS=-Pslidedo.release.keystore=!TEMP_KEYSTORE! -Pslidedo.release.keyAlias=slidedo-upload -Pslidedo.release.storePassword=slidedo-release-test -Pslidedo.release.keyPassword=slidedo-release-test"
)

echo Building SlideDo Android release %VERSION_NAME% (%VERSION_CODE%).
pushd "%ANDROID_ROOT%"
call build-debug.bat :app:printReleaseVersion :app:assembleRelease :app:bundleRelease --warning-mode all --console plain %GRADLE_SIGNING_ARGS%
set "RESULT=%ERRORLEVEL%"
popd
if not "%RESULT%"=="0" exit /b %RESULT%

set "APK=%ANDROID_ROOT%\app\build\outputs\apk\release\app-release.apk"
set "AAB=%ANDROID_ROOT%\app\build\outputs\bundle\release\app-release.aab"
if not exist "%APK%" (
    echo Missing release APK: %APK%
    exit /b 1
)
if not exist "%AAB%" (
    echo Missing release AAB: %AAB%
    exit /b 1
)

echo Android release artifacts:
echo   %APK%
echo   %AAB%
exit /b 0

:read_version
if not exist "%VERSION_FILE%" exit /b 0
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_NAME=" "%VERSION_FILE%"') do set "VERSION_NAME=%%b"
for /f "tokens=1,* delims==" %%a in ('findstr /B "VERSION_CODE=" "%VERSION_FILE%"') do set "VERSION_CODE=%%b"
exit /b 0

:find_tool
set "%~2="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\%~1.exe" set "%~2=%JAVA_HOME%\bin\%~1.exe"
if not defined %~2 if exist "C:\Program Files\Java\jdk-25\bin\%~1.exe" set "%~2=C:\Program Files\Java\jdk-25\bin\%~1.exe"
if not defined %~2 if exist "C:\Program Files\Java\jdk-22\bin\%~1.exe" set "%~2=C:\Program Files\Java\jdk-22\bin\%~1.exe"
if not defined %~2 for /f "delims=" %%p in ('where %~1 2^>nul') do if not defined %~2 set "%~2=%%p"
exit /b 0
