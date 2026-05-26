package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.model.toMonetaryScale
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PurchaseTest {
    @Test
    fun `should create Purchase with valid data`() {
        val purchase = samplePurchase()

        assertEquals(1L, purchase.id)
        assertEquals("Fuel purchase", purchase.description)
        assertEquals(BigDecimal("100.00"), purchase.transactionAmount)
        assertEquals(TargetCurrency("United-States-Dollar"), purchase.transactionCurrency)
        assertEquals(TargetCurrency("Brazil-Real"), purchase.targetCurrency)
    }

    @Test
    fun `should use Long ID immutably`() {
        val purchase = samplePurchase(id = 99L)

        assertEquals(99L, purchase.id)
        assertTrue(Purchase::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `should accept BigDecimal amounts`() {
        val amount = BigDecimal("1234567890.12")
        val rate = ExchangeRate(BigDecimal("1.234567"), TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), Instant.now())
        val converted = amount.multiply(rate.rate).toMonetaryScale()

        val purchase = samplePurchase(transactionAmount = amount, exchangeRate = rate, convertedAmount = converted)

        assertEquals(amount, purchase.transactionAmount)
        assertEquals(converted, purchase.convertedAmount)
    }

    @Test
    fun `should preserve TransactionDate and TargetCurrency as value objects`() {
        val date = TransactionDate(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
        val transactionCurrency = TargetCurrency("Brazil-Real")
        val targetCurrency = TargetCurrency("GBP")

        val purchase =
            samplePurchase(
                transactionDate = date,
                transactionCurrency = transactionCurrency,
                targetCurrency = targetCurrency,
            )

        assertEquals(date, purchase.transactionDate)
        assertEquals(transactionCurrency, purchase.transactionCurrency)
        assertEquals(targetCurrency, purchase.targetCurrency)
    }

    @Test
    fun `should store exchangeRate and convertedAmount`() {
        val rate = ExchangeRate(BigDecimal("5.432100"), TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), Instant.now())
        val converted = BigDecimal("543.21")

        val purchase = samplePurchase(exchangeRate = rate, convertedAmount = converted)

        assertEquals(rate, purchase.exchangeRate)
        assertEquals(converted, purchase.convertedAmount)
    }

    @Test
    fun `should set createdAt audit timestamp`() {
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")

        val purchase = samplePurchase(createdAt = createdAt)

        assertEquals(createdAt, purchase.createdAt)
    }

    @Test
    fun `should not allow modification after creation`() {
        assertTrue(Purchase::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `equals() should match on id only`() {
        val first = samplePurchase(id = 7L, transactionAmount = BigDecimal("1.00"))
        val second =
            samplePurchase(id = 7L, transactionAmount = BigDecimal("999.00"), convertedAmount = BigDecimal("999.00"))

        assertEquals(first, second)
    }

    @Test
    fun `hashCode() should depend on id`() {
        val first = samplePurchase(id = 7L)
        val second = samplePurchase(id = 7L, convertedAmount = BigDecimal("999.99"))

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `two purchases with same id should be equal`() {
        val first = samplePurchase(id = 2L)
        val second = samplePurchase(id = 2L)

        assertEquals(first, second)
    }

    @Test
    fun `two purchases with different id should not be equal`() {
        val first = samplePurchase(id = 1L)
        val second = samplePurchase(id = 2L)

        assertNotEquals(first, second)
    }

    @Test
    fun `convertedAmount should be calculable from rate and original amount`() {
        val amount = BigDecimal("10.00")
        val rate = ExchangeRate(BigDecimal("5.250000"), TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), Instant.now())
        val converted = amount.multiply(rate.rate).toMonetaryScale()

        val purchase = samplePurchase(transactionAmount = amount, exchangeRate = rate, convertedAmount = converted)

        assertEquals(BigDecimal("52.50"), purchase.convertedAmount)
    }

    @Test
    fun `should reject zero amounts`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                samplePurchase(transactionAmount = BigDecimal.ZERO)
            }

        assertEquals("Transaction amount must be positive", error.message)
    }

    @Test
    fun `should handle very large amounts precision`() {
        val amount = BigDecimal("999999999999.99")
        val rate = ExchangeRate(BigDecimal("9.999999"), TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), Instant.now())
        val converted = amount.multiply(rate.rate).toMonetaryScale()

        val purchase = samplePurchase(transactionAmount = amount, exchangeRate = rate, convertedAmount = converted)

        assertEquals(converted, purchase.convertedAmount)
    }

    @Test
    fun `should not equal different type`() {
        assertNotEquals<Any>(samplePurchase(), "not-a-purchase")
        assertNotEquals<Any?>(samplePurchase(), null)
    }

    @Test
    fun `should equal itself`() {
        val purchase = samplePurchase()
        assertEquals(purchase, purchase)
    }

    @Test
    fun `should reject non positive id`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                samplePurchase(id = 0L)
            }

        assertEquals("Purchase id must be positive", error.message)
    }

    @Test
    fun `should reject negative converted amount`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                samplePurchase(convertedAmount = BigDecimal("-1.00"))
            }

        assertEquals("Converted amount must be zero or positive", error.message)
    }

    @Test
    fun `should reject blank description`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                samplePurchase(description = "   ")
            }

        assertEquals("Description must not be blank", error.message)
    }

    @Test
    fun `should reject too long description`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                samplePurchase(description = "a".repeat(51))
            }

        assertEquals("Description must have at most 50 characters", error.message)
    }

    private fun samplePurchase(
        id: Long = 1L,
        description: String = "Fuel purchase",
        transactionAmount: BigDecimal = BigDecimal("100.00"),
        transactionCurrency: TargetCurrency = TargetCurrency("United-States-Dollar"),
        transactionDate: TransactionDate = TransactionDate(LocalDateTime.of(2026, 1, 1, 10, 30, 45)),
        targetCurrency: TargetCurrency = TargetCurrency("Brazil-Real"),
        exchangeRate: ExchangeRate =
            ExchangeRate(
                rate = BigDecimal("5.000000"),
                sourceCurrency = TargetCurrency("United-States-Dollar"),
                targetCurrency = TargetCurrency("Brazil-Real"),
                retrievedAt = Instant.parse("2026-01-01T10:00:00Z"),
            ),
        convertedAmount: BigDecimal = BigDecimal("500.00"),
        createdAt: Instant = Instant.parse("2026-01-01T10:31:00Z"),
    ): Purchase =
        Purchase(
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
