package dev.spaceapps.secvault.feature.onboarding.presentation

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.vault.domain.model.BackupInfo
import dev.spaceapps.secvault.feature.vault.domain.usecase.BackupFormat

object OnboardingContract {
    enum class Step {
        Welcome,
        DeviceNotSecure,
        Creating,
        ShowId,
        ShowPhrase,
        ConfirmPhrase,
        Finishing,
        RestorePickFile,
        RestorePhrase,
        Restoring,
    }

    /** Steps shown with the "1 / 2 / 3" progress bar of the create-account flow. */
    val CREATE_STEPS = listOf(Step.ShowId, Step.ShowPhrase, Step.ConfirmPhrase)

    /** Steps of the restore-from-backup flow with a back button. */
    val RESTORE_STEPS = listOf(Step.RestorePickFile, Step.RestorePhrase)

    /** What went wrong; the UI turns it into a string resource. */
    sealed interface Error {
        data object UnlockNeeded : Error

        data class CreateFailed(
            val reason: String?,
        ) : Error

        data object WordsDontMatch : Error

        data class FinishFailed(
            val reason: String?,
        ) : Error

        /** Not a SecureVault backup, damaged, or too big. */
        data object BackupUnreadable : Error

        /** Not 24 valid recovery words. */
        data object InvalidPhrase : Error

        /** Valid words, but not this backup's account. */
        data object PhraseDoesNotMatch : Error

        /** Restoring needs a screen lock to protect the vault. */
        data object DeviceLockNeeded : Error

        data class RestoreFailed(
            val reason: String?,
        ) : Error
    }

    data class State(
        val step: Step = Step.Welcome,
        val accountId: String = "",
        val words: List<String> = emptyList(),
        val phraseSaved: Boolean = false,
        /** Word positions (0-based) to confirm, in order. */
        val confirmIndices: List<Int> = emptyList(),
        /** Chips to pick from. */
        val choices: List<String> = emptyList(),
        /** Picked word per confirm slot; null = empty slot. */
        val answers: List<String?> = emptyList(),
        /** Picked backup file (encrypted). */
        val backupContent: String? = null,
        val backupInfo: BackupInfo? = null,
        /** Recovery key as typed / pasted for restoring. */
        val restorePhrase: String = "",
        val error: Error? = null,
    ) : UiState {
        val canSubmitConfirmation: Boolean get() = answers.isNotEmpty() && answers.all { it != null }

        val restoreWords: List<String> get() = BackupFormat.parsePhrase(restorePhrase)

        val canSubmitRestore: Boolean
            get() = backupContent != null && restoreWords.size == BackupFormat.PHRASE_WORDS

        override fun toString() = "OnboardingState(step=$step)" // no recovery words in logs
    }

    sealed interface Intent : UiIntent {
        /** "Create new account" tapped, or retry after setting a device lock. */
        data object Start : Intent

        data object IdSeen : Intent

        data class PhraseSavedChanged(
            val saved: Boolean,
        ) : Intent

        data object PhraseWrittenDown : Intent

        data class ChoiceTapped(
            val word: String,
        ) : Intent

        data class SlotCleared(
            val slot: Int,
        ) : Intent

        data object SubmitConfirmation : Intent

        data object Back : Intent

        /** Steps of the restore-from-backup flow. */
        sealed interface RestoreIntent : Intent

        /** "Restore from backup" tapped. */
        data object StartRestore : RestoreIntent

        /** A file was picked (cancelled pickers send nothing). */
        class BackupPicked(
            val content: String,
        ) : RestoreIntent {
            override fun toString() = "BackupPicked"
        }

        class RestorePhraseChanged(
            val text: String,
        ) : RestoreIntent {
            override fun toString() = "RestorePhraseChanged" // no recovery words in logs
        }

        data object SubmitRestore : RestoreIntent
    }

    sealed interface Effect : UiEffect {
        /** Ask the platform to open the lock-screen settings. */
        data object OpenSecuritySettings : Effect
    }
}
