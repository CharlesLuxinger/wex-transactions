package com.charlesluxinger.wex_transactions.domain.event

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class ExchangeRateFetchedEvent(
    val sourceCurrency: String,
    val targetCurrency: String,
    val rate: BigDecimal,
    val retrievedAt: Instant,
    val rateDate: LocalDate,
)
