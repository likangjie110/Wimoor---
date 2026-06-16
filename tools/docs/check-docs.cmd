@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check-docs.ps1" %*
