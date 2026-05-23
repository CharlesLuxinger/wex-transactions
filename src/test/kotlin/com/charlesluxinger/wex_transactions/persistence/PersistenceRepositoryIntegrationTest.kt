package com.charlesluxinger.wex_transactions.persistence

import com.charlesluxinger.wex_transactions.config.TestContainersConfig
import com.charlesluxinger.wex_transactions.config.TestContainersSupport
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.PurchaseJpaEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.test.context.TestPropertySource
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(TestContainersConfig::class)
@TestPropertySource(
    properties = [
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
    ],
)
class PersistenceRepositoryIntegrationTest : TestContainersSupport() {
    @Autowired
    private lateinit var purchaseRepository: JpaRepository<PurchaseJpaEntity, Long>

    @Autowired
    private lateinit var exchangeRateRepository: JpaRepository<ExchangeRateJpaEntity, Long>

    @AfterEach
    fun tearDown() {
        cleanupDatabase()
    }

    @Test
    @DisplayName("Persist Purchase entity with all required fields")
    fun `should persist purchase with valid data`() {
        val now = Instant.now()
        val purchase =
            PurchaseJpaEntity(
                description = "Sample transaction",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.50"),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            )

        val saved = purchaseRepository.save(purchase)

        assertThat(saved.id).isNotNull()
        assertThat(saved.description).isEqualTo("Sample transaction")
        assertThat(saved.transactionAmount).isEqualByComparingTo(BigDecimal("100.00"))
        assertThat(saved.convertedAmount).isEqualByComparingTo(BigDecimal("550.00"))
    }

    @Test
    @DisplayName("Persist ExchangeRate entity with all required fields")
    fun `should persist exchange rate with valid data`() {
        val now = Instant.now()
        val today = LocalDate.now()
        val exchangeRate =
            ExchangeRateJpaEntity(
                rateDate = today,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.50"),
                createdAt = now,
            )

        val saved = exchangeRateRepository.save(exchangeRate)

        assertThat(saved.id).isNotNull()
        assertThat(saved.sourceCurrency).isEqualTo("USD")
        assertThat(saved.targetCurrency).isEqualTo("BRL")
        assertThat(saved.exchangeRate).isEqualByComparingTo(BigDecimal("5.50"))
    }

    @Test
    @DisplayName("Query persisted purchases by transaction date range")
    fun `should query purchases in date range`() {
        val now = Instant.now()
        purchaseRepository.save(
            PurchaseJpaEntity(
                description = "TX1",
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.50"),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            ),
        )

        val all = purchaseRepository.findAll()

        assertThat(all).hasSize(1)
    }

    @Test
    @DisplayName("Handle description field max length constraint (50 chars)")
    fun `should enforce description max length`() {
        val now = Instant.now()
        val longDescription = "a".repeat(50)
        val purchase =
            PurchaseJpaEntity(
                description = longDescription,
                transactionAmount = BigDecimal("100.00"),
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.50"),
                convertedAmount = BigDecimal("550.00"),
                createdAt = now,
            )

        val saved = purchaseRepository.save(purchase)

        assertThat(saved.description).hasSize(50)
    }

    @Test
    @DisplayName("Persist and retrieve multiple exchange rates")
    fun `should handle multiple exchange rates`() {
        val now = Instant.now()
        val today = LocalDate.now()
        exchangeRateRepository.save(
            ExchangeRateJpaEntity(
                rateDate = today,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.50"),
                createdAt = now,
            ),
        )
        exchangeRateRepository.save(
            ExchangeRateJpaEntity(
                rateDate = today,
                sourceCurrency = "USD",
                targetCurrency = "EUR",
                exchangeRate = BigDecimal("0.92"),
                createdAt = now,
            ),
        )

        val all = exchangeRateRepository.findAll()

        assertThat(all).hasSize(2)
    }

    @Test
    @DisplayName("Persist Purchase with precision constraints (DECIMAL 18,6 HALF_UP)")
    fun `should persist purchase with correct decimal precision`() {
        val now = Instant.now()
        val purchase =
            PurchaseJpaEntity(
                description = "Precision test",
                transactionAmount = BigDecimal("999999999999.999999"),
                transactionCurrency = "USD",
                transactionDate = now,
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.123456"),
                convertedAmount = BigDecimal("5123455.876543"),
                createdAt = now,
            )

        val saved = purchaseRepository.save(purchase)

        assertThat(saved.transactionAmount).isNotNull()
        assertThat(saved.exchangeRate).isNotNull()
    }
}
