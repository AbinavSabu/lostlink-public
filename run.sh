#!/usr/bin/env bash
# ========================================================
#   LostLink - Campus Unified Launcher (macOS/Linux)
# ========================================================

set -e

# Resolve absolute path to the directory containing this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================================"
echo "       LostLink - Campus Unified Launcher"
echo "========================================================"
echo ""

# Ensure Maven Wrapper has executable permissions
if [ -f "$SCRIPT_DIR/lostfound/mvnw" ]; then
    chmod +x "$SCRIPT_DIR/lostfound/mvnw"
fi

PROFILE_ARG=""
if [ "$1" == "mysql" ] || [ "$SPRING_PROFILES_ACTIVE" == "mysql" ]; then
    echo "[DB MODE] Running with MySQL profile..."
    PROFILE_ARG="-Dspring-boot.run.profiles=mysql"
else
    echo "[DB MODE] Defaulting to Embedded H2 Database (File: ./data/lostfounddb)"
    echo "          H2 Console available at: http://localhost:8081/h2-console"
    echo "          (To use MySQL instead, run: ./run.sh mysql)"
fi
echo ""

# Ensure frontend dependencies are installed
if [ ! -d "$SCRIPT_DIR/lostfound-frontend/node_modules" ]; then
    echo "[INFO] Installing frontend dependencies..."
    (cd "$SCRIPT_DIR/lostfound-frontend" && npm install)
fi

echo "Launching Backend and Frontend..."
echo "  - Backend REST API: http://localhost:8081"
echo "  - Frontend Web UI:  http://localhost:5173"
echo "  - H2 Web Console:   http://localhost:8081/h2-console"
echo ""
echo "  [SYNC] Frontend will synchronize and open browser once Backend is listening."
echo ""
echo "========================================================"
echo "  Press Ctrl + C anytime to stop both services."
echo "========================================================"
echo ""

# Trap SIGINT and SIGTERM to clean up background processes
cleanup() {
    echo ""
    echo "[INFO] Stopping all services..."
    if [ -f "$SCRIPT_DIR/stop.sh" ]; then
        bash "$SCRIPT_DIR/stop.sh"
    fi
    exit 0
}
trap cleanup SIGINT SIGTERM EXIT

# Execute via concurrently from frontend dependencies or background fallback
if npx --prefix "$SCRIPT_DIR/lostfound-frontend" concurrently --version >/dev/null 2>&1; then
    npx --prefix "$SCRIPT_DIR/lostfound-frontend" concurrently -k -n "BACKEND,FRONTEND" -c "blue.bold,cyan.bold" \
        "cd \"$SCRIPT_DIR/lostfound\" && ./mvnw spring-boot:run $PROFILE_ARG" \
        "node \"$SCRIPT_DIR/lostfound-frontend/wait-for-backend.js\" && cd \"$SCRIPT_DIR/lostfound-frontend\" && npm run dev"
else
    (cd "$SCRIPT_DIR/lostfound" && ./mvnw spring-boot:run $PROFILE_ARG) &
    BACKEND_PID=$!

    node "$SCRIPT_DIR/lostfound-frontend/wait-for-backend.js"

    (cd "$SCRIPT_DIR/lostfound-frontend" && npm run dev) &
    FRONTEND_PID=$!

    wait $BACKEND_PID $FRONTEND_PID
fi
