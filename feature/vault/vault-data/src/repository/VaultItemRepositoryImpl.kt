package dev.spaceapps.secvault.feature.vault.data.repository

import dev.spaceapps.secvault.core.data.cipher.EncryptedRecord
import dev.spaceapps.secvault.core.data.cipher.VaultCipher
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.database.VaultItemEntity
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.data.dto.VaultItemPayloadDto
import dev.spaceapps.secvault.feature.vault.data.dto.toDomain
import dev.spaceapps.secvault.feature.vault.data.dto.toPayload
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItemSummary
import dev.spaceapps.secvault.feature.vault.domain.model.toSummary
import dev.spaceapps.secvault.feature.vault.domain.repository.VaultItemRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class VaultItemRepositoryImpl(
    private val dao: VaultItemDao,
    private val cipher: VaultCipher,
    private val keys: VaultKeysRepository,
) : VaultItemRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSummaries(): Flow<List<VaultItemSummary>> =
        keys.lockState.flatMapLatest { state ->
            if (state != LockState.Unlocked) flowOf(emptyList())
            else dao.observeActive().map { rows -> rows.mapNotNull { decryptOrNull(it)?.toDomain(it.id)?.toSummary() } }
        }

    override suspend fun get(id: String): VaultItem? {
        val row = dao.get(id)?.takeUnless { it.deleted } ?: return null
        return decrypt(row).toDomain(id)
    }

    override suspend fun getAll(): List<VaultItem> =
        dao.observeActive().first().map { decrypt(it).toDomain(it.id) }.filter { !it.isTrashed }

    override suspend fun save(item: VaultItem) {
        val existing = dao.get(item.id)
        val previous = existing?.takeUnless { it.deleted }?.let { decryptOrNull(it) }
        val plain = json.encodeToString(VaultItemPayloadDto.serializer(), item.toPayload(previous)).encodeToByteArray()
        try {
            val record = cipher.encrypt(item.id, plain)
            dao.upsert(
                VaultItemEntity(
                    id = item.id,
                    version = (existing?.version ?: 0) + 1,
                    wrappedItemKey = record.wrappedItemKey,
                    ciphertext = record.ciphertext,
                    updatedAt = item.updatedAt,
                    deleted = false,
                    dirty = true,
                ),
            )
        } finally {
            plain.fill(0)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun delete(id: String) {
        val existing = dao.get(id) ?: return
        dao.upsert(
            VaultItemEntity(
                id = id,
                version = existing.version + 1,
                wrappedItemKey = ByteArray(0),
                ciphertext = ByteArray(0),
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                deleted = true,
                dirty = true,
            ),
        )
    }

    private suspend fun decrypt(row: VaultItemEntity): VaultItemPayloadDto {
        val plain = cipher.decrypt(row.id, EncryptedRecord(row.wrappedItemKey, row.ciphertext))
        try {
            return json.decodeFromString(VaultItemPayloadDto.serializer(), plain.decodeToString())
        } finally {
            plain.fill(0)
        }
    }

    /** A row that fails to decrypt (corrupted, or vault locked meanwhile) is skipped, not fatal. */
    private suspend fun decryptOrNull(row: VaultItemEntity): VaultItemPayloadDto? =
        try {
            decrypt(row)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
