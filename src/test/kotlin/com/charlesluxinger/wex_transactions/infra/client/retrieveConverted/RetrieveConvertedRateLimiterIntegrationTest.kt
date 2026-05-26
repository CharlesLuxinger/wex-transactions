package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo as wireMockEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class RetrieveConvertedRateLimiterIntegrationTest :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Test
    @DisplayName("retrieve converted returns 429 after treasury-api exceeds 10 calls")
    fun `retrieve converted returns 429 when treasury rate limiter is exceeded`() {
        stubTreasuryRate("5.10")
        val purchaseId = createPurchase("2026-01-16T10:00:00Z")

        Thread.sleep(1_100)

        repeat(10) {
            redisTemplate.delete(CACHE_KEY)
            givenJson()
                .accept(ContentType.JSON)
                .`when`()
                .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
                .then()
                .statusCode(200)
                .body("exchangeRateUsed", equalTo(5.10f))
        }

        redisTemplate.delete(CACHE_KEY)
        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(429)
            .body("title", equalTo("Too Many Requests"))
    }

    private fun createPurchase(transactionDate: String): Long =
        givenJson()
            .body(
                mapOf(
                    "description" to "Rate limiter purchase",
                    "transactionAmount" to "100.00",
                    "transactionCurrency" to "USD",
                    "transactionDate" to transactionDate,
                    "targetCurrency" to "BRL",
                ),
            ).`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(201)
            .extract()
            .path<Int>("id")
            .toLong()

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
                                  "record_date": "2026-01-16",
                                  "country": "Brazil",
                                  "currency": "Real",
                                  "country_currency_desc": "BRL",
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
        private const val CACHE_KEY = "exchangeRate:USD:BRL:2026-01-16"

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
            registry.add("resilience4j.ratelimiter.instances.treasury-api.limit-for-period") { 10 }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.limit-refresh-period") { "1s" }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.timeout-duration") { "0" }
        }
    }
}
