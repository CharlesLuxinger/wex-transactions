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
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class RetrieveConvertedUseCaseImpl(
    private val purchaseRepositoryPort: PurchaseRepositoryPort,
    private val exchangeRateCachePort: ExchangeRateCachePort,
    private val exchangeRateClientPort: ExchangeRateClientPort,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse =
        runBlocking {
            val purchase =
                purchaseRepositoryPort.findById(query.purchaseId)
                    ?: throw PurchaseNotFoundException(query.purchaseId)

            val sourceCurrency = purchase.transactionCurrency
            val targetCurrency = TargetCurrency(query.targetCurrency)
            val rateDate = purchase.transactionDate.value.toLocalDate()

            val rate =
                resolveRate(
                    sourceCurrency = sourceCurrency,
                    targetCurrency = targetCurrency,
                    rateDate = rateDate,
                ) ?: throw RateUnavailableException(sourceCurrency.code, targetCurrency.code)

            val convertedAmount =
                purchase.transactionAmount
                    .multiply(rate.rate)
                    .setScale(CONVERSION_SCALE, RoundingMode.HALF_UP)

            RetrieveConvertedResponse(
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

    private suspend fun resolveRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? =
        coroutineScope {
            val cacheDeferred =
                async {
                    RateCandidate(RateSource.CACHE, resolveCacheRate(sourceCurrency, targetCurrency))
                }
            val clientDeferred =
                async {
                    RateCandidate(RateSource.CLIENT, resolveClientRate(sourceCurrency, targetCurrency, rateDate))
                }

            val first =
                select<RateCandidate> {
                    cacheDeferred.onAwait { it }
                    clientDeferred.onAwait { it }
                }

            if (first.rate != null) {
                when (first.source) {
                    RateSource.CACHE -> clientDeferred.cancel()
                    RateSource.CLIENT -> cacheDeferred.cancel()
                }
                if (first.source == RateSource.CLIENT) {
                    bestEffort { exchangeRateCachePort.saveRate(sourceCurrency, targetCurrency, first.rate) }
                }
                return@coroutineScope first.rate
            }

            val second =
                when (first.source) {
                    RateSource.CACHE -> runCatching { clientDeferred.await() }.getOrNull()
                    RateSource.CLIENT -> runCatching { cacheDeferred.await() }.getOrNull()
                }

            if (second?.source == RateSource.CLIENT) {
                second.rate?.let { bestEffort { exchangeRateCachePort.saveRate(sourceCurrency, targetCurrency, it) } }
            }

            second?.rate
        }

    private suspend fun resolveCacheRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? =
        withContext(ioDispatcher) {
            withTimeoutOrNull(CACHE_TIMEOUT_MILLIS) {
                bestEffort { exchangeRateCachePort.getRate(sourceCurrency, targetCurrency) }
                    ?.takeUnless { isStale(it.retrievedAt) }
            }
        }

    private suspend fun resolveClientRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? =
        withContext(ioDispatcher) {
            withTimeoutOrNull(CLIENT_TIMEOUT_MILLIS) {
                bestEffort {
                    exchangeRateClientPort.fetchNearestPriorRate(
                        sourceCurrency = sourceCurrency,
                        targetCurrency = targetCurrency,
                        rateDate = rateDate,
                    )
                }
            }
        }

    private fun isStale(retrievedAt: Instant): Boolean =
        ChronoUnit.DAYS.between(retrievedAt, Instant.now()) > STALE_RATE_DAYS

    private inline fun <T> bestEffort(block: () -> T): T? =
        try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        }

    companion object {
        private const val CONVERSION_SCALE = 2
        private const val CACHE_TIMEOUT_MILLIS = 300L
        private const val CLIENT_TIMEOUT_MILLIS = 3_000L
        private const val STALE_RATE_DAYS = 180L
    }
}

private enum class RateSource {
    CACHE,
    CLIENT,
}

private data class RateCandidate(
    val source: RateSource,
    val rate: ExchangeRate?,
)
