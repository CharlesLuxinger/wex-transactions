package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.redis.core.StringRedisTemplate
import java.math.BigDecimal
import java.time.Instant

class RetrieveConvertedControllerV1IntegrationTest :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var exchangeRateClientPort: ExchangeRateClientPort

    @BeforeEach
    fun cleanRedisAndMocks() {
        redisTemplate.connectionFactory
            ?.connection
            ?.serverCommands()
            ?.flushAll()
        reset(exchangeRateClientPort)
    }

    @Test
    @DisplayName("Retrieve converted caches treasury rate in Redis")
    fun `retrieve converted caches treasury rate in redis`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                java.time.LocalDate.parse("2026-01-16"),
            ),
        ).thenReturn(sampleRate("5.10"))

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.10f))
            .body("convertedAmount", equalTo(510.00f))
            .body("targetCurrency", equalTo("BRL"))

        val cached = redisTemplate.opsForValue().get("exchangeRate:USD:BRL")
        assertThat(cached).isNotBlank()
    }

    @Test
    @DisplayName("Retrieve converted uses cache on second call")
    fun `retrieve converted uses cache on second call`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                java.time.LocalDate.parse("2026-01-16"),
            ),
        ).thenReturn(sampleRate("5.10"))

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

        verify(exchangeRateClientPort, times(1)).fetchNearestPriorRate(
            TargetCurrency("USD"),
            TargetCurrency("BRL"),
            java.time.LocalDate.parse("2026-01-16"),
        )
    }

    @Test
    @DisplayName("Retrieve converted falls back to treasury when redis value is malformed")
    fun `retrieve converted falls back to treasury when redis value is malformed`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z")
        redisTemplate.opsForValue().set("exchangeRate:USD:BRL", "{invalid-json")

        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                java.time.LocalDate.parse("2026-01-16"),
            ),
        ).thenReturn(sampleRate("5.20"))

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("exchangeRateUsed", equalTo(5.20f))

        verify(exchangeRateClientPort, times(1)).fetchNearestPriorRate(
            TargetCurrency("USD"),
            TargetCurrency("BRL"),
            java.time.LocalDate.parse("2026-01-16"),
        )

        val cached = redisTemplate.opsForValue().get("exchangeRate:USD:BRL")
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
    @DisplayName("Stored purchase conversion is rounded to 2 decimals")
    fun `stored purchase conversion is rounded to 2 decimals`() {
        val purchaseId = createPurchase("BRL", "2026-01-16T10:00:00Z", BigDecimal("10.005"))
        `when`(
            exchangeRateClientPort.fetchNearestPriorRate(
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                java.time.LocalDate.parse("2026-01-16"),
            ),
        ).thenReturn(sampleRate("5.10"))

        givenJson()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("convertedAmount", equalTo(51.03f))
    }

    private fun createPurchase(
        targetCurrency: String,
        transactionDate: String,
        amount: BigDecimal = BigDecimal("100.00"),
    ): Long {
        `when`(
            exchangeRateClientPort.fetchRate(
                TargetCurrency("USD"),
                TargetCurrency(targetCurrency),
            ),
        ).thenReturn(
            ExchangeRate(
                rate = BigDecimal.ONE,
                sourceCurrency = TargetCurrency("USD"),
                targetCurrency = TargetCurrency(targetCurrency),
                retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
            ),
        )

        return givenJson()
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
    }

    private fun sampleRate(rate: String): ExchangeRate =
        ExchangeRate(
            rate = BigDecimal(rate),
            sourceCurrency = TargetCurrency("USD"),
            targetCurrency = TargetCurrency("BRL"),
            retrievedAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
}
