@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "PROJECT_ROOT=%~dp0"
set "BENCHMARK_CLASSES=%TEMP%\slidedo-solver-benchmark-%RANDOM%-%RANDOM%"
set "SOURCE_LIST=%BENCHMARK_CLASSES%\sources.txt"
set "CORE_SOURCE=%PROJECT_ROOT%src\com\klotski\core"
if defined SLIDEDO_BENCHMARK_CORE_SOURCE set "CORE_SOURCE=%SLIDEDO_BENCHMARK_CORE_SOURCE%"

mkdir "%BENCHMARK_CLASSES%" || exit /b 1
for /r "%CORE_SOURCE%" %%F in (*.java) do (
    set "SOURCE=%%~fF"
    echo "!SOURCE:\=/!">>"%SOURCE_LIST%"
)
for /r "%PROJECT_ROOT%benchmark" %%F in (*.java) do (
    set "SOURCE=%%~fF"
    echo "!SOURCE:\=/!">>"%SOURCE_LIST%"
)

javac -encoding UTF-8 --release 17 -d "%BENCHMARK_CLASSES%" @"%SOURCE_LIST%"
if errorlevel 1 (
    set "BENCHMARK_EXIT=!errorlevel!"
    goto cleanup
)

java -Xms768m -Xmx768m -cp "%BENCHMARK_CLASSES%" com.klotski.benchmark.SolverBenchmark %*
set "BENCHMARK_EXIT=%errorlevel%"

:cleanup
rmdir /s /q "%BENCHMARK_CLASSES%"
exit /b %BENCHMARK_EXIT%
