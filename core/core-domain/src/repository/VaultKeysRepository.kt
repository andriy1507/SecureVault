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

    /** User confirmed the recovery phrase: vault becomes active and unlocked. */
    suspend fun completeOnboarding()

    /** Shows the OS prompt and loads keys into memory. */
    suspend fun unlock()

    /** Anonymous account ID ("7F3A-91C2-E04B-D58E"), null when there is no vault. */
    suspend fun accountId(): String?

    /** Erases the vault, its keys and all items from this device. */
    suspend fun deleteVault()

    /** Wipes keys from memory. No-op unless unlocked. */
    fun lock()
}
