@echo off
setlocal

set "ROOT=%~dp0"
set "WEBSOCKET_JAR=%ROOT%libs\Java-WebSocket-1.5.4.jar"
set "SLF4J_API_JAR=%ROOT%libs\slf4j-api-2.0.13.jar"
set "SLF4J_SIMPLE_JAR=%ROOT%libs\slf4j-simple-2.0.13.jar"
set "DEPENDENCY_CP=%WEBSOCKET_JAR%;%SLF4J_API_JAR%;%SLF4J_SIMPLE_JAR%"

call "%ROOT%build.bat"
if errorlevel 1 exit /b 1

echo Starting Minecraft Server Manager...
java -cp "%ROOT%out;%DEPENDENCY_CP%" main.Main %*
