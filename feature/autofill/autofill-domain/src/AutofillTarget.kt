package dev.spaceapps.secvault.feature.autofill.domain

/** What a form belongs to: a website (only when a trusted browser says so) or an app. */
sealed interface AutofillTarget {
    data class Website(val host: String) : AutofillTarget
    data class App(val packageName: String) : AutofillTarget

    /** How the target is stored in VaultItem.uris. */
    val uri: String
        get() = when (this) {
            is Website -> "https://$host"
            is App -> "androidapp://$packageName"
        }

    /** Suggested title for a newly saved login. */
    val title: String
        get() = when (this) {
            is Website -> host
            is App -> packageName.split('.').lastOrNull { it.isNotBlank() }?.replaceFirstChar { it.uppercase() } ?: packageName
        }

    companion object {
        /**
         * Any app can put a WebView on screen and claim to show "bank.com", so a web domain is only
         * trusted when it comes from a known browser. Otherwise the form is matched by its app package.
         */
        fun from(packageName: String, webDomain: String?): AutofillTarget {
            val host = webDomain?.let(UriMatcher::hostOf)
            return if (host != null && packageName in TrustedBrowsers.packages) Website(host) else App(packageName)
        }
    }
}

object TrustedBrowsers {
    val packages = setOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
        "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fenix", "org.mozilla.focus",
        "com.microsoft.emmx", "com.brave.browser", "com.brave.browser_beta",
        "com.sec.android.app.sbrowser", "com.vivaldi.browser", "com.duckduckgo.mobile.android",
        "com.opera.browser", "com.opera.mini.native", "com.kiwibrowser.browser", "org.chromium.chrome",
        "com.google.android.apps.chrome", "app.vanadium.browser", "org.mozilla.fennec_fdroid",
        "com.ecosia.android", "org.torproject.torbrowser",
    )
}

object UriMatcher {
    /** "https://www.Example.com:443/login" -> "example.com". Null if it has no host. */
    fun hostOf(value: String): String? {
        val withoutScheme = value.trim().substringAfter("://", value.trim())
        val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase().removePrefix("www.")
        return host.takeIf { it.contains('.') && it.none(Char::isWhitespace) }
    }

    /**
     * A saved site matches its own host and its subdomains ("example.com" fills "login.example.com"),
     * never the other way round, and never just by sharing a suffix ("badexample.com" gets nothing).
     */
    fun matches(target: AutofillTarget, savedUri: String): Boolean = when (target) {
        is AutofillTarget.App -> savedUri.trim() == target.uri
        is AutofillTarget.Website -> {
            if (savedUri.trim().startsWith("androidapp://")) false
            else hostOf(savedUri)?.let { saved -> target.host == saved || target.host.endsWith(".$saved") } ?: false
        }
    }
}
