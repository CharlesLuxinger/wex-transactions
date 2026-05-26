package com.charlesluxinger.wex_transactions.domain.port.outbound

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey

/**
 * Outbound port for idempotency key cache operations.
 * Manages storage and retrieval of idempotency keys with configurable TTL.
 * Implementations should handle cache misses gracefully (return null).
 */
interface IdempotencyKeyPort {
    /**
     * Store idempotency key mapping to purchase ID with 90-day TTL.
     *
     * @param key The idempotency key (UUID)
     * @param purchaseId The purchase ID to cache
     */
    fun store(
        key: IdempotencyKey,
        purchaseId: Long,
    )

    /**
     * Retrieve purchase ID by idempotency key.
     * Returns null if key not found or expired.
     *
     * @param key The idempotency key to look up
     * @return Purchase ID or null if cache miss/expired
     */
    fun findByKey(key: IdempotencyKey): Long?
}
