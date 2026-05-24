package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@FeignClient(
    name = "treasury-rates",
    url = "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od",
    configuration = [TreasuryFeignConfig::class],
)
interface TreasuryFeignClient {
    @GetMapping("/rates_of_exchange")
    fun fetchRates(
        @RequestParam("fields") fields: String,
        @RequestParam("filter") filter: String,
        @RequestParam("sort") sort: String,
        @RequestParam("page[size]") pageSize: Int,
    ): TreasuryExchangeRateResponse
}
