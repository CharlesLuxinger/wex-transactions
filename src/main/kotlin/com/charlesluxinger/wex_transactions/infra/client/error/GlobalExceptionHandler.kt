package com.charlesluxinger.wex_transactions.infra.client.error

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKeyConflictException
import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import feign.FeignException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

private const val HTTP_CLIENT_ERROR_MAX = 499

private fun mapFeignException(ex: FeignException): Triple<HttpStatus, String, String> =
    when (ex.status()) {
        HttpStatus.TOO_MANY_REQUESTS.value() ->
            Triple(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                "Dependent service rate limit exceeded",
            )
        in HttpStatus.BAD_REQUEST.value()..HTTP_CLIENT_ERROR_MAX ->
            Triple(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Conversion Unavailable",
                "Dependent service rejected the conversion request",
            )
        else ->
            Triple(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "Dependent service unavailable",
            )
    }

@RestControllerAdvice
@Suppress("TooManyFunctions")
class GlobalExceptionHandler {
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ProblemDetail> {
        val fieldErrors =
            ex.constraintViolations.map {
                mapOf(
                    "field" to it.propertyPath.toString(),
                    "message" to it.message,
                )
            }
        val firstMessage = fieldErrors.firstOrNull()?.get("message")?.toString() ?: "Validation failed"
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                firstMessage,
            )
        problemDetail.title = "Bad Request"
        problemDetail.type = URI.create("about:blank")
        problemDetail.setProperty("errors", fieldErrors)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

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

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(ex: MissingRequestHeaderException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Missing required header: ${ex.headerName}",
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

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ProblemDetail> {
        val parameterName = ex.name ?: "parameter"
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid value for $parameterName",
            )
        problemDetail.title = "Bad Request"
        problemDetail.type = URI.create("about:blank")
        problemDetail.setProperty(
            "errors",
            listOf(
                mapOf(
                    "field" to parameterName,
                    "message" to "Invalid value for $parameterName",
                ),
            ),
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
    }

    @ExceptionHandler(FeignException::class)
    fun handleFeignException(ex: FeignException): ResponseEntity<ProblemDetail> {
        val (httpStatus, title, detail) = mapFeignException(ex)
        val problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, detail)
        problemDetail.title = title
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(httpStatus).body(problemDetail)
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
        val deserializeErrorMsg =
            "Cannot deserialize value of type java.math.BigDecimal from " +
                "String \"abc\": not a valid representation"
        val fieldErrors =
            listOf(
                mapOf(
                    "field" to "transactionAmount",
                    "message" to deserializeErrorMsg,
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

    @ExceptionHandler(RequestNotPermitted::class)
    fun handleRequestNotPermitted(ex: RequestNotPermitted): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.message ?: "Rate limit exceeded",
            )
        problemDetail.title = "Too Many Requests"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail)
    }

    @ExceptionHandler(IdempotencyKeyConflictException::class)
    fun handleIdempotencyKeyConflictException(ex: IdempotencyKeyConflictException): ResponseEntity<ProblemDetail> {
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.message ?: "Idempotency key conflict detected",
            )
        problemDetail.title = "Conflict"
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail)
    }
}
