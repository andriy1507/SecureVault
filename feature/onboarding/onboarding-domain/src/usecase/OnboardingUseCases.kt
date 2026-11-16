package dev.spaceapps.secvault.feature.onboarding.domain.usecase

import dev.spaceapps.secvault.core.domain.model.BackupInfo
import dev.spaceapps.secvault.core.domain.model.VaultCreation
import dev.spaceapps.secvault.core.domain.repository.BackupRestorer
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.core.domain.security.SecureRandomSource

class CheckDeviceSecurityUseCase(
    private val repository: VaultKeysRepository,
) {
    operator fun invoke(): Boolean = repository.isDeviceSecure()
}

class CreateVaultUseCase(
    private val repository: VaultKeysRepository,
) {
    suspend operator fun invoke(): VaultCreation = repository.createVault()
}

class CompleteOnboardingUseCase(
    private val repository: VaultKeysRepository,
) {
    suspend operator fun invoke() = repository.completeOnboarding()
}

/** How the user proves they saved the recovery key: tap the right words among a few decoys. */
object ConfirmationPolicy {
    const val WORDS_TO_CONFIRM = 3
    const val DECOYS = 3
}

/** Picks which words (by index) the user must confirm to prove they wrote the phrase down. */
class PickConfirmationWordsUseCase(
    private val random: SecureRandomSource,
) {
    operator fun invoke(
        totalWords: Int,
        count: Int = ConfirmationPolicy.WORDS_TO_CONFIRM,
    ): List<Int> =
        (0 until totalWords)
            .toList()
            .shuffled(random)
            .take(count)
            .sorted()
}

/** Fisher-Yates shuffle over a CSPRNG. */
private fun <T> List<T>.shuffled(random: SecureRandomSource): List<T> {
    val result = toMutableList()
    for (i in result.size - 1 downTo 1) {
        val j = random.nextInt(i + 1)
        val tmp = result[i]
        result[i] = result[j]
        result[j] = tmp
    }
    return result
}

/** Word chips to choose from: the words asked for plus decoys from the same phrase, shuffled. */
class BuildConfirmationChoicesUseCase(
    private val random: SecureRandomSource,
) {
    operator fun invoke(
        words: List<String>,
        askedIndices: List<Int>,
        decoys: Int = ConfirmationPolicy.DECOYS,
    ): List<String> {
        val asked = askedIndices.map { words[it] }
        val others = words
            .filterIndexed {
                i,
                w,
                ->
                i !in askedIndices && w !in asked
            }.distinct()
            .shuffled(random)
            .take(decoys)
        return (asked.distinct() + others).shuffled(random)
    }
}

/** Header of a picked backup file; throws VaultError.UnsupportedBackup when it isn't a backup. */
class ReadRestoreInfoUseCase(
    private val restorer: BackupRestorer,
) {
    operator fun invoke(content: String): BackupInfo = restorer.readInfo(content)
}

/** Restores the backup's account and items on this device (a new device's first launch). */
class RestoreAccountUseCase(
    private val restorer: BackupRestorer,
) {
    suspend operator fun invoke(
        content: String,
        words: List<String>,
    ) = restorer.restore(content, words)
}

/** Compares typed words against the phrase, ignoring case and surrounding spaces. */
class VerifyConfirmationWordsUseCase {
    operator fun invoke(
        words: List<String>,
        answers: Map<Int, String>,
    ): Boolean =
        answers.isNotEmpty() && answers.all { (index, typed) ->
            words.getOrNull(index)?.equals(typed.trim(), ignoreCase = true) == true
        }
}
