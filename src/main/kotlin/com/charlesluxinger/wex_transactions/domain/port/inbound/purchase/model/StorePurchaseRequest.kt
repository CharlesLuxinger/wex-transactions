package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class StorePurchaseRequest(
    @field:NotBlank(message = "Description must not be blank")
    @field:Size(max = 50, message = "Description must have at most 50 characters")
    val description: String,
    @field:NotNull(message = "Transaction amount is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Transaction amount must be positive")
    val transactionAmount: BigDecimal,
    @field:NotBlank(message = "Transaction currency must not be blank")
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "Invalid currency code: INVALID")
    val transactionCurrency: String,
    @field:NotBlank(message = "Transaction date must not be blank")
    val transactionDate: String,
    @field:NotBlank(message = "Target currency must not be blank")
    @field:Pattern(regexp = "^[A-Za-z]{3}$", message = "Invalid currency code: INVALID")
    val targetCurrency: String,
)
