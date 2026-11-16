package dev.spaceapps.secvault.core.domain.repository

import dev.spaceapps.secvault.core.domain.model.BackupInfo

/**
 * Restores an account from a backup file on a new device. Implemented by the vault feature so onboarding
 * does not depend on it.
 */
interface BackupRestorer {
    /** Header of a picked file; throws VaultError.UnsupportedBackup when it isn't a backup. */
    fun readInfo(content: String): BackupInfo

    /** Restores the backup's account and items. Nothing on the device changes when the phrase is wrong. */
    suspend fun restore(
        content: String,
        words: List<String>,
    )
}
