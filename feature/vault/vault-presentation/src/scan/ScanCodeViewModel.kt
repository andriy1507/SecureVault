package dev.spaceapps.secvault.feature.vault.presentation.scan

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.vault.domain.usecase.ParseScannedCodeUseCase
import dev.spaceapps.secvault.feature.vault.domain.usecase.ScannedCode
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.Effect
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.Intent
import dev.spaceapps.secvault.feature.vault.presentation.scan.ScanCodeContract.State
import kotlinx.coroutines.launch

class ScanCodeViewModel(
    private val parseScannedCode: ParseScannedCodeUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {
    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Scanned -> check(intent.text)
            Intent.ToggleTorch -> setState { copy(torchOn = !torchOn) }
            Intent.EnterManually -> sendEffect(Effect.EnterManually)
            Intent.Close -> sendEffect(Effect.Close)
        }
    }

    private fun check(text: String) {
        if (currentState.isChecking) return
        setState { copy(isChecking = true) }
        viewModelScope.launch {
            when (val result = parseScannedCode(text)) {
                is ScannedCode.Otp -> {
                    sendEffect(Effect.Found(CodePrefill(result.info.issuer, result.info.account, result.info.secret)))
                }

                is ScannedCode.BareSecret -> {
                    sendEffect(Effect.Found(CodePrefill("", "", result.secret)))
                }

                ScannedCode.NotAnOtpCode -> {
                    setState { copy(notAnOtpCode = true) }
                    sendEffect(Effect.ScanAgain)
                }
            }
            setState { copy(isChecking = false) }
        }
    }
}
