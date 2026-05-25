package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExchangeRateCacheKeyBuilderTest {
    @Test
    fun `buildCacheKey uses exchangeRate source target format`() {
        val key = ExchangeRateCacheKeyBuilder.buildCacheKey(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertEquals("exchangeRate:USD:BRL", key)
    }
}
