package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.Purchase

interface PurchaseRepositoryPort {
    fun save(purchase: Purchase): Purchase

    fun findById(id: Long): Purchase?
}
