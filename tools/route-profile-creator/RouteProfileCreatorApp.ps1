Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

$workspaceRoot = "D:\Codex GPT\RSPS\KSBOT Script building"
$templateRoot = Join-Path $workspaceRoot "templates\route-profiles"
$outputRoot = Join-Path $workspaceRoot "research\route-profiles"
$mappingSessionRoot = Join-Path $workspaceRoot "research\mapping-sessions"

$serverPresets = @("Reason", "Near-Reality")
$botFamilyPresets = @("woodcutting-bot", "fishing-bot", "mining-bot", "thieving-bot", "slayer-bot", "bossing-bot", "mapping-tool", "generic-skilling-bot")
$activityPresets = @("woodcutting", "fishing", "mining", "slayer", "bossing", "restocking", "navigation", "mapping")
$captureCategories = @(
    "teleportDestination",
    "resourceAnchor",
    "returnAnchor",
    "bankDoor",
    "bankInside",
    "bankStand",
    "bankObject",
    "safeStand",
    "resourceTile",
    "npcTarget",
    "groundItemTarget",
    "bankTarget",
    "pathToResource",
    "pathToBank",
    "pathReturn",
    "door",
    "gate",
    "ladder",
    "stairs",
    "portal",
    "obstacle",
    "custom"
)

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

function Convert-ActionTextToArray {
    param([string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return @()
    }

    return @(
        $Value -split '[,|]' |
            ForEach-Object { $_.Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    )
}

function New-CaptureRecord {
    param(
        [string]$Category,
        [string]$Label,
        [string]$X,
        [string]$Y,
        [string]$Plane,
        [string]$Id,
        [string]$Actions,
        [string]$Notes
    )

    $record = [ordered]@{
        category = $Category
        label = $Label
        x = if ($X -match '^-?\d+$') { [int]$X } else { $X }
        y = if ($Y -match '^-?\d+$') { [int]$Y } else { $Y }
        plane = if ($Plane -match '^-?\d+$') { [int]$Plane } else { 0 }
        id = if ($Id -match '^-?\d+$') { [int]$Id } else { $Id }
        actions = @(Convert-ActionTextToArray $Actions)
        notes = $Notes
    }

    return $record
}

function Resolve-CaptureCategory {
    param(
        [string]$SourceType,
        [string]$Name,
        [string[]]$Actions
    )

    $actionsText = (($Actions | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join " ").ToLowerInvariant()
    $nameText = ([string]$Name).ToLowerInvariant()
    $source = ([string]$SourceType).ToLowerInvariant()

    if ($actionsText -match '\bbank\b' -or $actionsText -match '\bcollect\b') {
        return "bankTarget"
    }

    if ($nameText -match 'tree|oak|willow|maple|yew|magic|redwood') {
        if ($source -eq "npc") {
            return "npcTarget"
        }
        if ($source -eq "grounditem") {
            return "groundItemTarget"
        }
        return "resourceTile"
    }

    if ($source -eq "npc") {
        return "npcTarget"
    }

    if ($source -eq "grounditem") {
        return "groundItemTarget"
    }

    return "custom"
}

function New-EmptyRouteProfile {
    param(
        [string]$ServerName,
        [string]$BotFamily,
        [string]$RouteName,
        [string]$ActivityType,
        [string]$ResourceType,
        [string]$RouteRoot,
        [string]$CapturesDir,
        [string]$ExportsDir,
        [string]$ScreenshotsDir
    )

    $serverSlug = Convert-ToSlug $ServerName
    $botFamilySlug = Convert-ToSlug $BotFamily
    $routeSlug = Convert-ToSlug $RouteName

    return [ordered]@{
        meta = [ordered]@{
            serverName = $ServerName
            serverSlug = $serverSlug
            botFamily = $BotFamily
            botFamilySlug = $botFamilySlug
            routeName = $RouteName
            routeSlug = $routeSlug
            activityType = $ActivityType
            resourceType = $ResourceType
            createdDate = (Get-Date -Format "yyyy-MM-dd")
            status = "draft"
        }
        requirements = [ordered]@{
            levelRequirement = ""
            itemRequirements = @()
            accountRequirements = @()
            teleportRequirements = @()
            notes = @()
        }
        routeAnchors = [ordered]@{
            teleportDestination = @()
            treeOrTargetAnchor = @()
            returnTargetAnchor = @()
            bankDoorTiles = @()
            bankInsideTiles = @()
            bankStandTile = @()
            bankObjectTile = @()
            safeStandTiles = @()
        }
        resourceTargets = [ordered]@{
            names = @()
            objectIds = @()
            npcIds = @()
            groundItemIds = @()
            tiles = @()
        }
        bankTargets = [ordered]@{
            type = "booth"
            objectIds = @()
            npcIds = @()
            actions = @()
            tiles = @()
        }
        pathChains = [ordered]@{
            toResource = @()
            toBank = @()
            returnToResource = @()
            alternatePaths = @()
        }
        interactionTargets = [ordered]@{
            doors = @()
            gates = @()
            ladders = @()
            stairs = @()
            portals = @()
            obstacles = @()
        }
        walkerHints = [ordered]@{
            preferredScanRadius = 16
            avoidTiles = @()
            blockedTileFlags = @()
            regionNotes = @()
            customServerWarnings = @()
        }
        loot = [ordered]@{
            defaultEnabled = $false
            suggestedItems = @()
            pickupStyle = "immediate"
        }
        sourceData = [ordered]@{
            importedMapperExports = @()
            manualCaptureRows = @()
        }
        testStatus = [ordered]@{
            choppingOrInteraction = ""
            banking = ""
            powercutting = ""
            looting = ""
            knownIssues = @()
            nextFixes = @()
        }
        paths = [ordered]@{
            routeRoot = $RouteRoot
            capturesDir = $CapturesDir
            exportsDir = $ExportsDir
            screenshotsDir = $ScreenshotsDir
        }
    }
}

function Save-JsonFile {
    param(
        [object]$Data,
        [string]$Path
    )

    $json = $Data | ConvertTo-Json -Depth 12
    Set-Content -Path $Path -Value $json
}

function Get-RoutePaths {
    param(
        [string]$ServerName,
        [string]$BotFamily,
        [string]$RouteName
    )

    $serverSlug = Convert-ToSlug $ServerName
    $botFamilySlug = Convert-ToSlug $BotFamily
    $routeSlug = Convert-ToSlug $RouteName

    $routeRoot = Join-Path $outputRoot $serverSlug
    $routeRoot = Join-Path $routeRoot $botFamilySlug
    $routeRoot = Join-Path $routeRoot $routeSlug

    return [ordered]@{
        RouteRoot = $routeRoot
        CapturesDir = Join-Path $routeRoot "captures"
        ExportsDir = Join-Path $routeRoot "exports"
        ScreenshotsDir = Join-Path $routeRoot "screenshots"
        ProfilePath = Join-Path $routeRoot "route-profile.json"
        NotesPath = Join-Path $routeRoot "route-notes.md"
        ChecklistPath = Join-Path $routeRoot "test-checklist.md"
        IntakePath = Join-Path $routeRoot "route-intake.md"
    }
}

function Ensure-RouteScaffold {
    param(
        [string]$ServerName,
        [string]$BotFamily,
        [string]$RouteName,
        [string]$ActivityType,
        [string]$ResourceType
    )

    $paths = Get-RoutePaths -ServerName $ServerName -BotFamily $BotFamily -RouteName $RouteName

    Ensure-Directory $paths.RouteRoot
    Ensure-Directory $paths.CapturesDir
    Ensure-Directory $paths.ExportsDir
    Ensure-Directory $paths.ScreenshotsDir

    $tokens = @{
        SERVER_NAME = $ServerName
        SERVER_SLUG = Convert-ToSlug $ServerName
        BOT_FAMILY = $BotFamily
        BOT_FAMILY_SLUG = Convert-ToSlug $BotFamily
        ROUTE_NAME = $RouteName
        ROUTE_SLUG = Convert-ToSlug $RouteName
        ACTIVITY_TYPE = $ActivityType
        RESOURCE_TYPE = $ResourceType
        CREATED_DATE = (Get-Date -Format "yyyy-MM-dd")
        ROUTE_ROOT = $paths.RouteRoot
        CAPTURES_DIR = $paths.CapturesDir
        EXPORTS_DIR = $paths.ExportsDir
        SCREENSHOTS_DIR = $paths.ScreenshotsDir
    }

    if (-not (Test-Path $paths.ProfilePath)) {
        Write-TemplateFile -TemplatePath (Join-Path $templateRoot "route-profile.template.json") -TargetPath $paths.ProfilePath -Tokens $tokens
    }
    if (-not (Test-Path $paths.NotesPath)) {
        Write-TemplateFile -TemplatePath (Join-Path $templateRoot "route-notes.template.md") -TargetPath $paths.NotesPath -Tokens $tokens
    }
    if (-not (Test-Path $paths.ChecklistPath)) {
        Write-TemplateFile -TemplatePath (Join-Path $templateRoot "test-checklist.template.md") -TargetPath $paths.ChecklistPath -Tokens $tokens
    }
    if (-not (Test-Path $paths.IntakePath)) {
        Write-TemplateFile -TemplatePath (Join-Path $templateRoot "route-intake.template.md") -TargetPath $paths.IntakePath -Tokens $tokens
    }

    return $paths
}

function Save-RouteProfileData {
    param(
        [string]$ProfilePath,
        [hashtable]$Inputs,
        [System.Windows.Forms.DataGridView]$Grid,
        [string[]]$ImportedExports
    )

    $data = New-EmptyRouteProfile `
        -ServerName $Inputs.ServerName `
        -BotFamily $Inputs.BotFamily `
        -RouteName $Inputs.RouteName `
        -ActivityType $Inputs.ActivityType `
        -ResourceType $Inputs.ResourceType `
        -RouteRoot $Inputs.RouteRoot `
        -CapturesDir $Inputs.CapturesDir `
        -ExportsDir $Inputs.ExportsDir `
        -ScreenshotsDir $Inputs.ScreenshotsDir

    $manualRows = @()
    foreach ($row in $Grid.Rows) {
        if ($row.IsNewRow) {
            continue
        }

        $category = [string]$row.Cells["Category"].Value
        $label = [string]$row.Cells["Label"].Value
        $x = [string]$row.Cells["X"].Value
        $y = [string]$row.Cells["Y"].Value
        $plane = [string]$row.Cells["Plane"].Value
        $id = [string]$row.Cells["Id"].Value
        $actions = [string]$row.Cells["Actions"].Value
        $notes = [string]$row.Cells["Notes"].Value

        if ([string]::IsNullOrWhiteSpace($category) -and [string]::IsNullOrWhiteSpace($label) -and [string]::IsNullOrWhiteSpace($x) -and [string]::IsNullOrWhiteSpace($y)) {
            continue
        }

        if ([string]::IsNullOrWhiteSpace($category)) {
            $category = "custom"
        }

        $record = New-CaptureRecord -Category $category -Label $label -X $x -Y $y -Plane $plane -Id $id -Actions $actions -Notes $notes
        $manualRows += $record

        switch ($category) {
            "teleportDestination" { $data.routeAnchors.teleportDestination += $record }
            "resourceAnchor" { $data.routeAnchors.treeOrTargetAnchor += $record }
            "returnAnchor" { $data.routeAnchors.returnTargetAnchor += $record }
            "bankDoor" { $data.routeAnchors.bankDoorTiles += $record }
            "bankInside" { $data.routeAnchors.bankInsideTiles += $record }
            "bankStand" { $data.routeAnchors.bankStandTile += $record }
            "bankObject" {
                $data.routeAnchors.bankObjectTile += $record
                $data.bankTargets.tiles += $record
                if ($record.id -is [int]) {
                    $data.bankTargets.objectIds += $record.id
                }
                if ($record.actions.Count -gt 0) {
                    $data.bankTargets.actions += $record.actions
                }
            }
            "safeStand" { $data.routeAnchors.safeStandTiles += $record }
            "resourceTile" {
                $data.resourceTargets.tiles += $record
                if ($record.id -is [int]) {
                    $data.resourceTargets.objectIds += $record.id
                }
            }
            "npcTarget" {
                $data.resourceTargets.tiles += $record
                if ($record.id -is [int]) {
                    $data.resourceTargets.npcIds += $record.id
                }
            }
            "groundItemTarget" {
                $data.resourceTargets.tiles += $record
                if ($record.id -is [int]) {
                    $data.resourceTargets.groundItemIds += $record.id
                }
            }
            "bankTarget" {
                $data.bankTargets.tiles += $record
                if ($record.id -is [int]) {
                    if ([string]$record.notes -match 'source:npc') {
                        $data.bankTargets.npcIds += $record.id
                    } else {
                        $data.bankTargets.objectIds += $record.id
                    }
                }
                if ($record.actions.Count -gt 0) {
                    $data.bankTargets.actions += $record.actions
                }
            }
            "pathToResource" { $data.pathChains.toResource += $record }
            "pathToBank" { $data.pathChains.toBank += $record }
            "pathReturn" { $data.pathChains.returnToResource += $record }
            "door" { $data.interactionTargets.doors += $record }
            "gate" { $data.interactionTargets.gates += $record }
            "ladder" { $data.interactionTargets.ladders += $record }
            "stairs" { $data.interactionTargets.stairs += $record }
            "portal" { $data.interactionTargets.portals += $record }
            "obstacle" { $data.interactionTargets.obstacles += $record }
            default { }
        }
    }

    $data.sourceData.manualCaptureRows = $manualRows
    $data.sourceData.importedMapperExports = @($ImportedExports | Sort-Object -Unique)
    $data.bankTargets.objectIds = @($data.bankTargets.objectIds | Sort-Object -Unique)
    $data.bankTargets.npcIds = @($data.bankTargets.npcIds | Sort-Object -Unique)
    $data.bankTargets.actions = @($data.bankTargets.actions | Sort-Object -Unique)
    $data.resourceTargets.objectIds = @($data.resourceTargets.objectIds | Sort-Object -Unique)
    $data.resourceTargets.npcIds = @($data.resourceTargets.npcIds | Sort-Object -Unique)
    $data.resourceTargets.groundItemIds = @($data.resourceTargets.groundItemIds | Sort-Object -Unique)

    Save-JsonFile -Data $data -Path $ProfilePath
}

function Add-DefaultRow {
    param(
        [System.Windows.Forms.DataGridView]$Grid,
        [string]$Category = "resourceTile"
    )

    $index = $Grid.Rows.Add()
    $Grid.Rows[$index].Cells["Category"].Value = $Category
    $Grid.Rows[$index].Cells["Plane"].Value = 0
}

function Open-PathIfExists {
    param([string]$Path)

    if (-not [string]::IsNullOrWhiteSpace($Path) -and (Test-Path $Path)) {
        Start-Process explorer.exe $Path
    }
}

function Get-LatestMapperExport {
    param([string]$ServerName)

    $serverSlug = Convert-ToSlug $ServerName
    $searchRoot = Join-Path $mappingSessionRoot $serverSlug
    if (-not (Test-Path $searchRoot)) {
        return $null
    }

    return Get-ChildItem $searchRoot -Filter "*.json" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
}

$form = New-Object System.Windows.Forms.Form
$form.Text = "Route Profile Workbench"
$form.StartPosition = "CenterScreen"
$form.Size = New-Object System.Drawing.Size(1220, 860)
$form.MinimumSize = New-Object System.Drawing.Size(1100, 760)
$form.BackColor = [System.Drawing.Color]::FromArgb(17, 20, 27)
$form.ForeColor = [System.Drawing.Color]::White
$form.Font = New-Object System.Drawing.Font("Segoe UI", 9.5)
$form.AutoScaleMode = [System.Windows.Forms.AutoScaleMode]::Dpi

$title = New-Object System.Windows.Forms.Label
$title.Text = "Route Profile Workbench"
$title.Location = New-Object System.Drawing.Point(24, 18)
$title.Size = New-Object System.Drawing.Size(420, 30)
$title.Font = New-Object System.Drawing.Font("Segoe UI Semibold", 17)
$title.ForeColor = [System.Drawing.Color]::FromArgb(242, 245, 248)
$title.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left
$form.Controls.Add($title)

$subtitle = New-Object System.Windows.Forms.Label
$subtitle.Text = "Create reusable route folders, capture coordinates cleanly, and import mapper exports without living in shell prompts."
$subtitle.Location = New-Object System.Drawing.Point(24, 52)
$subtitle.Size = New-Object System.Drawing.Size(1120, 24)
$subtitle.ForeColor = [System.Drawing.Color]::FromArgb(165, 179, 193)
$subtitle.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($subtitle)

$newRouteToolbarButton = New-Object System.Windows.Forms.Button
$newRouteToolbarButton.Text = "+ New"
$newRouteToolbarButton.Size = New-Object System.Drawing.Size(92, 34)
$newRouteToolbarButton.Location = New-Object System.Drawing.Point(1030, 24)
$newRouteToolbarButton.BackColor = [System.Drawing.Color]::FromArgb(61, 126, 92)
$newRouteToolbarButton.ForeColor = [System.Drawing.Color]::White
$newRouteToolbarButton.FlatStyle = "Flat"
$newRouteToolbarButton.FlatAppearance.BorderSize = 0
$newRouteToolbarButton.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($newRouteToolbarButton)

$openRouteToolbarButton = New-Object System.Windows.Forms.Button
$openRouteToolbarButton.Text = "Open"
$openRouteToolbarButton.Size = New-Object System.Drawing.Size(92, 34)
$openRouteToolbarButton.Location = New-Object System.Drawing.Point(1132, 24)
$openRouteToolbarButton.BackColor = [System.Drawing.Color]::FromArgb(53, 86, 128)
$openRouteToolbarButton.ForeColor = [System.Drawing.Color]::White
$openRouteToolbarButton.FlatStyle = "Flat"
$openRouteToolbarButton.FlatAppearance.BorderSize = 0
$openRouteToolbarButton.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($openRouteToolbarButton)

$leftPanel = New-Object System.Windows.Forms.Panel
$leftPanel.Location = New-Object System.Drawing.Point(24, 96)
$leftPanel.Size = New-Object System.Drawing.Size(360, 700)
$leftPanel.BackColor = [System.Drawing.Color]::FromArgb(23, 28, 36)
$leftPanel.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Bottom -bor [System.Windows.Forms.AnchorStyles]::Left
$form.Controls.Add($leftPanel)

$rightPanel = New-Object System.Windows.Forms.Panel
$rightPanel.Location = New-Object System.Drawing.Point(404, 96)
$rightPanel.Size = New-Object System.Drawing.Size(780, 700)
$rightPanel.BackColor = [System.Drawing.Color]::FromArgb(23, 28, 36)
$rightPanel.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Bottom -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$form.Controls.Add($rightPanel)

function New-UiLabel {
    param(
        [string]$Text,
        [int]$X,
        [int]$Y,
        [int]$Width = 300,
        [int]$Height = 22
    )

    $label = New-Object System.Windows.Forms.Label
    $label.Text = $Text
    $label.Location = New-Object System.Drawing.Point($X, $Y)
    $label.Size = New-Object System.Drawing.Size($Width, $Height)
    $label.ForeColor = [System.Drawing.Color]::FromArgb(232, 237, 242)
    return $label
}

function New-UiTextBox {
    param(
        [int]$X,
        [int]$Y,
        [int]$Width = 310,
        [string]$Text = ""
    )

    $tb = New-Object System.Windows.Forms.TextBox
    $tb.Location = New-Object System.Drawing.Point($X, $Y)
    $tb.Size = New-Object System.Drawing.Size($Width, 28)
    $tb.Text = $Text
    $tb.BackColor = [System.Drawing.Color]::FromArgb(31, 38, 48)
    $tb.ForeColor = [System.Drawing.Color]::White
    $tb.BorderStyle = "FixedSingle"
    return $tb
}

function New-UiCombo {
    param(
        [int]$X,
        [int]$Y,
        [string[]]$Items,
        [string]$DefaultValue,
        [int]$Width = 310
    )

    $combo = New-Object System.Windows.Forms.ComboBox
    $combo.Location = New-Object System.Drawing.Point($X, $Y)
    $combo.Size = New-Object System.Drawing.Size($Width, 28)
    $combo.DropDownStyle = "DropDown"
    $combo.BackColor = [System.Drawing.Color]::FromArgb(31, 38, 48)
    $combo.ForeColor = [System.Drawing.Color]::White
    [void]$combo.Items.AddRange($Items)
    $combo.Text = $DefaultValue
    return $combo
}

function New-UiButton {
    param(
        [string]$Text,
        [int]$X,
        [int]$Y,
        [int]$Width,
        [int]$Height,
        [System.Drawing.Color]$Color
    )

    $btn = New-Object System.Windows.Forms.Button
    $btn.Text = $Text
    $btn.Location = New-Object System.Drawing.Point($X, $Y)
    $btn.Size = New-Object System.Drawing.Size($Width, $Height)
    $btn.BackColor = $Color
    $btn.ForeColor = [System.Drawing.Color]::White
    $btn.FlatStyle = "Flat"
    $btn.FlatAppearance.BorderSize = 0
    return $btn
}

function Show-NewRouteDialog {
    $dialog = New-Object System.Windows.Forms.Form
    $dialog.Text = "Create New Item"
    $dialog.StartPosition = "CenterParent"
    $dialog.Size = New-Object System.Drawing.Size(430, 420)
    $dialog.MinimumSize = New-Object System.Drawing.Size(430, 420)
    $dialog.BackColor = [System.Drawing.Color]::FromArgb(17, 20, 27)
    $dialog.ForeColor = [System.Drawing.Color]::White
    $dialog.Font = New-Object System.Drawing.Font("Segoe UI", 9.5)
    $dialog.FormBorderStyle = [System.Windows.Forms.FormBorderStyle]::FixedDialog
    $dialog.MaximizeBox = $false
    $dialog.MinimizeBox = $false
    $dialog.Padding = New-Object System.Windows.Forms.Padding(8)

    $kindLabel = New-UiLabel -Text "What would you like to create?" -X 24 -Y 20 -Width 340
    $kindLabel.Font = New-Object System.Drawing.Font("Segoe UI Semibold", 13)
    $dialog.Controls.Add($kindLabel)

    $kindCombo = New-UiCombo -X 24 -Y 54 -Items @("Route") -DefaultValue "Route" -Width 350
    $kindCombo.DropDownStyle = "DropDownList"
    $dialog.Controls.Add($kindCombo)

    $serverLabelModal = New-UiLabel -Text "Server Name" -X 24 -Y 96 -Width 320
    $serverBoxModal = New-UiCombo -X 24 -Y 120 -Items $serverPresets -DefaultValue $serverBox.Text -Width 350
    $dialog.Controls.Add($serverLabelModal)
    $dialog.Controls.Add($serverBoxModal)

    $botLabelModal = New-UiLabel -Text "Bot Family" -X 24 -Y 162 -Width 320
    $botBoxModal = New-UiCombo -X 24 -Y 186 -Items $botFamilyPresets -DefaultValue $botFamilyBox.Text -Width 350
    $dialog.Controls.Add($botLabelModal)
    $dialog.Controls.Add($botBoxModal)

    $routeLabelModal = New-UiLabel -Text "Route Name" -X 24 -Y 228 -Width 320
    $routeBoxModal = New-UiTextBox -X 24 -Y 252 -Width 350 -Text ""
    $dialog.Controls.Add($routeLabelModal)
    $dialog.Controls.Add($routeBoxModal)

    $activityLabelModal = New-UiLabel -Text "Activity Type" -X 24 -Y 294 -Width 320
    $activityBoxModal = New-UiCombo -X 24 -Y 318 -Items $activityPresets -DefaultValue $activityBox.Text -Width 170
    $resourceBoxModal = New-UiTextBox -X 204 -Y 318 -Width 170 -Text ""
    $resourceBoxModal.PlaceholderText = "Resource / target"
    $dialog.Controls.Add($activityLabelModal)
    $dialog.Controls.Add($activityBoxModal)
    $dialog.Controls.Add($resourceBoxModal)

    $createModalButton = New-UiButton -Text "Create" -X 198 -Y 356 -Width 84 -Height 30 -Color ([System.Drawing.Color]::FromArgb(61, 126, 92))
    $cancelModalButton = New-UiButton -Text "Cancel" -X 290 -Y 356 -Width 84 -Height 30 -Color ([System.Drawing.Color]::FromArgb(72, 80, 94))
    $dialog.Controls.Add($createModalButton)
    $dialog.Controls.Add($cancelModalButton)

    $dialog.Tag = $null

    $createModalButton.Add_Click({
        if ([string]::IsNullOrWhiteSpace($serverBoxModal.Text) -or
            [string]::IsNullOrWhiteSpace($botBoxModal.Text) -or
            [string]::IsNullOrWhiteSpace($routeBoxModal.Text) -or
            [string]::IsNullOrWhiteSpace($activityBoxModal.Text) -or
            [string]::IsNullOrWhiteSpace($resourceBoxModal.Text)) {
            [System.Windows.Forms.MessageBox]::Show("Fill in every field before creating a route.", "Route Profile Workbench")
            return
        }

        $dialog.Tag = [ordered]@{
            Kind = [string]$kindCombo.Text
            ServerName = [string]$serverBoxModal.Text.Trim()
            BotFamily = [string]$botBoxModal.Text.Trim()
            RouteName = [string]$routeBoxModal.Text.Trim()
            ActivityType = [string]$activityBoxModal.Text.Trim()
            ResourceType = [string]$resourceBoxModal.Text.Trim()
        }
        $dialog.DialogResult = [System.Windows.Forms.DialogResult]::OK
        $dialog.Close()
    })

    $cancelModalButton.Add_Click({
        $dialog.DialogResult = [System.Windows.Forms.DialogResult]::Cancel
        $dialog.Close()
    })

    if ($dialog.ShowDialog($form) -eq [System.Windows.Forms.DialogResult]::OK) {
        return $dialog.Tag
    }

    return $null
}

$serverLabel = New-UiLabel -Text "Server Name" -X 20 -Y 22
$serverBox = New-UiCombo -X 20 -Y 46 -Items $serverPresets -DefaultValue "Reason"
$leftPanel.Controls.Add($serverLabel)
$leftPanel.Controls.Add($serverBox)

$botFamilyLabel = New-UiLabel -Text "Bot Family" -X 20 -Y 88
$botFamilyBox = New-UiCombo -X 20 -Y 112 -Items $botFamilyPresets -DefaultValue "woodcutting-bot"
$leftPanel.Controls.Add($botFamilyLabel)
$leftPanel.Controls.Add($botFamilyBox)

$routeNameLabel = New-UiLabel -Text "Route Name" -X 20 -Y 154
$routeNameBox = New-UiTextBox -X 20 -Y 178
$leftPanel.Controls.Add($routeNameLabel)
$leftPanel.Controls.Add($routeNameBox)

$activityLabel = New-UiLabel -Text "Activity Type" -X 20 -Y 220
$activityBox = New-UiCombo -X 20 -Y 244 -Items $activityPresets -DefaultValue "woodcutting"
$leftPanel.Controls.Add($activityLabel)
$leftPanel.Controls.Add($activityBox)

$resourceLabel = New-UiLabel -Text "Resource / Target Type" -X 20 -Y 286
$resourceBox = New-UiTextBox -X 20 -Y 310
$leftPanel.Controls.Add($resourceLabel)
$leftPanel.Controls.Add($resourceBox)

$createButton = New-UiButton -Text "Refresh Current Route" -X 20 -Y 360 -Width 310 -Height 42 -Color ([System.Drawing.Color]::FromArgb(61, 126, 92))
$leftPanel.Controls.Add($createButton)

$openRouteButton = New-UiButton -Text "Open Route Folder" -X 20 -Y 418 -Width 150 -Height 36 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$openCapturesButton = New-UiButton -Text "Open Captures" -X 180 -Y 418 -Width 150 -Height 36 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$openExportsButton = New-UiButton -Text "Open Exports" -X 20 -Y 464 -Width 150 -Height 36 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$openScreensButton = New-UiButton -Text "Open Screenshots" -X 180 -Y 464 -Width 150 -Height 36 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$leftPanel.Controls.Add($openRouteButton)
$leftPanel.Controls.Add($openCapturesButton)
$leftPanel.Controls.Add($openExportsButton)
$leftPanel.Controls.Add($openScreensButton)

$importLatestButton = New-UiButton -Text "Import Latest Mapper Export" -X 20 -Y 524 -Width 310 -Height 36 -Color ([System.Drawing.Color]::FromArgb(126, 97, 45))
$importFileButton = New-UiButton -Text "Import Selected Mapper Export" -X 20 -Y 570 -Width 310 -Height 36 -Color ([System.Drawing.Color]::FromArgb(126, 97, 45))
$importClipboardButton = New-UiButton -Text "Import Clipboard Capture" -X 20 -Y 616 -Width 310 -Height 36 -Color ([System.Drawing.Color]::FromArgb(95, 78, 134))
$leftPanel.Controls.Add($importLatestButton)
$leftPanel.Controls.Add($importFileButton)
$leftPanel.Controls.Add($importClipboardButton)

$statusLabel = New-UiLabel -Text "No route loaded." -X 20 -Y 670 -Width 310 -Height 22
$statusLabel.ForeColor = [System.Drawing.Color]::FromArgb(181, 196, 208)
$statusLabel.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right -bor [System.Windows.Forms.AnchorStyles]::Bottom
$leftPanel.Controls.Add($statusLabel)

$routeRootBox = New-UiTextBox -X 20 -Y 696 -Width 310 -Text ""
$routeRootBox.ReadOnly = $true
$routeRootBox.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right -bor [System.Windows.Forms.AnchorStyles]::Bottom
$leftPanel.Controls.Add($routeRootBox)

$gridTitle = New-UiLabel -Text "Capture Rows" -X 18 -Y 18 -Width 180 -Height 26
$gridTitle.Font = New-Object System.Drawing.Font("Segoe UI Semibold", 13)
$rightPanel.Controls.Add($gridTitle)

$gridHint = New-UiLabel -Text "Add tiles, ids, actions, and notes here. These rows get written back into route-profile.json in a reusable structure." -X 18 -Y 48 -Width 720 -Height 36
$gridHint.ForeColor = [System.Drawing.Color]::FromArgb(165, 179, 193)
$rightPanel.Controls.Add($gridHint)

$captureGrid = New-Object System.Windows.Forms.DataGridView
$captureGrid.Location = New-Object System.Drawing.Point(18, 92)
$captureGrid.Size = New-Object System.Drawing.Size(744, 500)
$captureGrid.Anchor = [System.Windows.Forms.AnchorStyles]::Top -bor [System.Windows.Forms.AnchorStyles]::Bottom -bor [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right
$captureGrid.BackgroundColor = [System.Drawing.Color]::FromArgb(31, 38, 48)
$captureGrid.GridColor = [System.Drawing.Color]::FromArgb(52, 62, 76)
$captureGrid.BorderStyle = "None"
$captureGrid.EnableHeadersVisualStyles = $false
$captureGrid.ColumnHeadersDefaultCellStyle.BackColor = [System.Drawing.Color]::FromArgb(46, 56, 69)
$captureGrid.ColumnHeadersDefaultCellStyle.ForeColor = [System.Drawing.Color]::White
$captureGrid.DefaultCellStyle.BackColor = [System.Drawing.Color]::FromArgb(31, 38, 48)
$captureGrid.DefaultCellStyle.ForeColor = [System.Drawing.Color]::White
$captureGrid.DefaultCellStyle.SelectionBackColor = [System.Drawing.Color]::FromArgb(70, 96, 137)
$captureGrid.DefaultCellStyle.SelectionForeColor = [System.Drawing.Color]::White
$captureGrid.RowHeadersVisible = $false
$captureGrid.AllowUserToAddRows = $true
$captureGrid.AllowUserToDeleteRows = $true
$captureGrid.AutoSizeColumnsMode = "Fill"
$captureGrid.SelectionMode = "CellSelect"

$categoryColumn = New-Object System.Windows.Forms.DataGridViewComboBoxColumn
$categoryColumn.Name = "Category"
$categoryColumn.HeaderText = "Category"
[void]$categoryColumn.Items.AddRange($captureCategories)
$captureGrid.Columns.Add($categoryColumn) | Out-Null

foreach ($column in @(
    @{ Name = "Label"; Header = "Label" },
    @{ Name = "X"; Header = "X" },
    @{ Name = "Y"; Header = "Y" },
    @{ Name = "Plane"; Header = "Plane" },
    @{ Name = "Id"; Header = "ID" },
    @{ Name = "Actions"; Header = "Actions" },
    @{ Name = "Notes"; Header = "Notes" }
)) {
    $textColumn = New-Object System.Windows.Forms.DataGridViewTextBoxColumn
    $textColumn.Name = $column.Name
    $textColumn.HeaderText = $column.Header
    $captureGrid.Columns.Add($textColumn) | Out-Null
}

$rightPanel.Controls.Add($captureGrid)

$addRowButton = New-UiButton -Text "Add Row" -X 18 -Y 610 -Width 100 -Height 34 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$removeRowButton = New-UiButton -Text "Remove Row" -X 128 -Y 610 -Width 100 -Height 34 -Color ([System.Drawing.Color]::FromArgb(121, 67, 67))
$saveProfileButton = New-UiButton -Text "Save Route Profile JSON" -X 238 -Y 610 -Width 190 -Height 34 -Color ([System.Drawing.Color]::FromArgb(61, 126, 92))
$openProfileButton = New-UiButton -Text "Open route-profile.json" -X 438 -Y 610 -Width 180 -Height 34 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$openNotesButton = New-UiButton -Text "Open Notes / Intake" -X 628 -Y 610 -Width 134 -Height 34 -Color ([System.Drawing.Color]::FromArgb(53, 86, 128))
$addRowButton.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Bottom
$removeRowButton.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Bottom
$saveProfileButton.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Bottom
$openProfileButton.Anchor = [System.Windows.Forms.AnchorStyles]::Right -bor [System.Windows.Forms.AnchorStyles]::Bottom
$openNotesButton.Anchor = [System.Windows.Forms.AnchorStyles]::Right -bor [System.Windows.Forms.AnchorStyles]::Bottom

$rightPanel.Controls.Add($addRowButton)
$rightPanel.Controls.Add($removeRowButton)
$rightPanel.Controls.Add($saveProfileButton)
$rightPanel.Controls.Add($openProfileButton)
$rightPanel.Controls.Add($openNotesButton)

$importedExportsTitle = New-UiLabel -Text "Imported Mapper Exports" -X 18 -Y 660 -Width 250 -Height 22
$importedExportsTitle.Font = New-Object System.Drawing.Font("Segoe UI Semibold", 10.5)
$importedExportsTitle.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Bottom
$rightPanel.Controls.Add($importedExportsTitle)

$importedExportsBox = New-Object System.Windows.Forms.ListBox
$importedExportsBox.Location = New-Object System.Drawing.Point(18, 688)
$importedExportsBox.Size = New-Object System.Drawing.Size(744, 80)
$importedExportsBox.Anchor = [System.Windows.Forms.AnchorStyles]::Left -bor [System.Windows.Forms.AnchorStyles]::Right -bor [System.Windows.Forms.AnchorStyles]::Bottom
$importedExportsBox.BackColor = [System.Drawing.Color]::FromArgb(31, 38, 48)
$importedExportsBox.ForeColor = [System.Drawing.Color]::White
$importedExportsBox.BorderStyle = "FixedSingle"
$rightPanel.Controls.Add($importedExportsBox)

$script:lastRoutePaths = $null
$script:importedExports = New-Object System.Collections.Generic.List[string]

function Get-FormInputs {
    if ([string]::IsNullOrWhiteSpace($serverBox.Text) -or
        [string]::IsNullOrWhiteSpace($botFamilyBox.Text) -or
        [string]::IsNullOrWhiteSpace($routeNameBox.Text) -or
        [string]::IsNullOrWhiteSpace($activityBox.Text) -or
        [string]::IsNullOrWhiteSpace($resourceBox.Text)) {
        throw "Fill in every field before creating or saving a route profile."
    }

    $paths = Ensure-RouteScaffold `
        -ServerName $serverBox.Text.Trim() `
        -BotFamily $botFamilyBox.Text.Trim() `
        -RouteName $routeNameBox.Text.Trim() `
        -ActivityType $activityBox.Text.Trim() `
        -ResourceType $resourceBox.Text.Trim()

    return [ordered]@{
        ServerName = $serverBox.Text.Trim()
        BotFamily = $botFamilyBox.Text.Trim()
        RouteName = $routeNameBox.Text.Trim()
        ActivityType = $activityBox.Text.Trim()
        ResourceType = $resourceBox.Text.Trim()
        RouteRoot = $paths.RouteRoot
        CapturesDir = $paths.CapturesDir
        ExportsDir = $paths.ExportsDir
        ScreenshotsDir = $paths.ScreenshotsDir
        ProfilePath = $paths.ProfilePath
        NotesPath = $paths.NotesPath
        IntakePath = $paths.IntakePath
    }
}

function Update-RouteStatus {
    param(
        [string]$Message,
        [string]$RouteRoot = $null
    )

    $statusLabel.Text = $Message
    if ($RouteRoot) {
        $routeRootBox.Text = $RouteRoot
    }
}

function Save-CurrentRouteProfile {
    $inputs = Get-FormInputs
    Save-RouteProfileData `
        -ProfilePath $inputs.ProfilePath `
        -Inputs $inputs `
        -Grid $captureGrid `
        -ImportedExports $script:importedExports.ToArray()

    $script:lastRoutePaths = $inputs
    Update-RouteStatus -Message "Saved route-profile.json." -RouteRoot $inputs.RouteRoot
}

function Import-MapperExportPath {
    param([string]$Path)

    if (-not [string]::IsNullOrWhiteSpace($Path) -and (Test-Path $Path)) {
        if (-not $script:importedExports.Contains($Path)) {
            $script:importedExports.Add($Path)
            [void]$importedExportsBox.Items.Add($Path)
        }
        Update-RouteStatus -Message "Imported mapper export reference." -RouteRoot $routeRootBox.Text
    }
}

function Add-CaptureRecordToGrid {
    param([hashtable]$Record)

    foreach ($row in $captureGrid.Rows) {
        if ($row.IsNewRow) {
            continue
        }

        $existingCategory = [string]$row.Cells["Category"].Value
        $existingLabel = [string]$row.Cells["Label"].Value
        $existingX = [string]$row.Cells["X"].Value
        $existingY = [string]$row.Cells["Y"].Value
        $existingPlane = [string]$row.Cells["Plane"].Value
        $existingId = [string]$row.Cells["Id"].Value

        if ($existingCategory -eq [string]$Record.category -and
            $existingLabel -eq [string]$Record.label -and
            $existingX -eq [string]$Record.x -and
            $existingY -eq [string]$Record.y -and
            $existingPlane -eq [string]$Record.plane -and
            $existingId -eq [string]$Record.id) {
            return
        }
    }

    $index = $captureGrid.Rows.Add()
    $captureGrid.Rows[$index].Cells["Category"].Value = [string]$Record.category
    $captureGrid.Rows[$index].Cells["Label"].Value = [string]$Record.label
    $captureGrid.Rows[$index].Cells["X"].Value = [string]$Record.x
    $captureGrid.Rows[$index].Cells["Y"].Value = [string]$Record.y
    $captureGrid.Rows[$index].Cells["Plane"].Value = [string]$Record.plane
    $captureGrid.Rows[$index].Cells["Id"].Value = [string]$Record.id
    $captureGrid.Rows[$index].Cells["Actions"].Value = if ($Record.actions) { (($Record.actions | ForEach-Object { [string]$_ }) -join ", ") } else { "" }
    $captureGrid.Rows[$index].Cells["Notes"].Value = [string]$Record.notes
}

function Import-ClipboardCapture {
    param([string]$DefaultCategory = "")

    if (-not [System.Windows.Forms.Clipboard]::ContainsText()) {
        throw "Clipboard does not contain a capture line."
    }

    $raw = [System.Windows.Forms.Clipboard]::GetText().Trim()
    if ([string]::IsNullOrWhiteSpace($raw)) {
        throw "Clipboard capture line is empty."
    }

    $parts = $raw.Split(",", 7)
    if ($parts.Length -lt 6) {
        throw "Clipboard capture line is not in the expected format."
    }

    $sourceType = [string]$parts[0]
    $label = [string]$parts[1]
    $id = [string]$parts[2]
    $x = [string]$parts[3]
    $y = [string]$parts[4]
    $plane = [string]$parts[5]
    $actions = if ($parts.Length -ge 7) { [string]$parts[6] } else { "" }
    $actionArray = Convert-ActionTextToArray $actions

    $category = if ([string]::IsNullOrWhiteSpace($DefaultCategory)) {
        Resolve-CaptureCategory -SourceType $sourceType -Name $label -Actions $actionArray
    } else {
        $DefaultCategory
    }

    $record = New-CaptureRecord `
        -Category $category `
        -Label $label `
        -X $x `
        -Y $y `
        -Plane $plane `
        -Id $id `
        -Actions $actions `
        -Notes ("source:" + $sourceType)

    Add-CaptureRecordToGrid -Record $record
    Update-RouteStatus -Message "Imported clipboard capture into grid." -RouteRoot $routeRootBox.Text
}

function Import-MapperExportRows {
    param([string]$Path)

    Import-MapperExportPath -Path $Path
    $data = Get-Content -Path $Path -Raw | ConvertFrom-Json

    foreach ($anchor in @($data.anchors)) {
        $category = switch -Regex ([string]$anchor.label) {
            'teleport' { "teleportDestination"; break }
            'return' { "returnAnchor"; break }
            'bank.*door|door' { "bankDoor"; break }
            'bank.*inside|inside' { "bankInside"; break }
            'bank.*stand|stand' { "bankStand"; break }
            'bank' { "bankInside"; break }
            'resource|tree' { "resourceAnchor"; break }
            default { "safeStand" }
        }

        $record = New-CaptureRecord `
            -Category $category `
            -Label ([string]$anchor.label) `
            -X ([string]$anchor.tile.x) `
            -Y ([string]$anchor.tile.y) `
            -Plane ([string]$anchor.tile.plane) `
            -Id "" `
            -Actions "" `
            -Notes "imported-anchor"
        Add-CaptureRecordToGrid -Record $record
    }

    foreach ($waypoint in @($data.waypoints)) {
        $waypointNote = if ($waypoint.autoRecorded) { "imported-waypoint:auto" } else { "imported-waypoint" }
        $record = New-CaptureRecord `
            -Category "pathToBank" `
            -Label ([string]$waypoint.label) `
            -X ([string]$waypoint.tile.x) `
            -Y ([string]$waypoint.tile.y) `
            -Plane ([string]$waypoint.tile.plane) `
            -Id "" `
            -Actions "" `
            -Notes $waypointNote
        Add-CaptureRecordToGrid -Record $record
    }

    foreach ($object in @($data.objects)) {
        $category = Resolve-CaptureCategory -SourceType "object" -Name ([string]$object.name) -Actions @($object.actions)
        $record = New-CaptureRecord `
            -Category $category `
            -Label ([string]$object.name) `
            -X ([string]$object.tile.x) `
            -Y ([string]$object.tile.y) `
            -Plane ([string]$object.tile.plane) `
            -Id ([string]$object.id) `
            -Actions ((@($object.actions) -join ", ")) `
            -Notes "source:object"
        Add-CaptureRecordToGrid -Record $record
    }

    foreach ($npc in @($data.npcs)) {
        $category = Resolve-CaptureCategory -SourceType "npc" -Name ([string]$npc.name) -Actions @($npc.actions)
        $record = New-CaptureRecord `
            -Category $category `
            -Label ([string]$npc.name) `
            -X ([string]$npc.tile.x) `
            -Y ([string]$npc.tile.y) `
            -Plane ([string]$npc.tile.plane) `
            -Id ([string]$npc.id) `
            -Actions ((@($npc.actions) -join ", ")) `
            -Notes "source:npc"
        Add-CaptureRecordToGrid -Record $record
    }

    foreach ($item in @($data.groundItems)) {
        $category = Resolve-CaptureCategory -SourceType "groundItem" -Name ([string]$item.name) -Actions @($item.actions)
        $record = New-CaptureRecord `
            -Category $category `
            -Label ([string]$item.name) `
            -X ([string]$item.tile.x) `
            -Y ([string]$item.tile.y) `
            -Plane ([string]$item.tile.plane) `
            -Id ([string]$item.id) `
            -Actions ((@($item.actions) -join ", ")) `
            -Notes "source:groundItem"
        Add-CaptureRecordToGrid -Record $record
    }

    Update-RouteStatus -Message "Imported mapper export rows into grid." -RouteRoot $routeRootBox.Text
}

function Clear-CaptureGrid {
    if ($captureGrid.IsCurrentCellInEditMode) {
        [void]$captureGrid.EndEdit()
    }

    for ($index = $captureGrid.Rows.Count - 1; $index -ge 0; $index--) {
        if (-not $captureGrid.Rows[$index].IsNewRow) {
            $captureGrid.Rows.RemoveAt($index)
        }
    }
}

function Populate-GridFromCaptureRows {
    param([object[]]$Rows)

    Clear-CaptureGrid

    foreach ($record in @($Rows)) {
        $index = $captureGrid.Rows.Add()
        $captureGrid.Rows[$index].Cells["Category"].Value = [string]$record.category
        $captureGrid.Rows[$index].Cells["Label"].Value = [string]$record.label
        $captureGrid.Rows[$index].Cells["X"].Value = if ($null -ne $record.x) { [string]$record.x } else { "" }
        $captureGrid.Rows[$index].Cells["Y"].Value = if ($null -ne $record.y) { [string]$record.y } else { "" }
        $captureGrid.Rows[$index].Cells["Plane"].Value = if ($null -ne $record.plane) { [string]$record.plane } else { "0" }
        $captureGrid.Rows[$index].Cells["Id"].Value = if ($null -ne $record.id) { [string]$record.id } else { "" }
        $captureGrid.Rows[$index].Cells["Actions"].Value = if ($record.actions) { (($record.actions | ForEach-Object { [string]$_ }) -join ", ") } else { "" }
        $captureGrid.Rows[$index].Cells["Notes"].Value = [string]$record.notes
    }

    if ($captureGrid.Rows.Count -eq 0) {
        Add-DefaultRow -Grid $captureGrid
    }
}

function Load-RouteProfileFromPath {
    param([string]$ProfilePath)

    if (-not (Test-Path $ProfilePath)) {
        throw "Could not find route profile: $ProfilePath"
    }

    $data = Get-Content -Path $ProfilePath -Raw | ConvertFrom-Json

    $serverBox.Text = [string]$data.meta.serverName
    $botFamilyBox.Text = [string]$data.meta.botFamily
    $routeNameBox.Text = [string]$data.meta.routeName
    $activityBox.Text = [string]$data.meta.activityType
    $resourceBox.Text = [string]$data.meta.resourceType

    $script:importedExports.Clear()
    $importedExportsBox.Items.Clear()
    foreach ($path in @($data.sourceData.importedMapperExports)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$path)) {
            $script:importedExports.Add([string]$path)
            [void]$importedExportsBox.Items.Add([string]$path)
        }
    }

    Populate-GridFromCaptureRows -Rows @($data.sourceData.manualCaptureRows)

    $routeDir = Split-Path $ProfilePath -Parent
    $script:lastRoutePaths = [ordered]@{
        RouteRoot = [string]$data.paths.routeRoot
        CapturesDir = [string]$data.paths.capturesDir
        ExportsDir = [string]$data.paths.exportsDir
        ScreenshotsDir = [string]$data.paths.screenshotsDir
        ProfilePath = $ProfilePath
        NotesPath = Join-Path $routeDir "route-notes.md"
        IntakePath = Join-Path $routeDir "route-intake.md"
    }

    Update-RouteStatus -Message "Loaded existing route profile." -RouteRoot $script:lastRoutePaths.RouteRoot
}

$createButton.Add_Click({
    try {
        $inputs = Get-FormInputs
        $script:lastRoutePaths = $inputs
        Update-RouteStatus -Message "Current route scaffold refreshed." -RouteRoot $inputs.RouteRoot
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
    }
})

$addRowButton.Add_Click({
    Add-DefaultRow -Grid $captureGrid
})

$removeRowButton.Add_Click({
    foreach ($row in @($captureGrid.SelectedRows)) {
        if (-not $row.IsNewRow) {
            $captureGrid.Rows.Remove($row)
        }
    }
})

$saveProfileButton.Add_Click({
    try {
        Save-CurrentRouteProfile
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
    }
})

$openRouteButton.Add_Click({
    if ($script:lastRoutePaths) { Open-PathIfExists $script:lastRoutePaths.RouteRoot }
})

$openCapturesButton.Add_Click({
    if ($script:lastRoutePaths) { Open-PathIfExists $script:lastRoutePaths.CapturesDir }
})

$openExportsButton.Add_Click({
    if ($script:lastRoutePaths) { Open-PathIfExists $script:lastRoutePaths.ExportsDir }
})

$openScreensButton.Add_Click({
    if ($script:lastRoutePaths) { Open-PathIfExists $script:lastRoutePaths.ScreenshotsDir }
})

$openProfileButton.Add_Click({
    if ($script:lastRoutePaths) { Open-PathIfExists $script:lastRoutePaths.ProfilePath }
})

$openNotesButton.Add_Click({
    if ($script:lastRoutePaths) {
        Open-PathIfExists $script:lastRoutePaths.NotesPath
        Open-PathIfExists $script:lastRoutePaths.IntakePath
    }
})

$importLatestButton.Add_Click({
    $latest = Get-LatestMapperExport -ServerName $serverBox.Text.Trim()
    if ($null -eq $latest) {
        [System.Windows.Forms.MessageBox]::Show("No mapper exports found for that server yet.", "Route Profile Workbench")
        return
    }
    try {
        Import-MapperExportRows -Path $latest.FullName
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
    }
})

$importFileButton.Add_Click({
    $dialog = New-Object System.Windows.Forms.OpenFileDialog
    $dialog.InitialDirectory = $mappingSessionRoot
    $dialog.Filter = "JSON files (*.json)|*.json|All files (*.*)|*.*"
    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        try {
            Import-MapperExportRows -Path $dialog.FileName
        } catch {
            [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
        }
    }
})

$importClipboardButton.Add_Click({
    try {
        Import-ClipboardCapture
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
    }
})

$newRouteToolbarButton.Add_Click({
    $newItem = Show-NewRouteDialog
    if ($null -eq $newItem) {
        return
    }

    try {
        $serverBox.Text = $newItem.ServerName
        $botFamilyBox.Text = $newItem.BotFamily
        $routeNameBox.Text = $newItem.RouteName
        $activityBox.Text = $newItem.ActivityType
        $resourceBox.Text = $newItem.ResourceType

        $inputs = Get-FormInputs
        $script:lastRoutePaths = $inputs
        $script:importedExports.Clear()
        $importedExportsBox.Items.Clear()
        Clear-CaptureGrid
        Add-DefaultRow -Grid $captureGrid
        Update-RouteStatus -Message "New route scaffold created." -RouteRoot $inputs.RouteRoot
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
    }
})

$openRouteToolbarButton.Add_Click({
    $dialog = New-Object System.Windows.Forms.OpenFileDialog
    $dialog.InitialDirectory = $outputRoot
    $dialog.Filter = "Route profile JSON (route-profile.json)|route-profile.json|JSON files (*.json)|*.json|All files (*.*)|*.*"
    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        try {
            Load-RouteProfileFromPath -ProfilePath $dialog.FileName
        } catch {
            [System.Windows.Forms.MessageBox]::Show($_.Exception.Message, "Route Profile Workbench")
        }
    }
})

Add-DefaultRow -Grid $captureGrid

function Update-WorkbenchLayout {
    $margin = 24
    $topY = 96
    $bottomMargin = 24
    $panelGap = 20
    $leftWidth = 360
    $client = $form.ClientSize
    $panelHeight = [Math]::Max(640, $client.Height - $topY - $bottomMargin)
    $rightWidth = [Math]::Max(560, $client.Width - ($margin * 2) - $leftWidth - $panelGap)

    $subtitle.Width = [Math]::Max(700, $client.Width - 48)

    $leftPanel.Location = New-Object System.Drawing.Point($margin, $topY)
    $leftPanel.Size = New-Object System.Drawing.Size($leftWidth, $panelHeight)
    $rightPanel.Location = New-Object System.Drawing.Point(($margin + $leftWidth + $panelGap), $topY)
    $rightPanel.Size = New-Object System.Drawing.Size($rightWidth, $panelHeight)

    $newRouteToolbarButton.Left = $form.ClientSize.Width - 196
    $openRouteToolbarButton.Left = $form.ClientSize.Width - 94

    $statusLabel.Top = $leftPanel.ClientSize.Height - 76
    $routeRootBox.Top = $leftPanel.ClientSize.Height - 48

    $contentWidth = $rightPanel.ClientSize.Width - 36
    $gridHint.Width = $contentWidth
    $captureGrid.Width = $contentWidth

    $bottomPadding = 18
    $importsBoxHeight = 96
    $importsTitleGap = 28
    $buttonsHeight = 34
    $buttonsTop = $rightPanel.ClientSize.Height - ($importsBoxHeight + $importsTitleGap + $buttonsHeight + 34)
    $importsTitleTop = $buttonsTop + $buttonsHeight + 20
    $importsBoxTop = $importsTitleTop + 24

    $captureGrid.Height = [Math]::Max(260, $buttonsTop - $captureGrid.Top - 18)

    $addRowButton.Top = $buttonsTop
    $removeRowButton.Top = $buttonsTop
    $saveProfileButton.Top = $buttonsTop
    $openProfileButton.Top = $buttonsTop
    $openNotesButton.Top = $buttonsTop

    $openNotesButton.Left = $rightPanel.ClientSize.Width - $openNotesButton.Width - 18
    $openProfileButton.Left = $openNotesButton.Left - $openProfileButton.Width - 10

    $importedExportsTitle.Top = $importsTitleTop
    $importedExportsBox.Top = $importsBoxTop
    $importedExportsBox.Width = $contentWidth
    $importedExportsBox.Height = [Math]::Max(60, $rightPanel.ClientSize.Height - $importsBoxTop - $bottomPadding)

}

$form.Add_Shown({ Update-WorkbenchLayout })
$form.Add_Resize({ Update-WorkbenchLayout })

[void]$form.ShowDialog()
