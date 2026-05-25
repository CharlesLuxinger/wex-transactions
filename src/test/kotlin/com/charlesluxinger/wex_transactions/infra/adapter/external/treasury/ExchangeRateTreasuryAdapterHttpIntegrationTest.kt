package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import feign.FeignException
import feign.RetryableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
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

    @Test
    @DisplayName("Adapter fetches treasury rate via real HTTP client")
    fun `adapter fetches treasury rate via http`() {
        repeat(3) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(
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
                    ).addHeader("Content-Type", "application/json"),
            )
        }

        val result =
            adapter.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                LocalDate.parse("2026-05-23"),
            )

        assertThat(result).isNotNull

        val request = server.takeRequest()
        assertThat(request.path).contains("/rates_of_exchange")
        assertThat(request.path).contains("fields=")
        assertThat(request.path).contains("filter=")
        assertThat(request.path).contains("sort=")
    }

    @Test
    @DisplayName("Adapter returns null when treasury payload has no data")
    fun `adapter returns null when treasury payload has no data`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"data":null}""")
                .addHeader("Content-Type", "application/json"),
        )

        val result =
            adapter.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
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
        val beforeRequests = server.requestCount
        enqueueFailureResponses(status)

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
        val actualRequests = server.requestCount - beforeRequests
        assertThat(actualRequests).isEqualTo(expectedRequests)
    }

    @ParameterizedTest(name = "Adapter maps status {0} to RateUnavailableException")
    @MethodSource("treasuryFailureStatuses")
    fun `adapter converts treasury failures to rate unavailable`(status: Int) {
        enqueueFailureResponses(status)

        val thrown =
            assertThrows<RateUnavailableException> {
                adapter.fetchNearestPriorRate(
                    TargetCurrency("USD"),
                    TargetCurrency("BRL"),
                    LocalDate.parse("2026-05-23"),
                )
            }

        assertThat(thrown.from).isEqualTo("USD")
        assertThat(thrown.to).isEqualTo("BRL")
    }

    private fun enqueueFailureResponses(status: Int) {
        val attempts = if (status in RETRYABLE_STATUSES) 3 else 1
        repeat(attempts) {
            server.enqueue(
                MockResponse()
                    .setResponseCode(status)
                    .setBody("""{"error":"failure"}""")
                    .addHeader("Content-Type", "application/json"),
            )
        }
    }

    companion object {
        private val server = MockWebServer()
        private val RETRYABLE_STATUSES = setOf(429, 503, 504)
        private const val FIELDS = "record_date,country,currency,country_currency_desc,exchange_rate"
        private const val FILTER = "record_date:lte:2026-05-23,record_date:gte:2025-11-23"
        private const val SORT = "-record_date"
        private const val PAGE_SIZE = 10_000

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
            server.shutdown()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("treasury.api.base-url") {
                server
                    .url("/services/api/fiscal_service/v1/accounting/od")
                    .toString()
                    .removeSuffix("/")
            }
        }
    }
}
