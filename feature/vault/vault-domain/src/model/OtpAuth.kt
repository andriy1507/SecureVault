package dev.spaceapps.secvault.feature.vault.domain.model

/** What a 2FA QR code tells us: who issued it, for which account, and the secret (kept as the full URI). */
data class OtpAuthInfo(val issuer: String, val account: String, val secret: String) {
    override fun toString() = "OtpAuthInfo(issuer=$issuer)" // never log the secret
}

/**
 * Parses `otpauth://totp/Issuer:account?secret=...&issuer=...` (Google Authenticator key URI format).
 * Only TOTP is supported; HOTP (counter-based) links are rejected.
 */
object OtpAuthUri {
    private const val SCHEME = "otpauth://"
    private const val TOTP_TYPE = "totp/"
    private const val ISSUER_SEPARATOR = ':'
    private const val PERCENT = '%'
    private const val HEX_RADIX = 16
    private const val PARAM_SECRET = "secret"
    private const val PARAM_ISSUER = "issuer"

    fun isOtpAuth(text: String): Boolean = text.trim().startsWith(SCHEME, ignoreCase = true)

    fun parse(text: String): OtpAuthInfo? {
        val uri = text.trim()
        if (!isOtpAuth(uri)) return null
        val afterScheme = uri.substring(SCHEME.length)
        if (!afterScheme.startsWith(TOTP_TYPE, ignoreCase = true)) return null
        val label = decode(afterScheme.substring(TOTP_TYPE.length).substringBefore('?'))
        val params = afterScheme.substringAfter('?', "").split('&')
            .mapNotNull { part -> part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].lowercase() to decode(it[1]) } }
            .toMap()
        if (params[PARAM_SECRET].isNullOrBlank()) return null
        val labelIssuer = label.substringBefore(ISSUER_SEPARATOR, "").trim()
        val account = label.substringAfter(ISSUER_SEPARATOR).trim()
        val issuer = params[PARAM_ISSUER]?.trim().orEmpty().ifEmpty { labelIssuer }
        return OtpAuthInfo(issuer = issuer, account = account, secret = uri)
    }

    /** Percent-decoding (UTF-8) plus '+' as space, enough for otpauth labels. */
    private fun decode(value: String): String {
        val bytes = ArrayList<Byte>(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == PERCENT && i + 2 <= value.lastIndex) {
                val hex = value.substring(i + 1, i + 3).toIntOrNull(HEX_RADIX)
                if (hex != null) {
                    bytes += hex.toByte()
                    i += 3
                    continue
                }
            }
            val ch = if (c == '+') ' ' else c
            ch.toString().encodeToByteArray().forEach { bytes += it }
            i++
        }
        return bytes.toByteArray().decodeToString()
    }
}
