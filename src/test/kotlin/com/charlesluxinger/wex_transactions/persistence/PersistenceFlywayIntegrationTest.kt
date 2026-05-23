package com.charlesluxinger.wex_transactions.persistence

import com.charlesluxinger.wex_transactions.config.TestContainersConfig
import com.charlesluxinger.wex_transactions.config.TestContainersSupport
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.ExchangeRateJpaRepository
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.PurchaseJpaEntity
import com.charlesluxinger.wex_transactions.infra.adapter.persistence.PurchaseSpringDataRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
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
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
class PersistenceFlywayIntegrationTest : TestContainersSupport() {
    @Autowired
    private lateinit var purchaseRepository: PurchaseSpringDataRepository

    @Autowired
    private lateinit var exchangeRateRepository: ExchangeRateJpaRepository

    @AfterEach
    fun cleanup() {
        exchangeRateRepository.deleteAll()
        purchaseRepository.deleteAll()
    }

    @Test
    fun `flyway creates purchases and exchange rates schema`() {
        val now = Instant.now()

        val purchase =
            purchaseRepository.save(
                PurchaseJpaEntity(
                    description = "Test purchase",
                    transactionAmount = BigDecimal("100.00"),
                    transactionCurrency = "USD",
                    transactionDate = now,
                    targetCurrency = "BRL",
                    exchangeRate = BigDecimal("5.25"),
                    convertedAmount = BigDecimal("525.00"),
                    createdAt = now,
                ),
            )

        val exchangeRate =
            exchangeRateRepository.save(
                ExchangeRateJpaEntity(
                    rateDate = LocalDate.now(),
                    sourceCurrency = "USD",
                    targetCurrency = "BRL",
                    exchangeRate = BigDecimal("5.25"),
                    createdAt = now,
                ),
            )

        assertThat(purchase.id).isNotNull
        assertThat(purchase.description).isEqualTo("Test purchase")

        assertThat(exchangeRate.id).isNotNull
        assertThat(exchangeRate.exchangeRate).isEqualByComparingTo(BigDecimal("5.25"))
    }

    @Test
    fun `flyway creates unique constraint on exchange rates`() {
        val now = Instant.now()
        val rateDate = LocalDate.now()

        exchangeRateRepository.save(
            ExchangeRateJpaEntity(
                rateDate = rateDate,
                sourceCurrency = "USD",
                targetCurrency = "BRL",
                exchangeRate = BigDecimal("5.25"),
                createdAt = now,
            ),
        )

        assertThatThrownBy {
            exchangeRateRepository.saveAndFlush(
                ExchangeRateJpaEntity(
                    rateDate = rateDate,
                    sourceCurrency = "USD",
                    targetCurrency = "BRL",
                    exchangeRate = BigDecimal("5.50"),
                    createdAt = now,
                ),
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
