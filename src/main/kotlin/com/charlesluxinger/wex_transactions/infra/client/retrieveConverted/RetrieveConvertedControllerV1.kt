package com.charlesluxinger.wex_transactions.infra.client.retrieveConverted

import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.RetrieveConvertedQueryPort
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedQuery
import com.charlesluxinger.wex_transactions.domain.port.inbound.retrieveConverted.model.RetrieveConvertedResponse
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/purchases")
class RetrieveConvertedControllerV1(
    private val retrieveConvertedQueryPort: RetrieveConvertedQueryPort,
) {
    @GetMapping("/{purchaseId}/converted")
    @ResponseStatus(HttpStatus.OK)
    fun retrieveConverted(
        @PathVariable purchaseId: Long,
        @RequestParam
        @NotBlank(message = "Target currency must not be blank")
        targetCurrency: String,
    ): RetrieveConvertedResponse {
        val response =
            retrieveConvertedQueryPort.retrieveConverted(
                RetrieveConvertedQuery(
                    purchaseId = purchaseId,
                    targetCurrency = targetCurrency,
                ),
            )

        return response.copy(targetCurrency = targetCurrency.uppercase())
    }
}
