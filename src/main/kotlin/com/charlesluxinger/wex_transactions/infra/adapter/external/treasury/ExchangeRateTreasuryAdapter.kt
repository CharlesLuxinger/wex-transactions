package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.Currency
import java.util.Locale.US

@Component
class ExchangeRateTreasuryAdapter(
    private val treasuryFeignClient: TreasuryFeignClient,
) : ExchangeRateClientPort {
    override fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate =
        fetchNearestPriorRate(from, to, LocalDate.now())
            ?: throw IllegalStateException("No exchange rate available for $from -> $to")

    override fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? {
        val minDate = rateDate.minusMonths(DEFAULT_WINDOW_MONTHS)

        return treasuryFeignClient.fetchRates(
            fields = FIELDS,
            filter = FILTER_FORMAT.format(rateDate, minDate),
            sort = SORT,
            pageSize = PAGE_SIZE,
        ).data?.let { data ->
            val matched = matchRate(targetCurrency, data) ?: return@let null
            if (!matched.hasValidExchangeRate || !matched.hasValidRecordDate) return@let null
            ExchangeRate(matched.rate, sourceCurrency, targetCurrency, matched.retrievedAt)
        }
    }

    private fun matchRate(
        target: TargetCurrency,
        records: List<TreasuryRateRecord>,
    ): TreasuryRateRecord? {
        val isoCurrency = runCatching { Currency.getInstance(target.code) }.getOrNull() ?: return null
        val displayName = isoCurrency.getDisplayName(US).lowercase()

        return records.firstOrNull { record ->
            if (!record.hasValidDescription) return@firstOrNull false
            val desc = record.countryCurrencyDesc.lowercase()
            val currencyPart = desc.substringAfter("-")
            displayName.contains(currencyPart) ||
                desc.contains(displayName.split(" ").lastOrNull() ?: "")
        }
    }

    companion object {
        private const val PAGE_SIZE = 10_000
        private const val DEFAULT_WINDOW_MONTHS = 6L
        private const val FIELDS = "record_date,country,currency,country_currency_desc,exchange_rate"
        private const val FILTER_FORMAT = "record_date:lte:%s,record_date:gte:%s"
        private const val SORT = "-record_date"
    }
}
