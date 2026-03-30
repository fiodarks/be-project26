$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

Set-Location (Split-Path -Parent $PSScriptRoot)

$output = Join-Path (Get-Location) "Project26-submission.zip"
if (Test-Path -LiteralPath $output) {
    Remove-Item -LiteralPath $output -Force
}

$excludedDirs = @(
    ".gradle",
    ".idea",
    "build",
    "out"
)

$excludedFiles = @(
    ".env"
)

$files = Get-ChildItem -Recurse -File -Force | Where-Object {
    $rel = $_.FullName.Substring((Get-Location).Path.Length).TrimStart('\', '/')
    if ($excludedFiles -contains $rel) { return $false }

    foreach ($dir in $excludedDirs) {
        if ($rel -like "$dir\\*" -or $rel -eq $dir) { return $false }
    }

    if ($rel -like "*.zip") { return $false }
    return $true
} | ForEach-Object { $_.FullName }

if ($files.Count -eq 0) {
    throw "No files found to package."
}

Compress-Archive -Path $files -DestinationPath $output -CompressionLevel Optimal
Write-Host "OK: created $output"

