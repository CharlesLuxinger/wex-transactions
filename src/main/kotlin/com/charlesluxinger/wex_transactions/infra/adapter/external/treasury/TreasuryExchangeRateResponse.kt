package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

@JsonIgnoreProperties(ignoreUnknown = true)
data class TreasuryExchangeRateResponse(
    @JsonProperty("data")
    val data: List<TreasuryRateRecord>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TreasuryRateRecord(
    @JsonProperty("record_date")
    val recordDate: String = "",
    @JsonProperty("country")
    val country: String = "",
    @JsonProperty("currency")
    val currency: String = "",
    @JsonProperty("country_currency_desc")
    val countryCurrencyDesc: String = "",
    @JsonProperty("exchange_rate")
    val exchangeRate: String = "",
    @JsonProperty("effective_date")
    val effectiveDate: String? = null,
) {
    val hasValidExchangeRate: Boolean
    val hasValidRecordDate: Boolean
    val hasValidDescription: Boolean

    init {
        fun isAvailable(value: String) = value != "null" && value.isNotBlank()
        hasValidExchangeRate = isAvailable(exchangeRate)
        hasValidRecordDate = isAvailable(recordDate)
        hasValidDescription = isAvailable(countryCurrencyDesc)
    }

    val rate: BigDecimal get() = exchangeRate.toBigDecimal()
    val parsedRecordDate: LocalDate get() = LocalDate.parse(recordDate, ISO_LOCAL_DATE)
    val retrievedAt: Instant get() = parsedRecordDate.atStartOfDay(UTC).toInstant()
}
