package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExchangeRateCacheKeyBuilderTest {
    @Test
    fun `buildCacheKey uses exchangeRate source target date format`() {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("USD"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 15),
            )

        assertEquals("exchangeRate:USD:Canada-Dollar:2024-01-15", key)
    }

    @Test
    fun `buildCacheKey creates distinct keys for distinct dates`() {
        val olderDateKey =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("USD"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 14),
            )
        val newerDateKey =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("USD"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 15),
            )

        assertEquals("exchangeRate:USD:Canada-Dollar:2024-01-14", olderDateKey)
        assertEquals("exchangeRate:USD:Canada-Dollar:2024-01-15", newerDateKey)
    }

    @Test
    fun `buildPairPrefix uses exchangeRate source target prefix format`() {
        val keyPrefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertEquals("exchangeRate:USD:BRL:", keyPrefix)
    }
}
