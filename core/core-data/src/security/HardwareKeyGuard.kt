package dev.spaceapps.secvault.core.data.security

/**
 * Seals data with a non-exportable hardware key (Android Keystore / iOS Secure Enclave)
 * that only works after the user passes the device lock (PIN, pattern, biometrics).
 *
 * Errors are thrown as [dev.spaceapps.secvault.core.domain.error.VaultError].
 */
interface HardwareKeyGuard {
    fun isDeviceSecure(): Boolean

    /** May show the OS prompt (Android does, iOS doesn't). */
    suspend fun seal(plain: ByteArray): ByteArray

    /** Always shows the OS prompt. */
    suspend fun unseal(sealed: ByteArray): ByteArray

    /** Deletes the hardware key. Anything sealed with it becomes unreadable. */
    fun reset()
}
