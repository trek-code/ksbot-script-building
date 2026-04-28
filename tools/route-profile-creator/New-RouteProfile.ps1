param(
    [string]$ServerName,
    [string]$BotFamily,
    [string]$RouteName,
    [string]$ActivityType,
    [string]$ResourceType
)

$ErrorActionPreference = "Stop"

$workspaceRoot = "D:\Codex GPT\RSPS\KSBOT Script building"
$templateRoot = Join-Path $workspaceRoot "templates\route-profiles"
$outputRoot = Join-Path $workspaceRoot "research\route-profiles"

function Read-RequiredValue {
    param(
        [string]$Prompt,
        [string]$CurrentValue
    )

    if (-not [string]::IsNullOrWhiteSpace($CurrentValue)) {
        return $CurrentValue.Trim()
    }

    do {
        $value = Read-Host $Prompt
    } while ([string]::IsNullOrWhiteSpace($value))

    return $value.Trim()
}

function Convert-ToSlug {
    param([string]$Value)

    $slug = $Value.ToLowerInvariant() -replace '[^a-z0-9]+', '-'
    $slug = $slug -replace '^-+', ''
    $slug = $slug -replace '-+$', ''

    if ([string]::IsNullOrWhiteSpace($slug)) {
        throw "Unable to create a slug from '$Value'."
    }

    return $slug
}

function Ensure-Directory {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Write-TemplateFile {
    param(
        [string]$TemplatePath,
        [string]$TargetPath,
        [hashtable]$Tokens
    )

    $content = Get-Content -Path $TemplatePath -Raw
    foreach ($key in $Tokens.Keys) {
        $content = $content.Replace("{{${key}}}", [string]$Tokens[$key])
    }
    Set-Content -Path $TargetPath -Value $content -NoNewline
}

$ServerName = Read-RequiredValue "Server name" $ServerName
$BotFamily = Read-RequiredValue "Bot family (example: woodcutting-bot)" $BotFamily
$RouteName = Read-RequiredValue "Route name (example: Draynor Willows)" $RouteName
$ActivityType = Read-RequiredValue "Activity type (example: woodcutting, mining, slayer)" $ActivityType
$ResourceType = Read-RequiredValue "Resource or target type (example: Willow Tree, Rune Ore, Goblin)" $ResourceType

$serverSlug = Convert-ToSlug $ServerName
$botFamilySlug = Convert-ToSlug $BotFamily
$routeSlug = Convert-ToSlug $RouteName

$routeRoot = Join-Path $outputRoot $serverSlug
$routeRoot = Join-Path $routeRoot $botFamilySlug
$routeRoot = Join-Path $routeRoot $routeSlug

$capturesDir = Join-Path $routeRoot "captures"
$exportsDir = Join-Path $routeRoot "exports"
$screenshotsDir = Join-Path $routeRoot "screenshots"

Ensure-Directory $routeRoot
Ensure-Directory $capturesDir
Ensure-Directory $exportsDir
Ensure-Directory $screenshotsDir

$tokens = @{
    SERVER_NAME = $ServerName
    SERVER_SLUG = $serverSlug
    BOT_FAMILY = $BotFamily
    BOT_FAMILY_SLUG = $botFamilySlug
    ROUTE_NAME = $RouteName
    ROUTE_SLUG = $routeSlug
    ACTIVITY_TYPE = $ActivityType
    RESOURCE_TYPE = $ResourceType
    CREATED_DATE = (Get-Date -Format "yyyy-MM-dd")
    ROUTE_ROOT = $routeRoot
    CAPTURES_DIR = $capturesDir
    EXPORTS_DIR = $exportsDir
    SCREENSHOTS_DIR = $screenshotsDir
}

Write-TemplateFile `
    -TemplatePath (Join-Path $templateRoot "route-profile.template.json") `
    -TargetPath (Join-Path $routeRoot "route-profile.json") `
    -Tokens $tokens

Write-TemplateFile `
    -TemplatePath (Join-Path $templateRoot "route-notes.template.md") `
    -TargetPath (Join-Path $routeRoot "route-notes.md") `
    -Tokens $tokens

Write-TemplateFile `
    -TemplatePath (Join-Path $templateRoot "test-checklist.template.md") `
    -TargetPath (Join-Path $routeRoot "test-checklist.md") `
    -Tokens $tokens

Write-TemplateFile `
    -TemplatePath (Join-Path $templateRoot "route-intake.template.md") `
    -TargetPath (Join-Path $routeRoot "route-intake.md") `
    -Tokens $tokens

@"
Route profile scaffold created.

Route root:
$routeRoot

Created files:
- route-profile.json
- route-notes.md
- test-checklist.md
- route-intake.md

Created directories:
- captures\
- exports\
- screenshots\
"@ | Write-Host
