package dev.spaceapps.secvault.feature.generator.domain.usecase

import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GeneratePassphraseUseCaseTest {
    @Test
    fun passphraseHasRequestedWordsAndSeparator() =
        runTest {
            val random = object : SecureRandomSource {
                private val r = Random(7)

                override fun nextInt(until: Int) = r.nextInt(until)
            }
            val phrase = GeneratePassphraseUseCase(random) { listOf("maple", "orbit", "velvet", "harbor") }(
                PassphraseOptions(words = 5, includeNumber = true),
            )
            val parts = phrase.split("-")
            assertEquals(6, parts.size)
            assertTrue(parts.last().all { it.isDigit() })
            assertTrue(parts.any { it.first().isUpperCase() })
        }
}
