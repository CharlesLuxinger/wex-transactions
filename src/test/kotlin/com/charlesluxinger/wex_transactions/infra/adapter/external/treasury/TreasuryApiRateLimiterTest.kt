package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.config.AbstractRestApiIntegrationTest
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.LocalDate

class TreasuryApiRateLimiterTest : AbstractRestApiIntegrationTest() {
    @Autowired
    private lateinit var adapter: ExchangeRateTreasuryAdapter

    @AfterEach
    fun resetRateLimiter() {
        repeat(10) {
            runCatching {
                adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
            }
        }
        Thread.sleep(1_100)
    }

    @Test
    @DisplayName("11th call in same minute is rejected by treasury-api rate limiter")
    fun `eleventh call is rejected`() {
        repeat(10) {
            runCatching {
                adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
            }
        }

        val thrown =
            org.junit.jupiter.api.assertThrows<RateUnavailableException> {
                adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
            }

        assertThat(thrown.message).contains("United-States-Dollar")
        assertThat(thrown.message).contains("Brazil-Real")
    }

    @Test
    @DisplayName("Rate limiter refresh permits call after period reset")
    fun `rate limiter resets and permits next call`() {
        repeat(10) {
            runCatching {
                adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
            }
        }

        org.junit.jupiter.api.assertThrows<RateUnavailableException> {
            adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
        }

        Thread.sleep(1_100)

        // After rate limiter resets, the call is permitted but Treasury API still fails
        org.junit.jupiter.api.assertThrows<RateUnavailableException> {
            adapter.fetchNearestPriorRate(unitedStateDollar, BRL, RATE_DATE)
        }
    }

    companion object {
        private val unitedStateDollar = TargetCurrency("United-States-Dollar")
        private val BRL = TargetCurrency("Brazil-Real")
        private val RATE_DATE = LocalDate.parse("2026-05-23")

        @JvmStatic
        @DynamicPropertySource
        fun registerRateLimiterProperties(registry: DynamicPropertyRegistry) {
            registry.add("resilience4j.ratelimiter.instances.treasury-api.limit-for-period") { 10 }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.limit-refresh-period") { "1s" }
            registry.add("resilience4j.ratelimiter.instances.treasury-api.timeout-duration") { "0" }
            registry.add("resilience4j.circuitbreaker.instances.treasury-rates.failure-rate-threshold") { 100 }
            registry.add("resilience4j.circuitbreaker.instances.treasury-rates.minimum-number-of-calls") { 100 }
            registry.add(
                "treasury.api.base-url",
            ) { "http://127.0.0.1:65535/services/api/fiscal_service/v1/accounting/od" }
        }
    }
}
