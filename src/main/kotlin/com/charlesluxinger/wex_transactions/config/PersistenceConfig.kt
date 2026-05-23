package com.charlesluxinger.wex_transactions.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ConditionalOnProperty(name = ["app.persistence.enabled"], havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = ["com.charlesluxinger.wex_transactions.infra.adapter.persistence"])
class PersistenceConfig
