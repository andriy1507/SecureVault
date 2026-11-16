package dev.spaceapps.secvault.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.WindowManager
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.SettingsTheme
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.model.VaultItem
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import dev.spaceapps.secvault.feature.autofill.domain.AutofillTarget
import dev.spaceapps.secvault.feature.autofill.domain.FindAutofillMatchesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Opened when the user taps "Unlock SecureVault" in another app.
 * Unlocks with the device lock, finds logins for that app/site, returns the chosen one, then locks again.
 */
class AutofillAuthActivity :
    FragmentActivity(),
    KoinComponent {
    private val keys: VaultKeysRepository by inject()
    private val findMatches: FindAutofillMatchesUseCase by inject()
    private var lockWhenDone = false
    private var screen by mutableStateOf<AutofillScreen>(AutofillScreen.Unlocking)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val usernameIds = ids(EXTRA_USERNAME_IDS)
        val passwordIds = ids(EXTRA_PASSWORD_IDS)
        val target = AutofillTarget.from(
            intent.getStringExtra(EXTRA_PACKAGE).orEmpty(),
            intent.getStringExtra(EXTRA_WEB_DOMAIN),
        )
        setContent {
            SettingsTheme {
                AutofillSheet(screen, onPick = { finishWith(it, usernameIds, passwordIds) }, onCancel = ::cancel)
            }
        }

        lifecycleScope.launch {
            try {
                keys.refresh()
                if (keys.lockState.value != LockState.Unlocked) {
                    keys.unlock() // device lock prompt
                    lockWhenDone = true
                }
                val matches = findMatches(target)
                if (matches.size == 1) {
                    finishWith(matches.single(), usernameIds, passwordIds)
                } else {
                    screen = AutofillScreen.Picker(target.title, matches)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.AuthCancelled) {
                cancel()
            } catch (e: Exception) {
                screen = AutofillScreen.Message(
                    when (e) {
                        is VaultError.NoVault -> getString(R.string.autofill_no_vault)
                        is VaultError.KeyInvalidated -> getString(R.string.autofill_key_invalidated)
                        else -> getString(R.string.autofill_unlock_failed, e.message.orEmpty())
                    },
                )
            }
        }
    }

    private fun finishWith(
        item: VaultItem,
        usernameIds: List<AutofillId>,
        passwordIds: List<AutofillId>,
    ) {
        // A username-only form and a login without a username: nothing to fill
        if (passwordIds.isEmpty() && item.username.isEmpty()) return cancel()
        val values = buildList {
            if (item.username.isNotEmpty()) usernameIds.forEach { add(it to AutofillValue.forText(item.username)) }
            passwordIds.forEach { add(it to AutofillValue.forText(item.password)) }
        }
        val dataset = Datasets.build(this, item.title, values, subtitle = item.username)
        setResult(RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset))
        finish()
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        if (lockWhenDone) keys.lock()
        super.onDestroy()
    }

    private fun ids(key: String): List<AutofillId> =
        IntentCompat.getParcelableArrayListExtra(intent, key, AutofillId::class.java).orEmpty()

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_WEB_DOMAIN = "web_domain"
        private const val EXTRA_USERNAME_IDS = "username_ids"
        private const val EXTRA_PASSWORD_IDS = "password_ids"
        private var requestCode = 0

        fun intentSender(
            context: Context,
            form: ParsedForm,
        ): IntentSender {
            val intent = Intent(context, AutofillAuthActivity::class.java)
                .putExtra(EXTRA_PACKAGE, form.packageName)
                .putExtra(EXTRA_WEB_DOMAIN, form.webDomain)
                .putParcelableArrayListExtra(EXTRA_USERNAME_IDS, ArrayList(form.usernameIds))
                .putParcelableArrayListExtra(EXTRA_PASSWORD_IDS, ArrayList(form.passwordIds))
            // MUTABLE: the autofill framework adds its own extras when it starts the activity.
            return PendingIntent
                .getActivity(
                    context,
                    requestCode++,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE,
                ).intentSender
        }
    }
}
