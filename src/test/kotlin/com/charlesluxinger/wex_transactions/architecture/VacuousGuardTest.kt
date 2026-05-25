package com.charlesluxinger.wex_transactions.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Guards the architecture test suite against vacuous green outcomes.
 *
 * Enforces non-vacuous assertion policy: all layer packages must be non-empty.
 * This ensures the ClassFileImporter is correctly wired and the hexagonal architecture
 * is properly established with domain, application, and infrastructure layers.
 *
 * Governance reference: AGENTS.md - "Dependency direction and architecture constraints"
 */
class VacuousGuardTest : ArchitectureTest() {
    @Test
    fun `base package import scope is non-empty`() {
        val classes = importClasses()
        // Ensures that the ClassFileImporter is correctly wired to the compiled classes.
        // A vacuously passing importer (returning zero classes) would fail here first.
        assertThat(classes).isNotEmpty()
    }

    @Test
    fun `domain package must be non-empty`() {
        // Domain layer must exist and contain classes to enforce architecture.
        val domainClasses = importClassesFrom(DOMAIN_PACKAGE)
        assertThat(domainClasses).isNotEmpty()
    }

    @Test
    fun `application package must be non-empty`() {
        // Application layer must exist and contain classes to enforce architecture.
        val applicationClasses = importClassesFrom(APPLICATION_PACKAGE)
        assertThat(applicationClasses).isNotEmpty()
    }

    @Test
    fun `infrastructure package must be non-empty`() {
        // Infrastructure layer must exist and contain classes to enforce architecture.
        val infraClasses = importClassesFrom(INFRA_PACKAGE)
        assertThat(infraClasses).isNotEmpty()
    }
}
