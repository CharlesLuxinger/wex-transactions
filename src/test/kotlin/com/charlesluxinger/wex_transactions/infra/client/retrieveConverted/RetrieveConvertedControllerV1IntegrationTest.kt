package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaRepository
import io.restassured.http.ContentType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class RetrieveConvertedControllerV1IntegrationTest :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @Autowired
    private lateinit var exchangeRateJpaRepository: ExchangeRateJpaRepository

    @Test
    @DisplayName("Retrieve converted uses persisted treasury rate without mock clients")
    fun `retrieve converted uses persisted treasury rate without mock clients`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        exchangeRateJpaRepository.save(
            ExchangeRateJpaEntity(
                rateDate = LocalDate.parse("2026-01-16"),
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.10"),
                createdAt = Instant.parse("2026-01-16T12:00:00Z"),
            ),
        )

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("purchaseId", equalTo(purchaseId.toInt()))
            .body("exchangeRateUsed", equalTo(5.10f))
            .body("convertedAmount", equalTo(510.00f))
            .body("targetCurrency", equalTo("BRL"))
    }

    @Test
    @DisplayName("Retrieve converted returns 422 when no eligible treasury rate exists")
    fun `retrieve converted returns 422 when no eligible treasury rate exists`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        exchangeRateJpaRepository.deleteAll()

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=EUR")
            .then()
            .statusCode(422)
            .body("title", equalTo("Conversion Unavailable"))
            .body("status", equalTo(422))
            .body("detail", equalTo("Exchange rate unavailable: USD → EUR"))
    }

    @Test
    @DisplayName("Fallback persisted rate is tagged as treasury source")
    fun `fallback persisted rate is tagged as treasury source`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        exchangeRateJpaRepository.deleteAll()

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)

        val persistedRates = exchangeRateJpaRepository.findAll()
        assertThat(persistedRates).hasSize(1)
        assertThat(persistedRates.first().rateSource).isEqualTo(ExchangeRateJpaEntity.TREASURY_SOURCE)
    }

    @Test
    @DisplayName("Retrieve converted returns 404 for missing purchase")
    fun `retrieve converted returns 404 for missing purchase`() {
        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/999999/converted?targetCurrency=BRL")
            .then()
            .statusCode(404)
            .body("title", equalTo("Not Found"))
            .body("detail", equalTo("Purchase with ID 999999 not found"))
    }

    @Test
    @DisplayName("Stored purchase conversion is rounded to 2 decimals")
    fun `stored purchase conversion is rounded to 2 decimals`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z", BigDecimal("10.005"))

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("convertedAmount", equalTo(51.03f))
            .body("exchangeRateUsed", notNullValue())
    }

    private fun createPurchase(
        targetCurrency: String,
        transactionDate: String,
        amount: BigDecimal = BigDecimal("100.00"),
    ): Long =
        givenJson()
            .body(
                mapOf(
                    "description" to "Lunch at Restaurant",
                    "transactionAmount" to amount,
                    "transactionCurrency" to "USD",
                    "transactionDate" to transactionDate,
                    "targetCurrency" to targetCurrency,
                ),
            ).`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(201)
            .extract()
            .path<Int>("id")
            .toLong()

    companion object {
        private val server = MockWebServer()

        @JvmStatic
        @BeforeAll
        fun startServer() {
            server.dispatcher = FixedTreasuryDispatcher()
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

    private class FixedTreasuryDispatcher : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val isRatesPath = request.path.orEmpty().contains("/rates_of_exchange")
            val filter = request.requestUrl?.queryParameter("filter").orEmpty()
            val eurRequested = filter.contains("EUR", ignoreCase = true)

            val responseBody =
                if (!isRatesPath) {
                    null
                } else if (eurRequested) {
                    "{\"data\":[]}"
                } else {
                    """
                    {
                      "data": [
                        {
                          "record_date": "2026-01-15",
                          "country": "Brazil",
                          "currency": "Real",
                          "country_currency_desc": "Brazil-Real",
                          "exchange_rate": "5.10"
                        }
                      ]
                    }
                    """.trimIndent()
                }

            val status = if (isRatesPath) 200 else 404
            val response = MockResponse().setResponseCode(status)
            if (responseBody != null) {
                response.addHeader("Content-Type", "application/json")
                response.setBody(responseBody)
            }
            return response
        }
    }
}
