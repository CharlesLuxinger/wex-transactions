package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
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
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
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
}
