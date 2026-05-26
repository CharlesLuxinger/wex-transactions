package com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant

data class RetrieveConvertedResponse(
    @field:NotNull(message = "Purchase id must not be null")
    val purchaseId: Long,
    @field:NotBlank(message = "Description must not be blank")
    @field:Size(max = 50, message = "Description must have at most 50 characters")
    val description: String,
    @field:NotBlank(message = "Transaction date must not be blank")
    val transactionDate: String,
    @field:NotNull(message = "Original United-States-Dollar amount must not be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Original amount must be positive")
    val originalUsdAmount: BigDecimal,
    @field:NotNull(message = "Exchange rate used must not be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be positive")
    val exchangeRateUsed: BigDecimal,
    @field:NotNull(message = "Converted amount must not be null")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Converted amount must be positive")
    val convertedAmount: BigDecimal,
    @field:NotBlank(message = "Target currency must not be blank")
    @field:Size(min = 3, max = 3, message = "Target currency must be exactly 3 characters")
    val targetCurrency: String,
    @field:NotNull(message = "Created at must not be null")
    val createdAt: Instant,
)
