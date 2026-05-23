package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
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
    fun `port should accept RetrieveConvertedQuery and return Purchase`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val purchase = samplePurchase(id = 123L)
        repository.save(purchase)
        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository)

        val result = port.retrieveConverted(RetrieveConvertedQuery(123L))

        assertEquals(123L, result.id)
    }

    @Test
    fun `port should throw PurchaseNotFoundException if not found`() {
        val repository = InMemoryPurchaseRepositoryPort()
        val port: RetrieveConvertedQueryPort = InMemoryRetrieveConvertedQueryPort(repository)

        assertFailsWith<PurchaseNotFoundException> {
            port.retrieveConverted(RetrieveConvertedQuery(999L))
        }
    }

    @Test
    fun `retrieve converted query data class should support copy and equality`() {
        val query = RetrieveConvertedQuery(10)
        val copied = query.copy(purchaseId = 11)

        assertEquals(10L, query.component1())
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

        assertEquals(BigDecimal("5.000000"), result.rate)
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
) : RetrieveConvertedQueryPort {
    override fun retrieveConverted(query: RetrieveConvertedQuery): Purchase =
        repository.findById(query.purchaseId) ?: throw PurchaseNotFoundException(query.purchaseId)
}

private class InMemoryPurchaseRepositoryPort : PurchaseRepositoryPort {
    private val storage = mutableMapOf<Long, Purchase>()

    override fun save(purchase: Purchase): Purchase {
        storage[purchase.id] = purchase
        return purchase
    }

    override fun findById(id: Long): Purchase? = storage[id]
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
