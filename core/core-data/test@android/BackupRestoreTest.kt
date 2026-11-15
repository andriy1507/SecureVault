package dev.spaceapps.secvault.core.data.cipher

import dev.spaceapps.secvault.core.data.crypto.Bip39
import dev.spaceapps.secvault.core.data.crypto.KeyHierarchy
import dev.spaceapps.secvault.core.data.crypto.NewAccount
import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.database.VaultItemEntity
import dev.spaceapps.secvault.core.data.repository.VaultKeysRepositoryImpl
import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.data.storage.VaultKeyStorage
import dev.spaceapps.secvault.core.data.storage.createVaultDataStore
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class BackupRestoreTest {
    /** Stand-in for the Keystore: "seals" by copying. */
    private class FakeGuard : HardwareKeyGuard {
        override fun isDeviceSecure() = true

        override suspend fun seal(plain: ByteArray) = plain.copyOf()

        override suspend fun unseal(sealed: ByteArray) = sealed.copyOf()

        override fun reset() = Unit
    }

    private class NoRowsDao : VaultItemDao {
        override fun observeActive(): Flow<List<VaultItemEntity>> = flowOf(emptyList())

        override suspend fun get(id: String): VaultItemEntity? = null

        override suspend fun upsert(entity: VaultItemEntity) = Unit

        override suspend fun deleteAll() = Unit
    }

    /** One device: its own key storage and in-memory session. */
    private inner class Device {
        val session = VaultSession()
        val storage = VaultKeyStorage(
            createVaultDataStore(File(Files.createTempDirectory("vault").toFile(), "keys.preferences_pb").absolutePath),
        )
        val keys = VaultKeysRepositoryImpl(FakeGuard(), storage, wordlist, session, NoRowsDao())
        val backups = BackupCipherImpl(session, storage, wordlist)

        /** Puts [account] on this device, unlocked, as onboarding would. */
        suspend fun install(account: NewAccount) {
            storage.saveNewVault(account.accountId, account.bundle.serialize(), account.recoveryBlob)
            storage.setRecoveryConfirmed()
            session.open(account.bundle)
        }
    }

    private val words by lazy { loadWordlist() }
    private val wordlist = WordlistSource { words }

    private suspend fun newAccount() = KeyHierarchy.createAccount(Bip39(words))

    @Test
    fun backupOpensOnlyWithTheAccountsRecoveryKey() =
        runTest {
            val account = newAccount()
            val phone = Device().apply { install(account) }
            val sealed = phone.backups.seal("items".encodeToByteArray())
            assertEquals(account.accountId, sealed.accountId)

            // Another phone with no vault at all can open it with the words.
            assertEquals("items", Device().backups.open(sealed, account.recoveryWords).decodeToString())

            val otherWords = newAccount().recoveryWords
            assertFailsWith<VaultError.RecoveryPhraseMismatch> { Device().backups.open(sealed, otherWords) }

            val notWords = List(account.recoveryWords.size) { "abandon" }
            assertFailsWith<VaultError.InvalidRecoveryPhrase> { Device().backups.open(sealed, notWords) }
            assertFailsWith<VaultError.InvalidRecoveryPhrase> {
                Device().backups.open(sealed, account.recoveryWords.drop(1))
            }
        }

    @Test
    fun tamperedOrReboundBackupIsRejected() =
        runTest {
            val account = newAccount()
            val sealed = Device().apply { install(account) }.backups.seal("items".encodeToByteArray())

            val payload = sealed.payload.copyOf().also { it[it.size - 1] = (it.last().toInt() xor 1).toByte() }
            assertFailsWith<VaultError.UnsupportedBackup> {
                Device().backups.open(
                    SealedBackup(sealed.accountId, sealed.recoveryBlob, payload),
                    account.recoveryWords,
                )
            }
            // The recovery data is bound to its account ID.
            assertFailsWith<VaultError.RecoveryPhraseMismatch> {
                Device().backups.open(
                    SealedBackup("AAAA-AAAA-AAAA-AAAA", sealed.recoveryBlob, sealed.payload),
                    account.recoveryWords,
                )
            }
        }

    @Test
    fun sealNeedsUnlockedVault() =
        runTest {
            val phone = Device().apply { install(newAccount()) }
            phone.session.close()
            assertFailsWith<VaultError.Locked> { phone.backups.seal(byteArrayOf(1)) }
        }

    @Test
    fun restoreKeepsAccountIdAndVaultKey() =
        runTest {
            val account = newAccount()
            val sealed = Device().apply { install(account) }.backups.seal("items".encodeToByteArray())

            val newPhone = Device()
            var populatedWhileLocked = false
            newPhone.keys.restoreVault(account.recoveryWords, sealed.accountId, sealed.recoveryBlob) {
                populatedWhileLocked = newPhone.keys.lockState.value != LockState.Unlocked
                // Keys are loaded: the same vault key reopens the backup through the session.
                assertContentEquals(account.bundle.vaultKey, newPhone.session.vaultKeyCopy())
            }
            assertTrue(populatedWhileLocked)
            assertEquals(LockState.Unlocked, newPhone.keys.lockState.value)
            assertEquals(account.accountId, newPhone.keys.accountId())
            val stored = newPhone.storage.read()
            assertTrue(stored.recoveryConfirmed)
            assertContentEquals(account.recoveryBlob, stored.recoveryBlob)
            // New backups from the restored phone open with the same words.
            val again = newPhone.backups.seal("more".encodeToByteArray())
            assertEquals("more", Device().backups.open(again, account.recoveryWords).decodeToString())
        }

    @Test
    fun wrongPhraseLeavesExistingVaultUntouched() =
        runTest {
            val existing = newAccount()
            val phone = Device().apply { install(existing) }
            val other = newAccount()
            val sealed = Device().apply { install(other) }.backups.seal(byteArrayOf(1))

            assertFailsWith<VaultError.RecoveryPhraseMismatch> {
                phone.keys.restoreVault(existing.recoveryWords, sealed.accountId, sealed.recoveryBlob) {}
            }
            assertEquals(existing.accountId, phone.storage.read().accountId)
            assertTrue(phone.session.isOpen)
        }

    @Test
    fun failedPopulateWipesRestoredVault() =
        runTest {
            val account = newAccount()
            val sealed = Device().apply { install(account) }.backups.seal(byteArrayOf(1))
            val newPhone = Device()
            assertFailsWith<IllegalStateException> {
                newPhone.keys.restoreVault(account.recoveryWords, sealed.accountId, sealed.recoveryBlob) {
                    error("import failed")
                }
            }
            assertNull(newPhone.storage.read().accountId)
            assertTrue(!newPhone.session.isOpen)
        }

    private fun loadWordlist(): List<String> {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            val f = File(dir, "shared/composeResources/files/bip39_english.txt")
            if (f.exists()) return f.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            dir = dir.parentFile
        }
        error("Wordlist not found")
    }
}
