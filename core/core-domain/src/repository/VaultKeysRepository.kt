package dev.spaceapps.secvault.core.domain.repository

import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.model.VaultCreation
import kotlinx.coroutines.flow.StateFlow

/** Owns the vault keys: creation, sealing in hardware, unlocking and locking. */
interface VaultKeysRepository {
    val lockState: StateFlow<LockState>

    /** Reads storage and updates [lockState]. Call on app start. */
    suspend fun refresh()

    fun isDeviceSecure(): Boolean

    /**
     * Generates all keys and seals them with the hardware key (may show the OS prompt).
     * The vault stays pending until [completeOnboarding] is called after the user confirms the phrase.
     */
    suspend fun createVault(): VaultCreation

    /** True when an unconfirmed vault from an earlier run can still be resumed with [resumePendingVault]. */
    suspend fun hasPendingVault(): Boolean

    /**
     * Shows the OS prompt and returns the recovery phrase of the unconfirmed vault again, so onboarding
     * can continue after the app was killed. Finish with [completeOnboarding].
     */
    suspend fun resumePendingVault(): VaultCreation

    /** User confirmed the recovery phrase: vault becomes active and unlocked. */
    suspend fun completeOnboarding()

    /**
     * Restores an existing account on this device from its recovery phrase and the recovery data
     * carried in a backup. Keeps the account ID; device keys are fresh. Replaces any vault on this device.
     *
     * [populate] runs with the keys loaded (e.g. to import the backup's items). If it fails, the
     * restored vault is wiped again. The vault becomes unlocked only after it succeeds.
     *
     * Throws [dev.spaceapps.secvault.core.domain.error.VaultError.InvalidRecoveryPhrase] or
     * [dev.spaceapps.secvault.core.domain.error.VaultError.RecoveryPhraseMismatch] before touching anything.
     */
    suspend fun restoreVault(
        words: List<String>,
        accountId: String,
        recoveryBlob: ByteArray,
        populate: suspend () -> Unit,
    )

    /** Shows the OS prompt and loads keys into memory. */
    suspend fun unlock()

    /** Anonymous account ID ("7F3A-91C2-E04B-D58E"), null when there is no vault. */
    suspend fun accountId(): String?

    /** Erases the vault, its keys and all items from this device. */
    suspend fun deleteVault()

    /** Wipes keys from memory. No-op unless unlocked. */
    fun lock()
}
