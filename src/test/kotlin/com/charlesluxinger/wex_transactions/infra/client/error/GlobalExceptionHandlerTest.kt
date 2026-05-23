package com.charlesluxinger.wex_transactions.infra.client.error

import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

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
    }
}
