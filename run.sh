#!/usr/bin/env bash
set -e

PORT=${1:-8080}

# 1) Try to stop anything on the port (politely, then force)
PID=$(lsof -nP -iTCP:$PORT -sTCP:LISTEN -t 2>/dev/null || true)
if [ -n "$PID" ]; then
  echo "Stopping process $PID on port $PORT..."
  kill "$PID" || true
  sleep 1
  if lsof -nP -iTCP:$PORT -sTCP:LISTEN >/dev/null; then
    echo "Force killing process $PID..."
    kill -9 "$PID" || true
  fi
fi

# 2) Rebuild quickly and run (skip tests for speed)
echo "Starting Spring Boot on port $PORT..."
mvn spring-boot:run



# 3) Verify Springboot allowing API calls from port 5173 (Front-end)
curl -i \
  -H "Origin: http://localhost:5173" \
  http://localhost:8080/api/hello