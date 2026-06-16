[CmdletBinding()]
param(
    [int]$Port = 3000
)

$ErrorActionPreference = "Stop"
$RepoRoot = (Resolve-Path (Join-Path (Join-Path $PSScriptRoot "..") "..")).Path

Set-Location $RepoRoot
Write-Host "Serving docs from $RepoRoot/docs on http://localhost:$Port" -ForegroundColor Cyan
& npx --yes docsify-cli serve docs -p $Port
