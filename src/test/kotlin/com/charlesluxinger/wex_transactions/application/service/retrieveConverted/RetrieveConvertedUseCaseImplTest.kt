package com.charlesluxinger.wex_transactions.application.service.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateRepositoryPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant

class RetrieveConvertedUseCaseImplTest {
    private val purchaseRepositoryPort = mock(PurchaseRepositoryPort::class.java)
    private val exchangeRateRepositoryPort = mock(ExchangeRateRepositoryPort::class.java)
    private val exchangeRateClientPort = mock(ExchangeRateClientPort::class.java)
    private val useCase =
        RetrieveConvertedUseCaseImpl(purchaseRepositoryPort, exchangeRateRepositoryPort, exchangeRateClientPort)

    private fun makePurchase(
        id: Long,
        description: String,
        amount: BigDecimal,
        rate: ExchangeRate,
        convertedAmount: BigDecimal,
        transactionDate: TransactionDate = TransactionDate("2026-04-10T12:00:00Z"),
        createdAt: Instant = Instant.parse("2026-04-10T12:00:00Z"),
        sourceCurrency: TargetCurrency = TargetCurrency("USD"),
        targetCurrency: TargetCurrency = TargetCurrency("BRL"),
    ): Purchase =
        Purchase(
            id = id,
            description = description,
            transactionAmount = amount,
            transactionCurrency = sourceCurrency,
            transactionDate = transactionDate,
            targetCurrency = targetCurrency,
            exchangeRate = rate,
            convertedAmount = convertedAmount,
            createdAt = createdAt,
        )

    private fun makeRate(
        rate: String,
        date: String,
        sourceCurrency: TargetCurrency = TargetCurrency("USD"),
        targetCurrency: TargetCurrency = TargetCurrency("BRL"),
    ): ExchangeRate =
        ExchangeRate(
            rate = BigDecimal(rate),
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            retrievedAt = Instant.parse(date),
        )

    @Test
    @DisplayName("Purchase and exchange rate found returns converted response")
    fun `purchase and exchange rate found returns converted response`() {
        val purchaseId = 1L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val createdAt = Instant.parse("2026-05-23T12:00:00Z")

        val purchaseRate = makeRate("5.000000", "2026-05-23T11:59:00Z")
        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Office chair",
                amount = BigDecimal("100.00"),
                rate = purchaseRate,
                convertedAmount = BigDecimal("500.00"),
                transactionDate = transactionDate,
                createdAt = createdAt,
            )

        val exchangeRate = makeRate("5.00", "2026-05-23T11:59:59Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = transactionDate.value.toLocalDate(),
                maxWindowMonths = 6L,
            ),
        ).thenReturn(exchangeRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(purchaseId, response.purchaseId)
        assertEquals("Office chair", response.description)
        assertEquals(transactionDate.toCanonicalString(), response.transactionDate)
        assertEquals(BigDecimal("100.00"), response.originalUsdAmount)
        assertEquals(BigDecimal("5.000000"), response.exchangeRateUsed)
        assertEquals(BigDecimal("500.00"), response.convertedAmount)
        assertEquals("BRL", response.targetCurrency)
        assertEquals(createdAt, response.createdAt)
    }

    @Test
    @DisplayName("Missing purchase throws PurchaseNotFoundException")
    fun `missing purchase throws purchase not found exception`() {
        val purchaseId = 999L
        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(null)

        assertThrows(PurchaseNotFoundException::class.java) { useCase.retrieveConverted(query) }
    }

    @Test
    @DisplayName("Missing exchange rate in window throws RateUnavailableException")
    fun `missing exchange rate in window throws rate unavailable exception`() {
        val purchaseId = 2L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")

        val purchase =
            Purchase(
                id = purchaseId,
                description = "Notebook",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = sourceCurrency,
                transactionDate = transactionDate,
                targetCurrency = targetCurrency,
                exchangeRate =
                    ExchangeRate(
                        rate = BigDecimal("5.000000"),
                        sourceCurrency = sourceCurrency,
                        targetCurrency = targetCurrency,
                        retrievedAt = Instant.parse("2026-05-23T11:59:00Z"),
                    ),
                convertedAmount = BigDecimal("500.00"),
                createdAt = Instant.parse("2026-05-23T12:00:00Z"),
            )

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = transactionDate.value.toLocalDate(),
                maxWindowMonths = 6L,
            ),
        ).thenReturn(null)

        assertThrows(RateUnavailableException::class.java) { useCase.retrieveConverted(query) }
    }

    @Test
    @DisplayName("Persisted miss with Treasury fallback calls client and persists rate")
    fun `persisted miss with treasury fallback calls client and persists rate`() {
        val purchaseId = 3L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-04-10T12:00:00Z")
        val rateDate = transactionDate.value.toLocalDate()

        val purchaseRate = makeRate("5.000000", "2026-04-10T11:59:00Z")
        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Monitor",
                amount = BigDecimal("200.00"),
                rate = purchaseRate,
                convertedAmount = BigDecimal("1000.00"),
            )

        val treasuryRate = makeRate("5.70", "2026-04-09T00:00:00Z")
        val savedRate = makeRate("5.70", "2026-04-09T00:00:00Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(null)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(treasuryRate)
        `when`(
            exchangeRateRepositoryPort.save(treasuryRate, rateDate),
        ).thenReturn(savedRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(purchaseId, response.purchaseId)
        assertEquals("Monitor", response.description)
        assertEquals(BigDecimal("200.00"), response.originalUsdAmount)
        assertEquals(BigDecimal("5.700000"), response.exchangeRateUsed)
        assertEquals(BigDecimal("1140.00"), response.convertedAmount)

        verify(exchangeRateClientPort).fetchNearestPriorRate(
            sourceCurrency,
            targetCurrency,
            rateDate,
            6L,
        )
        verify(exchangeRateRepositoryPort).save(treasuryRate, rateDate)
    }

    @Test
    @DisplayName("Rate at exact 6-month boundary is accepted when repository returns it")
    fun `rate at boundary date is accepted`() {
        val purchaseId = 5L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val rateDate = transactionDate.value.toLocalDate()
        val boundaryDate = rateDate.minusMonths(6L)

        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Tablet",
                amount = BigDecimal("150.00"),
                rate = makeRate("6.000000", "2026-05-23T11:59:00Z"),
                convertedAmount = BigDecimal("900.00"),
                transactionDate = transactionDate,
            )

        val boundaryRate = makeRate("6.00", "${boundaryDate}T00:00:00Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(boundaryRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(purchaseId, response.purchaseId)
        assertEquals(BigDecimal("150.00"), response.originalUsdAmount)
        assertEquals(BigDecimal("6.000000"), response.exchangeRateUsed)
        assertEquals(BigDecimal("900.00"), response.convertedAmount)
        verifyNoInteractions(exchangeRateClientPort)
    }

    @Test
    @DisplayName("Rate outside window causes miss with Treasury fallback and persist")
    fun `rate outside window triggers fallback`() {
        val purchaseId = 6L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val rateDate = transactionDate.value.toLocalDate()

        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Keyboard",
                amount = BigDecimal("80.00"),
                rate = makeRate("5.000000", "2026-05-23T11:59:00Z"),
                convertedAmount = BigDecimal("400.00"),
                transactionDate = transactionDate,
            )

        val fallbackRate = makeRate("5.50", "2026-05-20T00:00:00Z")
        val savedRate = makeRate("5.50", "2026-05-20T00:00:00Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(null)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(fallbackRate)
        `when`(
            exchangeRateRepositoryPort.save(fallbackRate, rateDate),
        ).thenReturn(savedRate)

        val response = useCase.retrieveConverted(query)

        assertEquals(purchaseId, response.purchaseId)
        assertEquals(BigDecimal("80.00"), response.originalUsdAmount)
        assertEquals(BigDecimal("5.500000"), response.exchangeRateUsed)
        assertEquals(BigDecimal("440.00"), response.convertedAmount)

        verify(exchangeRateClientPort).fetchNearestPriorRate(
            sourceCurrency,
            targetCurrency,
            rateDate,
            6L,
        )
        verify(exchangeRateRepositoryPort).save(fallbackRate, rateDate)
    }

    @Test
    @DisplayName("Cache reuse: first call miss+write, second call uses persisted rate")
    fun `cache reuse uses persisted rate on second call`() {
        val purchaseId = 7L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val rateDate = transactionDate.value.toLocalDate()

        val purchaseRate = makeRate("5.000000", "2026-05-23T11:59:00Z")
        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Mouse",
                amount = BigDecimal("50.00"),
                rate = purchaseRate,
                convertedAmount = BigDecimal("250.00"),
                transactionDate = transactionDate,
            )

        val treasuryRate = makeRate("5.75", "2026-05-20T00:00:00Z")
        val persistedRate = makeRate("5.75", "2026-05-20T00:00:00Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(null).thenReturn(persistedRate)
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(treasuryRate)

        // First call: repository miss -> Treasury fallback -> persist
        useCase.retrieveConverted(query)

        // Second call: repository hit (persisted rate available)
        useCase.retrieveConverted(query)

        verify(exchangeRateRepositoryPort, times(2)).findNearestPriorRate(
            sourceCurrency,
            targetCurrency,
            rateDate,
            6L,
        )
        verify(exchangeRateClientPort, times(1)).fetchNearestPriorRate(
            sourceCurrency,
            targetCurrency,
            rateDate,
            6L,
        )
        verify(exchangeRateRepositoryPort, times(1)).save(treasuryRate, rateDate)
    }

    @Test
    @DisplayName("Persisted rate found does not call Treasury client")
    fun `persisted rate found does not call treasury client`() {
        val purchaseId = 4L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val rateDate = transactionDate.value.toLocalDate()

        val purchaseRate = makeRate("5.000000", "2026-05-23T11:59:00Z")
        val purchase =
            makePurchase(
                id = purchaseId,
                description = "Desk",
                amount = BigDecimal("300.00"),
                rate = purchaseRate,
                convertedAmount = BigDecimal("1500.00"),
                transactionDate = transactionDate,
                createdAt = Instant.parse("2026-05-23T12:00:00Z"),
            )

        val persistedRate = makeRate("5.25", "2026-05-22T00:00:00Z")

        val query = RetrieveConvertedQuery(purchaseId = purchaseId, targetCurrency = "BRL")

        `when`(purchaseRepositoryPort.findById(purchaseId)).thenReturn(purchase)
        `when`(
            exchangeRateRepositoryPort.findNearestPriorRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6L,
            ),
        ).thenReturn(persistedRate)

        useCase.retrieveConverted(query)

        verifyNoInteractions(exchangeRateClientPort)
    }
}
