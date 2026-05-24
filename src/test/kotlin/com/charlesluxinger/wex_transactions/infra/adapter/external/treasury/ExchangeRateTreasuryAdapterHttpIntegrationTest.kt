package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalDate

class ExchangeRateTreasuryAdapterHttpIntegrationTest : AbstractRestApiIntegrationTest() {
    @Autowired
    private lateinit var adapter: ExchangeRateTreasuryAdapter

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

    companion object {
        private val server = MockWebServer()

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
