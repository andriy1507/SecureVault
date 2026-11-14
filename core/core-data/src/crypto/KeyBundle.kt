package dev.spaceapps.secvault.core.data.crypto

import dev.whyoleg.cryptography.algorithms.EC
import dev.whyoleg.cryptography.algorithms.ECDH
import dev.whyoleg.cryptography.algorithms.ECDSA

/**
 * P-256 key pairs, stored encoded:
 * private keys as PKCS#8 DER, public keys as uncompressed RAW (65 bytes).
 * sign = ECDSA (auth, device list), agree = ECDH (receiving keys when pairing).
 */
internal class KeyPairSet(
    val signPrivate: ByteArray,
    val signPublic: ByteArray,
    val agreePrivate: ByteArray,
    val agreePublic: ByteArray,
) {
    fun fields(): List<ByteArray> = listOf(signPrivate, signPublic, agreePrivate, agreePublic)

    fun wipe() {
        signPrivate.fill(0)
        agreePrivate.fill(0)
    }

    companion object {
        suspend fun generate(): KeyPairSet {
            val sign = Crypto.provider.get(ECDSA).keyPairGenerator(EC.Curve.P256).generateKey()
            val agree = Crypto.provider.get(ECDH).keyPairGenerator(EC.Curve.P256).generateKey()
            return KeyPairSet(
                signPrivate = sign.privateKey.encodeToByteArray(EC.PrivateKey.Format.DER),
                signPublic = sign.publicKey.encodeToByteArray(EC.PublicKey.Format.RAW),
                agreePrivate = agree.privateKey.encodeToByteArray(EC.PrivateKey.Format.DER),
                agreePublic = agree.publicKey.encodeToByteArray(EC.PublicKey.Format.RAW),
            )
        }

        fun fromFields(f: List<ByteArray>, offset: Int) =
            KeyPairSet(f[offset], f[offset + 1], f[offset + 2], f[offset + 3])
    }
}

/** Everything sealed by the hardware key on this device. */
internal class KeyBundle(
    val identity: KeyPairSet, // account-level, same on all devices
    val device: KeyPairSet,   // unique per device, never restored
    val vaultKey: ByteArray,  // AES-256, wraps item keys
) {
    fun serialize(): ByteArray = Fields.write(FORMAT_VERSION, identity.fields() + device.fields() + listOf(vaultKey))

    fun wipe() {
        identity.wipe()
        device.wipe()
        vaultKey.fill(0)
    }

    companion object {
        private const val FORMAT_VERSION: Byte = 1

        fun deserialize(bytes: ByteArray): KeyBundle {
            val f = Fields.read(FORMAT_VERSION, bytes)
            require(f.size == 9) { "Corrupted key bundle" }
            return KeyBundle(KeyPairSet.fromFields(f, 0), KeyPairSet.fromFields(f, 4), f[8])
        }
    }
}

/** Simple binary layout: [version | (u16 length | bytes)*]. */
internal object Fields {
    fun write(version: Byte, fields: List<ByteArray>): ByteArray {
        var out = byteArrayOf(version)
        for (f in fields) {
            require(f.size <= 0xFFFF)
            out += byteArrayOf((f.size ushr 8).toByte(), f.size.toByte()) + f
        }
        return out
    }

    fun read(version: Byte, bytes: ByteArray): List<ByteArray> {
        require(bytes.isNotEmpty() && bytes[0] == version) { "Unsupported format" }
        val fields = mutableListOf<ByteArray>()
        var i = 1
        while (i < bytes.size) {
            require(i + 2 <= bytes.size) { "Corrupted data" }
            val len = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            i += 2
            require(i + len <= bytes.size) { "Corrupted data" }
            fields += bytes.copyOfRange(i, i + len)
            i += len
        }
        return fields
    }
}
