package dev.spaceapps.secvault.feature.vault.domain.model

/** What can be shown about a backup file before the recovery phrase is entered. */
data class BackupInfo(
    val accountId: String,
    val createdAt: Long,
    val itemCount: Int,
)

/** A backup opened with its recovery phrase. Holds secrets: keep it short-lived. */
data class DecryptedBackup(
    val accountId: String,
    /** The account's recovery data, needed to restore the account on a new device. */
    val recoveryBlob: ByteArray,
    val items: List<VaultItem>,
) {
    override fun toString() = "DecryptedBackup(accountId=$accountId, items=${items.size})"
}

/** An exported backup, ready to be written to a file. */
data class BackupFile(
    val suggestedName: String,
    val content: String,
    val itemCount: Int,
) {
    override fun toString() = "BackupFile($suggestedName, items=$itemCount)"
}

data class ImportResult(
    val imported: Int,
    val skipped: Int,
)
