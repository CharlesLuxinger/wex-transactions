package com.charlesluxinger.wex_transactions.infra.adapter.event

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.mockito.ArgumentMatchers.anyMap

class RedisExchangeRateEventAdapterTest {
    private val stringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val streamOperations =
        mock(StreamOperations::class.java) as StreamOperations<String, String, String>
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val streamProperties = ExchangeRateEventsStreamProperties()

    private val adapter =
        RedisExchangeRateEventAdapter(
            stringRedisTemplate = stringRedisTemplate,
            objectMapper = objectMapper,
            streamProperties = streamProperties,
        )

    @Test
    fun `publish success adds to stream`() {
        val event = sampleEvent()

        @Suppress("UNCHECKED_CAST")
        val payloadCaptor =
            ArgumentCaptor.forClass(
                MutableMap::class.java,
            ) as ArgumentCaptor<MutableMap<String, String>>

        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)

        adapter.publish(event)

        verify(streamOperations).add(eq(streamProperties.key), payloadCaptor.capture())
        val payload = payloadCaptor.value
        assertEquals(objectMapper.writeValueAsString(event), payload[streamProperties.payloadField])
    }

    @Test
    fun `publish failure does not propagate`() {
        val event = sampleEvent()
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        doThrow(
            RuntimeException("boom"),
        ).`when`(
            streamOperations,
        ).add(eq(streamProperties.key), anyMap())

        val logger = LoggerFactory.getLogger(RedisExchangeRateEventAdapter::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        try {
            assertDoesNotThrow { adapter.publish(event) }
            assertTrue(
                appender.list.any {
                    it.level == Level.WARN &&
                        it.formattedMessage.contains("Failed to publish ExchangeRateFetchedEvent to stream=")
                },
            )
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `uses correct stream key from properties`() {
        val customProperties =
            ExchangeRateEventsStreamProperties(
                key = "custom-stream-key",
                group = "exchange-rate-fetched-group",
                consumer = "exchange-rate-fetched-consumer",
                payloadField = "payload",
            )
        val customAdapter =
            RedisExchangeRateEventAdapter(
                stringRedisTemplate = stringRedisTemplate,
                objectMapper = objectMapper,
                streamProperties = customProperties,
            )

        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)

        customAdapter.publish(sampleEvent())

        @Suppress("UNCHECKED_CAST")
        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(streamOperations).add(keyCaptor.capture(), anyMap())
        assertEquals(customProperties.key, keyCaptor.value)
    }

    private fun sampleEvent(): ExchangeRateFetchedEvent =
        ExchangeRateFetchedEvent(
            sourceCurrency = "United-States-Dollar",
            targetCurrency = "Brazil-Real",
            rate = BigDecimal("5.25"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            rateDate = LocalDate.parse("2026-01-16"),
        )
}
