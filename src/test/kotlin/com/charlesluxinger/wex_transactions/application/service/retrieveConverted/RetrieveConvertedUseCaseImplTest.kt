package com.charlesluxinger.wex_transactions.application.service.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant

class RetrieveConvertedUseCaseImplTest {
    private val purchaseRepositoryPort = mock(PurchaseRepositoryPort::class.java)
    private val exchangeRateCachePort = mock(ExchangeRateCachePort::class.java)
    private val exchangeRateClientPort = mock(ExchangeRateClientPort::class.java)

    private val useCase =
        RetrieveConvertedUseCaseImpl(
            purchaseRepositoryPort = purchaseRepositoryPort,
            exchangeRateCachePort = exchangeRateCachePort,
            exchangeRateClientPort = exchangeRateClientPort,
        )

    @Test
    fun `cache hit returns cached rate and skips treasury`() {
        val purchase = samplePurchase(1L)
        val query = RetrieveConvertedQuery(purchaseId = 1L, targetCurrency = "BRL")
        val cachedRate = sampleRate("5.10")

        `when`(purchaseRepositoryPort.findById(1L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(cachedRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.10"), response.exchangeRateUsed)
        assertEquals(BigDecimal("510.00"), response.convertedAmount)
        verify(
            exchangeRateClientPort,
            never(),
        ).fetchNearestPriorRate(
            TargetCurrency("USD"),
            TargetCurrency("BRL"),
            purchase.transactionDate.value.toLocalDate(),
        )
    }

    @Test
    fun `cache miss fetches treasury and stores in cache`() {
        val purchase = samplePurchase(2L)
        val query = RetrieveConvertedQuery(purchaseId = 2L, targetCurrency = "BRL")
        val fetchedRate = sampleRate("5.25")

        `when`(purchaseRepositoryPort.findById(2L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(null)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(fetchedRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.25"), response.exchangeRateUsed)
        assertEquals(BigDecimal("525.00"), response.convertedAmount)
        verify(exchangeRateCachePort).saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), fetchedRate)
    }

    @Test
    fun `missing purchase throws not found`() {
        `when`(purchaseRepositoryPort.findById(999L)).thenReturn(null)

        assertThrows(PurchaseNotFoundException::class.java) {
            useCase.retrieveConverted(RetrieveConvertedQuery(999L, "BRL"))
        }
    }

    @Test
    fun `no rate in cache and treasury throws unavailable`() {
        val purchase = samplePurchase(3L)

        `when`(purchaseRepositoryPort.findById(3L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(null)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(null)

        assertThrows(RateUnavailableException::class.java) {
            useCase.retrieveConverted(RetrieveConvertedQuery(3L, "BRL"))
        }
    }

    private fun samplePurchase(id: Long): Purchase =
        Purchase(
            id = id,
            description = "Monitor",
            transactionAmount = BigDecimal("100.00"),
            transactionCurrency = TargetCurrency("USD"),
            transactionDate = TransactionDate("2026-01-16T10:00:00Z"),
            targetCurrency = TargetCurrency("BRL"),
            exchangeRate = sampleRate("5.00"),
            convertedAmount = BigDecimal("500.00"),
            createdAt = Instant.parse("2026-01-16T10:00:00Z"),
        )

    private fun sampleRate(rate: String): ExchangeRate =
        ExchangeRate(
            rate = BigDecimal(rate),
            sourceCurrency = TargetCurrency("USD"),
            targetCurrency = TargetCurrency("BRL"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
}
