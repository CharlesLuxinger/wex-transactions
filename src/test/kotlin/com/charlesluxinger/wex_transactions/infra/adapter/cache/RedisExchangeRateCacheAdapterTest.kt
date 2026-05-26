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
    fun `getEligibleRate returns nearest prior rate within six month window`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val prefix =
            ExchangeRateCacheKeyBuilder.buildPairPrefix(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
            )
        val eligibleKey = "${prefix}2026-01-15"
        val tooOldKey = "${prefix}2025-01-01"
        val futureKey = "${prefix}2026-01-20"
        val eligibleValue =
            """
            {"rate":"5.10","sourceCurrency":"United-States-Dollar","targetCurrency":"Brazil-Real","retrievedAt":"2026-01-15T12:00:00Z"}
            """.trimIndent()

        `when`(stringRedisTemplate.keys("$prefix*")).thenReturn(setOf(tooOldKey, eligibleKey, futureKey))
        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(eligibleKey)).thenReturn(eligibleValue)

        val result =
            adapter.getEligibleRate(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                purchaseDate,
            )

        assertEquals(BigDecimal("5.10"), result?.rate)
    }

    @Test
    fun `getEligibleRate returns null on cache miss`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val prefix =
            ExchangeRateCacheKeyBuilder.buildPairPrefix(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
            )
        `when`(stringRedisTemplate.keys("$prefix*")).thenReturn(emptySet())

        val result =
            adapter.getEligibleRate(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                purchaseDate,
            )

        assertNull(result)
    }

    @Test
    fun `getEligibleRate ignores cached rate with mismatched target currency`() {
        val purchaseDate = LocalDate.parse("2026-01-16")
        val prefix =
            ExchangeRateCacheKeyBuilder.buildPairPrefix(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
            )
        val key = "${prefix}2026-01-15"
        val mismatchedValue =
            """
            {"rate":"5.10","sourceCurrency":"United-States-Dollar","targetCurrency":"Euro Area-Euro","retrievedAt":"2026-01-15T12:00:00Z"}
            """.trimIndent()

        `when`(stringRedisTemplate.keys("$prefix*")).thenReturn(setOf(key))
        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
        `when`(valueOperations.get(key)).thenReturn(mismatchedValue)

        val result =
            adapter.getEligibleRate(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                purchaseDate,
            )

        assertNull(result)
    }

    @Test
    fun `saveRate sets value with 180 days ttl`() {
        val rateDate = LocalDate.parse("2026-01-16")
        val rate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("United-States-Dollar"),
                targetCurrency = TargetCurrency("Brazil-Real"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)

        adapter.saveRate(TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), rateDate, rate)

        val keyCaptor = ArgumentCaptor.forClass(String::class.java)
        val valueCaptor = ArgumentCaptor.forClass(String::class.java)
        val ttlCaptor = ArgumentCaptor.forClass(Duration::class.java)

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture())

        assertEquals(
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                rateDate,
            ),
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
                sourceCurrency = TargetCurrency("United-States-Dollar"),
                targetCurrency = TargetCurrency("Brazil-Real"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        `when`(brokenMapper.writeValueAsString(any())).thenThrow(RuntimeException("boom"))

        safeAdapter.saveRate(TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), rateDate, rate)

        verify(stringRedisTemplate, org.mockito.Mockito.never()).opsForValue()
    }

    @Test
    fun `saveRate skips write when target currency mismatches`() {
        val rateDate = LocalDate.parse("2026-01-16")
        val rate =
            ExchangeRate(
                rate = BigDecimal("5.25"),
                sourceCurrency = TargetCurrency("United-States-Dollar"),
                targetCurrency = TargetCurrency("Euro Area-Euro"),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            )

        adapter.saveRate(TargetCurrency("United-States-Dollar"), TargetCurrency("Brazil-Real"), rateDate, rate)

        verify(stringRedisTemplate, org.mockito.Mockito.never()).opsForValue()
    }
}
