@echo off
setlocal

set TEST_DIR=%~dp0tests
set SRC_DIR=%~dp0src
set LIBS_DIR=%~dp0libs
set OUT_DIR=%~dp0out

set JUNIT_JAR=%LIBS_DIR%\junit-platform-console-standalone-1.10.0.jar
set WEBSOCKET_JAR=%LIBS_DIR%\Java-WebSocket-1.5.4.jar
set SLF4J_API_JAR=%LIBS_DIR%\slf4j-api-2.0.13.jar
set SLF4J_SIMPLE_JAR=%LIBS_DIR%\slf4j-simple-2.0.13.jar
set DEPENDENCY_CP=%WEBSOCKET_JAR%;%SLF4J_API_JAR%;%SLF4J_SIMPLE_JAR%

if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"

if not exist "%JUNIT_JAR%" (
    echo Downloading JUnit 5...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.0/junit-platform-console-standalone-1.10.0.jar' -OutFile '%JUNIT_JAR%'"
)
if not exist "%JUNIT_JAR%" (
    echo Failed to download JUnit 5.
    exit /b 1
)

if not exist "%WEBSOCKET_JAR%" (
    echo Downloading Java-WebSocket...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar' -OutFile '%WEBSOCKET_JAR%'"
)
if not exist "%WEBSOCKET_JAR%" (
    echo Failed to download Java-WebSocket.
    exit /b 1
)

if not exist "%SLF4J_API_JAR%" (
    echo Downloading SLF4J API...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar' -OutFile '%SLF4J_API_JAR%'"
)
if not exist "%SLF4J_API_JAR%" (
    echo Failed to download SLF4J API.
    exit /b 1
)

if not exist "%SLF4J_SIMPLE_JAR%" (
    echo Downloading SLF4J Simple...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar' -OutFile '%SLF4J_SIMPLE_JAR%'"
)
if not exist "%SLF4J_SIMPLE_JAR%" (
    echo Failed to download SLF4J Simple.
    exit /b 1
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo Compiling source files...
dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -d "%OUT_DIR%" -cp "%DEPENDENCY_CP%" @"sources.txt"
if errorlevel 1 (
    echo Source compilation failed!
    del sources.txt 2>nul
    exit /b 1
)

echo Compiling test files...
dir /s /b "%TEST_DIR%\*.java" > test_sources.txt
javac -d "%OUT_DIR%" -cp "%OUT_DIR%;%JUNIT_JAR%;%DEPENDENCY_CP%" @"test_sources.txt"
if errorlevel 1 (
    echo Test compilation failed!
    del sources.txt test_sources.txt 2>nul
    exit /b 1
)

del sources.txt test_sources.txt 2>nul

echo.
echo Running tests...
echo ==================
java -jar "%JUNIT_JAR%" execute -cp "%OUT_DIR%;%DEPENDENCY_CP%" --scan-classpath

exit /b %ERRORLEVEL%
