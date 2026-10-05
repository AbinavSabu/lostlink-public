#!/usr/bin/env bash
# ========================================================
#    LostLink - Service Shutdown (macOS/Linux)
# ========================================================

echo "========================================================"
echo "            Stopping LostLink Services"
echo "========================================================"
echo ""

kill_port() {
    local PORT=$1
    local NAME=$2
    echo "[INFO] Checking port $PORT ($NAME)..."

    local PIDS=""
    if command -v lsof >/dev/null 2>&1; then
        PIDS=$(lsof -ti :$PORT 2>/dev/null || true)
    elif command -v fuser >/dev/null 2>&1; then
        PIDS=$(fuser $PORT/tcp 2>/dev/null || true)
    fi

    if [ -n "$PIDS" ]; then
        for PID in $PIDS; do
            echo "[KILLED] Terminating $NAME process PID: $PID"
            kill -9 "$PID" 2>/dev/null || true
        done
    else
        echo "[OK] No active process on port $PORT."
    fi
}

# 1. Terminate Backend (8081)
kill_port 8081 "Backend (Spring Boot)"

# 2. Terminate Frontend (5173)
kill_port 5173 "Frontend (Vite)"

echo ""
echo "========================================================"
echo "    All LostLink services stopped successfully!"
echo "========================================================"
echo ""
