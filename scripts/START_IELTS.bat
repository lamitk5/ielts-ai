@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=C:\Users\haida\.codex\worktrees\ai-phase2-phase3-integration\ielts-ai-tutor"
if exist "%~dp0..\backend\mvnw.cmd" set "PROJECT_ROOT=%~dp0.."

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\scripts\start-ielts.ps1" -ProjectRoot "%PROJECT_ROOT%"
if errorlevel 1 (
    echo.
    echo IELTS launcher stopped with an error. Review the message above.
    pause
)
