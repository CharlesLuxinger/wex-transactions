package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo as wireMockEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.Duration

class RetrieveConvertedControllerV1IntegrationTest :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanRedisAndWireMock() {
        val cacheKeys = redisTemplate.keys("exchangeRate:*")
        redisTemplate.delete(cacheKeys)
        redisTemplate.opsForStream<String, String>().trim("exchange-rate-fetched-events", 0)
        server.resetAll()
    }

    private fun awaitCache(
        key: String,
        timeout: Duration = Duration.ofSeconds(3),
    ): String? {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        var value: String?
        do {
            value = redisTemplate.opsForValue().get(key)
            if (value != null) return value
            Thread.sleep(100)
        } while (System.currentTimeMillis() < deadline)
        return null
    }

    @Test
    @DisplayName("Retrieve converted caches treasury rate in Redis")
    fun `retrieve converted caches treasury rate in redis`() {
        stubDefaultTreasuryRate("1.00")
        stubTreasuryRateForDate("2026-01-16", "5.10")

        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.10f))
            .body("convertedAmount", equalTo(510.00f))
            .body("targetCurrency", equalTo("BRL"))

        val cached = awaitCache("exchangeRate:USD:Brazil-Real:2026-01-16")
        assertThat(cached).isNotBlank()
    }

    @Test
    @DisplayName("Retrieve converted uses cache on second call")
    fun `retrieve converted uses cache on second call`() {
        stubDefaultTreasuryRate("1.00")
        stubTreasuryRateForDate("2026-01-16", "5.10")

        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.10f))

        awaitCache("exchangeRate:USD:Brazil-Real:2026-01-16")

        val treasuryCallsForDate =
            server.findAll(
                getRequestedFor(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                    .withQueryParam("filter", containing("record_date:lte:2026-01-16")),
            )
        assertThat(treasuryCallsForDate).hasSize(1)
    }

    @Test
    @DisplayName("Retrieve converted falls back to treasury when redis value is malformed")
    fun `retrieve converted falls back to treasury when redis value is malformed`() {
        stubDefaultTreasuryRate("1.00")
        stubTreasuryRateForDate("2026-01-16", "5.20")

        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        redisTemplate.opsForValue().set("exchangeRate:USD:Brazil-Real:2026-01-16", "{invalid-json")

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.20f))

        val treasuryCallsForDate =
            server.findAll(
                getRequestedFor(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                    .withQueryParam("filter", containing("record_date:lte:2026-01-16")),
            )
        assertThat(treasuryCallsForDate).hasSize(1)

        val cached = awaitCache("exchangeRate:USD:Brazil-Real:2026-01-16")
        val cacheValue = objectMapper.readTree(cached)
        assertThat(cacheValue.path("rate").asText()).isEqualTo("5.20")
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
    @DisplayName("Retrieve converted returns 422 when no rate is available")
    fun `retrieve converted returns 422 when no rate is available`() {
        stubDefaultTreasuryRate("1.00")
        stubTreasuryNoRateForDate("2026-01-16")

        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(422)
            .body("status", equalTo(422))
            .body("title", equalTo("Conversion Unavailable"))
            .body("detail", equalTo("Exchange rate unavailable: USD → Brazil-Real"))
            .body("type", equalTo("about:blank"))
    }

    @Test
    @DisplayName("Retrieve converted returns latest cached rate when treasury fails")
    fun `retrieve converted uses Fallback latest cached rate when treasury fails`() {
        stubDefaultTreasuryRate("1.00")
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        val staleCachedRate =
            """
            {
              "rate": "5.45",
              "sourceCurrency": "USD",
              "targetCurrency": "Brazil-Real",
              "retrievedAt": "2026-01-15T12:00:00Z"
            }
            """.trimIndent()
        redisTemplate.opsForValue().set("exchangeRate:USD:Brazil-Real:2026-01-10", staleCachedRate)

        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .atPriority(1)
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:2026-01-16"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(
                    aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"rate limit exceeded\"}"),
                ),
        )

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.45f))
            .body("convertedAmount", equalTo(545.00f))
    }

    @Test
    @DisplayName("Retrieve converted fallback uses latest cached key across dates")
    fun `retrieve converted Fallback uses latest cached key ignoring date`() {
        stubDefaultTreasuryRate("1.00")
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        val olderCache =
            """
            {
              "rate": "5.20",
              "sourceCurrency": "USD",
              "targetCurrency": "Brazil-Real",
              "retrievedAt": "2026-01-12T12:00:00Z"
            }
            """.trimIndent()
        val newerCache =
            """
            {
              "rate": "5.60",
              "sourceCurrency": "USD",
              "targetCurrency": "Brazil-Real",
              "retrievedAt": "2026-01-15T12:00:00Z"
            }
            """.trimIndent()
        redisTemplate.opsForValue().set("exchangeRate:USD:Brazil-Real:2026-01-10", olderCache)
        redisTemplate.opsForValue().set("exchangeRate:USD:Brazil-Real:2026-01-15", newerCache)

        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .atPriority(1)
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:2026-01-16"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"upstream down\"}"),
                ),
        )

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.60f))
            .body("convertedAmount", equalTo(560.00f))
    }

    @Test
    @DisplayName("Retrieve converted returns 400 when targetCurrency is blank")
    fun `retrieve converted returns 400 when target currency is blank`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=   ")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Target currency must not be blank"))
            .body("errors.field", hasItem("retrieveConverted.targetCurrency"))
            .body("errors.message", hasItem("Target currency must not be blank"))
    }

    @Test
    @DisplayName("Stored purchase conversion is rounded to 2 decimals")
    fun `stored purchase conversion is rounded to 2 decimals`() {
        stubDefaultTreasuryRate("1.00")
        stubTreasuryRateForDate("2026-01-16", "5.10")

        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z", BigDecimal("10.005"))

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("convertedAmount", equalTo(51.05f))
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

    private fun stubDefaultTreasuryRate(rate: String) {
        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .atPriority(10)
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(rateResponse(rate, "2026-01-15")),
        )
    }

    private fun stubTreasuryRateForDate(
        rateDate: String,
        rate: String,
    ) {
        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .atPriority(1)
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:$rateDate"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(rateResponse(rate, rateDate)),
        )
    }

    private fun stubTreasuryNoRateForDate(rateDate: String) {
        server.stubFor(
            get(urlPathEqualTo("/services/api/fiscal_service/v1/accounting/od/rates_of_exchange"))
                .atPriority(1)
                .withQueryParam(
                    "fields",
                    wireMockEqualTo("record_date,country,currency,country_currency_desc,exchange_rate"),
                ).withQueryParam("sort", wireMockEqualTo("-record_date"))
                .withQueryParam("filter", containing("record_date:lte:$rateDate"))
                .withQueryParam("page[size]", wireMockEqualTo("10000"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[]}"),
                ),
        )
    }

    private fun rateResponse(
        rate: String,
        recordDate: String,
    ) = aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody(
            """
            {
              "data": [
                {
                  "record_date": "$recordDate",
                  "country": "Brazil",
                  "currency": "Real",
                  "country_currency_desc": "Brazil-Real",
                  "exchange_rate": "$rate"
                }
              ]
            }
            """.trimIndent(),
        )

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
