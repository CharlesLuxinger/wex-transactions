package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.model.Purchase

interface PurchaseRepositoryPort {
    fun findById(id: Long): Purchase?

    fun findByIdempotencyKey(key: IdempotencyKey): Purchase?

    fun save(
        purchase: Purchase,
        key: IdempotencyKey,
    ): Purchase
}
