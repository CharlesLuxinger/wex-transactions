package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.WireMockConfig
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import

@Import(WireMockConfig::class)
class PurchaseControllerV1IntegrationTest : AbstractRestApiIntegrationTest() {
    @Test
    @DisplayName("POST purchase returns 422 when amount format is invalid")
    fun `post purchase returns 422 when amount format is invalid`() {
        val payload =
            mapOf(
                "description" to "Lunch at Restaurant",
                "transactionAmount" to "abc",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(422)
            .body("status", equalTo(422))
            .body("title", equalTo("Unprocessable Entity"))
            .body("errors.field", hasItem("transactionAmount"))
            .body("errors.message", hasItem(containsString("Cannot deserialize value")))
    }

    @Test
    @DisplayName("POST duplicate purchase creates separate purchase (no idempotency)")
    fun `post duplicate purchase creates separate purchase`() {
        val payload =
            mapOf(
                "description" to "Duplicate Purchase Test",
                "transactionAmount" to "100.00",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        // First POST — expect 201 Created
        val firstResponse =
            givenJson()
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id")

        // Second POST with identical payload — expect 201 Created (new duplicate)
        // This documents that idempotency is NOT implemented (current behavior)
        val secondResponse =
            givenJson()
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id")

        // Verify both calls created different purchases
        assert(firstResponse != secondResponse) { "Expected different IDs for duplicate submissions" }
    }
}
