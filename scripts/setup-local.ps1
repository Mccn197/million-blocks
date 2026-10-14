$ErrorActionPreference = "Stop"
$Root = Resolve-Path "$PSScriptRoot\.."
$Server = Join-Path $Root "local\server"
$Headers = @{ "User-Agent" = "MillionBlocks/0.6 (https://github.com/Mccn197/million-blocks)" }
$VersionLine = (& java -version 2>&1 | Select-Object -First 1).ToString()
if ($VersionLine -notmatch 'version "25\.') { throw "Select Java 25 first." }
New-Item -ItemType Directory -Force -Path (Join-Path $Server "plugins") | Out-Null
$Jar = Join-Path $Server "paper.jar"
if ((Test-Path $Jar) -or (Test-Path (Join-Path $Server "paper-build.json"))) { throw "paper.jar exists; preserve it first." }
$Builds = Invoke-RestMethod -Headers $Headers -Uri "https://fill.papermc.io/v3/projects/paper/versions/26.2/builds"
$Build = $Builds | Where-Object { $_.id -eq 121 -and $_.channel -eq "STABLE" } | Select-Object -First 1
if (-not $Build) { throw "Paper 26.2 stable build 121 unavailable; refusing another build." }
$Download = $Build.downloads.'server:default'
if ($Download.url -notmatch '^https://' -or $Download.checksums.sha256 -notmatch '^[a-fA-F0-9]{64}$') { throw "Invalid download metadata." }
$Temp = Join-Path $Server (".paper-download-" + [guid]::NewGuid() + ".jar")
try {
    Invoke-WebRequest -Headers $Headers -Uri $Download.url -OutFile $Temp
    if ((Get-FileHash -Algorithm SHA256 $Temp).Hash -ne $Download.checksums.sha256) { throw "Paper checksum mismatch." }
    Move-Item -Path $Temp -Destination $Jar
} finally {
    if (Test-Path $Temp) { Remove-Item $Temp }
}
$Build | ConvertTo-Json -Depth 20 | Set-Content (Join-Path $Server "paper-build.json")
$Start = Join-Path $Server "start.ps1"
if (-not (Test-Path $Start)) { Set-Content $Start 'Set-Location $PSScriptRoot; java -Xms512M -Xmx2G -jar paper.jar --nogui' }
Write-Host "Paper build 121 downloaded and verified. Review/accept the Minecraft EULA manually before starting. No server started."
