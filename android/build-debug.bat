@echo off
setlocal

if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if "%~1"=="" (
    call "%~dp0gradlew.bat" :app:assembleDebug
) else (
    call "%~dp0gradlew.bat" %*
)
set "RESULT=%ERRORLEVEL%"
endlocal & exit /b %RESULT%
