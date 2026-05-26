package com.charlesluxinger.wex_transactions.config

import io.restassured.RestAssured
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestContainersConfig::class)
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
abstract class AbstractRestApiIntegrationTest :
    TestContainersSupport(),
    RestAssuredRequestSupport {
    @LocalServerPort
    private var serverPort: Int = 0

    @BeforeEach
    fun configureRestAssured() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.port = serverPort
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @AfterEach
    fun resetRestAssuredAndDatabase() {
        RestAssured.reset()
        cleanupDatabase()
    }
}
