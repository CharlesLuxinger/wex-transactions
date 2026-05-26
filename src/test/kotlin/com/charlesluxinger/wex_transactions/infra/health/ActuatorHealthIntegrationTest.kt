package com.charlesluxinger.wex_transactions.infra.health

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ActuatorHealthIntegrationTest : AbstractRestApiIntegrationTest() {
    @Test
    @DisplayName("Actuator health endpoint returns UP")
    fun `actuator health endpoint returns up`() {
        givenJson()
            .`when`()
            .get("/actuator/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"))
    }
}
