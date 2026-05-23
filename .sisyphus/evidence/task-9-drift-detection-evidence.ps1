#Requires -Version 7.0
<#
.SYNOPSIS
    QA Scenario: Drift Detection Test for Task 9.
    Demonstrates that the validation script correctly fails when a REQ-ID
    is missing from the traceability matrix.
#>

$specPath = ".docs/SPEC.md"
$matrixPath = ".sisyphus/evidence/task-9-traceability.md"
$backupPath = ".sisyphus/evidence/task-9-traceability.md.bak"

function Write-Step { param($Msg) Write-Host "`n=== $Msg ===" }
function Write-Result { param($PassFail, $Msg) Write-Host "$(if ($PassFail -eq 'PASS') {'[PASS]'} else {'[FAIL]'}) $Msg" }

Write-Step "QA Scenario: Drift Detection"

# Backup matrix
Copy-Item -Path $matrixPath -Destination $backupPath -Force

# Simulate drift: remove REQ-07 block from the matrix
$lines = Get-Content -Path $matrixPath
$newLines = @()
$inReq07 = $false
foreach ($line in $lines) {
    if ($line -match '^### REQ-07:') {
        $inReq07 = $true
        $newLines += "### REQ-XX — REMOVED FOR DRIFT TEST"
        continue
    }
    if ($inReq07 -and $line -match '^### ') {
        $inReq07 = $false
        $newLines += $line
        continue
    }
    if (-not $inReq07) {
        $newLines += $line
    }
}
Set-Content -Path $matrixPath -Value $newLines

Write-Host "Simulated drift: removed REQ-07 block from traceability matrix"

# Now run validation
$output = & pwsh -NoProfile -ExecutionPolicy Bypass -File ".sisyphus/evidence/task-9-validate-traceability.ps1" 2>&1 | Out-String
$exitCode = $LASTEXITCODE

Write-Host $output

# Restore matrix
Copy-Item -Path $backupPath -Destination $matrixPath -Force
Remove-Item -Path $backupPath -Force
Write-Host "Matrix restored from backup."

if ($exitCode -eq 1) {
    Write-Result "PASS" "Drift detection WORKS: validation returned exit 1 when REQ-XX was missing"
    exit 0
} else {
    Write-Result "FAIL" "Drift detection FAILED: validation returned exit $exitCode despite missing REQ-XX"
    exit 1
}
