@echo off
setlocal

set "ROOT=%~dp0"
set "OUT_DIR=%ROOT%out"
set "LIBS_DIR=%ROOT%libs"
set "WEBSOCKET_JAR=%LIBS_DIR%\Java-WebSocket-1.5.4.jar"

if not exist "%WEBSOCKET_JAR%" (
    echo Java-WebSocket JAR not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar' -OutFile '%WEBSOCKET_JAR%'"
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo Compiling source files...
dir /s /b "%ROOT%src\*.java" > "%OUT_DIR%\sources.txt"
javac -cp "%WEBSOCKET_JAR%" -d "%OUT_DIR%" @"%OUT_DIR%\sources.txt"

if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo Build finished. Classes are in "%OUT_DIR%".
