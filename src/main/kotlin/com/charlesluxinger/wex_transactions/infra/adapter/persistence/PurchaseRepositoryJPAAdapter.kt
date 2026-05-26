package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKeyConflictException
import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PurchaseRepositoryJPAAdapter(
    private val purchaseSpringDataRepository: PurchaseSpringDataRepository,
) : PurchaseRepositoryPort {
    override fun findById(id: Long): Purchase? =
        purchaseSpringDataRepository
            .findById(id)
            .map {
                it.toDomain()
            }.orElse(null)

    override fun findByIdempotencyKey(key: IdempotencyKey): Purchase? =
        purchaseSpringDataRepository
            .findByIdempotencyKey(key.value)
            ?.toDomain()

    @Transactional
    override fun save(
        purchase: Purchase,
        key: IdempotencyKey,
    ): Purchase =
        try {
            val entity = PurchaseJpaEntity.fromDomainWithIdempotencyKey(purchase, key.value)
            purchaseSpringDataRepository.save(entity).toDomain()
        } catch (e: DataIntegrityViolationException) {
            logger.error(
                "[ADAPTER][PURCHASE_REPOSITORY][FAILED] " +
                    "Idempotency key conflict detected for key=${key.value}; " +
                    "DataIntegrityViolationException: ${e.message}",
            )

            throw IdempotencyKeyConflictException(
                idempotencyKey = key.value,
                message =
                    "Idempotency key already exists for another purchase; " +
                        "conflict detected for key=${key.value}",
            )
        }

    companion object {
        private val logger = LoggerFactory.getLogger(PurchaseRepositoryJPAAdapter::class.java)
    }
}
