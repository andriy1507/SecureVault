package dev.spaceapps.secvault.feature.vault.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentCardTest {
    private val today = YearMonth(2026, 11)

    @Test
    fun numberKeepsDigitsAndBrandLength() {
        assertEquals("4111111111111111", PaymentCard.sanitizeNumber("4111 1111-1111 1111"))
        assertEquals(15, PaymentCard.sanitizeNumber("3782 822463 100059999").length) // Amex max 15
        assertEquals(19, PaymentCard.sanitizeNumber("4".repeat(25)).length)
    }

    @Test
    fun groupsByBrand() {
        assertEquals(listOf("4111", "1111", "1111", "1111"), PaymentCard.groupNumber("4111111111111111"))
        assertEquals(listOf("3782", "822463", "10005"), PaymentCard.groupNumber("378282246310005"))
        assertEquals(listOf("4111", "11"), PaymentCard.groupNumber("411111"))
    }

    @Test
    fun luhn() {
        assertTrue(PaymentCard.isLuhnValid("4111111111111111"))
        assertTrue(PaymentCard.isLuhnValid("378282246310005"))
        assertFalse(PaymentCard.isLuhnValid("4111111111111112"))
    }

    @Test
    fun expiryTypesLikeBankingApps() {
        assertEquals("05", PaymentCard.sanitizeExpiry("5")) // 5 → 05
        assertEquals("1", PaymentCard.sanitizeExpiry("1")) // could be 10–12
        assertEquals("1", PaymentCard.sanitizeExpiry("13")) // month 13 can't be typed
        assertEquals("0", PaymentCard.sanitizeExpiry("00")) // month 00 can't be typed
        assertEquals("0528", PaymentCard.sanitizeExpiry("05/28"))
        assertEquals("1228", PaymentCard.sanitizeExpiry("122899"))
        assertEquals("05/", PaymentCard.formatExpiry("05"))
        assertEquals("05/28", PaymentCard.formatExpiry("0528"))
    }

    @Test
    fun validation() {
        assertTrue(PaymentCard.validate("", "", "", today).isValid)
        assertTrue(PaymentCard.validate("4111111111111111", "1228", "123", today).isValid)
        assertEquals(CardError.InvalidNumber, PaymentCard.validate("4111111111111112", "", "", today).number)
        assertEquals(CardError.InvalidNumber, PaymentCard.validate("41111", "", "", today).number)
        assertEquals(CardError.InvalidExpiry, PaymentCard.validate("", "05", "", today).expiry)
        assertEquals(CardError.InvalidCvv, PaymentCard.validate("378282246310005", "", "123", today).cvv)
        assertNull(PaymentCard.validate("378282246310005", "", "1234", today).cvv)
    }

    @Test
    fun expiredIsAWarningOnly() {
        val v = PaymentCard.validate("4111111111111111", "1025", "", today)
        assertTrue(v.expired)
        assertTrue(v.isValid)
        assertFalse(PaymentCard.validate("", "1126", "", today).expired) // current month still valid
    }

    @Test
    fun yearMonthFromEpoch() {
        assertEquals(YearMonth(1970, 1), PaymentCard.yearMonthOf(0))
        assertEquals(YearMonth(2026, 11), PaymentCard.yearMonthOf(1_794_700_800_000)) // 2026-11-14
        assertEquals(YearMonth(2024, 2), PaymentCard.yearMonthOf(1_709_164_800_000)) // 2024-02-29
    }
}
