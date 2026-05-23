package com.charlesluxinger.wex_transactions.infra.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ExchangeRateSpringDataRepository : JpaRepository<ExchangeRateJpaEntity, Long>
