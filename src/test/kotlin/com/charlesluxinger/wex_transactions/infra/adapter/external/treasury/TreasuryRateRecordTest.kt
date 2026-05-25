package com.charlesluxinger.wex_transactions.infra.adapter.external.treasury

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TreasuryRateRecordTest {
    @Test
    @DisplayName("Record with null-string exchangeRate has hasValidExchangeRate = false")
    fun `null exchangeRate marks hasValidExchangeRate false`() {
        val record = TreasuryRateRecord(exchangeRate = "null")
        assertThat(record.hasValidExchangeRate).isFalse()
    }

    @Test
    @DisplayName("Record with blank exchangeRate has hasValidExchangeRate = false")
    fun `blank exchangeRate marks hasValidExchangeRate false`() {
        val record = TreasuryRateRecord(exchangeRate = "")
        assertThat(record.hasValidExchangeRate).isFalse()
    }

    @Test
    @DisplayName("Record with valid exchangeRate has hasValidExchangeRate = true")
    fun `valid exchangeRate marks hasValidExchangeRate true`() {
        val record = TreasuryRateRecord(exchangeRate = "5.25")
        assertThat(record.hasValidExchangeRate).isTrue()
    }

    @Test
    @DisplayName("Record with trimmed and uppercase null-like exchangeRate has hasValidExchangeRate = false")
    fun `trimmed uppercase null like exchangeRate marks hasValidExchangeRate false`() {
        val nullLikeValues = listOf(" null ", " NULL ", "-", " N/A ", "na", " NA ")

        nullLikeValues.forEach { value ->
            val record = TreasuryRateRecord(exchangeRate = value)
            assertThat(record.hasValidExchangeRate).isFalse()
        }
    }

    @Test
    @DisplayName("Record parsedRate uses safe conversion and returns null for invalid numeric value")
    fun `parsedRate returns null for invalid numeric value`() {
        val invalidRate = TreasuryRateRecord(exchangeRate = "x5.25")
        val validRate = TreasuryRateRecord(exchangeRate = " 5.25 ")

        assertThat(invalidRate.parsedRate).isNull()
        assertThat(validRate.parsedRate).isEqualByComparingTo("5.25")
    }

    @Test
    @DisplayName("Record with null-string recordDate has hasValidRecordDate = false")
    fun `null recordDate marks hasValidRecordDate false`() {
        val record = TreasuryRateRecord(recordDate = "null")
        assertThat(record.hasValidRecordDate).isFalse()
    }

    @Test
    @DisplayName("Record with blank recordDate has hasValidRecordDate = false")
    fun `blank recordDate marks hasValidRecordDate false`() {
        val record = TreasuryRateRecord(recordDate = "")
        assertThat(record.hasValidRecordDate).isFalse()
    }

    @Test
    @DisplayName("Record with valid recordDate has hasValidRecordDate = true")
    fun `valid recordDate marks hasValidRecordDate true`() {
        val record = TreasuryRateRecord(recordDate = "2026-05-20")
        assertThat(record.hasValidRecordDate).isTrue()
    }

    @Test
    @DisplayName("Record parsedRecordDateOrNull returns null for invalid date")
    fun `parsedRecordDateOrNull returns null for invalid date`() {
        val invalidDate = TreasuryRateRecord(recordDate = "2026/05/20")
        val validDate = TreasuryRateRecord(recordDate = " 2026-05-20 ")

        assertThat(invalidDate.parsedRecordDateOrNull).isNull()
        assertThat(validDate.parsedRecordDateOrNull).isEqualTo(java.time.LocalDate.parse("2026-05-20"))
    }

    @Test
    @DisplayName("Record with null-string countryCurrencyDesc has hasValidDescription = false")
    fun `null countryCurrencyDesc marks hasValidDescription false`() {
        val record = TreasuryRateRecord(countryCurrencyDesc = "null")
        assertThat(record.hasValidDescription).isFalse()
    }

    @Test
    @DisplayName("Record with blank countryCurrencyDesc has hasValidDescription = false")
    fun `blank countryCurrencyDesc marks hasValidDescription false`() {
        val record = TreasuryRateRecord(countryCurrencyDesc = "")
        assertThat(record.hasValidDescription).isFalse()
    }

    @Test
    @DisplayName("Record with valid countryCurrencyDesc has hasValidDescription = true")
    fun `valid countryCurrencyDesc marks hasValidDescription true`() {
        val record = TreasuryRateRecord(countryCurrencyDesc = "Brazil-Real")
        assertThat(record.hasValidDescription).isTrue()
    }

    @Test
    @DisplayName("Record with null-like countryCurrencyDesc values has hasValidDescription = false")
    fun `null like countryCurrencyDesc marks hasValidDescription false`() {
        val nullLikeValues = listOf(" null ", "-", " N/A ", "NA")

        nullLikeValues.forEach { value ->
            val record = TreasuryRateRecord(countryCurrencyDesc = value)
            assertThat(record.hasValidDescription).isFalse()
        }
    }
}
