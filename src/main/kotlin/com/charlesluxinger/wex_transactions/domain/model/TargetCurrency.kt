package com.charlesluxinger.wex_transactions.domain.model

import jakarta.validation.constraints.NotBlank

class TargetCurrency(
    @field:NotBlank
    code: String,
) {
    val code: String = code.trim()

    init {
        if (this.code.isBlank()) {
            throw InvalidCurrencyException(this.code)
        }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is TargetCurrency && code == other.code)

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code
}
