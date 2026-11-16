package dev.spaceapps.secvault.core.domain.model

data class PasswordOptions(
    val length: Int = DEFAULT_LENGTH,
    val uppercase: Boolean = true,
    val lowercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    /** Leave out look-alike characters (l, 1, I, O, 0). */
    val avoidAmbiguous: Boolean = false,
) {
    companion object {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 64
        const val DEFAULT_LENGTH = 18
    }
}

enum class PasswordStrength { Weak, Fair, Strong, VeryStrong }
