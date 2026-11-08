#!/bin/sh
# Upload the security videos to YouTube Studio. See ytupload.py for the commands.
HERE="$(cd "$(dirname "$0")" && pwd)"
[ -x "$HERE/.venv/bin/python" ] || { python3 -m venv "$HERE/.venv" && "$HERE/.venv/bin/pip" install -q playwright; }
exec "$HERE/.venv/bin/python" "$HERE/ytupload.py" "$@"
