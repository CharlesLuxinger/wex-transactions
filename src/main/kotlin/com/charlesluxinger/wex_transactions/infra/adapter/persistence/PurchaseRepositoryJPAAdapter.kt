package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.outbound.PurchaseRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PurchaseRepositoryJPAAdapter(
    private val purchaseSpringDataRepository: PurchaseSpringDataRepository,
) : PurchaseRepositoryPort {
    @Transactional
    override fun save(purchase: Purchase): Purchase {
        val entity = PurchaseJpaEntity.fromDomain(purchase)
        val saved = purchaseSpringDataRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): Purchase? =
        purchaseSpringDataRepository
            .findById(id)
            .map {
                it.toDomain()
            }.orElse(null)
}
