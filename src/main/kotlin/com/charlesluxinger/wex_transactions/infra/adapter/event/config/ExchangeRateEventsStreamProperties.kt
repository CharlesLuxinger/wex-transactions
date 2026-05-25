package com.charlesluxinger.wex_transactions.infra.adapter.event.config

data class ExchangeRateEventsStreamProperties(
    val key: String = "exchange-rate-fetched-events",
    val group: String = "exchange-rate-fetched-group",
    val consumer: String = "exchange-rate-fetched-consumer",
    val payloadField: String = "payload",
)
