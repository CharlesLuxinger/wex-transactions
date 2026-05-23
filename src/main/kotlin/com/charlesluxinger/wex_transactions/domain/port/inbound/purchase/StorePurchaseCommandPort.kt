package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand

interface StorePurchaseCommandPort {
    fun storePurchase(command: StorePurchaseCommand): Purchase
}
