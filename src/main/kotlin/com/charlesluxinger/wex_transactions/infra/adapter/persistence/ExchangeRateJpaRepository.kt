package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ExchangeRateJpaRepository : JpaRepository<ExchangeRateJpaEntity, Long> {
    @Query(
        """
        SELECT e FROM ExchangeRateJpaEntity e
        WHERE e.sourceCurrency = :sourceCurrency
          AND e.targetCurrency = :targetCurrency
          AND e.rateSource = :rateSource
          AND e.rateDate <= :rateDate
          AND e.rateDate >= :minDate
        ORDER BY e.rateDate DESC
        """,
    )
    fun findNearestPriorRate(
        @Param("sourceCurrency") sourceCurrency: String,
        @Param("targetCurrency") targetCurrency: String,
        @Param("rateSource") rateSource: String,
        @Param("rateDate") rateDate: LocalDate,
        @Param("minDate") minDate: LocalDate,
    ): List<ExchangeRateJpaEntity>
}
