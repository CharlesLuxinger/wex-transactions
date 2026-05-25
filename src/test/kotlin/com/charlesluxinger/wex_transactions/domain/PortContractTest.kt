package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCacheKeyBuilder
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PortContractTest {
    @Test
    fun `retrieve converted query returns response`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val cache = InMemoryExchangeRateCachePort()
        val client = FixedRateExchangeRateClientPort()
        val purchase = samplePurchase(123L)

        repository.save(purchase)
        cache.saveRate(TargetCurrency("USD"), TargetCurrency("BRL"), sampleRate("5.00"))

        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository, cache, client)

        val result = port.retrieveConverted(RetrieveConvertedQuery(123L, "BRL"))

        assertEquals(123L, result.purchaseId)
        assertEquals(BigDecimal("5.00"), result.exchangeRateUsed)
    }

    @Test
    fun `retrieve converted throws not found`() {
        val port: RetrieveConvertedQueryPort =
            InMemoryRetrieveConvertedQueryPort(
                InMemoryPurchaseRepositoryPort(),
                InMemoryExchangeRateCachePort(),
                FixedRateExchangeRateClientPort(),
            )

        assertFailsWith<PurchaseNotFoundException> {
            port.retrieveConverted(RetrieveConvertedQuery(999L, "BRL"))
        }
    }

    @Test
    fun `retrieve converted throws unavailable when client has no rate`() {
        val repository = InMemoryPurchaseRepositoryPort()
        repository.save(samplePurchase(456L))

        val port: RetrieveConvertedQueryPort =
            InMemoryRetrieveConvertedQueryPort(
                repository,
                InMemoryExchangeRateCachePort(),
                object : ExchangeRateClientPort {
                    override fun fetchRate(
                        from: TargetCurrency,
                        to: TargetCurrency,
                    ): ExchangeRate = throw RateUnavailableException(from.code, to.code)

                    override fun fetchNearestPriorRate(
                        sourceCurrency: TargetCurrency,
                        targetCurrency: TargetCurrency,
                        rateDate: java.time.LocalDate,
                    ): ExchangeRate? = null
                },
            )

        assertFailsWith<RateUnavailableException> {
            port.retrieveConverted(RetrieveConvertedQuery(456L, "EUR"))
        }
    }
}

private class InMemoryRetrieveConvertedQueryPort(
    private val repository: PurchaseRepositoryPort,
    private val cache: ExchangeRateCachePort,
    private val client: ExchangeRateClientPort,
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse {
        val purchase = repository.findById(query.purchaseId) ?: throw PurchaseNotFoundException(query.purchaseId)
        val target = TargetCurrency(query.targetCurrency)
        val source = purchase.transactionCurrency
        val rateDate = purchase.transactionDate.value.toLocalDate()

        val rate =
            cache.getRate(source, target)
                ?: client
                    .fetchNearestPriorRate(source, target, rateDate)
                    ?.also { cache.saveRate(source, target, it) }
                ?: throw RateUnavailableException(source.code, target.code)

        val converted = purchase.transactionAmount.multiply(rate.rate).setScale(2, RoundingMode.HALF_UP)

        return RetrieveConvertedResponse(
            purchaseId = purchase.id,
            description = purchase.description,
            transactionDate = purchase.transactionDate.toCanonicalString(),
            originalUsdAmount = purchase.transactionAmount,
            exchangeRateUsed = rate.rate,
            convertedAmount = converted,
            targetCurrency = target.code,
            createdAt = purchase.createdAt,
        )
    }
}

private class InMemoryExchangeRateCachePort : ExchangeRateCachePort {
    private val storage = mutableMapOf<String, ExchangeRate>()

    override fun getRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
    ): ExchangeRate? = storage[ExchangeRateCacheKeyBuilder.buildCacheKey(sourceCurrency, targetCurrency)]

    override fun saveRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rate: ExchangeRate,
    ) {
        storage[ExchangeRateCacheKeyBuilder.buildCacheKey(sourceCurrency, targetCurrency)] = rate
    }
}

private class InMemoryPurchaseRepositoryPort : PurchaseRepositoryPort {
    private val storage = mutableMapOf<Long, Purchase>()

    override fun save(purchase: Purchase): Purchase {
        storage[purchase.id] = purchase
        return purchase
    }

    override fun findById(id: Long): Purchase? = storage[id]
}

private class FixedRateExchangeRateClientPort : ExchangeRateClientPort {
    override fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate = sampleRate("5.00")

    override fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: java.time.LocalDate,
    ): ExchangeRate? =
        if (sourceCurrency == TargetCurrency("USD") && targetCurrency == TargetCurrency("BRL")) {
            sampleRate("5.00")
        } else {
            null
        }
}

private fun samplePurchase(id: Long): Purchase =
    Purchase(
        id = id,
        description = "Fuel",
        transactionAmount = BigDecimal("10.00"),
        transactionCurrency = TargetCurrency("USD"),
        transactionDate = TransactionDate("2026-01-10T10:30:45Z"),
        targetCurrency = TargetCurrency("BRL"),
        exchangeRate = sampleRate("5.00"),
        convertedAmount = BigDecimal("50.00"),
        createdAt = Instant.parse("2026-01-10T10:31:00Z"),
    )

private fun sampleRate(rate: String): ExchangeRate =
    ExchangeRate(
        rate = BigDecimal(rate),
        sourceCurrency = TargetCurrency("USD"),
        targetCurrency = TargetCurrency("BRL"),
        retrievedAt = Instant.parse("2026-01-10T10:00:00Z"),
    )
