package dev.spaceapps.secvault.feature.onboarding.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.ui.clipboard.rememberSecureClipboard
import dev.spaceapps.secvault.core.ui.components.AppLogo
import dev.spaceapps.secvault.core.ui.components.AppTextField
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.BottomActions
import dev.spaceapps.secvault.core.ui.components.ChevronTrailing
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ErrorText
import dev.spaceapps.secvault.core.ui.components.FullScreenProgress
import dev.spaceapps.secvault.core.ui.components.HeroMessage
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.NavIcon
import dev.spaceapps.secvault.core.ui.components.PrimaryButton
import dev.spaceapps.secvault.core.ui.components.SecondaryButton
import dev.spaceapps.secvault.core.ui.components.SegmentBar
import dev.spaceapps.secvault.core.ui.components.TonalButton
import dev.spaceapps.secvault.core.ui.files.TextFileFormat
import dev.spaceapps.secvault.core.ui.files.rememberTextFileSaver
import dev.spaceapps.secvault.core.ui.navigation.PlatformBackHandler
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_continue
import dev.spaceapps.secvault.core.ui.resources.action_copy
import dev.spaceapps.secvault.core.ui.resources.copied
import dev.spaceapps.secvault.core.ui.resources.copied_clears_in
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.CREATE_STEPS
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Effect
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Error
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Intent
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.RESTORE_STEPS
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.State
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingContract.Step
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingViewModel
import dev.spaceapps.secvault.feature.onboarding.resources.Res
import dev.spaceapps.secvault.feature.onboarding.resources.app_title
import dev.spaceapps.secvault.feature.onboarding.resources.cd_clear_word
import dev.spaceapps.secvault.feature.onboarding.resources.confirm_body
import dev.spaceapps.secvault.feature.onboarding.resources.confirm_finish
import dev.spaceapps.secvault.feature.onboarding.resources.confirm_title
import dev.spaceapps.secvault.feature.onboarding.resources.confirm_word_label
import dev.spaceapps.secvault.feature.onboarding.resources.create_id_body
import dev.spaceapps.secvault.feature.onboarding.resources.create_id_label
import dev.spaceapps.secvault.feature.onboarding.resources.create_id_title
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_body
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_copy_label
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_file_failed
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_file_saved
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_save_txt
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_saved_check
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_title
import dev.spaceapps.secvault.feature.onboarding.resources.create_key_warning
import dev.spaceapps.secvault.feature.onboarding.resources.create_title
import dev.spaceapps.secvault.feature.onboarding.resources.device_not_secure_body
import dev.spaceapps.secvault.feature.onboarding.resources.device_not_secure_open_settings
import dev.spaceapps.secvault.feature.onboarding.resources.device_not_secure_retry
import dev.spaceapps.secvault.feature.onboarding.resources.device_not_secure_title
import dev.spaceapps.secvault.feature.onboarding.resources.error_backup_unreadable
import dev.spaceapps.secvault.feature.onboarding.resources.error_create_failed
import dev.spaceapps.secvault.feature.onboarding.resources.error_device_lock_needed
import dev.spaceapps.secvault.feature.onboarding.resources.error_finish_failed
import dev.spaceapps.secvault.feature.onboarding.resources.error_invalid_phrase
import dev.spaceapps.secvault.feature.onboarding.resources.error_phrase_mismatch
import dev.spaceapps.secvault.feature.onboarding.resources.error_restore_failed
import dev.spaceapps.secvault.feature.onboarding.resources.error_unlock_needed
import dev.spaceapps.secvault.feature.onboarding.resources.error_words_dont_match
import dev.spaceapps.secvault.feature.onboarding.resources.recovery_file_content
import dev.spaceapps.secvault.feature.onboarding.resources.recovery_file_name
import dev.spaceapps.secvault.feature.onboarding.resources.recovery_word_line
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_create_subtitle
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_create_title
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_legal_and
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_legal_prefix
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_legal_privacy
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_legal_terms
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_restore_subtitle
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_restore_title
import dev.spaceapps.secvault.feature.onboarding.resources.welcome_tagline
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Seconds the recovery key may stay on the clipboard. Short on purpose: it opens the whole vault. */
private const val RECOVERY_CLIPBOARD_SECONDS = 60

/** Words per row in the recovery key grid. */
private const val WORD_COLUMNS = 3

@Composable
fun OnboardingScreen(
    onOpenSecuritySettings: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            Effect.OpenSecuritySettings -> onOpenSecuritySettings()
        }
    }
    OnboardingContent(state, viewModel::onIntent, onOpenSecuritySettings, onOpenTerms, onOpenPrivacy)
}

@Composable
internal fun OnboardingContent(
    state: State,
    onIntent: (Intent) -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val backEnabled = state.step in CREATE_STEPS || state.step in RESTORE_STEPS || state.step == Step.DeviceNotSecure
    PlatformBackHandler(enabled = backEnabled) {
        onIntent(Intent.Back)
    }
    when (state.step) {
        Step.Welcome -> WelcomeStep(state.error, onIntent, onOpenTerms, onOpenPrivacy)
        Step.DeviceNotSecure -> DeviceNotSecureStep(onOpenSecuritySettings, onIntent)
        Step.Creating, Step.Finishing, Step.Restoring -> FullScreenProgress()
        Step.RestorePickFile, Step.RestorePhrase -> RestoreFlow(state, onIntent)
        Step.ShowId, Step.ShowPhrase, Step.ConfirmPhrase -> CreateFlow(state, onIntent)
    }
}

@Composable
internal fun errorText(error: Error?): String? =
    when (error) {
        null -> null
        Error.UnlockNeeded -> stringResource(Res.string.error_unlock_needed)
        is Error.CreateFailed -> stringResource(Res.string.error_create_failed, error.reason.orEmpty())
        Error.WordsDontMatch -> stringResource(Res.string.error_words_dont_match)
        is Error.FinishFailed -> stringResource(Res.string.error_finish_failed, error.reason.orEmpty())
        Error.BackupUnreadable -> stringResource(Res.string.error_backup_unreadable)
        Error.InvalidPhrase -> stringResource(Res.string.error_invalid_phrase)
        Error.PhraseDoesNotMatch -> stringResource(Res.string.error_phrase_mismatch)
        Error.DeviceLockNeeded -> stringResource(Res.string.error_device_lock_needed)
        is Error.RestoreFailed -> stringResource(Res.string.error_restore_failed, error.reason.orEmpty())
    }

// --- 00 · Welcome ---------------------------------------------------------------------------

@Composable
private fun WelcomeStep(
    error: Error?,
    onIntent: (Intent) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = Dimens.heroPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppLogo()
            Text(
                stringResource(Res.string.app_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = Dimens.dp8),
            )
            Text(
                stringResource(Res.string.welcome_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.padding(start = Dimens.dp24, end = Dimens.dp24, bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            ErrorText(errorText(error), Modifier.fillMaxWidth(), TextAlign.Center)
            ListGroup {
                // Linking another device needs cloud sync: not in the local-only MVP. Restoring uses a backup file.
                ListRow(
                    title = stringResource(Res.string.welcome_create_title),
                    subtitle = stringResource(Res.string.welcome_create_subtitle),
                    leadingIcon = Icons.Outlined.AddCircle,
                    trailing = { ChevronTrailing() },
                    onClick = { onIntent(Intent.Start) },
                )
                ListDivider()
                ListRow(
                    title = stringResource(Res.string.welcome_restore_title),
                    subtitle = stringResource(Res.string.welcome_restore_subtitle),
                    leadingIcon = Icons.Outlined.SettingsBackupRestore,
                    trailing = { ChevronTrailing() },
                    onClick = { onIntent(Intent.StartRestore) },
                )
            }
            LegalText(onOpenTerms, onOpenPrivacy)
        }
    }
}

@Composable
private fun LegalText(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val linkStyle =
        TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
    val prefix = stringResource(Res.string.welcome_legal_prefix)
    val terms = stringResource(Res.string.welcome_legal_terms)
    val and = stringResource(Res.string.welcome_legal_and)
    val privacy = stringResource(Res.string.welcome_legal_privacy)
    val text = buildAnnotatedString {
        append(prefix)
        withLink(LinkAnnotation.Clickable(terms, linkStyle) { onOpenTerms() }) { append(terms) }
        append(and)
        withLink(LinkAnnotation.Clickable(privacy, linkStyle) { onOpenPrivacy() }) { append(privacy) }
    }
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.dp8),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

// --- Device has no screen lock ---------------------------------------------------------------

@Composable
private fun DeviceNotSecureStep(
    onOpenSettings: () -> Unit,
    onIntent: (Intent) -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            HeroMessage(
                icon = Icons.Filled.Lock,
                title = stringResource(Res.string.device_not_secure_title),
                body = stringResource(Res.string.device_not_secure_body),
            )
        }
        BottomActions {
            SecondaryButton(stringResource(Res.string.device_not_secure_open_settings), onOpenSettings)
            PrimaryButton(stringResource(Res.string.device_not_secure_retry), { onIntent(Intent.Start) })
        }
    }
}

// --- 11–13 · Create account -------------------------------------------------------------------

@Composable
private fun CreateFlow(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(Res.string.create_title),
                navIcon = NavIcon.Back,
                onNavigate = { onIntent(Intent.Back) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SegmentBar(
                count = CREATE_STEPS.size,
                filled = CREATE_STEPS.indexOf(state.step) + 1,
                modifier = Modifier.padding(horizontal = Dimens.dp24),
            )
            when (state.step) {
                Step.ShowId -> IdStep(state, onIntent, snackbar)
                Step.ShowPhrase -> PhraseStep(state, onIntent, snackbar)
                else -> ConfirmStep(state, onIntent)
            }
        }
    }
}

@Composable
private fun IdStep(
    state: State,
    onIntent: (Intent) -> Unit,
    snackbar: SnackbarHostState,
) {
    val clipboard = rememberSecureClipboard()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            HeroMessage(
                icon = Icons.Filled.Fingerprint,
                title = stringResource(Res.string.create_id_title),
                body = stringResource(Res.string.create_id_body),
            ) {
                Surface(
                    modifier = Modifier.padding(top = Dimens.dp8),
                    shape = RoundedCornerShape(Dimens.radiusLarge),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        state.accountId,
                        modifier = Modifier.padding(horizontal = Dimens.dp20, vertical = Dimens.dp16),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                val label = stringResource(Res.string.create_id_label)
                TonalButton(
                    text = stringResource(CoreRes.string.action_copy),
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        clipboard.copy(state.accountId, sensitive = false, clearAfterSeconds = null)
                        scope.launch { snackbar.showSnackbar(getString(CoreRes.string.copied, label)) }
                    },
                )
            }
        }
        BottomActions { PrimaryButton(stringResource(CoreRes.string.action_continue), { onIntent(Intent.IdSeen) }) }
    }
}

@Composable
private fun PhraseStep(
    state: State,
    onIntent: (Intent) -> Unit,
    snackbar: SnackbarHostState,
) {
    val clipboard = rememberSecureClipboard()
    val scope = rememberCoroutineScope()
    val saver = rememberTextFileSaver(TextFileFormat.RecoveryKey) { saved ->
        scope.launch {
            snackbar.showSnackbar(
                getString(if (saved) Res.string.create_key_file_saved else Res.string.create_key_file_failed),
            )
        }
    }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(
                    1f,
                ).verticalScroll(rememberScrollState())
                .padding(start = Dimens.dp24, end = Dimens.dp24, top = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp16),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.create_key_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.create_key_body, state.words.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WordGrid(state.words)

            val copyLabel = stringResource(Res.string.create_key_copy_label)
            val fileName = stringResource(Res.string.recovery_file_name, state.accountId)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                TonalButton(
                    text = stringResource(CoreRes.string.action_copy),
                    icon = Icons.Outlined.ContentCopy,
                    onClick = {
                        clipboard.copy(
                            state.words.joinToString(" "),
                            sensitive = true,
                            clearAfterSeconds = RECOVERY_CLIPBOARD_SECONDS,
                        )
                        scope.launch {
                            snackbar.showSnackbar(
                                getString(CoreRes.string.copied_clears_in, copyLabel, RECOVERY_CLIPBOARD_SECONDS),
                            )
                        }
                    },
                )
                TonalButton(
                    text = stringResource(Res.string.create_key_save_txt),
                    icon = Icons.Outlined.Description,
                    onClick = {
                        scope.launch {
                            val lines = state.words.mapIndexed {
                                i,
                                w,
                                ->
                                getString(Res.string.recovery_word_line, i + 1, w)
                            }
                            saver.save(
                                fileName,
                                getString(Res.string.recovery_file_content, state.accountId, lines.joinToString("\n")),
                            )
                        }
                    },
                )
            }
            WarningCard(stringResource(Res.string.create_key_warning))
        }
        BottomActions {
            Row(
                Modifier.fillMaxWidth().clickable { onIntent(Intent.PhraseSavedChanged(!state.phraseSaved)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = state.phraseSaved, onCheckedChange = { onIntent(Intent.PhraseSavedChanged(it)) })
                Text(stringResource(Res.string.create_key_saved_check), style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryButton(stringResource(CoreRes.string.action_continue), {
                onIntent(Intent.PhraseWrittenDown)
            }, enabled = state.phraseSaved)
        }
    }
}

@Composable
private fun WordGrid(words: List<String>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
        words.chunked(WORD_COLUMNS).forEachIndexed { row, chunk ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                chunk.forEachIndexed { col, word ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.radiusSmall),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            Modifier.padding(horizontal = Dimens.dp8, vertical = Dimens.dp10),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.dp4),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${row * WORD_COLUMNS + col + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(word, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                repeat(WORD_COLUMNS - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String) {
    Surface(
        shape = RoundedCornerShape(Dimens.radiusMedium),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(Modifier.fillMaxWidth().padding(Dimens.dp12), horizontalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
            Icon(Icons.Outlined.Warning, contentDescription = null)
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConfirmStep(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(
                    1f,
                ).verticalScroll(rememberScrollState())
                .padding(start = Dimens.dp24, end = Dimens.dp24, top = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp20),
        ) {
            Text(
                stringResource(Res.string.confirm_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(Res.string.confirm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            state.confirmIndices.forEachIndexed { slot, wordIndex ->
                val answer = state.answers.getOrNull(slot)
                AppTextField(
                    value = answer.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(Res.string.confirm_word_label, wordIndex + 1),
                    trailing = if (answer != null) {
                        {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = stringResource(Res.string.cd_clear_word),
                                modifier = Modifier.clickable { onIntent(Intent.SlotCleared(slot)) },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            ErrorText(errorText(state.error))
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                state.choices.chunked(WORD_COLUMNS).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.dp8)) {
                        row.forEach { word ->
                            FilterChip(
                                selected = word in state.answers,
                                onClick = { onIntent(Intent.ChoiceTapped(word)) },
                                label = {
                                    Text(
                                        word,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(WORD_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        BottomActions {
            PrimaryButton(
                stringResource(Res.string.confirm_finish),
                { onIntent(Intent.SubmitConfirmation) },
                enabled = state.canSubmitConfirmation,
            )
        }
    }
}
