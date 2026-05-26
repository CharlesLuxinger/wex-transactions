package com.charlesluxinger.wex_transactions.domain.model

import java.util.UUID

class IdempotencyKey(
    val value: UUID,
) {
    override fun equals(other: Any?): Boolean = this === other || (other is IdempotencyKey && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}
