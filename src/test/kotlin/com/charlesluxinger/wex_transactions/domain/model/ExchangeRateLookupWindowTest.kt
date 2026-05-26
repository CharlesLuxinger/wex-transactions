package com.charlesluxinger.wex_transactions.domain.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExchangeRateLookupWindowTest {
    @Test
    fun `isEligible accepts rate on purchase date`() {
        val purchaseDate = LocalDate.parse("2026-01-16")

        assertTrue(ExchangeRateLookupWindow.isEligible(purchaseDate, purchaseDate))
    }

    @Test
    fun `isEligible accepts rate at six month boundary`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val boundary = purchaseDate.minusMonths(ExchangeRateLookupWindow.WINDOW_MONTHS)

        assertTrue(ExchangeRateLookupWindow.isEligible(boundary, purchaseDate))
    }

    @Test
    fun `isEligible rejects rate older than six months`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val tooOld = purchaseDate.minusMonths(ExchangeRateLookupWindow.WINDOW_MONTHS).minusDays(1)

        assertFalse(ExchangeRateLookupWindow.isEligible(tooOld, purchaseDate))
    }

    @Test
    fun `isEligible rejects rate after purchase date`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val futureRateDate = purchaseDate.plusDays(1)

        assertFalse(ExchangeRateLookupWindow.isEligible(futureRateDate, purchaseDate))
    }
}
