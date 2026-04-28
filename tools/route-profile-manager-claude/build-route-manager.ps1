param(
    [string]$Version = "1.0.0"
)

$ErrorActionPreference = "Stop"

$workspace  = "D:\Claude Code\OLDER GPT Agent code framework\RSPS\KSBOT Script building"
$toolDir    = Join-Path $workspace "tools\route-profile-manager"
$sourceDir  = Join-Path $toolDir "src"
$classDir   = Join-Path $toolDir "classes"
$jarPath    = Join-Path $toolDir "route-profile-manager-$Version.jar"
$launchPath = Join-Path $toolDir "Launch-RouteManager.bat"

$sources = Get-ChildItem $sourceDir -Filter "*.java" | ForEach-Object { $_.FullName }
if (-not $sources) {
    throw "No Java source files found in $sourceDir"
}

New-Item -ItemType Directory -Force $classDir | Out-Null
Remove-Item -Path (Join-Path $classDir "*") -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "Compiling..."
javac --release 11 -encoding UTF-8 -d $classDir $sources

if ($LASTEXITCODE -ne 0) {
    throw "Compilation failed with exit code $LASTEXITCODE"
}

if (Test-Path $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

Write-Host "Packaging..."
jar --create --file $jarPath --main-class RouteProfileManager -C $classDir .

if ($LASTEXITCODE -ne 0) {
    throw "Jar packaging failed with exit code $LASTEXITCODE"
}

# Write a simple launcher batch file
$bat = @"
@echo off
java -jar "%~dp0route-profile-manager-$Version.jar"
"@
Set-Content -LiteralPath $launchPath -Value $bat -Encoding ASCII

Write-Host ""
Write-Host "Build complete:"
Write-Host "  JAR:      $jarPath"
Write-Host "  Launcher: $launchPath"
Write-Host ""
Write-Host "Run with:  java -jar `"$jarPath`""
Write-Host "   Or just double-click: Launch-RouteManager.bat"
