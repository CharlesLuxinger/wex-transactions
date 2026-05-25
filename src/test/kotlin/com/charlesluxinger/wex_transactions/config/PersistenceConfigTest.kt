package com.charlesluxinger.wex_transactions.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.core.env.Environment
import kotlin.test.assertEquals

class PersistenceConfigTest {
    @Test
    fun `should throw IllegalStateException when persistence is disabled outside test profile`() {
        val environment = mock(Environment::class.java)
        `when`(environment.activeProfiles).thenReturn(arrayOf("production"))

        val guard = PersistenceGuard(environment)

        val exception =
            assertThrows<IllegalStateException> {
                guard.validate()
            }
        assertEquals("Persistence cannot be disabled outside of the test profile.", exception.message)
    }

    @Test
    fun `should not throw exception when persistence is disabled in test profile`() {
        val environment = mock(Environment::class.java)
        `when`(environment.activeProfiles).thenReturn(arrayOf("test"))

        val guard = PersistenceGuard(environment)
        guard.validate() // should not throw
    }
}
