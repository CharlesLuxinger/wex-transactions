package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import java.time.LocalDate

interface ExchangeRateCachePort {
    fun getRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate?

    fun saveRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        rate: ExchangeRate,
    )

    fun getLatestRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate?
}
