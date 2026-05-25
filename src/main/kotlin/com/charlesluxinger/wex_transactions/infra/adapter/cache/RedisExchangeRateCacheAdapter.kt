package com.charlesluxinger.wex_transactions.infra.adapter.cache

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCacheKeyBuilder
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisExchangeRateCacheAdapter(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : ExchangeRateCachePort {
    override fun getRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                sourceCurrency,
                targetCurrency,
            )
        return runCatching {
            stringRedisTemplate
                .opsForValue()
                .get(key)
                ?.let { value -> objectMapper.readValue(value, ExchangeRateCacheValue::class.java).toDomain() }
        }.onFailure { exception ->
            logger.warn(
                "Failed to read exchange rate from cache for key={} due to {}",
                key,
                exception.javaClass.simpleName,
            )
        }.getOrNull()
    }

    override fun saveRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rate: ExchangeRate,
    ) {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                sourceCurrency,
                targetCurrency,
            )
        runCatching {
            val value = objectMapper.writeValueAsString(ExchangeRateCacheValue.fromDomain(rate))
            stringRedisTemplate
                .opsForValue()
                .set(key, value, Duration.ofDays(CACHE_TTL_DAYS))
        }.onFailure { exception ->
            logger.warn(
                "Failed to write exchange rate to cache for key={} due to {}",
                key,
                exception.javaClass.simpleName,
            )
        }
    }

    private companion object {
        private const val CACHE_TTL_DAYS = 180L
        private val logger = LoggerFactory.getLogger(RedisExchangeRateCacheAdapter::class.java)
    }
}

data class ExchangeRateCacheValue(
    val rate: String,
    val sourceCurrency: String,
    val targetCurrency: String,
    val retrievedAt: String,
) {
    fun toDomain(): ExchangeRate =
        ExchangeRate(
            rate = rate.toBigDecimal(),
            sourceCurrency = TargetCurrency(sourceCurrency),
            targetCurrency = TargetCurrency(targetCurrency),
            retrievedAt = java.time.Instant.parse(retrievedAt),
        )

    companion object {
        fun fromDomain(exchangeRate: ExchangeRate): ExchangeRateCacheValue =
            ExchangeRateCacheValue(
                rate = exchangeRate.rate.toPlainString(),
                sourceCurrency = exchangeRate.sourceCurrency.code,
                targetCurrency = exchangeRate.targetCurrency.code,
                retrievedAt = exchangeRate.retrievedAt.toString(),
            )
    }
}
