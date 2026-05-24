package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TreasuryExchangeRateResponse(
    @JsonProperty("data")
    val data: List<TreasuryRateRecord>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TreasuryRateRecord(
    @JsonProperty("record_date")
    val recordDate: String? = null,
    @JsonProperty("country")
    val country: String? = null,
    @JsonProperty("currency")
    val currency: String? = null,
    @JsonProperty("country_currency_desc")
    val countryCurrencyDesc: String? = null,
    @JsonProperty("exchange_rate")
    val exchangeRate: String? = null,
    @JsonProperty("effective_date")
    val effectiveDate: String? = null,
)
