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
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.slf4j.LoggerFactory
import org.slf4j.MDC
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
                    ?.also { publishToCache(it, purchase) }

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
    ): ExchangeRate? =
        exchangeRateClientPort.fetchNearestPriorRate(
            sourceCurrency = sourceCurrency,
            targetCurrency = targetCurrency,
            rateDate = rateDate,
        )

    @Suppress("TooGenericExceptionCaught")
    private fun publishToCache(
        rate: ExchangeRate,
        purchase: Purchase,
    ) {
        try {
            exchangeRateEventPort.publish(
                ExchangeRateFetchedEvent(
                    sourceCurrency = rate.sourceCurrency.code,
                    targetCurrency = rate.targetCurrency.code,
                    rate = rate.rate,
                    retrievedAt = rate.retrievedAt,
                    rateDate = purchase.transactionDate.value.toLocalDate(),
                ),
            )
        } catch (ex: Exception) {
            logger.warn(
                "[USECASE][CACHE_PUBLISH][FAILED] " +
                    "traceId={} purchaseId={} sourceCurrency={} targetCurrency={} rateDate={} message={}",
                MDC.get(TRACE_ID_KEY),
                purchase.id,
                rate.sourceCurrency.code,
                rate.targetCurrency.code,
                purchase.transactionDate.value.toLocalDate(),
                ex.message,
                ex,
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RetrieveConvertedUseCaseImpl::class.java)
        private const val CONVERSION_SCALE = 2
        private const val TRACE_ID_KEY = "traceId"
    }
}
