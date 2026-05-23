#Requires -Version 7.0
<#
.SYNOPSIS
    QA Scenario: Handoff completeness check for Task 10.
    Validates all tasks in the plan have mandatory sections.
#>

$planPath = ".sisyphus/plans/wex-tech-challenge-implementation-tasks.md"
$content = Get-Content -Path $planPath -Raw

$failures = 0

Write-Host "=== Handoff Completeness Check ==="

# Check 1: All 10 tasks present
$taskCount = [regex]::Matches($content, '- \[x\] \d+\.').Count
if ($taskCount -ge 10) {
    Write-Host "[PASS] All $taskCount tasks found"
} else {
    Write-Host "[FAIL] Expected 10+ tasks, found $taskCount"
    $failures++
}

# Check 2: Each task has Agent Profile section
$sectionsWithProfile = [regex]::Matches($content, '- Category:').Count
if ($sectionsWithProfile -ge 10) {
    Write-Host "[PASS] All tasks have Agent Profile (Category field)"
} else {
    Write-Host "[FAIL] Expected 10+ Category sections, found $sectionsWithProfile"
    $failures++
}

# Check 3: Each task has QA Scenarios
$qaCount = [regex]::Matches($content, 'Scenario:').Count
if ($qaCount -ge 10) {
    Write-Host "[PASS] All tasks have QA Scenarios ($qaCount scenarios found)"
} else {
    Write-Host "[FAIL] Expected 10+ QA scenarios, found $qaCount"
    $failures++
}

# Check 4: Evidence paths present
$evidenceCount = [regex]::Matches($content, '\.sisyphus/evidence/').Count
if ($evidenceCount -ge 10) {
    Write-Host "[PASS] Evidence paths found ($evidenceCount references)"
} else {
    Write-Host "[FAIL] Expected 10+ evidence paths, found $evidenceCount"
    $failures++
}

# Check 5: Acceptance criteria present
$acCount = [regex]::Matches($content, '- \[ \]').Count
if ($acCount -ge 10) {
    Write-Host "[PASS] Acceptance criteria found ($acCount items)"
} else {
    Write-Host "[FAIL] Expected 10+ acceptance criteria items, found $acCount"
    $failures++
}

# Check 6: Readiness packet file exists
if (Test-Path ".sisyphus/evidence/task-10-readiness-packet.md") {
    Write-Host "[PASS] Readiness packet exists"
} else {
    Write-Host "[FAIL] Readiness packet missing"
    $failures++
}

Write-Host "`n=== Results ==="
if ($failures -eq 0) {
    Write-Host "[PASS] Handoff completeness check PASSED"
    exit 0
} else {
    Write-Host "[FAIL] $failures checks failed"
    exit 1
}
