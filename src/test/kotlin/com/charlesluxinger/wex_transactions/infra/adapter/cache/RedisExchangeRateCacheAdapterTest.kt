package com.charlesluxinger.wex_transactions.infra.adapter.cache

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCacheKeyBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.mockito.ArgumentMatchers.any

class RedisExchangeRateCacheAdapterTest {
    private val stringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    private val adapter = RedisExchangeRateCacheAdapter(stringRedisTemplate, objectMapper)

    @Test
    fun `getRate returns exchange rate when key exists`() {
        val rateDate = LocalDate.parse("2026-01-16")
        val keyValue =
            """
            {"rate":"5.10","sourceCurrency":"USD","targetCurrency":"BRL","retrievedAt":"2026-01-15T12:00:00Z"}
            """.trimIndent()

        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        val key = ExchangeRateCacheKeyBuilder.buildCacheKey(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate)
        `when`(valueOperations.get(key)).thenReturn(keyValue)

        val result = adapter.getRate(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate)

        assertEquals(BigDecimal("5.10"), result?.rate)
        assertEquals(TargetCurrency("USD"), result?.sourceCurrency)
        assertEquals(TargetCurrency("BRL"), result?.targetCurrency)
    }

    @Test
    fun `getRate returns null on cache miss`() {
        val rateDate = LocalDate.parse("2026-01-16")
        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        val key = ExchangeRateCacheKeyBuilder.buildCacheKey(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate)
        `when`(valueOperations.get(key)).thenReturn(null)

        val result = adapter.getRate(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate)

        assertNull(result)
    }

    @Test
    fun `saveRate sets value with 180 days ttl`() {
        val rateDate = LocalDate.parse("2026-01-16")
        val rate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)

        adapter.saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate, rate)

        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val ttlCaptor = ArgumentCaptor.forClass(Duration::class.java)

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture())

        assertEquals(
            ExchangeRateCacheKeyBuilder.buildCacheKey(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate),
            keyCaptor.value,
        )
        assertEquals(Duration.ofDays(180), ttlCaptor.value)
    }

    @Test
    fun `saveRate swallows serialization error`() {
        val rateDate = LocalDate.parse("2026-01-16")
        val brokenMapper = mock(ObjectMapper::class.java)
        val safeAdapter = RedisExchangeRateCacheAdapter(stringRedisTemplate, brokenMapper)
        val rate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency("BRL"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        `when`(brokenMapper.writeValueAsString(any())).thenThrow(RuntimeException("boom"))

        safeAdapter.saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), rateDate, rate)

        verify(stringRedisTemplate, org.mockito.Mockito.never()).opsForValue()
    }

    @Test
    fun `getLatestRate returns latest exchange rate for pair`() {
        val prefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(TargetCurrency("USD"), TargetCurrency("BRL"))
        val newestKey = "${prefix}2026-01-16"
        val olderKey = "${prefix}2026-01-15"
        val newestValue =
            """
            {"rate":"5.10","sourceCurrency":"USD","targetCurrency":"BRL","retrievedAt":"2026-01-16T12:00:00Z"}
            """.trimIndent()
        val olderValue =
            """
            {"rate":"5.05","sourceCurrency":"USD","targetCurrency":"BRL","retrievedAt":"2026-01-15T12:00:00Z"}
            """.trimIndent()

        `when`(stringRedisTemplate.keys("${prefix}*")).thenReturn(setOf(olderKey, newestKey))
        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(newestKey)).thenReturn(newestValue)
        `when`(valueOperations.get(olderKey)).thenReturn(olderValue)

        val result = adapter.getLatestRate(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertEquals(BigDecimal("5.10"), result?.rate)
    }

    @Test
    fun `getLatestRate returns null when pair has no keys`() {
        val prefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(TargetCurrency("USD"), TargetCurrency("BRL"))
        `when`(stringRedisTemplate.keys("${prefix}*")).thenReturn(emptySet())

        val result = adapter.getLatestRate(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertNull(result)
    }
}
