package dev.spaceapps.secvault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.spaceapps.secvault.resources.Res
import dev.spaceapps.secvault.resources.version_format
import org.jetbrains.compose.resources.stringResource
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationProtectedDataWillBecomeUnavailable

private const val SHORT_VERSION_KEY = "CFBundleShortVersionString"
private const val BUILD_KEY = "CFBundleVersion"

@Composable
internal actual fun rememberAppVersion(): String {
    val info = remember { NSBundle.mainBundle.infoDictionary }
    return stringResource(
        Res.string.version_format,
        info?.get(SHORT_VERSION_KEY)?.toString().orEmpty(),
        info?.get(BUILD_KEY)?.toString().orEmpty(),
    )
}

/** iOS has no screenshot blocking API; the setting is hidden there. */
@Composable
internal actual fun SecureWindow(enabled: Boolean) = Unit

internal actual val isScreenshotBlockingSupported: Boolean = false

/** Protected data becomes unavailable right after the device locks (needs a passcode, which Vault requires). */
@Composable
internal actual fun DeviceLockEffect(onLocked: () -> Unit) {
    val callback = rememberUpdatedState(onLocked)
    DisposableEffect(Unit) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationProtectedDataWillBecomeUnavailable,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> callback.value() }
        onDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }
}
