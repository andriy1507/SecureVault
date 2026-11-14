package dev.spaceapps.secvault.core.data.security

import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import dev.whyoleg.cryptography.random.CryptographyRandom

internal class CryptoSecureRandom : SecureRandomSource {
    override fun nextInt(until: Int): Int = CryptographyRandom.nextInt(until)
}
