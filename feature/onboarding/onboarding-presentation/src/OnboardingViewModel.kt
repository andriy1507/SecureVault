package dev.spaceapps.secvault.feature.onboarding.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.BuildConfirmationChoicesUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CheckDeviceSecurityUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CompleteOnboardingUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.CreateVaultUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.PickConfirmationWordsUseCase
import dev.spaceapps.secvault.feature.onboarding.domain.usecase.VerifyConfirmationWordsUseCase
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Effect
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Error
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Intent
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.State
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Step
import dev.spaceapps.secvault.feature.vault.domain.usecase.ReadBackupInfoUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromBackupUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val checkDeviceSecurity: CheckDeviceSecurityUseCase,
    private val createVault: CreateVaultUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val pickConfirmationWords: PickConfirmationWordsUseCase,
    private val buildConfirmationChoices: BuildConfirmationChoicesUseCase,
    private val verifyConfirmationWords: VerifyConfirmationWordsUseCase,
    private val readBackupInfo: ReadBackupInfoUseCase,
    private val restoreFromBackup: RestoreFromBackupUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    override fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.Start -> start()
            Intent.IdSeen -> setState { copy(step = Step.ShowPhrase, error = null) }
            is Intent.PhraseSavedChanged -> setState { copy(phraseSaved = intent.saved) }
            Intent.PhraseWrittenDown -> if (currentState.phraseSaved) startConfirmation()
            is Intent.ChoiceTapped -> choiceTapped(intent.word)
            is Intent.SlotCleared -> slotCleared(intent.slot)
            Intent.SubmitConfirmation -> submit()
            Intent.Back -> back()
            is Intent.RestoreIntent -> handleRestoreIntent(intent)
        }
    }

    private fun choiceTapped(word: String) {
        setState {
            val slot = answers.indexOfFirst { it == null }
            if (slot < 0 || word in answers) {
                this
            } else {
                copy(answers = answers.toMutableList().also { it[slot] = word }, error = null)
            }
        }
    }

    private fun slotCleared(slot: Int) {
        setState {
            copy(answers = answers.toMutableList().also { if (slot in it.indices) it[slot] = null }, error = null)
        }
    }

    private fun start() {
        if (currentState.step == Step.Creating) return
        if (!checkDeviceSecurity()) {
            setState { copy(step = Step.DeviceNotSecure, error = null) }
            sendEffect(Effect.OpenSecuritySettings)
            return
        }
        setState { copy(step = Step.Creating, error = null) }
        viewModelScope.launch {
            try {
                val creation = createVault()
                setState {
                    State(step = Step.ShowId, accountId = creation.accountId, words = creation.recoveryWords)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.DeviceNotSecure) {
                setState { copy(step = Step.DeviceNotSecure) }
            } catch (e: VaultError.AuthCancelled) {
                setState { copy(step = Step.Welcome, error = Error.UnlockNeeded) }
            } catch (e: Exception) {
                setState { copy(step = Step.Welcome, error = Error.CreateFailed(e.message)) }
            }
        }
    }

    private fun startConfirmation() {
        val indices = pickConfirmationWords(currentState.words.size)
        setState {
            copy(
                step = Step.ConfirmPhrase,
                confirmIndices = indices,
                choices = buildConfirmationChoices(words, indices),
                answers = List(indices.size) { null },
                error = null,
            )
        }
    }

    private fun back() {
        setState {
            when (step) {
                Step.ShowId, Step.DeviceNotSecure -> State()

                // the pending vault is replaced on the next Start
                Step.ShowPhrase -> copy(step = Step.ShowId, error = null)

                Step.ConfirmPhrase -> copy(step = Step.ShowPhrase, error = null)

                Step.RestorePickFile -> State()

                Step.RestorePhrase -> copy(step = Step.RestorePickFile, restorePhrase = "", error = null)

                else -> this
            }
        }
    }

    private fun submit() {
        val state = currentState
        if (!state.canSubmitConfirmation) return
        val answers = state.confirmIndices.zip(state.answers).associate { (index, word) -> index to word.orEmpty() }
        if (!verifyConfirmationWords(state.words, answers)) {
            setState { copy(answers = List(confirmIndices.size) { null }, error = Error.WordsDontMatch) }
            return
        }
        setState { copy(step = Step.Finishing, error = null) }
        viewModelScope.launch {
            try {
                completeOnboarding() // lock state becomes Unlocked; the app navigates away
                setState { State(step = Step.Finishing) } // drop the phrase from memory
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(step = Step.ConfirmPhrase, error = Error.FinishFailed(e.message)) }
            }
        }
    }

    // --- Restore from backup -------------------------------------------------------------------

    private fun handleRestoreIntent(intent: Intent.RestoreIntent) {
        when (intent) {
            Intent.StartRestore -> setState { State(step = Step.RestorePickFile) }
            is Intent.BackupPicked -> backupPicked(intent.content)
            is Intent.RestorePhraseChanged -> setState { copy(restorePhrase = intent.text, error = null) }
            Intent.SubmitRestore -> submitRestore()
        }
    }

    private fun backupPicked(content: String) {
        val info = try {
            readBackupInfo(content)
        } catch (expected: VaultError.UnsupportedBackup) {
            null
        }
        setState {
            if (info == null) {
                copy(backupContent = null, backupInfo = null, error = Error.BackupUnreadable)
            } else {
                copy(step = Step.RestorePhrase, backupContent = content, backupInfo = info, error = null)
            }
        }
    }

    private fun submitRestore() {
        val state = currentState
        val content = state.backupContent ?: return
        if (!state.canSubmitRestore || state.step == Step.Restoring) return
        if (!checkDeviceSecurity()) {
            setState { copy(error = Error.DeviceLockNeeded) }
            sendEffect(Effect.OpenSecuritySettings)
            return
        }
        setState { copy(step = Step.Restoring, error = null) }
        viewModelScope.launch {
            try {
                restoreFromBackup(content, state.restoreWords) // lock state becomes Unlocked; the app navigates away
                setState { State(step = Step.Restoring) } // drop the phrase from memory
            } catch (e: CancellationException) {
                throw e
            } catch (expected: Exception) {
                val error = restoreError(expected)
                setState {
                    copy(
                        step = if (error == Error.BackupUnreadable) Step.RestorePickFile else Step.RestorePhrase,
                        error = error,
                    )
                }
            }
        }
    }

    private fun restoreError(e: Exception): Error =
        when (e) {
            is VaultError.InvalidRecoveryPhrase -> Error.InvalidPhrase
            is VaultError.RecoveryPhraseMismatch -> Error.PhraseDoesNotMatch
            is VaultError.UnsupportedBackup -> Error.BackupUnreadable
            is VaultError.DeviceNotSecure -> Error.DeviceLockNeeded
            is VaultError.AuthCancelled -> Error.UnlockNeeded
            else -> Error.RestoreFailed(e.message)
        }

    override fun onCleared() {
        setState { State() }
        super.onCleared()
    }
}
