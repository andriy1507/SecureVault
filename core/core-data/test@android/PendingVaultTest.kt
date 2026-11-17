package dev.spaceapps.secvault.core.data.cipher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.database.VaultItemEntity
import dev.spaceapps.secvault.core.data.repository.VaultKeysRepositoryImpl
import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.data.storage.VaultKeyStorage
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/** Onboarding that is interrupted on the recovery-key step can be resumed, for a limited time. */
class PendingVaultTest {
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

    /** Keeps preferences in memory: no files, so no file locking differences between systems. */
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    private class FakeClock(
        var millis: Long = START,
    ) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(millis)
    }

    private val words by lazy { loadWordlist() }
    private val wordlist = WordlistSource { words }
    private val clock = FakeClock()
    private val storage = VaultKeyStorage(InMemoryDataStore())

    /** A new repository on the same storage, like the app starting again after the process was killed. */
    private fun app() = VaultKeysRepositoryImpl(FakeGuard(), storage, wordlist, VaultSession(), NoRowsDao(), clock)

    @Test
    fun resumeAfterRestartReturnsTheSameWords() =
        runTest {
            val created = app().createVault()

            val restarted = app()
            restarted.refresh()
            assertEquals(LockState.NoVault, restarted.lockState.value)
            assertTrue(restarted.hasPendingVault())

            val resumed = restarted.resumePendingVault()
            assertEquals(created.accountId, resumed.accountId)
            assertEquals(created.recoveryWords, resumed.recoveryWords)

            restarted.completeOnboarding()
            assertEquals(LockState.Unlocked, restarted.lockState.value)
        }

    @Test
    fun pendingDataIsGoneAfterConfirmation() =
        runTest {
            val repository = app()
            repository.createVault()
            assertNotNull(storage.read().pendingRecovery)

            repository.completeOnboarding()

            val stored = storage.read()
            assertNull(stored.pendingRecovery)
            assertNull(stored.pendingCreatedAt)
            assertFalse(app().hasPendingVault())
            assertFailsWith<VaultError.NoVault> { app().resumePendingVault() }
        }

    @Test
    fun pendingVaultOlderThanADayIsWiped() =
        runTest {
            app().createVault()
            clock.millis += DAY_MS + 1

            val restarted = app()
            assertFalse(restarted.hasPendingVault())
            restarted.refresh()

            assertEquals(LockState.NoVault, restarted.lockState.value)
            assertNull(storage.read().sealedBundle)
        }

    @Test
    fun pendingVaultYoungerThanADayIsKept() =
        runTest {
            app().createVault()
            clock.millis += DAY_MS - 1

            val restarted = app()
            restarted.refresh()

            assertNotNull(storage.read().sealedBundle)
            assertTrue(restarted.hasPendingVault())
        }

    @Test
    fun unconfirmedVaultWithoutPendingDataIsStillWiped() =
        runTest {
            // What an interrupted restore leaves behind: sealed keys, never confirmed, no pending phrase.
            storage.saveNewVault("7F3A-91C2-E04B-D58E", byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))

            val restarted = app()
            assertFalse(restarted.hasPendingVault())
            restarted.refresh()

            assertEquals(LockState.NoVault, restarted.lockState.value)
            assertNull(storage.read().sealedBundle)
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

    private companion object {
        const val START = 1_800_000_000_000L
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
