package com.charlesluxinger.wex_transactions.architecture

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DependencyDirectionTest : ArchitectureTest() {
    @Test
    fun `domain must not depend on application`() {
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        assertThat(domainClasses).isNotEmpty
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(APPLICATION_PACKAGE)
                .because("Domain layer must be independent of Application layer in hexagonal architecture")
        rule.check(domainClasses)
    }

    @Test
    fun `domain must not depend on infrastructure`() {
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        assertThat(domainClasses).isNotEmpty
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(DOMAIN_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(INFRA_PACKAGE)
                .because("Domain layer must not depend on Infrastructure layer")
        rule.check(domainClasses)
    }

    @Test
    fun `application must not depend on infrastructure`() {
        val appClasses = importClassesFrom(APPLICATION_PACKAGE)
        assertThat(appClasses).isNotEmpty
        val rule: ArchRule =
            noClasses()
                .that()
                .resideInAnyPackage(APPLICATION_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(INFRA_PACKAGE)
                .because("Application layer must not depend on Infrastructure layer")
        rule.check(appClasses)
    }
}
