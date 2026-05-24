package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import java.time.LocalDate

interface ExchangeRateClientPort {
    fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate

    fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        maxWindowMonths: Long,
    ): ExchangeRate?
}
