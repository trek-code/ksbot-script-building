param(
    [string]$Version = "1.0.0",
    [ValidateSet("Reason", "Near-Reality")]
    [string]$Server = "Reason"
)

$ErrorActionPreference = "Stop"

$workspace = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$sourceDir = Join-Path $workspace "scripts\\rs-mechanics-capture\\src"
$classDir = Join-Path $workspace "build\\rs-mechanics-capture\\classes"
$packageDir = Join-Path $workspace "handoff\\packages"
$serverSlug = $Server.ToLower().Replace("-", "")
$jarName = "rs-mechanics-capture-" + $serverSlug + "-" + $Version + ".jar"
$jarPath = Join-Path $packageDir $jarName
$latestName = "rs-mechanics-capture-" + $serverSlug + "-latest.jar"

$deployDir = $null
$ksbotJar = $null
$clientJar = $null

switch ($Server) {
    "Reason" {
        $ksbotJar = "C:\\Users\\jonez\\.kreme\\servers\\Reason\\rs.kreme.reason-api.jar"
        $clientJar = "C:\\Users\\jonez\\.kreme\\servers\\Reason\\ReasonRSPS-client.jar"
        $deployDir = "C:\\Users\\jonez\\.kreme\\servers\\Reason\\scripts\\rs_mechanics_capture"
    }
    "Near-Reality" {
        $ksbotJar = "C:\\Users\\jonez\\.kreme\\servers\\Near-Reality\\rs.kreme.nearreality-api.jar"
        $clientJar = "C:\\Users\\jonez\\.kreme\\servers\\Near-Reality\\NearReality-client.jar"
        $deployDir = "C:\\Users\\jonez\\.kreme\\servers\\Near-Reality\\scripts\\rs_mechanics_capture"
    }
}

if (-not (Test-Path $sourceDir)) { throw "Missing source directory: $sourceDir" }
if (-not (Test-Path $ksbotJar)) { throw "Missing KSBot API jar: $ksbotJar" }
if (-not (Test-Path $clientJar)) { throw "Missing client jar: $clientJar" }

$sources = Get-ChildItem -Path $sourceDir -Filter "*.java" | ForEach-Object { $_.FullName }
if (-not $sources) { throw "No Java source files found in $sourceDir" }

New-Item -ItemType Directory -Force -Path $classDir | Out-Null
New-Item -ItemType Directory -Force -Path $packageDir | Out-Null
Remove-Item -Path (Join-Path $classDir "*") -Recurse -Force -ErrorAction SilentlyContinue

$classpath = $ksbotJar + ";" + $clientJar
javac --release 11 -encoding UTF-8 -cp $classpath -d $classDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

if (Test-Path $jarPath) { Remove-Item -LiteralPath $jarPath -Force }
jar --create --file $jarPath -C $classDir .
if ($LASTEXITCODE -ne 0) { throw "jar packaging failed with exit code $LASTEXITCODE" }

if ($deployDir) {
    New-Item -ItemType Directory -Force -Path $deployDir | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $deployDir $jarName) -Force
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $deployDir $latestName) -Force
}

Write-Host "Built mechanics-capture package:"
Write-Host $jarPath
if ($deployDir) {
    Write-Host "Deployed to:"
    Write-Host $deployDir
}
