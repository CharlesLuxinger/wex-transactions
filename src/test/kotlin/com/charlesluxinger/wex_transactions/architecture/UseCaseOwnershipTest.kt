package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Architecture tests for use-case ownership enforcement.
 *
 * Governance rule: AGENTS.md Section "Governance Rules" - Rule 3
 * - Use case implementations (`*UseCaseImpl`) must reside in Application layer
 * - Use cases coordinate domain objects but don't contain core business rules
 *
 * This ensures proper layer separation and DDD boundary enforcement.
 */
class UseCaseOwnershipTest : ArchitectureTest() {
    @Test
    fun `use case implementations must be in application layer`() {
        val allClasses = importClasses()
        val useCaseImpls = allClasses.filter { it.simpleName.contains("UseCaseImpl") }
        assertThat(useCaseImpls).isNotEmpty

        val rule =
            classes()
                .that()
                .haveNameMatching(".*UseCaseImpl.*")
                .should()
                .resideInAnyPackage(APPLICATION_PACKAGE)
                .because("UseCaseImpl must be in application layer per AGENTS.md governance")
        rule.check(allClasses)
    }

    @Test
    fun `domain layer must not contain use case implementations`() {
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        assertThat(domainClasses).isNotEmpty
        val useCaseInDomain = domainClasses.filter { it.simpleName.contains("UseCaseImpl") }
        assertThat(useCaseInDomain).isEmpty()
    }

    @Test
    fun `infrastructure layer must not contain use case implementations`() {
        val infraClasses = importClassesFrom(INFRA_PACKAGE)
        assertThat(infraClasses).isNotEmpty
        val useCaseInInfra = infraClasses.filter { it.simpleName.contains("UseCaseImpl") }
        assertThat(useCaseInInfra).isEmpty()
    }
}
