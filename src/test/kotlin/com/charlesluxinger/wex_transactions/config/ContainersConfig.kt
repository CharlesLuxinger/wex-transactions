package com.charlesluxinger.wex_transactions.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class ContainersConfig {
    companion object {
        private val POSTGRES_CONTAINER: PostgreSQLContainer<*> =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.1-alpine3.23"))
                .withDatabaseName("wex_transactions")
                .withUsername("postgres")
                .withPassword("postgres")
    }

    @Bean(destroyMethod = "")
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> = POSTGRES_CONTAINER
}
