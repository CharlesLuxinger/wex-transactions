package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model

import jakarta.validation.constraints.NotBlank

data class RetrieveConvertedQuery(
    val purchaseId: Long,
    @field:NotBlank(message = "Target currency must not be blank")
    val targetCurrency: String,
)
