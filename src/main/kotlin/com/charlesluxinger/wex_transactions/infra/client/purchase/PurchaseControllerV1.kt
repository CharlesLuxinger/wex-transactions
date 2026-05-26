package com.charlesluxinger.wex_transactions.infra.client.purchase

import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseRequest
import com.charlesluxinger.wex_transactions.domain.port.inbound.purchase.model.StorePurchaseResponse
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
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
    ): StorePurchaseResponse {
        val result =
            storePurchaseCommandPort.storePurchase(
                StorePurchaseCommand(
                    description = request.description.trim(),
                    transactionAmount = request.transactionAmount,
                    transactionCurrency = request.transactionCurrency,
                    transactionDate = request.transactionDate,
                ),
            )

        return StorePurchaseResponse(
            id = result.id,
            description = result.description,
            transactionAmount = result.transactionAmount,
            transactionCurrency = result.transactionCurrency.code,
            transactionDate = result.transactionDate.toCanonicalString(),
            createdAt = result.createdAt,
        )
    }

    private companion object {
        private const val API_PURCHASES_RATE_LIMITER = "api-purchases"
    }
}
