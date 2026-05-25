package com.charlesluxinger.wex_transactions.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
open class TestContainersSupport {
    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    protected fun cleanupDatabase() {
        val tables = listOf("purchases")

        tables.forEach { table ->
            try {
                jdbcTemplate.execute("TRUNCATE TABLE $table RESTART IDENTITY CASCADE")
            } catch (_: Exception) {
                // Ignore missing tables or truncation errors
            }
        }
    }
}
