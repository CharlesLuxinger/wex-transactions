package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo as wireMockEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo as hamcrestEqualTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@ActiveProfiles("test")
class PurchaseControllerV1Test : AbstractRestApiIntegrationTest() {
    @Test
    @DisplayName("Should successfully store a valid purchase transaction")
    fun `should store purchase successfully`() {
        stubTreasuryRate("1.00")

        val payload =
            mapOf(
                "description" to "Lunch at Restaurant",
                "transactionAmount" to 15.50,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("description", hamcrestEqualTo("Lunch at Restaurant"))
            .body("transactionAmount", hamcrestEqualTo(15.50f))
            .body("transactionCurrency", hamcrestEqualTo("United-States-Dollar"))
            .body("transactionDate", hamcrestEqualTo("2026-05-23T12:00:00Z"))
    }

    @Test
    @DisplayName("Should create two distinct purchases for duplicate POST body")
    fun `should create distinct purchases for duplicate post body`() {
        stubTreasuryRate("1.00")

        val payload =
            mapOf(
                "description" to "Lunch at Restaurant",
                "transactionAmount" to 15.50,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        val firstId =
            givenJson()
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("description", hamcrestEqualTo("Lunch at Restaurant"))
                .body("transactionAmount", hamcrestEqualTo(15.50f))
                .body("transactionCurrency", hamcrestEqualTo("United-States-Dollar"))
                .body("transactionDate", hamcrestEqualTo("2026-05-23T12:00:00Z"))
                .extract()
                .path<Int>("id")
                .toLong()

        val secondId =
            givenJson()
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("description", hamcrestEqualTo("Lunch at Restaurant"))
                .body("transactionAmount", hamcrestEqualTo(15.50f))
                .body("transactionCurrency", hamcrestEqualTo("United-States-Dollar"))
                .body("transactionDate", hamcrestEqualTo("2026-05-23T12:00:00Z"))
                .extract()
                .path<Int>("id")
                .toLong()

        assertThat(firstId).isNotNull()
        assertThat(secondId).isNotNull()
        assertThat(secondId).isNotEqualTo(firstId)
    }

    @Test
    @DisplayName("Should fail when description is blank")
    fun `should fail when description is blank`() {
        val payload =
            mapOf(
                "description" to "   ",
                "transactionAmount" to 15.50,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", hamcrestEqualTo("Bad Request"))
            .body("detail", hamcrestEqualTo("Description must not be blank"))
            .body("errors.message", hasItem("Description must not be blank"))
    }

    @Test
    @DisplayName("Should fail when description exceeds 50 characters")
    fun `should fail when description exceeds 50 characters`() {
        val payload =
            mapOf(
                "description" to "A".repeat(51),
                "transactionAmount" to 15.50,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", hamcrestEqualTo("Bad Request"))
            .body("detail", hamcrestEqualTo("Description must have at most 50 characters"))
            .body("errors.message", hasItem("Description must have at most 50 characters"))
    }

    @Test
    @DisplayName("Should fail when transaction amount is non-positive")
    fun `should fail when transaction amount is non-positive`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to -1.00,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", hamcrestEqualTo("Bad Request"))
            .body("detail", hamcrestEqualTo("Transaction amount must be positive"))
            .body("errors.message", hasItem("Transaction amount must be positive"))
    }

    @Test
    @DisplayName("Should fail when transaction date format is invalid")
    fun `should fail when transaction date format is invalid`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to 10.00,
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "invalid-date",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", hamcrestEqualTo("Bad Request"))
            .body("detail", hamcrestEqualTo("Invalid ISO-8601 transaction date format"))
    }

    @Test
    @DisplayName("Should fail when transaction currency is not United-States-Dollar")
    fun `should fail when transaction currency is not United-States-Dollar`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to 10.00,
                "transactionCurrency" to "Brazil-Real",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", hamcrestEqualTo("Bad Request"))
            .body("detail", hamcrestEqualTo("Transaction currency must be United-States-Dollar"))
            .body("errors.message", hasItem("Transaction currency must be United-States-Dollar"))
    }

    private fun stubTreasuryRate(rate: String) {
        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "data": [
                                {
                                  "record_date": "2026-05-23",
                                  "country": "Euro Area",
                                  "currency": "Euro",
                                  "country_currency_desc": "Euro Area-Euro",
                                  "exchange_rate": "$rate"
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                ),
        )
    }

    companion object {
        private val server = WireMockServer(0)

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            server.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            server.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("treasury.api.base-url") {
                "${server.baseUrl()}/services/api/fiscal_service/v1/accounting/od"
            }
        }
    }
}
