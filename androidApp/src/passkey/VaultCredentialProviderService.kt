package dev.spaceapps.secvault.passkey

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.vault.domain.passkey.FindPasskeysUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Vault as a passkey provider in Android's Credential Manager (Android 14+).
 * The user turns it on in Settings › Passwords, passkeys & accounts.
 * While the vault is locked, sign-in shows only "Unlock Vault", so the system never sees
 * which sites have passkeys until the user unlocks.
 */
@RequiresApi(34)
class VaultCredentialProviderService :
    CredentialProviderService(),
    KoinComponent {
    private val keys: VaultKeysRepository by inject()
    private val findPasskeys: FindPasskeysUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        PasskeyLog.step("begin create: ${request::class.simpleName} from ${request.callingAppInfo?.packageName}")
        if (request !is BeginCreatePublicKeyCredentialRequest) {
            callback.onError(CreateCredentialUnsupportedException())
            return
        }
        scope.launch {
            keys.refresh()
            PasskeyLog.step("begin create: vault ${keys.lockState.value}")
            if (keys.lockState.value == LockState.NoVault) {
                callback.onError(CreateCredentialNoCreateOptionException())
                return@launch
            }
            val entry = CreateEntry
                .Builder(
                    getString(R.string.passkey_account_name),
                    PasskeyIntents.create(this@VaultCredentialProviderService),
                ).setDescription(getString(R.string.passkey_create_description))
                .build()
            callback.onResult(BeginCreateCredentialResponse(createEntries = listOf(entry)))
        }
    }

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        val job = scope.launch {
            try {
                keys.refresh()
                val wantsPasskey = request.beginGetCredentialOptions.any { it is BeginGetPublicKeyCredentialOption }
                val response = when {
                    !wantsPasskey -> {
                        BeginGetCredentialResponse()
                    }

                    keys.lockState.value == LockState.Unlocked -> {
                        PasskeyEntries.build(this@VaultCredentialProviderService, request, findPasskeys)
                    }

                    keys.lockState.value == LockState.Locked -> {
                        BeginGetCredentialResponse(
                            authenticationActions = listOf(
                                AuthenticationAction(
                                    getString(R.string.passkey_unlock_action),
                                    PasskeyIntents.unlock(this@VaultCredentialProviderService),
                                ),
                            ),
                        )
                    }

                    else -> {
                        BeginGetCredentialResponse()
                    }
                }
                PasskeyLog.step(
                    "begin get: passkey=$wantsPasskey vault=${keys.lockState.value} " +
                        "entries=${response.credentialEntries.size} unlockAction=${response.authenticationActions.isNotEmpty()}",
                )
                callback.onResult(response)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PasskeyLog.failed("begin get", e)
                callback.onError(GetCredentialUnknownException(e.message))
            }
        }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // Vault keeps no sign-in session per app, so there's nothing to clear.
        callback.onResult(null)
    }
}

/** Passkey entries for a sign-in request; needs an unlocked vault. Used by the service and after unlock. */
@RequiresApi(34)
internal object PasskeyEntries {
    suspend fun build(
        context: Context,
        request: BeginGetCredentialRequest,
        find: FindPasskeysUseCase,
    ): BeginGetCredentialResponse {
        val entries =
            request.beginGetCredentialOptions
                .filterIsInstance<BeginGetPublicKeyCredentialOption>()
                .flatMap { option ->
                    val options = runCatching { PasskeyJson.parseSignIn(option.requestJson) }
                        .onFailure { PasskeyLog.failed("parse sign-in request", it) }
                        .getOrNull() ?: return@flatMap emptyList()
                    PasskeyLog.step("sign-in request for rpId=${options.rpId}")
                    find(options.rpId, options.allowCredentialIds).map { match ->
                        val passkey = match.passkey
                        PublicKeyCredentialEntry
                            .Builder(
                                context,
                                passkey.userName.ifBlank { match.item.username.ifBlank { match.item.title } },
                                PasskeyIntents.signIn(context, match.item.id, passkey.credentialId),
                                option,
                            ).setDisplayName(passkey.userDisplayName.ifBlank { match.item.title })
                            .build()
                    }
                }
        return BeginGetCredentialResponse(credentialEntries = entries)
    }
}

/** PendingIntents for the system sheet; mutable because the system adds the request to them. */
internal object PasskeyIntents {
    const val EXTRA_ITEM_ID = "passkey_item_id"
    const val EXTRA_CREDENTIAL_ID = "passkey_credential_id"
    private var requestCode = 0

    fun create(context: Context) = pending(context, Intent(context, PasskeyCreateActivity::class.java))

    fun unlock(context: Context) = pending(context, Intent(context, PasskeyUnlockActivity::class.java))

    fun signIn(
        context: Context,
        itemId: String,
        credentialId: String,
    ) = pending(
        context,
        Intent(context, PasskeySignInActivity::class.java)
            .putExtra(EXTRA_ITEM_ID, itemId)
            .putExtra(EXTRA_CREDENTIAL_ID, credentialId),
    )

    private fun pending(
        context: Context,
        intent: Intent,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode++,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
