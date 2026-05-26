package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRateLookupWindow
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import feign.FeignException
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
        val minDate = ExchangeRateLookupWindow.minEligibleDate(rateDate)
        val filter = buildRateFilter(rateDate, minDate, targetCurrency)

        return treasuryFeignClient
            .fetchRates(
                fields = FIELDS,
                filter = filter,
                sort = SORT,
                pageSize = PAGE_SIZE,
            ).data
            ?.let { data -> mapFirstValidRecord(data, sourceCurrency, targetCurrency) }
    }

    override fun isSupportedCurrency(targetCurrency: TargetCurrency): Boolean = currencyDescriptorExists(targetCurrency)

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

        val eligibleCachedRate =
            exchangeRateCachePort.getEligibleRate(
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
            )

        if (eligibleCachedRate != null) {
            logger.warn(
                "[ADAPTER][FALLBACK_CACHE] sourceCurrency={} targetCurrency={} cachedRetrievedAt={}",
                sourceCurrency.value,
                targetCurrency.value,
                eligibleCachedRate.retrievedAt,
            )
            return eligibleCachedRate
        }

        throw RateUnavailableException(sourceCurrency.value, targetCurrency.value)
    }

    private fun mapFirstValidRecord(
        data: List<TreasuryRateRecord>,
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? =
        data
            .firstOrNull {
                it.hasValidExchangeRate &&
                    it.hasValidRecordDate &&
                    it.hasValidDescription &&
                    it.countryCurrencyDesc.equals(targetCurrency.value, ignoreCase = true)
            }?.let { matched ->
                val parsedRate = matched.parsedRate
                val recordDate = matched.parsedRecordDateOrNull
                if (parsedRate != null && recordDate != null) {
                    ExchangeRate(
                        parsedRate,
                        sourceCurrency,
                        TargetCurrency(matched.countryCurrencyDesc),
                        matched.retrievedAt,
                    )
                } else {
                    null
                }
            }

    private fun currencyDescriptorExists(targetCurrency: TargetCurrency): Boolean {
        val filter = buildDescriptorFilter(targetCurrency)
        return try {
            val data =
                treasuryFeignClient
                    .fetchRates(
                        fields = FIELDS,
                        filter = filter,
                        sort = SORT,
                        pageSize = 1,
                    ).data
                    ?: return false

            data.any {
                it.hasValidDescription &&
                    it.countryCurrencyDesc.equals(targetCurrency.value, ignoreCase = true)
            }
        } catch (exception: FeignException) {
            logger.warn(
                "Unable to validate treasury currency descriptor for {}",
                targetCurrency.value,
                exception,
            )
            false
        }
    }

    companion object {
        private const val TREASURY_RATES_RESILIENCE = "treasury-rates"
        private const val TREASURY_API_RATE_LIMITER = "treasury-api"
        private val logger = LoggerFactory.getLogger(ExchangeRateTreasuryAdapter::class.java)
        const val FIELDS = "record_date,country,currency,country_currency_desc,exchange_rate"
        const val SORT = "-record_date"
        const val PAGE_SIZE = 10_000

        fun buildRateFilter(
            rateDate: LocalDate,
            minDate: LocalDate,
            targetCurrency: TargetCurrency,
        ): String =
            "record_date:lte:$rateDate,record_date:gte:$minDate,country_currency_desc:eq:${targetCurrency.value}"

        fun buildDescriptorFilter(targetCurrency: TargetCurrency): String =
            "country_currency_desc:eq:${targetCurrency.value}"
    }
}
