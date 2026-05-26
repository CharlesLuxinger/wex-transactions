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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExchangeRateFetchedEventListenerTest {
    @Mock
    private lateinit var exchangeRateCachePort: ExchangeRateCachePort

    @Mock
    private lateinit var stringRedisTemplate: StringRedisTemplate

    @Mock
    private lateinit var streamOperations: StreamOperations<String, String, String>

    private val streamProperties = ExchangeRateEventsStreamProperties()
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @BeforeEach
    fun setUp() {
        reset(exchangeRateCachePort, stringRedisTemplate, streamOperations)
    }

    @Test
    fun `happy path should save cache with exact event params including rate date`() {
        val listener = createListener()
        val event = sampleEvent(rateDate = LocalDate.parse("2026-01-16"))
        val record = eventRecord(event)
        val expectedRate =
            ExchangeRate(
                rate = event.rate,
                sourceCurrency = TargetCurrency(event.sourceCurrency),
                targetCurrency = TargetCurrency(event.targetCurrency),
                retrievedAt = event.retrievedAt,
            )

        listener.onMessage(record)

        verify(exchangeRateCachePort).saveRate(
            TargetCurrency(event.sourceCurrency),
            TargetCurrency(event.targetCurrency),
            event.rateDate,
            expectedRate,
        )

        verify(streamOperations).acknowledge(streamProperties.group, record)
    }

    @Test
    fun `failure path should catch cache exception log and not rethrow`() {
        val failingCachePort =
            object : ExchangeRateCachePort {
                override fun getRate(
                    sourceCurrency: TargetCurrency,
                    targetCurrency: TargetCurrency,
                    rateDate: LocalDate,
                ): ExchangeRate? = null

                override fun saveRate(
                    sourceCurrency: TargetCurrency,
                    targetCurrency: TargetCurrency,
                    rateDate: LocalDate,
                    rate: ExchangeRate,
                ): Unit = throw IOException("cache unavailable")

                override fun getLatestRate(
                    sourceCurrency: TargetCurrency,
                    targetCurrency: TargetCurrency,
                ): ExchangeRate? = null
            }
        val listener = createListener(failingCachePort)
        val expectedRateDate = LocalDate.parse("2026-01-16")
        val event = sampleEvent(rateDate = expectedRateDate)
        val record = eventRecord(event)

        val logger = LoggerFactory.getLogger(ExchangeRateFetchedEventListener::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        try {
            assertDoesNotThrow { listener.onMessage(record) }

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
                sourceCurrency = TargetCurrency("United-States-Dollar"),
                targetCurrency = TargetCurrency("Brazil-Real"),
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
        val expectedRate =
            ExchangeRate(
                rate = event.rate,
                sourceCurrency = TargetCurrency(event.sourceCurrency),
                targetCurrency = TargetCurrency(event.targetCurrency),
                retrievedAt = event.retrievedAt,
            )

        listener.onMessage(record)

        verify(exchangeRateCachePort).saveRate(
            TargetCurrency(event.sourceCurrency),
            TargetCurrency(event.targetCurrency),
            expectedRateDate,
            expectedRate,
        )
    }

    private fun createListener(
        cachePort: ExchangeRateCachePort = exchangeRateCachePort,
    ): ExchangeRateFetchedEventListener {
        `when`(stringRedisTemplate.opsForStream<String, String>()).thenReturn(streamOperations)
        return ExchangeRateFetchedEventListener(
            exchangeRateCachePort = cachePort,
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
            sourceCurrency = "United-States-Dollar",
            targetCurrency = "Brazil-Real",
            rate = BigDecimal("5.25"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            rateDate = rateDate,
        )
}
