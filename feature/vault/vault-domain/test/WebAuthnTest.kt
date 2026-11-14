package dev.spaceapps.secvault.feature.vault.domain.passkey

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebAuthnTest {
    @Test
    fun base64UrlMatchesRfc4648WithoutPadding() {
        val vectors = mapOf(
            "" to "",
            "f" to "Zg",
            "fo" to "Zm8",
            "foo" to "Zm9v",
            "foob" to "Zm9vYg",
            "fooba" to "Zm9vYmE",
        )
        for ((plain, encoded) in vectors) {
            assertEquals(encoded, Base64Url.encode(plain.encodeToByteArray()))
            assertEquals(plain, Base64Url.decode(encoded).decodeToString())
        }
        val urlBytes = byteArrayOf(0xFB.toByte(), 0xFF.toByte())
        assertEquals("-_8", Base64Url.encode(urlBytes))
        assertContentEquals(urlBytes, Base64Url.decode("+/8=")) // standard alphabet and padding accepted
    }

    @Test
    fun cborEncodesIntsAndStrings() {
        fun hex(b: ByteArray) = b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        assertEquals("0a", hex(CborWriter().int(10).bytes()))
        assertEquals("1818", hex(CborWriter().int(24).bytes()))
        assertEquals("1903e8", hex(CborWriter().int(1000).bytes()))
        assertEquals("20", hex(CborWriter().int(-1).bytes()))
        assertEquals("26", hex(CborWriter().int(-7).bytes()))
        assertEquals("6466666f6f", hex(CborWriter().text("ffoo").bytes()))
        assertEquals("a0", hex(CborWriter().mapHeader(0).bytes()))
    }

    @Test
    fun coseKeyAndAuthenticatorDataLayout() {
        val point = ByteArray(65) { if (it == 0) 0x04 else it.toByte() }
        val cose = WebAuthn.coseKey(point)
        // {1: 2, 3: -7, -1: 1, -2: x(32), -3: y(32)}
        assertEquals(0xA5.toByte(), cose[0])
        assertEquals(77, cose.size)

        val rpHash = ByteArray(32) { 1 }
        val signIn = WebAuthn.authenticatorData(rpHash, WebAuthn.STANDARD_FLAGS)
        assertEquals(37, signIn.size)
        assertEquals(0x1D, signIn[32].toInt()) // UP | UV | BE | BS

        val credentialId = ByteArray(16) { 9 }
        val register = WebAuthn.authenticatorData(
            rpHash,
            WebAuthn.STANDARD_FLAGS or WebAuthn.FLAG_AT,
            credentialId,
            cose,
        )
        assertEquals(37 + 16 + 2 + 16 + cose.size, register.size)
        assertEquals(16, register[37 + 16 + 1].toInt()) // credential id length, low byte
    }

    @Test
    fun rpIdMatching() {
        assertTrue(matchesRpId("https://www.example.com/login", "example.com"))
        assertTrue(matchesRpId("accounts.example.com", "example.com"))
        assertFalse(matchesRpId("https://example.com.evil.io", "example.com"))
        assertFalse(matchesRpId("https://notexample.com", "example.com"))
    }
}
