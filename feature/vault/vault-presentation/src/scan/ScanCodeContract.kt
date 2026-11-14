package dev.spaceapps.secvault.feature.vault.presentation.scan

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState

/** What the editor gets from a scanned 2FA QR code. */
data class CodePrefill(val issuer: String, val account: String, val secret: String) {
    override fun toString() = "CodePrefill(issuer=$issuer)"
}

object ScanCodeContract {

    data class State(
        val torchOn: Boolean = false,
        /** Last scanned QR wasn't a 2FA code; scanning continues. */
        val notAnOtpCode: Boolean = false,
        val isChecking: Boolean = false,
    ) : UiState

    sealed interface Intent : UiIntent {
        data class Scanned(val text: String) : Intent
        data object ToggleTorch : Intent
        data object EnterManually : Intent
        data object Close : Intent
    }

    sealed interface Effect : UiEffect {
        data class Found(val prefill: CodePrefill) : Effect
        /** Restart the camera after a QR that wasn't a 2FA code. */
        data object ScanAgain : Effect
        data object EnterManually : Effect
        data object Close : Effect
    }
}
