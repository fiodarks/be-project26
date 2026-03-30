$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Resolve-RefPath {
    param(
        [Parameter(Mandatory = $true)][string]$BaseFile,
        [Parameter(Mandatory = $true)][string]$RefFile
    )
    $baseDir = Split-Path -Parent $BaseFile
    return (Resolve-Path -LiteralPath (Join-Path $baseDir $RefFile)).Path
}

function Test-JsonPointerKey {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string]$KeyName
    )
    $pattern = "^(?<indent>\\s*)$([Regex]::Escape($KeyName))\\s*:"
    return [bool](Select-String -LiteralPath $FilePath -Pattern $pattern -SimpleMatch:$false -Quiet)
}

function Test-OpenApiRef {
    param(
        [Parameter(Mandatory = $true)][string]$BaseFile,
        [Parameter(Mandatory = $true)][string]$RefValue
    )

    $refFile = ""
    $fragment = ""
    if ($RefValue -match "^(?<file>[^#]*)(#(?<frag>.*))?$") {
        $refFile = $Matches["file"]
        $fragment = $Matches["frag"]
    }

    $targetFile = $BaseFile
    if ($refFile -ne "") {
        $targetFile = Resolve-RefPath -BaseFile $BaseFile -RefFile $refFile
        if (-not (Test-Path -LiteralPath $targetFile)) {
            return @{ ok = $false; message = "Missing ref target file: $refFile (from $BaseFile)" }
        }
    }

    if ($fragment -eq "" -or $fragment -eq "/") {
        return @{ ok = $true; message = "" }
    }

    if ($fragment -notmatch "^/") {
        if ($fragment -eq "components") {
            if (-not (Test-JsonPointerKey -FilePath $targetFile -KeyName "components")) {
                return @{ ok = $false; message = "Ref fragment '#$fragment' not found in $targetFile" }
            }
            return @{ ok = $true; message = "" }
        }
        return @{ ok = $true; message = "" } # best-effort: skip non-pointer fragments
    }

    if ($fragment -match "^/components/(schemas|securitySchemes|parameters|responses)/(?<name>[^/]+)$") {
        $name = $Matches["name"]
        if (-not (Test-JsonPointerKey -FilePath $targetFile -KeyName $name)) {
            return @{ ok = $false; message = "Ref fragment '#$fragment' missing key '$name' in $targetFile" }
        }
        return @{ ok = $true; message = "" }
    }

    return @{ ok = $true; message = "" } # unknown fragment pattern; don't fail the build
}

function Get-RefValuesFromYaml {
    param([Parameter(Mandatory = $true)][string]$FilePath)
    # Best-effort YAML $ref extraction (not a YAML parser).
    $refs = New-Object System.Collections.Generic.List[string]
    foreach ($line in Get-Content -LiteralPath $FilePath) {
        if ($line -match '^\\s*\\$ref:\\s*(?<val>.+?)\\s*$') {
            $val = $Matches["val"].Trim()
            if (($val.StartsWith("'") -and $val.EndsWith("'")) -or ($val.StartsWith('"') -and $val.EndsWith('"'))) {
                $val = $val.Substring(1, $val.Length - 2)
            }
            $refs.Add($val)
        }
    }
    return $refs
}

$entrypoints = New-Object System.Collections.Generic.List[string]
foreach ($candidate in @(
    ".\\openapi\\openapi.yaml",
    ".\\src\\main\\resources\\openapi\\openapi.yaml"
)) {
    if (Test-Path -LiteralPath $candidate) {
        $entrypoints.Add((Resolve-Path -LiteralPath $candidate).Path)
    }
}

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

foreach ($spec in $entrypoints) {
    if (-not (Test-Path -LiteralPath $spec)) {
        $warnings.Add("Missing OpenAPI entrypoint: $spec")
        continue
    }

    $refsToCheck = Get-RefValuesFromYaml -FilePath $spec

    # Also scan immediate sibling fragments referenced via $ref for nested refs.
    foreach ($ref in $refsToCheck) {
        if ($ref -match "^(?<file>[^#]+)#$") { continue }
        if ($ref -match "^(?<file>[^#]+)#") {
            $refFileRel = $Matches["file"]
            if ($refFileRel -ne "" -and (Test-Path -LiteralPath (Join-Path (Split-Path -Parent $spec) $refFileRel))) {
                $refFileAbs = Resolve-RefPath -BaseFile $spec -RefFile $refFileRel
                foreach ($nestedRef in (Get-RefValuesFromYaml -FilePath $refFileAbs)) {
                    $refsToCheck.Add($nestedRef)
                }
            }
        }
    }

    foreach ($ref in $refsToCheck | Select-Object -Unique) {
        $result = Test-OpenApiRef -BaseFile $spec -RefValue $ref
        if (-not $result.ok) {
            $errors.Add($result.message)
        }
    }
}

if ($warnings.Count -gt 0) {
    Write-Host "WARNINGS:"
    $warnings | ForEach-Object { Write-Host "  - $_" }
}

if ($errors.Count -gt 0) {
    Write-Host "ERRORS:"
    $errors | ForEach-Object { Write-Host "  - $_" }
    exit 1
}

Write-Host "OK: basic ref checks passed."
