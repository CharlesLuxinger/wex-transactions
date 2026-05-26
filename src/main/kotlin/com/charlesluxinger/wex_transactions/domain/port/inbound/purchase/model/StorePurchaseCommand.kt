package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import jakarta.validation.constraints.Past
import java.math.BigDecimal

data class StorePurchaseCommand(
    val description: String,
    val transactionAmount: BigDecimal,
    val transactionCurrency: String,
    @Past(message = "Transaction date must be in the past")
    val transactionDate: String,
)
