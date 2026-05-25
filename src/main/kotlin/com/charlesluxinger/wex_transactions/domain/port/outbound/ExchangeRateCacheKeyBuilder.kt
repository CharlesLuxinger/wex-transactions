package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency

object ExchangeRateCacheKeyBuilder {
    fun buildCacheKey(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): String = "exchangeRate:${sourceCurrency.code}:${targetCurrency.code}"
}
