package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.CenteredContent
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ConfirmDialog
import dev.spaceapps.secvault.core.ui.components.FullScreenProgress
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.LocalSnackbarHostState
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.domain.usecase.TrashPolicy
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashContract.State
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashViewModel
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.cd_empty_trash
import dev.spaceapps.secvault.feature.vault.resources.cd_restore
import dev.spaceapps.secvault.feature.vault.resources.restored_snackbar
import dev.spaceapps.secvault.feature.vault.resources.trash_delete_dialog_text
import dev.spaceapps.secvault.feature.vault.resources.trash_delete_dialog_title
import dev.spaceapps.secvault.feature.vault.resources.trash_delete_forever
import dev.spaceapps.secvault.feature.vault.resources.trash_deleted_days_ago
import dev.spaceapps.secvault.feature.vault.resources.trash_deleted_today
import dev.spaceapps.secvault.feature.vault.resources.trash_empty_action
import dev.spaceapps.secvault.feature.vault.resources.trash_empty_body
import dev.spaceapps.secvault.feature.vault.resources.trash_empty_dialog_text
import dev.spaceapps.secvault.feature.vault.resources.trash_empty_dialog_title
import dev.spaceapps.secvault.feature.vault.resources.trash_empty_title
import dev.spaceapps.secvault.feature.vault.resources.trash_info
import dev.spaceapps.secvault.feature.vault.resources.trash_title
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** 38 · Trash. Tap restores; long press deletes forever. */
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is Effect.Restored -> scope.launch {
                snackbar.showSnackbar(
                    getString(Res.string.restored_snackbar, effect.title),
                )
            }

            Effect.Close -> onBack()
        }
    }
    TrashContent(state, viewModel::onIntent)
}

@Composable
internal fun TrashContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(Res.string.trash_title),
                navIcon = NavIcon.Back,
                onNavigate = { onIntent(Intent.Back) },
            ) {
                if (state.items.isNotEmpty()) {
                    IconButton(onClick = { onIntent(Intent.EmptyTrash) }) {
                        Icon(Icons.Outlined.DeleteSweep, stringResource(Res.string.cd_empty_trash))
                    }
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> FullScreenProgress(Modifier.padding(padding))

            state.items.isEmpty() -> CenteredContent(Modifier.padding(padding)) {
                HeroMessage(
                    icon = Icons.Outlined.Delete,
                    title = stringResource(Res.string.trash_empty_title),
                    body = stringResource(Res.string.trash_empty_body, TrashPolicy.RETENTION_DAYS),
                    iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    iconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    bottom = Dimens.dp24,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
            ) {
                item {
                    Text(
                        stringResource(Res.string.trash_info, TrashPolicy.RETENTION_DAYS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Dimens.dp4),
                    )
                }
                items(state.items, key = { it.summary.id }) { trashed ->
                    VaultCard(
                        item = trashed.summary,
                        subtitle = if (trashed.daysAgo == 0) {
                            stringResource(Res.string.trash_deleted_today, trashed.daysLeft)
                        } else {
                            stringResource(Res.string.trash_deleted_days_ago, trashed.daysAgo, trashed.daysLeft)
                        },
                        onClick = { onIntent(Intent.Restore(trashed.summary.id)) },
                        onLongClick = { onIntent(Intent.Delete(trashed.summary)) },
                        trailing = {
                            IconButton(onClick = { onIntent(Intent.Restore(trashed.summary.id)) }) {
                                Icon(Icons.Outlined.RestoreFromTrash, stringResource(Res.string.cd_restore))
                            }
                        },
                    )
                }
            }
        }
    }

    if (state.confirmEmpty) {
        ConfirmDialog(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(Res.string.trash_empty_dialog_title),
            text = stringResource(Res.string.trash_empty_dialog_text),
            confirmText = stringResource(Res.string.trash_empty_action),
            onConfirm = { onIntent(Intent.ConfirmEmpty) },
            onDismiss = { onIntent(Intent.Dismiss) },
        )
    }
    state.confirmDelete?.let { item ->
        ConfirmDialog(
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(Res.string.trash_delete_dialog_title, item.title),
            text = stringResource(Res.string.trash_delete_dialog_text),
            confirmText = stringResource(Res.string.trash_delete_forever),
            onConfirm = { onIntent(Intent.ConfirmDelete) },
            onDismiss = { onIntent(Intent.Dismiss) },
        )
    }
}
