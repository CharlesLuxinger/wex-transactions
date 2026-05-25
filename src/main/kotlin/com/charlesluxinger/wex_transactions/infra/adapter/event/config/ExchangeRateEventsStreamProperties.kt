package com.charlesluxinger.wex_transactions.infra.adapter.event.config

import org.springframework.data.redis.connection.stream.MapRecord
import java.util.UUID

data class ExchangeRateEventsStreamProperties(
    val key: String = "exchange-rate-fetched-events",
    val group: String = "exchange-rate-fetched-group",
    val consumer: String = "exchange-rate-fetched-consumer",
    val payloadField: String = "payload",
    val traceIdField: String = "traceId",
) {
    fun resolveTraceId(record: MapRecord<String, String, String>): String {
        val envelopeTraceId = record.value[traceIdField]
        return if (!envelopeTraceId.isNullOrBlank()) envelopeTraceId else UUID.randomUUID().toTraceId()
    }
}
