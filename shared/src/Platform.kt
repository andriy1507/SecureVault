package dev.spaceapps.secvault

import androidx.compose.runtime.Composable

/** "1.0.0 (100)". */
@Composable
internal expect fun rememberAppVersion(): String

/** Android: FLAG_SECURE on the window (no screenshots, blank in recent apps). iOS: nothing. */
@Composable
internal expect fun SecureWindow(enabled: Boolean)

internal expect val isScreenshotBlockingSupported: Boolean

/** Calls [onLocked] when the screen turns off / the phone locks while the app is running. */
@Composable
internal expect fun DeviceLockEffect(onLocked: () -> Unit)
