package com.charlesluxinger.wex_transactions.persistence

import com.charlesluxinger.wex_transactions.config.TestContainersConfig
import com.charlesluxinger.wex_transactions.config.TestContainersSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(TestContainersConfig::class)
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class PersistenceFlywayIntegrationTest : TestContainersSupport() {
    @Test
    fun `flyway creates purchases and exchange rates schema`() {
        val purchasesColumns = getColumns("purchases")
        val exchangeRatesColumns = getColumns("exchange_rates")

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

    private fun getColumns(tableName: String): List<String> =
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
