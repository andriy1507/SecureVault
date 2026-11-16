package dev.spaceapps.secvault.feature.vault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.spaceapps.secvault.core.domain.model.BackupFormat
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ErrorText
import dev.spaceapps.secvault.core.ui.components.FieldRow
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.LocalSnackbarHostState
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.PrimaryButton
import dev.spaceapps.secvault.core.ui.components.RecoveryPhraseField
import dev.spaceapps.secvault.core.ui.components.SecondaryButton
import dev.spaceapps.secvault.core.ui.components.SectionHeader
import dev.spaceapps.secvault.core.ui.components.TonalButton
import dev.spaceapps.secvault.core.ui.files.BackupFileFormat
import dev.spaceapps.secvault.core.ui.files.rememberTextFileOpener
import dev.spaceapps.secvault.core.ui.files.rememberTextFileSaver
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_cancel
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Error
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.State
import dev.spaceapps.secvault.feature.vault.resources.Res
import dev.spaceapps.secvault.feature.vault.resources.backup_error_export_failed
import dev.spaceapps.secvault.feature.vault.resources.backup_error_import_failed
import dev.spaceapps.secvault.feature.vault.resources.backup_error_invalid_phrase
import dev.spaceapps.secvault.feature.vault.resources.backup_error_not_saved
import dev.spaceapps.secvault.feature.vault.resources.backup_error_phrase_mismatch
import dev.spaceapps.secvault.feature.vault.resources.backup_error_unreadable
import dev.spaceapps.secvault.feature.vault.resources.backup_export_action
import dev.spaceapps.secvault.feature.vault.resources.backup_export_body
import dev.spaceapps.secvault.feature.vault.resources.backup_export_header
import dev.spaceapps.secvault.feature.vault.resources.backup_exported
import dev.spaceapps.secvault.feature.vault.resources.backup_import_account
import dev.spaceapps.secvault.feature.vault.resources.backup_import_action
import dev.spaceapps.secvault.feature.vault.resources.backup_import_body
import dev.spaceapps.secvault.feature.vault.resources.backup_import_header
import dev.spaceapps.secvault.feature.vault.resources.backup_import_items
import dev.spaceapps.secvault.feature.vault.resources.backup_import_other_account
import dev.spaceapps.secvault.feature.vault.resources.backup_import_submit
import dev.spaceapps.secvault.feature.vault.resources.backup_imported
import dev.spaceapps.secvault.feature.vault.resources.backup_title
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Settings → Backup & import: encrypted export, and import of a backup with its recovery key. */
@Composable
fun BackupScreen(
    state: State,
    effects: Flow<Effect>,
    onIntent: (Intent) -> Unit,
    onBack: () -> Unit,
) {
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val saver =
        rememberTextFileSaver(BackupFileFormat.Backup) { saved -> onIntent(Intent.ExportWritten(saved)) }
    CollectEffects(effects) { effect ->
        when (effect) {
            is Effect.SaveFile -> saver.save(effect.file.suggestedName, effect.file.content)

            is Effect.Exported -> scope.launch {
                snackbar.showSnackbar(getString(Res.string.backup_exported, effect.itemCount))
            }

            is Effect.Imported -> scope.launch {
                snackbar.showSnackbar(
                    getString(Res.string.backup_imported, effect.result.imported, effect.result.skipped),
                )
            }

            Effect.Close -> onBack()
        }
    }
    BackupContent(state, onIntent)
}

@Composable
internal fun BackupContent(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(Res.string.backup_title),
                navIcon = NavIcon.Back,
                onNavigate = { onIntent(Intent.Back) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            ExportSection(state, onIntent)
            ImportSection(state, onIntent)
        }
    }
}

@Composable
private fun ExportSection(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        SectionHeader(stringResource(Res.string.backup_export_header))
        Explanation(stringResource(Res.string.backup_export_body))
        TonalButton(
            text = stringResource(Res.string.backup_export_action),
            onClick = { onIntent(Intent.Export) },
            icon = Icons.Outlined.FileUpload,
            enabled = !state.exporting && !state.importing,
        )
        if (state.importContent == null) ErrorText(exportErrorText(state.error))
    }
}

@Composable
private fun ImportSection(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    // A cancelled or unreadable pick sends nothing.
    val opener = rememberTextFileOpener(BackupFileFormat.Backup) { content ->
        if (content != null) onIntent(Intent.ImportPicked(content))
    }
    val info = state.importInfo
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
        SectionHeader(stringResource(Res.string.backup_import_header))
        Explanation(stringResource(Res.string.backup_import_body))
        if (state.importContent == null || info == null) {
            TonalButton(
                text = stringResource(Res.string.backup_import_action),
                onClick = opener::open,
                icon = Icons.Outlined.FileDownload,
                enabled = !state.exporting,
            )
            ErrorText(importErrorText(state.error))
        } else {
            ListGroup {
                FieldRow(stringResource(Res.string.backup_import_account), info.accountId, monospace = true)
                ListDivider()
                FieldRow(stringResource(Res.string.backup_import_items), info.itemCount.toString())
            }
            if (state.otherAccount) Explanation(stringResource(Res.string.backup_import_other_account))
            RecoveryPhraseField(
                value = state.phrase,
                onValueChange = { onIntent(Intent.PhraseChanged(it)) },
                wordCount = state.phraseWords.size,
                requiredWords = BackupFormat.PHRASE_WORDS,
                isError = state.error == Error.InvalidPhrase || state.error == Error.PhraseDoesNotMatch,
                enabled = !state.importing,
            )
            ErrorText(importErrorText(state.error))
            PrimaryButton(
                stringResource(Res.string.backup_import_submit),
                { onIntent(Intent.SubmitImport) },
                enabled = state.canImport,
            )
            SecondaryButton(stringResource(CoreRes.string.action_cancel), { onIntent(Intent.CancelImport) })
        }
    }
}

@Composable
private fun Explanation(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dimens.dp4),
    )
}

@Composable
private fun exportErrorText(error: Error?): String? =
    when (error) {
        is Error.ExportFailed -> stringResource(Res.string.backup_error_export_failed, error.reason.orEmpty())
        Error.FileNotSaved -> stringResource(Res.string.backup_error_not_saved)
        else -> null
    }

@Composable
private fun importErrorText(error: Error?): String? =
    when (error) {
        Error.BackupUnreadable -> stringResource(Res.string.backup_error_unreadable)
        Error.InvalidPhrase -> stringResource(Res.string.backup_error_invalid_phrase)
        Error.PhraseDoesNotMatch -> stringResource(Res.string.backup_error_phrase_mismatch)
        is Error.ImportFailed -> stringResource(Res.string.backup_error_import_failed, error.reason.orEmpty())
        else -> null
    }
