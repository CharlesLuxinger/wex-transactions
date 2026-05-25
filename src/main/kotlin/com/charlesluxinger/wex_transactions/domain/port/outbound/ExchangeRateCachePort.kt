package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency

interface ExchangeRateCachePort {
    fun getRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate?

    fun saveRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rate: ExchangeRate,
    )
}
