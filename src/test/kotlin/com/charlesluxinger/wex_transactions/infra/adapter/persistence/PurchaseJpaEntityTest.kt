package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import java.math.BigDecimal
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PurchaseJpaEntityTest {
    companion object {
        private val NOW = Instant.parse("2024-01-15T10:30:00Z")
        private val LATER = Instant.parse("2024-01-15T11:30:00Z")

        private fun createPurchase(
            id: Long? = null,
            description: String = "Flight to NYC",
            transactionAmount: BigDecimal = BigDecimal("1500.00"),
            transactionCurrency: String = "USD",
            transactionDate: Instant = NOW,
            targetCurrency: String = "BRL",
            exchangeRate: BigDecimal = BigDecimal("5.123456"),
            convertedAmount: BigDecimal = BigDecimal("7684.90"),
            createdAt: Instant = NOW,
        ) = PurchaseJpaEntity(
            id = id,
            description = description,
            transactionAmount = transactionAmount,
            transactionCurrency = transactionCurrency,
            transactionDate = transactionDate,
            targetCurrency = targetCurrency,
            exchangeRate = exchangeRate,
            convertedAmount = convertedAmount,
            createdAt = createdAt,
        )
    }

    @Test
    fun `constructor with id=null initializes all properties`() {
        val purchase = createPurchase(id = null)
        assertEquals(null, purchase.id)
        assertEquals("Flight to NYC", purchase.description)
        assertEquals(BigDecimal("1500.00"), purchase.transactionAmount)
        assertEquals("USD", purchase.transactionCurrency)
        assertEquals(NOW, purchase.transactionDate)
        assertEquals("BRL", purchase.targetCurrency)
        assertEquals(BigDecimal("5.123456"), purchase.exchangeRate)
        assertEquals(BigDecimal("7684.90"), purchase.convertedAmount)
        assertEquals(NOW, purchase.createdAt)
    }

    @Test
    fun `constructor with id=123L initializes all properties including id`() {
        val purchase = createPurchase(id = 123L)
        assertEquals(123L, purchase.id)
        assertEquals("Flight to NYC", purchase.description)
    }

    @Test
    fun `property setters work correctly`() {
        val purchase = createPurchase(id = null)
        purchase.id = 456L
        purchase.description = "Hotel in Paris"
        purchase.transactionAmount = BigDecimal("2000.00")

        assertEquals(456L, purchase.id)
        assertEquals("Hotel in Paris", purchase.description)
        assertEquals(BigDecimal("2000.00"), purchase.transactionAmount)
    }

    @Test
    fun `toString includes properties`() {
        val purchase = createPurchase(id = 1L)
        val str = purchase.toString()
        assertTrue(str.contains("PurchaseJpaEntity"))
    }

    @Test
    fun `null id with other properties`() {
        val purchase =
            createPurchase(
                id = null,
                description = "Pending",
                transactionAmount = BigDecimal("999.99"),
            )

        assertEquals(null, purchase.id)
        assertEquals("Pending", purchase.description)
        assertEquals(BigDecimal("999.99"), purchase.transactionAmount)
    }

    @Test
    fun `different timestamps produce different property values`() {
        val purchase1 = createPurchase(createdAt = NOW)
        val purchase2 = createPurchase(createdAt = LATER)

        assertEquals(NOW, purchase1.createdAt)
        assertEquals(LATER, purchase2.createdAt)
    }

    @Test
    fun `all constructor parameters are accessible`() {
        val purchase =
            createPurchase(
                id = 999L,
                description = "Complex",
                transactionAmount = BigDecimal("9999.99"),
                transactionCurrency = "EUR",
                transactionDate = LATER,
                targetCurrency = "GBP",
                exchangeRate = BigDecimal("1.123456"),
                convertedAmount = BigDecimal("11111.11"),
                createdAt = LATER,
            )

        assertEquals(999L, purchase.id)
        assertEquals("Complex", purchase.description)
        assertEquals(BigDecimal("9999.99"), purchase.transactionAmount)
        assertEquals("EUR", purchase.transactionCurrency)
        assertEquals(LATER, purchase.transactionDate)
        assertEquals("GBP", purchase.targetCurrency)
        assertEquals(BigDecimal("1.123456"), purchase.exchangeRate)
        assertEquals(BigDecimal("11111.11"), purchase.convertedAmount)
        assertEquals(LATER, purchase.createdAt)
    }
}
