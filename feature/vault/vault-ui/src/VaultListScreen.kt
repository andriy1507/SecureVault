package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.ui.clipboard.rememberCopyAction
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.CenteredContent
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ConfirmDialog
import dev.spaceapps.secvault.core.ui.components.FullScreenProgress
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.SectionHeader
import dev.spaceapps.secvault.core.ui.components.TonalButton
import dev.spaceapps.secvault.core.ui.navigation.PlatformBackHandler
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_back
import dev.spaceapps.secvault.core.ui.resources.action_delete as coreDelete
import dev.spaceapps.secvault.core.ui.resources.cd_more
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.domain.model.ItemFilter
import dev.spaceapps.secvault.feature.vault.domain.model.ItemSort
import dev.spaceapps.secvault.feature.vault.domain.usecase.TrashPolicy
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.Sheet
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListContract.State
import dev.spaceapps.secvault.feature.vault.presentation.list.actionCopyFields
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.action_add_favorite
import dev.spaceapps.secvault.feature.vault.resources.action_copy_card_number
import dev.spaceapps.secvault.feature.vault.resources.action_copy_cvv
import dev.spaceapps.secvault.feature.vault.resources.action_copy_note
import dev.spaceapps.secvault.feature.vault.resources.action_copy_password
import dev.spaceapps.secvault.feature.vault.resources.action_copy_username
import dev.spaceapps.secvault.feature.vault.resources.action_delete
import dev.spaceapps.secvault.feature.vault.resources.action_edit
import dev.spaceapps.secvault.feature.vault.resources.action_open_website
import dev.spaceapps.secvault.feature.vault.resources.action_remove_favorite
import dev.spaceapps.secvault.feature.vault.resources.cd_clear_search
import dev.spaceapps.secvault.feature.vault.resources.cd_copy
import dev.spaceapps.secvault.feature.vault.resources.cd_open
import dev.spaceapps.secvault.feature.vault.resources.cd_search
import dev.spaceapps.secvault.feature.vault.resources.cd_sort
import dev.spaceapps.secvault.feature.vault.resources.codes_empty_body
import dev.spaceapps.secvault.feature.vault.resources.codes_empty_title
import dev.spaceapps.secvault.feature.vault.resources.codes_scan
import dev.spaceapps.secvault.feature.vault.resources.empty_body
import dev.spaceapps.secvault.feature.vault.resources.empty_title
import dev.spaceapps.secvault.feature.vault.resources.filter_all
import dev.spaceapps.secvault.feature.vault.resources.filter_cards
import dev.spaceapps.secvault.feature.vault.resources.filter_codes
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_cards_add
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_cards_body
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_cards_title
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_logins_add
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_logins_body
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_logins_title
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_notes_add
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_notes_body
import dev.spaceapps.secvault.feature.vault.resources.filter_empty_notes_title
import dev.spaceapps.secvault.feature.vault.resources.filter_logins
import dev.spaceapps.secvault.feature.vault.resources.filter_notes
import dev.spaceapps.secvault.feature.vault.resources.menu_lock
import dev.spaceapps.secvault.feature.vault.resources.menu_trash
import dev.spaceapps.secvault.feature.vault.resources.new_item
import dev.spaceapps.secvault.feature.vault.resources.no_results_add
import dev.spaceapps.secvault.feature.vault.resources.no_results_body
import dev.spaceapps.secvault.feature.vault.resources.no_results_title
import dev.spaceapps.secvault.feature.vault.resources.search_hint
import dev.spaceapps.secvault.feature.vault.resources.section_all_items
import dev.spaceapps.secvault.feature.vault.resources.section_favorites
import dev.spaceapps.secvault.feature.vault.resources.section_results
import dev.spaceapps.secvault.feature.vault.resources.sheet_add_title
import dev.spaceapps.secvault.feature.vault.resources.sheet_sort_title
import dev.spaceapps.secvault.feature.vault.resources.sort_date_added
import dev.spaceapps.secvault.feature.vault.resources.sort_last_modified
import dev.spaceapps.secvault.feature.vault.resources.sort_name
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_text
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_title
import dev.spaceapps.secvault.feature.vault.resources.type_card
import dev.spaceapps.secvault.feature.vault.resources.type_card_desc
import dev.spaceapps.secvault.feature.vault.resources.type_code
import dev.spaceapps.secvault.feature.vault.resources.type_code_desc
import dev.spaceapps.secvault.feature.vault.resources.type_login
import dev.spaceapps.secvault.feature.vault.resources.type_login_desc
import dev.spaceapps.secvault.feature.vault.resources.type_note
import dev.spaceapps.secvault.feature.vault.resources.type_note_desc
import dev.spaceapps.secvault.feature.vault.resources.vault_title
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** 01 · Vault, with 19 (empty), 20 (no results), 07/35/36/37 sheets and dialogs. */
@Composable
fun VaultListScreen(
    state: State,
    effects: Flow<Effect>,
    onIntent: (Intent) -> Unit,
    onOpenItem: (id: String) -> Unit,
    onEditItem: (id: String) -> Unit,
    onNewItem: (type: ItemType, title: String) -> Unit,
    onItemTrashed: (id: String, title: String) -> Unit,
    onOpenTrash: () -> Unit,
    onScanCode: () -> Unit,
    onLock: () -> Unit,
) {
    val copy = rememberCopyAction()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    CollectEffects(effects) { effect ->
        when (effect) {
            is Effect.OpenItem -> onOpenItem(effect.id)

            is Effect.EditItem -> onEditItem(effect.id)

            is Effect.NewItem -> onNewItem(effect.type, effect.title)

            is Effect.Copy -> scope.launch {
                copy.copy(effect.request.text, effect.request.field.sensitive, getString(effect.request.field.label))
            }

            is Effect.OpenUrl -> uriHandler.openWebsite(effect.url)

            is Effect.Trashed -> onItemTrashed(effect.id, effect.title)

            Effect.OpenTrash -> onOpenTrash()

            Effect.ScanCode -> onScanCode()

            Effect.Lock -> onLock()
        }
    }
    VaultListContent(state, onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VaultListContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Scaffold(
        topBar = {
            if (!state.searchActive) {
                AppTopBar(stringResource(Res.string.vault_title)) {
                    IconButton(onClick = { onIntent(Intent.SortClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, stringResource(Res.string.cd_sort))
                    }
                    OverflowMenu(onIntent)
                }
            }
        },
        floatingActionButton = {
            if (!state.searchActive) {
                ExtendedFloatingActionButton(
                    onClick = { onIntent(Intent.AddClicked) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.new_item)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    FullScreenProgress()
                }

                state.isEmptyVault -> {
                    EmptyVault()
                }

                else -> {
                    SearchBar(state, onIntent)
                    if (!state.searchActive) FilterChips(state.filter, onIntent)
                    when {
                        state.filter == ItemFilter.Codes && state.noResults && state.query.isBlank() -> CodesEmpty(
                            onIntent,
                        )

                        state.noResults && state.query.isBlank() -> FilterEmpty(state.filter, onIntent)

                        state.noResults -> NoResults(state.query, onIntent)

                        state.filter == ItemFilter.Codes -> CodeList(state, onIntent)

                        else -> ItemList(state, onIntent)
                    }
                }
            }
        }
    }

    when (state.sheet) {
        Sheet.None -> Unit
        Sheet.NewItemType -> NewItemTypeSheet(onIntent)
        Sheet.Sort -> SortSheet(state.sort, onIntent)
        Sheet.ItemActions -> ItemActionsSheet(state, onIntent)
    }

    val selected = state.selected
    if (state.confirmTrash && selected != null) {
        ConfirmDialog(
            icon = Icons.Outlined.Delete,
            title = stringResource(Res.string.trash_dialog_title, selected.title),
            text = stringResource(Res.string.trash_dialog_text, TrashPolicy.RETENTION_DAYS),
            confirmText = stringResource(CoreRes.string.coreDelete),
            onConfirm = { onIntent(Intent.ConfirmTrash) },
            onDismiss = { onIntent(Intent.DismissTrash) },
        )
    }
}

@Composable
private fun OverflowMenu(onIntent: (Intent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(CoreRes.string.cd_more)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.menu_trash)) },
            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            onClick = {
                open = false
                onIntent(Intent.TrashClicked)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.menu_lock)) },
            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            onClick = {
                open = false
                onIntent(Intent.LockClicked)
            },
        )
    }
}

@Composable
private fun SearchBar(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    // Back closes the search (and its keyboard) before it can leave the app.
    PlatformBackHandler(enabled = state.searchActive || state.query.isNotEmpty()) {
        focusManager.clearFocus()
        onIntent(Intent.SearchActiveChanged(false))
    }
    TextField(
        value = state.query,
        onValueChange = {
            if (!state.searchActive) onIntent(Intent.SearchActiveChanged(true))
            onIntent(Intent.QueryChanged(it))
        },
        placeholder = { Text(stringResource(Res.string.search_hint)) },
        singleLine = true,
        leadingIcon = {
            if (state.searchActive) {
                IconButton(onClick = {
                    focusManager.clearFocus()
                    onIntent(Intent.SearchActiveChanged(false))
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(CoreRes.string.action_back))
                }
            } else {
                Icon(Icons.Filled.Search, stringResource(Res.string.cd_search))
            }
        },
        trailingIcon = {
            if (state.query.isNotEmpty()) {
                IconButton(onClick = { onIntent(Intent.QueryChanged("")) }) {
                    Icon(Icons.Filled.Close, stringResource(Res.string.cd_clear_search))
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.dp4)
            .heightIn(min = Dimens.searchBarHeight),
    )
}

private val FILTERS = listOf(
    ItemFilter.All to Res.string.filter_all,
    ItemFilter.Logins to Res.string.filter_logins,
    ItemFilter.Cards to Res.string.filter_cards,
    ItemFilter.Notes to Res.string.filter_notes,
    ItemFilter.Codes to Res.string.filter_codes,
)

@Composable
private fun FilterChips(
    selected: ItemFilter,
    onIntent: (Intent) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        FILTERS.forEach { (filter, label) ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = { onIntent(Intent.FilterChanged(filter)) },
                label = { Text(stringResource(label)) },
                leadingIcon = {
                    val icon = if (isSelected) Icons.Filled.Check else filter.icon
                    if (icon !=
                        null
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                    }
                },
            )
        }
    }
}

private val ItemFilter.icon: ImageVector?
    get() = when (this) {
        ItemFilter.All -> null
        ItemFilter.Logins -> ItemType.Login.icon
        ItemFilter.Cards -> ItemType.Card.icon
        ItemFilter.Notes -> ItemType.Note.icon
        ItemFilter.Codes -> Icons.Outlined.Schedule
    }

@Composable
private fun ItemList(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = Dimens.dp4,
            bottom = Dimens.fabListPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
    ) {
        if (state.favorites.isNotEmpty()) {
            item(key = "h_fav") { SectionHeader(stringResource(Res.string.section_favorites)) }
            items(state.favorites, key = { "f_" + it.id }) { item -> Card(item, onIntent) }
        }
        if (state.others.isNotEmpty()) {
            item(key = "h_all") {
                SectionHeader(
                    stringResource(
                        if (state.query.isNotBlank()) Res.string.section_results else Res.string.section_all_items,
                    ),
                )
            }
            items(state.others, key = { "a_" + it.id }) { item -> Card(item, onIntent) }
        }
    }
}

@Composable
private fun Card(
    item: dev.spaceapps.secvault.core.domain.model.VaultItemSummary,
    onIntent: (Intent) -> Unit,
) {
    VaultCard(
        item = item,
        onClick = { onIntent(Intent.ItemClicked(item.id)) },
        onLongClick = { onIntent(Intent.ItemLongPressed(item)) },
        trailing = {
            if (item.type == ItemType.Note) {
                IconButton(onClick = { onIntent(Intent.ItemClicked(item.id)) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        stringResource(Res.string.cd_open),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                IconButton(onClick = { onIntent(Intent.QuickCopy(item.id)) }) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        stringResource(Res.string.cd_copy),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/** 2FA codes, Google Authenticator style: tap a row to copy its current code. */
@Composable
private fun CodeList(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Dimens.screenPadding,
            end = Dimens.screenPadding,
            top = Dimens.dp4,
            bottom = Dimens.fabListPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
    ) {
        items(state.favorites + state.others, key = { "c_" + it.id }) { item ->
            val code = state.codes[item.id]
            VaultCard(
                item = item,
                subtitle = item.subtitle,
                onClick = { onIntent(Intent.CopyCode(item.id)) },
                onLongClick = { onIntent(Intent.ItemLongPressed(item)) },
                trailing = {
                    if (code != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
                        ) {
                            Text(
                                formatTotp(code.code),
                                style = MaterialTheme.typography.headlineSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TotpRing(code)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun CodesEmpty(onIntent: (Intent) -> Unit) {
    CenteredContent {
        HeroMessage(
            icon = Icons.Filled.QrCodeScanner,
            title = stringResource(Res.string.codes_empty_title),
            body = stringResource(Res.string.codes_empty_body),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            TonalButton(stringResource(Res.string.codes_scan), {
                onIntent(Intent.ScanCodeClicked)
            }, icon = Icons.Filled.QrCodeScanner)
        }
    }
}

@Composable
private fun EmptyVault() {
    CenteredContent {
        HeroMessage(
            icon = Icons.Filled.Inventory2,
            title = stringResource(Res.string.empty_title),
            body = stringResource(Res.string.empty_body),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** A type filter with nothing in it yet (no search text): offer to add that type. */
@Composable
private fun FilterEmpty(
    filter: ItemFilter,
    onIntent: (Intent) -> Unit,
) {
    val type = filter.itemType ?: return
    val (title, body, action) = when (type) {
        ItemType.Login -> Triple(
            Res.string.filter_empty_logins_title,
            Res.string.filter_empty_logins_body,
            Res.string.filter_empty_logins_add,
        )

        ItemType.Card -> Triple(
            Res.string.filter_empty_cards_title,
            Res.string.filter_empty_cards_body,
            Res.string.filter_empty_cards_add,
        )

        ItemType.Note -> Triple(
            Res.string.filter_empty_notes_title,
            Res.string.filter_empty_notes_body,
            Res.string.filter_empty_notes_add,
        )
    }
    CenteredContent {
        HeroMessage(
            icon = type.icon,
            title = stringResource(title),
            body = stringResource(body),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            TonalButton(stringResource(action), { onIntent(Intent.NewItemTypeChosen(type)) }, icon = Icons.Filled.Add)
        }
    }
}

private val ItemFilter.itemType: ItemType?
    get() = when (this) {
        ItemFilter.Logins -> ItemType.Login
        ItemFilter.Cards -> ItemType.Card
        ItemFilter.Notes -> ItemType.Note
        ItemFilter.All, ItemFilter.Codes -> null
    }

@Composable
private fun NoResults(
    query: String,
    onIntent: (Intent) -> Unit,
) {
    CenteredContent {
        HeroMessage(
            icon = Icons.Filled.SearchOff,
            title = stringResource(Res.string.no_results_title, query.trim()),
            body = stringResource(Res.string.no_results_body),
            iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            TonalButton(
                text = stringResource(Res.string.no_results_add, query.trim()),
                icon = Icons.Filled.Add,
                onClick = { onIntent(Intent.AddFromQuery) },
            )
        }
    }
}

// --- Sheets -----------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewItemTypeSheet(onIntent: (Intent) -> Unit) {
    ModalBottomSheet(onDismissRequest = { onIntent(Intent.DismissSheet) }) {
        SheetTitle(stringResource(Res.string.sheet_add_title))
        listOf(
            Triple(ItemType.Login, Res.string.type_login, Res.string.type_login_desc),
            Triple(ItemType.Card, Res.string.type_card, Res.string.type_card_desc),
            Triple(ItemType.Note, Res.string.type_note, Res.string.type_note_desc),
        ).forEach { (type, title, desc) ->
            ListRow(
                title = stringResource(title),
                subtitle = stringResource(desc),
                leadingIcon = type.icon,
                modifier = Modifier.padding(horizontal = Dimens.dp8),
                onClick = { onIntent(Intent.NewItemTypeChosen(type)) },
            )
        }
        ListRow(
            title = stringResource(Res.string.type_code),
            subtitle = stringResource(Res.string.type_code_desc),
            leadingIcon = Icons.Filled.QrCodeScanner,
            modifier = Modifier.padding(horizontal = Dimens.dp8),
            onClick = { onIntent(Intent.ScanCodeClicked) },
        )
        SheetBottomSpace()
    }
}

private val SORTS: List<Pair<ItemSort, StringResource>> = listOf(
    ItemSort.Name to Res.string.sort_name,
    ItemSort.DateAdded to Res.string.sort_date_added,
    ItemSort.LastModified to Res.string.sort_last_modified,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    selected: ItemSort,
    onIntent: (Intent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(Intent.DismissSheet) }) {
        SheetTitle(stringResource(Res.string.sheet_sort_title))
        SORTS.forEach { (sort, label) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = sort == selected,
                        role = Role.RadioButton,
                        onClick = { onIntent(Intent.SortChanged(sort)) },
                    ).heightIn(min = Dimens.dp56)
                    .padding(horizontal = Dimens.dp24),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
            ) {
                RadioButton(selected = sort == selected, onClick = null)
                Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
            }
        }
        SheetBottomSpace()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemActionsSheet(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val item = state.selected ?: return
    ModalBottomSheet(onDismissRequest = { onIntent(Intent.DismissSheet) }) {
        Row(
            Modifier.padding(start = Dimens.dp24, end = Dimens.dp24, bottom = Dimens.dp16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
        ) {
            TypeAvatar(item.title, item.type)
            Column {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                val subtitle = summarySubtitle(item)
                if (subtitle.isNotBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        ListDivider()
        item.type.actionCopyFields.forEach { field ->
            SheetAction(field.actionIcon, stringResource(field.actionLabel)) { onIntent(Intent.CopyFromActions(field)) }
        }
        if (item.type == ItemType.Login) {
            SheetAction(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.action_open_website)) {
                onIntent(Intent.OpenWebsiteSelected)
            }
        }
        SheetAction(Icons.Outlined.Edit, stringResource(Res.string.action_edit)) { onIntent(Intent.EditSelected) }
        SheetAction(
            if (item.favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
            stringResource(if (item.favorite) Res.string.action_remove_favorite else Res.string.action_add_favorite),
        ) { onIntent(Intent.ToggleFavoriteSelected) }
        SheetAction(Icons.Outlined.Delete, stringResource(Res.string.action_delete), MaterialTheme.colorScheme.error) {
            onIntent(Intent.TrashSelected)
        }
        SheetBottomSpace()
    }
}

private val CopyField.actionLabel: StringResource
    get() = when (this) {
        CopyField.Username -> Res.string.action_copy_username
        CopyField.CardNumber -> Res.string.action_copy_card_number
        CopyField.CardCvv -> Res.string.action_copy_cvv
        CopyField.Notes -> Res.string.action_copy_note
        else -> Res.string.action_copy_password
    }

private val CopyField.actionIcon: ImageVector
    get() = when (this) {
        CopyField.Username -> Icons.Outlined.Person
        CopyField.CardNumber, CopyField.CardCvv -> ItemType.Card.icon
        CopyField.Notes -> ItemType.Note.icon
        else -> ItemType.Login.icon
    }

@Composable
private fun SheetAction(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    ListRow(
        title = text,
        leadingIcon = icon,
        contentColor = color,
        modifier = Modifier.padding(horizontal = Dimens.dp8),
        onClick = onClick,
    )
}

@Composable
internal fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = Dimens.dp24, end = Dimens.dp24, bottom = Dimens.dp8),
    )
}

@Composable
internal fun SheetBottomSpace() {
    androidx.compose.foundation.layout
        .Spacer(Modifier.size(Dimens.dp24))
}
