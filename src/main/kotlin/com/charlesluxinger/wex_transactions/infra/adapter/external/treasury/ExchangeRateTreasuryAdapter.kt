package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Component
class ExchangeRateTreasuryAdapter(
    private val treasuryFeignClient: TreasuryFeignClient,
) : ExchangeRateClientPort {
    override fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate {
        val rate =
            fetchNearestPriorRate(from, to, LocalDate.now(), DEFAULT_WINDOW_MONTHS)
                ?: throw IllegalStateException("No exchange rate available for $from -> $to")
        return rate
    }

    override fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        maxWindowMonths: Long,
    ): ExchangeRate? {
        val minDate = rateDate.minusMonths(maxWindowMonths)
        val filter = "record_date:lte:$rateDate,record_date:gte:$minDate"
        val fields = "record_date,country,currency,country_currency_desc,exchange_rate"
        val sort = "-record_date"

        val response =
            treasuryFeignClient.fetchRates(
                fields = fields,
                filter = filter,
                sort = sort,
                pageSize = PAGE_SIZE,
            )

        val matched = response.data?.let { matchRate(targetCurrency, it) } ?: return null

        val rateValue = matched.exchangeRate?.let { BigDecimal(it) }
        val recordDate =
            matched.recordDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }

        return if (rateValue != null && recordDate != null) {
            ExchangeRate(
                rate = rateValue,
                sourceCurrency = sourceCurrency,
                targetCurrency = targetCurrency,
                retrievedAt = recordDate.atStartOfDay(DEFAULT_ZONE).toInstant(),
            )
        } else {
            null
        }
    }

    private fun matchRate(
        target: TargetCurrency,
        records: List<TreasuryRateRecord>,
    ): TreasuryRateRecord? {
        val isoCurrency = runCatching { Currency.getInstance(target.code) }.getOrNull() ?: return null
        val displayName = isoCurrency.getDisplayName(Locale.US).lowercase()

        return records.firstOrNull { record ->
            val desc = record.countryCurrencyDesc?.lowercase() ?: return@firstOrNull false
            val currencyPart = desc.substringAfter("-")
            displayName.contains(currencyPart) ||
                desc.contains(displayName.split(" ").lastOrNull() ?: "")
        }
    }

    companion object {
        private const val PAGE_SIZE = 10_000
        private const val DEFAULT_WINDOW_MONTHS = 6L
        private val DEFAULT_ZONE = java.time.ZoneOffset.UTC
    }
}
