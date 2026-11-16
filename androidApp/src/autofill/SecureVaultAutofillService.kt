package dev.spaceapps.secvault.autofill

import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillValue
import dev.spaceapps.secvault.R
import dev.spaceapps.secvault.core.domain.model.PasswordOptions
import dev.spaceapps.secvault.core.domain.usecase.GeneratePasswordUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * System autofill provider. The vault is always locked here (the app locks when it leaves the screen),
 * so suggestions never carry secrets: "Unlock SecureVault" opens [AutofillAuthActivity], which unlocks
 * with the device lock and returns the chosen login. Generated passwords need no unlock.
 */
class SecureVaultAutofillService :
    AutofillService(),
    KoinComponent {
    private val generatePassword: GeneratePasswordUseCase by inject()

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: return callback.onSuccess(null)
        val form = StructureParser.parse(structure)
        if (form.isEmpty || form.packageName == packageName) return callback.onSuccess(null)

        val response = FillResponse.Builder()

        // 1. Unlock and pick a saved login
        response.addDataset(
            Datasets.build(
                context = this,
                label = getString(R.string.autofill_unlock),
                subtitle = getString(R.string.autofill_unlock_subtitle),
                values = form.allIds.map { it to null }, // filled after authentication
                authentication = AutofillAuthActivity.intentSender(this, form),
            ),
        )

        // 2. Sign-up / change-password form: a fresh strong password, no unlock needed
        if (form.isNewPassword && form.passwordIds.isNotEmpty()) {
            val password = generatePassword(PasswordOptions())
            val value = AutofillValue.forText(password)
            response.addDataset(
                Datasets.build(
                    context = this,
                    label = getString(R.string.autofill_strong_password),
                    values = form.passwordIds.map { it to value },
                    subtitle = getString(R.string.autofill_strong_password_subtitle),
                    icon = R.drawable.ic_autofill_key,
                ),
            )
        }

        // 3. After submit, Android offers "Save to SecureVault?"
        if (form.passwordIds.isNotEmpty()) {
            val type = if (form.usernameIds.isNotEmpty()) {
                SaveInfo.SAVE_DATA_TYPE_PASSWORD or
                    SaveInfo.SAVE_DATA_TYPE_USERNAME
            } else {
                SaveInfo.SAVE_DATA_TYPE_PASSWORD
            }
            response.setSaveInfo(
                SaveInfo
                    .Builder(type, form.passwordIds.toTypedArray())
                    .apply { if (form.usernameIds.isNotEmpty()) setOptionalIds(form.usernameIds.toTypedArray()) }
                    .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
                    .build(),
            )
        }
        callback.onSuccess(response.build())
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: return callback.onSuccess()
        val form = StructureParser.parse(structure)
        val password = form.passwordValue
        if (password.isNullOrEmpty() || form.packageName == packageName) return callback.onSuccess()
        // Saving encrypts with the vault key, so it needs an unlock too: hand over to an activity.
        callback.onSuccess(AutofillSaveActivity.intentSender(this, form, form.usernameValue.orEmpty(), password))
    }
}
