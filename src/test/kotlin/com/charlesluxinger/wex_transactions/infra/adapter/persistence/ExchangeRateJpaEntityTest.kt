package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExchangeRateJpaEntityTest {
    companion object {
        private val NOW = Instant.parse("2024-01-15T10:30:00Z")
        private val LATER = Instant.parse("2024-01-15T11:30:00Z")
        private val DATE_1 = LocalDate.parse("2024-01-15")
        private val DATE_2 = LocalDate.parse("2024-01-16")

        private fun createExchangeRate(
            id: Long? = null,
            rateDate: LocalDate = DATE_1,
            sourceCurrency: String = "USD",
            targetCurrency: String = "BRL",
            exchangeRate: BigDecimal = BigDecimal("5.123456"),
            createdAt: Instant = NOW,
        ) = ExchangeRateJpaEntity(
            id = id,
            rateDate = rateDate,
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            exchangeRate = exchangeRate,
            createdAt = createdAt,
        )
    }

    @Test
    fun `constructor with id=null initializes all properties`() {
        val rate = createExchangeRate(id = null)
        assertEquals(null, rate.id)
        assertEquals(DATE_1, rate.rateDate)
        assertEquals("USD", rate.sourceCurrency)
        assertEquals("BRL", rate.targetCurrency)
        assertEquals(BigDecimal("5.123456"), rate.exchangeRate)
        assertEquals(NOW, rate.createdAt)
    }

    @Test
    fun `constructor with id=123L initializes all properties including id`() {
        val rate = createExchangeRate(id = 123L)
        assertEquals(123L, rate.id)
        assertEquals("USD", rate.sourceCurrency)
    }

    @Test
    fun `property setters work correctly`() {
        val rate = createExchangeRate(id = null)
        rate.id = 456L
        rate.sourceCurrency = "EUR"
        rate.exchangeRate = BigDecimal("6.000000")

        assertEquals(456L, rate.id)
        assertEquals("EUR", rate.sourceCurrency)
        assertEquals(BigDecimal("6.000000"), rate.exchangeRate)
    }

    @Test
    fun `equals returns true for same values`() {
        val rate1 = createExchangeRate(id = 1L)
        val rate2 = createExchangeRate(id = 1L)
        assertEquals(rate1.id, rate2.id)
    }

    @Test
    fun `equals returns false for different id`() {
        val rate1 = createExchangeRate(id = 1L)
        val rate2 = createExchangeRate(id = 2L)
        assertTrue(rate1.id != rate2.id)
    }

    @Test
    fun `equals returns false for different source currency`() {
        val rate1 = createExchangeRate(sourceCurrency = "USD")
        val rate2 = createExchangeRate(sourceCurrency = "EUR")
        assertTrue(rate1.sourceCurrency != rate2.sourceCurrency)
    }

    @Test
    fun `equals returns false for different exchange rate`() {
        val rate1 = createExchangeRate(exchangeRate = BigDecimal("5.000000"))
        val rate2 = createExchangeRate(exchangeRate = BigDecimal("6.000000"))
        assertTrue(rate1.exchangeRate != rate2.exchangeRate)
    }

    @Test
    fun `hashCode is consistent`() {
        val rate = createExchangeRate(id = 1L)
        val hash1 = rate.hashCode()
        val hash2 = rate.hashCode()
        assertEquals(hash1, hash2)
    }

    @Test
    fun `hashCode differs for different values`() {
        val rate1 = createExchangeRate(id = 1L)
        val rate2 = createExchangeRate(id = 2L)
        assertTrue(rate1.hashCode() != rate2.hashCode())
    }

    @Test
    fun `toString includes properties`() {
        val rate = createExchangeRate(id = 1L, sourceCurrency = "USD")
        val str = rate.toString()
        assertTrue(str.contains("ExchangeRateJpaEntity"))
    }

    @Test
    fun `null id with other properties`() {
        val rate =
            createExchangeRate(
                id = null,
                sourceCurrency = "JPY",
                exchangeRate = BigDecimal("0.007500"),
            )

        assertEquals(null, rate.id)
        assertEquals("JPY", rate.sourceCurrency)
        assertEquals(BigDecimal("0.007500"), rate.exchangeRate)
    }

    @Test
    fun `equals with null id`() {
        val rate1 = createExchangeRate(id = null)
        val rate2 = createExchangeRate(id = null)
        assertEquals(null, rate1.id)
        assertEquals(null, rate2.id)
    }

    @Test
    fun `different rate dates produce different property values`() {
        val rate1 = createExchangeRate(rateDate = DATE_1)
        val rate2 = createExchangeRate(rateDate = DATE_2)

        assertEquals(DATE_1, rate1.rateDate)
        assertEquals(DATE_2, rate2.rateDate)
    }

    @Test
    fun `different timestamps produce different property values`() {
        val rate1 = createExchangeRate(createdAt = NOW)
        val rate2 = createExchangeRate(createdAt = LATER)

        assertEquals(NOW, rate1.createdAt)
        assertEquals(LATER, rate2.createdAt)
    }

    @Test
    fun `all currency pairs covered`() {
        val rates =
            listOf(
                createExchangeRate(sourceCurrency = "USD", targetCurrency = "BRL"),
                createExchangeRate(sourceCurrency = "EUR", targetCurrency = "BRL"),
                createExchangeRate(sourceCurrency = "GBP", targetCurrency = "BRL"),
            )

        assertEquals(3, rates.size)
        assertEquals("USD", rates[0].sourceCurrency)
        assertEquals("EUR", rates[1].sourceCurrency)
        assertEquals("GBP", rates[2].sourceCurrency)
    }

    @Test
    fun `precision test with max scale values`() {
        val rate =
            createExchangeRate(
                exchangeRate = BigDecimal("999999999999.999999"),
            )
        assertEquals(BigDecimal("999999999999.999999"), rate.exchangeRate)
    }

    @Test
    fun `all constructor parameters are accessible`() {
        val rate =
            createExchangeRate(
                id = 999L,
                rateDate = DATE_2,
                sourceCurrency = "EUR",
                targetCurrency = "GBP",
                exchangeRate = BigDecimal("1.123456"),
                createdAt = LATER,
            )

        assertEquals(999L, rate.id)
        assertEquals(DATE_2, rate.rateDate)
        assertEquals("EUR", rate.sourceCurrency)
        assertEquals("GBP", rate.targetCurrency)
        assertEquals(BigDecimal("1.123456"), rate.exchangeRate)
        assertEquals(LATER, rate.createdAt)
    }
}
