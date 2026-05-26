package com.charlesluxinger.wex_transactions.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toMonetaryScale(): BigDecimal = this.setScale(MONETARY_SCALE, RoundingMode.HALF_UP)

private const val MONETARY_SCALE = 2
