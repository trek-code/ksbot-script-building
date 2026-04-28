param(
    [string]$Version = "1.0.0",
    [ValidateSet("Reason", "Near-Reality")]
    [string]$Server = "Reason"
)

$ErrorActionPreference = "Stop"

# ── Paths ──────────────────────────────────────────────────────────────────
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceDir   = Join-Path $projectRoot "src"
$classDir    = Join-Path $projectRoot "build\classes"
$packageDir  = Join-Path $projectRoot "build\packages"
$serverSlug  = $Server.ToLower().Replace("-", "")
$jarName     = "rs-perk-farmer-$serverSlug-$Version.jar"
$jarPath     = Join-Path $packageDir $jarName
$latestName  = "rs-perk-farmer-$serverSlug-latest.jar"

switch ($Server) {
    "Reason" {
        $ksbotJar  = "C:\Users\jonez\.kreme\servers\Reason\rs.kreme.reason-api.jar"
        $clientJar = "C:\Users\jonez\.kreme\servers\Reason\ReasonRSPS-client.jar"
        $deployDir = "C:\Users\jonez\.kreme\servers\Reason\scripts\rs_perk_farmer"
    }
    "Near-Reality" {
        $ksbotJar  = "C:\Users\jonez\.kreme\servers\Near-Reality\rs.kreme.nearreality-api.jar"
        $clientJar = "C:\Users\jonez\.kreme\servers\Near-Reality\NearReality-client.jar"
        $deployDir = "C:\Users\jonez\.kreme\servers\Near-Reality\scripts\rs_perk_farmer"
    }
}

if (-not (Test-Path $ksbotJar))  { throw "Missing KSBot API jar: $ksbotJar" }
if (-not (Test-Path $clientJar)) { throw "Missing client jar: $clientJar" }

# ── Clean + recreate build dirs ───────────────────────────────────────────
if (Test-Path $classDir)   { Remove-Item -Recurse -Force $classDir }
New-Item -ItemType Directory -Force -Path $classDir   | Out-Null
New-Item -ItemType Directory -Force -Path $packageDir | Out-Null
New-Item -ItemType Directory -Force -Path $deployDir  | Out-Null

# ── Compile ───────────────────────────────────────────────────────────────
$sources = Get-ChildItem -Recurse -Path $sourceDir -Filter *.java | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) { throw "No .java sources under $sourceDir" }

Write-Host "Compiling $($sources.Count) source file(s)..." -ForegroundColor Cyan
$cp = "$ksbotJar;$clientJar"
& javac -encoding UTF-8 -source 11 -target 11 -cp $cp -d $classDir @sources
if ($LASTEXITCODE -ne 0) { throw "javac failed." }

# ── Package jar ───────────────────────────────────────────────────────────
Write-Host "Packaging $jarName ..." -ForegroundColor Cyan
Push-Location $classDir
try {
    & jar cf $jarPath .
    if ($LASTEXITCODE -ne 0) { throw "jar failed." }
} finally { Pop-Location }

Copy-Item $jarPath (Join-Path $packageDir $latestName) -Force

# ── Deploy ────────────────────────────────────────────────────────────────
Copy-Item $jarPath $deployDir -Force
Write-Host ""
Write-Host "OK. Built:   $jarPath" -ForegroundColor Green
Write-Host "    Deployed to: $deployDir" -ForegroundColor Green
Write-Host "    Version:    $Version" -ForegroundColor Green
Write-Host "Reload Scripts in KSBot and look for 'RS Perk Farmer' under Mining." -ForegroundColor Yellow
