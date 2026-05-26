package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Service
class StorePurchaseUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
    private val exchangeRateClientPort: ExchangeRateClientPort,
) : StorePurchaseCommandPort {
    override fun storePurchase(command: StorePurchaseCommand): Purchase {
        require(command.description.isNotBlank()) { "Description must not be blank" }
        require(command.description.length <= MAX_DESCRIPTION_LENGTH) {
            "Description must have at most $MAX_DESCRIPTION_LENGTH characters"
        }
        require(command.transactionAmount > BigDecimal.ZERO) { "Transaction amount must be positive" }

        val centRoundedAmount = command.transactionAmount.setScale(CONVERSION_SCALE, RoundingMode.HALF_UP)

        val sourceCurrency = TargetCurrency(command.transactionCurrency)
        require(sourceCurrency.code == "USD") { "Only USD purchases are supported" }
        val targetCurrency = TargetCurrency(command.targetCurrency)
        val transactionDate = TransactionDate(command.transactionDate)

        val rate = exchangeRateClientPort.fetchRate(sourceCurrency, targetCurrency)
        val convertedAmount =
            centRoundedAmount
                .multiply(rate.rate)
                .setScale(CONVERSION_SCALE, RoundingMode.HALF_UP)

        val purchase =
            Purchase(
                id = NEW_PURCHASE_PLACEHOLDER_ID,
                description = command.description.trim(),
                transactionAmount = centRoundedAmount,
                transactionCurrency = sourceCurrency,
                transactionDate = transactionDate,
                targetCurrency = targetCurrency,
                exchangeRate = rate,
                convertedAmount = convertedAmount,
                createdAt = Instant.now(),
            )

        return purchaseRepositoryPort.save(purchase)
    }

    companion object {
        private const val NEW_PURCHASE_PLACEHOLDER_ID = 1L
        private const val MAX_DESCRIPTION_LENGTH = 50
        private const val CONVERSION_SCALE = 2
    }
}
