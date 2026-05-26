package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import org.junit.jupiter.api.Assertions.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
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
            transactionCurrency: String = "United-States-Dollar",
            transactionDate: Instant = NOW,
            createdAt: Instant = NOW,
            idempotencyKey: UUID = UUID.randomUUID(),
        ) = PurchaseJpaEntity(
            id = id,
            description = description,
            transactionAmount = transactionAmount,
            transactionCurrency = transactionCurrency,
            transactionDate = transactionDate,
            createdAt = createdAt,
            idempotencyKey = idempotencyKey,
        )
    }

    @Test
    fun `constructor with id=null initializes all properties`() {
        val purchase = createPurchase(id = null)
        assertEquals(null, purchase.id)
        assertEquals("Flight to NYC", purchase.description)
        assertEquals(BigDecimal("1500.00"), purchase.transactionAmount)
        assertEquals("United-States-Dollar", purchase.transactionCurrency)
        assertEquals(NOW, purchase.transactionDate)
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
                transactionCurrency = "Brazil-Real",
                transactionDate = LATER,
                createdAt = LATER,
            )

        assertEquals(999L, purchase.id)
        assertEquals("Complex", purchase.description)
        assertEquals(BigDecimal("9999.99"), purchase.transactionAmount)
        assertEquals("Brazil-Real", purchase.transactionCurrency)
        assertEquals(LATER, purchase.transactionDate)
        assertEquals(LATER, purchase.createdAt)
    }

    @Test
    fun `toDomain with null id throws IllegalArgumentException`() {
        val entity = createPurchase(id = null)
        assertThrows(IllegalArgumentException::class.java) { entity.toDomain() }
    }

    @Test
    fun `mapping from domain and to domain preserves all attributes`() {
        val originalDomain =
            Purchase(
                id = 100L,
                description = "Subscription",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = TargetCurrency("United-States-Dollar"),
                transactionDate = TransactionDate(LocalDateTime.of(2026, 5, 23, 12, 0, 0)),
                createdAt = NOW,
            )

        val idempotencyKey = UUID.fromString("550e8400-e29b-41d4-a716-446655440001")
        val jpaEntity = PurchaseJpaEntity.fromDomainWithIdempotencyKey(originalDomain, idempotencyKey)

        assertEquals(originalDomain.description, jpaEntity.description)
        assertEquals(originalDomain.transactionAmount, jpaEntity.transactionAmount)
        assertEquals(originalDomain.transactionCurrency.value, jpaEntity.transactionCurrency)
        assertEquals(
            originalDomain.transactionDate.value
                .atOffset(ZoneOffset.UTC)
                .toInstant(),
            jpaEntity.transactionDate,
        )
        assertEquals(originalDomain.createdAt, jpaEntity.createdAt)
        assertEquals(idempotencyKey, jpaEntity.idempotencyKey)

        jpaEntity.id = 100L
        val convertedDomain = jpaEntity.toDomain()

        assertEquals(originalDomain.id, convertedDomain.id)
        assertEquals(originalDomain.description, convertedDomain.description)
        assertEquals(originalDomain.transactionAmount, convertedDomain.transactionAmount)
        assertEquals(originalDomain.transactionCurrency.value, convertedDomain.transactionCurrency.value)
        assertEquals(originalDomain.transactionDate.value, convertedDomain.transactionDate.value)
        assertEquals(originalDomain.createdAt, convertedDomain.createdAt)
    }
}
