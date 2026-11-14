package dev.spaceapps.secvault.passkey

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.domerrors.InvalidStateError
import androidx.credentials.exceptions.domerrors.SecurityError
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import androidx.credentials.provider.PendingIntentHandler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.domain.passkey.AssertPasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.CreatePasskeyUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.FindPasskeysUseCase
import dev.spaceapps.secvault.feature.vault.domain.passkey.PasskeyAlreadyExistsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Unlocks done for passkeys. "Unlock Vault" in the sign-in sheet is followed by the user picking a
 * passkey; that pick doesn't ask again within [VERIFIED_WINDOW_MS]. A vault that was locked before
 * is locked again when the flow ends, or after [UNLOCK_TIMEOUT_MS] if the user walks away.
 */
internal object PasskeySession {
    private const val VERIFIED_WINDOW_MS = 60_000L
    private const val UNLOCK_TIMEOUT_MS = 120_000L
    private var verifiedAt = 0L
    private var lockWhenDone = false
    private val handler = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null

    fun verified(wasLocked: Boolean, keys: VaultKeysRepository) {
        verifiedAt = SystemClock.elapsedRealtime()
        if (wasLocked) {
            lockWhenDone = true
            timeout?.let(handler::removeCallbacks)
            timeout = Runnable { finish(keys) }.also { handler.postDelayed(it, UNLOCK_TIMEOUT_MS) }
        }
    }

    /** True once per unlock: the next passkey use right after "Unlock Vault" doesn't prompt again. */
    fun consumeRecentVerification(): Boolean {
        val recent = verifiedAt != 0L && SystemClock.elapsedRealtime() - verifiedAt < VERIFIED_WINDOW_MS
        verifiedAt = 0L
        return recent
    }

    fun finish(keys: VaultKeysRepository) {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        verifiedAt = 0L
        if (lockWhenDone) keys.lock()
        lockWhenDone = false
    }
}

/** Base for the passkey screens: no UI of their own, only the phone's unlock prompt over the other app. */
@RequiresApi(34)
abstract class PasskeyActivity : FragmentActivity(), KoinComponent {
    protected val keys: VaultKeysRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * WebAuthn user verification: the phone's screen lock or biometrics, every time, except right
     * after "Unlock Vault" in the same sheet.
     */
    protected suspend fun verifyUser() {
        if (PasskeySession.consumeRecentVerification() && keys.lockState.value == LockState.Unlocked) return
        keys.refresh()
        val wasLocked = keys.lockState.value != LockState.Unlocked
        keys.unlock()
        PasskeySession.verified(wasLocked, keys)
    }

    protected fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    protected fun finishWith(result: Intent?) {
        setResult(if (result == null) RESULT_CANCELED else RESULT_OK, result)
        finish()
    }
}

/** "Save to Vault" in the system's create-passkey sheet. */
@RequiresApi(34)
class PasskeyCreateActivity : PasskeyActivity() {
    private val createPasskey: CreatePasskeyUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        val calling = request?.callingRequest as? CreatePublicKeyCredentialRequest
        PasskeyLog.step("create: request=${request != null} publicKey=${calling != null}")
        if (request == null || calling == null) return fail(CreateCredentialUnknownException())

        lifecycleScope.launch {
            try {
                val caller = PasskeyJson.caller(this@PasskeyCreateActivity, request.callingAppInfo)
                PasskeyLog.step("create: caller=${caller.packageName} browser=${caller.isBrowser} origin=${caller.origin}")
                val options = PasskeyJson.parseCreation(calling.requestJson)
                PasskeyLog.step("create: rpId=${options.request.rpId}")
                PasskeyJson.checkOrigin(caller, options.request.rpId)
                verifyUser()
                PasskeyLog.step("create: user verified")
                val created = createPasskey(options.request)
                PasskeyLog.step("create: saved to item")
                val clientData = PasskeyJson.creationClientData(options.challenge, caller)
                val response = CreatePublicKeyCredentialResponse(PasskeyJson.registrationResponse(created, clientData))
                toast(getString(R.string.passkey_saved, options.request.rpName.ifBlank { options.request.rpId }))
                finishWith(Intent().also { PendingIntentHandler.setCreateCredentialResponse(it, response) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.AuthCancelled) {
                PasskeyLog.failed("create", e)
                fail(CreateCredentialCancellationException())
            } catch (e: PasskeyAlreadyExistsException) {
                PasskeyLog.failed("create", e)
                toast(getString(R.string.passkey_exists))
                fail(CreatePublicKeyCredentialDomException(InvalidStateError(), e.message))
            } catch (e: PasskeyRequestException) {
                PasskeyLog.failed("create", e)
                toast(getString(R.string.passkey_blocked))
                fail(CreatePublicKeyCredentialDomException(SecurityError(), e.message))
            } catch (e: Exception) {
                PasskeyLog.failed("create", e)
                toast(getString(R.string.passkey_failed, e.message.orEmpty()))
                fail(CreateCredentialUnknownException(e.message))
            } finally {
                PasskeySession.finish(keys)
            }
        }
    }

    private fun fail(error: CreateCredentialException) =
        finishWith(Intent().also { PendingIntentHandler.setCreateCredentialException(it, error) })
}

/** The user picked one of Vault's passkeys in the sign-in sheet. */
@RequiresApi(34)
class PasskeySignInActivity : PasskeyActivity() {
    private val assertPasskey: AssertPasskeyUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        val option = request?.credentialOptions?.filterIsInstance<GetPublicKeyCredentialOption>()?.firstOrNull()
        val itemId = intent.getStringExtra(PasskeyIntents.EXTRA_ITEM_ID)
        val credentialId = intent.getStringExtra(PasskeyIntents.EXTRA_CREDENTIAL_ID)
        PasskeyLog.step("sign in: request=${request != null} option=${option != null} entry=${itemId != null && credentialId != null}")
        if (request == null || option == null || itemId == null || credentialId == null) {
            return fail(GetCredentialUnknownException())
        }

        lifecycleScope.launch {
            try {
                val caller = PasskeyJson.caller(this@PasskeySignInActivity, request.callingAppInfo)
                val options = PasskeyJson.parseSignIn(option.requestJson)
                PasskeyJson.checkOrigin(caller, options.rpId)
                verifyUser()
                val clientData = PasskeyJson.signInClientData(options.challenge, caller)
                // A browser computes clientDataJSON itself and passes only its hash.
                val clientDataHash = option.clientDataHash ?: PasskeyJson.sha256(clientData)
                val assertion = assertPasskey(itemId, credentialId, options.rpId, clientDataHash)
                    ?: return@launch fail(NoCredentialException()).also { PasskeyLog.step("sign in: passkey not found") }
                PasskeyLog.step("sign in: signed for rpId=${options.rpId}")
                val credential = PublicKeyCredential(PasskeyJson.assertionResponse(assertion, clientData))
                finishWith(Intent().also { PendingIntentHandler.setGetCredentialResponse(it, GetCredentialResponse(credential)) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.AuthCancelled) {
                PasskeyLog.failed("sign in", e)
                fail(GetCredentialCancellationException())
            } catch (e: PasskeyRequestException) {
                PasskeyLog.failed("sign in", e)
                toast(getString(R.string.passkey_blocked))
                fail(GetPublicKeyCredentialDomException(SecurityError(), e.message))
            } catch (e: Exception) {
                PasskeyLog.failed("sign in", e)
                fail(GetCredentialUnknownException(e.message))
            } finally {
                PasskeySession.finish(keys)
            }
        }
    }

    private fun fail(error: GetCredentialException) =
        finishWith(Intent().also { PendingIntentHandler.setGetCredentialException(it, error) })
}

/** "Unlock Vault" in the sign-in sheet: unlocks, then hands the system the list of matching passkeys. */
@RequiresApi(34)
class PasskeyUnlockActivity : PasskeyActivity() {
    private val findPasskeys: FindPasskeysUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) ?: return finishWith(null)

        lifecycleScope.launch {
            try {
                keys.refresh()
                val wasLocked = keys.lockState.value != LockState.Unlocked
                keys.unlock()
                PasskeySession.verified(wasLocked, keys)
                val response = PasskeyEntries.build(this@PasskeyUnlockActivity, request, findPasskeys)
                if (response.credentialEntries.isEmpty()) {
                    toast(getString(R.string.passkey_none))
                    PasskeySession.finish(keys)
                }
                finishWith(Intent().also { PendingIntentHandler.setBeginGetCredentialResponse(it, response) })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PasskeyLog.failed("unlock", e)
                if (e !is VaultError.AuthCancelled) toast(getString(R.string.passkey_failed, e.message.orEmpty()))
                PasskeySession.finish(keys)
                finishWith(null)
            }
        }
    }
}
