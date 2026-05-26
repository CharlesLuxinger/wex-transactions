package com.charlesluxinger.wex_transactions.domain.model

import java.util.UUID

class IdempotencyKeyConflictException(
    val idempotencyKey: UUID,
    override val message: String,
) : DomainException(message)
