@echo off
setlocal

set "ROOT=%~dp0"
set "WEBSOCKET_JAR=%ROOT%libs\Java-WebSocket-1.5.4.jar"

call "%ROOT%build.bat"
if errorlevel 1 exit /b 1

echo Starting Minecraft Server Manager...
java -cp "%ROOT%out;%WEBSOCKET_JAR%" main.Main %*
