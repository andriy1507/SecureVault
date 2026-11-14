package dev.spaceapps.secvault.feature.onboarding.presentation

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState

object OnboardingContract {
    enum class Step { Welcome, DeviceNotSecure, Creating, ShowId, ShowPhrase, ConfirmPhrase, Finishing }

    /** Steps shown with the "1 / 2 / 3" progress bar of the create-account flow. */
    val CREATE_STEPS = listOf(Step.ShowId, Step.ShowPhrase, Step.ConfirmPhrase)

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
        val error: Error? = null,
    ) : UiState {
        val canSubmitConfirmation: Boolean get() = answers.isNotEmpty() && answers.all { it != null }

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
    }

    sealed interface Effect : UiEffect {
        /** Ask the platform to open the lock-screen settings. */
        data object OpenSecuritySettings : Effect
    }
}
