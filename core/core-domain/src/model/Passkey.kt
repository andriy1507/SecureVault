package dev.spaceapps.secvault.core.domain.model

/**
 * A WebAuthn passkey stored in the vault (encrypted with its login item).
 * Binary values are base64url without padding, as WebAuthn JSON uses them.
 */
data class Passkey(
    /** Random id the website stores to find this passkey. */
    val credentialId: String,
    /** The website's domain ("example.com"); the passkey only works there. */
    val rpId: String,
    val rpName: String,
    /** The website's own id for the account; returned on sign-in. */
    val userHandle: String,
    val userName: String,
    val userDisplayName: String,
    /** P-256 private key, PKCS#8 DER. */
    val privateKey: String,
    val createdAt: Long,
) {
    /** Never print key material to logs or crash reports. */
    override fun toString() = "Passkey(rpId=$rpId, createdAt=$createdAt)"
}
