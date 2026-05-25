package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Instant

@ActiveProfiles("test")
@Import(StubExchangeRateClientConfig::class)
class PurchaseControllerV1Test : AbstractRestApiIntegrationTest() {
    @Test
    @DisplayName("Should successfully store a valid purchase transaction")
    fun `should store purchase successfully`() {
        val payload =
            mapOf(
                "description" to "Lunch at Restaurant",
                "transactionAmount" to 15.50,
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "EUR",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(201) // HTTP 201 Created
            .body("id", notNullValue())
            .body("description", equalTo("Lunch at Restaurant"))
            .body("transactionAmount", equalTo(15.50f))
            .body("transactionCurrency", equalTo("USD"))
            .body("transactionDate", equalTo("2026-05-23T12:00:00Z"))
            .body("targetCurrency", equalTo("EUR"))
            .body("exchangeRate", equalTo(1.0f))
            .body("convertedAmount", equalTo(15.50f))
    }

    @Test
    @DisplayName("Should fail when description is blank")
    fun `should fail when description is blank`() {
        val payload =
            mapOf(
                "description" to "   ",
                "transactionAmount" to 15.50,
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "EUR",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Description must not be blank"))
    }

    @Test
    @DisplayName("Should fail when description exceeds 50 characters")
    fun `should fail when description exceeds 50 characters`() {
        val payload =
            mapOf(
                "description" to "A".repeat(51),
                "transactionAmount" to 15.50,
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "EUR",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Description must have at most 50 characters"))
    }

    @Test
    @DisplayName("Should fail when transaction amount is non-positive")
    fun `should fail when transaction amount is non-positive`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to -1.00,
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "EUR",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Transaction amount must be positive"))
    }

    @Test
    @DisplayName("Should fail when transaction date format is invalid")
    fun `should fail when transaction date format is invalid`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to 10.00,
                "transactionCurrency" to "USD",
                "transactionDate" to "invalid-date",
                "targetCurrency" to "EUR",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Invalid ISO-8601 transaction date: invalid-date"))
    }

    @Test
    @DisplayName("Should fail when currency is invalid")
    fun `should fail when currency is invalid`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to 10.00,
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "INVALID",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Invalid Currency"))
            .body("detail", equalTo("Invalid currency code"))
    }

    @Test
    @DisplayName("Should fail when transaction currency is not USD")
    fun `should fail when transaction currency is not USD`() {
        val payload =
            mapOf(
                "description" to "Book",
                "transactionAmount" to 10.00,
                "transactionCurrency" to "EUR",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "BRL",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("title", equalTo("Bad Request"))
            .body("detail", equalTo("Transaction currency must be USD"))
    }
}

@Profile("test")
@TestConfiguration
class StubExchangeRateClientConfig {
    @Bean
    @Primary
    fun stubExchangeRateClientPort(): ExchangeRateClientPort =
        object : ExchangeRateClientPort {
            override fun fetchRate(
                from: TargetCurrency,
                to: TargetCurrency,
            ): ExchangeRate = ExchangeRate(BigDecimal.ONE, from, to, Instant.now())

            override fun fetchNearestPriorRate(
                sourceCurrency: TargetCurrency,
                targetCurrency: TargetCurrency,
                rateDate: java.time.LocalDate,
            ): ExchangeRate? = null
        }
}
