param(
    [string]$Version = "0.1.0",
    [ValidateSet("Reason")]
    [string]$Server = "Reason",
    [switch]$Deploy
)

$ErrorActionPreference = "Stop"

$workspace = "D:\Codex GPT\RSPS\KSBOT Script building"
$sourceDir = Join-Path $workspace "scripts\woodcutter-reference\src"
$classDir = Join-Path $workspace "build\woodcutter-reference\classes"
$packageDir = Join-Path $workspace "handoff\packages"
$serverSlug = $Server.ToLower()
$jarPath = Join-Path $packageDir ("woodcutter-reference-" + $serverSlug + "-" + $Version + ".jar")

$ksbotJar = "C:\Users\jonez\.kreme\servers\Reason\rs.kreme.reason-api.jar"
$clientJar = "C:\Users\jonez\.kreme\servers\Reason\ReasonRSPS-client.jar"
$deployDir = "C:\Users\jonez\.kreme\servers\Reason\scripts\woodcutter_reference"
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

Write-Host "Built woodcutter reference package:"
Write-Host $jarPath

if ($Deploy) {
    New-Item -ItemType Directory -Force -Path $deployDir | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $deployDir ([System.IO.Path]::GetFileName($jarPath))) -Force
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $deployDir "woodcutter-reference-latest.jar") -Force
    Write-Host "Deployed to:"
    Write-Host $deployDir
}
