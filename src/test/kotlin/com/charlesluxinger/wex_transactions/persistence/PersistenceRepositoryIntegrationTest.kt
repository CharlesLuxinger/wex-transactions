package com.charlesluxinger.wex_transactions.persistence

import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateSpringDataRepository
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.PurchaseJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.PurchaseSpringDataRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PersistenceRepositoryIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres =
            PostgreSQLContainer(DockerImageName.parse("postgres:18.1-alpine3.23"))
                .withDatabaseName("wex_transactions")
                .withUsername("postgres")
                .withPassword("postgres")

        @JvmStatic
        @DynamicPropertySource
        fun configureDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.flyway.enabled") { true }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
        }
    }

    @Autowired
    private lateinit var purchaseSpringDataRepository: PurchaseSpringDataRepository

    @Autowired
    private lateinit var exchangeRateSpringDataRepository: ExchangeRateSpringDataRepository

    @Test
    fun `purchase entity persists and loads round trip`() {
        val now = Instant.parse("2026-01-01T12:00:00Z")
        val entity =
            PurchaseJpaEntity(
                description = "Office supplies",
                transactionAmount = BigDecimal("125.500000"),
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.430000"),
                convertedAmount = BigDecimal("681.465000"),
                createdAt = now,
            )

        val saved = purchaseSpringDataRepository.save(entity)
        val loaded = purchaseSpringDataRepository.findById(saved.id!!)

        assertThat(loaded).isPresent
        assertThat(loaded.get().id).isNotNull
        assertThat(loaded.get().description).isEqualTo("Office supplies")
        assertThat(loaded.get().transactionAmount).isEqualByComparingTo("125.500000")
        assertThat(loaded.get().transactionCurrency).isEqualTo("USD")
        assertThat(loaded.get().transactionDate).isEqualTo(now)
        assertThat(loaded.get().targetCurrency).isEqualTo("BRL")
        assertThat(loaded.get().exchangeRate).isEqualByComparingTo("5.430000")
        assertThat(loaded.get().convertedAmount).isEqualByComparingTo("681.465000")
        assertThat(loaded.get().createdAt).isEqualTo(now)
    }

    @Test
    fun `exchange rate entity persists and loads round trip`() {
        val now = Instant.parse("2026-01-02T10:15:00Z")
        val entity =
            ExchangeRateJpaEntity(
                rateDate = LocalDate.of(2026, 1, 2),
                sourceCurrency = "USD",
                targetCurrency = "EUR",
                exchangeRate = BigDecimal("0.925400"),
                createdAt = now,
            )

        val saved = exchangeRateSpringDataRepository.save(entity)
        val loaded = exchangeRateSpringDataRepository.findById(saved.id!!)

        assertThat(loaded).isPresent
        assertThat(loaded.get().id).isNotNull
        assertThat(loaded.get().rateDate).isEqualTo(LocalDate.of(2026, 1, 2))
        assertThat(loaded.get().sourceCurrency).isEqualTo("USD")
        assertThat(loaded.get().targetCurrency).isEqualTo("EUR")
        assertThat(loaded.get().exchangeRate).isEqualByComparingTo("0.925400")
        assertThat(loaded.get().createdAt).isEqualTo(now)
    }
}
