package com.charlesluxinger.wex_transactions.architecture

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Architecture test scaffold verification.
 *
 * Validates that:
 * 1. Base architecture test class can be instantiated
 * 2. Import policy correctly filters test classes
 * 3. Package constants are properly defined
 */
class ArchitectureScaffoldTest : ArchitectureTest() {
    @Test
    fun `base package constant is defined`() {
        assertThat(BASE_PACKAGE).isEqualTo("com.charlesluxinger.wex_transactions")
    }

    @Test
    fun `layer packages are properly derived`() {
        assertThat(DOMAIN_PACKAGE).isEqualTo("com.charlesluxinger.wex_transactions.domain..")
        assertThat(APPLICATION_PACKAGE).isEqualTo("com.charlesluxinger.wex_transactions.application..")
        assertThat(INFRA_PACKAGE).isEqualTo("com.charlesluxinger.wex_transactions.infra..")
        assertThat(INFRA_CLIENT_PACKAGE).isEqualTo("com.charlesluxinger.wex_transactions.infra.client..")
    }

    @Test
    fun `importClasses returns JavaClasses without throwing`() {
        val classes = importClasses()
        assertThat(classes).isNotNull
        assertThat(classes).isNotEmpty
    }

    @Test
    fun `importing base package excludes architecture test classes`() {
        val classes = importClasses()
        assertThat(classes.map { it.packageName })
            .noneMatch { packageName -> packageName.contains(".architecture") }
    }
}
