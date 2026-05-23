package com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class StorePurchaseResponse(
    @field:NotNull(message = "Id must not be null")
    val id: Long,
    @field:NotBlank(message = "Description must not be blank")
    @field:Size(max = 50, message = "Description must have at most 50 characters")
    val description: String,
    @field:NotNull(message = "Transaction amount is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Transaction amount must be positive")
    val transactionAmount: BigDecimal,
    @field:NotBlank(message = "Transaction currency must not be blank")
    @field:Size(min = 3, max = 3, message = "Transaction currency must be exactly 3 characters")
    val transactionCurrency: String,
    @field:NotBlank(message = "Transaction date must not be blank")
    val transactionDate: String,
    @field:NotBlank(message = "Target currency must not be blank")
    @field:Size(min = 3, max = 3, message = "Target currency must be exactly 3 characters")
    val targetCurrency: String,
    @field:NotNull(message = "Exchange rate must not be null")
    val exchangeRate: BigDecimal,
    @field:NotNull(message = "Converted amount must not be null")
    val convertedAmount: BigDecimal,
    @field:NotNull(message = "Created at must not be null")
    val createdAt: Instant,
)
