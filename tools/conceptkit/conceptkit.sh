#!/usr/bin/env bash
#
# conceptkit -- builds a concept project's README, docs, diagrams, animation and video
# scenes from its concept.toml, then runs the tests and the videokit pipeline.
#
#   tools/conceptkit/conceptkit.sh build cia-triad
#   tools/conceptkit/conceptkit.sh help
#
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VIDEOKIT_HOME="${VIDEOKIT_HOME:-$HOME/.cache/videokit}"
PY="$VIDEOKIT_HOME/venv/bin/python"
"$HERE/../videokit/videokit.sh" help >/dev/null   # sets up the Python environment on first use
PYTHONPATH="$HERE:$HERE/../videokit${PYTHONPATH:+:$PYTHONPATH}" exec "$PY" -m conceptkit "$@"
