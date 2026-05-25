package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TreasuryExchangeRateResponseTest {
    @Test
    @DisplayName("isAvailable rejects null-like exchange rate values")
    fun `isAvailable rejects null like values`() {
        val unavailableValues = listOf("NULL", " null ", "-", "N/A", "", "   ", "\t")

        unavailableValues.forEach { value ->
            val record = TreasuryRateRecord(exchangeRate = value)
            assertFalse(record.hasValidExchangeRate)
        }
    }

    @Test
    @DisplayName("isAvailable uses trim and lowercase to detect null-like values")
    fun `isAvailable uses trim and lowercase detection`() {
        assertFalse(TreasuryRateRecord(exchangeRate = " NuLl ").hasValidExchangeRate)
        assertFalse(TreasuryRateRecord(exchangeRate = " n/A ").hasValidExchangeRate)
    }

    @Test
    @DisplayName("isAvailable accepts valid exchange rate value")
    fun `isAvailable accepts valid value`() {
        assertTrue(TreasuryRateRecord(exchangeRate = "5.0").hasValidExchangeRate)
    }
}
