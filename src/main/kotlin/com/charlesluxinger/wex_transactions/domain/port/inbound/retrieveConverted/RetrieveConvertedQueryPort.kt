package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery

interface RetrieveConvertedQueryPort {
    fun retrieveConverted(query: RetrieveConvertedQuery): Purchase
}
