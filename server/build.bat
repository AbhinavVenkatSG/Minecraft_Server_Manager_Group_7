@echo off
setlocal

set "ROOT=%~dp0"
set "OUT_DIR=%ROOT%out"
set "LIBS_DIR=%ROOT%libs"
set "WEBSOCKET_JAR=%LIBS_DIR%\Java-WebSocket-1.5.4.jar"
set "SLF4J_API_JAR=%LIBS_DIR%\slf4j-api-2.0.13.jar"
set "SLF4J_SIMPLE_JAR=%LIBS_DIR%\slf4j-simple-2.0.13.jar"
set "DEPENDENCY_CP=%WEBSOCKET_JAR%;%SLF4J_API_JAR%;%SLF4J_SIMPLE_JAR%"

if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"

if not exist "%WEBSOCKET_JAR%" (
    echo Java-WebSocket JAR not found. Downloading...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar' -OutFile '%WEBSOCKET_JAR%'"
)
if not exist "%WEBSOCKET_JAR%" (
    echo Failed to download Java-WebSocket.
    exit /b 1
)

if not exist "%SLF4J_API_JAR%" (
    echo SLF4J API JAR not found. Downloading...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar' -OutFile '%SLF4J_API_JAR%'"
)
if not exist "%SLF4J_API_JAR%" (
    echo Failed to download SLF4J API.
    exit /b 1
)

if not exist "%SLF4J_SIMPLE_JAR%" (
    echo SLF4J Simple JAR not found. Downloading...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar' -OutFile '%SLF4J_SIMPLE_JAR%'"
)
if not exist "%SLF4J_SIMPLE_JAR%" (
    echo Failed to download SLF4J Simple.
    exit /b 1
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo Compiling source files...
dir /s /b "%ROOT%src\*.java" > "%OUT_DIR%\sources.txt"
javac -cp "%DEPENDENCY_CP%" -d "%OUT_DIR%" @"%OUT_DIR%\sources.txt"

if errorlevel 1 (
    echo Build failed.
    exit /b 1
)

echo Build finished. Classes are in "%OUT_DIR%".
