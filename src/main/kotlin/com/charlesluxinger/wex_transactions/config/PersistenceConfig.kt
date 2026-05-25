package com.charlesluxinger.wex_transactions.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.core.env.Environment
import jakarta.annotation.PostConstruct

@Configuration
@ConditionalOnProperty(name = ["app.persistence.enabled"], havingValue = "true", matchIfMissing = true)
@EnableJpaRepositories(basePackages = ["com.charlesluxinger.wex_transactions.infra.adapter.persistence"])
class PersistenceConfig

@Configuration
@ConditionalOnProperty(name = ["app.persistence.enabled"], havingValue = "false")
class PersistenceGuard(
    private val environment: Environment,
) {
    @PostConstruct
    fun validate() {
        val isTestProfile = environment.activeProfiles.contains("test")
        if (!isTestProfile) {
            throw IllegalStateException("Persistence cannot be disabled outside of the test profile.")
        }
    }
}
