package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaRepository
import com.charlesluxinger.wex_transactions.infra.client.purchase.StubExchangeRateClientConfig
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant.now
import java.time.LocalDate

@ActiveProfiles("test")
@Import(StubExchangeRateClientConfig::class)
class RetrieveConvertedControllerV1Test :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @Autowired
    private lateinit var exchangeRateJpaRepository: ExchangeRateJpaRepository

    @BeforeEach
    fun seedExchangeRate() {
        exchangeRateJpaRepository.save(
            ExchangeRateJpaEntity(
                rateDate = LocalDate.parse("2026-05-23"),
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.25"),
                createdAt = now(),
            ),
        )
    }

    @Test
    @DisplayName("Should retrieve converted purchase for valid purchaseId and targetCurrency")
    fun `should retrieve converted purchase successfully`() {
        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("purchaseId", equalTo(purchaseId.toInt()))
            .body("description", equalTo("Lunch at Restaurant"))
            .body("targetCurrency", equalTo("BRL"))
            .body("originalUsdAmount", equalTo(15.50f))
            .body("exchangeRateUsed", equalTo(5.25f))
            .body("convertedAmount", equalTo(81.38f))
            .body("transactionDate", notNullValue())
            .body("createdAt", notNullValue())
    }

    @Test
    @DisplayName("Should return 404 when purchase is not found")
    fun `should return 404 when purchase not found`() {
        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/99999/converted?targetCurrency=BRL")
            .then()
            .statusCode(404)
            .body("title", equalTo("Not Found"))
            .body("detail", equalTo("Purchase with ID 99999 not found"))
    }

    @Test
    @DisplayName("Should return 422 when exchange rate is unavailable")
    fun `should return 422 when exchange rate unavailable`() {
        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=EUR")
            .then()
            .statusCode(422)
            .body("title", equalTo("Conversion Unavailable"))
            .body("detail", equalTo("Exchange rate unavailable: USD → EUR"))
    }

    @Test
    @DisplayName("Should return 400 when target currency is invalid")
    fun `should return 400 when target currency is invalid`() {
        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=INVALID")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Currency"))
            .body("detail", equalTo("Invalid currency code: INVALID"))
    }

    private fun createPurchase(): Long {
        val createResponse =
            givenJson()
                .body(
                    mapOf(
                        "description" to "Lunch at Restaurant",
                        "transactionAmount" to 15.50,
                        "transactionCurrency" to "USD",
                        "transactionDate" to "2026-05-23T12:00:00Z",
                        "targetCurrency" to "BRL",
                    ),
                ).`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .extract()
                .path<Int>("id")

        return createResponse.toLong()
    }
}
