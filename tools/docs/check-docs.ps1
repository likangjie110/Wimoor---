[CmdletBinding()]
param(
    [string]$RepoRoot
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = (Resolve-Path (Join-Path (Join-Path $PSScriptRoot "..") "..")).Path
} else {
    $RepoRoot = (Resolve-Path $RepoRoot).Path
}

function Add-Error {
    param(
        [System.Collections.Generic.List[string]]$Errors,
        [string]$Message
    )
    [void]$Errors.Add($Message)
}

$errors = [System.Collections.Generic.List[string]]::new()
$codeFence = [string]([char]96) + [string]([char]96) + [string]([char]96)
$mermaidFence = $codeFence + "mermaid"

$requiredPaths = @(
    "docs/.nojekyll",
    "docs/index.html",
    "docs/zh-cn/README.md",
    "docs/zh-cn/_sidebar.md",
    "docs/zh-cn/_navbar.md",
    "docs/zh-cn/_content/skill/obsidian-workflow.md",
    "docs/zh-cn/_content/skill/github-pages.md",
    "docs/project-map/README.md"
)

foreach ($path in $requiredPaths) {
    $fullPath = Join-Path $RepoRoot $path
    if (-not (Test-Path -LiteralPath $fullPath)) {
        Add-Error $errors "Missing required path: $path"
    }
}

$indexPath = Join-Path $RepoRoot "docs/index.html"
if (Test-Path -LiteralPath $indexPath) {
    $indexContent = Get-Content -LiteralPath $indexPath -Raw -Encoding UTF8
    if ($indexContent -match "basePath:\s*'/zh-cn/'") {
        Add-Error $errors "docs/index.html still uses absolute basePath '/zh-cn/'. Use dynamic base path for GitHub Pages."
    }
    if ($indexContent -notmatch "resolveDocsBasePath") {
        Add-Error $errors "docs/index.html does not contain resolveDocsBasePath()."
    }
}

$mdRoots = @("docs/zh-cn", "docs/project-map")
$mdFiles = @()
foreach ($root in $mdRoots) {
    $fullRoot = Join-Path $RepoRoot $root
    if (Test-Path -LiteralPath $fullRoot) {
        $mdFiles += Get-ChildItem -LiteralPath $fullRoot -Recurse -File -Filter "*.md"
    }
}

foreach ($file in $mdFiles) {
    $relativeFile = $file.FullName.Substring($RepoRoot.Length).TrimStart("\", "/").Replace("\", "/")
    $lines = @(Get-Content -LiteralPath $file.FullName -Encoding UTF8)
    $fenceCount = 0
    foreach ($line in $lines) {
        if ($line.StartsWith($codeFence)) {
            $fenceCount++
        }
    }
    if (($fenceCount % 2) -ne 0) {
        Add-Error $errors "Unbalanced code fence in $relativeFile"
    }

    $inMermaid = $false
    $hasDiagramType = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if (-not $inMermaid -and $line.Trim() -eq $mermaidFence) {
            $inMermaid = $true
            $hasDiagramType = $false
            continue
        }
        if ($inMermaid -and $line.StartsWith($codeFence)) {
            if (-not $hasDiagramType) {
                Add-Error $errors "Mermaid block without diagram type in $relativeFile near line $($i + 1)"
            }
            $inMermaid = $false
            continue
        }
        if ($inMermaid -and $line -match '^\s*(flowchart|sequenceDiagram|graph|classDiagram|stateDiagram|erDiagram|gantt|pie)\b') {
            $hasDiagramType = $true
        }
    }

    $sensitivePattern = '(?i)(password|secret|access[_-]?key|refresh[_-]?token|appsecret)\s*[:=]\s*(?!<redacted>|xxx|xxxx|\s*$)[^`\s|]+'
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $sensitivePattern) {
            Add-Error $errors "Possible secret value in $relativeFile line $($i + 1): $($matches[0])"
        }
    }

    $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    $linkMatches = [regex]::Matches($content, '(!?\[[^\]]*\]\(([^)]+)\))')
    foreach ($match in $linkMatches) {
        $target = $match.Groups[2].Value.Trim()
        if ([string]::IsNullOrWhiteSpace($target)) {
            continue
        }
        if ($target -match '^(https?:|mailto:|tel:|#)') {
            continue
        }
        if ($target.StartsWith("/")) {
            continue
        }

        $target = $target.Trim("<", ">")
        $target = ($target -split "#")[0]
        $target = ($target -split "\?")[0]
        if ([string]::IsNullOrWhiteSpace($target)) {
            continue
        }

        $target = [System.Uri]::UnescapeDataString($target)
        $resolved = Join-Path $file.DirectoryName $target
        if (-not (Test-Path -LiteralPath $resolved)) {
            Add-Error $errors "Broken local link in $relativeFile -> $target"
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Host "Docs check failed:" -ForegroundColor Red
    foreach ($errorItem in $errors) {
        Write-Host " - $errorItem" -ForegroundColor Red
    }
    exit 1
}

Write-Host "Docs check passed. Markdown files checked: $($mdFiles.Count)" -ForegroundColor Green
