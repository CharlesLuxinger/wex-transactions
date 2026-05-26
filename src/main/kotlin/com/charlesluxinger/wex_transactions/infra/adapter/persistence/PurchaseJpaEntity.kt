package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Entity
@Table(name = "purchases")
class PurchaseJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
    @Column(name = "description", nullable = false, length = 50)
    var description: String,
    @Column(name = "transaction_amount", nullable = false, precision = 18, scale = 2)
    var transactionAmount: BigDecimal,
    @Column(name = "transaction_currency", nullable = false, length = 50)
    var transactionCurrency: String,
    @Column(name = "transaction_date", nullable = false)
    var transactionDate: Instant,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
) {
    fun toDomain(): Purchase =
        Purchase(
            id = requireNotNull(id) { "Purchase id must not be null after persistence" },
            description = description,
            transactionAmount = transactionAmount,
            transactionCurrency = TargetCurrency(transactionCurrency),
            transactionDate = TransactionDate(LocalDateTime.ofInstant(transactionDate, ZoneOffset.UTC)),
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(purchase: Purchase): PurchaseJpaEntity =
            PurchaseJpaEntity(
                description = purchase.description,
                transactionAmount = purchase.transactionAmount,
                transactionCurrency = purchase.transactionCurrency.code,
                transactionDate =
                    purchase.transactionDate.value
                        .atOffset(ZoneOffset.UTC)
                        .toInstant(),
                createdAt = purchase.createdAt,
            )
    }
}
