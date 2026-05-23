package com.charlesluxinger.wex_transactions.application.service.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateRepositoryPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant

class RetrieveConvertedUseCaseImplTest {
    private val purchaseRepositoryPort = mock(PurchaseRepositoryPort::class.java)
    private val exchangeRateRepositoryPort = mock(ExchangeRateRepositoryPort::class.java)
    private val useCase = RetrieveConvertedUseCaseImpl(purchaseRepositoryPort, exchangeRateRepositoryPort)

    @Test
    @DisplayName("Purchase and exchange rate found returns converted response")
    fun `purchase and exchange rate found returns converted response`() {
        val purchaseId = 1L
        val sourceCurrency = TargetCurrency("USD")
        val targetCurrency = TargetCurrency("BRL")
        val transactionDate = TransactionDate("2026-05-23T12:00:00Z")
        val createdAt = Instant.parse("2026-05-23T12:00:00Z")

        val purchase =
            Purchase(
                id = purchaseId,
                description = "Office chair",
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
                createdAt = createdAt,
            )

        val exchangeRate =
            ExchangeRate(
                rate = BigDecimal("5.00"),
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                retrievedAt = Instant.parse("2026-05-23T11:59:59Z"),
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
}
