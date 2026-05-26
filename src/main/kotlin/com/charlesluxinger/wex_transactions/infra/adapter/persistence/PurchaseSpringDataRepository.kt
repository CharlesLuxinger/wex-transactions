package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PurchaseSpringDataRepository : JpaRepository<PurchaseJpaEntity, Long> {
    fun findByIdempotencyKey(idempotencyKey: UUID): PurchaseJpaEntity?
}
