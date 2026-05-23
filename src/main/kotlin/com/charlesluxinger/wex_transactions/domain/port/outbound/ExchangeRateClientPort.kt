package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency

interface ExchangeRateClientPort {
    fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate
}
