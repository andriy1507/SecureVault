package dev.spaceapps.secvault

import dev.spaceapps.secvault.core.data.security.HardwareKeyGuard
import dev.spaceapps.secvault.core.domain.error.VaultError
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Implemented in Swift (SecureEnclaveKeyGuard.swift) with CryptoKit + LocalAuthentication.
 * Bytes cross the bridge as base64 strings to keep the Swift side simple.
 * completion(result, errorCode): errorCode is one of ERROR_* or null on success.
 */
interface NativeKeyGuard {
    fun isDeviceSecure(): Boolean
    fun seal(plainBase64: String, completion: (String?, String?) -> Unit)
    fun unseal(sealedBase64: String, reason: String, completion: (String?, String?) -> Unit)
    fun reset()

    companion object {
        const val ERROR_CANCELLED = "cancelled"
        const val ERROR_INVALIDATED = "invalidated"
        const val ERROR_NOT_SECURE = "not_secure"
    }
}

/** Adapts the callback-based Swift bridge to the suspend [HardwareKeyGuard]. */
@OptIn(ExperimentalEncodingApi::class)
internal class IosHardwareKeyGuard(private val native: NativeKeyGuard) : HardwareKeyGuard {

    override fun isDeviceSecure(): Boolean = native.isDeviceSecure()

    override suspend fun seal(plain: ByteArray): ByteArray =
        call { done -> native.seal(Base64.encode(plain), done) }

    override suspend fun unseal(sealed: ByteArray): ByteArray =
        call { done -> native.unseal(Base64.encode(sealed), "Unlock your vault", done) }

    override fun reset() = native.reset()

    private suspend fun call(block: ((String?, String?) -> Unit) -> Unit): ByteArray =
        suspendCancellableCoroutine { cont ->
            block { result, error ->
                if (!cont.isActive) return@block
                if (result != null) cont.resume(Base64.decode(result))
                else cont.resumeWithException(error.toVaultError())
            }
        }

    private fun String?.toVaultError(): VaultError = when (this) {
        NativeKeyGuard.ERROR_CANCELLED -> VaultError.AuthCancelled()
        NativeKeyGuard.ERROR_INVALIDATED -> VaultError.KeyInvalidated()
        NativeKeyGuard.ERROR_NOT_SECURE -> VaultError.DeviceNotSecure()
        else -> VaultError.AuthFailed(this)
    }
}
