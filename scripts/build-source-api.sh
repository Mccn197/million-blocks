#!/usr/bin/env bash
# Explicit fallback only: genuine Paper 26.2 source API, not certified build 121.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${MB_TOOLCHAIN_DIR:-/workspace/toolchain}"
export JAVA_HOME="$TOOLS/jdk-25.0.2+10"
export PATH="$JAVA_HOME/bin:$TOOLS/apache-maven-3.9.11/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts"
SOURCE="$TOOLS/source"
mkdir -p "$SOURCE" "$TOOLS/paper-source"
fetch_source() {
  local name="$1" url="$2" commit="$3"
  if [[ ! -d "$SOURCE/$name/.git" ]]; then
    git init "$SOURCE/$name"
    git -C "$SOURCE/$name" fetch --depth 1 "$url" "$commit"
    git -C "$SOURCE/$name" checkout --detach FETCH_HEAD
  fi
  [[ "$(git -C "$SOURCE/$name" rev-parse HEAD)" == "$commit" ]] || { echo "Wrong $name revision" >&2; exit 1; }
  [[ -z "$(git -C "$SOURCE/$name" status --porcelain)" ]] || { echo "$name sources have local changes; refusing" >&2; exit 1; }
}
fetch_source Paper https://github.com/PaperMC/Paper.git 58e6e45d67c998909581f9079189ef51eb6f1267
fetch_source brigadier https://github.com/Mojang/brigadier.git 9ba4f13c0fe82b07c08c2dc2d8043f075ffd0d98
cp "$ROOT/scripts/source-api-pom.xml" "$TOOLS/paper-source/pom.xml"
mvn -s "$TOOLS/settings.xml" -f "$TOOLS/paper-source/pom.xml" -B -ntp -Dmaven.repo.local="$TOOLS/m2" -Dmb.source.root="$SOURCE" clean install
