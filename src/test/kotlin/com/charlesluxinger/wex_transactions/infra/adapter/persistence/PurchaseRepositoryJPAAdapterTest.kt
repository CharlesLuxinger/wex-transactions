package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional

class PurchaseRepositoryJPAAdapterTest {
    private val springDataRepository = mock(PurchaseSpringDataRepository::class.java)
    private val adapter = PurchaseRepositoryJPAAdapter(springDataRepository)

    @Test
    @DisplayName("findById returns purchase when entity exists")
    fun `findById returns purchase when found`() {
        val now = Instant.now()
        val entity =
            PurchaseJpaEntity(
                id = 1L,
                description = "Test purchase",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = now,
                targetCurrency = "Brazil-Real",
                exchangeRate = BigDecimal("5.50"),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            )
        `when`(springDataRepository.findById(1L)).thenReturn(Optional.of(entity))

        val result = adapter.findById(1L)

        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(1L)
        assertThat(result?.description).isEqualTo("Test purchase")
    }

    @Test
    @DisplayName("findById returns null when entity does not exist")
    fun `findById returns null when not found`() {
        `when`(springDataRepository.findById(99L)).thenReturn(Optional.empty<PurchaseJpaEntity>())

        val result = adapter.findById(99L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("save persists and returns purchase")
    fun `save should persist and return purchase`() {
        val now = Instant.now()
        val purchase =
            Purchase(
                id = 1L,
                description = "Test purchase",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = TargetCurrency("United-States-Dollar"),
                transactionDate = TransactionDate("2026-01-10T15:30:45Z"),
                targetCurrency = TargetCurrency("Brazil-Real"),
                exchangeRate =
                    ExchangeRate(
                        rate = BigDecimal("5.50"),
                        sourceCurrency = TargetCurrency("United-States-Dollar"),
                        targetCurrency = TargetCurrency("Brazil-Real"),
                        retrievedAt = now,
                    ),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            )

        val savedEntity =
            PurchaseJpaEntity(
                id = 1L,
                description = "Test purchase",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = now,
                targetCurrency = "Brazil-Real",
                exchangeRate = BigDecimal("5.50"),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            )

        `when`(springDataRepository.save(any<PurchaseJpaEntity>())).thenReturn(savedEntity)

        val result = adapter.save(purchase)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.description).isEqualTo("Test purchase")
        assertThat(result.transactionAmount).isEqualByComparingTo(BigDecimal("100.00"))
        verify(springDataRepository).save(any<PurchaseJpaEntity>())
    }
}
