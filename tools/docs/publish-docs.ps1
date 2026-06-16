[CmdletBinding()]
param(
    [string]$Message = "docs: 完善 Obsidian 与 Pages 文档发布",
    [string]$Remote = "main",
    [string]$Branch = "main",
    [switch]$NoPush
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path (Join-Path $PSScriptRoot "..") "..")).Path
$CheckScript = Join-Path $PSScriptRoot "check-docs.ps1"

Set-Location $RepoRoot

& $CheckScript -RepoRoot $RepoRoot
if (-not $?) {
    exit 1
}

$preStaged = @(& git diff --cached --name-only)
if ($preStaged.Count -gt 0) {
    Write-Host "Staging area is not empty. Refuse to mix docs publish with existing staged changes:" -ForegroundColor Red
    $preStaged | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

$allowedPaths = @(
    ".gitignore",
    ".github/workflows/docs-verify.yml",
    ".github/workflows/pages.yml",
    "tools/docs",
    "docs/.nojekyll",
    "docs/index.html",
    "docs/zh-cn",
    "docs/project-map"
)

& git add -- $allowedPaths
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$staged = @(& git diff --cached --name-only)
if ($staged.Count -eq 0) {
    Write-Host "No docs changes to commit." -ForegroundColor Yellow
    exit 0
}

$allowedPattern = '^(?:\.gitignore|\.github/workflows/docs-verify\.yml|\.github/workflows/pages\.yml|tools/docs/|docs/\.nojekyll|docs/index\.html|docs/zh-cn/|docs/project-map/)'
$outOfScope = @($staged | Where-Object { $_ -notmatch $allowedPattern })
if ($outOfScope.Count -gt 0) {
    Write-Host "Out-of-scope files entered the staging area. Aborting before commit:" -ForegroundColor Red
    $outOfScope | ForEach-Object { Write-Host " - $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Staged docs files:" -ForegroundColor Cyan
$staged | ForEach-Object { Write-Host " - $_" }

& git commit -m $Message
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (-not $NoPush) {
    & git push $Remote "HEAD:$Branch"
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

Write-Host "Docs publish complete." -ForegroundColor Green
