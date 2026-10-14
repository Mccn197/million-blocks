#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR=$(find "$ROOT/target" -maxdepth 1 -name 'MillionBlocks-*.jar' | head -n 1)
[ -n "$JAR" ] || { echo "No built jar found. Run mvn clean package first."; exit 1; }
mkdir -p "$ROOT/local/server/plugins"
cp "$JAR" "$ROOT/local/server/plugins/MillionBlocks.jar"
echo "Deployed $(basename "$JAR") -> local/server/plugins/MillionBlocks.jar"
