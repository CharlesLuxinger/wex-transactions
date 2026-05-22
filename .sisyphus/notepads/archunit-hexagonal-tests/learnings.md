# Learnings

## 2026-05-22 Session Init

### Reference Implementation (estaparking)
- Architecture tests live at: `src/test/kotlin/com/charlesluxinger/estaparking/architecture/`
- Files: `ArchitectureTest.kt` (base), `ArchitectureScaffoldTest.kt`, `DependencyDirectionTest.kt`, `ControllerBoundaryTest.kt`, `UseCaseOwnershipTest.kt`
- Base class pattern: abstract `ArchitectureTest` with companion object constants + `importClasses()` / `importClassesFrom(pkg)`
- Uses `ClassFileImporter().importPackages(BASE_PACKAGE)` (not `ClassFileImporter().withImportOption(ImportOption.DoNotIncludeTests())`)
- Empty-package guard: `if (domainClasses.isEmpty()) return` — vacuous pass allowed for scaffold state
- `allowEmptyShould(true)` used on DependencyDirectionTest rules

### Project-specific constants (wex-transactions)
- BASE_PACKAGE = `com.charlesluxinger.wex_transactions`
- DOMAIN_PACKAGE = `com.charlesluxinger.wex_transactions.domain..`
- APPLICATION_PACKAGE = `com.charlesluxinger.wex_transactions.application..`
- INFRA_PACKAGE = `com.charlesluxinger.wex_transactions.infra..`
- INFRA_CLIENT_PACKAGE = `com.charlesluxinger.wex_transactions.infra..client..`

### Build file key lines
- Test deps block: lines 41-44 (`build.gradle.kts`)
- `useJUnitPlatform()` at line 68
- ArchUnit version to use: `1.4.0` (plan says 1.4.2 but check Maven Central for latest stable)

### Gradle / style constraints
- ktlint_official style, 120-char lines, 4-space indent
- Run: `./gradlew ktlintMainSourceSetFormat ktlintTestSourceSetFormat` before committing
- `./gradlew test --tests "*Architecture*"` should work for targeted runs
