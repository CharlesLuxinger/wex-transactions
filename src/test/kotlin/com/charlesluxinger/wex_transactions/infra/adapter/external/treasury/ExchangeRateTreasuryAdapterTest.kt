package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import com.charlesluxinger.wex_transactions.domain.model.ExchangeRate
import com.charlesluxinger.wex_transactions.domain.model.TargetCurrency
import com.charlesluxinger.wex_transactions.domain.port.outbound.ExchangeRateCachePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ExchangeRateTreasuryAdapterTest {
    @Mock
    private lateinit var treasuryFeignClient: TreasuryFeignClient

    @Mock
    private lateinit var exchangeRateCachePort: ExchangeRateCachePort

    private lateinit var adapter: ExchangeRateTreasuryAdapter

    private val usd = TargetCurrency("United-States-Dollar")
    private val brazilReal = TargetCurrency("Brazil-Real")
    private val rateDate = LocalDate.parse("2026-05-23")
    private val minBoundary = rateDate.minusMonths(6)

    @BeforeEach
    fun setUp() {
        adapter = ExchangeRateTreasuryAdapter(treasuryFeignClient, exchangeRateCachePort)
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

        adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

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

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("5.25"))
        assertThat(result.sourceCurrency.value).isEqualTo("United-States-Dollar")
        assertThat(result.targetCurrency.value).isEqualTo("Brazil-Real")
    }

    @Test
    @DisplayName("Null data returns null rate")
    fun `null data returns null`() {
        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = null))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Empty records list returns null rate")
    fun `empty records list returns null`() {
        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = emptyList()))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Null fields in record returns null rate")
    fun `null fields in record returns null`() {
        val record =
            TreasuryRateRecord(
                recordDate = "null",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "null",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

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

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

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

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("4.80"))
    }

    @Test
    @DisplayName("Null-like exchange rate values are safely rejected")
    fun `null like exchange rate values are rejected`() {
        val record =
            TreasuryRateRecord(
                recordDate = "2026-05-20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = " N/A ",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["N/A", "-", "", "invalid-rate"])
    @DisplayName("Malformed exchange rate values are safely rejected")
    fun `malformed exchange rate values are rejected`(malformedRate: String) {
        val record =
            TreasuryRateRecord(
                recordDate = "2026-05-20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = malformedRate,
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Multiple records choose newest exchange rate")
    fun `multiple records choose newest record`() {
        val newestRecord =
            TreasuryRateRecord(
                recordDate = "2026-05-22",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5.90",
            )
        val olderRecord =
            TreasuryRateRecord(
                recordDate = "2026-05-20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5.10",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(newestRecord, olderRecord)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("5.90"))
        assertThat(result.retrievedAt)
            .isEqualTo(
                LocalDate
                    .parse("2026-05-22")
                    .atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant(),
            )
    }

    @Test
    @DisplayName("Record older than 6-month window by one day is ignored")
    fun `record older than six month window by one day is ignored`() {
        var capturedFilter: String? = null

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenAnswer { invocation ->
            capturedFilter = invocation.getArgument(1)
            TreasuryExchangeRateResponse(data = emptyList())
        }

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        val outOfWindowDate = rateDate.minusMonths(6).minusDays(1)
        assertThat(capturedFilter).contains("record_date:gte:$minBoundary")
        assertThat(capturedFilter).doesNotContain(outOfWindowDate.toString())
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Invalid numeric exchange rate is safely rejected")
    fun `invalid numeric exchange rate is rejected`() {
        val record =
            TreasuryRateRecord(
                recordDate = "2026-05-20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5,25",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("Invalid record date is safely rejected")
    fun `invalid record date is rejected`() {
        val record =
            TreasuryRateRecord(
                recordDate = "2026/05/20",
                country = "Brazil",
                currency = "Real",
                countryCurrencyDesc = "Brazil-Real",
                exchangeRate = "5.10",
            )

        `when`(
            treasuryFeignClient.fetchRates(anyString(), anyString(), anyString(), anyInt()),
        ).thenReturn(TreasuryExchangeRateResponse(data = listOf(record)))

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName("No descriptor mapping is applied")
    fun `uses treasury country currency description directly`() {
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

        val result = adapter.fetchNearestPriorRate(usd, brazilReal, rateDate)

        assertThat(result).isNotNull
        assertThat(result!!.targetCurrency.value).isEqualTo("Brazil-Real")
    }

    @Test
    @DisplayName("Circuit open with stale cache available returns cached rate")
    fun `circuit open with stale cache available returns cached rate`() {
        val staleRate =
            ExchangeRate(
                rate = BigDecimal("5.00"),
                sourceCurrency = usd,
                targetCurrency = brazilReal,
                retrievedAt = Instant.parse("2026-05-01T00:00:00Z"),
            )

        `when`(exchangeRateCachePort.getLatestRate(usd, brazilReal)).thenReturn(staleRate)

        val result = adapter.fallback(usd, brazilReal, rateDate, RuntimeException("Treasury unavailable"))

        assertThat(result).isNotNull
        assertThat(result!!.rate).isEqualByComparingTo(BigDecimal("5.00"))
        assertThat(result.retrievedAt).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"))
    }

    @Test
    @DisplayName("Circuit open with no cache available throws exception")
    fun `circuit open with no cache available throws exception`() {
        `when`(exchangeRateCachePort.getLatestRate(usd, brazilReal)).thenReturn(null)

        org.junit.jupiter.api.assertThrows<com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException> {
            adapter.fallback(usd, brazilReal, rateDate, RuntimeException("Treasury unavailable"))
        }
    }
}
