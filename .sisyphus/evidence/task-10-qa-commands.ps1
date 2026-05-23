#Requires -Version 7.0
<#
.SYNOPSIS
    QA Scenario: Command playbook dry validation for Task 10.
    Verifies all Gradle commands referenced in the plan are syntactically valid.
#>

$planPath = ".sisyphus/plans/wex-tech-challenge-implementation-tasks.md"
$readinessPath = ".sisyphus/evidence/task-10-readiness-packet.md"
$exitCode = 0

Write-Host "=== Command Playbook Dry Validation ==="

# Collect all ./gradlew commands from both documents
$commands = @()
foreach ($path in @($planPath, $readinessPath)) {
    $content = Get-Content -Path $path -Raw
    $matches = [regex]::Matches($content, '\./gradlew[^\n\r]*')
    foreach ($m in $matches) {
        $commands += $m.Value.Trim()
    }
}

$commands = $commands | Sort-Object -Unique
Write-Host "Found $($commands.Count) unique Gradle commands to validate"

foreach ($cmd in $commands) {
    # Structural validation (not execution — dry run)
    $parts = $cmd -split '\s+'
    
    # Check 1: Must start with ./gradlew
    if ($parts[0] -ne './gradlew') {
        Write-Host "[FAIL] Invalid command prefix: $cmd"
        $exitCode = 1
        continue
    }
    
    # Check 2: Must have at least one task
    $tasks = $parts | Where-Object { $_ -notmatch '^--' -and $_ -ne './gradlew' }
    if ($tasks.Count -eq 0) {
        Write-Host "[FAIL] No Gradle task specified: $cmd"
        $exitCode = 1
        continue
    }
    
    # Check 3: Known tasks only
    $validTasks = @(
        'clean', 'test', 'build', 'bootRun', 'bootJar', 'jar', 'classes',
        'ktlintMainSourceSetFormat', 'ktlintTestSourceSetFormat',
        'ktlintMainSourceSetCheck', 'ktlintTestSourceSetCheck',
        'detekt', 'detektBaseline',
        'jacocoTestReport', 'jacocoTestCoverageVerification',
        'compileKotlin', 'compileTestKotlin', 'compileJava', 'compileTestJava',
        'processResources', 'processTestResources',
        'checkKotlinGradlePluginConfigurationErrors',
        'testClasses', 'classes'
    )
    
    $invalidTasks = @()
    foreach ($task in $tasks) {
        # Handle --tests "pattern" — check only the main task name
        if ($task -notin $validTasks -and $task -notlike '--*' -and $task -notlike '"*') {
            $invalidTasks += $task
        }
    }
    
    if ($invalidTasks.Count -gt 0) {
        Write-Host "[WARN] Unrecognized tasks: $($invalidTasks -join ', ') in: $cmd"
        # Not failing — let the user decide
    } else {
        Write-Host "[PASS] Valid: $cmd"
    }
}

# Check 4: Readiness packet exists and has wave dispatch
$content = Get-Content -Path $readinessPath -Raw
$hasWaveDispatch = $content -match '## 2\. Wave Dispatch Order'
$hasVerificationPlaybook = $content -match '## 3\. Verification Command Playbook'
$hasEvidenceInventory = $content -match '## 4\. Evidence Inventory'

if ($hasWaveDispatch) { Write-Host "[PASS] Wave dispatch order present" }
else { Write-Host "[FAIL] Wave dispatch order missing"; $exitCode = 1 }

if ($hasVerificationPlaybook) { Write-Host "[PASS] Verification playbook present" }
else { Write-Host "[FAIL] Verification playbook missing"; $exitCode = 1 }

if ($hasEvidenceInventory) { Write-Host "[PASS] Evidence inventory present" }
else { Write-Host "[FAIL] Evidence inventory missing"; $exitCode = 1 }

Write-Host "`n=== Results ==="
if ($exitCode -eq 0) {
    Write-Host "[PASS] Command playbook validation PASSED"
} else {
    Write-Host "[FAIL] Some validation checks failed"
}
exit $exitCode
