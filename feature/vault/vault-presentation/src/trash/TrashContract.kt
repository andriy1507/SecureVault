package dev.spaceapps.secvault.feature.vault.presentation.trash

import dev.spaceapps.secvault.core.domain.model.VaultItemSummary
import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState

object TrashContract {
    data class TrashedItem(
        val summary: VaultItemSummary,
        val daysAgo: Int,
        val daysLeft: Int,
    )

    data class State(
        val isLoading: Boolean = true,
        val items: List<TrashedItem> = emptyList(),
        val confirmEmpty: Boolean = false,
        /** Item waiting for "Delete forever" confirmation. */
        val confirmDelete: VaultItemSummary? = null,
    ) : UiState

    sealed interface Intent : UiIntent {
        data class Restore(
            val id: String,
        ) : Intent

        data class Delete(
            val item: VaultItemSummary,
        ) : Intent

        data object ConfirmDelete : Intent

        data object EmptyTrash : Intent

        data object ConfirmEmpty : Intent

        data object Dismiss : Intent

        data object Back : Intent
    }

    sealed interface Effect : UiEffect {
        data class Restored(
            val title: String,
        ) : Effect

        data object Close : Effect
    }
}
