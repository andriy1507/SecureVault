package dev.spaceapps.secvault.feature.vault.presentation.list

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.model.ItemFilter
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.usecase.GenerateTotpUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultItemUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.MoveToTrashUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ObserveVaultItemsUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ToggleFavoriteUseCase
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyRequest
import dev.spaceapps.secvault.feature.vault.presentation.common.quickCopy
import dev.spaceapps.secvault.feature.vault.presentation.common.valueOf
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Sheet
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.State
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VaultListViewModel(
    private val observeItems: ObserveVaultItemsUseCase,
    private val getItem: GetVaultItemUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val generateTotp: GenerateTotpUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {

    private var codesJob: Job? = null

    init {
        viewModelScope.launch {
            observeItems().collect { items ->
                setState { copy(isLoading = false, allItems = items).recomputed() }
                updateCodeTicker()
            }
        }
    }

    /** While the Codes filter is shown, refreshes every code once a second. Secrets stay in this job only. */
    private fun updateCodeTicker() {
        codesJob?.cancel()
        if (currentState.filter != ItemFilter.Codes) {
            setState { copy(codes = emptyMap()) }
            return
        }
        val ids = currentState.allItems.filter { it.hasTotp }.map { it.id }
        codesJob = viewModelScope.launch {
            val secrets = ids.mapNotNull { id -> getItem(id)?.totpSecret?.takeIf { it.isNotBlank() }?.let { id to it } }
            while (isActive) {
                val codes = secrets.mapNotNull { (id, secret) -> generateTotp(secret)?.let { id to it } }.toMap()
                setState { copy(codes = codes) }
                delay(CODE_TICK_MILLIS)
            }
        }
    }

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.QueryChanged -> setState { copy(query = intent.query).recomputed() }
            is Intent.SearchActiveChanged -> setState {
                copy(searchActive = intent.active, query = if (intent.active) query else "").recomputed()
            }
            is Intent.FilterChanged -> {
                setState { copy(filter = intent.filter).recomputed() }
                updateCodeTicker()
            }
            is Intent.SortChanged -> setState { copy(sort = intent.sort, sheet = Sheet.None).recomputed() }
            is Intent.ItemClicked -> sendEffect(Effect.OpenItem(intent.id))
            is Intent.ItemLongPressed -> setState { copy(sheet = Sheet.ItemActions, selected = intent.item) }
            is Intent.QuickCopy -> withItem(intent.id) { item -> item.quickCopy()?.let { sendEffect(Effect.Copy(it)) } }
            Intent.AddClicked -> setState { copy(sheet = Sheet.NewItemType) }
            is Intent.NewItemTypeChosen -> {
                setState { copy(sheet = Sheet.None) }
                sendEffect(Effect.NewItem(intent.type, ""))
            }
            Intent.AddFromQuery -> sendEffect(Effect.NewItem(ItemType.Login, currentState.query.trim()))
            Intent.SortClicked -> setState { copy(sheet = Sheet.Sort) }
            Intent.DismissSheet -> setState { copy(sheet = Sheet.None) }
            is Intent.CopyFromActions -> selected { summary ->
                withItem(summary.id) { item ->
                    item.valueOf(intent.field).takeIf { it.isNotEmpty() }?.let { sendEffect(Effect.Copy(CopyRequest(it, intent.field))) }
                }
            }
            Intent.EditSelected -> selected { sendEffect(Effect.EditItem(it.id)) }
            Intent.ToggleFavoriteSelected -> selected { viewModelScope.launch { toggleFavorite(it.id) } }
            Intent.OpenWebsiteSelected -> selected { summary ->
                withItem(summary.id) { item -> item.url.takeIf { it.isNotBlank() }?.let { sendEffect(Effect.OpenUrl(it)) } }
            }
            Intent.TrashSelected -> setState { copy(sheet = Sheet.None, confirmTrash = true) }
            Intent.ConfirmTrash -> {
                val item = currentState.selected
                setState { copy(confirmTrash = false, selected = null) }
                if (item != null) {
                    viewModelScope.launch {
                        moveToTrash(item.id)
                        sendEffect(Effect.Trashed(item.id, item.title))
                    }
                }
            }
            Intent.DismissTrash -> setState { copy(confirmTrash = false, selected = null) }
            Intent.TrashClicked -> sendEffect(Effect.OpenTrash)
            Intent.ScanCodeClicked -> {
                setState { copy(sheet = Sheet.None) }
                sendEffect(Effect.ScanCode)
            }
            is Intent.CopyCode -> currentState.codes[intent.id]?.let {
                sendEffect(Effect.Copy(CopyRequest(it.code, CopyField.Totp)))
            }
            Intent.LockClicked -> sendEffect(Effect.Lock)
        }
    }

    override fun onCleared() {
        codesJob?.cancel()
        super.onCleared()
    }

    /** Runs [block] with the selected item and closes the actions sheet. */
    private inline fun selected(block: (dev.spaceapps.secvault.feature.vault.domain.model.VaultItemSummary) -> Unit) {
        val item = currentState.selected ?: return
        setState { copy(sheet = Sheet.None, selected = null) }
        block(item)
    }

    private fun withItem(id: String, block: (dev.spaceapps.secvault.feature.vault.domain.model.VaultItem) -> Unit) {
        viewModelScope.launch { getItem(id)?.let(block) }
    }

    private fun State.recomputed(): State {
        val visible = observeItems.apply(allItems, query, filter, sort)
        // While searching, favorites aren't pulled out: results stay in one list.
        return if (query.isNotBlank()) {
            copy(favorites = emptyList(), others = visible)
        } else {
            copy(favorites = visible.filter { it.favorite }, others = visible.filterNot { it.favorite })
        }
    }
}

private const val CODE_TICK_MILLIS = 1000L

/** Fields offered in the item actions sheet, by type. */
val ItemType.actionCopyFields: List<CopyField>
    get() = when (this) {
        ItemType.Login -> listOf(CopyField.Username, CopyField.Password)
        ItemType.Card -> listOf(CopyField.CardNumber, CopyField.CardCvv)
        ItemType.Note -> listOf(CopyField.Notes)
    }
