@echo off
setlocal

call "%~dp0build.bat"
if errorlevel 1 exit /b 1

java -cp "%~dp0out" main.Main %*
