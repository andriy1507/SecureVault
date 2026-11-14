package dev.spaceapps.secvault.core.data.session

import dev.spaceapps.secvault.core.data.crypto.KeyBundle
import dev.spaceapps.secvault.core.domain.error.VaultError

/** Keys of the unlocked vault. Lives only in memory; [close] wipes them. */
internal class VaultSession {
    private var bundle: KeyBundle? = null

    val isOpen: Boolean get() = bundle != null

    fun open(keys: KeyBundle) {
        close()
        bundle = keys
    }

    fun close() {
        bundle?.wipe()
        bundle = null
    }

    /** A copy of the vault key; callers wipe it after use. */
    fun vaultKeyCopy(): ByteArray = bundle?.vaultKey?.copyOf() ?: throw VaultError.Locked()
}
