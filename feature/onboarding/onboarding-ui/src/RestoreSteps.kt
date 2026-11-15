package dev.spaceapps.secvault.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.BottomActions
import dev.spaceapps.secvault.core.ui.components.ErrorText
import dev.spaceapps.secvault.core.ui.components.FieldRow
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.PrimaryButton
import dev.spaceapps.secvault.core.ui.components.RecoveryPhraseField
import dev.spaceapps.secvault.core.ui.files.BackupFileFormat
import dev.spaceapps.secvault.core.ui.files.rememberTextFileOpener
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Error
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Intent
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.State
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Step
import dev.spaceapps.secvault.feature.onboarding.resources.Res
import dev.spaceapps.secvault.feature.onboarding.resources.restore_action
import dev.spaceapps.secvault.feature.onboarding.resources.restore_backup_account
import dev.spaceapps.secvault.feature.onboarding.resources.restore_backup_items
import dev.spaceapps.secvault.feature.onboarding.resources.restore_phrase_body
import dev.spaceapps.secvault.feature.onboarding.resources.restore_phrase_title
import dev.spaceapps.secvault.feature.onboarding.resources.restore_pick_action
import dev.spaceapps.secvault.feature.onboarding.resources.restore_pick_body
import dev.spaceapps.secvault.feature.onboarding.resources.restore_pick_title
import dev.spaceapps.secvault.feature.onboarding.resources.restore_title
import dev.spaceapps.secvault.feature.vault.domain.usecase.BackupFormat
import org.jetbrains.compose.resources.stringResource

/** Restore an account from a backup file and its recovery key (new phone, same account ID). */
@Composable
internal fun RestoreFlow(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(Res.string.restore_title),
                navIcon = NavIcon.Back,
                onNavigate = { onIntent(Intent.Back) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.step == Step.RestorePhrase) {
                RestorePhraseStep(state, onIntent)
            } else {
                RestorePickFileStep(state.error, onIntent)
            }
        }
    }
}

@Composable
private fun RestorePickFileStep(
    error: Error?,
    onIntent: (Intent) -> Unit,
) {
    // A cancelled or unreadable pick sends nothing: the user just stays here.
    val opener = rememberTextFileOpener(BackupFileFormat.Backup) { content ->
        if (content != null) onIntent(Intent.BackupPicked(content))
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            HeroMessage(
                icon = Icons.Outlined.SettingsBackupRestore,
                title = stringResource(Res.string.restore_pick_title),
                body = stringResource(Res.string.restore_pick_body),
            ) {
                ErrorText(errorText(error), Modifier.fillMaxWidth(), TextAlign.Center)
            }
        }
        BottomActions { PrimaryButton(stringResource(Res.string.restore_pick_action), opener::open) }
    }
}

@Composable
private fun RestorePhraseStep(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val words = state.restoreWords
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.dp24, end = Dimens.dp24, top = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16),
        ) {
            Text(
                stringResource(Res.string.restore_phrase_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(Res.string.restore_phrase_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            state.backupInfo?.let { info ->
                ListGroup {
                    FieldRow(stringResource(Res.string.restore_backup_account), info.accountId, monospace = true)
                    ListDivider()
                    FieldRow(stringResource(Res.string.restore_backup_items), info.itemCount.toString())
                }
            }
            RecoveryPhraseField(
                value = state.restorePhrase,
                onValueChange = { onIntent(Intent.RestorePhraseChanged(it)) },
                wordCount = words.size,
                requiredWords = BackupFormat.PHRASE_WORDS,
                isError = state.error == Error.InvalidPhrase || state.error == Error.PhraseDoesNotMatch,
            )
            ErrorText(errorText(state.error))
        }
        BottomActions {
            PrimaryButton(
                stringResource(Res.string.restore_action),
                { onIntent(Intent.SubmitRestore) },
                enabled = state.canSubmitRestore,
            )
        }
    }
}
