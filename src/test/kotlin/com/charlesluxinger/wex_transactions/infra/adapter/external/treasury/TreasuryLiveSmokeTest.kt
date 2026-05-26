package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate

@Tag("live")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_TREASURY_TESTS", matches = "true")
class TreasuryLiveSmokeTest {
    private val objectMapper = jacksonObjectMapper()
    private val httpClient = HttpClient.newHttpClient()

    @Test
    @DisplayName("Live treasury filter returns Brazil-Real and not Afghanistan-Afghani")
    fun `live treasury filter returns target currency`() {
        val rateDate = LocalDate.parse("2025-05-22")
        val minDate = rateDate.minusMonths(6)
        val filter =
            ExchangeRateTreasuryAdapter.buildRateFilter(
                rateDate,
                minDate,
                TargetCurrency("Brazil-Real"),
            )
        val uri =
            URI.create(
                "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange" +
                    "?fields=${ExchangeRateTreasuryAdapter.FIELDS}" +
                    "&filter=$filter" +
                    "&sort=${ExchangeRateTreasuryAdapter.SORT}" +
                    "&page%5Bsize%5D=5",
            )

        val response =
            httpClient.send(
                HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString(),
            )

        assertThat(response.statusCode()).isEqualTo(200)

        val data = objectMapper.readTree(response.body()).path("data")
        assertThat(data.isArray).isTrue()
        assertThat(data.size()).isGreaterThan(0)
        val firstDescriptor = data.first().path("country_currency_desc").asText()
        assertThat(firstDescriptor).isEqualToIgnoringCase("Brazil-Real")
        assertThat(firstDescriptor).isNotEqualToIgnoringCase("Afghanistan-Afghani")
    }
}
