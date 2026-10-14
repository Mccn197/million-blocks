#!/usr/bin/env bash
# Download only: never start a server or overwrite an existing installation.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER="$ROOT/local/server"
UA='MillionBlocks/0.6 (https://github.com/Mccn197/million-blocks)'
command -v java >/dev/null
command -v python3 >/dev/null
command -v curl >/dev/null
JAVA_MAJOR=$(java -version 2>&1 | sed -nE '1s/.*version "([0-9]+).*/\1/p')
[[ "$JAVA_MAJOR" == 25 ]] || { echo 'Select Java 25 first.' >&2; exit 1; }
mkdir -p "$SERVER/plugins"
[[ ! -e "$SERVER/paper.jar" && ! -e "$SERVER/paper-build.json" ]] || { echo 'paper.jar exists; preserve/archive it before preparing another server.' >&2; exit 1; }
TMP=$(mktemp -d "$SERVER/.paper-download-XXXXXX")
trap 'rm -rf "$TMP"' EXIT
curl -fsSL -H "User-Agent: $UA" https://fill.papermc.io/v3/projects/paper/versions/26.2/builds -o "$TMP/builds.json"
python3 - "$TMP" <<'PY'
import json,pathlib,re,sys
p=pathlib.Path(sys.argv[1]);builds=json.loads((p/'builds.json').read_text())
b=next((b for b in builds if b['id']==121 and b['channel']=='STABLE'),None)
if not b:raise SystemExit('Paper 26.2 stable build 121 not available; refusing another build')
d=b['downloads']['server:default'];sha=d['checksums']['sha256']
assert d['url'].startswith('https://') and re.fullmatch('[0-9a-fA-F]{64}',sha)
(p/'url').write_text(d['url']);(p/'sha256').write_text(sha.lower());(p/'paper-build.json').write_text(json.dumps(b,indent=2))
PY
curl -fsSL -H "User-Agent: $UA" "$(cat "$TMP/url")" -o "$TMP/paper.jar"
python3 - "$TMP" <<'PY'
import hashlib,pathlib,sys
p=pathlib.Path(sys.argv[1]);assert hashlib.sha256((p/'paper.jar').read_bytes()).hexdigest()==(p/'sha256').read_text(),'Paper checksum mismatch'
PY
# Hard-link creation fails if another process created paper.jar in the meantime.
ln "$TMP/paper.jar" "$SERVER/paper.jar"
cp -n "$TMP/paper-build.json" "$SERVER/paper-build.json"
if [[ ! -e "$SERVER/start.sh" ]]; then
  cat > "$SERVER/start.sh" <<'START'
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
exec java -Xms512M -Xmx2G -jar paper.jar --nogui
START
  chmod +x "$SERVER/start.sh"
fi
echo 'Paper 26.2 build 121 downloaded and verified. Review the Minecraft EULA yourself before starting.'
echo 'No server started; no EULA setting written; no plugin deployed.'
