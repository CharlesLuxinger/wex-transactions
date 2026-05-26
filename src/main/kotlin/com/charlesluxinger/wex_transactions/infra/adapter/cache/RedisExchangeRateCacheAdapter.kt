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
import java.time.LocalDate

@Component
class RedisExchangeRateCacheAdapter(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : ExchangeRateCachePort {
    override fun getRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                sourceCurrency,
                targetCurrency,
                rateDate,
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
        rateDate: LocalDate,
        rate: ExchangeRate,
    ) {
        val key =
            ExchangeRateCacheKeyBuilder.buildCacheKey(
                sourceCurrency,
                targetCurrency,
                rateDate,
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

    override fun getLatestRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? {
        val pairPrefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(sourceCurrency, targetCurrency)
        return runCatching {
            val keys = stringRedisTemplate.keys("$pairPrefix*").sortedDescending()

            keys.firstNotNullOfOrNull { key ->
                stringRedisTemplate
                    .opsForValue()
                    .get(key)
                    ?.let { value ->
                        objectMapper.readValue(value, ExchangeRateCacheValue::class.java).toDomain()
                    }
            }
        }.onFailure { exception ->
            logger.warn(
                "Failed to read latest exchange rate from cache for keyPrefix={} due to {}",
                pairPrefix,
                exception.javaClass.simpleName,
            )
        }.getOrNull()
    }

    private companion object {
        private const val CACHE_TTL_DAYS = 180L
        private val logger = LoggerFactory.getLogger(RedisExchangeRateCacheAdapter::class.java)
    }
}
