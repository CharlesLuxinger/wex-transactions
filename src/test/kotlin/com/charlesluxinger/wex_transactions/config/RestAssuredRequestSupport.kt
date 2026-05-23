package com.charlesluxinger.wex_transactions.config

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification

interface RestAssuredRequestSupport {
    fun givenJson(): RequestSpecification =
        given()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
}
