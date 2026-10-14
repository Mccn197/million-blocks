#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${MB_TOOLCHAIN_DIR:-/workspace/toolchain}"
export JAVA_HOME="$TOOLS/jdk-25.0.2+10"
export PATH="$JAVA_HOME/bin:$TOOLS/apache-maven-3.9.11/bin:$PATH"
if [[ -f /etc/ssl/certs/java/cacerts ]]; then
  export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
fi
cd "$ROOT"
args=()
if [[ "${1:-}" == --source-api ]]; then
  [[ -f "$TOOLS/m2/io/papermc/paper/paper-api/26.2-source-58e6e45/paper-api-26.2-source-58e6e45.jar" ]] || "$ROOT/scripts/build-source-api.sh"
  args+=('-Dpaper.version=26.2-source-58e6e45')
elif [[ $# -gt 0 ]]; then
  echo 'Usage: scripts/build.sh [--source-api]' >&2; exit 2
fi
mvn -s "$TOOLS/settings.xml" -B -ntp -Dmaven.repo.local="$TOOLS/m2" "${args[@]}" clean package
