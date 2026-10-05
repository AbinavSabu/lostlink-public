@echo off
setlocal
title LostLink - Campus Lost and Found Hub

echo ========================================================
echo       LostLink - Campus Lost and Found Unified Launcher
echo ========================================================
echo.

:: Check optional profile selection from argument (%1) or environment variable
set "PROFILE_ARG="
if /i "%1"=="mysql" (
    set "SPRING_PROFILES_ACTIVE=mysql"
)

if /i "%SPRING_PROFILES_ACTIVE%"=="mysql" (
    echo [DB MODE] Running with MySQL profile...
    :: Attempt to start MySQL80 if available, but do not block if absent
    sc query MySQL80 | find "RUNNING" >nul 2>&1
    if %ERRORLEVEL% NEQ 0 (
        net start MySQL80 >nul 2>&1
    )
    set "PROFILE_ARG=-Dspring-boot.run.profiles=mysql"
) else (
    echo [DB MODE] Defaulting to Embedded H2 Database (File: ./data/lostfounddb)
    echo           H2 Console available at: http://localhost:8081/h2-console
    echo           (To use MySQL instead, run: run.bat mysql)
)
echo.

:: Ensure frontend dependencies are installed
if not exist "%~dp0lostfound-frontend\node_modules" (
    echo [INFO] Installing frontend dependencies...
    cd /d "%~dp0lostfound-frontend"
    call npm install
    cd /d "%~dp0"
)

:: Launch both Backend and Frontend in a single console with synchronized startup
echo Launching Backend and Frontend in a single console...
echo.
echo   - Backend REST API: http://localhost:8081
echo   - Frontend Web UI:  http://localhost:5173
echo   - H2 Web Console:   http://localhost:8081/h2-console
echo.
echo   [SYNC] Frontend will synchronize and open browser once Backend is listening.
echo.
echo ========================================================
echo   Press Ctrl + C anytime to stop both services.
echo ========================================================
echo.

cd /d "%~dp0"
call npx --prefix "%~dp0lostfound-frontend" concurrently -k -n "BACKEND,FRONTEND" -c "blue.bold,cyan.bold" ^
    "cd /d \"%~dp0lostfound\" && mvnw.cmd spring-boot:run %PROFILE_ARG%" ^
    "node \"%~dp0lostfound-frontend\wait-for-backend.js\" && cd /d \"%~dp0lostfound-frontend\" && npm run dev"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [INFO] Services stopped.
    pause
)
