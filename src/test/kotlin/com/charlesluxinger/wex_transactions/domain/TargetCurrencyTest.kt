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
    fun `should create with treasury descriptor Canada-Dollar`() {
        val currency = TargetCurrency("Canada-Dollar")

        assertEquals("Canada-Dollar", currency.value)
    }

    @Test
    fun `should create with treasury descriptor Mexico-Peso`() {
        val currency = TargetCurrency("Mexico-Peso")

        assertEquals("Mexico-Peso", currency.value)
    }

    @Test
    fun `should reject blank code`() {
        val error =
            assertFailsWith<InvalidCurrencyException> {
                TargetCurrency("")
            }

        assertEquals("", error.code)
    }

    @Test
    fun `should reject whitespace code`() {
        assertFailsWith<InvalidCurrencyException> {
            TargetCurrency("   ")
        }
    }

    @Test
    fun `should trim surrounding spaces`() {
        val currency = TargetCurrency("  Canada-Dollar  ")

        assertEquals("Canada-Dollar", currency.value)
    }

    @Test
    fun `should preserve original casing`() {
        val currency = TargetCurrency("canada-Dollar")

        assertEquals("canada-Dollar", currency.value)
    }

    @Test
    fun `should not allow modification after creation`() {
        assertTrue(TargetCurrency::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `equals should match on code`() {
        val first = TargetCurrency("Canada-Dollar")
        val second = TargetCurrency("Canada-Dollar")

        assertEquals(first, second)
    }

    @Test
    fun `hashCode should depend on code`() {
        val first = TargetCurrency("Canada-Dollar")
        val second = TargetCurrency("Canada-Dollar")

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `Canada-Dollar and Canada-Dollar should be equal`() {
        assertEquals(TargetCurrency("Canada-Dollar"), TargetCurrency("Canada-Dollar"))
    }

    @Test
    fun `Canada-Dollar and Mexico-Peso should not be equal`() {
        assertNotEquals(TargetCurrency("Canada-Dollar"), TargetCurrency("Mexico-Peso"))
    }

    @Test
    fun `should accept common treasury descriptors`() {
        val accepted = listOf("Canada-Dollar", "Mexico-Peso", "Brazil-Real", "Japan-Yen")

        accepted.forEach { code ->
            assertEquals(code, TargetCurrency(code).value)
        }
    }

    @Test
    fun `should not equal different type`() {
        assertNotEquals<Any>(TargetCurrency("Canada-Dollar"), "not-a-currency")
        assertNotEquals<Any?>(TargetCurrency("Canada-Dollar"), null)
    }

    @Test
    fun `toString should return code`() {
        assertEquals("Canada-Dollar", TargetCurrency("Canada-Dollar").toString())
    }
}
