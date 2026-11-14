package dev.spaceapps.secvault.feature.vault.domain.repository

import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode

/** RFC 6238 one-time codes. */
interface TotpGenerator {
    /** Null when [secret] is not a valid base32 secret or otpauth:// URI. */
    suspend fun generate(secret: String, nowEpochMillis: Long): TotpCode?
}
