package dev.spaceapps.secvault.feature.lock.domain.usecase

import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveLockStateUseCase(
    private val repository: VaultKeysRepository,
) {
    operator fun invoke(): StateFlow<LockState> = repository.lockState
}

class RefreshLockStateUseCase(
    private val repository: VaultKeysRepository,
) {
    suspend operator fun invoke() = repository.refresh()
}

class UnlockVaultUseCase(
    private val repository: VaultKeysRepository,
) {
    suspend operator fun invoke() = repository.unlock()
}

class LockVaultUseCase(
    private val repository: VaultKeysRepository,
) {
    operator fun invoke() = repository.lock()
}

class GetAccountIdUseCase(
    private val repository: VaultKeysRepository,
) {
    suspend operator fun invoke(): String? = repository.accountId()
}
