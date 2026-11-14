package dev.spaceapps.secvault.feature.vault.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OtpAuthUriTest {
    @Test
    fun parsesGoogleAuthenticatorFormat() {
        val uri = "otpauth://totp/Example%20Co:alice%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example%20Co"
        val info = OtpAuthUri.parse(uri)!!
        assertEquals("Example Co", info.issuer)
        assertEquals("alice@example.com", info.account)
        assertEquals(uri, info.secret)
    }

    @Test
    fun issuerFromLabelWhenParamMissing() {
        assertEquals("GitHub", OtpAuthUri.parse("otpauth://totp/GitHub:dev?secret=ABC")!!.issuer)
    }

    @Test
    fun rejectsHotpAndMissingSecret() {
        assertNull(OtpAuthUri.parse("otpauth://hotp/x?secret=ABC&counter=1"))
        assertNull(OtpAuthUri.parse("otpauth://totp/x?issuer=y"))
        assertNull(OtpAuthUri.parse("https://example.com"))
    }
}
