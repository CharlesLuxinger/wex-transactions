package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class StorePurchaseUseCaseImplTest {
    private val purchaseRepositoryPort = mock(PurchaseRepositoryPort::class.java)
    private val exchangeRateClientPort = mock(ExchangeRateClientPort::class.java)
    private val useCase = StorePurchaseUseCaseImpl(purchaseRepositoryPort, exchangeRateClientPort)

    @Test
    @DisplayName("Blank description triggers require() lambda in use case validation")
    fun `blank description throws exception`() {
        val command =
            StorePurchaseCommand(
                description = "   ",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
    }

    @Test
    @DisplayName("Description exceeding max length triggers require() error lambda")
    fun `description exceeding max length throws exception`() {
        val command =
            StorePurchaseCommand(
                description = "x".repeat(51),
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
    }

    @Test
    @DisplayName("Non-positive transaction amount triggers require() lambda")
    fun `non positive amount throws exception`() {
        val command =
            StorePurchaseCommand(
                description = "Valid description",
                transactionAmount = BigDecimal.ZERO,
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { useCase.storePurchase(command) }
    }
}
