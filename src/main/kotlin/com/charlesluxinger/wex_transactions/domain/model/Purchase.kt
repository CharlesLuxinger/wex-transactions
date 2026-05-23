package com.charlesluxinger.wex_transactions.domain.model

import java.math.BigDecimal
import java.time.Instant

class Purchase(
    val id: Long,
    val transactionAmount: BigDecimal,
    val transactionCurrency: TargetCurrency,
    val transactionDate: TransactionDate,
    val targetCurrency: TargetCurrency,
    val exchangeRate: ExchangeRate,
    val convertedAmount: BigDecimal,
    val createdAt: Instant,
) {
    init {
        require(id > 0) { "Purchase id must be positive" }
        require(transactionAmount > BigDecimal.ZERO) { "Transaction amount must be positive" }
        require(convertedAmount >= BigDecimal.ZERO) { "Converted amount must be zero or positive" }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is Purchase && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}
