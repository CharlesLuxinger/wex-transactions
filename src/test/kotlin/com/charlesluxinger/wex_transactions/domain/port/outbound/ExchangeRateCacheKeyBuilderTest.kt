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
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 15),
            )

        assertEquals("exchangeRate:United-States-Dollar:Canada-Dollar:2024-01-15", key)
    }

    @Test
    fun `buildCacheKey creates distinct keys for distinct dates`() {
        val olderDateKey =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 14),
            )
        val newerDateKey =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Canada-Dollar"),
                LocalDate.of(2024, 1, 15),
            )

        assertEquals("exchangeRate:United-States-Dollar:Canada-Dollar:2024-01-14", olderDateKey)
        assertEquals("exchangeRate:United-States-Dollar:Canada-Dollar:2024-01-15", newerDateKey)
    }

    @Test
    fun `buildPairPrefix uses exchangeRate source target prefix format`() {
        val keyPrefix =
            ExchangeRateCacheKeyBuilder.buildPairPrefix(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
            )

        assertEquals("exchangeRate:United-States-Dollar:Brazil-Real:", keyPrefix)
    }
}
