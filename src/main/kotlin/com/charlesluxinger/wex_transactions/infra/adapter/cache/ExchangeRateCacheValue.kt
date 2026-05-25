package com.charlesluxinger.wex_transactions.infra.adapter.cache

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import java.time.Instant

data class ExchangeRateCacheValue(
    val rate: String,
    val sourceCurrency: String,
    val targetCurrency: String,
    val retrievedAt: String,
) {
    fun toDomain(): ExchangeRate =
        ExchangeRate(
            rate = rate.toBigDecimal(),
            sourceCurrency = TargetCurrency(sourceCurrency),
            targetCurrency = TargetCurrency(targetCurrency),
            retrievedAt = Instant.parse(retrievedAt),
        )

    companion object {
        fun fromDomain(exchangeRate: ExchangeRate): ExchangeRateCacheValue =
            ExchangeRateCacheValue(
                rate = exchangeRate.rate.toPlainString(),
                sourceCurrency = exchangeRate.sourceCurrency.code,
                targetCurrency = exchangeRate.targetCurrency.code,
                retrievedAt = exchangeRate.retrievedAt.toString(),
            )
    }
}
