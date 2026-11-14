package dev.spaceapps.secvault.core.data.security

import android.app.KeyguardManager
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import dev.spaceapps.secvault.core.domain.error.VaultError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AES-256-GCM key in Android Keystore (StrongBox when available).
 * Every use needs a fresh device unlock (PIN / pattern / password / strong biometric).
 * Sealed format: [12-byte IV | ciphertext + tag].
 */
internal class AndroidHardwareKeyGuard(private val context: Context) : HardwareKeyGuard {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun isDeviceSecure(): Boolean =
        context.getSystemService(KeyguardManager::class.java).isDeviceSecure

    override suspend fun seal(plain: ByteArray): ByteArray {
        if (!isDeviceSecure()) throw VaultError.DeviceNotSecure()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        initOrThrow { cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val authed = authenticate(cipher, title = "Protect your vault")
        return authed.iv + authed.doFinal(plain)
    }

    override suspend fun unseal(sealed: ByteArray): ByteArray {
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: throw VaultError.KeyInvalidated()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        initOrThrow { cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, sealed, 0, IV_SIZE)) }
        val authed = authenticate(cipher, title = "Unlock your vault")
        return authed.doFinal(sealed, IV_SIZE, sealed.size - IV_SIZE)
    }

    override fun reset() {
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private inline fun initOrThrow(init: () -> Unit) {
        try {
            init()
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Lock screen removed or biometrics changed: key is gone for good.
            throw VaultError.KeyInvalidated()
        }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return try {
            generateKey(strongBox = true)
        } catch (e: StrongBoxUnavailableException) {
            generateKey(strongBox = false)
        }
    }

    private fun generateKey(strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(
                0, // 0 = authenticate for every single use
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
            )
            .setInvalidatedByBiometricEnrollment(true)
            .setUnlockedDeviceRequired(true)
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    /** Shows BiometricPrompt bound to [cipher]; returns the cipher once the hardware authorizes it. */
    private suspend fun authenticate(cipher: Cipher, title: String): Cipher = withContext(Dispatchers.Main) {
        val activity = CurrentActivity.awaitResumed()
        suspendCancellableCoroutine { cont ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authed = result.cryptoObject?.cipher
                    if (!cont.isActive) return
                    if (authed != null) cont.resume(authed)
                    else cont.resumeWithException(VaultError.AuthFailed("No cipher in result"))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!cont.isActive) return
                    val error = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> VaultError.AuthCancelled()
                        else -> VaultError.AuthFailed(errString.toString())
                    }
                    cont.resumeWithException(error)
                }
                // onAuthenticationFailed = one bad fingerprint; the prompt stays open, nothing to do.
            }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()
            prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "secvault_bundle_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
