package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.TransactionDate
import java.time.LocalDateTime
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TransactionDateTest {
    @Test
    fun `should accept ISO-8601 date with timezone`() {
        val date = TransactionDate("2026-01-10T15:30:45+02:00")

        assertEquals(LocalDateTime.of(2026, 1, 10, 15, 30, 45), date.value)
    }

    @Test
    fun `should accept ISO-8601 date without timezone`() {
        val date = TransactionDate("2026-01-10T15:30:45")

        assertEquals(LocalDateTime.of(2026, 1, 10, 15, 30, 45), date.value)
    }

    @Test
    fun `should accept ISO-8601 with milliseconds`() {
        val date = TransactionDate("2026-01-10T15:30:45.123Z")

        assertEquals(LocalDateTime.of(2026, 1, 10, 15, 30, 45), date.value)
    }

    @Test
    fun `should normalize to canonical format yyyy MM dd HH mm ss XXX`() {
        val date = TransactionDate("2026-01-10T15:30:45+02:00")

        assertEquals("2026-01-10T15:30:45Z", date.toCanonicalString())
        assertEquals("2026-01-10T15:30:45Z", date.toString())
    }

    @Test
    fun `should trim input before parsing`() {
        val date = TransactionDate(" 2026-01-10T15:30:45Z ")

        assertEquals(LocalDateTime.of(2026, 1, 10, 15, 30, 45), date.value)
    }

    @Test
    fun `should not allow modification after creation`() {
        assertTrue(TransactionDate::class.memberProperties.none { it is KMutableProperty<*> })
    }

    @Test
    fun `equals should match on normalized value`() {
        val first = TransactionDate("2026-01-10T15:30:45.999Z")
        val second = TransactionDate("2026-01-10T15:30:45Z")

        assertEquals(first, second)
    }

    @Test
    fun `hashCode should depend on normalized value`() {
        val first = TransactionDate("2026-01-10T15:30:45.500Z")
        val second = TransactionDate("2026-01-10T15:30:45Z")

        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `two dates with same normalized value should be equal`() {
        val first = TransactionDate(LocalDateTime.of(2026, 1, 10, 15, 30, 45, 999_000_000))
        val second = TransactionDate(LocalDateTime.of(2026, 1, 10, 15, 30, 45))

        assertEquals(first, second)
    }

    @Test
    fun `two dates with different values should not be equal`() {
        val first = TransactionDate("2026-01-10T15:30:45Z")
        val second = TransactionDate("2026-01-10T15:30:46Z")

        assertNotEquals(first, second)
    }

    @Test
    fun `should reject invalid ISO-8601 format`() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                TransactionDate("10/01/2026 15:30:45")
            }

        assertTrue(error.message?.contains("Invalid ISO-8601 transaction date") == true)
    }

    @Test
    fun `should handle future dates`() {
        val date = TransactionDate("2099-12-31T23:59:59Z")

        assertEquals(LocalDateTime.of(2099, 12, 31, 23, 59, 59), date.value)
    }

    @Test
    fun `should handle very old dates`() {
        val date = TransactionDate("1900-01-01T00:00:00Z")

        assertEquals(LocalDateTime.of(1900, 1, 1, 0, 0, 0), date.value)
    }

    @Test
    fun `should drop milliseconds in normalized form`() {
        val date = TransactionDate("2026-01-10T15:30:45.123+00:00")

        assertEquals("2026-01-10T15:30:45Z", date.toCanonicalString())
    }
}
