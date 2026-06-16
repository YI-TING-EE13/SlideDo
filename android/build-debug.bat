@echo off
setlocal

if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

call :clean_dir "%~dp0app\build\intermediates\incremental\debug"
call :clean_dir "%~dp0app\build\intermediates\incremental\release"
call :clean_dir "%~dp0app\build\intermediates\merged_res_blame_folder\debug\mergeDebugResources"
call :clean_dir "%~dp0app\build\intermediates\merged_res_blame_folder\release\mergeReleaseResources"
call :clean_dir "%~dp0app\build\intermediates\packaged_res\debug\packageDebugResources"
call :clean_dir "%~dp0app\build\intermediates\packaged_res\release\packageReleaseResources"

if "%~1"=="" (
    call "%~dp0gradlew.bat" :app:assembleDebug
) else (
    call "%~dp0gradlew.bat" %*
)
set "RESULT=%ERRORLEVEL%"
endlocal & exit /b %RESULT%

:clean_dir
if exist "%~1" (
    attrib -R "%~1" /S /D >nul 2>nul
    rmdir /S /Q "%~1" >nul 2>nul
)
exit /b 0
