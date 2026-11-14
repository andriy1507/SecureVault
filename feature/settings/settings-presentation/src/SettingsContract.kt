package dev.spaceapps.secvault.feature.settings.presentation

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.settings.domain.AppSettings
import dev.spaceapps.secvault.feature.settings.domain.AppTheme
import dev.spaceapps.secvault.feature.settings.domain.AutoLock
import dev.spaceapps.secvault.feature.settings.domain.ClipboardClear
import dev.spaceapps.secvault.feature.settings.domain.DELETE_CONFIRMATION

object SettingsContract {
    enum class Dialog { None, Theme, AutoLock, ClipboardClear, DeleteAccount }

    data class State(
        val settings: AppSettings = AppSettings(),
        val accountId: String = "",
        val dialog: Dialog = Dialog.None,
        val deleteConfirmation: String = "",
        val isDeleting: Boolean = false,
    ) : UiState {
        val canDelete: Boolean get() = deleteConfirmation.trim() == DELETE_CONFIRMATION && !isDeleting
    }

    sealed interface Intent : UiIntent {
        data object CopyId : Intent

        data class ShowDialog(
            val dialog: Dialog,
        ) : Intent

        data class ThemeChosen(
            val theme: AppTheme,
        ) : Intent

        data class DynamicColorChanged(
            val enabled: Boolean,
        ) : Intent

        data class AutoLockChosen(
            val autoLock: AutoLock,
        ) : Intent

        data class ClipboardClearChosen(
            val clear: ClipboardClear,
        ) : Intent

        data class BlockScreenshotsChanged(
            val enabled: Boolean,
        ) : Intent

        data class DeleteConfirmationChanged(
            val text: String,
        ) : Intent

        data object ConfirmDeleteAccount : Intent

        data object LockNow : Intent
    }

    sealed interface Effect : UiEffect {
        data class CopyId(
            val id: String,
        ) : Effect
    }
}
