package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExchangeRateTreasuryAdapterTest {
    @Mock
    private lateinit var treasuryFeignClient: TreasuryFeignClient

    private lateinit var adapter: ExchangeRateTreasuryAdapter

    private val usd = TargetCurrency("USD")
    private val brl = TargetCurrency("BRL")
    private val rateDate = LocalDate.parse("2026-05-23")
    private val minBoundary = rateDate.minusMonths(6)

    @BeforeEach
    fun setUp() {
        adapter = ExchangeRateTreasuryAdapter(treasuryFeignClient)
    }

    @Test
    @DisplayName("Filter includes lte and gte boundary conditions")
    fun `filter includes lte and gte conditions`() {
        var capturedFilter: String? = null

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenAnswer { invocation ->
            capturedFilter = invocation.getArgument(1)
            TreasuryExchangeRateResponse(data = emptyList())
        }

        adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(capturedFilter).contains("record_date:lte:$rateDate")
        assertThat(capturedFilter).contains("record_date:gte:$minBoundary")
    }

    @Test
    @DisplayName("Normal response with matching record returns ExchangeRate")
    fun `normal response with matching record returns exchange rate`() {
        val record =
            TreasuryRateRecord(
                recordDate = "2026-05-20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5.25",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("5.25"))
        assertThat(result.sourceCurrency.code).isEqualTo("USD")
        assertThat(result.targetCurrency.code).isEqualTo("BRL")
    }

    @Test
    @DisplayName("Null data returns null rate")
    fun `null data returns null`() {
        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = null))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Empty records list returns null rate")
    fun `empty records list returns null`() {
        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = emptyList()))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Null fields in record returns null rate")
    fun `null fields in record returns null`() {
        val record =
            TreasuryRateRecord(
                recordDate = null,
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = null,
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Record at exact purchase date boundary is parsed correctly")
    fun `record at exact boundary date is parsed`() {
        val record =
            TreasuryRateRecord(
                recordDate = "$rateDate",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5.00",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("5.00"))
    }

    @Test
    @DisplayName("Record at 6-month boundary date is parsed correctly")
    fun `record at six month boundary date is parsed`() {
        val record =
            TreasuryRateRecord(
                recordDate = "$minBoundary",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "4.80",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brl, rateDate, 6L)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("4.80"))
    }
}
