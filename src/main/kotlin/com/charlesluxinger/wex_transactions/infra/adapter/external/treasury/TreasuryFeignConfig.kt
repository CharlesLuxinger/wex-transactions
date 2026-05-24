package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import feign.FeignException
import feign.RetryableException
import feign.Retryer
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import kotlin.math.pow
import kotlin.math.roundToLong

class TreasuryFeignConfig {
    @Bean
    fun treasuryRetryer(): Retryer = JitterRetryer()

    @Bean
    fun treasuryErrorDecoder(): ErrorDecoder =
        ErrorDecoder { _, response ->
            when (response.status()) {
                TOO_MANY_REQUESTS, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                    RetryableException(
                        response.status(),
                        response.reason(),
                        response.request().httpMethod(),
                        response
                            .headers()
                            ?.get("retry-after")
                            ?.firstOrNull()
                            ?.toLongOrNull(),
                        response.request(),
                    )

                else -> FeignException.errorStatus(response.reason(), response)
            }
        }

    companion object {
        private const val INITIAL_BACKOFF_MS = 500L
        private const val MAX_BACKOFF_MS = 2_000L
        private const val MAX_RETRIES = 3
        private const val TOO_MANY_REQUESTS = 429
        private const val SERVICE_UNAVAILABLE = 503
        private const val GATEWAY_TIMEOUT = 504
    }
}

class JitterRetryer(
    private val initialBackoff: Long = 500L,
    private val maxBackoff: Long = 2_000L,
    private val maxAttempts: Int = 3,
    private val randomSource: () -> Double = { Math.random() },
) : Retryer {
    private var attempt = 1

    override fun continueOrPropagate(e: RetryableException) {
        if (attempt >= maxAttempts) throw e
        val exponential = initialBackoff * 1.5.pow(attempt - 1)
        val backoff = exponential.roundToLong().coerceIn(initialBackoff, maxBackoff)
        val jitter = (backoff * randomSource()).roundToLong()
        attempt++
        Thread.sleep(backoff + jitter)
    }

    override fun clone(): Retryer = JitterRetryer(initialBackoff, maxBackoff, maxAttempts, randomSource)
}
