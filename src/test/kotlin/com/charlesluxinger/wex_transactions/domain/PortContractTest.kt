package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateRepositoryPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StorePurchaseCommandPortTest {
    @Test
    fun `port should accept StorePurchaseCommand and return Purchase`() {
        val port = InMemoryStorePurchaseCommandPort()
        val command =
            StorePurchaseCommand(
                description = "Fuel",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-01-10T10:30:45Z",
                targetCurrency = "BRL",
            )

        val result = port.storePurchase(command)

        assertEquals(BigDecimal("10.00"), result.transactionAmount)
        assertEquals(TargetCurrency("USD"), result.transactionCurrency)
        assertEquals(TargetCurrency("BRL"), result.targetCurrency)
        assertEquals(BigDecimal("50.00"), result.convertedAmount)
    }

    @Test
    fun `port should propagate domain exceptions on invalid input`() {
        val port = InMemoryStorePurchaseCommandPort()
        val invalidCommand =
            StorePurchaseCommand(
                description = "Fuel",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "BAD",
                transactionDate = "2026-01-10T10:30:45Z",
                targetCurrency = "BRL",
            )

        assertFailsWith<InvalidCurrencyException> {
            port.storePurchase(invalidCommand)
        }
    }

    @Test
    fun `store purchase command data class should support copy and equality`() {
        val original =
            StorePurchaseCommand(
                description = "Fuel",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "USD",
                transactionDate = "2026-01-10T10:30:45Z",
                targetCurrency = "BRL",
            )

        val copied = original.copy(targetCurrency = "EUR")

        assertEquals("USD", original.transactionCurrency)
        assertEquals("EUR", copied.targetCurrency)
        assertEquals(original, original.copy())
    }
}

class RetrieveConvertedQueryPortTest {
    @Test
    fun `port should accept RetrieveConvertedQuery and return RetrieveConvertedResponse`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val exchangeRateRepository = InMemoryExchangeRateRepositoryPort()
        val purchase = samplePurchase(id = 123L)
        repository.save(purchase)
        exchangeRateRepository.save(
            ExchangeRate(
                BigDecimal("5.000000"),
                purchase.transactionCurrency,
                TargetCurrency("BRL"),
                Instant.parse("2026-01-10T10:00:00Z"),
            ),
            purchase.transactionDate.value.toLocalDate(),
        )
        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository, exchangeRateRepository)

        val result = port.retrieveConverted(RetrieveConvertedQuery(123L, "BRL"))

        assertEquals(123L, result.purchaseId)
    }

    @Test
    fun `port should throw PurchaseNotFoundException if not found`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val exchangeRateRepository = InMemoryExchangeRateRepositoryPort()
        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository, exchangeRateRepository)

        assertFailsWith<PurchaseNotFoundException> {
            port.retrieveConverted(RetrieveConvertedQuery(999L, "BRL"))
        }
    }

    @Test
    fun `port should throw RateUnavailableException if no rate found`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val exchangeRateRepository = InMemoryExchangeRateRepositoryPort()
        val purchase = samplePurchase(id = 456L)
        repository.save(purchase)
        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository, exchangeRateRepository)

        assertFailsWith<RateUnavailableException> {
            port.retrieveConverted(RetrieveConvertedQuery(456L, "EUR"))
        }
    }

    @Test
    fun `retrieve converted query data class should support copy and equality`() {
        val query = RetrieveConvertedQuery(10, "EUR")
        val copied = query.copy(purchaseId = 11)

        assertEquals(10L, query.purchaseId)
        assertEquals(11L, copied.purchaseId)
        assertEquals(query, query.copy())
    }
}

class PurchaseRepositoryPortTest {
    @Test
    fun `port should save Purchase and return saved instance`() {
        val repository: PurchaseRepositoryPort = InMemoryPurchaseRepositoryPort()
        val purchase = samplePurchase(id = 1L)

        val saved = repository.save(purchase)

        assertEquals(purchase, saved)
    }

    @Test
    fun `port should retrieve Purchase by id`() {
        val repository: PurchaseRepositoryPort = InMemoryPurchaseRepositoryPort()
        val purchase = samplePurchase(id = 2L)
        repository.save(purchase)

        val found = repository.findById(2L)

        assertNotNull(found)
        assertEquals(2L, found.id)
    }

    @Test
    fun `port should return null if Purchase not found`() {
        val repository: PurchaseRepositoryPort = InMemoryPurchaseRepositoryPort()

        val found = repository.findById(999L)

        assertNull(found)
    }
}

class ExchangeRateClientPortTest {
    @Test
    fun `port should fetch rate for valid currency pair`() {
        val client: ExchangeRateClientPort = FakeExchangeRateClientPort()

        val result = client.fetchRate(TargetCurrency("USD"), TargetCurrency("BRL"))

        assertEquals(BigDecimal("5.00"), result.rate)
    }

    @Test
    fun `port should throw RateUnavailableException if rate not found`() {
        val client: ExchangeRateClientPort = FakeExchangeRateClientPort()

        assertFailsWith<RateUnavailableException> {
            client.fetchRate(TargetCurrency("JPY"), TargetCurrency("BRL"))
        }
    }
}

private class InMemoryStorePurchaseCommandPort : StorePurchaseCommandPort {
    override fun storePurchase(command: StorePurchaseCommand): Purchase {
        val source = TargetCurrency(command.transactionCurrency)
        val target = TargetCurrency(command.targetCurrency)
        val date = TransactionDate(command.transactionDate)
        val rate = ExchangeRate(BigDecimal("5.000000"), source, target, Instant.parse("2026-01-10T10:30:00Z"))
        val converted = command.transactionAmount.multiply(rate.rate).setScale(2, RoundingMode.HALF_UP)

        return Purchase(
            id = 1L,
            description = command.description,
            transactionAmount = command.transactionAmount,
            transactionCurrency = source,
            transactionDate = date,
            targetCurrency = target,
            exchangeRate = rate,
            convertedAmount = converted,
            createdAt = Instant.parse("2026-01-10T10:31:00Z"),
        )
    }
}

private class InMemoryRetrieveConvertedQueryPort(
    private val repository: PurchaseRepositoryPort,
    private val exchangeRateRepository: ExchangeRateRepositoryPort,
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): RetrieveConvertedResponse {
        val purchase = repository.findById(query.purchaseId) ?: throw PurchaseNotFoundException(query.purchaseId)
        val targetCurrency = TargetCurrency(query.targetCurrency)
        val rateDate = purchase.transactionDate.value.toLocalDate()
        val rate =
            exchangeRateRepository.findNearestPriorRate(
                sourceCurrency = purchase.transactionCurrency,
                targetCurrency = targetCurrency,
                rateDate = rateDate,
                maxWindowMonths = 6,
            ) ?: throw RateUnavailableException(purchase.transactionCurrency.code, targetCurrency.code)

        val convertedAmount = purchase.transactionAmount.multiply(rate.rate).setScale(2, RoundingMode.HALF_UP)

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
}

private class InMemoryPurchaseRepositoryPort : PurchaseRepositoryPort {
    private val storage = mutableMapOf<Long, Purchase>()

    override fun save(purchase: Purchase): Purchase {
        storage[purchase.id] = purchase
        return purchase
    }

    override fun findById(id: Long): Purchase? = storage[id]
}

private class InMemoryExchangeRateRepositoryPort : ExchangeRateRepositoryPort {
    private val storage = mutableListOf<ExchangeRate>()

    override fun findNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        maxWindowMonths: Long,
    ): ExchangeRate? = storage.find { it.sourceCurrency == sourceCurrency && it.targetCurrency == targetCurrency }

    override fun save(
        exchangeRate: ExchangeRate,
        rateDate: LocalDate,
    ): ExchangeRate {
        storage.add(exchangeRate)
        return exchangeRate
    }
}

private class FakeExchangeRateClientPort : ExchangeRateClientPort {
    override fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate {
        if (from == TargetCurrency("USD") && to == TargetCurrency("BRL")) {
            return ExchangeRate(BigDecimal("5.000000"), from, to, Instant.parse("2026-01-10T10:00:00Z"))
        }

        throw RateUnavailableException(from.code, to.code)
    }

    override fun fetchNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
    ): ExchangeRate? = null
}

private fun samplePurchase(id: Long): Purchase =
    Purchase(
        id = id,
        description = "Fuel",
        transactionAmount = BigDecimal("10.00"),
        transactionCurrency = TargetCurrency("USD"),
        transactionDate = TransactionDate(LocalDateTime.of(2026, 1, 10, 10, 30, 45)),
        targetCurrency = TargetCurrency("BRL"),
        exchangeRate =
            ExchangeRate(
                BigDecimal("5.000000"),
                TargetCurrency("USD"),
                TargetCurrency("BRL"),
                Instant.parse("2026-01-10T10:30:00Z"),
            ),
        convertedAmount = BigDecimal("50.00"),
        createdAt = Instant.parse("2026-01-10T10:31:00Z"),
    )
