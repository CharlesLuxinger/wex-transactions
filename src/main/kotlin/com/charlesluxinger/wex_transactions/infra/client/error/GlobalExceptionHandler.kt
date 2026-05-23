package com.charlesluxinger.wex_transactions.infra.client.error

import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
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
        val fieldError = ex.bindingResult.fieldError
        val detail = fieldError?.defaultMessage ?: "Validation failed"
        val title =
            if ((fieldError?.field == "transactionCurrency" || fieldError?.field == "targetCurrency") &&
                detail.contains("Invalid currency code")
            ) {
                "Invalid Currency"
            } else {
                "Bad Request"
            }
        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                detail,
            )
        problemDetail.title = title
        problemDetail.type = URI.create("about:blank")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail)
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
}
