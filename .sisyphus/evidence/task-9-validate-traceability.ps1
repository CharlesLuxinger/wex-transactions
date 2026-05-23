#Requires -Version 7.0
<#
.SYNOPSIS
    Validates REQ-to-implementation traceability completeness.
    Used by QA scenarios in Task 9 (Requirement-to-Task Traceability Slice).

.DESCRIPTION
    Reads SPEC.md and extracts all REQ-IDs, then checks each is mapped
    in the traceability matrix (.sisyphus/evidence/task-9-traceability.md).

    Exit 0 = 100% mapping, everything ok
    Exit 1 = missing mappings or errors
#>

$ErrorActionPrevention = "Stop"
$specPath = ".docs/SPEC.md"
$matrixPath = ".sisyphus/evidence/task-9-traceability.md"

# --- Helper ---
function Write-Result {
    param($PassFail, $Message)
    $symbol = if ($PassFail -eq "PASS") { "[PASS]" } else { "[FAIL]" }
    Write-Host "$symbol $Message"
}

# --- Step 1: Extract all REQ-IDs from SPEC.md ---
Write-Host "=== Step 1: Extract REQ-IDs from SPEC.md ==="
$specContent = Get-Content -Path $specPath -Raw
$reqIds = [regex]::Matches($specContent, 'REQ-\d{2}') | ForEach-Object { $_.Value } | Sort-Object -Unique
Write-Host "Found $($reqIds.Count) unique REQ-IDs: $($reqIds -join ', ')"

# --- Step 2: Check each REQ-ID is in the traceability matrix ---
Write-Host "`n=== Step 2: Validate REQ-IDs in traceability matrix ==="
$matrixContent = Get-Content -Path $matrixPath -Raw

$missing = @()
foreach ($reqId in $reqIds) {
    if ($matrixContent -match "### $reqId" -or $matrixContent -match "\|.*$reqId") {
        Write-Result "PASS" "$reqId found in traceability matrix"
    } else {
        Write-Result "FAIL" "$reqId MISSING from traceability matrix"
        $missing += $reqId
    }
}

# --- Step 3: Summary ---
Write-Host "`n=== Results ==="
$total = $reqIds.Count
$found = $total - $missing.Count

if ($missing.Count -eq 0) {
    Write-Result "PASS" "$found/$total REQ-IDs mapped - 100% traceability"
    exit 0
} else {
    Write-Result "FAIL" "$found/$total REQ-IDs mapped - $($missing.Count) missing: $($missing -join ', ')"
    exit 1
}
