package com.charlesluxinger.wex_transactions.infra.adapter.external

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateClientPort
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class DummyExchangeRateClientAdapter : ExchangeRateClientPort {
    override fun fetchRate(
        from: TargetCurrency,
        to: TargetCurrency,
    ): ExchangeRate {
        // Return a dummy rate of 1.0 for now to support wave 2 testing without a real external client
        return ExchangeRate(
            rate = BigDecimal("1.000000"),
            sourceCurrency = from,
            targetCurrency = to,
            retrievedAt = Instant.now(),
        )
    }
}
