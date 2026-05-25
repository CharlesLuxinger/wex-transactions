package com.charlesluxinger.wex_transactions.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.GenericContainer
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

        private val REDIS_CONTAINER: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:8.6.0-alpine3.23")).withExposedPorts(6379)
    }

    @Bean(destroyMethod = "")
    @ServiceConnection
    fun postgres(): PostgreSQLContainer<*> = POSTGRES_CONTAINER

    @Bean(destroyMethod = "")
    @ServiceConnection(name = "redis")
    fun redis(): GenericContainer<*> = REDIS_CONTAINER
}
