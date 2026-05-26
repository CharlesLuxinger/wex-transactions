package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.model.toMonetaryScale
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import java.math.BigDecimal
import java.time.Instant
import org.springframework.stereotype.Service

@Service
class StorePurchaseUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
) : StorePurchaseCommandPort {
    override fun storePurchase(command: StorePurchaseCommand): Purchase {
        require(command.description.isNotBlank()) { "Description must not be blank" }
        require(command.description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must have at most $MAX_DESCRIPTION_LENGTH characters"
        }
        require(command.transactionAmount > BigDecimal.ZERO) { "Transaction amount must be positive" }

        val centRoundedAmount = command.transactionAmount.toMonetaryScale()

        val sourceCurrency = TargetCurrency(command.transactionCurrency)
        require(sourceCurrency.value == DEFAULT_SOURCE_CURRENCY_USD) { "Only $DEFAULT_SOURCE_CURRENCY_USD purchases are supported" }
        val transactionDate = TransactionDate(command.transactionDate)

        val purchase =
            Purchase(
                id = NEW_PURCHASE_PLACEHOLDER_ID,
                description = command.description.trim(),
                transactionAmount = centRoundedAmount,
                transactionCurrency = sourceCurrency,
                transactionDate = transactionDate,
                createdAt = Instant.now(),
            )

        return purchaseRepositoryPort.save(purchase)
    }

    companion object {
        const val DEFAULT_SOURCE_CURRENCY_USD = "United-States-Dollar"
        private const val NEW_PURCHASE_PLACEHOLDER_ID = 1L
        private const val MAX_DESCRIPTION_LENGTH = 50
    }
}
