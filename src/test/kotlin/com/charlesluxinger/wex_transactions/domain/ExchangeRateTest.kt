package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import java.math.BigDecimal
import java.time.Instant
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ExchangeRateTest {
    @Test
    fun `should create ExchangeRate with valid BigDecimal rate`() {
        val exchangeRate = sampleRate(rate = BigDecimal("5.123456"))

        assertEquals(BigDecimal("5.12"), exchangeRate.rate)
    }

    @Test
    fun `should store source and target currencies`() {
        val exchangeRate = sampleRate(source = TargetCurrency("United-States-Dollar"), target = TargetCurrency("Brazil-Real"))

        assertEquals(TargetCurrency("United-States-Dollar"), exchangeRate.sourceCurrency)
        assertEquals(TargetCurrency("Brazil-Real"), exchangeRate.targetCurrency)
    }

    @Test
    fun `should set retrievedAt timestamp`() {
        val retrievedAt = Instant.parse("2026-01-01T10:00:00Z")

        val exchangeRate = sampleRate(retrievedAt = retrievedAt)

        assertEquals(retrievedAt, exchangeRate.retrievedAt)
    }

    @Test
    fun `should accept decimal 18 6 precision rates`() {
        val exchangeRate = sampleRate(rate = BigDecimal("123456789012.123456"))

        assertEquals(BigDecimal("123456789012.12"), exchangeRate.rate)
    }

    @Test
    fun `should round to 2 decimal places`() {
        val exchangeRate = sampleRate(rate = BigDecimal("5.1234567"))

        assertEquals(BigDecimal("5.12"), exchangeRate.rate)
    }

    @Test
    fun `should not allow modification after creation`() {
        assertTrue(ExchangeRate::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `equals should match on rate and currencies`() {
        val first = sampleRate(rate = BigDecimal("5.100000"))
        val second = sampleRate(rate = BigDecimal("5.1"))

        assertEquals(first, second)
    }

    @Test
    fun `hashCode should depend on rate and currencies`() {
        val first = sampleRate(rate = BigDecimal("5.100000"))
        val second = sampleRate(rate = BigDecimal("5.1"))

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `two rates with same values should be equal`() {
        val first = sampleRate()
        val second = sampleRate()

        assertEquals(first, second)
    }

    @Test
    fun `two rates with different rates should not be equal`() {
        val first = sampleRate(rate = BigDecimal("5.10"))
        val second = sampleRate(rate = BigDecimal("5.20"))

        assertNotEquals(first, second)
    }

    @Test
    fun `should reject zero rate`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                sampleRate(rate = BigDecimal.ZERO)
            }

        assertEquals("Exchange rate must be positive", error.message)
    }

    @Test
    fun `should handle very large rates`() {
        val exchangeRate = sampleRate(rate = BigDecimal("999999999999.999999"))

        assertEquals(BigDecimal("1000000000000.00"), exchangeRate.rate)
    }

    @Test
    fun `should handle same source and target currency with one rate`() {
        val exchangeRate =
            sampleRate(rate = BigDecimal("1.000000"), source = TargetCurrency("United-States-Dollar"), target = TargetCurrency("United-States-Dollar"))

        assertEquals(TargetCurrency("United-States-Dollar"), exchangeRate.sourceCurrency)
        assertEquals(TargetCurrency("United-States-Dollar"), exchangeRate.targetCurrency)
        assertEquals(BigDecimal("1.00"), exchangeRate.rate)
    }

    @Test
    fun `should not equal different type`() {
        assertNotEquals<Any>(sampleRate(), "not-a-rate")
        assertNotEquals<Any?>(sampleRate(), null)
    }

    @Test
    fun `should not equal when sourceCurrency differs`() {
        assertNotEquals(
            sampleRate(source = TargetCurrency("United-States-Dollar")),
            sampleRate(source = TargetCurrency("Brazil-Real")),
        )
    }

    @Test
    fun `should not equal when targetCurrency differs`() {
        assertNotEquals(
            sampleRate(target = TargetCurrency("Brazil-Real")),
            sampleRate(target = TargetCurrency("Mexico-Peso")),
        )
    }

    @Test
    fun `toString should include pair and rate`() {
        val exchangeRate =
            sampleRate(rate = BigDecimal("5.250000"), source = TargetCurrency("United-States-Dollar"), target = TargetCurrency("Brazil-Real"))

        assertEquals("United-States-Dollar/Brazil-Real=5.25", exchangeRate.toString())
    }

    private fun sampleRate(
        rate: BigDecimal = BigDecimal("5.250000"),
        source: TargetCurrency = TargetCurrency("United-States-Dollar"),
        target: TargetCurrency = TargetCurrency("Brazil-Real"),
        retrievedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): ExchangeRate = ExchangeRate(rate, source, target, retrievedAt)
}
