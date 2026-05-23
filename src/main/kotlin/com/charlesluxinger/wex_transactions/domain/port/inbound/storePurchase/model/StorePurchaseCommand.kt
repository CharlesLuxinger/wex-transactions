package com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.model

import java.math.BigDecimal

data class StorePurchaseCommand(
    val transactionAmount: BigDecimal,
    val transactionCurrency: String,
    val transactionDate: String,
    val targetCurrency: String,
)
