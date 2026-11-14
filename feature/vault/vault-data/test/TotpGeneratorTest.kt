package dev.spaceapps.secvault.feature.vault.data.totp

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TotpGeneratorTest {
    private val generator = TotpGeneratorImpl()

    /** RFC 6238 appendix B, SHA-1 secret "12345678901234567890" (base32 below), 8 digits. */
    @Test
    fun matchesRfc6238Vectors() = runTest {
        val uri = "otpauth://totp/x?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&digits=8"
        val vectors = mapOf(59L to "94287082", 1111111109L to "07081804", 1234567890L to "89005924", 2000000000L to "69279037")
        for ((seconds, expected) in vectors) {
            assertEquals(expected, generator.generate(uri, seconds * 1000)?.code)
        }
    }

    @Test
    fun bareSecretUsesSixDigitsAndThirtySeconds() = runTest {
        val code = generator.generate("GEZD GNBV GY3T QOJQ GEZD GNBV GY3T QOJQ", 59_000)!!
        assertEquals("287082", code.code)
        assertEquals(1, code.secondsRemaining)
    }

    @Test
    fun invalidSecretGivesNull() = runTest {
        assertNull(generator.generate("not base32 !!", 0))
    }
}
