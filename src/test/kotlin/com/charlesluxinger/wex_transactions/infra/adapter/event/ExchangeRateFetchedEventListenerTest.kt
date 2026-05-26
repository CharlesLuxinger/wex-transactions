package com.charlesluxinger.wex_transactions.infra.adapter.event

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.infra.adapter.event.config.ExchangeRateEventsStreamProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.io.IOException
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
    fun `happy path should save cache with exact event params including rate date`() {
        val listener = createListener()
        val event = sampleEvent(rateDate = LocalDate.parse("2026-01-16"))
        val record = eventRecord(event)

        listener.onMessage(record)

        val sourceCaptor = ArgumentCaptor.forClass(TargetCurrency::class.java)
        val targetCaptor = ArgumentCaptor.forClass(TargetCurrency::class.java)
        val rateDateCaptor = ArgumentCaptor.forClass(LocalDate::class.java)
        val rateCaptor = ArgumentCaptor.forClass(ExchangeRate::class.java)

        verify(exchangeRateCachePort, times(1)).saveRate(
            sourceCaptor.capture(),
            targetCaptor.capture(),
            rateDateCaptor.capture(),
            rateCaptor.capture(),
        )

        assertEquals("USD", sourceCaptor.value.code)
        assertEquals("BRL", targetCaptor.value.code)
        assertEquals(LocalDate.parse("2026-01-16"), rateDateCaptor.value)
        assertNotNull(rateCaptor.value)
        assertEquals(BigDecimal("5.2500"), rateCaptor.value.rate)
        assertEquals("USD", rateCaptor.value.sourceCurrency.code)
        assertEquals("BRL", rateCaptor.value.targetCurrency.code)
        assertEquals(Instant.parse("2026-01-15T12:00:00Z"), rateCaptor.value.retrievedAt)

        verify(streamOperations, times(1)).acknowledge(streamProperties.group, record)
    }

    @Test
    fun `failure path should catch cache exception log and not rethrow`() {
        val listener = createListener()
        val expectedRateDate = LocalDate.parse("2026-01-16")
        val event = sampleEvent(rateDate = expectedRateDate)
        val record = eventRecord(event)

        val expectedRate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        doThrow(IOException("cache unavailable"))
            .`when`(exchangeRateCachePort)
            .saveRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                expectedRateDate,
                expectedRate,
            )

        val logger = LoggerFactory.getLogger(ExchangeRateFetchedEventListener::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        try {
            assertDoesNotThrow { listener.onMessage(record) }

            verify(exchangeRateCachePort, times(1)).saveRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                expectedRateDate,
                expectedRate,
            )
            verify(streamOperations, never()).acknowledge(streamProperties.group, record)

            assertTrue(
                appender.list.any {
                    (it.level == Level.WARN || it.level == Level.ERROR) &&
                        it.formattedMessage.contains("Failed to save exchange rate from stream event")
                },
            )
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `serialization should preserve exchange rate fields through json roundtrip`() {
        val exchangeRate =
            ExchangeRate(
                rate = BigDecimal("5.1234"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        val payload = objectMapper.writeValueAsString(exchangeRate)
        val restored = objectMapper.readValue(payload, ExchangeRate::class.java)

        assertEquals(exchangeRate.rate, restored.rate)
        assertEquals(exchangeRate.sourceCurrency, restored.sourceCurrency)
        assertEquals(exchangeRate.targetCurrency, restored.targetCurrency)
        assertEquals(exchangeRate.retrievedAt, restored.retrievedAt)
    }

    @Test
    fun `date parameter should pass event rate date to cache save call`() {
        val listener = createListener()
        val expectedRateDate = LocalDate.parse("2026-02-03")
        val event = sampleEvent(rateDate = expectedRateDate)
        val record = eventRecord(event)

        listener.onMessage(record)

        val rateDateCaptor = ArgumentCaptor.forClass(LocalDate::class.java)
        verify(exchangeRateCachePort).saveRate(
            org.mockito.ArgumentMatchers.any(TargetCurrency::class.java),
            org.mockito.ArgumentMatchers.any(TargetCurrency::class.java),
            rateDateCaptor.capture(),
            org.mockito.ArgumentMatchers.any(ExchangeRate::class.java),
        )

        assertEquals(expectedRateDate, rateDateCaptor.value)
    }

    private fun createListener(): ExchangeRateFetchedEventListener {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        return ExchangeRateFetchedEventListener(
            exchangeRateCachePort = exchangeRateCachePort,
            stringRedisTemplate = stringRedisTemplate,
            streamProperties = streamProperties,
            objectMapper = objectMapper,
        )
    }

    private fun eventRecord(event: ExchangeRateFetchedEvent): MapRecord<String, String, String> {
        val payload = objectMapper.writeValueAsString(event)
        return MapRecord.create(streamProperties.key, mapOf(streamProperties.payloadField to payload))
    }

    private fun sampleEvent(rateDate: LocalDate): ExchangeRateFetchedEvent =
        ExchangeRateFetchedEvent(
            sourceCurrency = "USD",
            targetCurrency = "BRL",
            rate = BigDecimal("5.25"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            rateDate = rateDate,
        )
}
