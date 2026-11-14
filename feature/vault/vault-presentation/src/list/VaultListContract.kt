package dev.spaceapps.secvault.feature.vault.presentation.list

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.vault.domain.model.ItemFilter
import dev.spaceapps.secvault.feature.vault.domain.model.ItemSort
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItemSummary
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyRequest

object VaultListContract {
    enum class Sheet { None, NewItemType, Sort, ItemActions }

    data class State(
        val isLoading: Boolean = true,
        val query: String = "",
        val searchActive: Boolean = false,
        val filter: ItemFilter = ItemFilter.All,
        val sort: ItemSort = ItemSort.Name,
        val allItems: List<VaultItemSummary> = emptyList(),
        val favorites: List<VaultItemSummary> = emptyList(),
        val others: List<VaultItemSummary> = emptyList(),
        val sheet: Sheet = Sheet.None,
        /** Item the actions sheet / delete dialog is about. */
        val selected: VaultItemSummary? = null,
        val confirmTrash: Boolean = false,
        /** Live 2FA codes by item id, filled while the Codes filter is on. */
        val codes: Map<String, TotpCode> = emptyMap(),
    ) : UiState {
        val isEmptyVault: Boolean get() = !isLoading && allItems.isEmpty()
        val noResults: Boolean get() = !isLoading && allItems.isNotEmpty() && favorites.isEmpty() && others.isEmpty()
    }

    sealed interface Intent : UiIntent {
        data class QueryChanged(
            val query: String,
        ) : Intent

        data class SearchActiveChanged(
            val active: Boolean,
        ) : Intent

        data class FilterChanged(
            val filter: ItemFilter,
        ) : Intent

        data class SortChanged(
            val sort: ItemSort,
        ) : Intent

        data class ItemClicked(
            val id: String,
        ) : Intent

        data class ItemLongPressed(
            val item: VaultItemSummary,
        ) : Intent

        data class QuickCopy(
            val id: String,
        ) : Intent

        data object AddClicked : Intent

        data class NewItemTypeChosen(
            val type: ItemType,
        ) : Intent

        /** "Add “netflix”" on the no-results screen. */
        data object AddFromQuery : Intent

        data object SortClicked : Intent

        data object DismissSheet : Intent

        data class CopyFromActions(
            val field: dev.spaceapps.secvault.feature.vault.presentation.common.CopyField,
        ) : Intent

        data object EditSelected : Intent

        data object ToggleFavoriteSelected : Intent

        data object OpenWebsiteSelected : Intent

        data object TrashSelected : Intent

        data object ConfirmTrash : Intent

        data object DismissTrash : Intent

        data object TrashClicked : Intent

        /** "2FA code" in the add sheet, or "Scan QR code" on the empty Codes list. */
        data object ScanCodeClicked : Intent

        data class CopyCode(
            val id: String,
        ) : Intent

        data object LockClicked : Intent
    }

    sealed interface Effect : UiEffect {
        data class OpenItem(
            val id: String,
        ) : Effect

        data class EditItem(
            val id: String,
        ) : Effect

        data class NewItem(
            val type: ItemType,
            val title: String,
        ) : Effect

        data class Copy(
            val request: CopyRequest,
        ) : Effect

        data class OpenUrl(
            val url: String,
        ) : Effect

        data class Trashed(
            val id: String,
            val title: String,
        ) : Effect

        data object OpenTrash : Effect

        data object ScanCode : Effect

        data object Lock : Effect
    }
}
