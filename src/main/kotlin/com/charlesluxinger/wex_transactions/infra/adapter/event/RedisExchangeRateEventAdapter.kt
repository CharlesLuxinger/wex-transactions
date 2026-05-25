package com.charlesluxinger.wex_transactions.infra.adapter.event

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateEventPort
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RedisExchangeRateEventAdapter(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val streamProperties: ExchangeRateEventsStreamProperties,
) : ExchangeRateEventPort {
    override fun publish(event: ExchangeRateFetchedEvent) {
        runCatching {
            val payload = objectMapper.writeValueAsString(event)
            val traceId = MDC.get("traceId") ?: UUID.randomUUID().toString().replace("-", "")
            val streamKey = streamProperties.key
            val payloadRecord =
                mapOf(
                    streamProperties.payloadField to payload,
                    streamProperties.traceIdField to traceId,
                )
            stringRedisTemplate.opsForStream<String, String>().add(streamKey, payloadRecord)
        }.onFailure { exception ->
            logger.warn(
                "Failed to publish ExchangeRateFetchedEvent to stream={} due to {}: {}",
                streamProperties.key,
                exception.javaClass.simpleName,
                exception.message,
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisExchangeRateEventAdapter::class.java)
    }
}
