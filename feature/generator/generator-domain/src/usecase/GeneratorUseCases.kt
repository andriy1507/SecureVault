package dev.spaceapps.secvault.feature.generator.domain.usecase

import dev.spaceapps.secvault.core.domain.model.PasswordStrength
import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import dev.spaceapps.secvault.core.domain.usecase.PasswordCharsets
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratorMode
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameOptions
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameType
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorHistoryRepository
import dev.spaceapps.secvault.feature.generator.domain.repository.GeneratorWordsSource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.StateFlow

/** "maple-Orbit-velvet-harbor-7": random words from the word list, optionally a digit. */
class GeneratePassphraseUseCase(
    private val random: SecureRandomSource,
    private val words: GeneratorWordsSource,
) {
    suspend operator fun invoke(options: PassphraseOptions): String {
        val list = words.words()
        val count = options.words.coerceIn(PassphraseOptions.MIN_WORDS, PassphraseOptions.MAX_WORDS)
        val picked = MutableList(count) { list[random.nextInt(list.size)] }
        if (options.capitalize) {
            val index = random.nextInt(count)
            picked[index] = picked[index].replaceFirstChar { it.uppercaseChar() }
        }
        if (options.includeNumber) {
            picked += PasswordCharsets.DIGITS[random.nextInt(PasswordCharsets.DIGITS.length)].toString()
        }
        return picked.joinToString(options.separator.value)
    }
}

/** "quiet.otter.42" or "x7kq2mzp". */
class GenerateUsernameUseCase(
    private val random: SecureRandomSource,
    private val words: GeneratorWordsSource,
) {
    suspend operator fun invoke(options: UsernameOptions): String =
        when (options.type) {
            UsernameType.RandomWords -> {
                val list = words.words()
                val parts = List(UsernameOptions.WORDS) { list[random.nextInt(list.size)] } +
                    random.nextInt(UsernameOptions.NUMBER_BOUND).toString()
                parts.joinToString(UsernameOptions.SEPARATOR)
            }

            UsernameType.RandomCharacters -> {
                buildString {
                    repeat(
                        UsernameOptions.RANDOM_CHARACTERS_LENGTH,
                    ) { append(PasswordCharsets.USERNAME[random.nextInt(PasswordCharsets.USERNAME.length)]) }
                }
            }
        }
}

/** Words make passphrases strong even when the character pool is small, so they are rated by word count. */
fun passphraseStrength(options: PassphraseOptions): PasswordStrength =
    when {
        options.words <= PASSPHRASE_FAIR_WORDS -> PasswordStrength.Fair
        options.words <= PASSPHRASE_STRONG_WORDS -> PasswordStrength.Strong
        else -> PasswordStrength.VeryStrong
    }

private const val PASSPHRASE_FAIR_WORDS = 3
private const val PASSPHRASE_STRONG_WORDS = 4

class ObserveGeneratorHistoryUseCase(
    private val repository: GeneratorHistoryRepository,
) {
    operator fun invoke(): StateFlow<List<GeneratedValue>> = repository.history
}

class AddToGeneratorHistoryUseCase(
    private val repository: GeneratorHistoryRepository,
) {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(
        value: String,
        mode: GeneratorMode,
    ) = repository.add(GeneratedValue(value, mode, Clock.System.now().toEpochMilliseconds()))
}

class ClearGeneratorHistoryUseCase(
    private val repository: GeneratorHistoryRepository,
) {
    operator fun invoke() = repository.clear()
}
