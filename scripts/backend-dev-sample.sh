#!/bin/sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname "$0")" && pwd)"

export APP_SAMPLE_DATA_ENABLED=true
export SERVER_PORT="${SERVER_PORT:-8081}"

exec "$SCRIPT_DIR/backend-dev.sh"
