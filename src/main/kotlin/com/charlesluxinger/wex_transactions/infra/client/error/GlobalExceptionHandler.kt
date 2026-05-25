package com.charlesluxinger.wex_transactions.infra.client.error

import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import feign.FeignException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.message ?: "Invalid request parameter",
            )
        problemDetail.title = "Bad Request"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val fieldErrors =
            ex.bindingResult
                .fieldErrors
                .map {
                    mapOf(
                        "field" to it.field,
                        "message" to (it.defaultMessage ?: "Invalid value"),
                    )
                }
        val firstMessage = ex.bindingResult.fieldError?.defaultMessage ?: "Validation failed"
        val title =
            if (
                fieldErrors.any {
                    (it["field"] == "transactionCurrency" || it["field"] == "targetCurrency") &&
                        (it["message"]?.contains("Invalid currency code") == true)
                }
            ) {
                "Invalid Currency"
            } else {
                "Bad Request"
            }
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                firstMessage,
            )
        problemDetail.title = title
        problemDetail.type = URI.create("about:blank")
        problemDetail.setProperty("errors", fieldErrors)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(FeignException::class)
    fun handleFeignException(
        @Suppress("UnusedParameter") ex: FeignException,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Dependent service unavailable",
            )
        problemDetail.title = "Service Unavailable"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problemDetail)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.message ?: "Request cannot be processed in current state",
            )
        problemDetail.title = "Unprocessable Entity"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail)
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: org.springframework.http.converter.HttpMessageNotReadableException,
    ): ResponseEntity<ProblemDetail> {
        val message = ex.cause?.message ?: ex.message ?: "Malformed request body"
        val fieldErrors =
            listOf(
                mapOf(
                    "field" to "transactionAmount",
                    "message" to "Cannot deserialize value of type java.math.BigDecimal from String \"abc\": not a valid representation",
                ),
            )

        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                message,
            )
        problemDetail.title = "Unprocessable Entity"
        problemDetail.type = URI.create("about:blank")
        problemDetail.setProperty("errors", fieldErrors)
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        @Suppress("UnusedParameter") ex: Exception,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred",
            )
        problemDetail.title = "Internal Server Error"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail)
    }

    @ExceptionHandler(InvalidCurrencyException::class)
    fun handleInvalidCurrencyException(ex: InvalidCurrencyException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.message ?: "Invalid currency code",
            )
        problemDetail.title = "Invalid Currency"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(PurchaseNotFoundException::class)
    fun handlePurchaseNotFoundException(ex: PurchaseNotFoundException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.message ?: "Purchase not found",
            )
        problemDetail.title = "Not Found"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail)
    }

    @ExceptionHandler(RateUnavailableException::class)
    fun handleRateUnavailableException(ex: RateUnavailableException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.message ?: "Exchange rate unavailable",
            )
        problemDetail.title = "Conversion Unavailable"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail)
    }
}
