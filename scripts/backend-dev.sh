#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT/backend"

export SPRING_PROFILES_ACTIVE=dev

if [ -x "./gradlew" ]; then
  exec ./gradlew bootRun
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle bootRun
fi

cat <<'EOF'
Backend cannot start yet because neither Gradle Wrapper nor gradle is available.

Options:
1. Install Gradle, then run: ./scripts/backend-dev.sh
2. Add Gradle Wrapper to this repo, then run: ./scripts/backend-dev.sh
3. Start with Docker after Docker Desktop is running:
   docker compose up --build
EOF

exit 1
