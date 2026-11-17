package dev.spaceapps.secvault.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.spaceapps.secvault.core.domain.model.ItemType
import dev.spaceapps.secvault.core.ui.components.LocalSnackbarHostState
import dev.spaceapps.secvault.core.ui.navigation.PlatformBackHandler
import dev.spaceapps.secvault.feature.legal.ui.LegalDocument
import dev.spaceapps.secvault.feature.legal.ui.LegalScreen
import dev.spaceapps.secvault.feature.settings.ui.SettingsPlatform
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromTrashUseCase
import dev.spaceapps.secvault.resources.Res
import dev.spaceapps.secvault.resources.tab_generator
import dev.spaceapps.secvault.resources.tab_settings
import dev.spaceapps.secvault.resources.tab_vault
import dev.spaceapps.secvault.resources.trashed_snackbar
import dev.spaceapps.secvault.rememberIsChangingConfigurations
import dev.spaceapps.secvault.resources.undo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

internal enum class Tab(
    val label: StringResource,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Vault(Res.string.tab_vault, Icons.Outlined.Lock, Icons.Filled.Lock),
    Generator(Res.string.tab_generator, Icons.Outlined.Password, Icons.Filled.Password),
    Settings(Res.string.tab_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}

private sealed interface Route {
    data class Tabs(
        val tab: Tab,
    ) : Route

    data class ItemDetail(
        val id: String,
    ) : Route

    /** [id] == null: a new item of [type], optionally prefilled (search text, scanned 2FA code). */
    data class ItemEdit(
        val id: String?,
        val type: ItemType = ItemType.Login,
        val title: String = "",
        val username: String = "",
        val totpSecret: String = "",
    ) : Route {
        override fun toString() = "ItemEdit(id=$id)"
    }

    data object ScanCode : Route

    data object Trash : Route

    data object Backup : Route

    data class Legal(
        val document: LegalDocument,
    ) : Route
}

/** One back stack entry; [key] identifies its saved UI state (scroll positions, text fields). */
private class Entry(
    val route: Route,
    val key: String,
)

/**
 * Back stack of the unlocked app. The root is always the tab screen.
 * [onRemoved] is called with the key of every pushed entry that leaves the stack, so its saved state is dropped.
 */
private class BackStack(
    private val onRemoved: (String) -> Unit,
) {
    private var nextId = 0
    val entries = mutableStateListOf(Entry(Route.Tabs(Tab.Vault), ROOT_KEY))
    val top: Entry get() = entries.last()

    private fun entry(route: Route) = Entry(route, "entry-${nextId++}")

    private fun removeLast() {
        onRemoved(entries.removeAt(entries.lastIndex).key)
    }

    fun push(route: Route) {
        entries += entry(route)
    }

    fun pop() {
        if (entries.size > 1) removeLast()
    }

    fun replaceTop(route: Route) {
        onRemoved(entries[entries.lastIndex].key)
        entries[entries.lastIndex] = entry(route)
    }

    fun popToRoot() {
        while (entries.size > 1) removeLast()
    }

    fun selectTab(tab: Tab) {
        popToRoot()
        entries[0] = Entry(Route.Tabs(tab), ROOT_KEY)
    }

    companion object {
        const val ROOT_KEY = "root"
    }
}

/** Saved-state key of a tab: each tab keeps its own scroll position while you switch between them. */
private fun tabStateKey(tab: Tab) = "tab-${tab.name}"

/**
 * Screens of the unlocked vault.
 * ViewModels live in stores scoped to the composition: the session store is cleared on lock,
 * and each pushed screen's store when that screen closes, so decrypted data never outlives its screen.
 */
@Composable
internal fun VaultNavHost(
    onLock: () -> Unit,
    settingsPlatform: SettingsPlatform,
) {
    // Keeps each screen's rememberSaveable state (e.g. list scroll position) while it is covered
    // by another screen or tab, and restores it on back.
    val saveableState = rememberSaveableStateHolder()
    val stack = remember { BackStack(onRemoved = saveableState::removeState) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val restoreFromTrash = koinInject<RestoreFromTrashUseCase>()

    fun onTrashed(
        id: String,
        title: String,
    ) {
        stack.popToRoot()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = getString(Res.string.trashed_snackbar, title),
                actionLabel = getString(Res.string.undo),
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) restoreFromTrash(id)
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbar) {
        ScopedViewModels {
            val entry = stack.top
            val top = entry.route
            if (top !is Route.Tabs) PlatformBackHandler { stack.pop() }
            when (top) {
                is Route.Tabs -> Tabs(
                    tab = top.tab,
                    saveableState = saveableState,
                    snackbar = snackbar,
                    onSelect = stack::selectTab,
                    vault = {
                        VaultListRoute(
                            onOpenItem = { stack.push(Route.ItemDetail(it)) },
                            onEditItem = { stack.push(Route.ItemEdit(it)) },
                            onNewItem = { type, title -> stack.push(Route.ItemEdit(null, type, title)) },
                            onItemTrashed = ::onTrashed,
                            onOpenTrash = { stack.push(Route.Trash) },
                            onScanCode = { stack.push(Route.ScanCode) },
                            onLock = onLock,
                        )
                    },
                    settings = {
                        SettingsRoute(
                            platform = settingsPlatform,
                            onOpenTerms = { stack.push(Route.Legal(LegalDocument.Terms)) },
                            onOpenPrivacy = { stack.push(Route.Legal(LegalDocument.Privacy)) },
                            onOpenBackup = { stack.push(Route.Backup) },
                        )
                    },
                )

                else -> Box(Modifier.fillMaxSize()) {
                    key(entry.key) {
                        saveableState.SaveableStateProvider(entry.key) {
                            ScopedViewModels { PushedScreen(top, stack, ::onTrashed) }
                        }
                    }
                    SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding())
                }
            }
        }
    }
}

@Composable
private fun PushedScreen(
    route: Route,
    stack: BackStack,
    onTrashed: (String, String) -> Unit,
) {
    when (route) {
        is Route.ItemDetail -> ItemDetailRoute(
            itemId = route.id,
            refreshKey = 0,
            onBack = stack::pop,
            onEdit = { stack.push(Route.ItemEdit(it)) },
            onTrashed = onTrashed,
        )

        is Route.ItemEdit -> ItemEditRoute(
            itemId = route.id,
            newType = route.type,
            newTitle = route.title,
            newUsername = route.username,
            newTotpSecret = route.totpSecret,
            onClose = stack::pop,
            onSaved = { id, isNew -> if (isNew) stack.replaceTop(Route.ItemDetail(id)) else stack.pop() },
            onTrashed = onTrashed,
        )

        Route.Trash -> TrashRoute(onBack = stack::pop)

        Route.Backup -> BackupRoute(onBack = stack::pop)

        Route.ScanCode -> ScanCodeRoute(
            onFound = { stack.replaceTop(Route.ItemEdit(null, ItemType.Login, it.issuer, it.account, it.secret)) },
            onEnterManually = { stack.replaceTop(Route.ItemEdit(null, ItemType.Login)) },
            onClose = stack::pop,
        )

        is Route.Legal -> LegalScreen(route.document, onBack = stack::pop)

        is Route.Tabs -> Unit
    }
}

@Composable
private fun Tabs(
    tab: Tab,
    saveableState: SaveableStateHolder,
    snackbar: SnackbarHostState,
    onSelect: (Tab) -> Unit,
    vault: @Composable () -> Unit,
    settings: @Composable () -> Unit,
) {
    Scaffold(
        // Each tab screen handles the status bar itself; this scaffold only adds the bottom bar.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    val selected = entry == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelect(entry) },
                        icon = { Icon(if (selected) entry.selectedIcon else entry.icon, contentDescription = null) },
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            key(tab) {
                saveableState.SaveableStateProvider(tabStateKey(tab)) {
                    when (tab) {
                        Tab.Vault -> vault()
                        Tab.Generator -> GeneratorRoute()
                        Tab.Settings -> settings()
                    }
                }
            }
        }
    }
}

/**
 * Keeps the [ViewModelStore]s of [ScopedViewModels] in the parent owner (the Activity at the top), so they
 * survive Activity recreation such as a theme change, rotation or split screen.
 */
internal class ScopedStoreHolder : ViewModel() {
    private val stores = mutableMapOf<Int, ViewModelStore>()
    private var nextKey = 0

    fun newKey(): Int = nextKey++

    fun storeFor(key: Int): ViewModelStore {
        nextKey = maxOf(nextKey, key + 1) // keys restored after process death must not be handed out again
        return stores.getOrPut(key) { ViewModelStore() }
    }

    fun clear(key: Int) {
        stores.remove(key)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}

/**
 * Provides a ViewModelStore that is cleared when [content] leaves composition for good (lock, onboarding done).
 * It is kept when the Activity is only recreated, so e.g. the recovery key on screen is not lost.
 */
@Composable
internal fun ScopedViewModels(content: @Composable () -> Unit) {
    val holder = viewModel { ScopedStoreHolder() }
    val key = rememberSaveable { holder.newKey() }
    val owner = remember(holder, key) {
        object : ViewModelStoreOwner {
            override val viewModelStore = holder.storeFor(key)
        }
    }
    val isChangingConfigurations = rememberIsChangingConfigurations()
    DisposableEffect(owner) { onDispose { if (!isChangingConfigurations()) holder.clear(key) } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
