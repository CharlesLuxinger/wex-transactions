package com.charlesluxinger.wex_transactions.infra.adapter.event

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.MDC
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertNull

class ExchangeRateFetchedEventListenerTraceIdTest {
    private val exchangeRateCachePort = mock(ExchangeRateCachePort::class.java)
    private val stringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val streamOperations =
        mock(StreamOperations::class.java) as StreamOperations<String, String, String>

    private val streamProperties = ExchangeRateEventsStreamProperties()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private lateinit var listener: ExchangeRateFetchedEventListener

    @BeforeEach
    fun setUp() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        listener =
            ExchangeRateFetchedEventListener(
                exchangeRateCachePort = exchangeRateCachePort,
                stringRedisTemplate = stringRedisTemplate,
                streamProperties = streamProperties,
                objectMapper = objectMapper,
            )
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `should set MDC from envelope traceId`() {
        val record = createRecord(traceId = "envelope-trace-id-001")

        listener.onMessage(record)

        assertNull(MDC.get("traceId"), "MDC must be cleared after processing")
    }

    @Test
    fun `should generate UUID when envelope traceId is absent`() {
        val record = createRecord(traceId = null)

        listener.onMessage(record)

        assertNull(MDC.get("traceId"), "MDC must be cleared after processing")
    }

    @Test
    fun `should clear MDC after processing`() {
        val record = createRecord(traceId = "some-trace-id")

        listener.onMessage(record)

        assertNull(MDC.get("traceId"))
    }

    @Test
    fun `should clear MDC even on processing failure`() {
        val record = createRecord(traceId = "fail-trace", payload = "invalid-json")

        listener.onMessage(record)

        assertNull(MDC.get("traceId"))
    }

    @Test
    fun `should handle record without traceId field gracefully`() {
        val record = MapRecord.create(streamProperties.key, mapOf("payload" to serializedEvent()))

        listener.onMessage(record)

        assertNull(MDC.get("traceId"), "MDC must be cleared after processing")
    }

    private fun createRecord(
        traceId: String?,
        payload: String = serializedEvent(),
    ): MapRecord<String, String, String> {
        val values = mutableMapOf<String, String>("payload" to payload)
        if (traceId != null) {
            values["traceId"] = traceId
        }
        return MapRecord.create(streamProperties.key, values)
    }

    private fun serializedEvent(): String {
        val event =
            ExchangeRateFetchedEvent(
                sourceCurrency = "United-States-Dollar",
                targetCurrency = "Brazil-Real",
                rate = BigDecimal("5.12"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
                rateDate = LocalDate.parse("2026-01-16"),
            )
        return objectMapper.writeValueAsString(event)
    }
}
