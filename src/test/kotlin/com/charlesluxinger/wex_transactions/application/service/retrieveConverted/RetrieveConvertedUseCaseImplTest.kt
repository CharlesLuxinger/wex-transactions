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
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class RetrieveConvertedUseCaseImplTest {
    private val purchaseRepositoryPort = mock(PurchaseRepositoryPort::class.java)
    private val exchangeRateCachePort = mock(ExchangeRateCachePort::class.java)
    private val exchangeRateClientPort = mock(ExchangeRateClientPort::class.java)

    private val useCase =
        RetrieveConvertedUseCaseImpl(
            purchaseRepositoryPort = purchaseRepositoryPort,
            exchangeRateCachePort = exchangeRateCachePort,
            exchangeRateClientPort = exchangeRateClientPort,
            ioDispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun `cache hit returns cached rate and skips treasury`() {
        val purchase = samplePurchase(1L)
        val query = RetrieveConvertedQuery(purchaseId = 1L, targetCurrency = "BRL")
        val cachedRate = sampleRate("5.10")

        `when`(purchaseRepositoryPort.findById(1L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(cachedRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(sampleRate("5.50"))

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.10"), response.exchangeRateUsed)
        assertEquals(BigDecimal("510.00"), response.convertedAmount)
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
    fun cacheStaleThenClientValidWins() {
        val purchase = samplePurchase(20L)
        val query = RetrieveConvertedQuery(purchaseId = 20L, targetCurrency = "BRL")
        val staleCacheRate = sampleRate("5.00", "2025-01-01T12:00:00Z")
        val clientRate = sampleRate("5.55", "2026-01-15T12:00:00Z")

        `when`(purchaseRepositoryPort.findById(20L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(staleCacheRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(clientRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.55"), response.exchangeRateUsed)
        verify(exchangeRateCachePort).saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), clientRate)
    }

    @Test
    fun bothBranchesFailThrowsRateUnavailable() {
        val purchase = samplePurchase(21L)
        val staleCacheRate = sampleRate("5.00", "2025-01-01T12:00:00Z")

        `when`(purchaseRepositoryPort.findById(21L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(staleCacheRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(null)

        assertThrows(RateUnavailableException::class.java) {
            useCase.retrieveConverted(RetrieveConvertedQuery(21L, "BRL"))
        }
    }

    @Test
    fun cacheWinnerDoesNotPublish() {
        val purchase = samplePurchase(22L)
        val query = RetrieveConvertedQuery(purchaseId = 22L, targetCurrency = "BRL")
        val cacheRate = sampleRate("5.22")

        `when`(purchaseRepositoryPort.findById(22L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(cacheRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.22"), response.exchangeRateUsed)
        verify(exchangeRateCachePort, never()).saveRate(
            TargetCurrency("USD"),
            TargetCurrency("BRL"),
            cacheRate,
        )
    }

    @Test
    fun purchaseNotFoundSkipsRateCalls() {
        `when`(purchaseRepositoryPort.findById(9999L)).thenReturn(null)

        assertThrows(PurchaseNotFoundException::class.java) {
            useCase.retrieveConverted(RetrieveConvertedQuery(9999L, "BRL"))
        }

        verifyNoInteractions(exchangeRateCachePort)
        verifyNoInteractions(exchangeRateClientPort)
    }

    @Test
    fun cacheOlderThan180DaysIsStale() {
        val purchase = samplePurchase(23L)
        val query = RetrieveConvertedQuery(purchaseId = 23L, targetCurrency = "BRL")
        val staleCacheRate = sampleRate("5.00", "2025-01-01T12:00:00Z")
        val clientRate = sampleRate("5.33", "2026-01-15T12:00:00Z")

        `when`(purchaseRepositoryPort.findById(23L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(staleCacheRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(clientRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.33"), response.exchangeRateUsed)
    }

    @Test
    fun cacheTimeoutClientSucceeds() {
        val purchase = samplePurchase(24L)
        val query = RetrieveConvertedQuery(purchaseId = 24L, targetCurrency = "BRL")
        val clientRate = sampleRate("5.60", "2026-01-15T12:00:00Z")

        `when`(purchaseRepositoryPort.findById(24L)).thenReturn(purchase)
        doAnswer { throw IllegalStateException("cache timeout") }
            .`when`(exchangeRateCachePort)
            .getRate(TargetCurrency("USD"), TargetCurrency("BRL"))
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(clientRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.60"), response.exchangeRateUsed)
    }

    @Test
    fun cancelLoserAfterWinnerSuccess() {
        val purchase = samplePurchase(25L)
        val query = RetrieveConvertedQuery(purchaseId = 25L, targetCurrency = "BRL")
        val cacheRate = sampleRate("5.44")

        `when`(purchaseRepositoryPort.findById(25L)).thenReturn(purchase)
        `when`(exchangeRateCachePort.getRate(TargetCurrency("USD"), TargetCurrency("BRL"))).thenReturn(cacheRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                purchase.transactionDate.value.toLocalDate(),
            ),
        ).thenReturn(sampleRate("6.00"))

        val response = useCase.retrieveConverted(query)

        assertEquals(BigDecimal("5.44"), response.exchangeRateUsed)
        verify(exchangeRateClientPort, times(1)).fetchNearestPriorRate(
            TargetCurrency("USD"),
            TargetCurrency("BRL"),
            LocalDate.parse("2026-01-16"),
        )
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

    private fun sampleRate(
        rate: String,
        retrievedAt: String = "2026-01-15T12:00:00Z",
    ): ExchangeRate =
        ExchangeRate(
            rate = BigDecimal(rate),
            sourceCurrency = TargetCurrency("USD"),
            targetCurrency = TargetCurrency("BRL"),
            retrievedAt = Instant.parse(retrievedAt),
        )
}
