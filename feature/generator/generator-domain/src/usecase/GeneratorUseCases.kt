package dev.spaceapps.secvault.feature.generator.domain.usecase

import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratorMode
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordStrength
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameOptions
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameType
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorHistoryRepository
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorWordsSource
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.ln
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private object Charsets {
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
class GeneratePasswordUseCase(private val random: SecureRandomSource) {

    operator fun invoke(options: PasswordOptions): String {
        fun String.clean() = if (options.avoidAmbiguous) filterNot { it in Charsets.AMBIGUOUS } else this
        val sets = buildList {
            if (options.uppercase) add(Charsets.UPPER.clean())
            if (options.lowercase) add(Charsets.LOWER.clean())
            if (options.digits) add(Charsets.DIGITS.clean())
            if (options.symbols) add(Charsets.SYMBOLS)
        }.ifEmpty { listOf(Charsets.LOWER.clean()) }
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
        val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
    }
}

/** "maple-Orbit-velvet-harbor-7": random words from the word list, optionally a digit. */
class GeneratePassphraseUseCase(private val random: SecureRandomSource, private val words: GeneratorWordsSource) {
    suspend operator fun invoke(options: PassphraseOptions): String {
        val list = words.words()
        val count = options.words.coerceIn(PassphraseOptions.MIN_WORDS, PassphraseOptions.MAX_WORDS)
        val picked = MutableList(count) { list[random.nextInt(list.size)] }
        if (options.capitalize) {
            val index = random.nextInt(count)
            picked[index] = picked[index].replaceFirstChar { it.uppercaseChar() }
        }
        if (options.includeNumber) picked += Charsets.DIGITS[random.nextInt(Charsets.DIGITS.length)].toString()
        return picked.joinToString(options.separator.value)
    }
}

/** "quiet.otter.42" or "x7kq2mzp". */
class GenerateUsernameUseCase(private val random: SecureRandomSource, private val words: GeneratorWordsSource) {
    suspend operator fun invoke(options: UsernameOptions): String = when (options.type) {
        UsernameType.RandomWords -> {
            val list = words.words()
            val parts = List(UsernameOptions.WORDS) { list[random.nextInt(list.size)] } +
                random.nextInt(UsernameOptions.NUMBER_BOUND).toString()
            parts.joinToString(UsernameOptions.SEPARATOR)
        }
        UsernameType.RandomCharacters -> buildString {
            repeat(UsernameOptions.RANDOM_CHARACTERS_LENGTH) { append(Charsets.USERNAME[random.nextInt(Charsets.USERNAME.length)]) }
        }
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
        if (password.any { it.isLowerCase() }) pool += Charsets.LOWER.length
        if (password.any { it.isUpperCase() }) pool += Charsets.UPPER.length
        if (password.any { it.isDigit() }) pool += Charsets.DIGITS.length
        if (password.any { !it.isLetterOrDigit() }) pool += Charsets.SYMBOLS.length
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

/** Words make passphrases strong even when the character pool is small, so they are rated by word count. */
fun passphraseStrength(options: PassphraseOptions): PasswordStrength = when {
    options.words <= PASSPHRASE_FAIR_WORDS -> PasswordStrength.Fair
    options.words <= PASSPHRASE_STRONG_WORDS -> PasswordStrength.Strong
    else -> PasswordStrength.VeryStrong
}

private const val PASSPHRASE_FAIR_WORDS = 3
private const val PASSPHRASE_STRONG_WORDS = 4

class ObserveGeneratorHistoryUseCase(private val repository: GeneratorHistoryRepository) {
    operator fun invoke(): StateFlow<List<GeneratedValue>> = repository.history
}

class AddToGeneratorHistoryUseCase(private val repository: GeneratorHistoryRepository) {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(value: String, mode: GeneratorMode) =
        repository.add(GeneratedValue(value, mode, Clock.System.now().toEpochMilliseconds()))
}

class ClearGeneratorHistoryUseCase(private val repository: GeneratorHistoryRepository) {
    operator fun invoke() = repository.clear()
}
