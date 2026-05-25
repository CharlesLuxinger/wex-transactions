package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class StorePurchaseUseCaseImplTest {
    @Mock
    private lateinit var purchaseRepositoryPort: PurchaseRepositoryPort

    @Mock
    private lateinit var exchangeRateClientPort: ExchangeRateClientPort

    private val usd = TargetCurrency("USD")
    private val brl = TargetCurrency("BRL")

    @Test
    @DisplayName("Stores purchase successfully with rounded amount and converted amount")
    fun `stores purchase successfully with rounded values`() {
        var persistedPurchase: Purchase? = null
        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun save(purchase: Purchase): Purchase {
                    persistedPurchase = purchase
                    return purchase
                }

                override fun findById(id: Long): Purchase? = null
            }
        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "  New TV  ",
                transactionAmount = BigDecimal("10.005"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )
        val rate = sampleRate("5.678")

        `when`(exchangeRateClientPort.fetchRate(usd, brl)).thenReturn(rate)

        val result = useCase.storePurchase(command)

        verify(exchangeRateClientPort, times(1)).fetchRate(usd, brl)
        val savedPurchase = checkNotNull(persistedPurchase)
        assertEquals("New TV", savedPurchase.description)
        assertEquals(BigDecimal("10.01"), savedPurchase.transactionAmount)
        assertEquals(BigDecimal("5.68"), savedPurchase.exchangeRate.rate)
        assertEquals(BigDecimal("56.86"), savedPurchase.convertedAmount)
        assertEquals("USD", savedPurchase.transactionCurrency.code)
        assertEquals("BRL", savedPurchase.targetCurrency.code)
        assertEquals(TransactionDate("2026-05-23T12:00:00Z"), savedPurchase.transactionDate)
        assertEquals(savedPurchase.convertedAmount, result.convertedAmount)
    }

    @Test
    @DisplayName("Blank description throws and does not call ports")
    fun `blank description throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "   ",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Description over max length throws and does not call ports")
    fun `description exceeding max length throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "x".repeat(51),
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Non-positive amount throws and does not call ports")
    fun `non positive amount throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal.ZERO,
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Only USD source currency is accepted")
    fun `non usd source currency throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "EUR",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        val exception = assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }

        assertEquals("Only USD purchases are supported", exception.message)
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Invalid source currency token throws domain exception")
    fun `invalid source currency token throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "US",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(InvalidCurrencyException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Invalid target currency token throws domain exception")
    fun `invalid target currency token throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "B1L",
            )

        assertThrows(InvalidCurrencyException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Invalid transaction date format throws")
    fun `invalid transaction date throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "23-05-2026 12:00:00",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(exchangeRateClientPort)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Rate client failure does not persist purchase")
    fun `rate client failure does not save purchase`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        `when`(exchangeRateClientPort.fetchRate(usd, brl)).thenThrow(RuntimeException("Treasury down"))

        assertThrows(RuntimeException::class.java) { useCase.storePurchase(command) }
        verify(exchangeRateClientPort, times(1)).fetchRate(usd, brl)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    private fun sampleRate(rate: String): ExchangeRate =
        ExchangeRate(
            rate = BigDecimal(rate),
            sourceCurrency = TargetCurrency("USD"),
            targetCurrency = TargetCurrency("BRL"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
}
