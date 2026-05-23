package com.charlesluxinger.wex_transactions.infra.client.storePurchase

import com.charlesluxinger.wex_transactions.domain.model.Purchase
import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.StorePurchaseCommandPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.model.StorePurchaseCommand
import com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.model.StorePurchaseRequest
import com.charlesluxinger.wex_transactions.domain.port.inbound.storePurchase.model.StorePurchaseResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/purchases")
class StorePurchaseControllerV1(
    private val storePurchaseCommandPort: StorePurchaseCommandPort,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun storePurchase(
        @jakarta.validation.Valid @RequestBody request: StorePurchaseRequest,
    ): StorePurchaseResponse {
        validate(request)

        val result =
            storePurchaseCommandPort.storePurchase(
                StorePurchaseCommand(
                    description = request.description.trim(),
                    transactionAmount = request.transactionAmount,
                    transactionCurrency = request.transactionCurrency,
                    transactionDate = request.transactionDate,
                    targetCurrency = request.targetCurrency,
                ),
            )

        return result.toResponse()
    }

    private fun validate(request: StorePurchaseRequest) {
        require(request.description.isNotBlank()) { "Description must not be blank" }
        require(request.description.length <= Purchase.MAX_DESCRIPTION_LENGTH) {
            "Description must have at most ${Purchase.MAX_DESCRIPTION_LENGTH} characters"
        }
        require(request.transactionAmount > BigDecimal.ZERO) { "Transaction amount must be positive" }
        TransactionDate(request.transactionDate)
    }

    private fun Purchase.toResponse(): StorePurchaseResponse =
        StorePurchaseResponse(
            id = id,
            description = description,
            transactionAmount = transactionAmount,
            transactionCurrency = transactionCurrency.code,
            transactionDate = transactionDate.toCanonicalString(),
            targetCurrency = targetCurrency.code,
            exchangeRate = exchangeRate.rate,
            convertedAmount = convertedAmount,
            createdAt = createdAt,
        )
}
