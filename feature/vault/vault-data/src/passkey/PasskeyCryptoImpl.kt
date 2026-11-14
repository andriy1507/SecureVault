package dev.spaceapps.secvault.feature.vault.data.passkey

import dev.spaceapps.secvault.feature.vault.domain.repository.PasskeyCrypto
import dev.spaceapps.secvault.feature.vault.domain.repository.PasskeyKeyPair
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDSA
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom

/** Passkey keys with the platform provider (JCA on Android, CryptoKit/CommonCrypto on iOS). */
internal class PasskeyCryptoImpl : PasskeyCrypto {
    private val provider = CryptographyProvider.Default
    private val ecdsa get() = provider.get(ECDSA)

    override suspend fun generateKeyPair(): PasskeyKeyPair {
        val pair = ecdsa.keyPairGenerator(EC.Curve.P256).generateKey()
        return PasskeyKeyPair(
            privateKey = pair.privateKey.encodeToByteArray(EC.PrivateKey.Format.DER),
            publicKeyRaw = pair.publicKey.encodeToByteArray(EC.PublicKey.Format.RAW),
            publicKeySpki = pair.publicKey.encodeToByteArray(EC.PublicKey.Format.DER),
        )
    }

    override suspend fun sign(
        privateKey: ByteArray,
        data: ByteArray,
    ): ByteArray =
        ecdsa
            .privateKeyDecoder(EC.Curve.P256)
            .decodeFromByteArray(EC.PrivateKey.Format.DER, privateKey)
            .signatureGenerator(SHA256, ECDSA.SignatureFormat.DER)
            .generateSignature(data)

    override suspend fun sha256(data: ByteArray): ByteArray = provider.get(SHA256).hasher().hash(data)

    override fun randomBytes(size: Int): ByteArray = CryptographyRandom.nextBytes(size)
}
