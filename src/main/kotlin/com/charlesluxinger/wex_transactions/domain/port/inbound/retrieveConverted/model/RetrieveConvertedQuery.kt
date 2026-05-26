package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model

import jakarta.validation.constraints.NotBlank

data class RetrieveConvertedQuery(
    val purchaseId: Long,
    // Internal-only contract: always normalized Treasury descriptor (e.g., "Brazil-Real").
    // API boundary is responsible for ISO-4217 -> descriptor normalization.
    @field:NotBlank(message = "Target currency must not be blank")
    val targetCurrency: String,
)
