package com.charlesluxinger.wex_transactions.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Scales a BigDecimal to monetary precision (2 decimal places) using HALF_UP rounding.
 *
 * This is the standard rounding mode for financial calculations per banking standards.
 *
 * @return a new BigDecimal with scale of 2 and HALF_UP rounding applied
 *
 * Example:
 * ```
 * BigDecimal("100.1234").toMonetaryScale() // returns BigDecimal("100.12")
 * BigDecimal("100.125").toMonetaryScale()  // returns BigDecimal("100.13") (HALF_UP)
 * ```
 */
fun BigDecimal.toMonetaryScale(): BigDecimal = this.setScale(MONETARY_SCALE, RoundingMode.HALF_UP)

private const val MONETARY_SCALE = 2
