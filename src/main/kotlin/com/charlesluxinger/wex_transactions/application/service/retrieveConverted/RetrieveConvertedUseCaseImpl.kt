package com.charlesluxinger.wex_transactions.application.service.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.springframework.stereotype.Service
import java.math.RoundingMode

@Service
class RetrieveConvertedUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
    private val exchangeRateCachePort: ExchangeRateCachePort,
    private val exchangeRateClientPort: ExchangeRateClientPort,
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse {
        val purchase =
            purchaseRepositoryPort.findById(query.purchaseId)
                ?: throw PurchaseNotFoundException(query.purchaseId)

        val sourceCurrency = purchase.transactionCurrency
        val targetCurrency = TargetCurrency(query.targetCurrency)
        val rateDate = purchase.transactionDate.value.toLocalDate()
        val cachedRate = exchangeRateCachePort.getRate(sourceCurrency, targetCurrency)

        val rate =
            cachedRate
                ?: exchangeRateClientPort
                    .fetchNearestPriorRate(
                        sourceCurrency = sourceCurrency,
                        targetCurrency = targetCurrency,
                        rateDate = rateDate,
                    )?.also { fetchedRate ->
                        exchangeRateCachePort.saveRate(
                            sourceCurrency = sourceCurrency,
                            targetCurrency = targetCurrency,
                            rate = fetchedRate,
                        )
                    } ?: throw RateUnavailableException(sourceCurrency.code, targetCurrency.code)

        val convertedAmount =
            purchase.transactionAmount
                .multiply(rate.rate)
                .setScale(CONVERSION_SCALE, RoundingMode.HALF_UP)

        return RetrieveConvertedResponse(
            purchaseId = purchase.id,
            description = purchase.description,
            transactionDate = purchase.transactionDate.toCanonicalString(),
            originalUsdAmount = purchase.transactionAmount,
            exchangeRateUsed = rate.rate,
            convertedAmount = convertedAmount,
            targetCurrency = targetCurrency.code,
            createdAt = purchase.createdAt,
        )
    }

    companion object {
        private const val CONVERSION_SCALE = 2
    }
}
