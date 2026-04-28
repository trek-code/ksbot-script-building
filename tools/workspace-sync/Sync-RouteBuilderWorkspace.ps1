param(
    [switch]$Reverse
)

$ErrorActionPreference = "Stop"

$canonicalRoot = "D:\Code Agents\Codex GPT\RSPS\KSBOT Script building\live-src\route\builder"
$mirrorRoot = "D:\Codex GPT\RSPS\KSBOT Script building\scripts\route-builder-claude\src"

if (-not (Test-Path $canonicalRoot)) {
    throw "Canonical Route Builder source root not found: $canonicalRoot"
}

if (-not (Test-Path $mirrorRoot)) {
    New-Item -ItemType Directory -Force -Path $mirrorRoot | Out-Null
}

$source = $canonicalRoot
$destination = $mirrorRoot

if ($Reverse) {
    $source = $mirrorRoot
    $destination = $canonicalRoot
}

$files = @(
    "ReasonRouteBuilderBot.java",
    "RouteBuilderBot.java",
    "RouteBuilderPanel.java",
    "RouteBuilderSession.java"
)

foreach ($file in $files) {
    Copy-Item (Join-Path $source $file) (Join-Path $destination $file) -Force
}

Write-Host "Synced Route Builder files:"
Write-Host "Source:      $source"
Write-Host "Destination: $destination"
