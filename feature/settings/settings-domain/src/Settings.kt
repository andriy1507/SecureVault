package dev.spaceapps.secvault.feature.settings.domain

import dev.spaceapps.secvault.core.domain.repository.VaultKeysRepository
import kotlinx.coroutines.flow.Flow

enum class AppTheme { System, Light, Dark }

/** When the unlocked vault locks itself after the app goes to the background. */
enum class AutoLock(val delaySeconds: Int?) {
    Immediately(0),
    OneMinute(60),
    FiveMinutes(5 * 60),
    FifteenMinutes(15 * 60),

    /** Only when the phone's screen turns off / the phone locks. */
    OnDeviceLock(null),
}

/** How long copied secrets stay on the clipboard. */
enum class ClipboardClear(val seconds: Int?) {
    TenSeconds(10),
    ThirtySeconds(30),
    OneMinute(60),
    Never(null),
}

data class AppSettings(
    val theme: AppTheme = AppTheme.System,
    val dynamicColor: Boolean = false,
    val autoLock: AutoLock = AutoLock.OneMinute,
    val clipboardClear: ClipboardClear = ClipboardClear.ThirtySeconds,
    /** Android: FLAG_SECURE (no screenshots, hidden in recent apps). */
    val blockScreenshots: Boolean = true,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun reset()
}

class ObserveSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

class UpdateSettingsUseCase(private val repository: SettingsRepository) {
    suspend operator fun invoke(transform: (AppSettings) -> AppSettings) = repository.update(transform)
}

class GetAccountIdForSettingsUseCase(private val keys: VaultKeysRepository) {
    suspend operator fun invoke(): String? = keys.accountId()
}

class LockNowUseCase(private val keys: VaultKeysRepository) {
    operator fun invoke() = keys.lock()
}

/** Erases the vault from this device and resets the app to its first-run state. */
class DeleteAccountUseCase(private val keys: VaultKeysRepository, private val settings: SettingsRepository) {
    suspend operator fun invoke() {
        keys.deleteVault()
        settings.reset()
    }
}

/** Text the user must type to confirm account deletion. Not translated on purpose: it's a code, not a word. */
const val DELETE_CONFIRMATION = "DELETE"
