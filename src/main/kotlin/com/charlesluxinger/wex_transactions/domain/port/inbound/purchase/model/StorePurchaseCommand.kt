package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import jakarta.validation.constraints.Past
import java.math.BigDecimal

data class StorePurchaseCommand(
    val description: String,
    val transactionAmount: BigDecimal,
    val transactionCurrency: String,
    @Past(message = "Transaction date must be in the past")
    val transactionDate: String,
) {
    init {
        require(description.isNotBlank()) { "Description must not be blank" }
        require(description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must have at most $MAX_DESCRIPTION_LENGTH characters"
        }
        require(transactionAmount > BigDecimal.ZERO) { "Transaction amount must be positive" }

        require(transactionCurrency == DEFAULT_SOURCE_CURRENCY_USD) {
            "Only $DEFAULT_SOURCE_CURRENCY_USD purchases are supported"
        }
    }

    companion object {
        private const val MAX_DESCRIPTION_LENGTH = 50
        const val DEFAULT_SOURCE_CURRENCY_USD = "United-States-Dollar"
    }
}
