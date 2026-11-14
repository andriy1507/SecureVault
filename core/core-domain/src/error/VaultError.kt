package dev.spaceapps.secvault.core.domain.error

/** Errors the UI can react to. Anything else is an unexpected failure. */
sealed class VaultError(message: String) : Exception(message) {
    /** No PIN / pattern / password set on the device. */
    class DeviceNotSecure : VaultError("Device lock is not set")

    /** User dismissed the unlock prompt. */
    class AuthCancelled : VaultError("Authentication cancelled")

    /** Authentication failed for another reason (lockout, hardware error). */
    class AuthFailed(reason: String? = null) : VaultError("Authentication failed: ${reason.orEmpty()}")

    /**
     * Hardware key was destroyed: device lock removed or biometrics changed.
     * Only the recovery phrase can restore access.
     */
    class KeyInvalidated : VaultError("Device key invalidated")

    /** Operation needs an existing vault. */
    class NoVault : VaultError("No vault on this device")

    /** Operation needs the vault unlocked. */
    class Locked : VaultError("Vault is locked")
}
