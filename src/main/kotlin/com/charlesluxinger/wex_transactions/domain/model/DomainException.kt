package com.charlesluxinger.wex_transactions.domain.model

sealed class DomainException(
    message: String,
) : Exception(message)

data class PurchaseNotFoundException(
    val purchaseId: Long,
) : DomainException("Purchase with ID $purchaseId not found")

data class RateUnavailableException(
    val from: String,
    val to: String,
) : DomainException("Exchange rate unavailable: $from → $to")

data class RateStaleException(
    val minutes: Long,
) : DomainException("Exchange rate is older than $minutes minutes")

data class InvalidCurrencyException(
    val code: String,
) : DomainException("Invalid currency code: $code")
