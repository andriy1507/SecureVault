package dev.spaceapps.secvault.feature.vault.domain.repository

import dev.spaceapps.secvault.feature.vault.domain.model.BackupInfo
import dev.spaceapps.secvault.feature.vault.domain.model.DecryptedBackup
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem

/** Encrypted backup files. Only the account's recovery phrase can open them. */
interface VaultBackupRepository {
    /** File content holding [items] (needs the vault unlocked). */
    suspend fun export(
        items: List<VaultItem>,
        createdAt: Long,
    ): String

    /** Public header of a backup. Throws VaultError.UnsupportedBackup when it isn't one. */
    fun readInfo(content: String): BackupInfo

    /**
     * Throws VaultError.UnsupportedBackup, VaultError.InvalidRecoveryPhrase or VaultError.RecoveryPhraseMismatch.
     */
    suspend fun decrypt(
        content: String,
        words: List<String>,
    ): DecryptedBackup
}
