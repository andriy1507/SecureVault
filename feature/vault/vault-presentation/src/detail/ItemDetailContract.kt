package dev.spaceapps.secvault.feature.vault.presentation.detail

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyRequest

object ItemDetailContract {

    data class State(
        val isLoading: Boolean = true,
        val notFound: Boolean = false,
        val item: VaultItem? = null,
        val totp: TotpCode? = null,
        val passwordVisible: Boolean = false,
        val cvvVisible: Boolean = false,
        val showHistory: Boolean = false,
        val confirmTrash: Boolean = false,
        /** Credential id of the passkey waiting for delete confirmation. */
        val confirmDeletePasskey: String? = null,
    ) : UiState {
        override fun toString() = "ItemDetailState(loading=$isLoading)" // no secrets in logs
    }

    sealed interface Intent : UiIntent {
        data class Load(val id: String) : Intent
        data object TogglePasswordVisible : Intent
        data object ToggleCvvVisible : Intent
        data class Copy(val field: CopyField) : Intent
        data class CopyHistory(val index: Int) : Intent
        data object ToggleFavorite : Intent
        data object Edit : Intent
        data object OpenWebsite : Intent
        data class ShowHistory(val show: Boolean) : Intent
        data object Trash : Intent
        data object ConfirmTrash : Intent
        data object DismissTrash : Intent
        data class DeletePasskey(val credentialId: String) : Intent
        data object ConfirmDeletePasskey : Intent
        data object DismissDeletePasskey : Intent
        data object Back : Intent
    }

    sealed interface Effect : UiEffect {
        data class Copy(val request: CopyRequest) : Effect
        data class Edit(val id: String) : Effect
        data class OpenUrl(val url: String) : Effect
        data class Trashed(val id: String, val title: String) : Effect
        data object Close : Effect
    }
}
