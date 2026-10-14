$ErrorActionPreference = "Stop"
$McVersion = "26.2"
$Root = Resolve-Path "$PSScriptRoot\.."
$Server = Join-Path $Root "local\server"
$Headers = @{ "User-Agent" = "ReverieMillionBlocks/0.1 (https://gitlab.com/reverie-concepts)" }

$VersionLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($VersionLine -notmatch 'version "(\d+)') { throw "Cannot detect Java version." }
if ([int]$Matches[1] -lt 25) { throw "Paper $McVersion requires Java 25+. Found Java $($Matches[1])." }

New-Item -ItemType Directory -Force -Path (Join-Path $Server "plugins") | Out-Null
$Builds = Invoke-RestMethod -Headers $Headers -Uri "https://fill.papermc.io/v3/projects/paper/versions/$McVersion/builds"
$Stable = $Builds | Where-Object { $_.channel -eq "STABLE" } | Select-Object -First 1
if (-not $Stable) { throw "No stable Paper build found for $McVersion" }
$Url = $Stable.downloads.'server:default'.url
Invoke-WebRequest -Headers $Headers -Uri $Url -OutFile (Join-Path $Server "paper.jar")
Set-Content -Path (Join-Path $Server "eula.txt") -Value "eula=true"
Set-Content -Path (Join-Path $Server "start.ps1") -Value 'java -Xms2G -Xmx4G -jar paper.jar --nogui'
Write-Host "Local Paper server ready in $Server"
