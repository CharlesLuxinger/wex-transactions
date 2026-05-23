package com.charlesluxinger.wex_transactions.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import

@TestConfiguration
@Import(ContainersConfig::class)
class TestContainersConfig
