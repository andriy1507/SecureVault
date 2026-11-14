package dev.spaceapps.secvault.core.data.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HKDF
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.SHA1
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.random.CryptographyRandom

enum class HmacAlgorithm { Sha1, Sha256, Sha512 }

/** Public MAC for features that need one (TOTP, RFC 6238). Vault keys and ciphers stay internal. */
object Hmac {
    // SHA-1 is what most TOTP issuers use; HMAC-SHA1 is still sound for this.
    @OptIn(DelicateCryptographyApi::class)
    suspend fun compute(algorithm: HmacAlgorithm, key: ByteArray, data: ByteArray): ByteArray {
        val digest = when (algorithm) {
            HmacAlgorithm.Sha1 -> SHA1
            HmacAlgorithm.Sha256 -> SHA256
            HmacAlgorithm.Sha512 -> SHA512
        }
        return Crypto.provider.get(HMAC).keyDecoder(digest)
            .decodeFromByteArray(HMAC.Key.Format.RAW, key)
            .signatureGenerator()
            .generateSignature(data)
    }
}

/**
 * Thin wrapper over platform crypto (JCA on Android, CryptoKit on iOS) via cryptography-kotlin.
 * All primitives are available on Android API 30 and iOS: AES-256-GCM, SHA-256, HKDF-SHA256, P-256.
 */
internal object Crypto {
    val provider: CryptographyProvider get() = CryptographyProvider.Default

    private const val FORMAT_VERSION: Byte = 1

    fun randomBytes(size: Int): ByteArray = CryptographyRandom.nextBytes(size)

    fun newSymmetricKey(): ByteArray = randomBytes(32)

    suspend fun sha256(data: ByteArray): ByteArray = provider.get(SHA256).hasher().hash(data)

    suspend fun hkdf(input: ByteArray, info: String, size: Int = 32): ByteArray =
        provider.get(HKDF)
            .secretDerivation(SHA256, (size * 8).bits, salt = null, info = info.encodeToByteArray())
            .deriveSecretToByteArray(input)

    /** AES-256-GCM. Output: [version | 12-byte random IV | ciphertext | 16-byte tag]. */
    suspend fun encrypt(key: ByteArray, plain: ByteArray, ad: String): ByteArray {
        val cipher = aesKey(key).cipher()
        return byteArrayOf(FORMAT_VERSION) + cipher.encrypt(plain, ad.encodeToByteArray())
    }

    suspend fun decrypt(key: ByteArray, blob: ByteArray, ad: String): ByteArray {
        require(blob.size > 1 + 12 + 16 && blob[0] == FORMAT_VERSION) { "Unsupported format" }
        val cipher = aesKey(key).cipher()
        return cipher.decrypt(blob.copyOfRange(1, blob.size), ad.encodeToByteArray())
    }

    private suspend fun aesKey(raw: ByteArray): AES.GCM.Key {
        require(raw.size == 32) { "AES-256 key must be 32 bytes" }
        return provider.get(AES.GCM).keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, raw)
    }
}
