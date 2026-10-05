@echo off
setlocal
title Stopping LostLink Services

echo ========================================================
echo            Stopping LostLink Services
echo ========================================================
echo.

echo [1/2] Terminating Spring Boot Backend (Port 8081)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":8081" ^| findstr "LISTENING"') do (
    echo [KILLED] Backend process PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo [2/2] Terminating React Vite Frontend (Port 5173)...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr ":5173" ^| findstr "LISTENING"') do (
    echo [KILLED] Frontend process PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

echo.
echo ========================================================
echo     All LostLink services stopped successfully!
echo ========================================================
echo.
pause
