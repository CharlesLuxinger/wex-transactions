package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.ExchangeRateTreasuryAdapter.Companion.FIELDS
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.ExchangeRateTreasuryAdapter.Companion.PAGE_SIZE
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.ExchangeRateTreasuryAdapter.Companion.SORT
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo as wireMockEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import feign.FeignException
import feign.RetryableException
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalDate
import java.util.stream.Stream

class ExchangeRateTreasuryAdapterHttpIntegrationTest : AbstractRestApiIntegrationTest() {
    @Autowired
    private lateinit var adapter: ExchangeRateTreasuryAdapter

    @Autowired
    private lateinit var treasuryFeignClient: TreasuryFeignClient

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @BeforeEach
    fun resetWireMockAndCircuitBreaker() {
        server.resetAll()
        circuitBreakerRegistry.circuitBreaker("treasury-rates").reset()
    }

    @Test
    @DisplayName("Adapter fetches treasury rate via real HTTP client")
    fun `adapter fetches treasury rate via http`() {
        stubSuccessRate("5.75")

        val result =
            adapter.fetchNearestPriorRate(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                LocalDate.parse("2026-05-23"),
            )

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo("5.75")

        server.verify(
            getRequestedFor(urlPathEqualTo(TREASURY_PATH))
                .withQueryParam("fields", wireMockEqualTo(FIELDS))
                .withQueryParam("filter", containing("country_currency_desc:eq:Brazil-Real"))
                .withQueryParam("sort", wireMockEqualTo(SORT)),
        )
    }

    @Test
    @DisplayName("Adapter returns null when treasury payload has no data")
    fun `adapter returns null when treasury payload has no data`() {
        stubEmptyRateWindow()
        stubDescriptorExists()

        val result =
            adapter.fetchNearestPriorRate(
                TargetCurrency("United-States-Dollar"),
                TargetCurrency("Brazil-Real"),
                LocalDate.parse("2026-05-23"),
            )

        assertThat(result).isNull()
    }

    @ParameterizedTest(name = "Feign maps status {0} to {1}")
    @MethodSource("feignFailureMatrix")
    fun `feign maps treasury failure matrix`(
        status: Int,
        expectedException: Class<out Throwable>,
    ) {
        stubFailureResponses(status)

        val thrown =
            Assertions.assertThrows(expectedException) {
                treasuryFeignClient.fetchRates(
                    fields = FIELDS,
                    filter = FILTER,
                    sort = SORT,
                    pageSize = PAGE_SIZE,
                )
            }

        assertThat(thrown).isInstanceOf(expectedException)

        val expectedRequests = if (status in RETRYABLE_STATUSES) 3 else 1
        server.verify(expectedRequests, getRequestedFor(urlPathEqualTo(TREASURY_PATH)))
    }

    @ParameterizedTest(name = "Adapter maps status {0} to RateUnavailableException")
    @MethodSource("treasuryFailureStatuses")
    fun `adapter converts treasury failures to rate unavailable`(status: Int) {
        stubFailureResponses(status)

        val thrown =
            assertThrows<RateUnavailableException> {
                adapter.fetchNearestPriorRate(
                    TargetCurrency("United-States-Dollar"),
                    TargetCurrency("Brazil-Real"),
                    LocalDate.parse("2026-05-23"),
                )
            }

        assertThat(thrown.from).isEqualTo("United-States-Dollar")
        assertThat(thrown.to).isEqualTo("Brazil-Real")
    }

    private fun stubSuccessRate(rate: String) {
        server.stubFor(
            get(urlPathEqualTo(TREASURY_PATH))
                .withQueryParam("filter", containing("country_currency_desc:eq:Brazil-Real"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "data": [
                                {
                                  "record_date": "2026-05-20",
                                  "country": "Brazil",
                                  "currency": "Real",
                                  "country_currency_desc": "Brazil-Real",
                                  "exchange_rate": "$rate"
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )
    }

    private fun stubEmptyRateWindow() {
        server.stubFor(
            get(urlPathEqualTo(TREASURY_PATH))
                .withQueryParam("filter", containing("record_date:lte:2026-05-23"))
                .withQueryParam("filter", containing("country_currency_desc:eq:Brazil-Real"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"data":[]}"""),
                ),
        )
    }

    private fun stubDescriptorExists() {
        server.stubFor(
            get(urlPathEqualTo(TREASURY_PATH))
                .withQueryParam("filter", wireMockEqualTo("country_currency_desc:eq:Brazil-Real"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "data": [
                                {
                                  "record_date": "2026-05-20",
                                  "country": "Brazil",
                                  "currency": "Real",
                                  "country_currency_desc": "Brazil-Real",
                                  "exchange_rate": "5.75"
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )
    }

    private fun stubFailureResponses(status: Int) {
        val attempts = if (status in RETRYABLE_STATUSES) 3 else 1
        server.stubFor(
            get(urlPathEqualTo(TREASURY_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error":"failure"}"""),
                ),
        )
        repeat(attempts - 1) {
            server.stubFor(
                get(urlPathEqualTo(TREASURY_PATH))
                    .willReturn(
                        aResponse()
                            .withStatus(status)
                            .withHeader("Content-Type", "application/json")
                            .withBody("""{"error":"failure"}"""),
                    ),
            )
        }
    }

    companion object {
        private val server = WireMockServer(0)
        private const val TREASURY_PATH = "/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"
        private const val FILTER =
            "record_date:lte:2026-05-23,record_date:gte:2025-11-23,country_currency_desc:eq:Brazil-Real"
        private val RETRYABLE_STATUSES = setOf(429, 503, 504)

        @JvmStatic
        fun feignFailureMatrix(): Stream<Arguments> =
            Stream.of(
                Arguments.of(429, RetryableException::class.java),
                Arguments.of(503, RetryableException::class.java),
                Arguments.of(504, RetryableException::class.java),
                Arguments.of(500, FeignException::class.java),
            )

        @JvmStatic
        fun treasuryFailureStatuses(): Stream<Int> = Stream.of(429, 503, 504, 500)

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            server.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("treasury.api.base-url") {
                "${server.baseUrl()}/services/api/fiscal_service/v1/accounting/od"
            }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.limit-for-period") { "100000" }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.timeout-duration") { "0s" }
        }
    }
}
