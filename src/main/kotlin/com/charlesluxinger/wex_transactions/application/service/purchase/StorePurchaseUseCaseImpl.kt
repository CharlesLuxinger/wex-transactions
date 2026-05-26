package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.model.toMonetaryScale
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.IdempotencyKeyPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import java.time.Instant
import org.springframework.stereotype.Service

@Service
class StorePurchaseUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
    private val idempotencyKeyPort: IdempotencyKeyPort,
) : StorePurchaseCommandPort {
    override fun storePurchase(
        command: StorePurchaseCommand,
        idempotencyKey: IdempotencyKey,
    ): Purchase {
        val cachedPurchaseId = idempotencyKeyPort.findByKey(idempotencyKey)
        if (cachedPurchaseId != null) {
            return purchaseRepositoryPort.findById(cachedPurchaseId)
                ?: throw IllegalStateException(
                    "Cached purchase ID=$cachedPurchaseId not found in database; cache corrupted",
                )
        }

        val purchase =
            Purchase(
                id = NEW_PURCHASE_PLACEHOLDER_ID,
                description = command.description.trim(),
                transactionAmount = command.transactionAmount.toMonetaryScale(),
                transactionCurrency = TargetCurrency(command.transactionCurrency),
                transactionDate = TransactionDate(command.transactionDate),
                createdAt = Instant.now(),
            )

        val saved = purchaseRepositoryPort.saveWithIdempotencyKey(purchase, idempotencyKey)
        idempotencyKeyPort.store(idempotencyKey, saved.id)

        return saved
    }

    companion object {
        private const val NEW_PURCHASE_PLACEHOLDER_ID = 1L
    }
}
