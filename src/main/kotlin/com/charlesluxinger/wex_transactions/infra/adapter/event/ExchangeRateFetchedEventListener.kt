package com.charlesluxinger.wex_transactions.infra.adapter.event

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExchangeRateFetchedEventListener(
    private val exchangeRateCachePort: ExchangeRateCachePort,
    private val stringRedisTemplate: StringRedisTemplate,
    private val streamProperties: ExchangeRateEventsStreamProperties,
    objectMapper: ObjectMapper,
) : StreamListener<String, MapRecord<String, String, String>> {
    private val eventReader =
        objectMapper
            .copy()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val consumerName = "${streamProperties.consumer}-${UUID.randomUUID()}"
    val consumer: Consumer = Consumer.from(streamProperties.group, consumerName)
    private val streamOps = stringRedisTemplate.opsForStream<String, String>()

    override fun onMessage(record: MapRecord<String, String, String>) {
        handleRecord(record)
    }

    private fun handleRecord(record: MapRecord<String, String, String>) {
        MDC.put("traceId", streamProperties.resolveTraceId(record))

        try {
            val event =
                extractPayload(record)?.let { payload ->
                    runCatching { eventReader.readValue(payload, ExchangeRateFetchedEvent::class.java) }
                        .onFailure { ex ->
                            logger.warn("Ignoring malformed exchange-rate-fetched stream payload", ex)
                        }.getOrNull()
                } ?: return

            runCatching {
                val rate =
                    ExchangeRate(
                        rate = event.rate,
                        sourceCurrency = TargetCurrency(event.sourceCurrency),
                        targetCurrency = TargetCurrency(event.targetCurrency),
                        retrievedAt = event.retrievedAt,
                    )
                exchangeRateCachePort.saveRate(
                    sourceCurrency = TargetCurrency(event.sourceCurrency),
                    targetCurrency = TargetCurrency(event.targetCurrency),
                    rate = rate,
                )
            }.onSuccess {
                acknowledge(record)
            }.onFailure { ex ->
                logger.error("Failed to save exchange rate from stream event", ex)
            }
        } finally {
            MDC.clear()
        }
    }

    private fun extractPayload(record: MapRecord<String, String, String>): String? {
        val payload =
            record.value[streamProperties.payloadField]
                ?: record.value.values.singleOrNull()

        if (payload == null) {
            logger.warn("Ignoring exchange-rate-fetched stream payload with missing body")
        }
        return payload
    }

    private fun acknowledge(record: MapRecord<String, String, String>) {
        streamOps.acknowledge(streamProperties.group, record)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExchangeRateFetchedEventListener::class.java)
    }
}
