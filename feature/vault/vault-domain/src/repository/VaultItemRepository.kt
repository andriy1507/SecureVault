package dev.spaceapps.secvault.feature.vault.domain.repository

import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItemSummary
import kotlinx.coroutines.flow.Flow

interface VaultItemRepository {
    /** All items, trashed ones included. Emits an empty list while the vault is locked. */
    fun observeSummaries(): Flow<List<VaultItemSummary>>

    /** The item, trashed or not, or null if it doesn't exist. */
    suspend fun get(id: String): VaultItem?

    /** All items not in the trash, decrypted (autofill matching). Throws VaultError.Locked when locked. */
    suspend fun getAll(): List<VaultItem>

    /** Inserts or updates. */
    suspend fun save(item: VaultItem)

    /** Leaves a tombstone (needed later for sync), erases the content. */
    suspend fun delete(id: String)
}
