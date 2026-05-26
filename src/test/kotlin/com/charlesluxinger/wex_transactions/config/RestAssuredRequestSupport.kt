package com.charlesluxinger.wex_transactions.config

import com.charlesluxinger.wex_transactions.infra.filter.IdempotencyKeyFilter.Companion.IDEMPOTENCY_KEY_HEADER_NAME
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification

interface RestAssuredRequestSupport {
    fun givenJson(idempotencyKey: String? = null): RequestSpecification {
        val spec =
            given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)

        // Add idempotency key header if provided (not null)
        if (idempotencyKey != null) {
            spec.header(IDEMPOTENCY_KEY_HEADER_NAME, idempotencyKey)
        }

        return spec
    }
}
