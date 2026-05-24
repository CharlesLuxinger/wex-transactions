package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import feign.FeignException
import feign.Request
import feign.Request.HttpMethod
import feign.Response
import feign.RetryableException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.charset.Charset

class TreasuryFeignConfigTest {
    private val config = TreasuryFeignConfig()
    private val decoder = config.treasuryErrorDecoder()

    private val request =
        Request.create(
            HttpMethod.GET,
            "http://test",
            emptyMap(),
            null,
            Charset.defaultCharset(),
            null,
        )

    private fun buildResponse(
        status: Int,
        reason: String = "",
        headers: Map<String, Collection<String>> = emptyMap(),
    ): Response =
        Response
            .builder()
            .status(status)
            .reason(reason)
            .request(request)
            .headers(headers)
            .body("""{"error":"test"}""", Charset.defaultCharset())
            .build()

    @Test
    @DisplayName("Error decoder maps 429 to RetryableException")
    fun `error decoder maps 429 to retryable exception`() {
        val result = decoder.decode("test", buildResponse(429))

        assertThat(result).isInstanceOf(RetryableException::class.java)
    }

    @Test
    @DisplayName("Error decoder maps 503 to RetryableException")
    fun `error decoder maps 503 to retryable exception`() {
        val result = decoder.decode("test", buildResponse(503))

        assertThat(result).isInstanceOf(RetryableException::class.java)
    }

    @Test
    @DisplayName("Error decoder maps 504 to RetryableException")
    fun `error decoder maps 504 to retryable exception`() {
        val result = decoder.decode("test", buildResponse(504))

        assertThat(result).isInstanceOf(RetryableException::class.java)
    }

    @Test
    @DisplayName("Error decoder maps 400 to FeignException (non-retryable)")
    fun `error decoder maps 400 to feign exception`() {
        val result = decoder.decode("test", buildResponse(400))

        assertThat(result).isInstanceOf(FeignException::class.java)
        assertThat(result).isNotInstanceOf(RetryableException::class.java)
    }

    @Test
    @DisplayName("Error decoder maps 500 to FeignException (non-retryable)")
    fun `error decoder maps 500 to feign exception`() {
        val result = decoder.decode("test", buildResponse(500))

        assertThat(result).isInstanceOf(FeignException::class.java)
        assertThat(result).isNotInstanceOf(RetryableException::class.java)
    }

    @Test
    @DisplayName("Error decoder maps retry-after header into RetryableException")
    fun `error decoder maps retry after header`() {
        val result =
            decoder.decode(
                "test",
                buildResponse(
                    status = 429,
                    headers = mapOf("retry-after" to listOf("2")),
                ),
            )

        assertThat(result).isInstanceOf(RetryableException::class.java)
        assertThat((result as RetryableException).retryAfter()).isEqualTo(2L)
    }
}
