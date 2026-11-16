package dev.spaceapps.secvault.feature.vault.presentation.edit

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.domain.model.CardValidation
import dev.spaceapps.secvault.core.domain.model.ItemDraft
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.PasswordOptions
import dev.spaceapps.secvault.core.domain.model.PaymentCard
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.model.toDraft
import dev.spaceapps.secvault.core.domain.usecase.EstimateStrengthUseCase
import dev.spaceapps.secvault.core.domain.usecase.GeneratePasswordUseCase
import dev.spaceapps.secvault.core.domain.usecase.SaveVaultItemUseCase
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.MoveToTrashUseCase
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Error
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Field
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditContract.State
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ItemEditViewModel(
    private val getItem: GetVaultItemUseCase,
    private val saveItem: SaveVaultItemUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val generatePassword: GeneratePasswordUseCase,
    private val estimateStrength: EstimateStrengthUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    private var loaded = false
    private var existing: VaultItem? = null

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Load -> load(intent)

            is Intent.TypeChanged -> if (currentState.isNew) setState { copy(draft = draft.copy(type = intent.type)) }

            is Intent.FieldChanged -> setState { withDraft(draft.with(intent.field, intent.value)).copy(error = null) }

            Intent.AddUri -> setState { copy(extraUris = extraUris + "") }

            is Intent.UriChanged -> setState {
                copy(
                    extraUris = extraUris.toMutableList().also {
                        if (intent.index in
                            it.indices
                        ) {
                            it[intent.index] = intent.value
                        }
                    },
                )
            }

            is Intent.RemoveUri -> setState {
                copy(
                    extraUris = extraUris.toMutableList().also {
                        if (intent.index in
                            it.indices
                        ) {
                            it.removeAt(intent.index)
                        }
                    },
                )
            }

            is Intent.TotpScanned -> setState {
                copy(
                    draft = draft.copy(
                        totpSecret = intent.secret,
                        title = draft.title.ifBlank { intent.issuer },
                        username = draft.username.ifBlank { intent.account },
                    ),
                )
            }

            Intent.ToggleFavorite -> setState { copy(draft = draft.copy(favorite = !draft.favorite)) }

            Intent.TogglePasswordVisible -> setState { copy(passwordVisible = !passwordVisible) }

            Intent.ToggleCvvVisible -> setState { copy(cvvVisible = !cvvVisible) }

            Intent.GeneratePassword -> setState {
                withDraft(draft.copy(password = generatePassword(PasswordOptions()))).copy(passwordVisible = true)
            }

            Intent.Save -> save()

            Intent.Trash -> setState { copy(confirmTrash = true) }

            Intent.DismissTrash -> setState { copy(confirmTrash = false) }

            Intent.ConfirmTrash -> trash()

            Intent.Close -> sendEffect(Effect.Close)
        }
    }

    private fun load(intent: Intent.Load) {
        if (loaded) return
        loaded = true
        if (intent.id == null) {
            val draft =
                ItemDraft(
                    type = intent.type,
                    title = intent.title,
                    username = intent.username,
                    totpSecret = intent.totpSecret,
                )
            setState { State(isLoading = false, draft = draft) }
            return
        }
        viewModelScope.launch {
            val item = runCatching { getItem(intent.id) }.getOrNull()
            existing = item
            if (item == null) {
                sendEffect(Effect.Close)
                return@launch
            }
            // Editors work on digits only; separators are drawn by the UI.
            val draft = item.toDraft().let {
                it.copy(
                    cardNumber = PaymentCard.sanitizeNumber(it.cardNumber),
                    cardExpiry = PaymentCard.sanitizeExpiry(it.cardExpiry),
                )
            }
            setState {
                State(
                    isLoading = false,
                    itemId = item.id,
                    draft = draft.copy(uris = emptyList()),
                    extraUris = item.uris,
                ).withDraft(draft.copy(uris = emptyList()))
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun State.withDraft(draft: ItemDraft) =
        copy(
            draft = draft,
            strength = draft.password.takeIf { it.isNotEmpty() }?.let { estimateStrength(it) },
            cardValidation = if (draft.type == ItemType.Card) {
                val today = PaymentCard.yearMonthOf(Clock.System.now().toEpochMilliseconds())
                PaymentCard.validate(draft.cardNumber, draft.cardExpiry, draft.cardCvv, today)
            } else {
                CardValidation()
            },
        )

    private fun save() {
        val state = currentState
        if (state.isSaving) return
        setState { copy(isSaving = true, error = null) }
        viewModelScope.launch {
            try {
                val draft = state.draft.copy(uris = state.extraUris)
                when (val result = saveItem(existing, draft)) {
                    SaveVaultItemUseCase.Result.TitleRequired -> setState {
                        copy(
                            isSaving = false,
                            error = Error.TitleRequired,
                        )
                    }

                    is SaveVaultItemUseCase.Result.InvalidCard -> setState {
                        copy(isSaving = false, cardValidation = result.validation, showCardErrors = true)
                    }

                    is SaveVaultItemUseCase.Result.Saved -> sendEffect(Effect.Saved(result.id, state.isNew))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { copy(isSaving = false, error = Error.SaveFailed(e.message)) }
            }
        }
    }

    private fun trash() {
        val item = existing ?: return
        setState { copy(confirmTrash = false) }
        viewModelScope.launch {
            moveToTrash(item.id)
            sendEffect(Effect.Trashed(item.id, item.title))
        }
    }

    override fun onCleared() {
        existing = null
        setState { State() } // drop decrypted data
        super.onCleared()
    }

    private fun ItemDraft.with(
        field: Field,
        value: String,
    ) = when (field) {
        Field.Title -> copy(title = value)

        Field.Username -> copy(username = value)

        Field.Password -> copy(password = value)

        Field.Url -> copy(url = value)

        Field.Notes -> copy(notes = value)

        Field.Totp -> copy(totpSecret = value)

        Field.CardholderName -> copy(cardholderName = value)

        Field.CardNumber -> PaymentCard.sanitizeNumber(value).let { number ->
            copy(cardNumber = number, cardCvv = PaymentCard.sanitizeCvv(cardCvv, number))
        }

        Field.CardExpiry -> copy(cardExpiry = PaymentCard.sanitizeExpiry(value))

        Field.CardCvv -> copy(cardCvv = PaymentCard.sanitizeCvv(value, cardNumber))
    }
}
