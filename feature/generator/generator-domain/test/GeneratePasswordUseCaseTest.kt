package dev.spaceapps.secvault.feature.generator.domain.usecase

import dev.spaceapps.secvault.core.domain.security.SecureRandomSource
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordStrength
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class GeneratePasswordUseCaseTest {
    private val generator = GeneratePasswordUseCase(object : SecureRandomSource {
        private val r = Random(42)

        override fun nextInt(until: Int) = r.nextInt(until)
    })

    @Test
    fun containsEveryChosenSetAndRightLength() {
        repeat(200) {
            val p = generator(PasswordOptions(length = 12))
            assertEquals(12, p.length)
            assertTrue(
                p.any { it.isUpperCase() } && p.any { it.isLowerCase() } && p.any { it.isDigit() } &&
                    p.any { !it.isLetterOrDigit() },
                p,
            )
        }
    }

    @Test
    fun respectsDisabledSetsAndBounds() {
        val digitsOnly = generator(PasswordOptions(length = 3, uppercase = false, lowercase = false, symbols = false))
        assertEquals(PasswordOptions.MIN_LENGTH, digitsOnly.length)
        assertTrue(digitsOnly.all { it.isDigit() })
        assertEquals(PasswordOptions.MAX_LENGTH, generator(PasswordOptions(length = 500)).length)
    }

    @Test
    fun avoidAmbiguousLeavesOutLookAlikes() {
        repeat(100) {
            val p = generator(PasswordOptions(length = 64, avoidAmbiguous = true))
            assertTrue(p.none { it in "lI1O0o" }, p)
        }
    }

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

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        val estimate = EstimateStrengthUseCase()
        assertEquals(PasswordStrength.Weak, estimate("abc"))
        assertEquals(PasswordStrength.VeryStrong, estimate("k9#Vt2!qLx7&Rw4Zp"))
    }
}
