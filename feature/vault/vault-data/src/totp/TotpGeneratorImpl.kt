package dev.spaceapps.secvault.feature.vault.data.totp

import dev.spaceapps.secvault.core.data.crypto.Hmac
import dev.spaceapps.secvault.core.data.crypto.HmacAlgorithm
import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode
import dev.spaceapps.secvault.feature.vault.domain.repository.TotpGenerator

/**
 * RFC 6238 TOTP. Accepts a bare base32 secret ("JBSW Y3DP EHPK 3PXP") or an
 * otpauth://totp/...?secret=...&digits=...&period=...&algorithm=... URI.
 */
internal class TotpGeneratorImpl : TotpGenerator {
    private data class Params(
        val key: ByteArray,
        val digits: Int,
        val period: Int,
        val algorithm: HmacAlgorithm,
    )

    override suspend fun generate(
        secret: String,
        nowEpochMillis: Long,
    ): TotpCode? {
        val params = parse(secret) ?: return null
        val seconds = nowEpochMillis / MILLIS_PER_SECOND
        val counter = seconds / params.period
        val message =
            ByteArray(COUNTER_BYTES) { i -> (counter ushr (BITS_PER_BYTE * (COUNTER_BYTES - 1 - i))).toByte() }
        val mac = Hmac.compute(params.algorithm, params.key, message)
        params.key.fill(0)
        val offset = mac.last().toInt() and LOW_NIBBLE
        val binary = ((mac[offset].toInt() and SIGN_MASK) shl 24) or
            ((mac[offset + 1].toInt() and BYTE_MASK) shl 16) or
            ((mac[offset + 2].toInt() and BYTE_MASK) shl 8) or
            (mac[offset + 3].toInt() and BYTE_MASK)
        val code = (binary % pow10(params.digits)).toString().padStart(params.digits, '0')
        val remaining = params.period - (seconds % params.period).toInt()
        return TotpCode(code, params.period, remaining)
    }

    private fun parse(input: String): Params? {
        val trimmed = input.trim()
        if (!trimmed.startsWith(OTPAUTH_SCHEME, ignoreCase = true)) {
            val key = Base32Decoder.decode(trimmed) ?: return null
            return Params(key, DEFAULT_DIGITS, DEFAULT_PERIOD, HmacAlgorithm.Sha1)
        }
        val query = trimmed
            .substringAfter('?', "")
            .split('&')
            .mapNotNull { part ->
                part.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].lowercase() to it[1] }
            }.toMap()
        val key = query["secret"]?.let(Base32Decoder::decode) ?: return null
        val digits = query["digits"]?.toIntOrNull()?.takeIf { it in VALID_DIGITS } ?: DEFAULT_DIGITS
        val period = query["period"]?.toIntOrNull()?.takeIf { it > 0 } ?: DEFAULT_PERIOD
        val algorithm = when (query["algorithm"]?.uppercase()) {
            "SHA256" -> HmacAlgorithm.Sha256
            "SHA512" -> HmacAlgorithm.Sha512
            else -> HmacAlgorithm.Sha1
        }
        return Params(key, digits, period, algorithm)
    }

    private fun pow10(n: Int): Int {
        var r = 1
        repeat(n) { r *= DECIMAL }
        return r
    }

    private companion object {
        const val OTPAUTH_SCHEME = "otpauth://"
        const val DEFAULT_DIGITS = 6
        const val DEFAULT_PERIOD = 30
        val VALID_DIGITS = 6..8
        const val MILLIS_PER_SECOND = 1000L
        const val COUNTER_BYTES = 8
        const val BITS_PER_BYTE = 8
        const val LOW_NIBBLE = 0x0F
        const val SIGN_MASK = 0x7F
        const val BYTE_MASK = 0xFF
        const val DECIMAL = 10
    }
}

/** RFC 4648 base32 decoding; ignores spaces, dashes, padding and case. */
internal object Base32Decoder {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val BITS_PER_CHAR = 5
    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFF

    fun decode(input: String): ByteArray? {
        val clean = input.uppercase().filterNot { it == ' ' || it == '-' || it == '=' }
        if (clean.isEmpty()) return null
        val out = ArrayList<Byte>(clean.length * BITS_PER_CHAR / BITS_PER_BYTE)
        var buffer = 0
        var bits = 0
        for (c in clean) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) return null
            buffer = (buffer shl BITS_PER_CHAR) or v
            bits += BITS_PER_CHAR
            if (bits >= BITS_PER_BYTE) {
                out += ((buffer shr (bits - BITS_PER_BYTE)) and BYTE_MASK).toByte()
                bits -= BITS_PER_BYTE
            }
        }
        return out.toByteArray()
    }
}
