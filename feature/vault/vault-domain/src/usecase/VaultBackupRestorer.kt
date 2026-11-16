package dev.spaceapps.secvault.feature.vault.domain.usecase

import dev.spaceapps.secvault.core.domain.model.BackupInfo
import dev.spaceapps.secvault.core.domain.repository.BackupRestorer

/** Lets onboarding restore a backup through the vault's use cases without depending on this feature. */
class VaultBackupRestorer(
    private val readBackupInfo: ReadBackupInfoUseCase,
    private val restoreFromBackup: RestoreFromBackupUseCase,
) : BackupRestorer {
    override fun readInfo(content: String): BackupInfo = readBackupInfo(content)

    override suspend fun restore(
        content: String,
        words: List<String>,
    ) {
        restoreFromBackup(content, words)
    }
}
