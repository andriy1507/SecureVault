package dev.spaceapps.secvault

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.secvault.core.domain.model.LockState
import dev.spaceapps.secvault.core.ui.clipboard.LocalClipboardClearSeconds
import dev.spaceapps.secvault.core.ui.components.SplashScreen
import dev.spaceapps.secvault.core.ui.navigation.PlatformBackHandler
import dev.spaceapps.secvault.core.ui.theme.SecureVaultTheme
import dev.spaceapps.secvault.core.ui.theme.ThemeMode
import dev.spaceapps.secvault.core.ui.theme.isDynamicColorSupported
import dev.spaceapps.secvault.feature.generator.domain.usecase.ClearGeneratorHistoryUseCase
import dev.spaceapps.secvault.feature.legal.ui.LegalDocument
import dev.spaceapps.secvault.feature.legal.ui.LegalScreen
import dev.spaceapps.secvault.feature.lock.domain.usecase.LockVaultUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.ObserveLockStateUseCase
import dev.spaceapps.secvault.feature.lock.domain.usecase.RefreshLockStateUseCase
import dev.spaceapps.secvault.feature.settings.domain.AppSettings
import dev.spaceapps.secvault.feature.settings.domain.AppTheme
import dev.spaceapps.secvault.feature.settings.domain.AutoLock
import dev.spaceapps.secvault.feature.settings.domain.ObserveSettingsUseCase
import dev.spaceapps.secvault.feature.settings.ui.SettingsPlatform
import dev.spaceapps.secvault.navigation.LockRoute
import dev.spaceapps.secvault.navigation.OnboardingRoute
import dev.spaceapps.secvault.navigation.ScopedViewModels
import dev.spaceapps.secvault.navigation.VaultNavHost
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.koin.compose.koinInject

private fun AppTheme.toThemeMode() =
    when (this) {
        AppTheme.System -> ThemeMode.System
        AppTheme.Light -> ThemeMode.Light
        AppTheme.Dark -> ThemeMode.Dark
    }

/** Root: the lock state decides which screen is shown; settings drive theme, auto-lock and the clipboard. */
@Composable
@Preview
@DevelopmentEntryPoint
fun App() {
    val observeSettings = koinInject<ObserveSettingsUseCase>()
    val settings by remember { observeSettings() }.collectAsStateWithLifecycle(AppSettings())

    SecureVaultTheme(themeMode = settings.theme.toThemeMode(), dynamicColor = settings.dynamicColor) {
        CompositionLocalProvider(LocalClipboardClearSeconds provides settings.clipboardClear.seconds) {
            SecureWindow(settings.blockScreenshots)
            // Paints the theme background and sets content colors for every screen, including
            // those without a Scaffold (splash, lock, onboarding).
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                AppRoot(settings)
            }
        }
    }
}

/** The theme chosen in Settings (light/dark, dynamic color), for windows outside [App] such as autofill. */
@Composable
fun SettingsTheme(content: @Composable () -> Unit) {
    val observeSettings = koinInject<ObserveSettingsUseCase>()
    val settings by remember { observeSettings() }.collectAsStateWithLifecycle(AppSettings())
    SecureVaultTheme(themeMode = settings.theme.toThemeMode(), dynamicColor = settings.dynamicColor, content = content)
}

@OptIn(ExperimentalTime::class)
@Composable
private fun AppRoot(settings: AppSettings) {
    val observeLockState = koinInject<ObserveLockStateUseCase>()
    val refreshLockState = koinInject<RefreshLockStateUseCase>()
    val lockVault = koinInject<LockVaultUseCase>()
    val clearGeneratorHistory = koinInject<ClearGeneratorHistoryUseCase>()
    val openSecuritySettings = rememberOpenSecuritySettings()
    val openAutofillSettings = rememberAutofillSettingsAction()
    val openPasskeySettings = rememberPasskeySettingsAction()
    val appVersion = rememberAppVersion()
    val scope = rememberCoroutineScope()

    val lockState by observeLockState().collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { refreshLockState() }
    LaunchedEffect(lockState) { if (lockState != LockState.Unlocked) clearGeneratorHistory() }

    // Auto-lock. Only acts on an unlocked vault, so OS unlock prompts during onboarding/unlock don't interfere.
    var backgroundedAt by remember { mutableStateOf<Long?>(null) }
    var lockJob by remember { mutableStateOf<Job?>(null) }
    val autoLock = settings.autoLock
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        backgroundedAt = Clock.System.now().toEpochMilliseconds()
        val delaySeconds = autoLock.delaySeconds ?: return@LifecycleEventEffect
        if (delaySeconds == 0) {
            lockVault()
        } else {
            lockJob?.cancel()
            lockJob = scope.launch {
                delay(delaySeconds.seconds)
                lockVault()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        lockJob?.cancel()
        val since = backgroundedAt
        val delaySeconds = autoLock.delaySeconds
        // The timer may not have run while the process was frozen: check the elapsed time too.
        if (since != null && delaySeconds != null &&
            Clock.System.now().toEpochMilliseconds() - since >= delaySeconds.seconds.inWholeMilliseconds
        ) {
            lockVault()
        }
        backgroundedAt = null
    }
    if (autoLock == AutoLock.OnDeviceLock) DeviceLockEffect { lockVault() }

    when (lockState) {
        LockState.Unknown -> SplashScreen()

        LockState.NoVault -> ScopedViewModels { OnboardingFlow(openSecuritySettings) }

        LockState.Locked -> ScopedViewModels { LockRoute() }

        LockState.Unlocked -> ScopedViewModels {
            VaultNavHost(
                onLock = lockVault::invoke,
                settingsPlatform = SettingsPlatform(
                    appVersion = appVersion,
                    onOpenAutofill = openAutofillSettings,
                    onOpenPasskeys = openPasskeySettings,
                    dynamicColorSupported = isDynamicColorSupported,
                    screenshotBlockingSupported = isScreenshotBlockingSupported,
                ),
            )
        }
    }
}

/** Welcome / create-account flow, with the legal documents on top. */
@Composable
private fun OnboardingFlow(openSecuritySettings: () -> Unit) {
    var legal by remember { mutableStateOf<LegalDocument?>(null) }
    val document = legal
    if (document != null) {
        PlatformBackHandler { legal = null }
        LegalScreen(document, onBack = { legal = null })
    } else {
        OnboardingRoute(
            onOpenSecuritySettings = openSecuritySettings,
            onOpenTerms = { legal = LegalDocument.Terms },
            onOpenPrivacy = { legal = LegalDocument.Privacy },
        )
    }
}
