package com.charlesluxinger.wex_transactions.infra.adapter.cache

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRateLookupWindow
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
    override fun getEligibleRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? {
        val pairPrefix = ExchangeRateCacheKeyBuilder.buildPairPrefix(sourceCurrency, targetCurrency)
        val minDate = ExchangeRateLookupWindow.minEligibleDate(rateDate)

        return runCatching {
            stringRedisTemplate
                .keys("$pairPrefix*")
                .mapNotNull { key -> parseRateDateFromKey(key)?.let { keyDate -> key to keyDate } }
                .filter { (_, keyDate) -> ExchangeRateLookupWindow.isEligible(keyDate, rateDate) }
                .maxByOrNull { (_, keyDate) -> keyDate }
                ?.let { (key, _) -> readRateForKey(key, targetCurrency) }
        }.onFailure { exception ->
            logger.warn(
                "Failed to read eligible exchange rate from cache for keyPrefix={} due to {}",
                pairPrefix,
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
        if (!rate.targetCurrency.value.equals(targetCurrency.value, ignoreCase = true)) {
            logger.warn(
                "Skipping cache write due to target currency mismatch requested={} cached={}",
                targetCurrency.value,
                rate.targetCurrency.value,
            )
            return
        }

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

    private fun readRateForKey(
        key: String,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? {
        val value =
            stringRedisTemplate
                .opsForValue()
                .get(key)
                ?: return null

        val rate = objectMapper.readValue(value, ExchangeRateCacheValue::class.java).toDomain()
        return if (rate.targetCurrency.value.equals(targetCurrency.value, ignoreCase = true)) {
            rate
        } else {
            logger.warn(
                "Ignoring cached rate for key={} due to target currency mismatch expected={} actual={}",
                key,
                targetCurrency.value,
                rate.targetCurrency.value,
            )
            null
        }
    }

    private fun parseRateDateFromKey(key: String): LocalDate? {
        val datePart = key.substringAfterLast(':')
        return runCatching { LocalDate.parse(datePart) }.getOrNull()
    }

    private companion object {
        private const val CACHE_TTL_DAYS = 180L
        private val logger = LoggerFactory.getLogger(RedisExchangeRateCacheAdapter::class.java)
    }
}
