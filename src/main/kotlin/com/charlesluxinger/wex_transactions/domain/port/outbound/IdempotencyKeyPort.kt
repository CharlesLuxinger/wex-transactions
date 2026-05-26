package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey

interface IdempotencyKeyPort {
    fun save(
        key: IdempotencyKey,
        purchaseId: Long,
    )

    fun findByKey(key: IdempotencyKey): Long?
}
