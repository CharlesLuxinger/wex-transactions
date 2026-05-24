package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import feign.RetryableException
import feign.Request
import feign.Request.HttpMethod
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

class JitterRetryerTest {
    private val retryableException =
        RetryableException(
            503,
            "Service Unavailable",
            HttpMethod.GET,
            null as Long?,
            Request.create(HttpMethod.GET, "http://test", emptyMap(), null, Charset.defaultCharset(), null),
        )

    @Test
    @DisplayName("Max attempts exceeded throws original exception")
    fun `max attempts exceeded throws original exception`() {
        val retryer = JitterRetryer(maxAttempts = 2, randomSource = { 0.5 })

        retryer.continueOrPropagate(retryableException)
        assertThrows(RetryableException::class.java) {
            retryer.continueOrPropagate(retryableException)
        }
    }

    @Test
    @DisplayName("Single attempt within limit succeeds without exception")
    fun `single attempt within limit succeeds`() {
        val retryer = JitterRetryer(maxAttempts = 3, randomSource = { 0.5 })

        retryer.continueOrPropagate(retryableException)
        retryer.continueOrPropagate(retryableException)
    }

    @Test
    @DisplayName("Clone creates independent attempt counter")
    fun `clone creates independent attempt counter`() {
        val retryer = JitterRetryer(maxAttempts = 2, randomSource = { 0.5 })
        val cloned = retryer.clone() as JitterRetryer

        retryer.continueOrPropagate(retryableException)

        assertThrows(RetryableException::class.java) {
            retryer.continueOrPropagate(retryableException)
        }

        cloned.continueOrPropagate(retryableException)
        assertThrows(RetryableException::class.java) {
            cloned.continueOrPropagate(retryableException)
        }
    }
}
