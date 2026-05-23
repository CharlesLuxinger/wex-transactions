package com.charlesluxinger.wex_transactions.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PersistenceFlywayIntegrationTest {
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
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `flyway creates purchases and exchange rates schema`() {
        val purchasesColumns = columns("purchases")
        val exchangeRatesColumns = columns("exchange_rates")

        assertThat(purchasesColumns)
            .contains(
                "id",
                "description",
                "transaction_amount",
                "transaction_currency",
                "transaction_date",
                "target_currency",
                "exchange_rate",
                "converted_amount",
                "created_at",
            )

        assertThat(exchangeRatesColumns)
            .contains(
                "id",
                "rate_date",
                "source_currency",
                "target_currency",
                "exchange_rate",
                "created_at",
            )
    }

    @Test
    fun `flyway creates required index and unique constraint`() {
        val purchaseTransactionDateIndexCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE tablename = 'purchases'
                  AND indexname = 'idx_purchases_transaction_date'
                """.trimIndent(),
                Int::class.java,
            ) ?: 0

        val exchangeRateUniqueConstraintCount =
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'exchange_rates'
                  AND constraint_type = 'UNIQUE'
                  AND constraint_name = 'uk_exchange_rates_date_source_target'
                """.trimIndent(),
                Int::class.java,
            ) ?: 0

        assertThat(purchaseTransactionDateIndexCount).isEqualTo(1)
        assertThat(exchangeRateUniqueConstraintCount).isEqualTo(1)
    }

    private fun columns(tableName: String): List<String> =
        jdbcTemplate.queryForList(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_name = ?
            ORDER BY ordinal_position
            """.trimIndent(),
            String::class.java,
            tableName,
        )
}
