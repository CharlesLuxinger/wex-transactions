package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import java.math.BigDecimal

data class StorePurchaseCommand(
    val description: String,
    val transactionAmount: BigDecimal,
    val transactionCurrency: String,
    val transactionDate: String,
)
