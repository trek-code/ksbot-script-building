param(
    [string]$OutputRoot = "D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\reference-dump",
    [string]$JarPath = "D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $JarPath)) {
    throw "Missing KSBot API jar: $JarPath"
}

$classes = @(
    "rs.kreme.ksbot.api.KSContext",
    "rs.kreme.ksbot.api.scripts.Script",
    "rs.kreme.ksbot.api.scripts.ScriptManifest",
    "rs.kreme.ksbot.api.game.Pathing",
    "rs.kreme.ksbot.api.hooks.Bank",
    "rs.kreme.ksbot.api.hooks.Inventory",
    "rs.kreme.ksbot.api.hooks.GameObjects",
    "rs.kreme.ksbot.api.hooks.NPCs",
    "rs.kreme.ksbot.api.hooks.Players",
    "rs.kreme.ksbot.api.hooks.Skills",
    "rs.kreme.ksbot.api.queries.Query",
    "rs.kreme.ksbot.api.queries.TileObjectQuery",
    "rs.kreme.ksbot.api.queries.NPCQuery",
    "rs.kreme.ksbot.api.queries.TileItemQuery",
    "rs.kreme.ksbot.api.wrappers.KSObject",
    "rs.kreme.ksbot.api.wrappers.KSNPC",
    "rs.kreme.ksbot.api.wrappers.KSPlayer",
    "rs.kreme.ksbot.api.wrappers.KSGroundItem",
    "rs.kreme.ksbot.api.wrappers.KSItem",
    "rs.kreme.ksbot.api.commons.Random",
    "rs.kreme.ksbot.api.commons.Timer"
)

New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null

foreach ($class in $classes) {
    $safeName = ($class -replace '^rs\.kreme\.ksbot\.api\.', '') -replace '[.$]', '_'
    $outputFile = Join-Path $OutputRoot ($safeName + ".txt")
    javap -classpath $JarPath $class | Out-File -FilePath $outputFile -Encoding utf8
}

$indexLines = @(
    "# KSBot Local API Dump",
    "",
    "Generated from:",
    "",
    "- ``$JarPath``",
    "",
    "## Included classes",
    ""
)

foreach ($class in $classes) {
    $safeName = ($class -replace '^rs\.kreme\.ksbot\.api\.', '') -replace '[.$]', '_'
    $indexLines += "- ``$class`` -> ``$safeName.txt``"
}

Set-Content -Path (Join-Path $OutputRoot "api-index.md") -Value $indexLines -Encoding utf8

Write-Host "Generated KSBot API dump:"
Write-Host $OutputRoot
