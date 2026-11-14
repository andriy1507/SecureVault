package dev.spaceapps.secvault.feature.vault.domain.passkey

/** Base64url without padding, the encoding WebAuthn JSON uses for binary values. */
object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val BITS_PER_CHAR = 6
    private const val BYTE_MASK = 0xFF
    private const val CHAR_MASK = 0x3F
    private const val BITS_PER_BYTE = 8

    fun encode(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl BITS_PER_BYTE) or (b.toInt() and BYTE_MASK)
            bits += BITS_PER_BYTE
            while (bits >= BITS_PER_CHAR) {
                bits -= BITS_PER_CHAR
                out.append(ALPHABET[(buffer shr bits) and CHAR_MASK])
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (BITS_PER_CHAR - bits)) and CHAR_MASK])
        return out.toString()
    }

    /** Accepts standard base64 and padding too; throws IllegalArgumentException on other characters. */
    fun decode(text: String): ByteArray {
        val out = ArrayList<Byte>(text.length * BITS_PER_CHAR / BITS_PER_BYTE)
        var buffer = 0
        var bits = 0
        for (c in text) {
            val value = when (c) {
                '=' -> continue
                '+' -> ALPHABET.indexOf('-')
                '/' -> ALPHABET.indexOf('_')
                else -> ALPHABET.indexOf(c)
            }
            require(value >= 0) { "Not base64url" }
            buffer = (buffer shl BITS_PER_CHAR) or value
            bits += BITS_PER_CHAR
            if (bits >= BITS_PER_BYTE) {
                bits -= BITS_PER_BYTE
                out += ((buffer shr bits) and BYTE_MASK).toByte()
            }
        }
        return out.toByteArray()
    }
}

/** The tiny part of CBOR (RFC 8949) WebAuthn needs: ints, byte/text strings and maps. */
internal class CborWriter {
    private val out = ArrayList<Byte>()

    fun bytes(): ByteArray = out.toByteArray()

    fun int(value: Long): CborWriter = apply {
        if (value >= 0) head(MAJOR_UNSIGNED, value) else head(MAJOR_NEGATIVE, -1 - value)
    }

    fun byteString(value: ByteArray): CborWriter = apply {
        head(MAJOR_BYTES, value.size.toLong())
        value.forEach { out += it }
    }

    fun text(value: String): CborWriter = apply {
        val utf8 = value.encodeToByteArray()
        head(MAJOR_TEXT, utf8.size.toLong())
        utf8.forEach { out += it }
    }

    fun mapHeader(size: Int): CborWriter = apply { head(MAJOR_MAP, size.toLong()) }

    private fun head(major: Int, value: Long) {
        val type = major shl MAJOR_SHIFT
        when {
            value < INLINE_LIMIT -> out += (type or value.toInt()).toByte()
            value <= UINT8_MAX -> { out += (type or ONE_BYTE).toByte(); out += value.toByte() }
            value <= UINT16_MAX -> { out += (type or TWO_BYTES).toByte(); bigEndian(value, 2) }
            value <= UINT32_MAX -> { out += (type or FOUR_BYTES).toByte(); bigEndian(value, 4) }
            else -> { out += (type or EIGHT_BYTES).toByte(); bigEndian(value, 8) }
        }
    }

    private fun bigEndian(value: Long, size: Int) {
        for (i in size - 1 downTo 0) out += (value shr (i * Byte.SIZE_BITS)).toByte()
    }

    private companion object {
        const val MAJOR_UNSIGNED = 0
        const val MAJOR_NEGATIVE = 1
        const val MAJOR_BYTES = 2
        const val MAJOR_TEXT = 3
        const val MAJOR_MAP = 5
        const val MAJOR_SHIFT = 5
        const val INLINE_LIMIT = 24
        const val ONE_BYTE = 24
        const val TWO_BYTES = 25
        const val FOUR_BYTES = 26
        const val EIGHT_BYTES = 27
        const val UINT8_MAX = 0xFFL
        const val UINT16_MAX = 0xFFFFL
        const val UINT32_MAX = 0xFFFFFFFFL
    }
}

/** WebAuthn authenticator data and attestation (Level 3), for a software passkey stored in the vault. */
object WebAuthn {
    /** User present. */
    const val FLAG_UP = 0x01
    /** User verified (device unlock or biometrics). */
    const val FLAG_UV = 0x04
    /** Backup eligible and backed up: the key lives in the vault (recovery key, later sync), not one device. */
    const val FLAG_BE = 0x08
    const val FLAG_BS = 0x10
    /** Attested credential data included (registration only). */
    const val FLAG_AT = 0x40

    /** COSE ES256 (ECDSA P-256 with SHA-256), the only algorithm we create. */
    const val COSE_ALG_ES256 = -7L

    /** All-zero AAGUID: no attestation of the authenticator model. */
    private val AAGUID = ByteArray(16)
    private const val COORDINATE_SIZE = 32
    private const val UNCOMPRESSED_POINT_SIZE = 1 + 2 * COORDINATE_SIZE
    private const val COSE_KTY = 1L
    private const val COSE_ALG = 3L
    private const val COSE_CRV = -1L
    private const val COSE_X = -2L
    private const val COSE_Y = -3L
    private const val COSE_KTY_EC2 = 2L
    private const val COSE_CRV_P256 = 1L
    private const val COSE_KEY_ENTRIES = 5
    private const val ATTESTATION_ENTRIES = 3
    private const val COUNTER_SIZE = 4
    private const val CRED_ID_LENGTH_SIZE = 2

    /** COSE_Key for a P-256 public key given as an uncompressed point. */
    fun coseKey(publicKeyRaw: ByteArray): ByteArray {
        require(publicKeyRaw.size == UNCOMPRESSED_POINT_SIZE) { "Expected an uncompressed P-256 point" }
        val x = publicKeyRaw.copyOfRange(1, 1 + COORDINATE_SIZE)
        val y = publicKeyRaw.copyOfRange(1 + COORDINATE_SIZE, UNCOMPRESSED_POINT_SIZE)
        return CborWriter().mapHeader(COSE_KEY_ENTRIES)
            .int(COSE_KTY).int(COSE_KTY_EC2)
            .int(COSE_ALG).int(COSE_ALG_ES256)
            .int(COSE_CRV).int(COSE_CRV_P256)
            .int(COSE_X).byteString(x)
            .int(COSE_Y).byteString(y)
            .bytes()
    }

    /**
     * rpIdHash || flags || signCount, plus attested credential data when [credentialId] is given.
     * The counter stays 0: synced passkeys can't keep a reliable count, and 0 tells sites not to check it.
     */
    fun authenticatorData(
        rpIdHash: ByteArray,
        flags: Int,
        credentialId: ByteArray? = null,
        coseKey: ByteArray? = null,
    ): ByteArray {
        val out = ArrayList<Byte>()
        rpIdHash.forEach { out += it }
        out += flags.toByte()
        repeat(COUNTER_SIZE) { out += 0 }
        if (credentialId != null && coseKey != null) {
            AAGUID.forEach { out += it }
            for (i in CRED_ID_LENGTH_SIZE - 1 downTo 0) out += (credentialId.size shr (i * Byte.SIZE_BITS)).toByte()
            credentialId.forEach { out += it }
            coseKey.forEach { out += it }
        }
        return out.toByteArray()
    }

    /** Attestation object with "none" attestation: sites learn nothing about the device. */
    fun noneAttestation(authenticatorData: ByteArray): ByteArray =
        CborWriter().mapHeader(ATTESTATION_ENTRIES)
            .text("fmt").text("none")
            .text("attStmt").mapHeader(0)
            .text("authData").byteString(authenticatorData)
            .bytes()

    /** Flags for every passkey we create or use: present, verified, backed up. */
    const val STANDARD_FLAGS = FLAG_UP or FLAG_UV or FLAG_BE or FLAG_BS
}

/** Whether a saved address belongs to [rpId]: same host or a subdomain of it. */
internal fun matchesRpId(uri: String, rpId: String): Boolean {
    val host = uri.trim().lowercase()
        .substringAfter("://")
        .substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfter('@').substringBefore(':')
        .removePrefix("www.")
    val rp = rpId.lowercase().removePrefix("www.")
    return host.isNotEmpty() && (host == rp || host.endsWith(".$rp") || rp.endsWith(".$host"))
}
