package com.charlesluxinger.wex_transactions.infra.adapter.event.config

import java.util.UUID

fun UUID.toTraceId(): String = toString().replace("-", "")
