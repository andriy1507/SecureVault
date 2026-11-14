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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val checkDeviceSecurity: CheckDeviceSecurityUseCase,
    private val createVault: CreateVaultUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val pickConfirmationWords: PickConfirmationWordsUseCase,
    private val buildConfirmationChoices: BuildConfirmationChoicesUseCase,
    private val verifyConfirmationWords: VerifyConfirmationWordsUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    override fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.Start -> start()

            Intent.IdSeen -> setState { copy(step = Step.ShowPhrase, error = null) }

            is Intent.PhraseSavedChanged -> setState { copy(phraseSaved = intent.saved) }

            Intent.PhraseWrittenDown -> if (currentState.phraseSaved) startConfirmation()

            is Intent.ChoiceTapped -> setState {
                val slot = answers.indexOfFirst { it == null }
                if (slot < 0 || intent.word in answers) {
                    this
                } else {
                    copy(answers = answers.toMutableList().also { it[slot] = intent.word }, error = null)
                }
            }

            is Intent.SlotCleared -> setState {
                copy(
                    answers = answers.toMutableList().also {
                        if (intent.slot in
                            it.indices
                        ) {
                            it[intent.slot] = null
                        }
                    },
                    error = null,
                )
            }

            Intent.SubmitConfirmation -> submit()

            Intent.Back -> back()
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

    override fun onCleared() {
        setState { State() }
        super.onCleared()
    }
}
