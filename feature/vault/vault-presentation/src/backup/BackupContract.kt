package dev.spaceapps.secvault.feature.vault.presentation.backup

import dev.spaceapps.secvault.core.domain.model.BackupFormat
import dev.spaceapps.secvault.core.domain.model.BackupInfo
import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState
import dev.spaceapps.secvault.feature.vault.domain.model.BackupFile
import dev.spaceapps.secvault.feature.vault.domain.model.ImportResult

object BackupContract {
    /** What went wrong; the UI turns it into a string resource. */
    sealed interface Error {
        data class ExportFailed(
            val reason: String?,
        ) : Error

        data object FileNotSaved : Error

        /** Not a SecureVault backup, damaged, or too big. */
        data object BackupUnreadable : Error

        data object InvalidPhrase : Error

        /** Valid words, but not the backup's account. */
        data object PhraseDoesNotMatch : Error

        data class ImportFailed(
            val reason: String?,
        ) : Error
    }

    data class State(
        val exporting: Boolean = false,
        /** Picked backup waiting for the recovery key; null = no import in progress. */
        val importContent: String? = null,
        val importInfo: BackupInfo? = null,
        /** The backup belongs to another account than this vault. */
        val otherAccount: Boolean = false,
        val phrase: String = "",
        val importing: Boolean = false,
        val error: Error? = null,
    ) : UiState {
        val phraseWords: List<String> get() = BackupFormat.parsePhrase(phrase)

        val canImport: Boolean
            get() = importContent != null && !importing && phraseWords.size == BackupFormat.PHRASE_WORDS

        override fun toString() = "BackupState(exporting=$exporting, importing=$importing)" // no phrase in logs
    }

    sealed interface Intent : UiIntent {
        data object Export : Intent

        /** The file picker finished writing (or was cancelled). */
        data class ExportWritten(
            val saved: Boolean,
        ) : Intent

        class ImportPicked(
            val content: String,
        ) : Intent {
            override fun toString() = "ImportPicked"
        }

        class PhraseChanged(
            val text: String,
        ) : Intent {
            override fun toString() = "PhraseChanged" // no recovery words in logs
        }

        data object SubmitImport : Intent

        data object CancelImport : Intent

        data object Back : Intent
    }

    sealed interface Effect : UiEffect {
        /** Ask the UI to let the user choose where to save [file]. */
        data class SaveFile(
            val file: BackupFile,
        ) : Effect

        data class Exported(
            val itemCount: Int,
        ) : Effect

        data class Imported(
            val result: ImportResult,
        ) : Effect

        data object Close : Effect
    }
}
