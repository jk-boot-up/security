#!/bin/sh
# Upload with a quality check: see ytquality.py.
HERE="$(cd "$(dirname "$0")" && pwd)"
[ -x "$HERE/.venv/bin/yt-dlp" ] || "$HERE/.venv/bin/pip" install -q yt-dlp pillow
exec "$HERE/.venv/bin/python" "$HERE/ytquality.py" "$@"
