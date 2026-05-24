@echo off
setlocal enabledelayedexpansion
echo Checking for Java Compiler...
javac -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: 'javac' is not found in your PATH.
    echo Please ensure you have the Java Development Kit - JDK - installed and added to your PATH.
    echo You can download it from: https://adoptium.net/
    pause
    exit /b
)

echo Compiling...
if not exist bin mkdir bin
if exist sources.txt del sources.txt
for /r src %%f in (*.java) do (
    set "file=%%~ff"
    set "file=!file:\=/!"
    echo "!file!">> sources.txt
)
javac -encoding UTF-8 -d bin @sources.txt

if %errorlevel% neq 0 (
    del sources.txt
    echo Compilation Failed!
    pause
    exit /b
)
del sources.txt

echo Running Number Klotski...
java -cp bin com.klotski.ui.MainFrame
pause
endlocal
