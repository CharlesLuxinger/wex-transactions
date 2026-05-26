package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class StorePurchaseCommand(
    val description: String,
    val transactionAmount: BigDecimal,
    val transactionCurrency: String,
    val transactionDate: String,
    @field:NotBlank(message = "Target currency must not be blank")
    val targetCurrency: String,
)
