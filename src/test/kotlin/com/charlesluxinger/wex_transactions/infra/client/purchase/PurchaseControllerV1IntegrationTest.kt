package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.config.WireMockConfig
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
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
                "transactionCurrency" to "USD",
                "transactionDate" to "2026-05-23T12:00:00Z",
                "targetCurrency" to "BRL",
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
}
