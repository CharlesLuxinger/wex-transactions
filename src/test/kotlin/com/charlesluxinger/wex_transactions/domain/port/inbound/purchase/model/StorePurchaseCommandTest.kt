package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("StorePurchaseCommand")
class StorePurchaseCommandTest {
    @Test
    @DisplayName("Creates command successfully with valid data")
    fun `creates command with valid data`() {
        val command =
            StorePurchaseCommand(
                description = "New TV",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        assertEquals("New TV", command.description)
        assertEquals(BigDecimal("10.00"), command.transactionAmount)
        assertEquals("United-States-Dollar", command.transactionCurrency)
        assertEquals("2026-05-23T12:00:00Z", command.transactionDate)
    }

    @Test
    @DisplayName("Blank description throws IllegalArgumentException")
    fun `blank description throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "   ",
                    transactionAmount = BigDecimal("10.00"),
                    transactionCurrency = "United-States-Dollar",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Description must not be blank", exception.message)
    }

    @Test
    @DisplayName("Description exceeding max length throws IllegalArgumentException")
    fun `description exceeding max length throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "x".repeat(51),
                    transactionAmount = BigDecimal("10.00"),
                    transactionCurrency = "United-States-Dollar",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Description must have at most 50 characters", exception.message)
    }

    @Test
    @DisplayName("Non-positive amount throws IllegalArgumentException")
    fun `non positive amount throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "Valid description",
                    transactionAmount = BigDecimal.ZERO,
                    transactionCurrency = "United-States-Dollar",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Transaction amount must be positive", exception.message)
    }

    @Test
    @DisplayName("Negative amount throws IllegalArgumentException")
    fun `negative amount throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "Valid description",
                    transactionAmount = BigDecimal("-10.00"),
                    transactionCurrency = "United-States-Dollar",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Transaction amount must be positive", exception.message)
    }

    @Test
    @DisplayName("Only United-States-Dollar currency is accepted")
    fun `non usd source currency throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "Valid description",
                    transactionAmount = BigDecimal("10.00"),
                    transactionCurrency = "Brazil-Real",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Only United-States-Dollar purchases are supported", exception.message)
    }

    @Test
    @DisplayName("Empty currency throws IllegalArgumentException")
    fun `empty currency throws exception`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                StorePurchaseCommand(
                    description = "Valid description",
                    transactionAmount = BigDecimal("10.00"),
                    transactionCurrency = "",
                    transactionDate = "2026-05-23T12:00:00Z",
                )
            }

        assertEquals("Only United-States-Dollar purchases are supported", exception.message)
    }
}
