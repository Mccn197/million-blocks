#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${MB_TOOLCHAIN_DIR:-/workspace/toolchain}"
cd "$ROOT"
PYTHON="${MB_ASSET_PYTHON:-$TOOLS/asset-venv/bin/python}"
"$PYTHON" -m unittest discover -s tests -v
"$ROOT/scripts/build.sh" "$@"
git diff --check
