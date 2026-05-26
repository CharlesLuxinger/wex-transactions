package com.charlesluxinger.wex_transactions.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalExtensionsTest {
    @Test
    fun `toMonetaryScale() should scale to 2 decimals`() {
        val value = BigDecimal("100.1234")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.12"), result)
    }

    @Test
    fun `toMonetaryScale() should use HALF_UP rounding`() {
        // 100.125 with HALF_UP should round to 100.13
        val value = BigDecimal("100.125")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.13"), result)
    }

    @Test
    fun `toMonetaryScale() should round down when appropriate`() {
        // 100.124 should round down to 100.12
        val value = BigDecimal("100.124")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.12"), result)
    }

    @Test
    fun `toMonetaryScale() should round up when appropriate`() {
        // 100.126 should round up to 100.13
        val value = BigDecimal("100.126")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.13"), result)
    }

    @Test
    fun `toMonetaryScale() should handle zero`() {
        val result = BigDecimal.ZERO.toMonetaryScale()

        assertEquals(BigDecimal("0.00"), result)
    }

    @Test
    fun `toMonetaryScale() should handle positive values`() {
        val result = BigDecimal("50.5").toMonetaryScale()

        assertEquals(BigDecimal("50.50"), result)
    }

    @Test
    fun `toMonetaryScale() should handle negative values`() {
        val result = BigDecimal("-99.999").toMonetaryScale()

        assertEquals(BigDecimal("-100.00"), result)
    }

    @Test
    fun `toMonetaryScale() should handle very large values`() {
        val value = BigDecimal("999999999999.999")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("1000000000000.00"), result)
    }

    @Test
    fun `toMonetaryScale() should handle very small positive values`() {
        val value = BigDecimal("0.001")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("0.00"), result)
    }

    @Test
    fun `toMonetaryScale() should handle very small negative values`() {
        val value = BigDecimal("-0.001")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("0.00"), result)
    }

    @Test
    fun `toMonetaryScale() should be idempotent`() {
        val value = BigDecimal("100.1234")
        val first = value.toMonetaryScale()
        val second = first.toMonetaryScale()

        assertEquals(first, second)
    }

    @Test
    fun `toMonetaryScale() should preserve exact scale 2 values`() {
        val value = BigDecimal("100.00")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.00"), result)
        assertEquals(2, result.scale())
    }

    @Test
    fun `toMonetaryScale() should set scale to exactly 2`() {
        val value = BigDecimal("100")
        val result = value.toMonetaryScale()

        assertEquals(2, result.scale())
        assertEquals(BigDecimal("100.00"), result)
    }

    @Test
    fun `toMonetaryScale() should work with currency multiplication`() {
        val usdAmount = BigDecimal("100.00")
        val exchangeRate = BigDecimal("5.123456")
        val converted = usdAmount.multiply(exchangeRate).toMonetaryScale()

        assertEquals(BigDecimal("512.35"), converted)
    }

    @Test
    fun `toMonetaryScale() should handle repeating decimals`() {
        val value = BigDecimal("1.333333333")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("1.33"), result)
    }

    @Test
    fun `toMonetaryScale() should work with division results`() {
        val value = BigDecimal("100").divide(BigDecimal("3"), 10, RoundingMode.HALF_UP)
        val result = value.toMonetaryScale()

        // 100 / 3 = 33.333... which rounds to 33.33
        assertEquals(BigDecimal("33.33"), result)
    }

    @Test
    fun `toMonetaryScale() should handle one-decimal input`() {
        val value = BigDecimal("99.9")
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("99.90"), result)
    }

    @Test
    fun `toMonetaryScale() should handle scientific notation input`() {
        val value = BigDecimal("1E+2") // 100
        val result = value.toMonetaryScale()

        assertEquals(BigDecimal("100.00"), result)
    }

    @Test
    fun `toMonetaryScale() on chain operations should be cleaner than multiple setScale calls`() {
        val amount = BigDecimal("100.123")
        val rate = BigDecimal("5.456789")

        // Using extension function is cleaner
        val resultWithExtension = amount.multiply(rate).toMonetaryScale()

        // 100.123 * 5.456789 = 546.347266447, which HALF_UP rounds to 546.35
        assertEquals(BigDecimal("546.35"), resultWithExtension)
    }

    @Test
    fun `toMonetaryScale() edge case - banker's rounding like HALF_UP behavior`() {
        // Test that HALF_UP always rounds .5 away from zero
        val testCases =
            listOf(
                BigDecimal("10.015") to BigDecimal("10.02"),
                BigDecimal("10.025") to BigDecimal("10.03"),
                BigDecimal("10.035") to BigDecimal("10.04"),
                BigDecimal("10.045") to BigDecimal("10.05"),
                BigDecimal("10.055") to BigDecimal("10.06"),
            )

        testCases.forEach { (input, expected) ->
            assertEquals(expected, input.toMonetaryScale(), "Failed for input: $input")
        }
    }
}
