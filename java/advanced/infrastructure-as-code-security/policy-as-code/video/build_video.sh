#!/usr/bin/env bash
# Builds this project's video with the shared videokit library (see video/videokit.toml).
set -euo pipefail
cd "$(dirname "$0")"
exec ../../../../tools/videokit/videokit.sh "${1:-all}" . "${@:2}"
