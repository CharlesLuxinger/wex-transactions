package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model

data class RetrieveConvertedQuery(
    val purchaseId: Long,
    val targetCurrency: String,
)
