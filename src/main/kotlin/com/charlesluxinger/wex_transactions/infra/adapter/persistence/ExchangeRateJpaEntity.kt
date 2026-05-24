package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "exchange_rates")
class ExchangeRateJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "rate_date", nullable = false)
    var rateDate: LocalDate,
    @Column(name = "source_currency", nullable = false, length = 3)
    var sourceCurrency: String,
    @Column(name = "target_currency", nullable = false, length = 3)
    var targetCurrency: String,
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 6)
    var exchangeRate: BigDecimal,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
    @Column(name = "rate_source", nullable = false, length = 32)
    var rateSource: String = TREASURY_SOURCE,
) {
    fun isTreasurySource(): Boolean = rateSource == TREASURY_SOURCE

    fun toDomain(): ExchangeRate =
        ExchangeRate(
            rate = exchangeRate,
            sourceCurrency = TargetCurrency(sourceCurrency),
            targetCurrency = TargetCurrency(targetCurrency),
            retrievedAt = createdAt,
        )

    companion object {
        const val TREASURY_SOURCE = "TREASURY"
    }
}
