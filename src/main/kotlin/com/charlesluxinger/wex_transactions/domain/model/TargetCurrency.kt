package com.charlesluxinger.wex_transactions.domain.model

import java.util.Currency

class TargetCurrency(
    code: String,
) {
    val code: String = code.trim().uppercase()

    init {
        if (!CODE_REGEX.matches(this.code) || !isIso4217(this.code)) {
            throw InvalidCurrencyException(this.code)
        }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is TargetCurrency && code == other.code)

    override fun hashCode(): Int = code.hashCode()

    override fun toString(): String = code

    private fun isIso4217(value: String): Boolean =
        runCatching {
            Currency.getInstance(value)
        }.isSuccess

    companion object {
        private val CODE_REGEX = Regex("^[A-Z]{3}$")
    }
}
