package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent

fun interface ExchangeRateEventPort {
    fun publish(event: ExchangeRateFetchedEvent)
}
