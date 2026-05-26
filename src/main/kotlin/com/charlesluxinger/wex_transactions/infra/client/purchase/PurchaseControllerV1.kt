package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.domain.model.IdempotencyKey
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseRequest
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseResponse
import com.charlesluxinger.wex_transactions.infra.filter.IdempotencyKeyFilter.Companion.IDEMPOTENCY_KEY_HEADER_NAME
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/purchases")
class PurchaseControllerV1(
    private val storePurchaseCommandPort: StorePurchaseCommandPort,
) {
    @PostMapping
    @RateLimiter(name = API_PURCHASES_RATE_LIMITER)
    @ResponseStatus(HttpStatus.CREATED)
    fun storePurchase(
        @Valid @RequestBody request: StorePurchaseRequest,
        @RequestHeader(IDEMPOTENCY_KEY_HEADER_NAME, required = true) idempotencyKey: UUID,
    ): StorePurchaseResponse =
        storePurchaseCommandPort
            .storePurchase(
                StorePurchaseCommand(
                    description = request.description.trim(),
                    transactionAmount = request.transactionAmount,
                    transactionCurrency = request.transactionCurrency,
                    transactionDate = request.transactionDate,
                ),
                IdempotencyKey(idempotencyKey),
            ).let { StorePurchaseResponse.fromDomain(it) }

    private companion object {
        private const val API_PURCHASES_RATE_LIMITER = "api-purchases"
    }
}
