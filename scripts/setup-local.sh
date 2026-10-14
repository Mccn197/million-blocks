#!/usr/bin/env bash
set -euo pipefail

MC_VERSION="26.2"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERVER="$ROOT/local/server"
USER_AGENT="ReverieMillionBlocks/0.1 (https://gitlab.com/reverie-concepts)"

command -v java >/dev/null || { echo "Java 25 is required."; exit 1; }
command -v curl >/dev/null || { echo "curl is required."; exit 1; }
command -v jq >/dev/null || { echo "jq is required (apt install jq / brew install jq)."; exit 1; }

JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$JAVA_MAJOR" -lt 25 ]; then
  echo "Paper $MC_VERSION requires Java 25+. Found Java $JAVA_MAJOR."
  exit 1
fi

mkdir -p "$SERVER/plugins"
BUILDS=$(curl -fsSL -H "User-Agent: $USER_AGENT" "https://fill.papermc.io/v3/projects/paper/versions/$MC_VERSION/builds")
URL=$(echo "$BUILDS" | jq -r 'first(.[] | select(.channel == "STABLE") | .downloads."server:default".url) // "null"')
if [ "$URL" = "null" ]; then
  echo "No stable Paper build found for $MC_VERSION"
  exit 1
fi
curl -fL -H "User-Agent: $USER_AGENT" "$URL" -o "$SERVER/paper.jar"
echo 'eula=true' > "$SERVER/eula.txt"
cat > "$SERVER/start.sh" <<'START'
#!/usr/bin/env bash
java -Xms2G -Xmx4G -jar paper.jar --nogui
START
chmod +x "$SERVER/start.sh"
echo "Local Paper server ready in $SERVER"
echo "Build the plugin with: mvn clean package"
echo "Then copy target/MillionBlocks-*.jar to local/server/plugins/"
