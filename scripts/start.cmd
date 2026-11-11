@echo off
rem The same as scripts/start.sh, for Windows: database + server and the app on the emulator.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %*
