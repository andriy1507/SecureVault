package dev.spaceapps.secvault.feature.lock.presentation

import dev.spaceapps.secvault.core.presentation.mvi.UiEffect
import dev.spaceapps.secvault.core.presentation.mvi.UiIntent
import dev.spaceapps.secvault.core.presentation.mvi.UiState

object LockContract {

    sealed interface Error {
        /** Screen lock or biometrics changed, the device key is gone. */
        data object KeyInvalidated : Error
        data class Failed(val reason: String?) : Error
    }

    data class State(
        val accountId: String? = null,
        val isUnlocking: Boolean = false,
        val error: Error? = null,
    ) : UiState {
        val keyInvalidated: Boolean get() = error == Error.KeyInvalidated
    }

    sealed interface Intent : UiIntent {
        data object Unlock : Intent
    }

    sealed interface Effect : UiEffect
}
