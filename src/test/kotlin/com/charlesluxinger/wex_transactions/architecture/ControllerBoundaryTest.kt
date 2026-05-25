package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Architecture tests for controller boundary enforcement.
 *
 * Governance rule: AGENTS.md Section "Governance Rules" - Rule 2
 * - Controllers (`infra/client..`) must invoke inbound ports or use cases only
 * - Controllers must NOT import repositories or infrastructure adapters directly
 *
 * This ensures web layer remains decoupled from persistence concerns.
 */
class ControllerBoundaryTest : ArchitectureTest() {
    @Test
    fun `controllers must not depend on repositories`() {
        val classes = importClasses()
        val infraClientExists =
            classes.any {
                it.name.startsWith(
                    "${BASE_PACKAGE}.infra",
                ) &&
                    it.name.contains(".client")
            }
        assertThat(infraClientExists).isTrue()

        val rule =
            noClasses()
                .that()
                .resideInAnyPackage(INFRA_CLIENT_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..repository..", "..Repository..")
                .because("Controllers must not access repositories directly - use inbound ports instead")
        rule.check(classes)
    }

    @Test
    fun `controllers must not depend on adapters`() {
        val classes = importClasses()
        val infraClientExists =
            classes.any {
                it.name.startsWith(
                    "${BASE_PACKAGE}.infra",
                ) &&
                    it.name.contains(".client")
            }
        assertThat(infraClientExists).isTrue()

        val rule =
            noClasses()
                .that()
                .resideInAnyPackage(INFRA_CLIENT_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..adapter..", "..Adapter..")
                .because("Controllers must not depend on adapters directly - use ports instead")
        rule.check(classes)
    }
}
