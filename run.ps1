# ========================================================
#   LostLink - Campus Unified Launcher (PowerShell)
# ========================================================
param (
    [string]$Profile = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "       LostLink - Campus Unified Launcher" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

$profileArg = ""
if ($Profile -eq "mysql" -or $env:SPRING_PROFILES_ACTIVE -eq "mysql") {
    Write-Host "[DB MODE] Running with MySQL profile..." -ForegroundColor Yellow
    $profileArg = "-Dspring-boot.run.profiles=mysql"
} else {
    Write-Host "[DB MODE] Defaulting to Embedded H2 Database (File: ./data/lostfounddb)" -ForegroundColor Green
    Write-Host "          H2 Console available at: http://localhost:8081/h2-console" -ForegroundColor Green
    Write-Host "          (To use MySQL instead, run: .\run.ps1 -Profile mysql)" -ForegroundColor Gray
}
Write-Host ""

# Ensure frontend dependencies
if (-not (Test-Path "$ScriptDir\lostfound-frontend\node_modules")) {
    Write-Host "[INFO] Installing frontend dependencies..." -ForegroundColor Yellow
    Push-Location "$ScriptDir\lostfound-frontend"
    npm.cmd install
    Pop-Location
}

Write-Host "Launching Backend and Frontend..." -ForegroundColor Cyan
Write-Host "  - Backend REST API: http://localhost:8081" -ForegroundColor Gray
Write-Host "  - Frontend Web UI:  http://localhost:5173" -ForegroundColor Gray
Write-Host "  - H2 Web Console:   http://localhost:8081/h2-console" -ForegroundColor Gray
Write-Host ""
Write-Host "  [SYNC] Frontend will synchronize and open browser once Backend is listening." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Press Ctrl + C anytime to stop both services." -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

npx.cmd --prefix "$ScriptDir\lostfound-frontend" concurrently -k -n "BACKEND,FRONTEND" -c "blue.bold,cyan.bold" `
    "cd /d `"$ScriptDir\lostfound`" && mvnw.cmd spring-boot:run $profileArg" `
    "node `"$ScriptDir\lostfound-frontend\wait-for-backend.js`" && cd /d `"$ScriptDir\lostfound-frontend`" && npm run dev"
