package dev.spaceapps.secvault.feature.generator.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.generator.domain.model.GeneratorMode
import dev.spaceapps.secvault.feature.generator.domain.model.UsernameOptions
import dev.spaceapps.secvault.feature.generator.domain.usecase.AddToGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.ClearGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.EstimateStrengthUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.GeneratePassphraseUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.GeneratePasswordUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.GenerateUsernameUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.ObserveGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.generator.domain.usecase.passphraseStrength
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.Effect
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.Intent
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorContract.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class GeneratorViewModel(
    private val generatePassword: GeneratePasswordUseCase,
    private val generatePassphrase: GeneratePassphraseUseCase,
    private val generateUsername: GenerateUsernameUseCase,
    private val estimateStrength: EstimateStrengthUseCase,
    observeHistory: ObserveGeneratorHistoryUseCase,
    private val addToHistory: AddToGeneratorHistoryUseCase,
    private val clearHistory: ClearGeneratorHistoryUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {

    private var generateJob: Job? = null

    init {
        viewModelScope.launch { observeHistory().collect { setState { copy(history = it) } } }
        regenerate()
    }

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.ModeChanged -> {
                setState { copy(mode = intent.mode) }
                regenerate()
            }
            is Intent.PasswordOptionsChanged -> {
                setState { copy(password = intent.options) }
                regenerate()
            }
            is Intent.PassphraseOptionsChanged -> {
                setState { copy(passphrase = intent.options) }
                regenerate()
            }
            is Intent.SeparatorChosen -> {
                setState { copy(passphrase = passphrase.copy(separator = intent.separator), showSeparatorDialog = false) }
                regenerate()
            }
            is Intent.UsernameTypeChanged -> {
                setState { copy(usernameType = intent.type) }
                regenerate()
            }
            Intent.Regenerate -> regenerate()
            Intent.Copy -> currentState.value.takeIf { it.isNotEmpty() }?.let { value ->
                addToHistory(value, currentState.mode)
                sendEffect(Effect.Copy(value, currentState.mode))
            }
            is Intent.CopyHistory -> currentState.history.getOrNull(intent.index)?.let { sendEffect(Effect.Copy(it.value, it.mode)) }
            is Intent.ShowHistory -> setState { copy(showHistory = intent.show) }
            Intent.ClearHistory -> {
                clearHistory()
                setState { copy(showHistory = false) }
            }
            is Intent.ShowSeparatorDialog -> setState { copy(showSeparatorDialog = intent.show) }
        }
    }

    private fun regenerate() {
        generateJob?.cancel()
        val state = currentState
        generateJob = viewModelScope.launch {
            val (value, strength) = when (state.mode) {
                GeneratorMode.Password -> generatePassword(state.password).let { it to estimateStrength(it) }
                GeneratorMode.Passphrase -> generatePassphrase(state.passphrase) to passphraseStrength(state.passphrase)
                GeneratorMode.Username -> generateUsername(UsernameOptions(state.usernameType)) to null
            }
            setState { copy(value = value, strength = strength) }
        }
    }

    override fun onCleared() {
        setState { State() }
        super.onCleared()
    }
}
