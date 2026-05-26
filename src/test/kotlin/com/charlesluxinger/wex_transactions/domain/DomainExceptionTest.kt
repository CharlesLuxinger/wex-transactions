package com.charlesluxinger.wex_transactions.domain

import com.charlesluxinger.wex_transactions.domain.model.DomainException
import com.charlesluxinger.wex_transactions.domain.model.InvalidCurrencyException
import com.charlesluxinger.wex_transactions.domain.model.PurchaseNotFoundException
import com.charlesluxinger.wex_transactions.domain.model.RateStaleException
import com.charlesluxinger.wex_transactions.domain.model.RateUnavailableException
import kotlin.reflect.full.isSubclassOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DomainExceptionTest {
    @Test
    fun `DomainException should be Throwable`() {
        val exception = PurchaseNotFoundException(10)

        assertIs<Throwable>(exception)
    }

    @Test
    fun `exception message should be accessible`() {
        val exception = PurchaseNotFoundException(55)

        assertEquals("Purchase with ID 55 not found", exception.message)
    }

    @Test
    fun `PurchaseNotFoundException should contain purchaseId`() {
        val exception = PurchaseNotFoundException(77)

        assertEquals(77L, exception.purchaseId)
    }

    @Test
    fun `PurchaseNotFoundException message should include ID`() {
        val exception = PurchaseNotFoundException(77)

        assertTrue(exception.message?.contains("77") == true)
    }

    @Test
    fun `RateUnavailableException should contain from and to currencies`() {
        val exception = RateUnavailableException("United-States-Dollar", "Brazil-Real")

        assertEquals("United-States-Dollar", exception.from)
        assertEquals("Brazil-Real", exception.to)
    }

    @Test
    fun `RateUnavailableException message should include both currencies`() {
        val exception = RateUnavailableException("United-States-Dollar", "Brazil-Real")

        assertEquals("Exchange rate unavailable: United-States-Dollar → Brazil-Real", exception.message)
    }

    @Test
    fun `RateStaleException should contain minutes threshold`() {
        val exception = RateStaleException(30)

        assertEquals(30L, exception.minutes)
    }

    @Test
    fun `RateStaleException message should include minutes`() {
        val exception = RateStaleException(30)

        assertEquals("Exchange rate is older than 30 minutes", exception.message)
    }

    @Test
    fun `InvalidCurrencyException should contain invalid code`() {
        val exception = InvalidCurrencyException("XYZ")

        assertEquals("XYZ", exception.code)
    }

    @Test
    fun `InvalidCurrencyException message should include code`() {
        val exception = InvalidCurrencyException("XYZ")

        assertEquals("Invalid currency code: XYZ", exception.message)
    }

    @Test
    fun `all exception types should extend DomainException`() {
        val exceptions =
            listOf(
                PurchaseNotFoundException(1),
                RateUnavailableException("United-States-Dollar", "Brazil-Real"),
                RateStaleException(5),
                InvalidCurrencyException("BAD"),
            )

        exceptions.forEach { exception ->
            assertIs<DomainException>(exception)
        }
    }

    @Test
    fun `sealed class should prevent external subclasses`() {
        val sealedSubclasses = DomainException::class.sealedSubclasses

        assertEquals(4, sealedSubclasses.size)
        assertTrue(sealedSubclasses.all { it.isSubclassOf(DomainException::class) })
    }
}
