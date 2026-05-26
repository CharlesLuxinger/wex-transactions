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
import com.charlesluxinger.wex_transactions.domain.model.toMonetaryScale
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset

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
            exchangeRateCachePort.getRate(sourceCurrency, targetCurrency, rateDate)
                ?: fetchFromClientOrFallback(sourceCurrency, targetCurrency, rateDate, purchase)

        rate ?: throw RateUnavailableException(sourceCurrency.value, targetCurrency.value)

        val convertedAmount =
            purchase.transactionAmount
                .multiply(rate.rate)
                .toMonetaryScale()

        return RetrieveConvertedResponse(
            purchaseId = purchase.id,
            description = purchase.description,
            transactionDate = purchase.transactionDate.toCanonicalString(),
            transactionAmount = purchase.transactionAmount,
            transactionCurrency = purchase.transactionCurrency.value,
            exchangeRate = rate.rate,
            convertedAmount = convertedAmount,
            targetCurrency = targetCurrency.value,
            createdAt = purchase.createdAt,
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun fetchFromClientOrFallback(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        purchase: Purchase,
    ): ExchangeRate? =
        try {
            exchangeRateClientPort
                .fetchNearestPriorRate(
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                    rateDate = rateDate,
                )?.also { publishToCache(it, purchase) }
        } catch (exception: Exception) {
            val latestCachedRate =
                exchangeRateCachePort.getLatestRate(
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                )

            if (latestCachedRate != null) {
                logger.warn(
                    "[USECASE][TREASURY_FETCH][FALLBACK_CACHE] sourceCurrency={} targetCurrency={} " +
                        "cachedRetrievedAt={} message={}",
                    sourceCurrency.value,
                    targetCurrency.value,
                    latestCachedRate.retrievedAt,
                    exception.message,
                    exception,
                )
                latestCachedRate
            } else {
                throw exception
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private fun publishToCache(
        rate: ExchangeRate,
        purchase: Purchase,
    ) {
        try {
            exchangeRateEventPort.publish(
                ExchangeRateFetchedEvent(
                    sourceCurrency = rate.sourceCurrency.value,
                    targetCurrency = rate.targetCurrency.value,
                    rate = rate.rate,
                    retrievedAt = rate.retrievedAt,
                    rateDate = rate.retrievedAt.atOffset(ZoneOffset.UTC).toLocalDate(),
                ),
            )
        } catch (ex: Exception) {
            logger.warn(
                "[USECASE][CACHE_PUBLISH][FAILED] " +
                    "traceId={} purchaseId={} sourceCurrency={} targetCurrency={} rateDate={} message={}",
                MDC.get(TRACE_ID_KEY),
                purchase.id,
                rate.sourceCurrency.value,
                rate.targetCurrency.value,
                rate.retrievedAt.atOffset(ZoneOffset.UTC).toLocalDate(),
                ex.message,
                ex,
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RetrieveConvertedUseCaseImpl::class.java)
        private const val TRACE_ID_KEY = "traceId"
    }
}
