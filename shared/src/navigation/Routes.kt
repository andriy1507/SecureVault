package dev.spaceapps.secvault.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.feature.generator.presentation.GeneratorViewModel
import dev.spaceapps.secvault.feature.generator.ui.GeneratorScreen
import dev.spaceapps.secvault.feature.lock.presentation.LockViewModel
import dev.spaceapps.secvault.feature.lock.ui.LockScreen
import dev.spaceapps.secvault.feature.onboarding.presentation.OnboardingViewModel
import dev.spaceapps.secvault.feature.onboarding.ui.OnboardingScreen
import dev.spaceapps.secvault.feature.settings.presentation.SettingsViewModel
import dev.spaceapps.secvault.feature.settings.ui.SettingsPlatform
import dev.spaceapps.secvault.feature.settings.ui.SettingsScreen
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupViewModel
import dev.spaceapps.secvault.feature.vault.presentation.detail.ItemDetailViewModel
import dev.spaceapps.secvault.feature.vault.presentation.edit.ItemEditViewModel
import dev.spaceapps.secvault.feature.vault.presentation.list.VaultListViewModel
import dev.spaceapps.secvault.feature.vault.presentation.scan.CodePrefill
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeViewModel
import dev.spaceapps.secvault.feature.vault.presentation.trash.TrashViewModel
import dev.spaceapps.secvault.feature.vault.ui.BackupScreen
import dev.spaceapps.secvault.feature.vault.ui.ItemDetailScreen
import dev.spaceapps.secvault.feature.vault.ui.ItemEditScreen
import dev.spaceapps.secvault.feature.vault.ui.ScanCodeScreen
import dev.spaceapps.secvault.feature.vault.ui.TrashScreen
import dev.spaceapps.secvault.feature.vault.ui.VaultListScreen
import org.koin.compose.viewmodel.koinViewModel

// Each route gets its ViewModel from Koin and hands the screen plain state and lambdas, so screens
// in the *-ui modules stay stateless and previewable.

@Composable
internal fun LockRoute() {
    val viewModel = koinViewModel<LockViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LockScreen(state, viewModel::onIntent)
}

@Composable
internal fun OnboardingRoute(
    onOpenSecuritySettings: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val viewModel = koinViewModel<OnboardingViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onOpenSecuritySettings = onOpenSecuritySettings,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
    )
}

@Composable
internal fun GeneratorRoute() {
    val viewModel = koinViewModel<GeneratorViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    GeneratorScreen(state, viewModel.effects, viewModel::onIntent)
}

@Composable
internal fun SettingsRoute(
    platform: SettingsPlatform,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        platform = platform,
        onOpenTerms = onOpenTerms,
        onOpenPrivacy = onOpenPrivacy,
        onOpenBackup = onOpenBackup,
    )
}

@Composable
internal fun VaultListRoute(
    onOpenItem: (id: String) -> Unit,
    onEditItem: (id: String) -> Unit,
    onNewItem: (type: ItemType, title: String) -> Unit,
    onItemTrashed: (id: String, title: String) -> Unit,
    onOpenTrash: () -> Unit,
    onScanCode: () -> Unit,
    onLock: () -> Unit,
) {
    val viewModel = koinViewModel<VaultListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    VaultListScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onOpenItem = onOpenItem,
        onEditItem = onEditItem,
        onNewItem = onNewItem,
        onItemTrashed = onItemTrashed,
        onOpenTrash = onOpenTrash,
        onScanCode = onScanCode,
        onLock = onLock,
    )
}

@Composable
internal fun ItemDetailRoute(
    itemId: String,
    refreshKey: Int,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onTrashed: (id: String, title: String) -> Unit,
) {
    val viewModel = koinViewModel<ItemDetailViewModel>(key = "detail:$itemId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    ItemDetailScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onReload = viewModel::reload,
        itemId = itemId,
        refreshKey = refreshKey,
        onBack = onBack,
        onEdit = onEdit,
        onTrashed = onTrashed,
    )
}

@Composable
internal fun ItemEditRoute(
    itemId: String?,
    newType: ItemType,
    newTitle: String,
    onClose: () -> Unit,
    onSaved: (id: String, isNew: Boolean) -> Unit,
    onTrashed: (id: String, title: String) -> Unit,
    newUsername: String = "",
    newTotpSecret: String = "",
) {
    val viewModel = koinViewModel<ItemEditViewModel>(key = "edit:${itemId ?: "new"}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    ItemEditScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        itemId = itemId,
        newType = newType,
        newTitle = newTitle,
        onClose = onClose,
        onSaved = onSaved,
        onTrashed = onTrashed,
        scanCode = { onFound: (CodePrefill) -> Unit, onScanClose: () -> Unit ->
            ScanCodeRoute(
                onFound = onFound,
                onEnterManually = onScanClose,
                onClose = onScanClose,
                viewModelKey = "scan:edit",
            )
        },
        newUsername = newUsername,
        newTotpSecret = newTotpSecret,
    )
}

@Composable
internal fun TrashRoute(onBack: () -> Unit) {
    val viewModel = koinViewModel<TrashViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TrashScreen(state, viewModel.effects, viewModel::onIntent, onBack)
}

@Composable
internal fun BackupRoute(onBack: () -> Unit) {
    val viewModel = koinViewModel<BackupViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackupScreen(state, viewModel.effects, viewModel::onIntent, onBack)
}

@Composable
internal fun ScanCodeRoute(
    onFound: (CodePrefill) -> Unit,
    onEnterManually: () -> Unit,
    onClose: () -> Unit,
    viewModelKey: String = "scan",
) {
    val viewModel = koinViewModel<ScanCodeViewModel>(key = viewModelKey)
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScanCodeScreen(
        state = state,
        effects = viewModel.effects,
        onIntent = viewModel::onIntent,
        onFound = onFound,
        onEnterManually = onEnterManually,
        onClose = onClose,
    )
}
