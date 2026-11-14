package dev.spaceapps.secvault.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.autofill.domain.AutofillTarget
import dev.spaceapps.secvault.feature.autofill.domain.SaveAutofillCredentialUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** After "Save to SecureVault?": unlock, store the login, lock again. Shows only the OS prompt and a toast. */
class AutofillSaveActivity :
    FragmentActivity(),
    KoinComponent {
    private val keys: VaultKeysRepository by inject()
    private val saveCredential: SaveAutofillCredentialUseCase by inject()
    private var lockWhenDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = AutofillTarget.from(
            intent.getStringExtra(EXTRA_PACKAGE).orEmpty(),
            intent.getStringExtra(EXTRA_WEB_DOMAIN),
        )
        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty()
        val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
        intent.removeExtra(EXTRA_PASSWORD)

        lifecycleScope.launch {
            val message = try {
                keys.refresh()
                if (keys.lockState.value != LockState.Unlocked) {
                    keys.unlock()
                    lockWhenDone = true
                }
                when (val r = saveCredential(target, username, password)) {
                    is SaveAutofillCredentialUseCase.Result.Created -> getString(R.string.autofill_saved, r.title)
                    is SaveAutofillCredentialUseCase.Result.Updated -> getString(R.string.autofill_updated, r.title)
                    SaveAutofillCredentialUseCase.Result.NothingToSave -> null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.AuthCancelled) {
                getString(R.string.autofill_not_saved)
            } catch (e: VaultError.NoVault) {
                getString(R.string.autofill_create_vault_first)
            } catch (e: Exception) {
                getString(R.string.autofill_save_failed, e.message.orEmpty())
            }
            message?.let { Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show() }
            finish()
        }
    }

    override fun onDestroy() {
        if (lockWhenDone) keys.lock()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_WEB_DOMAIN = "web_domain"
        private const val EXTRA_USERNAME = "username"
        private const val EXTRA_PASSWORD = "password"
        private var requestCode = 0

        fun intentSender(
            context: Context,
            form: ParsedForm,
            username: String,
            password: String,
        ): IntentSender {
            // Explicit intent to our own non-exported activity; the extras never leave this app.
            val intent = Intent(context, AutofillSaveActivity::class.java)
                .putExtra(EXTRA_PACKAGE, form.packageName)
                .putExtra(EXTRA_WEB_DOMAIN, form.webDomain)
                .putExtra(EXTRA_USERNAME, username)
                .putExtra(EXTRA_PASSWORD, password)
            return PendingIntent
                .getActivity(
                    context,
                    requestCode++,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ).intentSender
        }
    }
}
