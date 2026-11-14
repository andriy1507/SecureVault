package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.clipboard.rememberCopyAction
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.CenteredContent
import dev.spaceapps.secvault.core.ui.components.ChevronTrailing
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ConfirmDialog
import dev.spaceapps.secvault.core.ui.components.FieldRow
import dev.spaceapps.secvault.core.ui.components.FullScreenProgress
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.SectionHeader
import dev.spaceapps.secvault.core.ui.components.VisibilityToggle
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_delete as coreDelete
import dev.spaceapps.secvault.core.ui.resources.cd_more
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.TotpCode
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.usecase.TrashPolicy
import dev.spaceapps.secvault.feature.vault.presentation.common.CopyField
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailContract.State
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailViewModel
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.action_delete
import dev.spaceapps.secvault.feature.vault.resources.cd_copy
import dev.spaceapps.secvault.feature.vault.resources.cd_delete_passkey
import dev.spaceapps.secvault.feature.vault.resources.cd_edit
import dev.spaceapps.secvault.feature.vault.resources.cd_favorite
import dev.spaceapps.secvault.feature.vault.resources.cd_open
import dev.spaceapps.secvault.feature.vault.resources.detail_subtitle
import dev.spaceapps.secvault.feature.vault.resources.field_card_number
import dev.spaceapps.secvault.feature.vault.resources.field_cardholder
import dev.spaceapps.secvault.feature.vault.resources.field_cvv
import dev.spaceapps.secvault.feature.vault.resources.field_expiry
import dev.spaceapps.secvault.feature.vault.resources.field_notes
import dev.spaceapps.secvault.feature.vault.resources.field_password
import dev.spaceapps.secvault.feature.vault.resources.field_totp_code
import dev.spaceapps.secvault.feature.vault.resources.field_username
import dev.spaceapps.secvault.feature.vault.resources.field_website
import dev.spaceapps.secvault.feature.vault.resources.history_changed
import dev.spaceapps.secvault.feature.vault.resources.history_count
import dev.spaceapps.secvault.feature.vault.resources.history_title
import dev.spaceapps.secvault.feature.vault.resources.not_found
import dev.spaceapps.secvault.feature.vault.resources.passkey_delete_text
import dev.spaceapps.secvault.feature.vault.resources.passkey_delete_title
import dev.spaceapps.secvault.feature.vault.resources.passkey_subtitle
import dev.spaceapps.secvault.feature.vault.resources.passkeys_title
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_text
import dev.spaceapps.secvault.feature.vault.resources.trash_dialog_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MASK_CHAR = '•'
private const val MASK_MAX = 16
private const val TOTP_GROUP = 3
private const val TOTP_SEPARATOR = " "

private fun mask(secret: String) = MASK_CHAR.toString().repeat(secret.length.coerceAtMost(MASK_MAX))

/** "482913" → "482 913". */
internal fun formatTotp(code: String) = code.chunked(TOTP_GROUP).joinToString(TOTP_SEPARATOR)

/** 10 · Item detail. [refreshKey] changes when the editor saved, to reload. */
@Composable
fun ItemDetailScreen(
    itemId: String,
    refreshKey: Int,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTrashed: (id: String, title: String) -> Unit,
    viewModel: ItemDetailViewModel = koinViewModel(key = "detail:$itemId"),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(itemId) { viewModel.onIntent(Intent.Load(itemId)) }
    LaunchedEffect(refreshKey) { if (refreshKey > 0) viewModel.reload() }
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is Effect.Copy -> scope.launch {
                copy.copy(effect.request.text, effect.request.field.sensitive, getString(effect.request.field.label))
            }

            is Effect.Edit -> onEdit(effect.id)

            is Effect.OpenUrl -> uriHandler.openWebsite(effect.url)

            is Effect.Trashed -> onTrashed(effect.id, effect.title)

            Effect.Close -> onBack()
        }
    }
    ItemDetailContent(state, viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemDetailContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val item = state.item
    Scaffold(
        topBar = {
            AppTopBar(title = "", navIcon = NavIcon.Back, onNavigate = { onIntent(Intent.Back) }) {
                if (item != null) {
                    IconButton(onClick = { onIntent(Intent.ToggleFavorite) }) {
                        Icon(
                            if (item.favorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                            stringResource(Res.string.cd_favorite),
                            tint =
                                if (item.favorite) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    IconButton(
                        onClick = { onIntent(Intent.Edit) },
                    ) { Icon(Icons.Outlined.Edit, stringResource(Res.string.cd_edit)) }
                    DetailMenu(onIntent)
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> FullScreenProgress(Modifier.padding(padding))

            state.notFound || item == null -> CenteredContent(Modifier.padding(padding)) {
                Text(stringResource(Res.string.not_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = Dimens.dp24),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp16),
            ) {
                Header(item)
                when (item.type) {
                    ItemType.Login -> LoginFields(item, state, onIntent)
                    ItemType.Card -> CardFields(item, state, onIntent)
                    ItemType.Note -> Unit
                }
                if (item.passkeys.isNotEmpty()) PasskeyList(item, onIntent)
                if (item.notes.isNotBlank()) {
                    ListGroup {
                        FieldRow(stringResource(Res.string.field_notes), item.notes) {
                            CopyButton { onIntent(Intent.Copy(CopyField.Notes)) }
                        }
                    }
                }
                if (item.passwordHistory.isNotEmpty()) {
                    ListGroup {
                        ListRow(
                            title = stringResource(Res.string.history_title),
                            subtitle = stringResource(Res.string.history_count, item.passwordHistory.size),
                            leadingIcon = Icons.Filled.History,
                            trailing = { ChevronTrailing() },
                            onClick = { onIntent(Intent.ShowHistory(true)) },
                        )
                    }
                }
            }
        }
    }

    if (state.showHistory && item != null) HistorySheet(item, onIntent)
    val passkeyToDelete = item?.passkeys?.firstOrNull { it.credentialId == state.confirmDeletePasskey }
    if (passkeyToDelete != null) {
        ConfirmDialog(
            icon = Icons.Outlined.Key,
            title = stringResource(Res.string.passkey_delete_title),
            text = stringResource(Res.string.passkey_delete_text, passkeyToDelete.rpId),
            confirmText = stringResource(CoreRes.string.coreDelete),
            onConfirm = { onIntent(Intent.ConfirmDeletePasskey) },
            onDismiss = { onIntent(Intent.DismissDeletePasskey) },
        )
    }
    if (state.confirmTrash && item != null) {
        ConfirmDialog(
            icon = Icons.Outlined.Delete,
            title = stringResource(Res.string.trash_dialog_title, item.title),
            text = stringResource(Res.string.trash_dialog_text, TrashPolicy.RETENTION_DAYS),
            confirmText = stringResource(CoreRes.string.coreDelete),
            onConfirm = { onIntent(Intent.ConfirmTrash) },
            onDismiss = { onIntent(Intent.DismissTrash) },
        )
    }
}

/** Passkeys saved through the system passkey prompt; they can be removed here, not edited. */
@Composable
private fun PasskeyList(
    item: VaultItem,
    onIntent: (Intent) -> Unit,
) {
    SectionHeader(stringResource(Res.string.passkeys_title))
    ListGroup {
        item.passkeys.forEachIndexed { index, passkey ->
            if (index > 0) ListDivider()
            ListRow(
                title = passkey.userName.ifBlank { passkey.userDisplayName.ifBlank { item.username } },
                subtitle = stringResource(Res.string.passkey_subtitle, passkey.rpId, relativeDays(passkey.createdAt)),
                leadingIcon = Icons.Outlined.Key,
                trailing = {
                    IconButton(onClick = { onIntent(Intent.DeletePasskey(passkey.credentialId)) }) {
                        Icon(Icons.Outlined.Delete, stringResource(Res.string.cd_delete_passkey))
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailMenu(onIntent: (Intent) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) { Icon(Icons.Filled.MoreVert, stringResource(CoreRes.string.cd_more)) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.action_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = {
                open = false
                onIntent(Intent.Trash)
            },
        )
    }
}

@Composable
private fun Header(item: VaultItem) {
    Row(
        Modifier.padding(horizontal = Dimens.dp4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp16),
    ) {
        TypeAvatar(item.title, item.type, large = true)
        Column {
            Text(item.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(
                    Res.string.detail_subtitle,
                    stringResource(item.type.label),
                    relativeDays(item.updatedAt),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Outlined.ContentCopy, stringResource(Res.string.cd_copy)) }
}

@Composable
private fun LoginFields(
    item: VaultItem,
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val hasCredentials = item.username.isNotEmpty() || item.password.isNotEmpty() || state.totp != null
    if (hasCredentials) {
        ListGroup {
            var first = true
            if (item.username.isNotEmpty()) {
                first = false
                FieldRow(stringResource(Res.string.field_username), item.username) {
                    CopyButton { onIntent(Intent.Copy(CopyField.Username)) }
                }
            }
            if (item.password.isNotEmpty()) {
                if (!first) ListDivider()
                first = false
                FieldRow(
                    stringResource(Res.string.field_password),
                    if (state.passwordVisible) item.password else mask(item.password),
                    monospace = true,
                ) {
                    VisibilityToggle(state.passwordVisible) { onIntent(Intent.TogglePasswordVisible) }
                    CopyButton { onIntent(Intent.Copy(CopyField.Password)) }
                }
            }
            val totp = state.totp
            if (totp != null) {
                if (!first) ListDivider()
                FieldRow(stringResource(Res.string.field_totp_code), formatTotp(totp.code), monospace = true) {
                    TotpRing(totp)
                    CopyButton { onIntent(Intent.Copy(CopyField.Totp)) }
                }
            }
        }
    }
    if (item.url.isNotBlank()) {
        ListGroup {
            FieldRow(stringResource(Res.string.field_website), displayHost(item.url)) {
                IconButton(onClick = { onIntent(Intent.OpenWebsite) }) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.cd_open))
                }
                CopyButton { onIntent(Intent.Copy(CopyField.Website)) }
            }
        }
    }
}

@Composable
internal fun TotpRing(totp: TotpCode) {
    Box(Modifier.padding(end = Dimens.dp4).size(Dimens.totpRing), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { totp.secondsRemaining.toFloat() / totp.periodSeconds },
            strokeWidth = Dimens.totpRingStroke,
            trackColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(Dimens.totpRing),
        )
        Text("${totp.secondsRemaining}", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CardFields(
    item: VaultItem,
    state: State,
    onIntent: (Intent) -> Unit,
) {
    CardPreview(item.title, item.cardNumber, item.cardholderName, item.cardExpiry)
    ListGroup {
        if (item.cardholderName.isNotBlank()) {
            FieldRow(stringResource(Res.string.field_cardholder), item.cardholderName) {
                CopyButton { onIntent(Intent.Copy(CopyField.CardholderName)) }
            }
            ListDivider()
        }
        if (item.cardNumber.isNotBlank()) {
            FieldRow(
                stringResource(Res.string.field_card_number),
                if (state.passwordVisible) groupCardNumber(item.cardNumber) else maskCardNumber(item.cardNumber),
                monospace = true,
            ) {
                VisibilityToggle(state.passwordVisible) { onIntent(Intent.TogglePasswordVisible) }
                CopyButton { onIntent(Intent.Copy(CopyField.CardNumber)) }
            }
            ListDivider()
        }
        if (item.cardExpiry.isNotBlank()) {
            FieldRow(stringResource(Res.string.field_expiry), item.cardExpiry) {
                CopyButton { onIntent(Intent.Copy(CopyField.CardExpiry)) }
            }
            ListDivider()
        }
        if (item.cardCvv.isNotBlank()) {
            FieldRow(
                stringResource(Res.string.field_cvv),
                if (state.cvvVisible) item.cardCvv else mask(item.cardCvv),
                monospace = true,
            ) {
                VisibilityToggle(state.cvvVisible) { onIntent(Intent.ToggleCvvVisible) }
                CopyButton { onIntent(Intent.Copy(CopyField.CardCvv)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySheet(
    item: VaultItem,
    onIntent: (Intent) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = { onIntent(Intent.ShowHistory(false)) }) {
        SheetTitle(stringResource(Res.string.history_title))
        item.passwordHistory.forEachIndexed { index, entry ->
            ListRow(
                title = mask(entry.password),
                subtitle = stringResource(Res.string.history_changed, relativeDays(entry.changedAt)),
                modifier = Modifier.padding(horizontal = Dimens.dp8),
                trailing = { CopyButton { onIntent(Intent.CopyHistory(index)) } },
            )
        }
        SheetBottomSpace()
    }
}
