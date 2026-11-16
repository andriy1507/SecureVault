package dev.spaceapps.secvault.feature.generator.domain.model

enum class GeneratorMode { Password, Passphrase, Username }

enum class PassphraseSeparator(
    val value: String,
) {
    Hyphen("-"),
    Space(" "),
    Period("."),
    Underscore("_"),
    Comma(","),
}

data class PassphraseOptions(
    val words: Int = DEFAULT_WORDS,
    val separator: PassphraseSeparator = PassphraseSeparator.Hyphen,
    val capitalize: Boolean = true,
    val includeNumber: Boolean = true,
) {
    companion object {
        const val MIN_WORDS = 3
        const val MAX_WORDS = 10
        const val DEFAULT_WORDS = 4
    }
}

enum class UsernameType { RandomWords, RandomCharacters }

data class UsernameOptions(
    val type: UsernameType = UsernameType.RandomWords,
) {
    companion object {
        const val RANDOM_CHARACTERS_LENGTH = 8
        const val WORDS = 2

        /** Upper bound (exclusive) of the number appended to word usernames. */
        const val NUMBER_BOUND = 100
        const val SEPARATOR = "."
    }
}

/** A value made in this session (cleared when the vault locks). */
data class GeneratedValue(
    val value: String,
    val mode: GeneratorMode,
    val createdAt: Long,
) {
    override fun toString() = "GeneratedValue(mode=$mode)"
}
