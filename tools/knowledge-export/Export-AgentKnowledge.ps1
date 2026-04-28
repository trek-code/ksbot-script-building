$ErrorActionPreference = "Stop"

$workspaceRoot = "D:\Codex GPT\RSPS\KSBOT Script building"
$outputRoot = Join-Path $workspaceRoot "handoff\knowledge"
$stagingRoot = Join-Path $outputRoot "current-agent-brief"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$zipPath = Join-Path $outputRoot ("agent-knowledge-bundle-" + $timestamp + ".zip")
$briefPath = Join-Path $stagingRoot "agent-brief.md"

$filesToCopy = @(
    "README.md",
    "research\servers\reason.md",
    "docs\coordinates\reason-coordinate-registry.md",
    "docs\script-ledger\scripts\woodcutter-bot.md",
    "scripts\woodcutter-bot\README.md",
    "scripts\woodcutter-bot\BUILD.md",
    "scripts\woodcutter-bot\src\ReasonWoodcutterBot.java",
    "scripts\woodcutter-bot\src\WoodcutterBot.java",
    "scripts\woodcutter-bot\src\WoodcuttingProfile.java",
    "scripts\woodcutter-bot\src\WoodcutterSettings.java",
    "build\woodcutter-bot\build-woodcutter.ps1"
)

if (Test-Path $stagingRoot) {
    Remove-Item -LiteralPath $stagingRoot -Recurse -Force
}

New-Item -ItemType Directory -Path $stagingRoot | Out-Null
New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null

foreach ($relativePath in $filesToCopy) {
    $sourcePath = Join-Path $workspaceRoot $relativePath
    if (-not (Test-Path $sourcePath)) {
        continue
    }

    $destinationPath = Join-Path $stagingRoot $relativePath
    $destinationDirectory = Split-Path -Parent $destinationPath
    New-Item -ItemType Directory -Path $destinationDirectory -Force | Out-Null
    Copy-Item -LiteralPath $sourcePath -Destination $destinationPath -Force
}

$routeProfilesSource = Join-Path $workspaceRoot "research\route-profiles\reason\woodcutting-bot"
$routeProfilesDestination = Join-Path $stagingRoot "research\route-profiles\reason\woodcutting-bot"
if (Test-Path $routeProfilesSource) {
    Copy-Item -LiteralPath $routeProfilesSource -Destination $routeProfilesDestination -Recurse -Force
}

$brief = @"
# Agent Brief

- Exported: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
- Workspace: $workspaceRoot
- Focus: Reason woodcutting bot, route library, and knowledge base

## What To Read First
- \`scripts\woodcutter-bot\src\WoodcutterBot.java\`
- \`scripts\woodcutter-bot\src\WoodcuttingProfile.java\`
- \`research\servers\reason.md\`
- \`docs\script-ledger\scripts\woodcutter-bot.md\`

## Current Expectations
- Woodcutter uses route-based chopping, banking, and optional anti-ban timing.
- Route profiles live under \`research\route-profiles\reason\woodcutting-bot\`.
- Bank pathing is direct by default; only awkward banks should use inside-bank approach tiles.
- Future agents should update the knowledge bundle after major framework changes.
"@

Set-Content -LiteralPath $briefPath -Value $brief -Encoding UTF8

if (Test-Path $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}

Compress-Archive -Path (Join-Path $stagingRoot "*") -DestinationPath $zipPath -Force

Write-Host "Knowledge bundle created:"
Write-Host $zipPath
Write-Host "Brief file:"
Write-Host $briefPath
