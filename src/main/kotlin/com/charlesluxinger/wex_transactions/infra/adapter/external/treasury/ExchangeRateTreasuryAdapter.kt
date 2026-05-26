package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ExchangeRateTreasuryAdapter(
    private val treasuryFeignClient: TreasuryFeignClient,
    private val exchangeRateCachePort: ExchangeRateCachePort,
) : ExchangeRateClientPort {
    @CircuitBreaker(name = TREASURY_RATES_RESILIENCE, fallbackMethod = "fallback")
    @Bulkhead(name = TREASURY_RATES_RESILIENCE, type = Bulkhead.Type.SEMAPHORE)
    @RateLimiter(name = TREASURY_API_RATE_LIMITER)
    override fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? {
        val minDate = rateDate.minusMonths(DEFAULT_WINDOW_MONTHS)

        return treasuryFeignClient
            .fetchRates(
                fields = FIELDS,
                filter = FILTER_FORMAT.format(rateDate, minDate),
                sort = SORT,
                pageSize = PAGE_SIZE,
            ).data
            ?.let { data ->
                val matched =
                    data.firstOrNull {
                        it.hasValidExchangeRate &&
                            it.hasValidRecordDate &&
                            it.hasValidDescription
                    } ?: return@let null

                val parsedRate = matched.parsedRate ?: return@let null
                if (matched.parsedRecordDateOrNull == null) return@let null

                ExchangeRate(
                    parsedRate,
                    sourceCurrency,
                    TargetCurrency(matched.countryCurrencyDesc),
                    matched.retrievedAt,
                )
            }
    }

    @Suppress("unused")
    fun fallback(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        throwable: Throwable,
    ): ExchangeRate? {
        logger.error(
            "Treasury rate lookup failed for {} -> {} on {}",
            sourceCurrency.value,
            targetCurrency.value,
            rateDate,
            throwable,
        )

        val latestCachedRate =
            exchangeRateCachePort.getLatestRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
            )

        if (latestCachedRate != null) {
            logger.warn(
                "[ADAPTER][FALLBACK_CACHE] sourceCurrency={} targetCurrency={} cachedRetrievedAt={}",
                sourceCurrency.value,
                targetCurrency.value,
                latestCachedRate.retrievedAt,
            )
            return latestCachedRate
        }

        throw RateUnavailableException(sourceCurrency.value, targetCurrency.value)
    }

    companion object {
        private const val DEFAULT_WINDOW_MONTHS = 6L
        private const val TREASURY_RATES_RESILIENCE = "treasury-rates"
        private const val TREASURY_API_RATE_LIMITER = "treasury-api"
        private val logger = LoggerFactory.getLogger(ExchangeRateTreasuryAdapter::class.java)
        const val FIELDS = "record_date,country,currency,country_currency_desc,exchange_rate"
        const val FILTER_FORMAT = "record_date:lte:%s,record_date:gte:%s"
        const val SORT = "-record_date"
        const val PAGE_SIZE = 10_000
    }
}
