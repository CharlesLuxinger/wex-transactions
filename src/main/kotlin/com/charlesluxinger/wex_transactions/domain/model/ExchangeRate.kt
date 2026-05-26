package com.charlesluxinger.wex_transactions.domain.model

import java.math.BigDecimal
import java.time.Instant

class ExchangeRate(
    rate: BigDecimal,
    val sourceCurrency: TargetCurrency,
    val targetCurrency: TargetCurrency,
    val retrievedAt: Instant,
) {
    val rate: BigDecimal = rate.toMonetaryScale()

    init {
        require(this.rate > BigDecimal.ZERO) { "Exchange rate must be positive" }
    }

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is ExchangeRate &&
                    rate.compareTo(other.rate) == 0 &&
                    sourceCurrency == other.sourceCurrency &&
                    targetCurrency == other.targetCurrency
            )

    override fun hashCode(): Int =
        31 * (31 * rate.stripTrailingZeros().hashCode() + sourceCurrency.hashCode()) + targetCurrency.hashCode()

    override fun toString(): String = "$sourceCurrency/$targetCurrency=$rate"

    companion object
}
