package dev.spaceapps.secvault.feature.generator.presentation

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratedValue
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratorMode
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PassphraseSeparator
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordOptions
import dev.spaceapps.secvault.feature.generator.domain.model.PasswordStrength
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameType

object GeneratorContract {
    data class State(
        val mode: GeneratorMode = GeneratorMode.Password,
        val password: PasswordOptions = PasswordOptions(),
        val passphrase: PassphraseOptions = PassphraseOptions(),
        val usernameType: UsernameType = UsernameType.RandomWords,
        val value: String = "",
        /** Null for usernames (no strength shown). */
        val strength: PasswordStrength? = null,
        val history: List<GeneratedValue> = emptyList(),
        val showHistory: Boolean = false,
        val showSeparatorDialog: Boolean = false,
    ) : UiState {
        override fun toString() = "GeneratorState(mode=$mode)"
    }

    sealed interface Intent : UiIntent {
        data class ModeChanged(
            val mode: GeneratorMode,
        ) : Intent

        data class PasswordOptionsChanged(
            val options: PasswordOptions,
        ) : Intent

        data class PassphraseOptionsChanged(
            val options: PassphraseOptions,
        ) : Intent

        data class SeparatorChosen(
            val separator: PassphraseSeparator,
        ) : Intent

        data class UsernameTypeChanged(
            val type: UsernameType,
        ) : Intent

        data object Regenerate : Intent

        data object Copy : Intent

        data class CopyHistory(
            val index: Int,
        ) : Intent

        data class ShowHistory(
            val show: Boolean,
        ) : Intent

        data object ClearHistory : Intent

        data class ShowSeparatorDialog(
            val show: Boolean,
        ) : Intent
    }

    sealed interface Effect : UiEffect {
        data class Copy(
            val text: String,
            val mode: GeneratorMode,
        ) : Effect {
            override fun toString() = "Copy($mode)"
        }
    }
}
