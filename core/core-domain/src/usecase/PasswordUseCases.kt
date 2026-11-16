package dev.spaceapps.secvault.core.domain.usecase

import dev.spaceapps.secvault.core.domain.model.PasswordOptions
import dev.spaceapps.secvault.core.domain.model.PasswordStrength
import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import kotlin.math.ln

/** Character sets shared by the password, passphrase and username generators. */
object PasswordCharsets {
    const val UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWER = "abcdefghijklmnopqrstuvwxyz"
    const val DIGITS = "0123456789"
    const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"
    const val AMBIGUOUS = "lI1O0o"
    const val USERNAME = "abcdefghijkmnpqrstuvwxyz23456789"
}

/**
 * Uniform picks from a CSPRNG, at least one character from every chosen set,
 * then a Fisher-Yates shuffle so those guaranteed characters aren't always first.
 */
class GeneratePasswordUseCase(
    private val random: SecureRandomSource,
) {
    operator fun invoke(options: PasswordOptions): String {
        fun String.clean() = if (options.avoidAmbiguous) filterNot { it in PasswordCharsets.AMBIGUOUS } else this
        val sets = buildList {
            if (options.uppercase) add(PasswordCharsets.UPPER.clean())
            if (options.lowercase) add(PasswordCharsets.LOWER.clean())
            if (options.digits) add(PasswordCharsets.DIGITS.clean())
            if (options.symbols) add(PasswordCharsets.SYMBOLS)
        }.ifEmpty { listOf(PasswordCharsets.LOWER.clean()) }
        val length = options.length.coerceIn(maxOf(PasswordOptions.MIN_LENGTH, sets.size), PasswordOptions.MAX_LENGTH)
        val all = sets.joinToString("")

        val chars = CharArray(length)
        sets.forEachIndexed { i, set -> chars[i] = set.pick() }
        for (i in sets.size until length) chars[i] = all.pick()
        random.shuffle(chars)
        return chars.concatToString().also { chars.fill(' ') }
    }

    private fun String.pick(): Char = this[random.nextInt(length)]
}

private fun SecureRandomSource.shuffle(chars: CharArray) {
    for (i in chars.size - 1 downTo 1) {
        val j = nextInt(i + 1)
        val tmp = chars[i]
        chars[i] = chars[j]
        chars[j] = tmp
    }
}

/**
 * Rough strength from the size of the character pool and the length (entropy in bits).
 * Good enough for a hint under a generated or typed password.
 */
class EstimateStrengthUseCase {
    operator fun invoke(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.Weak
        var pool = 0
        if (password.any { it.isLowerCase() }) pool += PasswordCharsets.LOWER.length
        if (password.any { it.isUpperCase() }) pool += PasswordCharsets.UPPER.length
        if (password.any { it.isDigit() }) pool += PasswordCharsets.DIGITS.length
        if (password.any { !it.isLetterOrDigit() }) pool += PasswordCharsets.SYMBOLS.length
        val bits = password.length * ln(pool.toDouble()) / ln(2.0)
        return when {
            bits < FAIR_BITS -> PasswordStrength.Weak
            bits < STRONG_BITS -> PasswordStrength.Fair
            bits < VERY_STRONG_BITS -> PasswordStrength.Strong
            else -> PasswordStrength.VeryStrong
        }
    }

    private companion object {
        const val FAIR_BITS = 40.0
        const val STRONG_BITS = 60.0
        const val VERY_STRONG_BITS = 80.0
    }
}
