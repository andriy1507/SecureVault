package dev.spaceapps.secvault.feature.vault.domain.usecase

import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.BackupFormat
import dev.spaceapps.secvault.core.domain.model.BackupInfo
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.repository.VaultItemRepository
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.domain.model.BackupFile
import dev.spaceapps.secvault.feature.vault.domain.model.ImportResult
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultBackupRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Writes all items not in the trash into an encrypted backup. */
class ExportVaultUseCase(
    private val items: VaultItemRepository,
    private val backups: VaultBackupRepository,
    private val keys: VaultKeysRepository,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(): BackupFile {
        val accountId = keys.accountId() ?: throw VaultError.NoVault()
        val all = items.getAll()
        val content = backups.export(all, Clock.System.now().toEpochMilliseconds())
        return BackupFile(BackupFormat.fileName(accountId), content, all.size)
    }
}

/** Header of a picked file; throws VaultError.UnsupportedBackup when it isn't a backup. */
class ReadBackupInfoUseCase(
    private val backups: VaultBackupRepository,
) {
    operator fun invoke(content: String): BackupInfo = backups.readInfo(content)
}

/** Account ID of the vault on this device, null when there is none. */
class GetVaultAccountIdUseCase(
    private val keys: VaultKeysRepository,
) {
    suspend operator fun invoke(): String? = keys.accountId()
}

/**
 * Adds a backup's items to the unlocked vault. An item equal to an existing one (same title, username and
 * website) is skipped.
 */
class ImportBackupUseCase(
    private val items: VaultItemRepository,
    private val backups: VaultBackupRepository,
) {
    suspend operator fun invoke(
        content: String,
        words: List<String>,
    ): ImportResult {
        val backup = backups.decrypt(content, words)
        return importItems(items, backup.items)
    }
}

/**
 * Onboarding on a new device: restores the backup's account (same account ID) and its items.
 * Nothing on the device changes when the phrase is wrong.
 */
class RestoreFromBackupUseCase(
    private val items: VaultItemRepository,
    private val backups: VaultBackupRepository,
    private val keys: VaultKeysRepository,
) {
    suspend operator fun invoke(
        content: String,
        words: List<String>,
    ): ImportResult {
        val backup = backups.decrypt(content, words)
        var result = ImportResult(0, 0)
        keys.restoreVault(words, backup.accountId, backup.recoveryBlob) {
            result = importItems(items, backup.items)
        }
        return result
    }
}

/** Same title, username and website (case and surrounding spaces ignored) = same item. */
internal fun VaultItem.duplicateKey(): String =
    listOf(title, username, url).joinToString(DUPLICATE_KEY_SEPARATOR) { it.trim().lowercase() }

private const val DUPLICATE_KEY_SEPARATOR = "\u0000"

@OptIn(ExperimentalUuidApi::class)
internal suspend fun importItems(
    repository: VaultItemRepository,
    incoming: List<VaultItem>,
): ImportResult {
    val seen = repository.getAll().mapTo(HashSet()) { it.duplicateKey() }
    var imported = 0
    var skipped = 0
    for (item in incoming) {
        if (!seen.add(item.duplicateKey())) {
            skipped++
            continue
        }
        // Keep the ID (same item across devices) unless it's taken here, e.g. by an item in the trash.
        val id = if (repository.get(item.id) == null) item.id else Uuid.random().toString()
        repository.save(item.copy(id = id, trashedAt = null))
        imported++
    }
    return ImportResult(imported, skipped)
}
