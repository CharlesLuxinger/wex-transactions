package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExchangeRateCacheKeyBuilderTest {
    @Test
    fun `buildCacheKey uses exchangeRate source target format`() {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                LocalDate.parse("2026-01-16"),
            )

        assertEquals("exchangeRate:USD:BRL:2026-01-16", key)
    }

    @Test
    fun `buildPairPrefix uses exchangeRate source target prefix format`() {
        val keyPrefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertEquals("exchangeRate:USD:BRL:", keyPrefix)
    }
}
