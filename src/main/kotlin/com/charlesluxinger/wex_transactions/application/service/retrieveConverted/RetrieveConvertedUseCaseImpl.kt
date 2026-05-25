package com.charlesluxinger.wex_transactions.application.service.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateEventPort
import com.charlesluxinger.wex_transactions.domain.event.ExchangeRateFetchedEvent
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.LocalDate

@Service
class RetrieveConvertedUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
    private val exchangeRateCachePort: ExchangeRateCachePort,
    private val exchangeRateClientPort: ExchangeRateClientPort,
    private val exchangeRateEventPort: ExchangeRateEventPort,
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse {
        val purchase =
            purchaseRepositoryPort.findById(query.purchaseId)
                ?: throw PurchaseNotFoundException(query.purchaseId)

        val sourceCurrency = purchase.transactionCurrency
        val targetCurrency = TargetCurrency(query.targetCurrency)
        val rateDate = purchase.transactionDate.value.toLocalDate()
        val rate =
            exchangeRateCachePort.getRate(sourceCurrency, targetCurrency)
                ?: fetchClient(sourceCurrency, targetCurrency, rateDate)

        rate ?: throw RateUnavailableException(sourceCurrency.code, targetCurrency.code)

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

    private fun fetchClient(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? {
        val fetchedRate = exchangeRateClientPort.fetchNearestPriorRate(
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            rateDate = rateDate,
        )

        if (fetchedRate != null) {
            val eventPort = exchangeRateEventPort
            runCatching {
                eventPort.publish(
                    ExchangeRateFetchedEvent(
                        sourceCurrency = sourceCurrency.code,
                        targetCurrency = targetCurrency.code,
                        rate = fetchedRate.rate,
                        retrievedAt = fetchedRate.retrievedAt,
                        rateDate = rateDate,
                    ),
                )
            }
        }

        return fetchedRate
    }

    companion object {
        private const val CONVERSION_SCALE = 2
    }
}
