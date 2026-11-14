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

internal class VaultKeysRepositoryImpl(
    private val guard: HardwareKeyGuard,
    private val storage: VaultKeyStorage,
    private val wordlist: WordlistSource,
    private val session: VaultSession,
    private val itemDao: VaultItemDao,
) : VaultKeysRepository {

    private val _lockState = MutableStateFlow(LockState.Unknown)
    override val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val mutex = Mutex()

    /** Keys of a vault created in this process whose phrase is not confirmed yet. */
    private var pending: KeyBundle? = null

    override fun isDeviceSecure(): Boolean = guard.isDeviceSecure()

    override suspend fun refresh() = mutex.withLock {
        val stored = storage.read()
        _lockState.value = when {
            pending != null -> LockState.NoVault // onboarding in progress
            stored.sealedBundle == null -> LockState.NoVault
            !stored.recoveryConfirmed -> {
                // Onboarding was interrupted: the phrase was never confirmed, so it may be lost.
                wipeVault()
                LockState.NoVault
            }
            session.isOpen -> LockState.Unlocked
            else -> LockState.Locked
        }
    }

    override suspend fun createVault(): VaultCreation = mutex.withLock {
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
        storage.saveNewVault(account.accountId, sealed, account.recoveryBlob)
        pending = account.bundle
        VaultCreation(account.accountId, account.recoveryWords)
    }

    override suspend fun completeOnboarding() = mutex.withLock {
        val bundle = pending ?: throw VaultError.NoVault()
        storage.setRecoveryConfirmed()
        pending = null
        session.open(bundle)
        _lockState.value = LockState.Unlocked
    }

    override suspend fun unlock() = mutex.withLock {
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

    override suspend fun deleteVault() = mutex.withLock {
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

    /** Items encrypted under old keys are unreadable without them, so they go too. */
    private suspend fun wipeVault() {
        session.close()
        storage.clear()
        guard.reset()
        itemDao.deleteAll()
    }
}
