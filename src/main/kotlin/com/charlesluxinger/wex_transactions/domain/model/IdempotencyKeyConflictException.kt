package com.charlesluxinger.wex_transactions.domain.model

/**
 * Thrown when an idempotency key conflict is detected.
 * Indicates that a request with the same idempotency key was processed before,
 * but the attempt to process it again resulted in a unique constraint violation.
 * This should not happen in normal operation; signals data corruption or race condition.
 */
class IdempotencyKeyConflictException(
    val idempotencyKey: String,
    override val message: String,
) : DomainException(message)
