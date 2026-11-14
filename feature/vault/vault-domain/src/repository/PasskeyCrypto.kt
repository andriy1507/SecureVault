package dev.spaceapps.secvault.feature.vault.domain.repository

/** P-256 key pair of a new passkey. */
class PasskeyKeyPair(
    /** PKCS#8 DER. */
    val privateKey: ByteArray,
    /** Uncompressed point: 0x04 || x (32) || y (32). */
    val publicKeyRaw: ByteArray,
    /** SubjectPublicKeyInfo DER, which Android's response JSON includes. */
    val publicKeySpki: ByteArray,
)

/** Cryptography a passkey needs; implemented with the platform crypto provider. */
interface PasskeyCrypto {
    suspend fun generateKeyPair(): PasskeyKeyPair

    /** ECDSA P-256 with SHA-256, DER-encoded signature as WebAuthn expects. */
    suspend fun sign(privateKey: ByteArray, data: ByteArray): ByteArray

    suspend fun sha256(data: ByteArray): ByteArray

    fun randomBytes(size: Int): ByteArray
}
