#!/usr/bin/env bash
set -euo pipefail
TOOLS="${MB_TOOLCHAIN_DIR:-/workspace/toolchain}"
mkdir -p "$TOOLS"
JDK_URL='https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%2B10/OpenJDK25U-jdk_x64_linux_hotspot_25.0.2_10.tar.gz'
MAVEN_URL='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz'
if [[ ! -x "$TOOLS/jdk-25.0.2+10/bin/java" ]]; then
  curl -fsSL "$JDK_URL" -o "$TOOLS/jdk25.tar.gz"
  curl -fsSL "$JDK_URL.sha256.txt" -o "$TOOLS/jdk25.sha256"
  python3 - "$TOOLS" <<'PY'
import hashlib, pathlib, sys, tarfile
p=pathlib.Path(sys.argv[1]);data=p/'jdk25.tar.gz'
assert hashlib.sha256(data.read_bytes()).hexdigest()==(p/'jdk25.sha256').read_text().split()[0], 'JDK checksum mismatch'
with tarfile.open(data) as t:t.extractall(p,filter='data')
PY
fi
if [[ ! -x "$TOOLS/apache-maven-3.9.11/bin/mvn" ]]; then
  curl -fsSL "$MAVEN_URL" -o "$TOOLS/maven.tar.gz"
  curl -fsSL "$MAVEN_URL.sha512" -o "$TOOLS/maven.sha512"
  python3 - "$TOOLS" <<'PY'
import hashlib, pathlib, sys, tarfile
p=pathlib.Path(sys.argv[1]);data=p/'maven.tar.gz'
assert hashlib.sha512(data.read_bytes()).hexdigest()==(p/'maven.sha512').read_text().strip(), 'Maven checksum mismatch'
with tarfile.open(data) as t:t.extractall(p,filter='data')
PY
fi
# Maven must use the platform proxy and its installed trust store; never disable TLS.
python3 - "$TOOLS" <<'PY'
import os,sys,urllib.parse,xml.etree.ElementTree as E
from pathlib import Path
u=urllib.parse.urlsplit(os.environ.get('HTTPS_PROXY',os.environ.get('https_proxy','')))
r=E.Element('settings')
if u.hostname:
 p=E.SubElement(E.SubElement(r,'proxies'),'proxy')
 for k,v in {'id':'cloud','active':'true','protocol':'http','host':u.hostname,'port':str(u.port or 80)}.items():E.SubElement(p,k).text=v
 if u.username or u.password:raise SystemExit('Authenticated proxy requires secure Maven configuration; no credentials will be persisted')
E.ElementTree(r).write(Path(sys.argv[1])/'settings.xml')
PY
"$TOOLS/jdk-25.0.2+10/bin/java" -version
"$TOOLS/apache-maven-3.9.11/bin/mvn" -version
