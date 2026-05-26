package com.charlesluxinger.wex_transactions.domain.model

import java.util.UUID

/**
 * Idempotency key value object.
 * Ensures client-provided request identifiers are properly validated and immutable.
 * Required for all purchase transactions to guarantee idempotent semantics.
 */
class IdempotencyKey(
    val value: UUID,
) {
    override fun equals(other: Any?): Boolean = this === other || (other is IdempotencyKey && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()
}
