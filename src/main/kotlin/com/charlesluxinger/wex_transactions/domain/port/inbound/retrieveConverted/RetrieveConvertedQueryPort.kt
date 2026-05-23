package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse

interface RetrieveConvertedQueryPort {
    fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse
}
