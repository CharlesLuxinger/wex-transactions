package com.charlesluxinger.wex_transactions.application.service.purchase

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.outbound.IdempotencyKeyPort
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class StorePurchaseUseCaseImplTest {
    @Mock
    private lateinit var purchaseRepositoryPort: PurchaseRepositoryPort

    @Mock
    private lateinit var idempotencyKeyPort: IdempotencyKeyPort


    @Test
    @DisplayName("Stores purchase successfully with rounded amount and converted amount")
    fun `stores purchase successfully with rounded values`() {
        var persistedPurchase: Purchase? = null
        var persistedKey: IdempotencyKey? = null

        val idempotencyKey = IdempotencyKey(UUID.randomUUID())

        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun findById(id: Long): Purchase? = null

                override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? = null

                override fun saveWithIdempotencyKey(
                    purchase: Purchase,
                    key: IdempotencyKey,
                ): Purchase {
                    persistedPurchase = purchase
                    persistedKey = key
                    return purchase
                }
            }

        val fakeIdempotencyKeyPort =
            object : IdempotencyKeyPort {
                override fun store(
                    key: IdempotencyKey,
                    purchaseId: Long,
                ) {
                    // No-op for testing
                }

                override fun findByKey(key: IdempotencyKey): Long? = null
            }

        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, fakeIdempotencyKeyPort)
        val command =
            StorePurchaseCommand(
                description = "  New TV  ",
                transactionAmount = BigDecimal("10.005"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )
        val result = useCase.storePurchase(command, idempotencyKey)

        val savedPurchase = checkNotNull(persistedPurchase)
        assertEquals("New TV", savedPurchase.description)
        assertEquals(BigDecimal("10.01"), savedPurchase.transactionAmount)
        assertEquals("United-States-Dollar", savedPurchase.transactionCurrency.value)
        assertEquals(TransactionDate("2026-05-23T12:00:00Z"), savedPurchase.transactionDate)
        assertEquals(idempotencyKey.value, persistedKey?.value)
    }

    @Test
    @DisplayName("Returns cached purchase when idempotency key is already stored")
    fun `returns cached purchase on idempotent request`() {
        val cachedPurchaseId = 42L
        val idempotencyKey = IdempotencyKey(UUID.randomUUID())
        val cachedPurchase =
            Purchase(
                id = cachedPurchaseId,
                description = "Cached TV",
                transactionAmount = BigDecimal("15.00"),
                transactionCurrency = TargetCurrency("United-States-Dollar"),
                transactionDate = TransactionDate("2026-05-23T12:00:00Z"),
                createdAt = java.time.Instant.now(),
            )

        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun findById(id: Long): Purchase? = if (id == cachedPurchaseId) cachedPurchase else null

                override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? = null

                override fun saveWithIdempotencyKey(
                    purchase: Purchase,
                    key: IdempotencyKey,
                ): Purchase = throw AssertionError("Should not call save when cache hit")
            }

        val fakeIdempotencyKeyPort =
            object : IdempotencyKeyPort {
                override fun store(
                    key: IdempotencyKey,
                    purchaseId: Long,
                ) {
                    throw AssertionError("Should not call store when cache hit")
                }

                override fun findByKey(key: IdempotencyKey): Long? = cachedPurchaseId
            }

        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, fakeIdempotencyKeyPort)
        val command =
            StorePurchaseCommand(
                description = "New TV",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        val result = useCase.storePurchase(command, idempotencyKey)

        assertEquals(cachedPurchaseId, result.id)
        assertEquals("Cached TV", result.description)
    }

    @Test
    @DisplayName("Throws exception when cached purchase is not found in database")
    fun `throws when idempotency cache is corrupted`() {
        val invalidPurchaseId = 999L
        val idempotencyKey = IdempotencyKey(UUID.randomUUID())

        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun findById(id: Long): Purchase? = null

                override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? = null

                override fun saveWithIdempotencyKey(
                    purchase: Purchase,
                    key: IdempotencyKey,
                ): Purchase = throw AssertionError("Should not reach save")
            }

        val fakeIdempotencyKeyPort =
            object : IdempotencyKeyPort {
                override fun store(
                    key: IdempotencyKey,
                    purchaseId: Long,
                ) {
                    throw AssertionError("Should not reach store")
                }

                override fun findByKey(key: IdempotencyKey): Long? = invalidPurchaseId
            }

        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, fakeIdempotencyKeyPort)
        val command =
            StorePurchaseCommand(
                description = "New TV",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        val exception =
            assertThrows(IllegalStateException::class.java) { useCase.storePurchase(command, idempotencyKey) }

        assertEquals("Cached purchase ID=$invalidPurchaseId not found in database; cache corrupted", exception.message)
    }

    @Test
    @DisplayName("Stores idempotency key after saving purchase")
    fun `stores idempotency key mapping`() {
        var storedKey: IdempotencyKey? = null
        var storedPurchaseId: Long? = null
        var savedPurchase: Purchase? = null

        val idempotencyKey = IdempotencyKey(UUID.randomUUID())

        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun findById(id: Long): Purchase? = null

                override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? = null

                override fun saveWithIdempotencyKey(
                    purchase: Purchase,
                    key: IdempotencyKey,
                ): Purchase {
                    savedPurchase =
                        Purchase(
                            id = 100L,
                            description = purchase.description,
                            transactionAmount = purchase.transactionAmount,
                            transactionCurrency = purchase.transactionCurrency,
                            transactionDate = purchase.transactionDate,
                            createdAt = purchase.createdAt,
                        )
                    return savedPurchase
                }
            }

        val fakeIdempotencyKeyPort =
            object : IdempotencyKeyPort {
                override fun store(
                    key: IdempotencyKey,
                    purchaseId: Long,
                ) {
                    storedKey = key
                    storedPurchaseId = purchaseId
                }

                override fun findByKey(key: IdempotencyKey): Long? = null
            }

        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, fakeIdempotencyKeyPort)
        val command =
            StorePurchaseCommand(
                description = "New TV",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        val result = useCase.storePurchase(command, idempotencyKey)

        assertEquals(idempotencyKey.value, storedKey?.value)
        assertEquals(100L, storedPurchaseId)
        assertEquals(100L, result.id)
    }

    @Test
    @DisplayName("Creates target currency from command")
    fun `converts command currency to target currency`() {
        var persistedPurchase: Purchase? = null

        val idempotencyKey = IdempotencyKey(UUID.randomUUID())

        val fakePurchaseRepositoryPort =
            object : PurchaseRepositoryPort {
                override fun findById(id: Long): Purchase? = null

                override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? = null

                override fun saveWithIdempotencyKey(
                    purchase: Purchase,
                    key: IdempotencyKey,
                ): Purchase {
                    persistedPurchase = purchase
                    return purchase
                }
            }

        val fakeIdempotencyKeyPort =
            object : IdempotencyKeyPort {
                override fun store(
                    key: IdempotencyKey,
                    purchaseId: Long,
                ) {
                    // No-op
                }

                override fun findByKey(key: IdempotencyKey): Long? = null
            }

        val useCase = StorePurchaseUseCaseImpl(fakePurchaseRepositoryPort, fakeIdempotencyKeyPort)
        val command =
            StorePurchaseCommand(
                description = "New TV",
                transactionAmount = BigDecimal("10.00"),
                transactionCurrency = "United-States-Dollar",
                transactionDate = "2026-05-23T12:00:00Z",
            )

        useCase.storePurchase(command, idempotencyKey)

        val savedPurchase = checkNotNull(persistedPurchase)
        assertEquals("United-States-Dollar", savedPurchase.transactionCurrency.value)
    }
}
