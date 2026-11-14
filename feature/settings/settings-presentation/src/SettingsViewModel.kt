package dev.spaceapps.secvault.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.settings.domain.AppSettings
import dev.spaceapps.secvault.feature.settings.domain.DeleteAccountUseCase
import dev.spaceapps.secvault.feature.settings.domain.GetAccountIdForSettingsUseCase
import dev.spaceapps.secvault.feature.settings.domain.LockNowUseCase
import dev.spaceapps.secvault.feature.settings.domain.ObserveSettingsUseCase
import dev.spaceapps.secvault.feature.settings.domain.UpdateSettingsUseCase
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Dialog
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Effect
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.Intent
import dev.spaceapps.secvault.feature.settings.presentation.SettingsContract.State
import kotlinx.coroutines.launch

class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    getAccountId: GetAccountIdForSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val lockNow: LockNowUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    init {
        viewModelScope.launch { observeSettings().collect { setState { copy(settings = it) } } }
        viewModelScope.launch {
            val id = getAccountId().orEmpty()
            setState { copy(accountId = id) }
        }
    }

    override fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.CopyId -> currentState.accountId.takeIf { it.isNotEmpty() }?.let { sendEffect(Effect.CopyId(it)) }

            is Intent.ShowDialog -> setState { copy(dialog = intent.dialog, deleteConfirmation = "") }

            is Intent.ThemeChosen -> update { it.copy(theme = intent.theme) }

            is Intent.DynamicColorChanged -> update { it.copy(dynamicColor = intent.enabled) }

            is Intent.AutoLockChosen -> update { it.copy(autoLock = intent.autoLock) }

            is Intent.ClipboardClearChosen -> update { it.copy(clipboardClear = intent.clear) }

            is Intent.BlockScreenshotsChanged -> update { it.copy(blockScreenshots = intent.enabled) }

            is Intent.DeleteConfirmationChanged -> setState { copy(deleteConfirmation = intent.text) }

            Intent.ConfirmDeleteAccount -> if (currentState.canDelete) {
                setState { copy(isDeleting = true) }
                viewModelScope.launch { deleteAccount() } // lock state becomes NoVault; the app shows Welcome
            }

            Intent.LockNow -> lockNow()
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        setState { copy(dialog = Dialog.None) }
        viewModelScope.launch { updateSettings(transform) }
    }
}
