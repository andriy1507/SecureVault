package dev.spaceapps.secvault.core.domain.error

/** Errors the UI can react to. Anything else is an unexpected failure. */
sealed class VaultError(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** No PIN / pattern / password set on the device. */
    class DeviceNotSecure : VaultError("Device lock is not set")

    /** User dismissed the unlock prompt. */
    class AuthCancelled : VaultError("Authentication cancelled")

    /** Authentication failed for another reason (lockout, hardware error). */
    class AuthFailed(
        reason: String? = null,
    ) : VaultError("Authentication failed: ${reason.orEmpty()}")

    /**
     * Hardware key was destroyed: device lock removed or biometrics changed.
     * Only the recovery phrase can restore access.
     */
    class KeyInvalidated : VaultError("Device key invalidated")

    /** Operation needs an existing vault. */
    class NoVault : VaultError("No vault on this device")

    /** Operation needs the vault unlocked. */
    class Locked : VaultError("Vault is locked")

    /** Not a valid 24-word recovery phrase (unknown word, wrong count or bad checksum). */
    class InvalidRecoveryPhrase(
        cause: Throwable? = null,
    ) : VaultError("Invalid recovery phrase", cause)

    /** A valid phrase, but not the one of this account / backup. */
    class RecoveryPhraseMismatch(
        cause: Throwable? = null,
    ) : VaultError("Recovery phrase does not match", cause)

    /** The file is not a SecureVault backup, is damaged, or comes from a newer app version. */
    class UnsupportedBackup(
        cause: Throwable? = null,
    ) : VaultError("Unsupported or damaged backup", cause)
}
