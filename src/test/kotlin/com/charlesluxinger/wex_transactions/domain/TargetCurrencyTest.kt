package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TargetCurrencyTest {
    @Test
    fun `should create with valid ISO-4217 code`() {
        val currency = TargetCurrency("USD")

        assertEquals("USD", currency.code)
    }

    @Test
    fun `should validate 3-character code length`() {
        val error =
            assertFailsWith<InvalidCurrencyException> {
                TargetCurrency("US")
            }

        assertEquals("US", error.code)
    }

    @Test
    fun `should normalize to uppercase`() {
        val currency = TargetCurrency("eur")

        assertEquals("EUR", currency.code)
    }

    @Test
    fun `should trim surrounding spaces`() {
        val currency = TargetCurrency("  usd  ")

        assertEquals("USD", currency.code)
    }

    @Test
    fun `should reject codes shorter than 3 chars`() {
        assertFailsWith<InvalidCurrencyException> {
            TargetCurrency("AB")
        }
    }

    @Test
    fun `should reject codes longer than 3 chars`() {
        assertFailsWith<InvalidCurrencyException> {
            TargetCurrency("USDD")
        }
    }

    @Test
    fun `should not allow modification after creation`() {
        assertTrue(TargetCurrency::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `equals should match on code`() {
        val first = TargetCurrency("USD")
        val second = TargetCurrency("usd")

        assertEquals(first, second)
    }

    @Test
    fun `hashCode should depend on code`() {
        val first = TargetCurrency("USD")
        val second = TargetCurrency("usd")

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `USD and USD should be equal`() {
        assertEquals(TargetCurrency("USD"), TargetCurrency("USD"))
    }

    @Test
    fun `USD and EUR should not be equal`() {
        assertNotEquals(TargetCurrency("USD"), TargetCurrency("EUR"))
    }

    @Test
    fun `should accept common currencies`() {
        val accepted = listOf("USD", "EUR", "GBP", "JPY")

        accepted.forEach { code ->
            assertEquals(code, TargetCurrency(code).code)
        }
    }

    @Test
    fun `should reject non existent currency codes`() {
        val error =
            assertFailsWith<InvalidCurrencyException> {
                TargetCurrency("ZZZ")
            }

        assertEquals("ZZZ", error.code)
    }

    @Test
    fun `toString should return code`() {
        assertEquals("USD", TargetCurrency("USD").toString())
    }
}
