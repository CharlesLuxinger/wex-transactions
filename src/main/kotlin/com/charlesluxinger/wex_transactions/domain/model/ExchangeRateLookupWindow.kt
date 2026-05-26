package com.charlesluxinger.wex_transactions.domain.model

import java.time.LocalDate

object ExchangeRateLookupWindow {
    const val WINDOW_MONTHS = 6L

    fun minEligibleDate(rateDate: LocalDate): LocalDate = rateDate.minusMonths(WINDOW_MONTHS)

    fun isEligible(
        rateRecordDate: LocalDate,
        purchaseDate: LocalDate,
    ): Boolean = !rateRecordDate.isAfter(purchaseDate) && !rateRecordDate.isBefore(minEligibleDate(purchaseDate))
}
