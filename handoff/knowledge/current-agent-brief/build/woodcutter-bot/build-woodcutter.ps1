param(
    [string]$Version = "1.3.0",
    [ValidateSet("Reason", "Near-Reality")]
    [string]$Server = "Reason"
)

$ErrorActionPreference = "Stop"

$workspace = "D:\Codex GPT\RSPS\KSBOT Script building"
$sourceDir = Join-Path $workspace "scripts\woodcutter-bot\src"
$classDir = Join-Path $workspace "build\woodcutter-bot\classes"
$packageDir = Join-Path $workspace "handoff\packages"
$serverSlug = $Server.ToLower().Replace("-", "")
$jarPath = Join-Path $packageDir ("woodcutter-bot-" + $serverSlug + "-" + $Version + ".jar")

switch ($Server) {
    "Reason" {
        $ksbotJar = "C:\Users\jonez\.kreme\servers\Reason\rs.kreme.reason-api.jar"
        $clientJar = "C:\Users\jonez\.kreme\servers\Reason\ReasonRSPS-client.jar"
    }
    "Near-Reality" {
        $ksbotJar = "C:\Users\jonez\.kreme\servers\Near-Reality\rs.kreme.nearreality-api.jar"
        $clientJar = "C:\Users\jonez\.kreme\servers\Near-Reality\NearReality-client.jar"
    }
}

$classpath = $ksbotJar + ";" + $clientJar

if (-not (Test-Path $ksbotJar)) {
    throw "Missing KSBot API jar: $ksbotJar"
}

if (-not (Test-Path $clientJar)) {
    throw "Missing client jar: $clientJar"
}

$sources = Get-ChildItem $sourceDir -Filter "*.java" | ForEach-Object { $_.FullName }

if (-not $sources) {
    throw "No Java source files found in $sourceDir"
}

New-Item -ItemType Directory -Force $classDir | Out-Null
New-Item -ItemType Directory -Force $packageDir | Out-Null

Remove-Item -Path (Join-Path $classDir "*") -Recurse -Force -ErrorAction SilentlyContinue

javac --release 11 -cp $classpath -d $classDir $sources

if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

if (Test-Path $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

jar --create --file $jarPath -C $classDir .

if ($LASTEXITCODE -ne 0) {
    throw "jar packaging failed with exit code $LASTEXITCODE"
}

Write-Host "Built woodcutter package:"
Write-Host $jarPath
