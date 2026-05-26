package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.WireMockConfig
import com.charlesluxinger.wex_transactions.infra.filter.IdempotencyKeyFilter.Companion.IDEMPOTENCY_KEY_HEADER_NAME
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
            .header(IDEMPOTENCY_KEY_HEADER_NAME, "550e8400-e29b-41d4-a716-446655440000")
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
    @DisplayName("POST purchase returns 400 when idempotency key is missing")
    fun `post purchase returns 400 when idempotency key is missing`() {
        val payload =
            mapOf(
                "description" to "Laptop charger",
                "transactionAmount" to "100.00",
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
            .body("status", equalTo(400))
            .body("title", equalTo("Bad Request"))
    }

    @Test
    @DisplayName("POST purchase returns 400 when idempotency key is invalid UUID")
    fun `post purchase returns 400 when idempotency key is invalid UUID`() {
        val payload =
            mapOf(
                "description" to "Laptop charger",
                "transactionAmount" to "100.00",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
            )

        givenJson()
            .header(IDEMPOTENCY_KEY_HEADER_NAME, "not-a-valid-uuid")
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("title", equalTo("Bad Request"))
    }

    @Test
    @DisplayName("POST purchase creates purchase with valid idempotency key")
    fun `post purchase creates purchase with valid idempotency key`() {
        val idempotencyKey = "550e8400-e29b-41d4-a716-446655440001"
        val payload =
            mapOf(
                "description" to "First Purchase",
                "transactionAmount" to "100.00",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        givenJson()
            .header(IDEMPOTENCY_KEY_HEADER_NAME, idempotencyKey)
            .body(payload)
            .`when`()
            .post("/api/v1/purchases")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .header(IDEMPOTENCY_KEY_HEADER_NAME, equalTo(idempotencyKey))
            .extract()
            .jsonPath()
            .getString("id")
    }

    @Test
    @DisplayName("POST duplicate purchase with same idempotency key returns same purchase (idempotent)")
    fun `post duplicate purchase with same idempotency key returns same purchase`() {
        val idempotencyKey = "550e8400-e29b-41d4-a716-446655440002"
        val payload =
            mapOf(
                "description" to "Idempotent Purchase Test",
                "transactionAmount" to "100.00",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        // First POST — expect 201 Created
        val firstResponse =
            givenJson()
                .header(IDEMPOTENCY_KEY_HEADER_NAME, idempotencyKey)
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .header(IDEMPOTENCY_KEY_HEADER_NAME, equalTo(idempotencyKey))
                .extract()
                .jsonPath()
                .getString("id")

        // Second POST with identical payload and same key — expect 201 Created (same purchase)
        val secondResponse =
            givenJson()
                .header(IDEMPOTENCY_KEY_HEADER_NAME, idempotencyKey)
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .header(IDEMPOTENCY_KEY_HEADER_NAME, equalTo(idempotencyKey))
                .extract()
                .jsonPath()
                .getString("id")

        // Verify both calls returned same purchase (idempotent)
        assert(firstResponse == secondResponse) { "Expected same ID for idempotent replay with same key" }
    }

    @Test
    @DisplayName("POST purchase with different idempotency keys creates different purchases")
    fun `post purchase with different idempotency keys creates different purchases`() {
        val key1 = "550e8400-e29b-41d4-a716-446655440003"
        val key2 = "550e8400-e29b-41d4-a716-446655440004"
        val payload =
            mapOf(
                "description" to "Same Description Purchase",
                "transactionAmount" to "100.00",
                "transactionCurrency" to "United-States-Dollar",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "Brazil-Real",
            )

        // First POST with key1
        val firstResponse =
            givenJson()
                .header(IDEMPOTENCY_KEY_HEADER_NAME, key1)
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id")

        // Second POST with key2 (different key, same payload)
        val secondResponse =
            givenJson()
                .header(IDEMPOTENCY_KEY_HEADER_NAME, key2)
                .body(payload)
                .`when`()
                .post("/api/v1/purchases")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getString("id")

        // Verify different keys create different purchases
        assert(firstResponse != secondResponse) { "Expected different IDs for different idempotency keys" }
    }
}
