package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseRequest
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal

class PurchaseControllerV1ValidationTest {
    private val mockPort = mock(StorePurchaseCommandPort::class.java)
    private val controller = PurchaseControllerV1(mockPort)

    @Test
    @DisplayName("Blank description triggers validate() require() lambda")
    fun `blank description triggers validation lambda`() {
        val request =
            StorePurchaseRequest(
                description = "   ",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { controller.storePurchase(request) }
    }

    @Test
    @DisplayName("Description exceeding 50 chars triggers validate() require() lambda for length")
    fun `description too long triggers validation lambda`() {
        val request =
            StorePurchaseRequest(
                description = "x".repeat(51),
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { controller.storePurchase(request) }
    }

    @Test
    @DisplayName("Non-positive transaction amount triggers validate() require() lambda")
    fun `non positive amount triggers validation lambda`() {
        val request =
            StorePurchaseRequest(
                description = "Valid description",
                transactionAmount = BigDecimal.ZERO,
                transactionCurrency = "USD",
                transactionDate = "2026-05-23T12:00:00Z",
                targetCurrency = "BRL",
            )

        assertThrows(IllegalArgumentException::class.java) { controller.storePurchase(request) }
    }
}
