package com.charlesluxinger.wex_transactions.domain.model

class TargetCurrency(
    value: String,
) {
    val value: String = value.trim()

    init {
        if (this.value.isBlank()) {
            throw InvalidCurrencyException(this.value)
        }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is TargetCurrency && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value
}
