package dev.spaceapps.secvault.feature.lock.presentation

import androidx.lifecycle.viewModelScope
import dev.spaceapps.secvault.core.domain.error.VaultError
import dev.spaceapps.secvault.core.presentation.mvi.MviViewModel
import dev.spaceapps.secvault.feature.lock.domain.usecase.GetAccountIdUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.UnlockVaultUseCase
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.Effect
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.Error
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.Intent
import dev.spaceapps.secvault.feature.lock.presentation.LockContract.State
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class LockViewModel(
    private val unlockVault: UnlockVaultUseCase,
    private val getAccountId: GetAccountIdUseCase,
) : MviViewModel<Intent, State, Effect>(State()) {

    init {
        viewModelScope.launch {
            val id = runCatching { getAccountId() }.getOrNull()
            setState { copy(accountId = id) }
        }
    }

    override fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.Unlock -> unlock()
        }
    }

    private fun unlock() {
        if (currentState.isUnlocking || currentState.keyInvalidated) return
        setState { copy(isUnlocking = true, error = null) }
        viewModelScope.launch {
            try {
                unlockVault() // lock state becomes Unlocked; the app navigates away
                setState { copy(isUnlocking = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: VaultError.AuthCancelled) {
                setState { copy(isUnlocking = false) }
            } catch (e: VaultError.KeyInvalidated) {
                setState { copy(isUnlocking = false, error = Error.KeyInvalidated) }
            } catch (e: Exception) {
                setState { copy(isUnlocking = false, error = Error.Failed(e.message)) }
            }
        }
    }
}
