package com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.model.StorePurchaseCommand

interface StorePurchaseCommandPort {
    fun storePurchase(command: StorePurchaseCommand): Purchase
}
