package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ExchangeRateRepositoryAdapterTest {
    private val exchangeRateJpaRepository = mock(ExchangeRateJpaRepository::class.java)
    private val adapter = ExchangeRateRepositoryAdapter(exchangeRateJpaRepository)

    @Test
    @DisplayName("findNearestPriorRate returns rate when entity exists")
    fun `findNearestPriorRate returns exchange rate when found`() {
        val rateDate = LocalDate.now()
        val now = Instant.now()
        val entity =
            ExchangeRateJpaEntity(
                id = 1L,
                rateDate = rateDate,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.25"),
                createdAt = now,
            )
        `when`(
            exchangeRateJpaRepository.findNearestPriorRate(
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                rateDate = rateDate,
                minDate = rateDate.minusMonths(6),
            ),
        ).thenReturn(listOf(entity))

        val result =
            adapter.findNearestPriorRate(
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                rateDate = rateDate,
                maxWindowMonths = 6,
            )

        assertThat(result).isNotNull
        assertThat(result?.rate).isEqualTo(BigDecimal("5.25"))
        assertThat(result?.sourceCurrency).isEqualTo(TargetCurrency("USD"))
        assertThat(result?.targetCurrency).isEqualTo(TargetCurrency("BRL"))
    }

    @Test
    @DisplayName("findNearestPriorRate returns null when no entities match")
    fun `findNearestPriorRate returns null when not found`() {
        val rateDate = LocalDate.now()
        `when`(
            exchangeRateJpaRepository.findNearestPriorRate(
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                rateDate = rateDate,
                minDate = rateDate.minusMonths(6),
            ),
        ).thenReturn(emptyList())

        val result =
            adapter.findNearestPriorRate(
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                rateDate = rateDate,
                maxWindowMonths = 6,
            )

        assertThat(result).isNull()
    }
}
