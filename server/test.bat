@echo off
setlocal

set "ROOT=%~dp0"
set "OUT_DIR=%ROOT%out-test"

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

dir /s /b "%ROOT%src\*.java" > "%OUT_DIR%\sources.txt"
if exist "%ROOT%test" dir /s /b "%ROOT%test\*.java" >> "%OUT_DIR%\sources.txt"

javac -d "%OUT_DIR%" @"%OUT_DIR%\sources.txt"

if errorlevel 1 (
    echo Test build failed.
    exit /b 1
)

java -cp "%OUT_DIR%" test.TestRunner
