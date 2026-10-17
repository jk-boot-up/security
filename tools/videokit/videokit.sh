#!/usr/bin/env bash
#
# videokit -- builds a pattern project's narrated teaching video.
#
#   tools/videokit/videokit.sh all <project>   everything, including the YouTube docs
#   tools/videokit/videokit.sh help            every command
#
# <project> is a project directory, its video/ directory, or just its slug
# (e.g. api-gateway-with-spring-cloud-gateway).
#
# The Python environment and the voice model live outside the repository,
# in $VIDEOKIT_HOME (default ~/.cache/videokit), and are set up on first use.
#
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export VIDEOKIT_HOME="${VIDEOKIT_HOME:-$HOME/.cache/videokit}"
PY="$VIDEOKIT_HOME/venv/bin/python"

if [ ! -x "$PY" ] || [ "$HERE/requirements.txt" -nt "$VIDEOKIT_HOME/venv/.installed" ]; then
    echo "videokit: setting up $VIDEOKIT_HOME (first run only)" >&2
    python3 -m venv "$VIDEOKIT_HOME/venv"
    "$PY" -m pip install -q --upgrade pip
    "$PY" -m pip install -q -r "$HERE/requirements.txt"
    touch "$VIDEOKIT_HOME/venv/.installed"
fi

PYTHONPATH="$HERE${PYTHONPATH:+:$PYTHONPATH}" exec "$PY" -m videokit "$@"
