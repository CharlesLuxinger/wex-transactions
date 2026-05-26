package com.charlesluxinger.wex_transactions.infra.adapter.cache

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.port.outbound.IdempotencyKeyPort
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonProcessingException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * Redis adapter for idempotency key caching.
 * Stores key→purchaseId mappings with 90-day TTL.
 * Enables fast replay detection without database queries.
 */
@Component
class IdempotencyKeyRedisAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : IdempotencyKeyPort {
    companion object {
        // 90 days in seconds: 90 * 24 * 60 * 60
        private const val IDEMPOTENCY_TTL_SECONDS = 7_776_000L
        private const val IDEMPOTENCY_KEY_PREFIX = "idempotency:"
    }

    override fun store(
        key: IdempotencyKey,
        purchaseId: Long,
    ) {
        try {
            val redisKey = "$IDEMPOTENCY_KEY_PREFIX${key.value}"
            val cacheValue = objectMapper.writeValueAsString(mapOf("purchaseId" to purchaseId))
            redisTemplate.opsForValue().set(redisKey, cacheValue, IDEMPOTENCY_TTL_SECONDS, TimeUnit.SECONDS)
        } catch (e: JsonProcessingException) {
            // Log but do not throw; cache failure should not block purchase creation
            // Fallback to database UNIQUE constraint
            System.err.println("Failed to serialize idempotency cache value for key=${key.value}: ${e.message}")
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // Catch other Redis/network errors; fail gracefully
            System.err.println("Failed to store idempotency key in Redis: ${e.message}")
        }
    }

    override fun findByKey(key: IdempotencyKey): Long? {
        return try {
            val redisKey = "$IDEMPOTENCY_KEY_PREFIX${key.value}"
            val cachedValue = redisTemplate.opsForValue().get(redisKey) ?: return null
            val parsed = objectMapper.readValue(cachedValue, Map::class.java)
            (parsed["purchaseId"] as? Number)?.toLong()
        } catch (e: JsonProcessingException) {
            // Log but do not throw; cache miss should fall through to database
            System.err.println("Failed to deserialize idempotency cache value for key=${key.value}: ${e.message}")
            null
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            // Catch other Redis/network errors; fail gracefully
            System.err.println("Failed to retrieve idempotency key from Redis: ${e.message}")
            null
        }
    }
}
