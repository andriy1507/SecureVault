package dev.spaceapps.secvault.feature.vault.data.repository

import dev.spaceapps.secvault.core.data.cipher.EncryptedRecord
import dev.spaceapps.secvault.core.data.cipher.VaultCipher
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.database.VaultItemEntity
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.model.VaultCreation
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.data.dto.VaultItemPayloadDto
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class VaultItemRepositoryTest {
    private class FakeDao : VaultItemDao {
        val rows = MutableStateFlow<Map<String, VaultItemEntity>>(emptyMap())

        override fun observeActive(): Flow<List<VaultItemEntity>> = rows.map { m -> m.values.filter { !it.deleted } }

        override suspend fun get(id: String) = rows.value[id]

        override suspend fun upsert(entity: VaultItemEntity) {
            rows.value = rows.value + (entity.id to entity)
        }

        override suspend fun deleteAll() {
            rows.value = emptyMap()
        }
    }

    /** Reversible stand-in; real crypto is covered in core-data tests. */
    private class FakeCipher : VaultCipher {
        override suspend fun encrypt(
            itemId: String,
            plaintext: ByteArray,
        ) = EncryptedRecord(itemId.encodeToByteArray(), plaintext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray())

        override suspend fun decrypt(
            itemId: String,
            record: EncryptedRecord,
        ) = record.ciphertext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    private class FakeKeys(
        state: LockState,
    ) : VaultKeysRepository {
        override val lockState = MutableStateFlow(state)

        override suspend fun refresh() = Unit

        override fun isDeviceSecure() = true

        override suspend fun createVault(): VaultCreation = error("unused")

        override suspend fun completeOnboarding() = Unit

        override suspend fun unlock() {
            lockState.value = LockState.Unlocked
        }

        override suspend fun accountId(): String? = null

        override suspend fun deleteVault() = Unit

        override fun lock() {
            lockState.value = LockState.Locked
        }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val dao = FakeDao()
    private val keys = FakeKeys(LockState.Unlocked)
    private val repo = VaultItemRepositoryImpl(dao, FakeCipher(), keys)

    private fun item(
        updatedAt: Long,
        password: String = "pw",
        title: String = "Mail",
    ) = VaultItem("id1", ItemType.Login, title, "me", password, "", "", false, createdAt = 1, updatedAt = updatedAt)

    @Test
    fun saveGetUpdateDelete() =
        runTest {
            repo.save(item(updatedAt = 10))
            assertEquals("pw", repo.get("id1")?.password)
            assertEquals(
                1,
                dao.rows.value
                    .getValue("id1")
                    .version,
            )

            repo.save(item(updatedAt = 20, password = "new"))
            val row = dao.rows.value.getValue("id1")
            assertEquals(2, row.version)
            assertTrue(row.dirty)

            repo.delete("id1")
            val tomb = dao.rows.value.getValue("id1")
            assertTrue(tomb.deleted && tomb.ciphertext.isEmpty() && tomb.version == 3L)
            assertNull(repo.get("id1"))
        }

    @Test
    fun fieldClocksTrackOnlyChangedFields() =
        runTest {
            repo.save(item(updatedAt = 10))
            repo.save(item(updatedAt = 20, password = "new"))
            val row = dao.rows.value.getValue("id1")
            val plain = FakeCipher()
                .decrypt(
                    row.id,
                    EncryptedRecord(row.wrappedItemKey, row.ciphertext),
                ).decodeToString()
            val dto = json.decodeFromString(VaultItemPayloadDto.serializer(), plain)
            assertEquals(20, dto.fieldClocks["password"])
            assertEquals(10, dto.fieldClocks["title"])
        }

    @Test
    fun lockedVaultShowsNothing() =
        runTest {
            repo.save(item(updatedAt = 10))
            assertEquals(1, repo.observeSummaries().first().size)
            keys.lock()
            assertEquals(0, repo.observeSummaries().first().size)
        }
}
