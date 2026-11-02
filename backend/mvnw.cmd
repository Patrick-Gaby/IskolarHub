@echo off
rem Minimal Maven wrapper for Windows. Run from the backend\ directory:  .\mvnw.cmd test
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\wrapper\mvnw.ps1" %*
exit /b %ERRORLEVEL%
