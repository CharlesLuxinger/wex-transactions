package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
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
                rateSource = ExchangeRateJpaEntity.TREASURY_SOURCE,
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
                rateSource = ExchangeRateJpaEntity.TREASURY_SOURCE,
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

    @Test
    @DisplayName("save persists treasury rate source")
    fun `save persists treasury rate source`() {
        val rateDate = LocalDate.now()
        val source = TargetCurrency("USD")
        val target = TargetCurrency("BRL")
        val domainRate =
            com.charlesluxinger.wex_transactions.domain.model.ExchangeRate(
                rate = BigDecimal("5.10"),
                sourceCurrency = source,
                targetCurrency = target,
                retrievedAt = Instant.now(),
            )

        val persisted =
            ExchangeRateJpaEntity(
                id = 10L,
                rateDate = rateDate,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.10"),
                createdAt = Instant.now(),
                rateSource = ExchangeRateJpaEntity.TREASURY_SOURCE,
            )

        `when`(exchangeRateJpaRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(persisted)

        adapter.save(domainRate, rateDate)

        val captor = ArgumentCaptor.forClass(ExchangeRateJpaEntity::class.java)
        verify(exchangeRateJpaRepository).save(captor.capture())
        assertThat(captor.value.rateSource).isEqualTo(ExchangeRateJpaEntity.TREASURY_SOURCE)
    }
}
