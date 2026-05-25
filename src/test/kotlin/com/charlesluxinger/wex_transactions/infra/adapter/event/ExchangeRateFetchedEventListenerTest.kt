package com.charlesluxinger.wex_transactions.infra.adapter.event

import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class ExchangeRateFetchedEventListenerTest {
    private val exchangeRateCachePort = mock(ExchangeRateCachePort::class.java)
    private val stringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val streamOperations =
        mock(StreamOperations::class.java) as StreamOperations<String, String, String>

    private val streamProperties = ExchangeRateEventsStreamProperties()
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `valid event saves to cache and acknowledges`() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        val listener =
            ExchangeRateFetchedEventListener(
                exchangeRateCachePort = exchangeRateCachePort,
                stringRedisTemplate = stringRedisTemplate,
                streamProperties = streamProperties,
                objectMapper = objectMapper,
            )

        val event = sampleEvent()
        val payload = objectMapper.writeValueAsString(event)
        val record = MapRecord.create(streamProperties.key, mapOf(streamProperties.payloadField to payload))

        listener.onMessage(record)

        val expectedRate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        verify(exchangeRateCachePort).saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), expectedRate)
        verify(streamOperations).acknowledge(streamProperties.group, record)
    }

    @Test
    fun `malformed payload does not save or acknowledge`() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        val listener =
            ExchangeRateFetchedEventListener(
                exchangeRateCachePort = exchangeRateCachePort,
                stringRedisTemplate = stringRedisTemplate,
                streamProperties = streamProperties,
                objectMapper = objectMapper,
            )

        val record = MapRecord.create(streamProperties.key, mapOf(streamProperties.payloadField to "not-json"))

        listener.onMessage(record)

        verifyNoInteractions(exchangeRateCachePort)
        verify(streamOperations, never()).acknowledge(streamProperties.group, record)
    }

    @Test
    fun `cache save failure does not acknowledge`() {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        val listener =
            ExchangeRateFetchedEventListener(
                exchangeRateCachePort = exchangeRateCachePort,
                stringRedisTemplate = stringRedisTemplate,
                streamProperties = streamProperties,
                objectMapper = objectMapper,
            )

        val event = sampleEvent()
        val payload = objectMapper.writeValueAsString(event)
        val record = MapRecord.create(streamProperties.key, mapOf(streamProperties.payloadField to payload))

        val expectedRate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        doThrow(RuntimeException("cache failure"))
            .`when`(exchangeRateCachePort)
            .saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), expectedRate)

        listener.onMessage(record)

        verify(exchangeRateCachePort).saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), expectedRate)
        verify(streamOperations, never()).acknowledge(streamProperties.group, record)
    }

    private fun sampleEvent(): ExchangeRateFetchedEvent =
        ExchangeRateFetchedEvent(
            sourceCurrency = "USD",
            targetCurrency = "BRL",
            rate = BigDecimal("5.25"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            rateDate = LocalDate.parse("2026-01-16"),
        )
}
