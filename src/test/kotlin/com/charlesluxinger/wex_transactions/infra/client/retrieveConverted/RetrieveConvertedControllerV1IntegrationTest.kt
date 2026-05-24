package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.RestAssuredRequestSupport
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.TreasuryFeignClient
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.TreasuryExchangeRateResponse
import com.charlesluxinger.wex_transactions.infra.adapter.external.treasury.TreasuryRateRecord
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaRepository
import feign.FeignException
import feign.Request
import feign.Response
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDate

/**
 * Integration test WITHOUT StubExchangeRateClientConfig.
 * Uses @MockBean on TreasuryFeignClient to mock the HTTP layer
 * while keeping the real ExchangeRateTreasuryAdapter wired by Spring.
 *
 * Note: StorePurchaseUseCaseImpl calls fetchRate() during purchase creation,
 * so the mock MUST return a valid rate for createPurchase() to succeed.
 */
@ActiveProfiles("test")
class RetrieveConvertedControllerV1IntegrationTest :
    AbstractRestApiIntegrationTest(),
    RestAssuredRequestSupport {
    @MockBean
    private lateinit var treasuryFeignClient: TreasuryFeignClient

    @Autowired
    private lateinit var exchangeRateJpaRepository: ExchangeRateJpaRepository

    @BeforeEach
    fun cleanDatabase() {
        exchangeRateJpaRepository.deleteAll()
    }

    @Test
    @DisplayName("Persisted rate hit returns 200 without Treasury call")
    fun `persisted rate hit returns 200`() {
        val rateValue = "5.25"

        // createPurchase() will call fetchRate() — stub a valid rate so POST succeeds
        `when`(
            treasuryFeignClient.fetchRates(
                fields = anyString(),
                filter = anyString(),
                sort = anyString(),
                pageSize = anyInt(),
            ),
        ).thenReturn(
            TreasuryExchangeRateResponse(
                data =
                    listOf(
                        TreasuryRateRecord(
                            recordDate = "2026-05-23",
                            country = "Brazil",
                            currency = "Real",
                            countryCurrencyDesc = "Brazil-Real",
                            exchangeRate = rateValue,
                        ),
                    ),
            ),
        )

        val purchaseId = createPurchase()
        val rateDate = LocalDate.parse("2026-05-23")

        // Persist a rate in DB — the GET path should find this instead of calling the client
        exchangeRateJpaRepository.save(
            ExchangeRateJpaEntity(
                rateDate = rateDate,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal(rateValue),
                createdAt = Instant.now(),
            ),
        )

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("purchaseId", equalTo(purchaseId.toInt()))
            .body("targetCurrency", equalTo("BRL"))
            .body("exchangeRateUsed", equalTo(5.25f))
            .body("convertedAmount", equalTo(81.38f))
    }

    @Test
    @DisplayName("Treasury fallback success caches the rate and returns 200")
    fun `treasury fallback caches rate and returns 200`() {
        val treasuryResponse =
            TreasuryExchangeRateResponse(
                data =
                    listOf(
                        TreasuryRateRecord(
                            recordDate = "2026-05-20",
                            country = "Brazil",
                            currency = "Real",
                            countryCurrencyDesc = "Brazil-Real",
                            exchangeRate = "5.75",
                        ),
                    ),
            )

        // Stub used for both createPurchase() and the GET fallback path
        `when`(
            treasuryFeignClient.fetchRates(
                fields = anyString(),
                filter = anyString(),
                sort = anyString(),
                pageSize = anyInt(),
            ),
        ).thenReturn(treasuryResponse)

        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=BRL")
            .then()
            .statusCode(200)
            .body("purchaseId", equalTo(purchaseId.toInt()))
            .body("targetCurrency", equalTo("BRL"))
            .body("exchangeRateUsed", equalTo(5.75f))
            .body("convertedAmount", equalTo(89.13f))

        val saved = exchangeRateJpaRepository.findAll()
        assertThat(saved).isNotEmpty
    }

    @Test
    @DisplayName("Treasury fallback with empty data returns 422")
    fun `treasury fallback empty returns 422`() {
        // First call (createPurchase): return a valid rate so POST succeeds
        // Subsequent calls (GET fallback): return empty data so 422 is returned
        `when`(
            treasuryFeignClient.fetchRates(
                fields = anyString(),
                filter = anyString(),
                sort = anyString(),
                pageSize = anyInt(),
            ),
        ).thenReturn(
            TreasuryExchangeRateResponse(
                data =
                    listOf(
                        TreasuryRateRecord(
                            recordDate = "2026-05-20",
                            country = "Brazil",
                            currency = "Real",
                            countryCurrencyDesc = "Brazil-Real",
                            exchangeRate = "5.00",
                        ),
                    ),
            ),
        ).thenReturn(
            TreasuryExchangeRateResponse(data = null),
        )

        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=EUR")
            .then()
            .statusCode(422)
            .body("title", equalTo("Conversion Unavailable"))
            .body("detail", equalTo("Exchange rate unavailable: USD \u2192 EUR"))
    }

    @Test
    @DisplayName("Transient Treasury failure returns 500 not 422")
    fun `transient treasury failure returns 500`() {
        // First call (createPurchase): return a valid rate so POST succeeds
        `when`(
            treasuryFeignClient.fetchRates(
                fields = anyString(),
                filter = anyString(),
                sort = anyString(),
                pageSize = anyInt(),
            ),
        ).thenReturn(
            TreasuryExchangeRateResponse(
                data =
                    listOf(
                        TreasuryRateRecord(
                            recordDate = "2026-05-20",
                            country = "Brazil",
                            currency = "Real",
                            countryCurrencyDesc = "Brazil-Real",
                            exchangeRate = "5.00",
                        ),
                    ),
            ),
        ).thenThrow(
            FeignException.errorStatus(
                "fetchRates",
                Response
                    .builder()
                    .status(500)
                    .reason("Internal Server Error")
                    .request(
                        Request.create(
                            Request.HttpMethod.GET,
                            "http://test",
                            emptyMap(),
                            null,
                            Charset.defaultCharset(),
                            null,
                        ),
                    ).headers(emptyMap())
                    .body("{}", Charset.defaultCharset())
                    .build(),
            ),
        )

        val purchaseId = createPurchase()

        given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/v1/purchases/$purchaseId/converted?targetCurrency=EUR")
            .then()
            .statusCode(500)
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
