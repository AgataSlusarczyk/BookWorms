#!/usr/bin/env bash
# The same as the "Start everything" task in VS Code, from the terminal:
# the database + server and, at the same time, the app on the emulator. Ctrl+C stops the server.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

(cd "$ROOT/server" && docker compose up -d --wait)

# The app in the background, its lines marked with [android] so they don't mix up with the server's.
"$ROOT/scripts/run-android.sh" 2>&1 | sed -u 's/^/[android] /' &
android_pid=$!
trap 'kill "$android_pid" 2> /dev/null || true' EXIT

cd "$ROOT/server"
./gradlew bootRun
