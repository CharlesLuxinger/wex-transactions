package com.charlesluxinger.wex_transactions.infra.client.error

import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import feign.FeignException
import feign.Request
import feign.Response
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.nio.charset.StandardCharsets
import java.util.Collections

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    @DisplayName("InvalidCurrencyException returns BAD_REQUEST with Invalid Currency title")
    fun `handle invalid currency exception`() {
        val ex = InvalidCurrencyException("XYZ")
        val response = handler.handleInvalidCurrencyException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.title).isEqualTo("Invalid Currency")
        assertThat(response.body?.detail).isEqualTo("Invalid currency code: XYZ")
    }

    @Test
    @DisplayName("IllegalArgumentException with null message uses default detail")
    fun `handle illegal argument with null message`() {
        val ex = IllegalArgumentException()
        val response = handler.handleIllegalArgumentException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.detail).isEqualTo("Invalid request parameter")
    }

    @Test
    @DisplayName("MethodArgumentNotValidException with non-currency field returns Bad Request title")
    fun `handle method argument not valid with non currency field`() {
        val bindingResult = BeanPropertyBindingResult("payload", "request")
        bindingResult.addError(FieldError("request", "description", "Description must not be blank"))
        val parameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(parameter, bindingResult)

        val response = handler.handleMethodArgumentNotValidException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.title).isEqualTo("Bad Request")
        assertThat(response.body?.detail).isEqualTo("Description must not be blank")
        assertThat(response.body?.properties?.get("errors"))
            .isEqualTo(listOf(mapOf("field" to "description", "message" to "Description must not be blank")))
    }

    @Test
    @DisplayName("MethodArgumentNotValidException collects all field errors")
    fun `handle method argument not valid collects all field errors`() {
        val bindingResult = BeanPropertyBindingResult("payload", "request")
        bindingResult.addError(FieldError("request", "transactionCurrency", "Invalid currency code: XX"))
        bindingResult.addError(FieldError("request", "targetCurrency", "Invalid currency code: YY"))
        val parameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(parameter, bindingResult)

        val response = handler.handleMethodArgumentNotValidException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.title).isEqualTo("Invalid Currency")
        assertThat(response.body?.detail).isEqualTo("Invalid currency code: XX")
        assertThat(response.body?.properties?.get("errors"))
            .isEqualTo(
                listOf(
                    mapOf("field" to "transactionCurrency", "message" to "Invalid currency code: XX"),
                    mapOf("field" to "targetCurrency", "message" to "Invalid currency code: YY"),
                ),
            )
    }

    @Test
    @DisplayName("MethodArgumentNotValidException with null fieldError returns default detail")
    fun `handle method argument not valid with null field error`() {
        val bindingResult = BeanPropertyBindingResult("payload", "request")
        val parameter = mock(MethodParameter::class.java)
        val ex = MethodArgumentNotValidException(parameter, bindingResult)

        val response = handler.handleMethodArgumentNotValidException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.detail).isEqualTo("Validation failed")
        assertThat(response.body?.properties?.get("errors")).isEqualTo(emptyList<Map<String, String>>())
    }

    @Test
    @DisplayName("FeignException with 429 returns TOO_MANY_REQUESTS")
    fun `handle feign exception with rate limit status`() {
        val request =
            Request.create(
                Request.HttpMethod.GET,
                "https://example.test/resource",
                emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null,
            )
        val feignResponse =
            Response
                .builder()
                .status(429)
                .reason("Too Many Requests")
                .request(request)
                .headers(Collections.emptyMap())
                .build()
        val ex = FeignException.errorStatus("treasuryClient#getRates", feignResponse)

        val response = handler.handleFeignException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.body?.title).isEqualTo("Too Many Requests")
    }

    @Test
    @DisplayName("FeignException with 400 returns UNPROCESSABLE_ENTITY")
    fun `handle feign exception with client error status`() {
        val request =
            Request.create(
                Request.HttpMethod.GET,
                "https://example.test/resource",
                emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null,
            )
        val feignResponse =
            Response
                .builder()
                .status(400)
                .reason("Bad Request")
                .request(request)
                .headers(Collections.emptyMap())
                .build()
        val ex = FeignException.errorStatus("treasuryClient#getRates", feignResponse)

        val response = handler.handleFeignException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        assertThat(response.body?.title).isEqualTo("Conversion Unavailable")
    }

    @Test
    @DisplayName("FeignException returns SERVICE_UNAVAILABLE with safe detail")
    fun `handle feign exception`() {
        val request =
            Request.create(
                Request.HttpMethod.GET,
                "https://example.test/resource",
                emptyMap(),
                null,
                StandardCharsets.UTF_8,
                null,
            )
        val feignResponse =
            Response
                .builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Collections.emptyMap())
                .build()
        val ex = FeignException.errorStatus("treasuryClient#getRates", feignResponse)

        val response = handler.handleFeignException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        assertThat(response.body?.title).isEqualTo("Service Unavailable")
        assertThat(response.body?.detail).isEqualTo("Dependent service unavailable")
    }

    @Test
    @DisplayName("IllegalStateException returns UNPROCESSABLE_ENTITY")
    fun `handle illegal state exception`() {
        val ex = IllegalStateException("Business state conflict")

        val response = handler.handleIllegalStateException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        assertThat(response.body?.title).isEqualTo("Unprocessable Entity")
        assertThat(response.body?.detail).isEqualTo("Business state conflict")
    }

    @Test
    @DisplayName("Generic Exception returns INTERNAL_SERVER_ERROR with safe detail")
    fun `handle generic exception`() {
        val response = handler.handleGenericException(RuntimeException("sensitive stack trace message"))

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.title).isEqualTo("Internal Server Error")
        assertThat(response.body?.detail).isEqualTo("An unexpected internal error occurred")
    }

    @Test
    @DisplayName("PurchaseNotFoundException returns NOT_FOUND with Not Found title")
    fun `handle purchase not found exception`() {
        val ex = PurchaseNotFoundException(77)
        val response = handler.handlePurchaseNotFoundException(ex)

        assertThat(response).isInstanceOf(ResponseEntity::class.java)
        assertThat(response.body).isInstanceOf(ProblemDetail::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.title).isEqualTo("Not Found")
        assertThat(response.body?.detail).isEqualTo("Purchase with ID 77 not found")
    }

    @Test
    @DisplayName("RateUnavailableException returns UNPROCESSABLE_ENTITY with Conversion Unavailable title")
    fun `handle rate unavailable exception`() {
        val ex = RateUnavailableException("United-States-Dollar", "Brazil-Real")
        val response = handler.handleRateUnavailableException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        assertThat(response.body?.title).isEqualTo("Conversion Unavailable")
        assertThat(response.body?.detail).isEqualTo("Exchange rate unavailable: United-States-Dollar → Brazil-Real")
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException returns BAD_REQUEST")
    fun `handle method argument type mismatch exception`() {
        val ex =
            MethodArgumentTypeMismatchException(
                "abc",
                Long::class.java,
                "purchaseId",
                mock(MethodParameter::class.java),
                IllegalArgumentException("invalid long"),
            )

        val response = handler.handleMethodArgumentTypeMismatchException(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.title).isEqualTo("Bad Request")
        assertThat(response.body?.detail).isEqualTo("Invalid value for purchaseId")
    }

    @Test
    @DisplayName("RequestNotPermitted returns TOO_MANY_REQUESTS with expected title")
    fun `handle request not permitted exception`() {
        val ex = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("treasury-api"))

        val response = handler.handleRequestNotPermitted(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.body?.title).isEqualTo("Too Many Requests")
        assertThat(response.body?.detail).contains("RateLimiter")
    }
}
