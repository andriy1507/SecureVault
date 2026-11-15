package dev.spaceapps.secvault.feature.vault.data.repository

import dev.spaceapps.secvault.core.data.cipher.BackupCipher
import dev.spaceapps.secvault.core.data.cipher.EncryptedRecord
import dev.spaceapps.secvault.core.data.cipher.SealedBackup
import dev.spaceapps.secvault.core.data.cipher.VaultCipher
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.database.VaultItemEntity
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.model.VaultCreation
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.domain.model.ImportResult
import dev.spaceapps.secvault.feature.vault.domain.model.ItemType
import dev.spaceapps.secvault.feature.vault.domain.model.Passkey
import dev.spaceapps.secvault.feature.vault.domain.model.PasswordHistoryEntry
import dev.spaceapps.secvault.feature.vault.domain.model.VaultItem
import dev.spaceapps.secvault.feature.vault.domain.usecase.BackupFormat
import dev.spaceapps.secvault.feature.vault.domain.usecase.ExportVaultUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ImportBackupUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ReadBackupInfoUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.RestoreFromBackupUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest

class VaultBackupRepositoryTest {
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

    private class FakeCipher : VaultCipher {
        override suspend fun encrypt(
            itemId: String,
            plaintext: ByteArray,
        ) = EncryptedRecord(ByteArray(0), plaintext.copyOf())

        override suspend fun decrypt(
            itemId: String,
            record: EncryptedRecord,
        ) = record.ciphertext.copyOf()
    }

    /** Reversible stand-in for the real backup crypto (covered in core-data tests). */
    private class FakeBackupCipher(
        private val accountId: String,
        private val words: List<String>,
    ) : BackupCipher {
        override suspend fun seal(plaintext: ByteArray) =
            SealedBackup(accountId, "blob-$accountId".encodeToByteArray(), plaintext.xor())

        override suspend fun open(
            backup: SealedBackup,
            words: List<String>,
        ): ByteArray {
            if (words.size != BackupFormat.PHRASE_WORDS) throw VaultError.InvalidRecoveryPhrase()
            if (words != this.words) throw VaultError.RecoveryPhraseMismatch()
            return backup.payload.xor()
        }

        private fun ByteArray.xor() = map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
    }

    private class FakeKeys(
        private val id: String?,
    ) : VaultKeysRepository {
        override val lockState = MutableStateFlow(LockState.Unlocked)
        var restoredAccount: String? = null

        override suspend fun refresh() = Unit

        override fun isDeviceSecure() = true

        override suspend fun createVault(): VaultCreation = error("unused")

        override suspend fun completeOnboarding() = Unit

        override suspend fun restoreVault(
            words: List<String>,
            accountId: String,
            recoveryBlob: ByteArray,
            populate: suspend () -> Unit,
        ) {
            restoredAccount = accountId
            populate()
        }

        override suspend fun unlock() = Unit

        override suspend fun accountId(): String? = restoredAccount ?: id

        override suspend fun deleteVault() = Unit

        override fun lock() = Unit
    }

    private val words = List(BackupFormat.PHRASE_WORDS) { "word$it" }
    private val accountId = "7F3A-91C2-E04B-D58E"

    /** One vault: its items, keys and backup plumbing. */
    private inner class Vault(
        id: String? = accountId,
    ) {
        val keys = FakeKeys(id)
        val items = VaultItemRepositoryImpl(FakeDao(), FakeCipher(), keys)
        val backups = VaultBackupRepositoryImpl(FakeBackupCipher(accountId, words))
        val export = ExportVaultUseCase(items, backups, keys)
        val import = ImportBackupUseCase(items, backups)
        val restore = RestoreFromBackupUseCase(items, backups, keys)
        val info = ReadBackupInfoUseCase(backups)
    }

    private fun login(
        id: String,
        title: String,
        username: String = "me",
        url: String = "example.com",
    ) = VaultItem(id, ItemType.Login, title, username, "pw-$id", url, "", false, createdAt = 1, updatedAt = 2)

    private val fullLogin = login("l1", "Mail").copy(
        notes = "note",
        favorite = true,
        uris = listOf("https://mail.example.com", "androidapp://com.example"),
        totpSecret = "JBSWY3DPEHPK3PXP",
        passwordHistory = listOf(PasswordHistoryEntry("old", 5)),
        passkeys = listOf(Passkey("cred", "example.com", "Example", "uh", "me", "Me", "pk", 3)),
    )
    private val card = VaultItem(
        id = "c1",
        type = ItemType.Card,
        title = "Visa",
        createdAt = 1,
        updatedAt = 2,
        cardholderName = "ME",
        cardNumber = "4111111111111111",
        cardExpiry = "12/30",
        cardCvv = "123",
    )
    private val note = VaultItem("n1", ItemType.Note, "Wifi", notes = "secret", createdAt = 1, updatedAt = 2)

    @Test
    fun exportThenRestoreOnNewPhoneKeepsEverything() =
        runTest {
            val phone = Vault()
            listOf(fullLogin, card, note).forEach { phone.items.save(it) }
            phone.items.save(login("t1", "Trashed").copy(trashedAt = 9))

            val file = phone.export()
            assertEquals(BackupFormat.fileName(accountId), file.suggestedName)
            assertEquals(3, file.itemCount)
            assertFalse("pw-l1" in file.content, "secrets must not be readable in the file")
            assertEquals(3, phone.info(file.content).itemCount)
            assertEquals(accountId, phone.info(file.content).accountId)

            val newPhone = Vault(id = null)
            assertEquals(ImportResult(3, 0), newPhone.restore(file.content, words))
            assertEquals(accountId, newPhone.keys.restoredAccount)
            val restored = newPhone.items.getAll().associateBy { it.id }
            assertEquals(setOf("l1", "c1", "n1"), restored.keys)
            assertEquals(fullLogin, restored.getValue("l1"))
            assertEquals(card, restored.getValue("c1"))
            assertEquals(note, restored.getValue("n1"))
        }

    @Test
    fun importSkipsDuplicatesAndKeepsTakenIdsApart() =
        runTest {
            val source = Vault()
            source.items.save(login("a", "Mail"))
            source.items.save(login("b", "Bank"))
            source.items.save(login("c", "Shop"))
            val file = source.export().content

            val target = Vault()
            target.items.save(login("x", " mail ", username = "ME", url = "Example.com ")) // same as "a"
            target.items.save(login("b", "Something else").copy(trashedAt = 1)) // id "b" taken by a trashed item

            assertEquals(ImportResult(imported = 2, skipped = 1), target.import(file, words))
            val titles = target.items
                .getAll()
                .map { it.title }
                .sorted()
            assertEquals(listOf(" mail ", "Bank", "Shop"), titles)
            val bank = target.items.getAll().single { it.title == "Bank" }
            assertNotEquals("b", bank.id)
            assertTrue(target.items.get("b")?.isTrashed == true)

            // Importing again adds nothing.
            assertEquals(ImportResult(imported = 0, skipped = 3), target.import(file, words))
        }

    @Test
    fun wrongPhraseImportsNothing() =
        runTest {
            val file = Vault().apply { items.save(login("a", "Mail")) }.export().content
            val target = Vault()
            val other = List(BackupFormat.PHRASE_WORDS) { "other$it" }
            assertFailsWith<VaultError.RecoveryPhraseMismatch> { target.import(file, other) }
            assertFailsWith<VaultError.InvalidRecoveryPhrase> { target.import(file, words.drop(1)) }
            assertTrue(target.items.getAll().isEmpty())
        }

    @Test
    fun notABackupIsRejected() =
        runTest {
            val vault = Vault()
            assertFailsWith<VaultError.UnsupportedBackup> { vault.info("hello") }
            assertFailsWith<VaultError.UnsupportedBackup> { vault.info("""{"format":"other","v":1}""") }
            val file = vault.export().content
            assertFailsWith<VaultError.UnsupportedBackup> { vault.info(file.replace("\"v\":1", "\"v\":2")) }
        }

    @Test
    fun phraseParsingAcceptsPastedTextAndRecoveryFile() {
        assertEquals(listOf("abandon", "ability", "able"), BackupFormat.parsePhrase("  Abandon\nability,  able "))
        val file = "Vault recovery key\n\nAccount ID: 7F3A-91C2\n\n" +
            "1. abandon\n2. ability\n3. able\n\nKeep this file offline."
        assertEquals(listOf("abandon", "ability", "able"), BackupFormat.parsePhrase(file))
        assertEquals(emptyList(), BackupFormat.parsePhrase("   "))
    }
}
