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

    private val usd = TargetCurrency("United-States-Dollar")
    private val brl = TargetCurrency("Brazil-Real")

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
        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "  New TV  ",
                transactionAmount = BigDecimal("10.005"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",

            )
        val result = useCase.storePurchase(command)

        val savedPurchase = checkNotNull(persistedPurchase)
        assertEquals("New TV", savedPurchase.description)
        assertEquals(BigDecimal("10.01"), savedPurchase.transactionAmount)
        assertEquals("United-States-Dollar", savedPurchase.transactionCurrency.code)
        assertEquals(TransactionDate("2026-05-23T12:00:00Z"), savedPurchase.transactionDate)
    }

    @Test
    @DisplayName("Blank description throws and does not call ports")
    fun `blank description throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "   ",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Description over max length throws and does not call ports")
    fun `description exceeding max length throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "x".repeat(51),
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Non-positive amount throws and does not call ports")
    fun `non positive amount throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal.ZERO,
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Only United-States-Dollar source currency is accepted")
    fun `non usd source currency throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "Brazil-Real",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        val exception = assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }

        assertEquals("Only United-States-Dollar purchases are supported", exception.message)
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Invalid source currency token throws domain exception")
    fun `invalid source currency token throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        assertThrows(InvalidCurrencyException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(purchaseRepositoryPort)
    }

    @Test
    @DisplayName("Invalid transaction date format throws")
    fun `invalid transaction date throws exception`() {
        val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort)
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "23-05-2026 12:00:00",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
        verifyNoInteractions(purchaseRepositoryPort)
    }
}
