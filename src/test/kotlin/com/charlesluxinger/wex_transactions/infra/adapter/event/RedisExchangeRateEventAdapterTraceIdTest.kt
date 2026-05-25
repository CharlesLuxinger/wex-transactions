package com.charlesluxinger.wex_transactions.infra.adapter.event

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.MDC
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RedisExchangeRateEventAdapterTraceIdTest {
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

    private val event = sampleEvent()

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `should include traceId in published record when MDC has traceId`() {
        val expectedTraceId = "trace-from-mdc-001"
        MDC.put("traceId", expectedTraceId)

        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)

        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<MutableMap<String, String>>

        adapter.publish(event)

        verify(streamOperations).add(eq(streamProperties.key), captor.capture())
        val record = captor.value
        assertNotNull(record[streamProperties.payloadField])
        assertTrue(record[streamProperties.traceIdField] == expectedTraceId)
    }

    @Test
    fun `should generate traceId when MDC is empty`() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)

        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<MutableMap<String, String>>

        adapter.publish(event)

        verify(streamOperations).add(eq(streamProperties.key), captor.capture())
        val record = captor.value
        val traceId = record[streamProperties.traceIdField]
        assertNotNull(traceId)
        assertTrue(traceId!!.length == 32)
        assertTrue(traceId.matches(Regex("[a-f0-9]+")))
    }

    @Test
    fun `should preserve payload content when traceId is added`() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)

        @Suppress("UNCHECKED_CAST")
        val captor =
            ArgumentCaptor.forClass(MutableMap::class.java) as ArgumentCaptor<MutableMap<String, String>>

        adapter.publish(event)

        verify(streamOperations).add(eq(streamProperties.key), captor.capture())
        val record = captor.value
        val payload = record[streamProperties.payloadField]
        assertNotNull(payload)
        assertTrue(payload!!.contains("USD"))
        assertTrue(payload.contains("BRL"))
    }

    private fun sampleEvent(): ExchangeRateFetchedEvent =
        ExchangeRateFetchedEvent(
            sourceCurrency = "USD",
            targetCurrency = "BRL",
            rate = BigDecimal("5.12"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            rateDate = LocalDate.parse("2026-01-16"),
        )
}
