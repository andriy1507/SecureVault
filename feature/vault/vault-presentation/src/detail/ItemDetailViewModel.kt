package dev.spaceapps.secvault.feature.vault.presentation.detail

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.passkey.DeletePasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GenerateTotpUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.MoveToTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ToggleFavoriteUseCase
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyRequest
import dev.spaceapps.secvault.feature.vault.presentation.common.valueOf
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ItemDetailViewModel(
    private val getItem: GetVaultItemUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val generateTotp: GenerateTotpUseCase,
    private val deletePasskey: DeletePasskeyUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {

    private var loadedId: String? = null
    private var totpJob: Job? = null

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Load -> if (intent.id != loadedId) {
                loadedId = intent.id
                reload()
            }
            Intent.TogglePasswordVisible -> setState { copy(passwordVisible = !passwordVisible) }
            Intent.ToggleCvvVisible -> setState { copy(cvvVisible = !cvvVisible) }
            is Intent.Copy -> copy(intent.field)
            is Intent.CopyHistory -> currentState.item?.passwordHistory?.getOrNull(intent.index)?.let {
                sendEffect(Effect.Copy(CopyRequest(it.password, CopyField.History)))
            }
            Intent.ToggleFavorite -> loadedId?.let { id ->
                viewModelScope.launch {
                    toggleFavorite(id)
                    reload()
                }
            }
            Intent.Edit -> loadedId?.let { sendEffect(Effect.Edit(it)) }
            Intent.OpenWebsite -> currentState.item?.url?.takeIf { it.isNotBlank() }?.let { sendEffect(Effect.OpenUrl(it)) }
            is Intent.ShowHistory -> setState { copy(showHistory = intent.show) }
            Intent.Trash -> setState { copy(confirmTrash = true) }
            Intent.DismissTrash -> setState { copy(confirmTrash = false) }
            Intent.ConfirmTrash -> {
                val item = currentState.item ?: return
                setState { copy(confirmTrash = false) }
                viewModelScope.launch {
                    moveToTrash(item.id)
                    sendEffect(Effect.Trashed(item.id, item.title))
                }
            }
            is Intent.DeletePasskey -> setState { copy(confirmDeletePasskey = intent.credentialId) }
            Intent.DismissDeletePasskey -> setState { copy(confirmDeletePasskey = null) }
            Intent.ConfirmDeletePasskey -> {
                val id = loadedId ?: return
                val credentialId = currentState.confirmDeletePasskey ?: return
                setState { copy(confirmDeletePasskey = null) }
                viewModelScope.launch {
                    deletePasskey(id, credentialId)
                    reload()
                }
            }
            Intent.Back -> sendEffect(Effect.Close)
        }
    }

    /** Also called when coming back from the editor. */
    fun reload() {
        val id = loadedId ?: return
        viewModelScope.launch {
            val item = runCatching { getItem(id) }.getOrNull()
            setState {
                if (item == null) State(isLoading = false, notFound = true)
                else copy(isLoading = false, notFound = false, item = item)
            }
            startTotp(item?.totpSecret.orEmpty())
        }
    }

    private fun startTotp(secret: String) {
        totpJob?.cancel()
        if (secret.isBlank()) {
            setState { copy(totp = null) }
            return
        }
        totpJob = viewModelScope.launch {
            while (isActive) {
                val code = generateTotp(secret)
                setState { copy(totp = code) }
                delay(TICK_MILLIS)
            }
        }
    }

    private fun copy(field: CopyField) {
        val state = currentState
        val text = if (field == CopyField.Totp) state.totp?.code.orEmpty() else state.item?.valueOf(field).orEmpty()
        if (text.isNotEmpty()) sendEffect(Effect.Copy(CopyRequest(text, field)))
    }

    override fun onCleared() {
        totpJob?.cancel()
        setState { State() } // drop decrypted data
        super.onCleared()
    }

    private companion object {
        const val TICK_MILLIS = 1000L
    }
}
