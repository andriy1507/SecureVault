package dev.spaceapps.secvault.feature.autofill.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutofillMatcherTest {

    @Test
    fun webDomainOnlyTrustedFromBrowsers() {
        assertEquals(AutofillTarget.Website("example.com"), AutofillTarget.from("com.android.chrome", "www.example.com"))
        // A random app claiming a bank's domain is matched by its own package instead
        assertEquals(AutofillTarget.App("com.evil.app"), AutofillTarget.from("com.evil.app", "bank.com"))
    }

    @Test
    fun hostParsing() {
        assertEquals("example.com", UriMatcher.hostOf("https://user@WWW.Example.com:443/login?x=1"))
        assertEquals("example.com", UriMatcher.hostOf("example.com/path"))
        assertEquals(null, UriMatcher.hostOf("not a url"))
    }

    @Test
    fun websiteMatching() {
        val login = AutofillTarget.Website("login.example.com")
        assertTrue(UriMatcher.matches(login, "https://example.com"))
        assertTrue(UriMatcher.matches(AutofillTarget.Website("example.com"), "example.com"))
        assertFalse(UriMatcher.matches(AutofillTarget.Website("example.com"), "https://login.example.com"))
        assertFalse(UriMatcher.matches(AutofillTarget.Website("badexample.com"), "https://example.com"))
        assertFalse(UriMatcher.matches(AutofillTarget.Website("example.com"), "androidapp://example.com"))
    }

    @Test
    fun appMatchingIsExact() {
        val app = AutofillTarget.App("com.example.app")
        assertTrue(UriMatcher.matches(app, "androidapp://com.example.app"))
        assertFalse(UriMatcher.matches(app, "androidapp://com.example.app.evil"))
        assertFalse(UriMatcher.matches(app, "https://example.app"))
    }
}
