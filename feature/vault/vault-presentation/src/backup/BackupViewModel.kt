package dev.spaceapps.secvault.feature.vault.presentation.backup

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.usecase.ExportVaultUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.GetVaultAccountIdUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ImportBackupUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ReadBackupInfoUseCase
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Error
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.backup.BackupContract.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Settings → Backup: export the vault to an encrypted file, or import one with its recovery key. */
class BackupViewModel(
    private val exportVault: ExportVaultUseCase,
    private val readBackupInfo: ReadBackupInfoUseCase,
    private val importBackup: ImportBackupUseCase,
    private val getAccountId: GetVaultAccountIdUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    /** Items in the file being saved, for the "exported" message. */
    private var pendingExportCount = 0

    override fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.Export -> export()
            is Intent.ExportWritten -> exportWritten(intent.saved)
            is Intent.ImportPicked -> importPicked(intent.content)
            is Intent.PhraseChanged -> setState { copy(phrase = intent.text, error = null) }
            Intent.SubmitImport -> submitImport()
            Intent.CancelImport -> setState { State() }
            Intent.Back -> sendEffect(Effect.Close)
        }
    }

    private fun export() {
        if (currentState.exporting) return
        setState { copy(exporting = true, error = null) }
        viewModelScope.launch {
            try {
                val file = exportVault()
                pendingExportCount = file.itemCount
                sendEffect(Effect.SaveFile(file))
            } catch (e: CancellationException) {
                throw e
            } catch (expected: Exception) {
                setState { copy(exporting = false, error = Error.ExportFailed(expected.message)) }
            }
        }
    }

    private fun exportWritten(saved: Boolean) {
        setState { copy(exporting = false, error = if (saved) null else Error.FileNotSaved) }
        if (saved) sendEffect(Effect.Exported(pendingExportCount))
    }

    private fun importPicked(content: String) {
        val info = try {
            readBackupInfo(content)
        } catch (expected: VaultError.UnsupportedBackup) {
            setState { State(error = Error.BackupUnreadable) }
            return
        }
        setState { State(importContent = content, importInfo = info) }
        viewModelScope.launch {
            val own = getAccountId()
            setState { if (importInfo == info) copy(otherAccount = own != null && own != info.accountId) else this }
        }
    }

    private fun submitImport() {
        val state = currentState
        val content = state.importContent ?: return
        if (!state.canImport) return
        setState { copy(importing = true, error = null) }
        viewModelScope.launch {
            try {
                val result = importBackup(content, state.phraseWords)
                setState { State() } // drop the phrase and the file from memory
                sendEffect(Effect.Imported(result))
            } catch (e: CancellationException) {
                throw e
            } catch (expected: Exception) {
                setState { copy(importing = false, error = importError(expected)) }
            }
        }
    }

    private fun importError(e: Exception): Error =
        when (e) {
            is VaultError.InvalidRecoveryPhrase -> Error.InvalidPhrase
            is VaultError.RecoveryPhraseMismatch -> Error.PhraseDoesNotMatch
            is VaultError.UnsupportedBackup -> Error.BackupUnreadable
            else -> Error.ImportFailed(e.message)
        }

    override fun onCleared() {
        setState { State() }
        super.onCleared()
    }
}
