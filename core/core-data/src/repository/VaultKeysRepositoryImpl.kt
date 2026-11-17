package dev.spaceapps.secvault.core.data.repository

import dev.spaceapps.secvault.core.data.crypto.Bip39
import dev.spaceapps.secvault.core.data.crypto.KeyBundle
import dev.spaceapps.secvault.core.data.crypto.KeyHierarchy
import dev.spaceapps.secvault.core.data.crypto.WordlistSource
import dev.spaceapps.secvault.core.data.database.VaultItemDao
import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.data.session.VaultSession
import dev.spaceapps.secvault.core.data.storage.VaultKeyStorage
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.model.VaultCreation
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

internal class VaultKeysRepositoryImpl(
    private val guard: HardwareKeyGuard,
    private val storage: VaultKeyStorage,
    private val wordlist: WordlistSource,
    private val session: VaultSession,
    private val itemDao: VaultItemDao,
    private val clock: Clock = Clock.System,
) : VaultKeysRepository {
    private val _lockState = MutableStateFlow(LockState.Unknown)
    override val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val mutex = Mutex()

    /** Keys of a vault created in this process whose phrase is not confirmed yet. */
    private var pending: KeyBundle? = null

    override fun isDeviceSecure(): Boolean = guard.isDeviceSecure()

    override suspend fun refresh() =
        mutex.withLock {
            val stored = storage.read()
            _lockState.value = when {
                pending != null -> {
                    LockState.NoVault
                }

                // onboarding in progress
                stored.sealedBundle == null -> {
                    LockState.NoVault
                }

                !stored.recoveryConfirmed -> {
                    // Onboarding was interrupted. Keep the vault only while its phrase can be shown again.
                    if (!isResumable(stored)) wipeVault()
                    LockState.NoVault
                }

                session.isOpen -> {
                    LockState.Unlocked
                }

                else -> {
                    LockState.Locked
                }
            }
        }

    override suspend fun createVault(): VaultCreation =
        mutex.withLock {
            if (!guard.isDeviceSecure()) throw VaultError.DeviceNotSecure()

            pending?.wipe()
            pending = null

            val account = KeyHierarchy.createAccount(Bip39(wordlist.load()))
            wipeVault() // fresh hardware key and empty item table for a fresh vault
            val sealed = try {
                guard.seal(account.bundle.serialize())
            } catch (e: Throwable) {
                account.bundle.wipe()
                throw e
            }
            storage.saveNewVault(
                account.accountId,
                sealed,
                account.recoveryBlob,
                account.pendingRecovery,
                clock.now().toEpochMilliseconds(),
            )
            pending = account.bundle
            VaultCreation(account.accountId, account.recoveryWords)
        }

    override suspend fun hasPendingVault(): Boolean =
        mutex.withLock {
            val stored = storage.read()
            stored.sealedBundle != null && !stored.recoveryConfirmed && isResumable(stored)
        }

    override suspend fun resumePendingVault(): VaultCreation =
        mutex.withLock {
            val stored = storage.read()
            val sealed = stored.sealedBundle
            val accountId = stored.accountId
            val blob = stored.pendingRecovery
            val resumable = !stored.recoveryConfirmed && isResumable(stored)
            if (sealed == null || accountId == null || blob == null || !resumable) throw VaultError.NoVault()
            val plain = guard.unseal(sealed)
            val bundle = try {
                KeyBundle.deserialize(plain)
            } finally {
                plain.fill(0)
            }
            val words = try {
                KeyHierarchy.openPendingRecovery(blob, bundle.vaultKey, accountId, Bip39(wordlist.load()))
            } catch (e: Throwable) {
                bundle.wipe()
                throw e
            }
            pending?.wipe()
            pending = bundle
            _lockState.value = LockState.NoVault
            VaultCreation(accountId, words)
        }

    override suspend fun completeOnboarding() =
        mutex.withLock {
            val bundle = pending ?: throw VaultError.NoVault()
            storage.setRecoveryConfirmed()
            pending = null
            session.open(bundle)
            _lockState.value = LockState.Unlocked
        }

    override suspend fun restoreVault(
        words: List<String>,
        accountId: String,
        recoveryBlob: ByteArray,
        populate: suspend () -> Unit,
    ) = mutex.withLock {
        if (!guard.isDeviceSecure()) throw VaultError.DeviceNotSecure()
        // Checks the phrase before anything on this device is touched.
        val bundle = KeyHierarchy.restoreFromRecovery(words, recoveryBlob, accountId, Bip39(wordlist.load()))

        pending?.wipe()
        pending = null
        wipeVault()
        try {
            val sealed = guard.seal(bundle.serialize())
            // Stays unconfirmed until populated: an interrupted restore is wiped by refresh() on next start.
            storage.saveNewVault(accountId, sealed, recoveryBlob)
            session.open(bundle)
            populate()
        } catch (e: Throwable) {
            bundle.wipe()
            wipeVault()
            throw e
        }
        storage.setRecoveryConfirmed()
        _lockState.value = LockState.Unlocked
    }

    override suspend fun unlock() =
        mutex.withLock {
            val sealed = storage.read().sealedBundle ?: throw VaultError.NoVault()
            val plain = guard.unseal(sealed)
            try {
                session.open(KeyBundle.deserialize(plain))
            } finally {
                plain.fill(0)
            }
            _lockState.value = LockState.Unlocked
        }

    override suspend fun accountId(): String? = storage.read().accountId

    override suspend fun deleteVault() =
        mutex.withLock {
            pending?.wipe()
            pending = null
            wipeVault()
            _lockState.value = LockState.NoVault
        }

    override fun lock() {
        if (_lockState.value != LockState.Unlocked) return
        session.close()
        _lockState.value = LockState.Locked
    }

    private fun isResumable(stored: VaultKeyStorage.Stored): Boolean {
        val createdAt = stored.pendingCreatedAt ?: return false
        if (stored.pendingRecovery == null) return false
        return clock.now().toEpochMilliseconds() - createdAt in 0..PENDING_MAX_AGE_MS
    }

    /** Items encrypted under old keys are unreadable without them, so they go too. */
    private suspend fun wipeVault() {
        session.close()
        storage.clear()
        guard.reset()
        itemDao.deleteAll()
    }

    private companion object {
        const val PENDING_MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}
