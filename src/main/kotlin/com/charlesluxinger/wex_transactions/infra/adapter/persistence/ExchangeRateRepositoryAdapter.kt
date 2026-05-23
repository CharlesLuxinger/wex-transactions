package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateRepositoryPort
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ExchangeRateRepositoryAdapter(
    private val exchangeRateJpaRepository: ExchangeRateJpaRepository,
) : ExchangeRateRepositoryPort {
    override fun findNearestPriorRate(
        sourceCurrency: TargetCurrency,
        targetCurrency: TargetCurrency,
        rateDate: LocalDate,
        maxWindowMonths: Long,
    ): ExchangeRate? {
        val minDate = rateDate.minusMonths(maxWindowMonths)
        val results =
            exchangeRateJpaRepository.findNearestPriorRate(
                sourceCurrency = sourceCurrency.code,
                targetCurrency = targetCurrency.code,
                rateDate = rateDate,
                minDate = minDate,
            )
        return results.firstOrNull()?.toDomain()
    }
}
