@echo off
setlocal

set TEST_DIR=%~dp0tests
set SRC_DIR=%~dp0src
set LIBS_DIR=%~dp0libs
set OUT_DIR=%~dp0out

set JUNIT_JAR=%LIBS_DIR%\junit-platform-console-standalone-1.10.0.jar
set WEBSOCKET_JAR=%LIBS_DIR%\Java-WebSocket-1.5.4.jar

if not exist "%LIBS_DIR%" mkdir "%LIBS_DIR%"

if not exist "%JUNIT_JAR%" (
    echo Downloading JUnit 5...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.10.0/junit-platform-console-standalone-1.10.0.jar' -OutFile '%JUNIT_JAR%'"
)

if not exist "%WEBSOCKET_JAR%" (
    echo Downloading Java-WebSocket...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.4/Java-WebSocket-1.5.4.jar' -OutFile '%WEBSOCKET_JAR%'"
)

if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%"

echo Compiling source files...
dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -d "%OUT_DIR%" -cp "%WEBSOCKET_JAR%" @"sources.txt"
if errorlevel 1 (
    echo Source compilation failed!
    del sources.txt 2>nul
    exit /b 1
)

echo Compiling test files...
dir /s /b "%TEST_DIR%\*.java" > test_sources.txt
javac -d "%OUT_DIR%" -cp "%OUT_DIR%;%JUNIT_JAR%;%WEBSOCKET_JAR%" @"test_sources.txt"
if errorlevel 1 (
    echo Test compilation failed!
    del sources.txt test_sources.txt 2>nul
    exit /b 1
)

del sources.txt test_sources.txt 2>nul

echo.
echo Running tests...
echo ==================
java -jar "%JUNIT_JAR%" -cp "%OUT_DIR%;%WEBSOCKET_JAR%" --scan-classpath

exit /b %ERRORLEVEL%
