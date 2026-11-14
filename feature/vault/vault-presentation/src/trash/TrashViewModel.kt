package dev.spaceapps.secvault.feature.vault.presentation.trash

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.usecase.DeleteVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.EmptyTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ObserveTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.PurgeExpiredTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.TrashPolicy
import dev.spaceapps.secvault.feature.vault.domain.usecase.daysLeftInTrash
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.State
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.TrashedItem
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class TrashViewModel(
    private val observeTrash: ObserveTrashUseCase,
    private val restore: RestoreFromTrashUseCase,
    private val delete: DeleteVaultItemUseCase,
    private val emptyTrash: EmptyTrashUseCase,
    private val purgeExpired: PurgeExpiredTrashUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    init {
        viewModelScope.launch {
            var purged = false
            observeTrash().collect { items ->
                if (!purged) {
                    purged = true
                    purgeExpired(items)
                }
                val now = Clock.System.now().toEpochMilliseconds()
                val day = 1.days.inWholeMilliseconds
                setState {
                    copy(
                        isLoading = false,
                        items = items.map { summary ->
                            val trashedAt = summary.trashedAt ?: now
                            TrashedItem(
                                summary = summary,
                                daysAgo = ((now - trashedAt) / day).toInt().coerceIn(0, TrashPolicy.RETENTION_DAYS),
                                daysLeft = daysLeftInTrash(trashedAt, now),
                            )
                        },
                    )
                }
            }
        }
    }

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Restore -> {
                val title = currentState.items
                    .firstOrNull { it.summary.id == intent.id }
                    ?.summary
                    ?.title
                    .orEmpty()
                viewModelScope.launch {
                    restore(intent.id)
                    sendEffect(Effect.Restored(title))
                }
            }

            is Intent.Delete -> {
                setState { copy(confirmDelete = intent.item) }
            }

            Intent.ConfirmDelete -> {
                val item = currentState.confirmDelete ?: return
                setState { copy(confirmDelete = null) }
                viewModelScope.launch { delete(item.id) }
            }

            Intent.EmptyTrash -> {
                if (currentState.items.isNotEmpty()) setState { copy(confirmEmpty = true) }
            }

            Intent.ConfirmEmpty -> {
                val items = currentState.items.map { it.summary }
                setState { copy(confirmEmpty = false) }
                viewModelScope.launch { emptyTrash(items) }
            }

            Intent.Dismiss -> {
                setState { copy(confirmEmpty = false, confirmDelete = null) }
            }

            Intent.Back -> {
                sendEffect(Effect.Close)
            }
        }
    }
}
