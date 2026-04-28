param(
    [string]$WorkspaceRoot = $(Split-Path (Split-Path $PSScriptRoot -Parent) -Parent)
)

$ErrorActionPreference = "Stop"

$liveRoot = Join-Path $WorkspaceRoot "live-src"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

$mappings = @(
    @{
        ScriptFolder = "woodcutter-bot"
        PackagePath = "reason\woodcutter"
    },
    @{
        ScriptFolder = "world-mapper"
        PackagePath = "reason\mapper"
    },
    @{
        ScriptFolder = "woodcutter-reference"
        PackagePath = "reason\woodcutterreference"
    },
    @{
        ScriptFolder = "route-builder"
        PackagePath = "route\builder"
    },
    @{
        ScriptFolder = "rs-perk-farmer"
        PackagePath = "rsperkfarmer"
    },
    @{
        ScriptFolder = "rs-perk-farmer-v103"
        PackagePath = "rsperkfarmerv103"
    },
    @{
        ScriptFolder = "rs-perk-farmer-cl-refined-v2"
        PackagePath = "rsperkfarmerclrefinedv2"
    },
    @{
        ScriptFolder = "rs-perk-farmer-cl-world-boss-handler-test"
        PackagePath = "rsperkfarmerclworldbosshandlertest"
    },
    @{
        ScriptFolder = "pf-wbh-v125"
        PackagePath = "pfwbh125"
    },
    @{
        ScriptFolder = "rs-mechanics-capture"
        PackagePath = "rsmechanicscapture"
    }
)

New-Item -ItemType Directory -Force -Path $liveRoot | Out-Null

foreach ($mapping in $mappings) {
    $scriptRoot = Join-Path $WorkspaceRoot ("scripts\" + $mapping.ScriptFolder)
    $sourceRoot = Join-Path $scriptRoot "src"
    $sharedRoot = Join-Path $liveRoot $mapping.PackagePath

    if (-not (Test-Path $sourceRoot)) {
        Write-Host "Skipping missing source root: $sourceRoot"
        continue
    }

    New-Item -ItemType Directory -Force -Path $sharedRoot | Out-Null

    if ((Get-Item $sourceRoot).Attributes -band [IO.FileAttributes]::ReparsePoint) {
        Write-Host "Already linked: $sourceRoot -> $sharedRoot"
        continue
    }

    Copy-Item -Path (Join-Path $sourceRoot "*") -Destination $sharedRoot -Recurse -Force

    $backupRoot = Join-Path $scriptRoot ("src.backup-" + $timestamp)
    Move-Item -LiteralPath $sourceRoot -Destination $backupRoot
    New-Item -ItemType Junction -Path $sourceRoot -Target $sharedRoot | Out-Null

    Write-Host "Linked $sourceRoot -> $sharedRoot"
    Write-Host "Backup created at $backupRoot"
}

Write-Host ""
Write-Host "Live source root ready at $liveRoot"
