package dev.spaceapps.secvault.feature.vault.data.dto

import kotlinx.serialization.Serializable

/**
 * A backup file. Everything except [payload] is readable without the recovery phrase.
 * Field names are part of the file format: don't rename.
 */
@Serializable
internal data class BackupEnvelopeDto(
    val format: String,
    val v: Int,
    val accountId: String,
    val createdAt: Long,
    val itemCount: Int,
    /** Base64: identity + vault key encrypted with the recovery phrase. */
    val recoveryBlob: String,
    /** Base64: [BackupPayloadDto] encrypted with a key derived from the vault key. */
    val payload: String,
) {
    companion object {
        const val FORMAT = "secvault-backup"
        const val VERSION = 1
    }
}

@Serializable
internal data class BackupPayloadDto(
    val items: List<BackupItemDto>,
)

@Serializable
internal data class BackupItemDto(
    val id: String,
    val item: VaultItemPayloadDto,
)
