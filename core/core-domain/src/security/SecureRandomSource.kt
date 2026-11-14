package dev.spaceapps.secvault.core.domain.security

/** Cryptographically secure randomness (platform CSPRNG). */
interface SecureRandomSource {
    /** Uniform random int in [0, until). */
    fun nextInt(until: Int): Int
}
