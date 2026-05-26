package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import java.time.LocalDate

object ExchangeRateCacheKeyBuilder {
    fun buildCacheKey(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): String = "exchangeRate:${sourceCurrency.code}:${targetCurrency.code}:$rateDate"

    fun buildPairPrefix(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): String = "exchangeRate:${sourceCurrency.code}:${targetCurrency.code}:"
}
