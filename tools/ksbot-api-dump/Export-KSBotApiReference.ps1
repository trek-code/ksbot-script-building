param(
    [string]$JarPath = "D:\Codex GPT\RSPS\KSBOT Script building\ksbot\rs.kreme.nearreality-api.jar",
    [string]$OutputRoot = "D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\reference"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar not found: $JarPath"
}

function Get-PackageName {
    param([string]$ClassName)
    $lastDot = $ClassName.LastIndexOf('.')
    if ($lastDot -lt 0) { return "" }
    return $ClassName.Substring(0, $lastDot)
}

function Get-SimpleName {
    param([string]$ClassName)
    $lastDot = $ClassName.LastIndexOf('.')
    if ($lastDot -lt 0) { return $ClassName }
    return $ClassName.Substring($lastDot + 1)
}

function Get-SafeFileName {
    param([string]$Name)
    return $Name.Replace('$', '__')
}

function Get-PackageOutputDir {
    param(
        [string]$PackageName,
        [string]$Root
    )
    $relative = $PackageName.Replace('rs.kreme.ksbot.api', '').TrimStart('.')
    if ([string]::IsNullOrWhiteSpace($relative)) {
        return $Root
    }
    return (Join-Path $Root ($relative.Replace('.', '\')))
}

New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

$classEntries = jar tf $JarPath | Where-Object {
    $_ -like 'rs/kreme/ksbot/api/*.class' -or
    $_ -like 'rs/kreme/ksbot/api/*/*.class' -or
    $_ -like 'rs/kreme/ksbot/api/*/*/*.class'
}

$classNames = @()
foreach ($entry in $classEntries) {
    if ($entry.EndsWith('/')) { continue }
    $classNames += $entry.Replace('/', '.').Replace('.class', '')
}

$grouped = @{}
foreach ($className in ($classNames | Sort-Object)) {
    $packageName = Get-PackageName -ClassName $className
    if (-not $grouped.ContainsKey($packageName)) {
        $grouped[$packageName] = @()
    }
    $grouped[$packageName] += $className
}

$groupOrder = @(
    'rs.kreme.ksbot.api',
    'rs.kreme.ksbot.api.commons',
    'rs.kreme.ksbot.api.game',
    'rs.kreme.ksbot.api.game.magic',
    'rs.kreme.ksbot.api.game.pathing',
    'rs.kreme.ksbot.api.game.utils',
    'rs.kreme.ksbot.api.hooks',
    'rs.kreme.ksbot.api.hooks.widgets',
    'rs.kreme.ksbot.api.queries',
    'rs.kreme.ksbot.api.wrappers',
    'rs.kreme.ksbot.api.scripts',
    'rs.kreme.ksbot.api.scripts.config',
    'rs.kreme.ksbot.api.scripts.config.annotation',
    'rs.kreme.ksbot.api.scripts.randoms',
    'rs.kreme.ksbot.api.scripts.task',
    'rs.kreme.ksbot.api.antibot',
    'rs.kreme.ksbot.api.interfaces'
)

foreach ($packageName in ($grouped.Keys | Sort-Object)) {
    $packageDir = Get-PackageOutputDir -PackageName $packageName -Root $OutputRoot
    New-Item -ItemType Directory -Path $packageDir -Force | Out-Null

    $packageIndex = @()
    $packageIndex += "# $packageName"
    $packageIndex += ""
    $packageIndex += "Generated from:"
    $packageIndex += ('- ``{0}``' -f $JarPath)
    $packageIndex += ""
    $packageIndex += "## Classes"
    $packageIndex += ""

    foreach ($className in ($grouped[$packageName] | Sort-Object)) {
        $simpleName = Get-SimpleName -ClassName $className
        $safeFileName = Get-SafeFileName -Name $simpleName
        $targetFile = Join-Path $packageDir ($safeFileName + ".md")
        $signatureDump = javap -classpath $JarPath $className 2>&1

        $content = @()
        $content += "# $className"
        $content += ""
        $content += ('Package: ``{0}``' -f $packageName)
        $content += ""
        $content += "Generated from:"
        $content += ('- ``{0}``' -f $JarPath)
        $content += ""
        $content += "## Public Signature Dump"
        $content += ""
        $content += '```text'
        foreach ($line in $signatureDump) {
            $content += [string]$line
        }
        $content += '```'

        Set-Content -LiteralPath $targetFile -Value $content -Encoding UTF8
        $packageIndex += ('- [{0}](./{1}.md)' -f $simpleName, $safeFileName)
    }

    Set-Content -LiteralPath (Join-Path $packageDir 'README.md') -Value $packageIndex -Encoding UTF8
}

$orderedPackages = @()
foreach ($packageName in $groupOrder) {
    if ($grouped.ContainsKey($packageName)) {
        $orderedPackages += $packageName
    }
}
foreach ($packageName in ($grouped.Keys | Sort-Object)) {
    if ($orderedPackages -notcontains $packageName) {
        $orderedPackages += $packageName
    }
}

$rootIndex = @()
$rootIndex += "# KSBot API Reference Index"
$rootIndex += ""
$rootIndex += "Generated local API reference from:"
$rootIndex += ('- ``{0}``' -f $JarPath)
$rootIndex += ""
$rootIndex += "This index is the jar-derived reference companion to:"
$rootIndex += '- ``D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\ksbot-api-booklet.md``'
$rootIndex += '- ``D:\Codex GPT\RSPS\KSBOT Script building\docs\ksbot-api\woodcutting-framework-guide.md``'
$rootIndex += ""
$rootIndex += "## Categories"
$rootIndex += ""

foreach ($packageName in $orderedPackages) {
    $relative = $packageName.Replace('rs.kreme.ksbot.api', '').TrimStart('.')
    if ([string]::IsNullOrWhiteSpace($relative)) {
        $readmePath = "./README.md"
    } else {
        $readmePath = "./" + ($relative.Replace('.', '/')) + "/README.md"
    }
    $rootIndex += ('- [{0}]({1})' -f $packageName, $readmePath)
}

$rootIndex += ""
$rootIndex += "## Notes"
$rootIndex += ""
$rootIndex += "- Inner classes are included as separate files using __ in place of $ in filenames."
$rootIndex += "- This dump captures callable signatures, not prose explanations."
$rootIndex += "- Use the booklet for implementation guidance and this reference for exact class and method lookup."

Set-Content -LiteralPath (Join-Path $OutputRoot 'README.md') -Value $rootIndex -Encoding UTF8

Write-Host "Generated KSBot API reference at:"
Write-Host $OutputRoot
