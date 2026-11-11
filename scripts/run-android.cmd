@echo off
rem The same as scripts/run-android.sh, for Windows: builds the app and opens it on the emulator.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-android.ps1" %*
