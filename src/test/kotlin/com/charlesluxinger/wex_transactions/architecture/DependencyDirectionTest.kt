package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * Architecture tests for strict dependency direction.
 *
 * Governance rule: AGENTS.md Section "Governance Rules" - Rule 1
 * - `domain..` must not depend on `application..` or `infra..`
 * - `application..` must not depend on `infra..`
 *
 * This enforces the hexagonal architecture principle: dependencies point inward.
 * Domain is the core, application orchestrates, infra implements adapters.
 */
class DependencyDirectionTest : ArchitectureTest() {
    @Test
    fun `domain must not depend on application`() {
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        if (domainClasses.isEmpty()) return
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(APPLICATION_PACKAGE)
                .because("Domain layer must be independent of Application layer in hexagonal architecture")
        rule.allowEmptyShould(true).check(domainClasses)
    }

    @Test
    fun `domain must not depend on infrastructure`() {
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        if (domainClasses.isEmpty()) return
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(INFRA_PACKAGE)
                .because("Domain layer must not depend on Infrastructure layer")
        rule.allowEmptyShould(true).check(domainClasses)
    }

    @Test
    fun `application must not depend on infrastructure`() {
        val appClasses = importClassesFrom(APPLICATION_PACKAGE)
        if (appClasses.isEmpty()) return
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(APPLICATION_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(INFRA_PACKAGE)
                .because("Application layer must not depend on Infrastructure layer")
        rule.allowEmptyShould(true).check(appClasses)
    }
}
