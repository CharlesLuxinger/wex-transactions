package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "purchases")
class PurchaseJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "description", nullable = false, length = 255)
    var description: String,
    @Column(name = "transaction_amount", nullable = false, precision = 18, scale = 6)
    var transactionAmount: BigDecimal,
    @Column(name = "transaction_currency", nullable = false, length = 3)
    var transactionCurrency: String,
    @Column(name = "transaction_date", nullable = false)
    var transactionDate: Instant,
    @Column(name = "target_currency", nullable = false, length = 3)
    var targetCurrency: String,
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 6)
    var exchangeRate: BigDecimal,
    @Column(name = "converted_amount", nullable = false, precision = 18, scale = 6)
    var convertedAmount: BigDecimal,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)
