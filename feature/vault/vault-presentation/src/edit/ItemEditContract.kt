package dev.spaceapps.secvault.feature.vault.presentation.edit

import dev.spaceapps.secvault.core.domain.model.CardValidation
import dev.spaceapps.secvault.core.domain.model.ItemDraft
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.domain.model.PasswordStrength
import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState

object ItemEditContract {
    enum class Field { Title, Username, Password, Url, Notes, Totp, CardholderName, CardNumber, CardExpiry, CardCvv }

    sealed interface Error {
        data object TitleRequired : Error

        data class SaveFailed(
            val reason: String?,
        ) : Error
    }

    data class State(
        val isLoading: Boolean = true,
        /** null: a new item. */
        val itemId: String? = null,
        val draft: ItemDraft = ItemDraft(),
        /** Extra websites for autofill, one per row (the first website is draft.url). */
        val extraUris: List<String> = emptyList(),
        val passwordVisible: Boolean = false,
        val cvvVisible: Boolean = false,
        val strength: PasswordStrength? = null,
        val isSaving: Boolean = false,
        val confirmTrash: Boolean = false,
        /** Card checks for the current draft; editors show them per field (see [showCardErrors]). */
        val cardValidation: CardValidation = CardValidation(),
        /** True after a save was refused, so every wrong card field is marked, not only finished ones. */
        val showCardErrors: Boolean = false,
        val error: Error? = null,
    ) : UiState {
        val isNew: Boolean get() = itemId == null

        override fun toString() = "ItemEditState(new=$isNew)"
    }

    sealed interface Intent : UiIntent {
        /** [id] == null: new item of [type], with optional prefilled fields (from search or a scanned 2FA code). */
        data class Load(
            val id: String?,
            val type: ItemType,
            val title: String,
            val username: String = "",
            val totpSecret: String = "",
        ) : Intent {
            override fun toString() = "Load(id=$id, type=$type)"
        }

        /** A 2FA QR scanned from the editor: fills the secret, and the name / username when still empty. */
        data class TotpScanned(
            val issuer: String,
            val account: String,
            val secret: String,
        ) : Intent {
            override fun toString() = "TotpScanned"
        }

        data class TypeChanged(
            val type: ItemType,
        ) : Intent

        data class FieldChanged(
            val field: Field,
            val value: String,
        ) : Intent

        data object AddUri : Intent

        data class UriChanged(
            val index: Int,
            val value: String,
        ) : Intent

        data class RemoveUri(
            val index: Int,
        ) : Intent

        data object ToggleFavorite : Intent

        data object TogglePasswordVisible : Intent

        data object ToggleCvvVisible : Intent

        data object GeneratePassword : Intent

        data object Save : Intent

        data object Trash : Intent

        data object ConfirmTrash : Intent

        data object DismissTrash : Intent

        data object Close : Intent
    }

    sealed interface Effect : UiEffect {
        data class Saved(
            val id: String,
            val isNew: Boolean,
        ) : Effect

        data class Trashed(
            val id: String,
            val title: String,
        ) : Effect

        data object Close : Effect
    }
}
