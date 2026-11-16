package dev.spaceapps.secvault.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPasteOff
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import dev.spaceapps.secvault.core.ui.clipboard.rememberCopyAction
import dev.spaceapps.secvault.core.ui.components.AppTextField
import dev.spaceapps.secvault.core.ui.components.AppTopBar
import dev.spaceapps.secvault.core.ui.components.ChevronTrailing
import dev.spaceapps.secvault.core.ui.components.CollectEffects
import dev.spaceapps.secvault.core.ui.components.ConfirmDialog
import dev.spaceapps.secvault.core.ui.components.FilledIconTextButton
import dev.spaceapps.secvault.core.ui.components.ListDivider
import dev.spaceapps.secvault.core.ui.components.ListGroup
import dev.spaceapps.secvault.core.ui.components.ListRow
import dev.spaceapps.secvault.core.ui.components.RadioOptionDialog
import dev.spaceapps.secvault.core.ui.components.SectionHeader
import dev.spaceapps.secvault.core.ui.components.SwitchRow
import dev.spaceapps.secvault.core.ui.resources.Res as CoreRes
import dev.spaceapps.secvault.core.ui.resources.action_delete
import dev.spaceapps.secvault.core.ui.theme.Dimens
import dev.spaceapps.secvault.feature.settings.domain.AppTheme
import dev.spaceapps.secvault.feature.settings.domain.AutoLock
import dev.spaceapps.secvault.feature.settings.domain.ClipboardClear
import dev.spaceapps.secvault.feature.settings.domain.DELETE_CONFIRMATION
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Dialog
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Effect
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Intent
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.State
import dev.spaceapps.secvault.feature.settings.resources.Res
import dev.spaceapps.secvault.feature.settings.resources.account_id_label
import dev.spaceapps.secvault.feature.settings.resources.account_id_title
import dev.spaceapps.secvault.feature.settings.resources.auto_lock
import dev.spaceapps.secvault.feature.settings.resources.auto_lock_15_min
import dev.spaceapps.secvault.feature.settings.resources.auto_lock_1_min
import dev.spaceapps.secvault.feature.settings.resources.auto_lock_5_min
import dev.spaceapps.secvault.feature.settings.resources.auto_lock_device
import dev.spaceapps.secvault.feature.settings.resources.auto_lock_immediately
import dev.spaceapps.secvault.feature.settings.resources.autofill
import dev.spaceapps.secvault.feature.settings.resources.autofill_subtitle
import dev.spaceapps.secvault.feature.settings.resources.backup
import dev.spaceapps.secvault.feature.settings.resources.backup_subtitle
import dev.spaceapps.secvault.feature.settings.resources.block_screenshots
import dev.spaceapps.secvault.feature.settings.resources.block_screenshots_subtitle
import dev.spaceapps.secvault.feature.settings.resources.clear_10_s
import dev.spaceapps.secvault.feature.settings.resources.clear_1_min
import dev.spaceapps.secvault.feature.settings.resources.clear_30_s
import dev.spaceapps.secvault.feature.settings.resources.clear_clipboard
import dev.spaceapps.secvault.feature.settings.resources.clear_never
import dev.spaceapps.secvault.feature.settings.resources.copy_id
import dev.spaceapps.secvault.feature.settings.resources.delete_account
import dev.spaceapps.secvault.feature.settings.resources.delete_account_subtitle
import dev.spaceapps.secvault.feature.settings.resources.delete_account_text
import dev.spaceapps.secvault.feature.settings.resources.delete_account_title
import dev.spaceapps.secvault.feature.settings.resources.dynamic_color
import dev.spaceapps.secvault.feature.settings.resources.dynamic_color_subtitle
import dev.spaceapps.secvault.feature.settings.resources.lock_now
import dev.spaceapps.secvault.feature.settings.resources.passkeys
import dev.spaceapps.secvault.feature.settings.resources.passkeys_subtitle
import dev.spaceapps.secvault.feature.settings.resources.privacy
import dev.spaceapps.secvault.feature.settings.resources.section_about
import dev.spaceapps.secvault.feature.settings.resources.section_appearance
import dev.spaceapps.secvault.feature.settings.resources.section_autofill
import dev.spaceapps.secvault.feature.settings.resources.section_backup
import dev.spaceapps.secvault.feature.settings.resources.section_danger
import dev.spaceapps.secvault.feature.settings.resources.section_security
import dev.spaceapps.secvault.feature.settings.resources.settings_title
import dev.spaceapps.secvault.feature.settings.resources.terms
import dev.spaceapps.secvault.feature.settings.resources.theme
import dev.spaceapps.secvault.feature.settings.resources.theme_dark
import dev.spaceapps.secvault.feature.settings.resources.theme_light
import dev.spaceapps.secvault.feature.settings.resources.theme_system
import dev.spaceapps.secvault.feature.settings.resources.version
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val THEMES: List<Pair<AppTheme, StringResource>> = listOf(
    AppTheme.Light to Res.string.theme_light,
    AppTheme.Dark to Res.string.theme_dark,
    AppTheme.System to Res.string.theme_system,
)

private val AUTO_LOCKS: List<Pair<AutoLock, StringResource>> = listOf(
    AutoLock.Immediately to Res.string.auto_lock_immediately,
    AutoLock.OneMinute to Res.string.auto_lock_1_min,
    AutoLock.FiveMinutes to Res.string.auto_lock_5_min,
    AutoLock.FifteenMinutes to Res.string.auto_lock_15_min,
    AutoLock.OnDeviceLock to Res.string.auto_lock_device,
)

private val CLIPBOARD: List<Pair<ClipboardClear, StringResource>> = listOf(
    ClipboardClear.TenSeconds to Res.string.clear_10_s,
    ClipboardClear.ThirtySeconds to Res.string.clear_30_s,
    ClipboardClear.OneMinute to Res.string.clear_1_min,
    ClipboardClear.Never to Res.string.clear_never,
)

private fun <T> List<Pair<T, StringResource>>.labelOf(value: T): StringResource = first { it.first == value }.second

/** Platform capabilities the settings screen adapts to. */
data class SettingsPlatform(
    val appVersion: String,
    /** Null where the system has no autofill provider setting to open (iOS). */
    val onOpenAutofill: (() -> Unit)?,
    /** Null where Vault can't be a passkey provider (Android before 14, iOS for now). */
    val onOpenPasskeys: (() -> Unit)? = null,
    val dynamicColorSupported: Boolean,
    val screenshotBlockingSupported: Boolean,
)

/** 03 · Settings, 08 · Delete account, 42 · Theme, 43 · Auto-lock, 44 · Clear clipboard. */
@Composable
fun SettingsScreen(
    state: State,
    effects: Flow<Effect>,
    onIntent: (Intent) -> Unit,
    platform: SettingsPlatform,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val copy = rememberCopyAction()
    val scope = rememberCoroutineScope()
    CollectEffects(effects) { effect ->
        when (effect) {
            is Effect.CopyId -> scope.launch {
                copy.copy(effect.id, sensitive = false, label = getString(Res.string.account_id_label))
            }
        }
    }
    SettingsContent(state, platform, onIntent, onOpenTerms, onOpenPrivacy, onOpenBackup)
}

@Composable
internal fun SettingsContent(
    state: State,
    platform: SettingsPlatform,
    onIntent: (Intent) -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val s = state.settings
    Scaffold(topBar = { AppTopBar(stringResource(Res.string.settings_title)) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.screenPadding, end = Dimens.screenPadding, bottom = Dimens.dp24),
            verticalArrangement = Arrangement.spacedBy(Dimens.dp12),
        ) {
            IdCard(state.accountId) { onIntent(Intent.CopyId) }

            if (platform.onOpenAutofill != null || platform.onOpenPasskeys != null) {
                SectionHeader(stringResource(Res.string.section_autofill))
                ListGroup {
                    platform.onOpenAutofill?.let { openAutofill ->
                        ListRow(
                            title = stringResource(Res.string.autofill),
                            subtitle = stringResource(Res.string.autofill_subtitle),
                            leadingIcon = Icons.Outlined.AutoAwesome,
                            trailing = { ChevronTrailing() },
                            onClick = openAutofill,
                        )
                    }
                    platform.onOpenPasskeys?.let { openPasskeys ->
                        if (platform.onOpenAutofill != null) ListDivider()
                        ListRow(
                            title = stringResource(Res.string.passkeys),
                            subtitle = stringResource(Res.string.passkeys_subtitle),
                            leadingIcon = Icons.Outlined.Key,
                            trailing = { ChevronTrailing() },
                            onClick = openPasskeys,
                        )
                    }
                }
            }

            SectionHeader(stringResource(Res.string.section_appearance))
            ListGroup {
                ListRow(
                    title = stringResource(Res.string.theme),
                    subtitle = stringResource(THEMES.labelOf(s.theme)),
                    leadingIcon = Icons.Outlined.Palette,
                    trailing = { ChevronTrailing() },
                    onClick = { onIntent(Intent.ShowDialog(Dialog.Theme)) },
                )
                if (platform.dynamicColorSupported) {
                    SwitchRow(
                        title = stringResource(Res.string.dynamic_color),
                        subtitle = stringResource(Res.string.dynamic_color_subtitle),
                        checked = s.dynamicColor,
                        onCheckedChange = { onIntent(Intent.DynamicColorChanged(it)) },
                        leadingIcon = Icons.Outlined.Colorize,
                    )
                }
            }

            SectionHeader(stringResource(Res.string.section_security))
            ListGroup {
                ListRow(
                    title = stringResource(Res.string.auto_lock),
                    subtitle = stringResource(AUTO_LOCKS.labelOf(s.autoLock)),
                    leadingIcon = Icons.Outlined.Timer,
                    trailing = { ChevronTrailing() },
                    onClick = { onIntent(Intent.ShowDialog(Dialog.AutoLock)) },
                )
                ListRow(
                    title = stringResource(Res.string.clear_clipboard),
                    subtitle = stringResource(CLIPBOARD.labelOf(s.clipboardClear)),
                    leadingIcon = Icons.Outlined.ContentPasteOff,
                    trailing = { ChevronTrailing() },
                    onClick = { onIntent(Intent.ShowDialog(Dialog.ClipboardClear)) },
                )
                if (platform.screenshotBlockingSupported) {
                    SwitchRow(
                        title = stringResource(Res.string.block_screenshots),
                        subtitle = stringResource(Res.string.block_screenshots_subtitle),
                        checked = s.blockScreenshots,
                        onCheckedChange = { onIntent(Intent.BlockScreenshotsChanged(it)) },
                        leadingIcon = Icons.Outlined.ScreenshotMonitor,
                    )
                }
                ListRow(
                    title = stringResource(Res.string.lock_now),
                    leadingIcon = Icons.Outlined.Lock,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = { onIntent(Intent.LockNow) },
                )
            }

            SectionHeader(stringResource(Res.string.section_backup))
            ListGroup {
                ListRow(
                    title = stringResource(Res.string.backup),
                    subtitle = stringResource(Res.string.backup_subtitle),
                    leadingIcon = Icons.Outlined.SettingsBackupRestore,
                    trailing = { ChevronTrailing() },
                    onClick = onOpenBackup,
                )
            }

            SectionHeader(stringResource(Res.string.section_about))
            ListGroup {
                ListRow(
                    title = stringResource(Res.string.version),
                    subtitle = platform.appVersion,
                    leadingIcon = Icons.Outlined.Info,
                )
                ListRow(
                    title = stringResource(Res.string.terms),
                    leadingIcon = Icons.Outlined.Gavel,
                    trailing = { ChevronTrailing() },
                    onClick = onOpenTerms,
                )
                ListRow(
                    title = stringResource(Res.string.privacy),
                    leadingIcon = Icons.Outlined.Policy,
                    trailing = { ChevronTrailing() },
                    onClick = onOpenPrivacy,
                )
            }

            SectionHeader(stringResource(Res.string.section_danger), color = MaterialTheme.colorScheme.error)
            ListGroup {
                ListRow(
                    title = stringResource(Res.string.delete_account),
                    subtitle = stringResource(Res.string.delete_account_subtitle),
                    leadingIcon = Icons.Outlined.DeleteForever,
                    contentColor = MaterialTheme.colorScheme.error,
                    onClick = { onIntent(Intent.ShowDialog(Dialog.DeleteAccount)) },
                )
            }
        }
    }

    Dialogs(state, onIntent)
}

@Composable
private fun IdCard(
    accountId: String,
    onCopy: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.radiusExtraLarge),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(Dimens.dp20), verticalArrangement = Arrangement.spacedBy(Dimens.dp12)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.dp12),
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Text(stringResource(Res.string.account_id_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(accountId, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
            FilledIconTextButton(stringResource(Res.string.copy_id), onCopy, icon = Icons.Outlined.ContentCopy)
        }
    }
}

@Composable
private fun Dialogs(
    state: State,
    onIntent: (Intent) -> Unit,
) {
    val dismiss = { onIntent(Intent.ShowDialog(Dialog.None)) }
    when (state.dialog) {
        Dialog.None -> Unit

        Dialog.Theme -> RadioOptionDialog(
            title = stringResource(Res.string.theme),
            options = THEMES.map { stringResource(it.second) },
            selectedIndex = THEMES.indexOfFirst { it.first == state.settings.theme },
            onConfirm = { onIntent(Intent.ThemeChosen(THEMES[it].first)) },
            onDismiss = dismiss,
        )

        Dialog.AutoLock -> RadioOptionDialog(
            title = stringResource(Res.string.auto_lock),
            options = AUTO_LOCKS.map { stringResource(it.second) },
            selectedIndex = AUTO_LOCKS.indexOfFirst { it.first == state.settings.autoLock },
            onConfirm = { onIntent(Intent.AutoLockChosen(AUTO_LOCKS[it].first)) },
            onDismiss = dismiss,
        )

        Dialog.ClipboardClear -> RadioOptionDialog(
            title = stringResource(Res.string.clear_clipboard),
            options = CLIPBOARD.map { stringResource(it.second) },
            selectedIndex = CLIPBOARD.indexOfFirst { it.first == state.settings.clipboardClear },
            onConfirm = { onIntent(Intent.ClipboardClearChosen(CLIPBOARD[it].first)) },
            onDismiss = dismiss,
        )

        Dialog.DeleteAccount -> ConfirmDialog(
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(Res.string.delete_account_title),
            text = stringResource(Res.string.delete_account_text, DELETE_CONFIRMATION),
            confirmText = stringResource(CoreRes.string.action_delete),
            confirmEnabled = state.canDelete,
            onConfirm = { onIntent(Intent.ConfirmDeleteAccount) },
            onDismiss = dismiss,
        ) {
            AppTextField(
                value = state.deleteConfirmation,
                onValueChange = { onIntent(Intent.DeleteConfirmationChanged(it)) },
                label = DELETE_CONFIRMATION,
            )
        }
    }
}
